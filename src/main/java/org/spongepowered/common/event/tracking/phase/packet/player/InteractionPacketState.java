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
package org.spongepowered.common.event.tracking.phase.packet.player;

import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.ThrowableEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.client.CPlayerDiggingPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.type.HandType;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.projectile.Projectile;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.cause.entity.spawn.SpawnTypes;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.world.BlockChangeFlags;
import org.spongepowered.asm.util.PrettyPrinter;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.block.SpongeBlockSnapshot;
import org.spongepowered.common.bridge.inventory.TrackedInventoryBridge;
import org.spongepowered.common.bridge.world.WorldServerBridge;
import org.spongepowered.common.event.ShouldFire;
import org.spongepowered.common.event.SpongeCommonEventFactory;
import org.spongepowered.common.event.tracking.TrackingUtil;
import org.spongepowered.common.event.tracking.context.ItemDropData;
import org.spongepowered.common.event.tracking.phase.packet.PacketState;
import org.spongepowered.common.item.inventory.util.ItemStackUtil;
import org.spongepowered.common.mixin.core.entity.EntityLivingBaseAccessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

public final class InteractionPacketState extends PacketState<InteractionPacketContext> {


    @Override
    public InteractionPacketContext createNewContext() {
        return new InteractionPacketContext(this);
    }

    @Override
    public boolean isInteraction() {
        return true;
    }

    @Override
    public void populateContext(final ServerPlayerEntity playerMP, final IPacket<?> packet, final InteractionPacketContext context) {
        final ItemStack stack = ItemStackUtil.cloneDefensive(playerMP.getHeldItemMainhand());
        if (stack != null) {
            context.itemUsed(stack);
        }
        final ItemStack itemInUse = ItemStackUtil.cloneDefensive(playerMP.getActiveItemStack());
        if (itemInUse != null) {
            context.activeItem(itemInUse);
        }
        final BlockPos target = ((CPlayerDiggingPacket) packet).getPosition();
        if (!playerMP.world.isBlockLoaded(target)) {
            context.targetBlock(BlockSnapshot.NONE);
        } else {
            context.targetBlock(((WorldServerBridge) playerMP.world).bridge$createSnapshot(target, BlockChangeFlags.NONE));
        }
        context.handUsed(HandTypes.MAIN_HAND);
    }

    @Override
    public boolean spawnEntityOrCapture(final InteractionPacketContext context, final Entity entity, final int chunkX, final int chunkZ) {
        return context.captureEntity(entity);
    }

    @Override
    public boolean shouldCaptureEntity() {
        return true;
    }

    @Override
    public boolean doesCaptureEntityDrops(final InteractionPacketContext context) {
        return true;
    }

    @Override
    public boolean tracksTileEntityChanges(final InteractionPacketContext currentContext) {
        return true;
    }

    @Override
    public boolean hasSpecificBlockProcess(final InteractionPacketContext context) {
        return true;
    }

    @Override
    public boolean doesCaptureNeighborNotifications(final InteractionPacketContext context) {
        return true;
    }

    @Override
    public boolean tracksBlockSpecificDrops(final InteractionPacketContext context) {
        return true;
    }

