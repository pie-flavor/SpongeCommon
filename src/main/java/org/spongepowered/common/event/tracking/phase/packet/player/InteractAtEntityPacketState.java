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

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.type.HandType;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.cause.entity.spawn.SpawnTypes;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.world.World;
import org.spongepowered.asm.util.PrettyPrinter;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.bridge.OwnershipTrackedBridge;
import org.spongepowered.common.event.SpongeCommonEventFactory;
import org.spongepowered.common.event.tracking.TrackingUtil;
import org.spongepowered.common.event.tracking.context.ItemDropData;
import org.spongepowered.common.event.tracking.phase.packet.BasicPacketContext;
import org.spongepowered.common.event.tracking.phase.packet.BasicPacketState;
import org.spongepowered.common.item.inventory.util.ItemStackUtil;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.client.CUseEntityPacket;

public final class InteractAtEntityPacketState extends BasicPacketState {

    @Override
    public boolean ignoresItemPreMerging() {
        return true;
    }

    @Override
    public boolean isPacketIgnored(IPacket<?> packetIn, ServerPlayerEntity packetPlayer) {
        final CUseEntityPacket useEntityPacket = (CUseEntityPacket) packetIn;
        // There are cases where a player is interacting with an entity that doesn't exist on the server.
        @Nullable net.minecraft.entity.Entity entity = useEntityPacket.getEntityFromWorld(packetPlayer.world);
        return entity == null;
    }

    @Override
    public void populateContext(ServerPlayerEntity playerMP, IPacket<?> packet, BasicPacketContext context) {
        final CUseEntityPacket useEntityPacket = (CUseEntityPacket) packet;
        final ItemStack stack = ItemStackUtil.cloneDefensive(playerMP.getHeldItem(useEntityPacket.getHand()));
        if (stack != null) {
            context.itemUsed(stack);
        }
        final HandType handType = (HandType) (Object) useEntityPacket.getHand();
        context.handUsed(handType);
    }

    @Override
    public boolean doesCaptureEntityDrops(BasicPacketContext context) {
        return true;
    }

    @Override
    public void unwind(BasicPacketContext context) {
        final ServerPlayerEntity player = context.getPacketPlayer();

        final CUseEntityPacket useEntityPacket = context.getPacket();
        final net.minecraft.entity.Entity entity = useEntityPacket.getEntityFromWorld(player.world);
        if (entity == null) {
            // Something happened?
            return;
        }
        final World spongeWorld = (World) player.world;
        if (entity instanceof OwnershipTrackedBridge) {
            ((OwnershipTrackedBridge) entity).tracked$setOwnerReference((User) player);
        } else {
            ((Entity) entity).setNotifier(player.getUniqueID());
        }


        try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            frame.pushCause(player);
            frame.addContext(EventContextKeys.SPAWN_TYPE, SpawnTypes.PLACEMENT);
            context.getCapturedEntitySupplier().acceptAndClearIfNotEmpty(entities -> {
                SpongeCommonEventFactory.callSpawnEntity(entities, context);
            });
            context.getCapturedItemsSupplier().acceptAndClearIfNotEmpty(entities -> {
                final List<Entity> items = entities.stream().map(entity1 -> (Entity) entity1).collect(Collectors.toList());
                SpongeCommonEventFactory.callSpawnEntity(items, context);
            });
            context.getPerEntityItemDropSupplier().acceptAndClearIfNotEmpty(map -> {
                final PrettyPrinter printer = new PrettyPrinter(80);
                printer.add("Processing Interact At Entity").centre().hr();
                printer.add("The item stacks captured are: ");

                for (Map.Entry<UUID, Collection<ItemDropData>> entry : map.asMap().entrySet()) {
                    printer.add("  - Entity with UUID: %s", entry.getKey());
                    for (ItemDropData stack : entry.getValue()) {
                        printer.add("    - %s", stack);
                    }
                }
                printer.trace(System.err);
            });
            context.getPerEntityItemEntityDropSupplier().acceptAndClearIfNotEmpty(map -> {
                for (Map.Entry<UUID, Collection<ItemEntity>> entry : map.asMap().entrySet()) {
                    final UUID entityUuid = entry.getKey();
                    final net.minecraft.entity.Entity entityFromUuid = player.getServerWorld().getEntityFromUuid(entityUuid);
                    if (entityFromUuid != null) {
                        final List<Entity> entities = entry.getValue()
                            .stream()
                            .map(entity1 -> (Entity) entity1)
                            .collect(Collectors.toList());
                        if (!entities.isEmpty()) {
                            // TODO - Remove the frame modifications to uncomment this line so we can simplify our code.
                            // SpongeCommonEventFactory.callDropItemCustom(entities, context);
                            DropItemEvent.Custom event =
                                SpongeEventFactory.createDropItemEventCustom(frame.getCurrentCause(),
                                    entities);
                            SpongeImpl.postEvent(event);
                            if (!event.isCancelled()) {
                                processSpawnedEntities(player, event);
                            }
                        }
                    }
                }
            });
            context.getCapturedItemStackSupplier().acceptAndClearIfNotEmpty(drops -> {
                final List<ItemEntity> items =
                    drops.stream().map(drop -> drop.create(player.getServerWorld())).collect(Collectors.toList());
                final List<Entity> entities = items
                    .stream()
                    .map(entity1 -> (Entity) entity1)
                    .collect(Collectors.toList());
                if (!entities.isEmpty()) {
                    DropItemEvent.Custom event = SpongeEventFactory.createDropItemEventCustom(frame.getCurrentCause(),
                        entities);
                    SpongeImpl.postEvent(event);
                    if (!event.isCancelled()) {
                        processSpawnedEntities(player, event);
                    }
                }

            });
        }
        // TODO - Determine if we need to pass the supplier or perform some parameterized
        //  process if not empty method on the capture object.
        TrackingUtil.processBlockCaptures(context);
    }



    @Override
    public boolean tracksEntitySpecificDrops() {
        return true;
    }

}
