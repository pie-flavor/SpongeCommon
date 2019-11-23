/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.event.tracking.phase.general;

import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.cause.entity.spawn.SpawnTypes;
import org.spongepowered.api.event.item.inventory.ChangeInventoryEvent;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.bridge.inventory.TrackedInventoryBridge;
import org.spongepowered.common.bridge.world.chunk.ChunkBridge;
import org.spongepowered.common.entity.EntityUtil;
import org.spongepowered.common.entity.PlayerTracker;
import org.spongepowered.common.event.SpongeCommonEventFactory;
import org.spongepowered.common.event.tracking.TrackingUtil;
import org.spongepowered.common.event.tracking.context.ItemDropData;
import org.spongepowered.common.event.tracking.phase.packet.PacketPhaseUtil;
import org.spongepowered.common.world.BlockChange;
import org.spongepowered.common.world.WorldManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

final class CommandState extends GeneralState<CommandPhaseContext> {

    private final BiConsumer<CauseStackManager.StackFrame, CommandPhaseContext> COMMAND_MODIFIER = super.getFrameModifier()
        .andThen((frame, ctx) -> {
            ctx.getSource(Object.class).ifPresent(frame::pushCause);
        });

    @Override
    public CommandPhaseContext createNewContext() {
        return new CommandPhaseContext(this)
            .addCaptures()
            .addEntityDropCaptures();
    }

    @Override
    public BiConsumer<CauseStackManager.StackFrame, CommandPhaseContext> getFrameModifier() {
        return this.COMMAND_MODIFIER;
    }

    @Override
    public boolean ignoresItemPreMerging() {
        return true;
    }

    @Override
    public void postBlockTransactionApplication(final BlockChange blockChange, final Transaction<? extends BlockSnapshot> transaction,
        final CommandPhaseContext context) {
        // We want to investigate if there is a user on the cause stack
        // and if possible, associate the notiifer/owner based on the change flag
        // We have to check if there is a player, because command blocks can be triggered
        // without player interaction.
        // Fixes https://github.com/SpongePowered/SpongeForge/issues/2442
        Sponge.getCauseStackManager().getCurrentCause().first(User.class).ifPresent(user -> {
            TrackingUtil.associateTrackerToTarget(blockChange, transaction, user);
        });
   }

    @Override
    public void associateNeighborStateNotifier(final CommandPhaseContext context, @Nullable final BlockPos sourcePos, final Block block,
        final BlockPos notifyPos, final ServerWorld minecraftWorld, final PlayerTracker.Type notifier) {
        context.getSource(Player.class)
            .ifPresent(player -> ((ChunkBridge) minecraftWorld.getChunkAt(notifyPos))
                .bridge$addTrackedBlockPosition(block, notifyPos, player, PlayerTracker.Type.NOTIFIER));
    }

    @Override
    public void unwind(final CommandPhaseContext phaseContext) {
        final Optional<PlayerEntity> playerSource = phaseContext.getSource(PlayerEntity.class);
        final CauseStackManager csm = Sponge.getCauseStackManager();
        if (playerSource.isPresent()) {
            // Post event for inventory changes
            ((TrackedInventoryBridge) playerSource.get().inventory).bridge$setCaptureInventory(false);
            final List<SlotTransaction> list = ((TrackedInventoryBridge) playerSource.get().inventory).bridge$getCapturedSlotTransactions();
            if (!list.isEmpty()) {
                final ChangeInventoryEvent event = SpongeEventFactory.createChangeInventoryEvent(csm.getCurrentCause(),
                        ((Inventory) playerSource.get().inventory), list);
                SpongeImpl.postEvent(event);
                PacketPhaseUtil.handleSlotRestore(playerSource.get(), null, list, event.isCancelled());
                list.clear();
            }
        }
        final CommandSource sender = phaseContext.getSource(CommandSource.class)
                .orElseThrow(TrackingUtil.throwWithContext("Expected to be capturing a Command Sender, but none found!", phaseContext));
        // TODO - Determine if we need to pass the supplier or perform some parameterized
        //  process if not empty method on the capture object.
        TrackingUtil.processBlockCaptures(phaseContext);
        phaseContext.getCapturedEntitySupplier()
            .acceptAndClearIfNotEmpty(entities ->
            {
                // TODO the entity spawn causes are not likely valid,
                // need to investigate further.
                csm.addContext(EventContextKeys.SPAWN_TYPE, SpawnTypes.PLACEMENT);

                SpongeCommonEventFactory.callSpawnEntity(entities, phaseContext);
            });
        phaseContext.getPerEntityItemDropSupplier()
            .acceptAndClearIfNotEmpty(uuidItemStackMultimap ->
            {
                for (final Map.Entry<UUID, Collection<ItemDropData>> entry : uuidItemStackMultimap.asMap().entrySet())
                {
                    final UUID key = entry.getKey();
                    @Nullable
                    net.minecraft.entity.Entity foundEntity = null;
                    for (final ServerWorld worldServer : WorldManager.getWorlds())
                    {
                        final net.minecraft.entity.Entity entityFromUuid = worldServer.func_175733_a(key);
                        if (entityFromUuid != null)
                        {
                            foundEntity = entityFromUuid;
                            break;
                        }
                    }
                    final Optional<Entity> affectedEntity = Optional.ofNullable((Entity) foundEntity);
                    if (!affectedEntity.isPresent())
                    {
                        continue;
                    }
                    final Collection<ItemDropData> itemStacks = entry.getValue();
                    if (itemStacks.isEmpty())
                    {
                        return;
                    }
                    final List<ItemDropData> items = new ArrayList<>();
                    items.addAll(itemStacks);
                    itemStacks.clear();

                    final ServerWorld minecraftWorld = (ServerWorld) affectedEntity.get().getWorld();
                    if (!items.isEmpty())
                    {
                        csm.addContext(EventContextKeys.SPAWN_TYPE, SpawnTypes.DROPPED_ITEM);

                        final List<Entity> itemEntities = items.stream()
                            .map(data -> data.create(minecraftWorld))
                            .map(entity -> (Entity) entity)
                            .collect(Collectors.toList());
                        csm.pushCause(affectedEntity.get());
                        final DropItemEvent.Destruct destruct =
                            SpongeEventFactory.createDropItemEventDestruct(csm.getCurrentCause(), itemEntities);
                        SpongeImpl.postEvent(destruct);
                        csm.popCause();
                        if (!destruct.isCancelled())
                        {
                            final boolean isPlayer = sender instanceof Player;
                            final Player player = isPlayer ? (Player) sender : null;
                            EntityUtil.processEntitySpawnsFromEvent(destruct, () -> Optional.ofNullable(isPlayer ? player : null));
                        }

                    }
                }
            });
    }

    @Override
    public boolean spawnEntityOrCapture(final CommandPhaseContext context, final Entity entity, final int chunkX, final int chunkZ) {
        // Instead of bulk capturing entities that are spawned in a command, some commands could potentially
        try (final CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            frame.addContext(EventContextKeys.SPAWN_TYPE, SpawnTypes.PLACEMENT);

            final List<Entity> entities = new ArrayList<>(1);
            entities.add(entity);
            return SpongeCommonEventFactory.callSpawnEntity(entities, context);
        }
    }

    @Override
    public boolean doesCaptureEntitySpawns() {
        return false;
    }

    @Override
    public boolean tracksEntitySpecificDrops() {
        return true;
    }

}
