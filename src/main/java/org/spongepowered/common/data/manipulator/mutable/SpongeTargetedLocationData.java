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
package org.spongepowered.common.data.manipulator.mutable;

import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.immutable.ImmutableTargetedLocationData;
import org.spongepowered.api.data.manipulator.mutable.TargetedLocationData;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.value.Value.Mutable;
import org.spongepowered.common.data.manipulator.immutable.ImmutableSpongeTargetedLocationData;
import org.spongepowered.common.data.manipulator.mutable.common.AbstractSingleData;
import org.spongepowered.common.data.value.mutable.SpongeValue;
import org.spongepowered.math.vector.Vector3d;

public final class SpongeTargetedLocationData extends AbstractSingleData<Vector3d, TargetedLocationData, ImmutableTargetedLocationData>
        implements TargetedLocationData {

    public SpongeTargetedLocationData() {
        this(Vector3d.ZERO);
    }

    public SpongeTargetedLocationData(Vector3d value) {
        super(TargetedLocationData.class, value, Keys.TARGETED_LOCATION);
    }

    @Override
    public TargetedLocationData copy() {
        return new SpongeTargetedLocationData(this.getValue());
    }

    @Override
    protected Mutable<?> getValueGetter() {
        return this.target();
    }

    @Override
    public ImmutableTargetedLocationData asImmutable() {
        return new ImmutableSpongeTargetedLocationData(this.getValue());
    }

    @Override
    public Mutable<Vector3d> target() {
        return new SpongeValue<>(this.usedKey, this.getValue());
    }

    @Override
    public DataContainer toContainer() {
        return super.toContainer()
                .set(this.usedKey, this.getValue());
    }

}
