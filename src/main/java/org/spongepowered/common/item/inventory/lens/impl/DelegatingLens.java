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
package org.spongepowered.common.item.inventory.lens.impl;

import net.minecraft.inventory.Slot;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.common.item.inventory.adapter.InventoryAdapter;
import org.spongepowered.common.item.inventory.adapter.impl.VanillaAdapter;
import org.spongepowered.common.item.inventory.lens.UniqueCustomSlotProvider;
import org.spongepowered.common.item.inventory.lens.Fabric;
import org.spongepowered.common.item.inventory.lens.Lens;
import org.spongepowered.common.item.inventory.lens.SlotProvider;

import java.util.List;

/**
 * A delegating Lens used in Containers. Provides ordered inventory access.
 */
@SuppressWarnings("rawtypes")
public class DelegatingLens extends AbstractLens {

    private Lens delegate;

    public DelegatingLens(int base, Lens lens, SlotProvider slots) {
        super(base, lens.slotCount(), VanillaAdapter.class);
        this.delegate = lens;
        this.init(slots);
    }

    protected void init(SlotProvider slots) {
        this.addSpanningChild(new DefaultIndexedLens(this.base, this.size, slots));
        this.addChild(delegate);
    }

    public DelegatingLens(int base, List<Slot> containerSlots, Lens lens, SlotProvider slots) {
        super(base, containerSlots.size(), VanillaAdapter.class);
        this.delegate = lens;
        UniqueCustomSlotProvider slotProvider = new UniqueCustomSlotProvider();
        for (Slot slot : containerSlots) {
            // Get slots from original slot provider and add them to custom slot provider in order of actual containerSlots.
            slotProvider.add(slots.getSlotLens(this.base + slot.slotIndex));
        }
        // Provide indexed access over the Container to the slots in the base inventory
        this.addSpanningChild(new DefaultIndexedLens(this.base, containerSlots.size(), slotProvider));
        this.addChild(delegate);
    }

    @SuppressWarnings("unchecked")
    @Override
    public InventoryAdapter getAdapter(Fabric inv, Inventory parent) {
        return new VanillaAdapter(inv, this, parent);
    }

}
