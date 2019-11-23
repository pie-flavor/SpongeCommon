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
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.ImmutableDataManipulator;
import org.spongepowered.api.data.manipulator.immutable.block.ImmutableAttachedData;
import org.spongepowered.api.data.manipulator.immutable.block.ImmutableDirectionalData;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.util.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.common.data.ImmutableDataCachingUtil;
import org.spongepowered.common.data.manipulator.immutable.block.ImmutableSpongeAttachedData;
import org.spongepowered.common.data.manipulator.immutable.block.ImmutableSpongeDirectionalData;
import org.spongepowered.common.util.Constants;

import java.util.Optional;
import net.minecraft.block.TorchBlock;

@Mixin(TorchBlock.class)
public abstract class BlockTorchMixin extends BlockMixin {

    @SuppressWarnings("RedundantTypeArguments") // some JDK's can fail to compile without the explicit type generics
    @Override
    public ImmutableList<ImmutableDataManipulator<?, ?>> bridge$getManipulators(final net.minecraft.block.BlockState blockState) {
        return ImmutableList.<ImmutableDataManipulator<?, ?>>of(impl$getIsAttachedFor(blockState), impl$getDirectionalData(blockState));
    }

    @Override
    public boolean bridge$supports(final Class<? extends ImmutableDataManipulator<?, ?>> immutable) {
        return ImmutableAttachedData.class.isAssignableFrom(immutable);
    }

    @Override
    public Optional<BlockState> bridge$getStateWithData(final net.minecraft.block.BlockState blockState, final ImmutableDataManipulator<?, ?> manipulator) {
        if (manipulator instanceof ImmutableAttachedData) {
            return Optional.of((BlockState) blockState);
        }
        if (manipulator instanceof ImmutableDirectionalData) {
            final Direction dir = Constants.DirectionFunctions.checkDirectionNotDown(((ImmutableDirectionalData) manipulator).direction().get());
            return Optional.of((BlockState) blockState.withProperty(TorchBlock.FACING, Constants.DirectionFunctions.getFor(dir)));
        }
        return super.bridge$getStateWithData(blockState, manipulator);
    }

    @Override
    public <E> Optional<BlockState> bridge$getStateWithValue(final net.minecraft.block.BlockState blockState, final Key<? extends Value<E>> key, final E value) {
        if (key.equals(Keys.ATTACHED)) {
            return Optional.of((BlockState) blockState);
        }
        if (key.equals(Keys.DIRECTION)) {
            final Direction dir = Constants.DirectionFunctions.checkDirectionNotDown((Direction) value);
            return Optional.of((BlockState) blockState.withProperty(TorchBlock.FACING, Constants.DirectionFunctions.getFor(dir)));
        }
        return super.bridge$getStateWithValue(blockState, key, value);
    }

    private ImmutableAttachedData impl$getIsAttachedFor(final net.minecraft.block.BlockState blockState) {
        return ImmutableDataCachingUtil.getManipulator(ImmutableSpongeAttachedData.class,
                blockState.get(TorchBlock.FACING) != net.minecraft.util.Direction.UP);
    }

    private ImmutableDirectionalData impl$getDirectionalData(final net.minecraft.block.BlockState blockState) {
        return ImmutableDataCachingUtil.getManipulator(ImmutableSpongeDirectionalData.class,
                Constants.DirectionFunctions.getFor(blockState.get(TorchBlock.FACING)));
    }
}
