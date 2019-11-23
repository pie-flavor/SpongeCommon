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
package org.spongepowered.common.event.tracking.phase.packet.inventory;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.cause.entity.spawn.SpawnTypes;
import org.spongepowered.api.event.item.inventory.container.InteractContainerEvent;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.common.event.SpongeCommonEventFactory;
import org.spongepowered.common.event.tracking.TrackingUtil;
import org.spongepowered.common.event.tracking.phase.packet.BasicPacketContext;
import org.spongepowered.common.event.tracking.phase.packet.BasicPacketState;
import org.spongepowered.common.item.inventory.util.ItemStackUtil;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.network.IPacket;

public final class CloseWindowState extends BasicPacketState {

    @Override
    public void populateContext(ServerPlayerEntity playerMP, IPacket<?> packet, BasicPacketContext context) {
        context.openContainer(playerMP.openContainer);
    }

    @Override
    public void unwind(BasicPacketContext context) {
        final ServerPlayerEntity player = context.getSource(ServerPlayerEntity.class).get();
        final Container container = context.getOpenContainer();
        ItemStackSnapshot lastCursor = context.getCursor();
        ItemStackSnapshot newCursor = ItemStackUtil.snapshotOf(player.inventory.getItemStack());
        if (lastCursor != null) {
            Sponge.getCauseStackManager().pushCause(player);
            InteractContainerEvent.Close event =
                    SpongeCommonEventFactory.callInteractInventoryCloseEvent(container, player, lastCursor, newCursor, true);
            if (event.isCancelled()) {
                Sponge.getCauseStackManager().popCause();
                return;
            }
            Sponge.getCauseStackManager().popCause();
        }

        if (context.getCapturedItemsSupplier().isEmpty() && context.getCapturedItemStackSupplier().isEmpty() && context.getCapturedBlockSupplier().isEmpty()) {
            return;
        }

        try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            frame.pushCause(player);
            frame.addContext(EventContextKeys.SPAWN_TYPE, SpawnTypes.DROPPED_ITEM);
            // items
            context.getCapturedItemsSupplier().acceptAndClearIfNotEmpty(items -> {
                final List<Entity> entities = items
                    .stream()
                    .map(entity -> (Entity) entity)
                    .collect(Collectors.toList());
                if (!entities.isEmpty()) {
                    SpongeCommonEventFactory.callDropItemClose(entities, context, () -> Optional.of((Player) player));
                }
            });
            // Pre-merged items
            context.getCapturedItemStackSupplier().acceptAndClearIfNotEmpty(stacks -> {
                final List<ItemEntity> items = stacks.stream()
                    .map(drop -> drop.create(player.getServerWorld()))
                    .collect(Collectors.toList());
                final List<Entity> entities = items
                    .stream()
                    .map(entity -> (Entity) entity)
                    .collect(Collectors.toList());
                if (!entities.isEmpty()) {
                    SpongeCommonEventFactory.callDropItemCustom(entities, context, () -> Optional.of((Player) player));
                }
            });
        }
        // TODO - Determine if we need to pass the supplier or perform some parameterized
        //  process if not empty method on the capture object.
        TrackingUtil.processBlockCaptures(context);

    }

    @Override
    public boolean doesCaptureEntityDrops(BasicPacketContext context) {
        return true;
    }
}
