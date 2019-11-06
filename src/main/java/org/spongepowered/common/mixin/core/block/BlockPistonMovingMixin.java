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
import org.spongepowered.api.data.manipulator.immutable.block.ImmutableDirectionalData;
import org.spongepowered.api.data.manipulator.immutable.block.ImmutablePistonData;
import org.spongepowered.api.data.type.PistonType;
import org.spongepowered.api.data.value.BaseValue;
import org.spongepowered.api.util.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.common.data.ImmutableDataCachingUtil;
import org.spongepowered.common.data.manipulator.immutable.block.ImmutableSpongeDirectionalData;
import org.spongepowered.common.data.manipulator.immutable.block.ImmutableSpongePistonData;
import org.spongepowered.common.util.Constants;

import java.util.Optional;
import net.minecraft.block.MovingPistonBlock;
import net.minecraft.block.PistonHeadBlock;

@Mixin(MovingPistonBlock.class)
public abstract class BlockPistonMovingMixin extends BlockMixin {

    @SuppressWarnings("RedundantTypeArguments") // some java compilers will not calculate this generic correctly
    @Override
    public ImmutableList<ImmutableDataManipulator<?, ?>> bridge$getManipulators(final net.minecraft.block.BlockState blockState) {
        return ImmutableList.<ImmutableDataManipulator<?, ?>>of(impl$getPistonTypeFor(blockState), impl$getDirectionalData(blockState));
    }

    @Override
    public boolean bridge$supports(final Class<? extends ImmutableDataManipulator<?, ?>> immutable) {
        return ImmutablePistonData.class.isAssignableFrom(immutable) || ImmutableDirectionalData.class.isAssignableFrom(immutable);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public Optional<BlockState> bridge$getStateWithData(final net.minecraft.block.BlockState blockState, final ImmutableDataManipulator<?, ?> manipulator) {
        if (manipulator instanceof ImmutablePistonData) {
            final PistonHeadBlock.EnumPistonType pistonType =
                    (PistonHeadBlock.EnumPistonType) (Object) ((ImmutablePistonData) manipulator).type().get();
            return Optional.of((BlockState) blockState.withProperty(MovingPistonBlock.TYPE, pistonType));
        }
        if (manipulator instanceof ImmutableDirectionalData) {
            final Direction dir = ((ImmutableDirectionalData) manipulator).direction().get();
            return Optional.of((BlockState) blockState.withProperty(MovingPistonBlock.FACING, Constants.DirectionFunctions.getFor(dir)));
        }
        return super.bridge$getStateWithData(blockState, manipulator);
    }

    @Override
    public <E> Optional<BlockState> bridge$getStateWithValue(final net.minecraft.block.BlockState blockState, final Key<? extends BaseValue<E>> key, final E value) {
        if (key.equals(Keys.PISTON_TYPE)) {
            final PistonHeadBlock.EnumPistonType pistonType = (PistonHeadBlock.EnumPistonType) value;
            return Optional.of((BlockState) blockState.withProperty(MovingPistonBlock.TYPE, pistonType));
        }
        if (key.equals(Keys.DIRECTION)) {
            final Direction dir = (Direction) value;
            return Optional.of((BlockState) blockState.withProperty(MovingPistonBlock.FACING, Constants.DirectionFunctions.getFor(dir)));
        }
        return super.bridge$getStateWithValue(blockState, key, value);
    }

    @SuppressWarnings("ConstantConditions")
    private ImmutablePistonData impl$getPistonTypeFor(final net.minecraft.block.BlockState blockState) {
        return ImmutableDataCachingUtil.getManipulator(ImmutableSpongePistonData.class,
                (PistonType) (Object) blockState.get(MovingPistonBlock.TYPE));
    }

    private ImmutableDirectionalData impl$getDirectionalData(final net.minecraft.block.BlockState blockState) {
        return ImmutableDataCachingUtil.getManipulator(ImmutableSpongeDirectionalData.class,
                Constants.DirectionFunctions.getFor(blockState.get(MovingPistonBlock.FACING)));
    }
}
