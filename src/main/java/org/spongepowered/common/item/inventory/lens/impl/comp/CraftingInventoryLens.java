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
package org.spongepowered.common.item.inventory.lens.impl.comp;

import net.minecraft.item.ItemStack;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.common.item.inventory.adapter.InventoryAdapter;
import org.spongepowered.common.item.inventory.adapter.impl.comp.CraftingInventoryAdapter;
import org.spongepowered.common.item.inventory.fabric.Fabric;
import org.spongepowered.common.item.inventory.lens.impl.slot.SlotLensProvider;
import org.spongepowered.common.item.inventory.lens.impl.DefaultIndexedLens;
import org.spongepowered.common.item.inventory.lens.impl.RealLens;
import org.spongepowered.common.item.inventory.lens.impl.slot.CraftingOutputSlotLens;

public class CraftingInventoryLens extends RealLens {

    private final int outputSlotIndex;

    private final CraftingOutputSlotLens outputSlot;

    private final CraftingGridInventoryLens craftingGrid;


    public CraftingInventoryLens(int outputSlotIndex, int gridBase, int width, int height, SlotLensProvider slots) {
        this(outputSlotIndex, gridBase, width, height, CraftingInventoryAdapter.class, slots);
    }

    public CraftingInventoryLens(int outputSlotIndex, int gridBase, int width, int height, Class<? extends Inventory> adapterType, SlotLensProvider slots) {
        super(gridBase, width * height + 1, adapterType);
        this.outputSlotIndex = outputSlotIndex;
        this.outputSlot = (CraftingOutputSlotLens)slots.getSlotLens(this.outputSlotIndex);
        this.craftingGrid = new CraftingGridInventoryLens(this.base, width, height, slots);
        this.size += 1; // output slot
        // Avoid the init() method in the superclass calling our init() too early
        this.init(slots);
    }

    private void init(SlotLensProvider slots) {
        this.addSpanningChild(this.outputSlot);
        this.addSpanningChild(this.craftingGrid);
        this.addChild(new DefaultIndexedLens(0, this.size, slots));
    }

    public CraftingGridInventoryLens getCraftingGrid() {
        return this.craftingGrid;
    }

    public CraftingOutputSlotLens getOutputSlot() {
        return this.outputSlot;
    }

    public ItemStack getOutputStack(Fabric inv) {
        return this.outputSlot.getStack(inv);
    }

    public boolean setOutputStack(Fabric inv, ItemStack stack) {
        return this.outputSlot.setStack(inv, stack);
    }

    @Override
    public InventoryAdapter getAdapter(Fabric inv, Inventory parent) {
        return new CraftingInventoryAdapter(inv, this, parent);
    }

}
