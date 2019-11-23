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
package org.spongepowered.common.data.manipulator.mutable.entity;

import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.immutable.entity.ImmutableAccelerationData;
import org.spongepowered.api.data.manipulator.mutable.entity.AccelerationData;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.value.Value.Mutable;
import org.spongepowered.common.data.manipulator.immutable.entity.ImmutableSpongeAccelerationData;
import org.spongepowered.common.data.manipulator.mutable.common.AbstractSingleData;
import org.spongepowered.common.data.value.mutable.SpongeValue;
import org.spongepowered.common.util.Constants;
import org.spongepowered.math.vector.Vector3d;

public class SpongeAccelerationData extends AbstractSingleData<Vector3d, AccelerationData, ImmutableAccelerationData> implements AccelerationData {

    public SpongeAccelerationData() {
        this(Vector3d.ZERO);
    }

    public SpongeAccelerationData(final double x, final double y, final double z) {
        this(new Vector3d(x, y, z));
    }

    public SpongeAccelerationData(final Vector3d acceleration) {
        super(AccelerationData.class, acceleration, Keys.ACCELERATION);
    }

    @Override
    protected Mutable<?> getValueGetter() {
        return acceleration();
    }

    @Override
    public AccelerationData copy() {
        return new SpongeAccelerationData();
    }

    @Override
    public ImmutableAccelerationData asImmutable() {
        return new ImmutableSpongeAccelerationData(getValue());
    }

    @Override
    public DataContainer toContainer() {
        return super.toContainer()
            .createView(Keys.ACCELERATION.getQuery())
            .set(Constants.Sponge.AccelerationData.ACCELERATION_X, getValue().getX())
            .set(Constants.Sponge.AccelerationData.ACCELERATION_Y, getValue().getY())
            .set(Constants.Sponge.AccelerationData.ACCELERATION_Z, getValue().getZ())
            .getContainer();
    }

    @Override
    public Mutable<Vector3d> acceleration() {
        return new SpongeValue<>(Keys.ACCELERATION, Vector3d.ZERO, this.getValue());
    }
}
