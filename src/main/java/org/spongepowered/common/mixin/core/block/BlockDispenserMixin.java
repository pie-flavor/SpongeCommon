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
package org.spongepowered.common.mixin.core.block;

import com.google.common.collect.ImmutableList;
import net.minecraft.block.DispenserBlock;
import net.minecraft.dispenser.IDispenseItemBehavior;
import net.minecraft.dispenser.ProxyBlockSource;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.DispenserTileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.ImmutableDataManipulator;
import org.spongepowered.api.data.manipulator.immutable.block.ImmutableDirectionalData;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.BlockChangeFlags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.block.SpongeBlockSnapshot;
import org.spongepowered.common.bridge.world.WorldServerBridge;
import org.spongepowered.common.bridge.world.chunk.ChunkBridge;
import org.spongepowered.common.data.ImmutableDataCachingUtil;
import org.spongepowered.common.data.manipulator.immutable.block.ImmutableSpongeDirectionalData;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.phase.block.BlockPhase;
import org.spongepowered.common.item.inventory.util.ItemStackUtil;
import org.spongepowered.common.util.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

@Mixin(DispenserBlock.class)
public abstract class BlockDispenserMixin extends BlockMixin {

    private ItemStack originalItem = ItemStack.EMPTY;
    private PhaseContext<?> impl$context = PhaseContext.empty();

    @Shadow protected abstract void dispense(World worldIn, BlockPos pos);

    @SuppressWarnings("RedundantTypeArguments") // some java compilers will not calculate this generic correctly
    @Override
    public ImmutableList<ImmutableDataManipulator<?, ?>> bridge$getManipulators(final net.minecraft.block.BlockState blockState) {
        return ImmutableList.<ImmutableDataManipulator<?, ?>>of(impl$getDirectionalData(blockState));
    }

    @Override
    public boolean bridge$supports(final Class<? extends ImmutableDataManipulator<?, ?>> immutable) {
        return ImmutableDirectionalData.class.isAssignableFrom(immutable);
    }

    @Override
    public Optional<BlockState> bridge$getStateWithData(final net.minecraft.block.BlockState blockState, final ImmutableDataManipulator<?, ?> manipulator) {
        if (manipulator instanceof ImmutableDirectionalData) {
            return Optional.of((BlockState) blockState.withProperty(DispenserBlock.FACING, Constants.DirectionFunctions
                .getFor(((ImmutableDirectionalData) manipulator).direction().get())));
        }
        return super.bridge$getStateWithData(blockState, manipulator);
    }

    @Override
    public <E> Optional<BlockState> bridge$getStateWithValue(final net.minecraft.block.BlockState blockState, final Key<? extends Value<E>> key, final E value) {
        if (key.equals(Keys.DIRECTION)) {
            return Optional.of((BlockState) blockState.withProperty(DispenserBlock.FACING, Constants.DirectionFunctions.getFor((Direction) value)));
        }
        return super.bridge$getStateWithValue(blockState, key, value);
    }

    private ImmutableDirectionalData impl$getDirectionalData(final net.minecraft.block.BlockState blockState) {
        return ImmutableDataCachingUtil.getManipulator(ImmutableSpongeDirectionalData.class,
                Constants.DirectionFunctions.getFor(blockState.get(DispenserBlock.FACING)));
    }

    @Inject(method = "dispense", at = @At(value = "HEAD"))
    private void impl$CreateContextOnDispensing(final World worldIn, final BlockPos pos, final CallbackInfo ci) {
        final net.minecraft.block.BlockState state = worldIn.getBlockState(pos);
        final SpongeBlockSnapshot spongeBlockSnapshot = ((WorldServerBridge) worldIn).bridge$createSnapshot(state, state, pos, BlockChangeFlags.ALL);
        final ChunkBridge mixinChunk = (ChunkBridge) worldIn.getChunkAt(pos);
        this.impl$context = BlockPhase.State.DISPENSE.createPhaseContext()
            .source(spongeBlockSnapshot)
            .owner(() -> mixinChunk.bridge$getBlockOwner(pos))
            .notifier(() -> mixinChunk.bridge$getBlockNotifier(pos))
            .buildAndSwitch();
    }

    @Inject(method = "dispense", at = @At(value = "RETURN"))
    private void impl$CloseContextOnDispensing(final World worldIn, final BlockPos pos, final CallbackInfo ci) {
        this.impl$context.close();
        this.impl$context = PhaseContext.empty();
    }

    @Inject(method = "dispense",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/dispenser/IBehaviorDispenseItem;dispense(Lnet/minecraft/dispenser/IBlockSource;Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;"
        ),
        slice = @Slice(
            from = @At(value = "FIELD", target = "Lnet/minecraft/dispenser/IBehaviorDispenseItem;DEFAULT_BEHAVIOR:Lnet/minecraft/dispenser/IBehaviorDispenseItem;"),
            to = @At("TAIL")
        ),
        locals = LocalCapture.CAPTURE_FAILSOFT
    )
    private void impl$InjectToStoreOriginalItem(
        final World worldIn, final BlockPos pos, final CallbackInfo ci, final ProxyBlockSource source, final DispenserTileEntity dispenser, final int slotIndex, final ItemStack dispensedItem, final IDispenseItemBehavior behavior) {
        this.originalItem = ItemStackUtil.cloneDefensiveNative(dispensedItem);
    }

    @Redirect(method = "dispense", at = @At(value = "INVOKE", target = "Lnet/minecraft/tileentity/TileEntityDispenser;setInventorySlotContents(ILnet/minecraft/item/ItemStack;)V"))
    private void impl$SetInventoryContentsThrowingEvent(final DispenserTileEntity dispenser, final int index, @Nullable final ItemStack stack) {
        final PhaseContext<?> context = PhaseTracker.getInstance().getCurrentContext();
        // If we captured nothing, simply set the slot contents and return
        if (context.getCapturedItemsOrEmptyList().isEmpty()) {
            dispenser.setInventorySlotContents(index, stack);
            return;
        }
        final ItemStack dispensedItem = context.getCapturedItems().get(0).getItem();
        final ItemStackSnapshot snapshot = ItemStackUtil.snapshotOf(dispensedItem);
        final List<ItemStackSnapshot> original = new ArrayList<>();
        original.add(snapshot);
        try (final CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            frame.pushCause(dispenser);
            final DropItemEvent.Pre dropEvent = SpongeEventFactory.createDropItemEventPre(frame.getCurrentCause(), ImmutableList.of(snapshot), original);
            SpongeImpl.postEvent(dropEvent);
            if (dropEvent.isCancelled()) {
                dispenser.setInventorySlotContents(index, this.originalItem);
                context.getCapturedItems().clear();
                return;
            }
            if (dropEvent.getDroppedItems().isEmpty()) {
                context.getCapturedItems().clear();
            }

            dispenser.setInventorySlotContents(index, stack);
        }
    }

}
