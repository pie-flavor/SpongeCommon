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
package org.spongepowered.common.event.tracking.phase.block;

import net.minecraft.entity.item.ItemEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.cause.entity.spawn.SpawnTypes;
import org.spongepowered.common.block.SpongeBlockSnapshot;
import org.spongepowered.common.bridge.world.WorldServerBridge;
import org.spongepowered.common.event.SpongeCommonEventFactory;
import org.spongepowered.common.event.tracking.TrackingUtil;
import org.spongepowered.common.event.tracking.context.GeneralizedContext;
import org.spongepowered.common.event.tracking.context.ItemDropData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

final class BlockDropItemsPhaseState extends BlockPhaseState {

    private final BiConsumer<CauseStackManager.StackFrame, GeneralizedContext> BLOCK_DROP_MODIFIER = super.getFrameModifier()
        .andThen((frame, ctx) -> {
            final BlockSnapshot blockSnapshot = ctx.getSource(BlockSnapshot.class)
                .orElseThrow(TrackingUtil.throwWithContext("Could not find a block dropping items!", ctx));
            frame.pushCause(blockSnapshot);
            frame.addContext(EventContextKeys.SPAWN_TYPE, SpawnTypes.DROPPED_ITEM);
        });

    BlockDropItemsPhaseState() {
    }

    @Override
    public BiConsumer<CauseStackManager.StackFrame, GeneralizedContext> getFrameModifier() {
        return this.BLOCK_DROP_MODIFIER;
    }

    @Override
    public GeneralizedContext createNewContext() {
        return super.createNewContext()
                .addBlockCaptures()
                .addEntityCaptures();
    }

    @SuppressWarnings({"unchecked", "Duplicates", "RedundantCast"})
    @Override
    public void unwind(GeneralizedContext context) {

        context.getCapturedItemsSupplier()
            .acceptAndClearIfNotEmpty(items -> {
                final ArrayList<Entity> entities = new ArrayList<>();
                for (ItemEntity item : items) {
                    entities.add((Entity) item);
                }
                SpongeCommonEventFactory.callDropItemDestruct(entities, context);
            });
        context.getBlockItemDropSupplier()
            .acceptAndClearIfNotEmpty(drops -> {
                drops.asMap().forEach((key, value) -> {
                    Sponge.getCauseStackManager().addContext(EventContextKeys.SPAWN_TYPE, SpawnTypes.DROPPED_ITEM);
                    SpongeCommonEventFactory.callDropItemDestruct(new ArrayList<>((Collection<? extends Entity>) (Collection<?>) value), context);
                });
            });
        context.getCapturedEntitySupplier()
            .acceptAndClearIfNotEmpty(entities -> SpongeCommonEventFactory.callSpawnEntity(entities, context));
        final SpongeBlockSnapshot blockSnapshot = context.getSource(SpongeBlockSnapshot.class)
            .orElseThrow(TrackingUtil.throwWithContext("Could not find a block dropping items!", context));
        final Optional<WorldServerBridge> maybeWorld = blockSnapshot.getWorldServer().map(worldserver -> (WorldServerBridge) worldserver);

        // TODO - Determine if we need to pass the supplier or perform some parameterized
        //  process if not empty method on the capture object.
        TrackingUtil.processBlockCaptures(context);
        context.getCapturedItemStackSupplier()
            .acceptAndClearIfNotEmpty(drops -> maybeWorld.ifPresent(mixinWorld -> {
                final List<ItemEntity> items = drops.stream()
                    .map(drop -> drop.create((ServerWorld) mixinWorld))
                    .collect(Collectors.toList());
                final List<Entity> entities = (List<Entity>) (List<?>) items;
                if (!entities.isEmpty()) {
                    Sponge.getCauseStackManager().addContext(EventContextKeys.SPAWN_TYPE, SpawnTypes.DROPPED_ITEM);
                    SpongeCommonEventFactory.callDropItemCustom(entities, context);
                }
                drops.clear();
            }));
        context.getBlockDropSupplier()
            .acceptAndClearIfNotEmpty(drops -> {
                for (BlockPos key : drops.asMap().keySet()) {
                    final List<ItemDropData> values = drops.get(key);
                    if (!values.isEmpty()) {
                        Sponge.getCauseStackManager().addContext(EventContextKeys.SPAWN_TYPE, SpawnTypes.DROPPED_ITEM);
                        TrackingUtil.spawnItemDataForBlockDrops(values, blockSnapshot, context);
                    }
                }
            });

    }

    @Override
    public boolean tracksBlockSpecificDrops(GeneralizedContext context) {
        return true;
    }
}
