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
import org.spongepowered.api.data.manipulator.immutable.entity.ImmutableTameableData;
import org.spongepowered.api.data.manipulator.mutable.entity.TameableData;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.value.OptionalValue.Mutable;
import org.spongepowered.api.data.value.Value.Immutable;
import org.spongepowered.api.data.value.ValueContainer;
import org.spongepowered.common.data.manipulator.mutable.entity.SpongeTameableData;
import org.spongepowered.common.data.processor.common.AbstractEntitySingleDataProcessor;
import org.spongepowered.common.data.value.immutable.ImmutableSpongeValue;
import org.spongepowered.common.data.value.mutable.SpongeOptionalValue;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;

public class HorseTameableDataProcessor
        extends AbstractEntitySingleDataProcessor<AbstractHorseEntity, Optional<UUID>, Mutable<UUID>, TameableData, ImmutableTameableData> {

    public HorseTameableDataProcessor() {
        super(AbstractHorseEntity.class, Keys.TAMED_OWNER);
    }

    @Override
    protected Optional<Optional<UUID>> getVal(AbstractHorseEntity tameable) {
        return Optional.of(Optional.ofNullable(tameable.getOwnerUniqueId()));
    }

    @Override
    public Optional<TameableData> fill(final DataContainer container, final TameableData tameableData) {
        if (!container.contains(Keys.TAMED_OWNER.getQuery())) {
            return Optional.empty();
        }
        final String uuid = container.getString(Keys.TAMED_OWNER.getQuery()).get();
        if (uuid.equals("none")) {
            return Optional.of(tameableData);
        }
        final UUID ownerUUID = UUID.fromString(uuid);
        return Optional.of(tameableData.set(Keys.TAMED_OWNER, Optional.of(ownerUUID)));
    }

    @Override
    protected boolean set(AbstractHorseEntity horse, Optional<UUID> uuidOptional) {
        horse.setOwnerUniqueId(uuidOptional.orElse(null));
        horse.setHorseTamed(uuidOptional.isPresent());
        return true;
    }

    @Override
    public DataTransactionResult removeFrom(ValueContainer<?> container) {
        return DataTransactionResult.failNoData();
    }

    @Override
    protected Mutable<UUID> constructValue(Optional<UUID> defaultValue) {
        return new SpongeOptionalValue<>(this.getKey(), defaultValue);
    }

    @Override
    protected Immutable<Optional<UUID>> constructImmutableValue(Optional<UUID> value) {
        return new ImmutableSpongeValue<>(Keys.TAMED_OWNER, Optional.empty(), value);
    }

    @Override
    protected TameableData createManipulator() {
        return new SpongeTameableData();
    }

}
