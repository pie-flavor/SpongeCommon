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
package org.spongepowered.common.event.tracking.phase.tick;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.item.FallingBlockEntity;
import net.minecraft.entity.item.HangingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.item.ItemFrameEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.CombatEntry;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.Ageable;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.projectile.Projectile;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.event.cause.entity.spawn.SpawnTypes;
import org.spongepowered.common.block.SpongeBlockSnapshot;
import org.spongepowered.common.bridge.entity.EntityBridge;
import org.spongepowered.common.entity.EntityUtil;
import org.spongepowered.common.event.SpongeCommonEventFactory;
import org.spongepowered.common.event.tracking.TrackingUtil;
import org.spongepowered.common.event.tracking.phase.general.ExplosionContext;
import org.spongepowered.common.mixin.core.util.CombatEntryAccessor;
import org.spongepowered.common.mixin.core.util.CombatTrackerAccessor;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.common.world.BlockChange;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

class EntityTickPhaseState extends TickPhaseState<EntityTickContext> {


    private final BiConsumer<CauseStackManager.StackFrame, EntityTickContext> ENTITY_TICK_MODIFIER =
        super.getFrameModifier().andThen((frame, context) -> {
            final Entity tickingEntity = context.getSource(Entity.class)
                .orElseThrow(TrackingUtil.throwWithContext("Not ticking on an Entity!", context));
            if (tickingEntity instanceof FallingBlockEntity) {
                context.getOwner().ifPresent(frame::pushCause);
            }
            frame.pushCause(tickingEntity);
        });

    EntityTickPhaseState() {
    }

