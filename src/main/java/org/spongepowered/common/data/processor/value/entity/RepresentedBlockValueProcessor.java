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
package org.spongepowered.common.data.processor.value.entity;

import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.value.Value.Immutable;
import org.spongepowered.api.data.value.Value.Mutable;
import org.spongepowered.api.data.value.ValueContainer;
import org.spongepowered.common.data.processor.common.AbstractSpongeValueProcessor;
import org.spongepowered.common.data.value.immutable.ImmutableSpongeValue;
import org.spongepowered.common.data.value.mutable.SpongeValue;

import java.util.Optional;
import net.minecraft.block.Blocks;
import net.minecraft.entity.item.minecart.AbstractMinecartEntity;

public class RepresentedBlockValueProcessor extends AbstractSpongeValueProcessor<AbstractMinecartEntity, BlockState, Mutable<BlockState>> {

    public RepresentedBlockValueProcessor() {
        super(AbstractMinecartEntity.class, Keys.REPRESENTED_BLOCK);
    }

    @Override
    protected Mutable<BlockState> constructValue(final BlockState value) {
        return new SpongeValue<>(Keys.REPRESENTED_BLOCK, (BlockState) Blocks.AIR.getDefaultState(), value);
    }

    @Override
    protected boolean set(final AbstractMinecartEntity container, final BlockState value) {
        container.setDisplayTile((net.minecraft.block.BlockState) value);
        return true;
    }

    @Override
    protected Optional<BlockState> getVal(final AbstractMinecartEntity container) {
        if(!container.hasDisplayTile()) return Optional.empty();
        return Optional.of((BlockState) container.getDisplayTile());
    }

    @Override
    protected Immutable<BlockState> constructImmutableValue(final BlockState value) {
        return new ImmutableSpongeValue<>(Keys.REPRESENTED_BLOCK, (BlockState) Blocks.AIR.getDefaultState(), value);
    }

    @Override
    public DataTransactionResult removeFrom(final ValueContainer<?> container) {
        if(container instanceof AbstractMinecartEntity) {
            final AbstractMinecartEntity cart = (AbstractMinecartEntity) container;
            final Immutable<BlockState> block = new ImmutableSpongeValue<>(Keys.REPRESENTED_BLOCK, (BlockState) cart.getDisplayTile());
            cart.setHasDisplayTile(false);
            return DataTransactionResult.builder().replace(block).build();
        }
        return DataTransactionResult.failNoData();
    }
}
