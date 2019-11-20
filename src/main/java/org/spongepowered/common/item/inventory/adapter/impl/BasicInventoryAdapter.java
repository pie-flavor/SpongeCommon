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
package org.spongepowered.common.item.inventory.adapter.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import net.minecraft.item.ItemStack;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.common.bridge.inventory.InventoryBridge;
import org.spongepowered.common.item.inventory.adapter.InventoryAdapter;
import org.spongepowered.common.item.inventory.lens.Fabric;
import org.spongepowered.common.item.inventory.lens.Lens;
import org.spongepowered.common.bridge.inventory.LensProviderBridge;
import org.spongepowered.common.item.inventory.lens.SlotProvider;
import org.spongepowered.common.item.inventory.lens.impl.DefaultEmptyLens;
import org.spongepowered.common.item.inventory.lens.impl.DefaultIndexedLens;
import org.spongepowered.common.item.inventory.lens.impl.QueryLens;
import org.spongepowered.common.item.inventory.lens.impl.ReusableLens;
import org.spongepowered.common.item.inventory.lens.impl.collections.SlotLensCollection;
import org.spongepowered.common.item.inventory.lens.slots.SlotLens;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

/**
 * Base Adapter implementation for {@link ItemStack} based Inventories.
 */
public class BasicInventoryAdapter implements InventoryAdapter, DefaultImplementedAdapterInventory, InventoryBridge, Inventory {

    private final Fabric inventory;
    protected final SlotProvider slots;
    protected final Lens lens;
    @Nullable private SlotCollection slotCollection;

    @Nullable
    protected List<Inventory> children;

    protected Inventory parent;

    public BasicInventoryAdapter(final Fabric inventory) {
        this(inventory, (Lens) null, null);
    }

    @SuppressWarnings("unchecked")
    public <T extends Lens> BasicInventoryAdapter(final Fabric inventory, final Class<T> lensType) {
        this.inventory = inventory;
        this.parent = this;
        if (inventory.fabric$getSize() == 0) {
            this.slots = new SlotLensCollection(0);
            this.lens = new DefaultEmptyLens(this);
        } else {
            final ReusableLens<T> lens = ReusableLens.getLens(lensType, this, () -> this.initSlots(inventory, this.parent),
                    (slots) -> (T) new DefaultIndexedLens(0, inventory.fabric$getSize(), slots));
            this.slots = lens.getSlots();
            this.lens = lens.getLens();
        }
    }

    public BasicInventoryAdapter(final Fabric inventory, @Nullable final Lens root, @Nullable final Inventory parent) {
        this.inventory = inventory;
        this.parent = parent == null ? this : parent;
        this.slots = this.initSlots(inventory, parent);
        this.lens = root != null ? root : checkNotNull(this.initRootLens(), "root lens");
    }

    // Constructs inventory with given list of inventories
    // TODO check if this is correct
    public BasicInventoryAdapter(Fabric inventory, List<Inventory> children, Inventory parent) {
        this.inventory = inventory;
        this.parent = parent == null ? this : parent;
        this.slots = this.initSlots(inventory, parent);

        this.lens = new QueryLens(
                children.stream()
                        .map(InventoryBridge.class::cast)
                        .map(InventoryBridge::bridge$getAdapter)
                        .map(InventoryAdapter::bridge$getRootLens).collect(Collectors.toList()));
        this.children = children; // Init cached children
    }

    private SlotProvider initSlots(final Fabric inventory, @Nullable final Inventory parent) {
        if (parent instanceof InventoryAdapter) {
            return ((InventoryAdapter) parent).bridge$getSlotProvider();
        }
        return new SlotLensCollection(inventory.fabric$getSize());
    }

    @Override
    public Inventory parent() {
        return this.parent;
    }

    protected Lens initRootLens() {
        if (this instanceof LensProviderBridge) {
            return ((LensProviderBridge) this).bridge$rootLens(this.inventory, this);
        }
        final int size = this.inventory.fabric$getSize();
        if (size == 0) {
            return new DefaultEmptyLens(this);
        }
        return new DefaultIndexedLens(0, size, this.slots);
    }

    @Override
    public SlotProvider bridge$getSlotProvider() {
        return this.slots;
    }

    @Override
    public Lens bridge$getRootLens() {
        return this.lens;
    }

    @Override
    public Fabric bridge$getFabric() {
        return this.inventory;
    }

    @Override
    public List<Slot> slots() {
        // slotlenscollection
        if (this.slotCollection == null) {
            this.slotCollection = new SlotCollection(this, this.bridge$getFabric(), this.impl$getLens(), this.slots);
        }
        return this.slotCollection.slots();
    }

    @Override
    public List<Inventory> children() {
        // TODO react to lens changes?
        if (this.children == null) {
            this.children = this.impl$generateChildren();
        }
        return this.children;
    }

    public static Optional<Slot> forSlot(final Fabric inv, final SlotLens slotLens, final Inventory parent) {
        return slotLens == null ? Optional.empty() : Optional.ofNullable((Slot) slotLens.getAdapter(inv, parent));
    }

    @Override
    public void clear() {
        // TODO clear without generating SlotAdapters
        this.slots().forEach(Inventory::clear);
    }

}
