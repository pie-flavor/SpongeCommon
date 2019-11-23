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
package org.spongepowered.common.data.manipulator.immutable.common;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.manipulator.immutable.ImmutableMappedData;
import org.spongepowered.api.data.manipulator.mutable.MappedData;
import org.spongepowered.api.data.value.MapValue.Immutable;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.common.data.value.immutable.ImmutableSpongeMapValue;
import org.spongepowered.common.util.ReflectionUtil;

import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public abstract class AbstractImmutableMappedData<K, V, I extends ImmutableMappedData<K, V, I, M>, M extends MappedData<K, V, M, I>>
    extends AbstractImmutableSingleData<Map<K, V>, I, M> implements ImmutableMappedData<K, V, I, M> {

    private final Class<? extends M> mutableClass;
    private final Immutable<K, V> mapValue;

    public AbstractImmutableMappedData(Class<I> immutableClass, Map<K, V> value,
                                          Key<? extends Value<Map<K, V>>> usedKey,
                                          Class<? extends M> manipulatorClass) {
        super(immutableClass, ImmutableMap.copyOf(value), usedKey);
        checkArgument(!Modifier.isAbstract(manipulatorClass.getModifiers()), "The immutable class cannot be abstract!");
        checkArgument(!Modifier.isInterface(manipulatorClass.getModifiers()), "The immutable class cannot be an interface!");
        this.mutableClass = manipulatorClass;
        this.mapValue = new ImmutableSpongeMapValue<>(this.usedKey, ImmutableMap.copyOf(this.value));
    }

    @Override
    protected final Immutable<K, V> getValueGetter() {
        return this.mapValue;
    }

    @Override
    public Map<K, V> getValue() {
        return ImmutableMap.copyOf(super.getValue());
    }

    @Override
    public M asMutable() {
        return ReflectionUtil.createInstance(this.mutableClass, this.value);
    }

    @Override
    public Optional<V> get(K key) {
        return Optional.ofNullable(super.getValue().get(checkNotNull(key, "Key cannot be null!")));
    }

    @Override
    public Set<K> getMapKeys() {
        return super.getValue().keySet();
    }

    @Override
    public Immutable<K, V> getMapValue() {
        return this.mapValue;
    }
}
