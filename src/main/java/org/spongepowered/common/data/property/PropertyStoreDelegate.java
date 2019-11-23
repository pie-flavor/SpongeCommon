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
package org.spongepowered.common.data.property;

import com.google.common.collect.ImmutableList;
import org.spongepowered.api.data.Property;
import org.spongepowered.api.data.property.PropertyHolder;
import org.spongepowered.api.data.property.provider.PropertyProvider;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;

public class PropertyStoreDelegate<T extends Property<?, ?>> implements PropertyProvider<T> {

    private final ImmutableList<PropertyProvider<T>> propertyStores;

    public PropertyStoreDelegate(ImmutableList<PropertyProvider<T>> propertyStores) {
        this.propertyStores = propertyStores;
    }

    @Override
    public Optional<T> getFor(PropertyHolder propertyHolder) {
        for (PropertyProvider<T> propertyStore : this.propertyStores) {
            final Optional<T> optional = propertyStore.getFor(propertyHolder);
            if (optional.isPresent()) {
                return optional;
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<T> getFor(Location<World> location) {
        for (PropertyProvider<T> propertyStore : this.propertyStores) {
            final Optional<T> optional = propertyStore.getFor(location);
            if (optional.isPresent()) {
                return optional;
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<T> getFor(Location<World> location, Direction direction) {
        for (PropertyProvider<T> propertyStore : this.propertyStores) {
            final Optional<T> optional = propertyStore.getFor(location, direction);
            if (optional.isPresent()) {
                return optional;
            }
        }
        return Optional.empty();
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE;
    }
}