    @Override
    public BiConsumer<CauseStackManager.StackFrame, EntityTickContext> getFrameModifier() {
        return this.ENTITY_TICK_MODIFIER;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void unwind(final EntityTickContext phaseContext) {
        final Entity tickingEntity = phaseContext.getSource(Entity.class)
                .orElseThrow(TrackingUtil.throwWithContext("Not ticking on an Entity!", phaseContext));
        try (final CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            this.processCaptures(tickingEntity, phaseContext, frame);
        }
    }

    protected void processCaptures(final Entity tickingEntity, final EntityTickContext phaseContext, final CauseStackManager.StackFrame frame) {
        phaseContext.addNotifierAndOwnerToCauseStack(frame);
        // If we're doing bulk captures for blocks, go ahead and do them. otherwise continue with entity checks
        if (phaseContext.allowsBulkBlockCaptures()) {
            if (!TrackingUtil.processBlockCaptures(phaseContext)) {
                ((EntityBridge) tickingEntity).bridge$onCancelledBlockChange(phaseContext);
            }
        }
        // And finally, if we're not capturing entities, there's nothing left for us to do.
        if (!phaseContext.allowsBulkEntityCaptures()) {
            return; // The rest of this method is all about entity captures
        }
        phaseContext.getCapturedEntitySupplier()
                .acceptAndClearIfNotEmpty(entities -> {
                    final List<Entity> experience = new ArrayList<>(entities.size());
                    final List<Entity> nonExp = new ArrayList<>(entities.size());
                    final List<Entity> breeding = new ArrayList<>(entities.size());
                    final List<Entity> projectile = new ArrayList<>(entities.size());
                    for (final Entity entity : entities) {
                        if (entity instanceof ExperienceOrbEntity) {
                            experience.add(entity);
                        } else if (tickingEntity instanceof Ageable && tickingEntity.getClass() == entity.getClass()) {
                            breeding.add(entity);
                        } else if (entity instanceof Projectile) {
                            projectile.add(entity);
                        } else {
                            nonExp.add(entity);
                        }
                    }

                    if (!experience.isEmpty()) {
                        frame.addContext(EventContextKeys.SPAWN_TYPE, SpawnTypes.EXPERIENCE);
                        appendContextOfPossibleEntityDeath(tickingEntity, frame);
                        SpongeCommonEventFactory.callSpawnEntity(experience, phaseContext);
                        frame.removeContext(EventContextKeys.LAST_DAMAGE_SOURCE);
                    }
                    if (!breeding.isEmpty()) {
                        frame.addContext(EventContextKeys.SPAWN_TYPE, SpawnTypes.BREEDING);
                        if (tickingEntity instanceof AnimalEntity) {
                            final PlayerEntity playerInLove = ((AnimalEntity) tickingEntity).func_191993_do();
                            if (playerInLove != null) {
                                frame.addContext(EventContextKeys.PLAYER, (Player) playerInLove);
                            }
                        }
                        SpongeCommonEventFactory.callSpawnEntity(breeding, phaseContext);

                        frame.removeContext(EventContextKeys.PLAYER);
                    }
                    if (!projectile.isEmpty()) {
                        frame.addContext(EventContextKeys.SPAWN_TYPE, SpawnTypes.PROJECTILE);
                        SpongeCommonEventFactory.callSpawnEntity(projectile, phaseContext);
                        frame.removeContext(EventContextKeys.SPAWN_TYPE);

                    }
                    frame.addContext(EventContextKeys.SPAWN_TYPE, SpawnTypes.PASSIVE);
                    SpongeCommonEventFactory.callSpawnEntity(nonExp, phaseContext);
                    frame.removeContext(EventContextKeys.SPAWN_TYPE);

                });
        phaseContext.getCapturedItemsSupplier()
                .acceptAndClearIfNotEmpty(entities -> {
                    final ArrayList<Entity> capturedEntities = new ArrayList<>();
                    for (final ItemEntity entity : entities) {
                        capturedEntities.add((Entity) entity);
                    }
                    frame.addContext(EventContextKeys.SPAWN_TYPE, SpawnTypes.DROPPED_ITEM);
                    SpongeCommonEventFactory.callDropItemCustom(capturedEntities, phaseContext);
                    frame.removeContext(EventContextKeys.SPAWN_TYPE);
                });
        // This could be happening regardless whether block bulk captures are done or not.
        // Would depend on whether entity captures are done.
        phaseContext.getBlockItemDropSupplier()
            .acceptAndClearIfNotEmpty(map -> {
                final List<SpongeBlockSnapshot> capturedBlocks = phaseContext.getCapturedOriginalBlocksChanged();
                for (final SpongeBlockSnapshot snapshot : capturedBlocks) {
                    final BlockPos blockPos = snapshot.getBlockPos();
                    final Collection<ItemEntity> entityItems = map.get(blockPos);
                    if (!entityItems.isEmpty()) {
                        frame.pushCause(snapshot);
                        frame.addContext(EventContextKeys.SPAWN_TYPE, SpawnTypes.DROPPED_ITEM);
                        final List<Entity> items = entityItems.stream().map(entity -> (Entity) entity).collect(Collectors.toList());
                        SpongeCommonEventFactory.callDropItemDestruct(items, phaseContext);

                        frame.popCause();
                    }
                }

            });
        phaseContext.getCapturedItemStackSupplier()
                .acceptAndClearIfNotEmpty(drops -> {
                    final List<Entity> items = drops.stream()
                            .map(drop -> drop.create((ServerWorld) tickingEntity.getWorld()))
                            .map(entity -> (Entity) entity)
                            .collect(Collectors.toList());
                    frame.addContext(EventContextKeys.SPAWN_TYPE, SpawnTypes.DROPPED_ITEM);
                    SpongeCommonEventFactory.callDropItemCustom(items, phaseContext);
                });

        // Some entities (DynamicTrees) can tell blocks to break themselves while they're ticking, and
        // specifically having removed the block but not performed the drops until the entity is ticking.
        phaseContext.getPerBlockEntitySpawnSuppplier()
            .acceptAndClearIfNotEmpty(blockDrops -> blockDrops.asMap().forEach((pos, drops) -> {
                final List<Entity> items = drops.stream()
                    .filter(entity -> entity instanceof ItemEntity)
                    .map(entity2 -> (Entity) entity2)
                    .collect(Collectors.toList());
                final BlockSnapshot snapshot = tickingEntity.getWorld().createSnapshot(VecHelper.toVector3i(pos));
                frame.pushCause(snapshot);
                if (!items.isEmpty()) {
                    frame.addContext(EventContextKeys.SPAWN_TYPE, SpawnTypes.DROPPED_ITEM);
                    SpongeCommonEventFactory.callDropItemCustom(items, phaseContext);
                }
                final List<Entity> nonItems = drops.stream()
                    .filter(entity -> !(entity instanceof ItemEntity))
                    .map(entity1 -> (Entity) entity1)
                    .collect(Collectors.toList());
                if (!nonItems.isEmpty()) {
                    frame.addContext(EventContextKeys.SPAWN_TYPE, SpawnTypes.BLOCK_SPAWNING);
                    SpongeCommonEventFactory.callSpawnEntityCustom(nonItems, phaseContext);
                }
            }));

    }

    private void appendContextOfPossibleEntityDeath(final Entity tickingEntity, final CauseStackManager.StackFrame frame) {
        if (EntityUtil.isEntityDead((net.minecraft.entity.Entity) tickingEntity)) {
            if (tickingEntity instanceof LivingEntity) {
                final CombatEntry entry = ((CombatTrackerAccessor) ((LivingEntity) tickingEntity).func_110142_aN()).accessor$getBestCombatEntry();
                if (entry != null) {
                    if (((CombatEntryAccessor) entry).accessor$getDamageSrc() != null) {
                        frame.addContext(EventContextKeys.LAST_DAMAGE_SOURCE,
                                (DamageSource) ((CombatEntryAccessor) entry).accessor$getDamageSrc());
                    }
                }
            }
        }
    }

    @Override
    protected EntityTickContext createNewContext() {
        return new EntityTickContext(this).addCaptures();
    }

    @Override
    public void postBlockTransactionApplication(final BlockChange blockChange, final Transaction<? extends BlockSnapshot> transaction,
        final EntityTickContext context) {
        if (blockChange == BlockChange.BREAK) {
            final Entity tickingEntity = context.getSource(Entity.class).get();
            final BlockPos blockPos = VecHelper.toBlockPos(transaction.getOriginal().getPosition());
            final List<HangingEntity> hangingEntities = ((ServerWorld) tickingEntity.getWorld())
                .func_175647_a(HangingEntity.class, new AxisAlignedBB(blockPos, blockPos).func_72314_b(1.1D, 1.1D, 1.1D),
                    entityIn -> {
                        if (entityIn == null) {
                            return false;
                        }

                        final BlockPos entityPos = entityIn.func_180425_c();
                        // Hanging Neighbor Entity
                        if (entityPos.equals(blockPos.func_177982_a(0, 1, 0))) {
                            return true;
                        }

                        // Check around source block
                        final Direction entityFacing = entityIn.func_174811_aO();

                        if (entityFacing == Direction.NORTH) {
                            return entityPos.equals(blockPos.func_177971_a(Constants.Entity.HANGING_OFFSET_NORTH));
                        } else if (entityFacing == Direction.SOUTH) {
                            return entityIn.func_180425_c().equals(blockPos.func_177971_a(Constants.Entity.HANGING_OFFSET_SOUTH));
                        } else if (entityFacing == Direction.WEST) {
                            return entityIn.func_180425_c().equals(blockPos.func_177971_a(Constants.Entity.HANGING_OFFSET_WEST));
                        } else if (entityFacing == Direction.EAST) {
                            return entityIn.func_180425_c().equals(blockPos.func_177971_a(Constants.Entity.HANGING_OFFSET_EAST));
                        }
                        return false;
                    });
            for (final HangingEntity entityHanging : hangingEntities) {
                if (entityHanging instanceof ItemFrameEntity) {
                    final ItemFrameEntity itemFrame = (ItemFrameEntity) entityHanging;
                    if (!itemFrame.field_70128_L) {
                        itemFrame.func_146065_b((net.minecraft.entity.Entity) tickingEntity, true);
                    }
                    itemFrame.func_70106_y();
                }
            }
        }
    }

    @Override
    public void appendContextPreExplosion(final ExplosionContext explosionContext, final EntityTickContext context) {
        if (!context.applyNotifierIfAvailable(explosionContext::owner)) {
            context.applyOwnerIfAvailable(explosionContext::owner);
        }
        explosionContext.source(context.getSource(Entity.class).orElseThrow(() -> new IllegalStateException("Ticking a non Entity")));
    }

    @Override
    public boolean spawnEntityOrCapture(final EntityTickContext context, final Entity entity, final int chunkX, final int chunkZ) {
        // Always need our source
        final Entity tickingEntity = context.getSource(Entity.class)
                .orElseThrow(TrackingUtil.throwWithContext("Not ticking on an Entity!", context));

        // Now to actually do something....
        if (context.allowsBulkEntityCaptures()) {
            // We need to check if any blocks are being broken at the moment. If they are, we need to
            // put them in the associated block pos multimap, because the block change may not have
            // occurred yet.
            // Refer to https://github.com/SpongePowered/SpongeCommon/issues/1443
            // Look at net.minecraft.world.World#breakBlock
            final Optional<BlockPos> pos = context.getCaptureBlockPos().getPos();
            if (pos.isPresent()) {
                return context.getPerBlockEntitySpawnSuppplier().get().put(pos.get(), (net.minecraft.entity.Entity) entity);
            } else {
                return context.getCapturedEntities().add(entity);
            }
            // Otherwise... we'll just spawn them normally.
        }
        // It kinda sucks we have to make the cause frame here, but if we're already here, we are
        // effectively already going to throw an event, and we're configured not to bulk capture.
        try (final CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            context.addNotifierAndOwnerToCauseStack(frame);
            frame.pushCause(tickingEntity);
            if (entity instanceof ExperienceOrbEntity) {
                frame.addContext(EventContextKeys.SPAWN_TYPE, SpawnTypes.EXPERIENCE);
                appendContextOfPossibleEntityDeath(tickingEntity, frame);
                final List<Entity> experience = new ArrayList<>(1);
                experience.add(entity);

                return SpongeCommonEventFactory.callSpawnEntity(experience, context);
            } else if (tickingEntity instanceof Ageable && tickingEntity.getClass() == entity.getClass()) {
                frame.addContext(EventContextKeys.SPAWN_TYPE, SpawnTypes.BREEDING);
                if (tickingEntity instanceof AnimalEntity) {
                    final PlayerEntity playerInLove = ((AnimalEntity) tickingEntity).func_191993_do();
                    if (playerInLove != null) {
                        frame.addContext(EventContextKeys.PLAYER, (Player) playerInLove);
                    }
                }
                final List<Entity> breeding = new ArrayList<>(1);
                breeding.add(entity);
                return SpongeCommonEventFactory.callSpawnEntity(breeding, context);

            } else if (entity instanceof Projectile) {
                frame.addContext(EventContextKeys.SPAWN_TYPE, SpawnTypes.PROJECTILE);
                final List<Entity> projectile = new ArrayList<>(1);
                projectile.add(entity);
                return SpongeCommonEventFactory.callSpawnEntity(projectile, context);

            }
            final List<Entity> nonExp = new ArrayList<>(1);
            nonExp.add(entity);

            frame.addContext(EventContextKeys.SPAWN_TYPE, SpawnTypes.PASSIVE);
            return SpongeCommonEventFactory.callSpawnEntity(nonExp, context);
        }
    }

    @Override
    public boolean doesCaptureEntitySpawns() {
        return false;
    }

    @Override
    public boolean alreadyProcessingBlockItemDrops() {
        return true;
    }

    /**
     * Specifically overridden here because some states have defaults and don't check the context.
     * @param context The context
     * @return True if bulk block captures are usable for this entity type (default true)
     */
    @Override
    public boolean doesBulkBlockCapture(final EntityTickContext context) {
        return context.allowsBulkBlockCaptures();
    }

    /**
     * Specifically overridden here because some states have defaults and don't check the context.
     * @param context The context
     * @return True if block events are to be tracked by the specific type of entity (default is true)
     */
    @Override
    public boolean doesBlockEventTracking(final EntityTickContext context) {
        return context.allowsBlockEvents();
    }
}
