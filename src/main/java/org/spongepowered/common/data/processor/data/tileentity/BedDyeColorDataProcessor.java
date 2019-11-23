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
import org.spongepowered.api.data.manipulator.immutable.ImmutableDyeableData;
import org.spongepowered.api.data.manipulator.mutable.DyeableData;
import org.spongepowered.api.data.type.DyeColor;
import org.spongepowered.api.data.type.DyeColors;
import org.spongepowered.api.data.value.Value.Immutable;
import org.spongepowered.api.data.value.Value.Mutable;
import org.spongepowered.api.data.value.ValueContainer;
import org.spongepowered.common.data.ImmutableDataCachingUtil;
import org.spongepowered.common.data.manipulator.mutable.SpongeDyeableData;
import org.spongepowered.common.data.processor.common.AbstractTileEntitySingleDataProcessor;
import org.spongepowered.common.data.value.immutable.ImmutableSpongeValue;
import org.spongepowered.common.data.value.mutable.SpongeValue;

import java.util.Optional;
import net.minecraft.tileentity.BedTileEntity;

public class BedDyeColorDataProcessor extends AbstractTileEntitySingleDataProcessor<BedTileEntity, DyeColor, Mutable<DyeColor>, DyeableData,
        ImmutableDyeableData> {

    public BedDyeColorDataProcessor() {
        super(BedTileEntity.class, Keys.DYE_COLOR);
    }

    @Override
    protected boolean set(BedTileEntity dataHolder, DyeColor value) {
        dataHolder.setColor((net.minecraft.item.DyeColor) (Object) value);
        return true;
    }

    @Override
    protected Optional<DyeColor> getVal(BedTileEntity dataHolder) {
        return Optional.of((DyeColor) (Object) dataHolder.getColor());
    }

    @Override
    protected Immutable<DyeColor> constructImmutableValue(DyeColor value) {
        return ImmutableDataCachingUtil.getValue(ImmutableSpongeValue.class, Keys.DYE_COLOR, DyeColors.RED, value);
    }

    @Override
    protected Mutable<DyeColor> constructValue(DyeColor actualValue) {
        // Beds are red by default, not white.
        return new SpongeValue<>(Keys.DYE_COLOR, DyeColors.RED, actualValue);
    }

    @Override
    public DataTransactionResult removeFrom(ValueContainer<?> container) {
        return DataTransactionResult.failNoData();
    }

    @Override
    protected DyeableData createManipulator() {
        // Beds are red by default, not white.
        return new SpongeDyeableData(DyeColors.RED);
    }
}
