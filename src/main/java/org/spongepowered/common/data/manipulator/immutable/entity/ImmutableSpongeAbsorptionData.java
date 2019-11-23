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
package org.spongepowered.common.data.manipulator.immutable.entity;

import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.immutable.entity.ImmutableAbsorptionData;
import org.spongepowered.api.data.manipulator.mutable.entity.AbsorptionData;
import org.spongepowered.api.data.value.Value.Immutable;
import org.spongepowered.common.data.manipulator.immutable.common.AbstractImmutableSingleData;
import org.spongepowered.common.data.manipulator.mutable.entity.SpongeAbsorptionData;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.data.value.immutable.ImmutableSpongeValue;

public final class ImmutableSpongeAbsorptionData extends AbstractImmutableSingleData<Double, ImmutableAbsorptionData, AbsorptionData> implements ImmutableAbsorptionData {

    private final Immutable<Double> value;

    public ImmutableSpongeAbsorptionData(double value) {
        super(ImmutableAbsorptionData.class, value, Keys.ABSORPTION);
        this.value = new ImmutableSpongeValue<>(this.usedKey, Constants.Entity.DEFAULT_ABSORPTION, this.getValue());
    }

    @Override
    public Immutable<Double> absorption() {
        return this.getValueGetter();
    }

    @Override
    protected Immutable<Double> getValueGetter() {
        return this.value;
    }

    @Override
    public AbsorptionData asMutable() {
        return new SpongeAbsorptionData(this.getValue());
    }

}
