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
import org.spongepowered.api.data.value.OptionalValue.Mutable;
import org.spongepowered.api.data.value.Value.Immutable;
import org.spongepowered.api.data.value.ValueContainer;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntitySnapshot;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.common.data.processor.common.AbstractSpongeValueProcessor;
import org.spongepowered.common.data.value.SpongeValueFactory;
import org.spongepowered.common.mixin.core.entity.EntityLivingBaseAccessor;

import java.util.Optional;

import javax.annotation.Nullable;
import net.minecraft.entity.LivingEntity;

public class LastAttackerValueProcessor
        extends AbstractSpongeValueProcessor<LivingEntity, Optional<EntitySnapshot>, Mutable<EntitySnapshot>> {

    public LastAttackerValueProcessor() {
        super(LivingEntity.class, Keys.LAST_ATTACKER);
    }

    @Override
    protected Mutable<EntitySnapshot> constructValue(Optional<EntitySnapshot> actualValue) {
        return SpongeValueFactory.getInstance().createOptionalValue(Keys.LAST_ATTACKER, actualValue.orElse(null));
    }

    @Override
    protected boolean set(LivingEntity dataHolder, @Nullable Optional<EntitySnapshot> lastAttacker) {
        if (lastAttacker == null || !lastAttacker.isPresent()) {
            dataHolder.setRevengeTarget(null);
            return true;
        }
        if (lastAttacker.get().getUniqueId().isPresent()) {
            final Optional<Entity> optionalEntity = lastAttacker.get().restore();
            if (optionalEntity.isPresent()) {
                final Entity entity = optionalEntity.get();
                if (entity.isLoaded() && entity instanceof LivingEntity) {
                    dataHolder.setRevengeTarget((LivingEntity) entity);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected Optional<Optional<EntitySnapshot>> getVal(LivingEntity container) {
        final LivingEntity entityLivingBase = ((EntityLivingBaseAccessor) container).accessor$getRevengeTarget();
        return Optional.of(Optional.ofNullable(entityLivingBase == null ? null : ((Living) entityLivingBase).createSnapshot()));
    }

    @Override
    protected Immutable<Optional<EntitySnapshot>> constructImmutableValue(Optional<EntitySnapshot> value) {
        return constructValue(value).asImmutable();
    }

    @Override
    public DataTransactionResult removeFrom(ValueContainer<?> container) {
        return DataTransactionResult.failNoData();
    }
}
