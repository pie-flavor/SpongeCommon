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
package org.spongepowered.common.data.processor.data.entity;

import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.immutable.ImmutableDyeableData;
import org.spongepowered.api.data.manipulator.mutable.DyeableData;
import org.spongepowered.api.data.type.DyeColor;
import org.spongepowered.api.data.type.DyeColors;
import org.spongepowered.api.data.value.Value.Immutable;
import org.spongepowered.api.data.value.Value.Mutable;
import org.spongepowered.api.data.value.ValueContainer;
import org.spongepowered.common.data.manipulator.mutable.SpongeDyeableData;
import org.spongepowered.common.data.processor.common.AbstractEntitySingleDataProcessor;
import org.spongepowered.common.data.value.SpongeValueFactory;

import java.util.Optional;
import net.minecraft.entity.passive.WolfEntity;

public class WolfDyeColorDataProcessor extends AbstractEntitySingleDataProcessor<WolfEntity, DyeColor, Mutable<DyeColor>, DyeableData, ImmutableDyeableData> {

    public WolfDyeColorDataProcessor() {
        super(WolfEntity.class, Keys.DYE_COLOR);
    }

    @Override
    protected Mutable<DyeColor> constructValue(DyeColor actualValue) {
        return SpongeValueFactory.getInstance().createValue(Keys.DYE_COLOR, actualValue, DyeColors.BLACK);
    }

    @Override
    protected boolean set(WolfEntity container, DyeColor value) {
        container.setCollarColor((net.minecraft.item.DyeColor) (Object) value);
        return true;
    }

    @Override
    protected Optional<DyeColor> getVal(WolfEntity container) {
        return Optional.of((DyeColor) (Object) container.getCollarColor());
    }

    @Override
    protected Immutable<DyeColor> constructImmutableValue(DyeColor value) {
        return constructValue(value).asImmutable();
    }

    @Override
    public DataTransactionResult removeFrom(ValueContainer<?> container) {
        return DataTransactionResult.failNoData();
    }

    @Override
    protected DyeableData createManipulator() {
        return new SpongeDyeableData();
    }
}
