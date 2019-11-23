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

import static org.spongepowered.common.util.Constants.Catalog.DEFAULT_HAND;

import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.immutable.entity.ImmutableDominantHandData;
import org.spongepowered.api.data.manipulator.mutable.entity.DominantHandData;
import org.spongepowered.api.data.type.HandPreference;
import org.spongepowered.api.data.type.HandPreferences;
import org.spongepowered.api.data.value.Value.Immutable;
import org.spongepowered.api.data.value.Value.Mutable;
import org.spongepowered.api.data.value.ValueContainer;
import org.spongepowered.common.data.manipulator.mutable.entity.SpongeDominantHandData;
import org.spongepowered.common.data.processor.common.AbstractEntitySingleDataProcessor;
import org.spongepowered.common.data.value.immutable.ImmutableSpongeValue;
import org.spongepowered.common.data.value.mutable.SpongeValue;

import java.util.Optional;
import net.minecraft.entity.MobEntity;

public class DominantHandDataProcessor extends AbstractEntitySingleDataProcessor<MobEntity, HandPreference, Mutable<HandPreference>, DominantHandData, ImmutableDominantHandData> {

    public DominantHandDataProcessor() {
        super(MobEntity.class, Keys.DOMINANT_HAND);
    }

    @Override
    protected boolean set(MobEntity dataHolder, HandPreference value) {
        // What happens with custom EnumHandSide?
        dataHolder.setLeftHanded(value.equals(HandPreferences.LEFT));
        return true;
    }

    @Override
    protected Optional<HandPreference> getVal(MobEntity dataHolder) {
        return Optional.of((HandPreference) (Object) dataHolder.getPrimaryHand());
    }

    @Override
    protected Immutable<HandPreference> constructImmutableValue(HandPreference value) {
        return ImmutableSpongeValue.cachedOf(Keys.DOMINANT_HAND, DEFAULT_HAND, value);
    }

    @Override
    protected Mutable<HandPreference> constructValue(HandPreference actualValue) {
        return new SpongeValue<>(Keys.DOMINANT_HAND,DEFAULT_HAND, actualValue);
    }

    @Override
    protected DominantHandData createManipulator() {
        return new SpongeDominantHandData();
    }

    @Override
    public DataTransactionResult removeFrom(ValueContainer<?> container) {
        return DataTransactionResult.failNoData();
    }
}
