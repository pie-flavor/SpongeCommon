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
import org.spongepowered.api.data.manipulator.immutable.entity.ImmutablePickupRuleData;
import org.spongepowered.api.data.manipulator.mutable.entity.PickupRuleData;
import org.spongepowered.api.data.type.PickupRule;
import org.spongepowered.api.data.value.Value.Immutable;
import org.spongepowered.api.data.value.Value.Mutable;
import org.spongepowered.api.data.value.ValueContainer;
import org.spongepowered.common.data.ImmutableDataCachingUtil;
import org.spongepowered.common.data.manipulator.mutable.entity.SpongePickupRuleData;
import org.spongepowered.common.data.processor.common.AbstractEntitySingleDataProcessor;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.data.value.immutable.ImmutableSpongeValue;
import org.spongepowered.common.data.value.mutable.SpongeValue;

import java.util.Optional;
import net.minecraft.entity.projectile.AbstractArrowEntity;

public final class PickupRuleDataProcessor extends AbstractEntitySingleDataProcessor<AbstractArrowEntity, PickupRule, Mutable<PickupRule>,
        PickupRuleData, ImmutablePickupRuleData> {

    public PickupRuleDataProcessor() {
        super(AbstractArrowEntity.class, Keys.PICKUP_RULE);
    }

    @Override
    protected boolean set(AbstractArrowEntity arrow, PickupRule value) {
        arrow.pickupStatus = (AbstractArrowEntity.PickupStatus) (Object) value;
        return true;
    }

    @Override
    protected Optional<PickupRule> getVal(AbstractArrowEntity arrow) {
        return Optional.of((PickupRule) (Object) arrow.pickupStatus);
    }

    @Override
    protected Immutable<PickupRule> constructImmutableValue(PickupRule value) {
        return ImmutableDataCachingUtil.getValue(ImmutableSpongeValue.class, this.key, value, Constants.Catalog.DEFAULT_PICKUP_RULE);
    }

    @Override
    protected Mutable<PickupRule> constructValue(PickupRule actualValue) {
        return new SpongeValue<>(this.key, Constants.Catalog.DEFAULT_PICKUP_RULE, actualValue);
    }

    @Override
    public DataTransactionResult removeFrom(ValueContainer<?> container) {
        return DataTransactionResult.failNoData();
    }

    @Override
    protected PickupRuleData createManipulator() {
        return new SpongePickupRuleData();
    }

}
