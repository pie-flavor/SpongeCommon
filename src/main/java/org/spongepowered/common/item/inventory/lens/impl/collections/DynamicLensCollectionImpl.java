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
package org.spongepowered.common.item.inventory.lens.impl.collections;

import org.spongepowered.api.data.property.Property;
import org.spongepowered.common.item.inventory.PropertyEntry;
import org.spongepowered.common.item.inventory.lens.DynamicLensCollection;
import org.spongepowered.common.item.inventory.lens.Lens;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DynamicLensCollectionImpl extends AbstractList<Lens> implements DynamicLensCollection {

    protected final Lens[] lenses;
    
    protected final Map<Property<?>, Object>[] properties;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public DynamicLensCollectionImpl(int size) {
        this.lenses = new Lens[size];
        this.properties = new Map[size];
    }

    @Override
    public void setProperty(Lens lens, PropertyEntry property) {
        this.setProperty(this.indexOf(lens), property);
    }

    @Override
    public void setProperty(int index, PropertyEntry property) {
        this.checkIndex(index);
        if (this.properties[index] == null) {
            this.properties[index] = new HashMap<>();
        } else {
            this.properties[index].remove(property.getProperty());
        }
        this.properties[index].put(property.getProperty(), property.getValue());
    }
    
    @Override
    public void removeProperty(Lens lens, Property<?> property) {
        this.removeProperty(this.indexOf(lens), property);
    }
    
    @Override
    public void removeProperty(int index, Property<?> property) {
        this.checkIndex(index);
        if (this.properties[index] != null) {
            this.properties[index].remove(property);
        }
    }

    @Override
    public Lens get(int index) {
        return this.getLens(index);
    }
    
    @Override
    public Lens getLens(int index) {
        this.checkIndex(index);
        return this.lenses[index];
    }

    @Override
    public int size() {
        return this.lenses.length;
    }
    
    @Override
    public boolean contains(Object o) {
        return this.indexOf(o) >= 0;
    }

    @Override
    public int indexOf(Object o) {
        if (o == null) {
            for (int i = 0; i < this.lenses.length; i++) {
                if (this.lenses[i] == null) {
                    return i;
                }
            }
        } else {
            for (int i = 0; i < this.lenses.length; i++) {
                if (o.equals(this.lenses[i])) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Override
    public Map<Property<?>, Object> getProperties(int index) {
        this.checkIndex(index);
        if (this.properties[index] == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(this.properties[index]);
    }
    
    @Override
    public Map<Property<?>, Object> getProperties(Lens child) {
        int index = this.indexOf(child);
        if (index < 0) {
            return Collections.emptyMap();
        }
        return this.getProperties(index);
    }

    @Override
    public boolean has(Lens lens) {
        return this.contains(lens);
    }

    @Override
    public boolean isSubsetOf(Collection<Lens> c) {
        return c.containsAll(this);
    }

    private void checkIndex(int index) {
        if (index >= this.lenses.length) {
            throw new IndexOutOfBoundsException("Index: " + index);
        }
    }

}