    @Override
    public boolean alreadyProcessingBlockItemDrops() {
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void unwind(final InteractionPacketContext phaseContext) {

        final ServerPlayerEntity player = phaseContext.getPacketPlayer();
        final ItemStack usedStack = phaseContext.getItemUsed();
        final HandType usedHand = phaseContext.getHandUsed();
        final ItemStackSnapshot usedSnapshot = ItemStackUtil.snapshotOf(usedStack);
        final Entity spongePlayer = (Entity) player;
        final BlockSnapshot targetBlock = phaseContext.getTargetBlock();
        
        final net.minecraft.item.ItemStack endActiveItem = player.getActiveItemStack();
        ((EntityLivingBaseAccessor) player).accessor$setActiveItemStack((net.minecraft.item.ItemStack) phaseContext.getActiveItem());

        try (final CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            frame.pushCause(spongePlayer);
            frame.addContext(EventContextKeys.SPAWN_TYPE, SpawnTypes.DROPPED_ITEM);
            frame.addContext(EventContextKeys.USED_ITEM, usedSnapshot);
            frame.addContext(EventContextKeys.USED_HAND, usedHand);
            frame.addContext(EventContextKeys.BLOCK_HIT, targetBlock);
            final boolean hasBlocks = !phaseContext.getCapturedBlockSupplier().isEmpty();
            final List<SpongeBlockSnapshot> capturedBlcoks = phaseContext.getCapturedOriginalBlocksChanged();
            final @Nullable BlockSnapshot firstBlockChange = hasBlocks ? capturedBlcoks.isEmpty()? null : capturedBlcoks.get(0) : null;
            if (hasBlocks) {
                if (!TrackingUtil.processBlockCaptures(phaseContext)) {
                    // Stop entities like XP from being spawned
                    phaseContext.getBlockItemDropSupplier().get().clear();
                    phaseContext.getCapturedItems().clear();
                    phaseContext.getPerEntityItemDropSupplier().get().clear();
                    phaseContext.getCapturedEntities().clear();
                    return;
                }
            }
            phaseContext.getBlockItemDropSupplier().acceptAndClearIfNotEmpty(map -> {
                if (ShouldFire.DROP_ITEM_EVENT_DESTRUCT) {
                    for (final Map.Entry<BlockPos, Collection<ItemEntity>> entry : map.asMap().entrySet()) {
                        if (!entry.getValue().isEmpty()) {
                            final List<Entity> items = entry.getValue().stream().map(entity -> (Entity) entity).collect(Collectors.toList());
                            final DropItemEvent.Destruct event =
                                SpongeEventFactory.createDropItemEventDestruct(Sponge.getCauseStackManager().getCurrentCause(), items);
                            SpongeImpl.postEvent(event);
                            if (!event.isCancelled()) {
                                processSpawnedEntities(player, event);
                            }
                        }
                    }
                } else {
                    for (final Map.Entry<BlockPos, Collection<ItemEntity>> entry : map.asMap().entrySet()) {
                        if (!entry.getValue().isEmpty()) {
                            processEntities(player, (Collection<Entity>) (Collection<?>) entry.getValue());
                        }
                    }
                }
            });


            phaseContext.getCapturedItemsSupplier()
                .acceptAndClearIfNotEmpty(items -> {
                    final ArrayList<Entity> entities = new ArrayList<>();
                    for (final ItemEntity item : items) {
                        entities.add((Entity) item);
                    }
                    final DropItemEvent.Dispense dispense =
                        SpongeEventFactory.createDropItemEventDispense(Sponge.getCauseStackManager().getCurrentCause(), entities);
                    SpongeImpl.postEvent(dispense);
                    if (!dispense.isCancelled()) {
                        processSpawnedEntities(player, dispense);
                    }
                });
            phaseContext.getPerEntityItemDropSupplier()
                .acceptAndClearIfNotEmpty(map -> {
                    if (map.isEmpty()) {
                        return;
                    }
                    final PrettyPrinter printer = new PrettyPrinter(80);
                    printer.add("Processing Interaction").centre().hr();
                    printer.add("The item stacks captured are: ");
                    for (final Map.Entry<UUID, Collection<ItemDropData>> entry : map.asMap().entrySet()) {
                        printer.add("  - Entity with UUID: %s", entry.getKey());
                        for (final ItemDropData stack : entry.getValue()) {
                            printer.add("    - %s", stack);
                        }
                    }
                    printer.trace(System.err);
                });
            phaseContext.getCapturedEntitySupplier().acceptAndClearIfNotEmpty(entities -> {
                throwEntitySpawnEvents(phaseContext, player, usedSnapshot, firstBlockChange, entities);
            });

            phaseContext.getPerEntityItemEntityDropSupplier().acceptAndClearIfNotEmpty((multimap -> {
                for (final Map.Entry<UUID, Collection<ItemEntity>> entry : multimap.asMap().entrySet()) {
                    if (entry.getKey().equals(player.getUniqueID())) {
                        throwEntitySpawnEvents(phaseContext, player, usedSnapshot, firstBlockChange, (Collection<Entity>) (Collection<?>) entry.getValue());
                    } else {
                        final net.minecraft.entity.Entity spawnedEntity = ((ServerWorld) player.world).func_175733_a(entry.getKey());
                        if (spawnedEntity != null) {
                            try (final CauseStackManager.StackFrame entityFrame = Sponge.getCauseStackManager().pushCauseFrame()) {
                                entityFrame.pushCause(spawnedEntity);
                                throwEntitySpawnEvents(phaseContext, player, usedSnapshot, firstBlockChange, (Collection<Entity>) (Collection<?>) entry.getValue());
                            }
                        }
                    }
                }
            }));

            final TrackedInventoryBridge trackedInventory = (TrackedInventoryBridge) player.openContainer;
            trackedInventory.bridge$setCaptureInventory(false);
            trackedInventory.bridge$getCapturedSlotTransactions().clear();
        }
        
        ((EntityLivingBaseAccessor) player).accessor$setActiveItemStack(endActiveItem);
    }

    private void throwEntitySpawnEvents(final InteractionPacketContext phaseContext, final ServerPlayerEntity player, final ItemStackSnapshot usedSnapshot,
        final BlockSnapshot firstBlockChange, final Collection<Entity> entities) {
        final List<Entity> projectiles = new ArrayList<>(entities.size());
        final List<Entity> spawnEggs = new ArrayList<>(entities.size());
        final List<Entity> xpOrbs = new ArrayList<>(entities.size());
        final List<Entity> normalPlacement = new ArrayList<>(entities.size());
        final List<Entity> items = new ArrayList<>(entities.size());
        for (final Entity entity : entities) {
            if (entity instanceof Projectile || entity instanceof ThrowableEntity) {
                projectiles.add(entity);
            } else if (usedSnapshot.getType() == ItemTypes.SPAWN_EGG) {
                spawnEggs.add(entity);
            } else if (entity instanceof ItemEntity) {
                items.add(entity);
            } else if (entity instanceof ExperienceOrbEntity) {
                xpOrbs.add(entity);
            } else {
                normalPlacement.add(entity);
            }
        }
        if (!projectiles.isEmpty()) {
            if (ShouldFire.SPAWN_ENTITY_EVENT) {
                try (final CauseStackManager.StackFrame frame2 = Sponge.getCauseStackManager().pushCauseFrame()) {
                    frame2.addContext(EventContextKeys.SPAWN_TYPE, SpawnTypes.PROJECTILE);
                    frame2.pushCause(usedSnapshot);
                    SpongeCommonEventFactory.callSpawnEntity(projectiles, phaseContext);
                }
            } else {
                processEntities(player, projectiles);
            }
        }
        if (!spawnEggs.isEmpty()) {
            if (ShouldFire.SPAWN_ENTITY_EVENT) {
                try (final CauseStackManager.StackFrame frame2 = Sponge.getCauseStackManager().pushCauseFrame()) {
                    frame2.addContext(EventContextKeys.SPAWN_TYPE, SpawnTypes.SPAWN_EGG);
                    frame2.pushCause(usedSnapshot);
                    SpongeCommonEventFactory.callSpawnEntity(spawnEggs, phaseContext);
                }
            } else {
                processEntities(player, spawnEggs);
            }
        }
        if (!items.isEmpty()) {
            if (ShouldFire.DROP_ITEM_EVENT_DISPENSE) {
                final DropItemEvent.Dispense dispense = SpongeEventFactory
                    .createDropItemEventDispense(Sponge.getCauseStackManager().getCurrentCause(), items);
                if (!SpongeImpl.postEvent(dispense)) {
                    processSpawnedEntities(player, dispense);
                }
            } else {
                processEntities(player, items);
            }
        }
        if (!xpOrbs.isEmpty()) {
            if (ShouldFire.SPAWN_ENTITY_EVENT) {
                try (final CauseStackManager.StackFrame stackFrame = Sponge.getCauseStackManager().pushCauseFrame()) {
                    if (firstBlockChange != null) {
                        stackFrame.pushCause(firstBlockChange);
                    }
                    stackFrame.addContext(EventContextKeys.SPAWN_TYPE, SpawnTypes.EXPERIENCE);
                    SpongeCommonEventFactory.callSpawnEntity(xpOrbs, phaseContext);
                }
            } else {
                processEntities(player, xpOrbs);
            }
        }
        if (!normalPlacement.isEmpty()) {
            if (ShouldFire.SPAWN_ENTITY_EVENT) {
                try (final CauseStackManager.StackFrame stackFrame = Sponge.getCauseStackManager().pushCauseFrame()) {
                    if (firstBlockChange != null) {
                        stackFrame.pushCause(firstBlockChange);
                    }
                    SpongeCommonEventFactory.callSpawnEntity(normalPlacement, phaseContext);
                }
            } else {
                processEntities(player, normalPlacement);
            }
        }
    }

    @Override
    public boolean tracksEntitySpecificDrops() {
        return true;
    }

}
