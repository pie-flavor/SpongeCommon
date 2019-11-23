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

import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.value.Value.Immutable;
import org.spongepowered.api.data.value.Value.Mutable;
import org.spongepowered.api.data.value.ValueContainer;
import org.spongepowered.common.data.processor.common.AbstractSpongeValueProcessor;
import org.spongepowered.common.data.value.mutable.SpongeValue;
import org.spongepowered.common.mixin.core.entity.item.EntityArmorStandAccessor;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.math.vector.Vector3d;
import java.util.Optional;
import net.minecraft.entity.item.ArmorStandEntity;

public class LeftArmRotationValueProcessor extends AbstractSpongeValueProcessor<ArmorStandEntity, Vector3d, Mutable<Vector3d>> {

    public LeftArmRotationValueProcessor() {
        super(ArmorStandEntity.class, Keys.LEFT_ARM_ROTATION);
    }

    @Override
    public DataTransactionResult removeFrom(final ValueContainer<?> container) {
        return DataTransactionResult.failNoData();
    }

    @Override
    protected Mutable<Vector3d> constructValue(final Vector3d actualValue) {
        return new SpongeValue<>(Keys.LEFT_ARM_ROTATION, Constants.Entity.ArmorStand.DEFAULT_LEFT_ARM_ROTATION, actualValue);
    }

    @Override
    protected boolean set(final ArmorStandEntity container, final Vector3d value) {
        container.setLeftArmRotation(VecHelper.toRotation(value));
        return true;
    }

    @Override
    protected Optional<Vector3d> getVal(final ArmorStandEntity container) {
        return Optional.of(VecHelper.toVector3d(((EntityArmorStandAccessor) container).accessor$getleftArmRotation()));
    }

    @Override
    protected Immutable<Vector3d> constructImmutableValue(final Vector3d value) {
        return constructValue(value).asImmutable();
    }

}
