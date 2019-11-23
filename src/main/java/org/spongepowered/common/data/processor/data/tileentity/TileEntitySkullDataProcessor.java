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
package org.spongepowered.common.data.processor.data.tileentity;

import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.immutable.ImmutableSkullData;
import org.spongepowered.api.data.manipulator.mutable.SkullData;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.type.SkullType;
import org.spongepowered.api.data.type.SkullTypes;
import org.spongepowered.api.data.value.Value.Immutable;
import org.spongepowered.api.data.value.Value.Mutable;
import org.spongepowered.api.data.value.ValueContainer;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.data.manipulator.mutable.SpongeSkullData;
import org.spongepowered.common.data.processor.common.AbstractTileEntitySingleDataProcessor;
import org.spongepowered.common.data.processor.common.SkullUtils;
import org.spongepowered.common.data.type.SpongeSkullType;
import org.spongepowered.common.data.util.DataUtil;
import org.spongepowered.common.data.value.immutable.ImmutableSpongeValue;
import org.spongepowered.common.data.value.mutable.SpongeValue;

import java.util.Optional;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.SkullTileEntity;

public class TileEntitySkullDataProcessor
        extends AbstractTileEntitySingleDataProcessor<SkullTileEntity, SkullType, Mutable<SkullType>, SkullData, ImmutableSkullData> {

    public TileEntitySkullDataProcessor() {
        super(SkullTileEntity.class, Keys.SKULL_TYPE);
    }

    @Override
    protected boolean supports(final SkullTileEntity skull) {
        return SkullUtils.supportsObject(skull);
    }

    @Override
    protected Optional<SkullType> getVal(final SkullTileEntity skull) {
        return Optional.of(SkullUtils.getSkullType(skull.getSkullType()));
    }

    @Override
    public Optional<SkullData> fill(final DataContainer container, final SkullData skullData) {
        return Optional.of(skullData.set(Keys.SKULL_TYPE, SpongeImpl.getGame().getRegistry()
                .getType(SkullType.class, DataUtil.getData(container, Keys.SKULL_TYPE, String.class)).get()));
    }

    @Override
    protected boolean set(final SkullTileEntity skull, final SkullType type) {
        skull.setType(((SpongeSkullType) type).getByteId());
        skull.markDirty();
        final BlockState blockState = skull.getWorld().getBlockState(skull.getPos());
        skull.getWorld().notifyBlockUpdate(skull.getPos(), blockState, blockState, 3);
        return true;
    }

    @Override
    public DataTransactionResult removeFrom(final ValueContainer<?> container) {
        return DataTransactionResult.failNoData();
    }

    @Override
    protected Mutable<SkullType> constructValue(final SkullType value) {
        return new SpongeValue<>(Keys.SKULL_TYPE, SkullTypes.SKELETON, value);
    }

    @Override
    protected Immutable<SkullType> constructImmutableValue(final SkullType value) {
        return ImmutableSpongeValue.cachedOf(Keys.SKULL_TYPE, SkullTypes.SKELETON, value);
    }

    @Override
    protected SkullData createManipulator() {
        return new SpongeSkullData();
    }

}
