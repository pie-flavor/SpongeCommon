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
package org.spongepowered.common.inventory.lens.impl;

import org.spongepowered.api.data.property.Property;
import org.spongepowered.common.inventory.PropertyEntry;
import org.spongepowered.common.inventory.lens.Lens;
import org.spongepowered.common.inventory.lens.LensCollection;
import org.spongepowered.common.inventory.lens.impl.struct.LensHandle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MutableLensCollection implements LensCollection {

    private final List<LensHandle> lenses = new ArrayList<>();
    private final Map<Lens, LensHandle> handleMap = new HashMap<>();

    @Override
    public Lens getLens(int index) {
        return this.lenses.get(index).lens;
    }

    @Override
    public Map<Property<?>, Object> getProperties(int index) {
        if (index >= this.lenses.size()) {
            throw new IndexOutOfBoundsException("Index: " + index);
        }
        return this.lenses.get(index).getProperties();
    }

    @Override
    public Map<Property<?>, Object> getProperties(Lens child) {
        if (!this.has(child)) {
            return Collections.emptyMap();
        }
        return this.handleMap.get(child).getProperties();
    }

    @Override
    public boolean has(Lens lens) {
        return this.handleMap.containsKey(lens);
    }

    @Override
    public List<Lens> children() {
        return this.lenses.stream().map(h -> h.lens).collect(Collectors.toList());
    }

    @Override
    public boolean isSubsetOf(Collection<Lens> c) {
        return c.containsAll(this.children());
    }

    public void add(Lens lens, PropertyEntry... properties) {
        LensHandle handle = this.handleMap.get(lens);
        if (handle == null) {
            handle = new LensHandle(lens, properties);
            this.lenses.add(handle);
            this.handleMap.put(lens, handle);
        } else {
            for (PropertyEntry property : properties) {
                handle.setProperty(property);
            }
        }
    }
}
