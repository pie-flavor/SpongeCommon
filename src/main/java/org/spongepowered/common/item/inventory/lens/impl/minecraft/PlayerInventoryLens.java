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
package org.spongepowered.common.item.inventory.lens.impl.minecraft;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.InventoryProperties;
import org.spongepowered.api.item.inventory.entity.PlayerInventory;
import org.spongepowered.api.item.inventory.equipment.EquipmentType;
import org.spongepowered.api.item.inventory.equipment.EquipmentTypes;
import org.spongepowered.api.item.inventory.type.CarriedInventory;
import org.spongepowered.common.item.inventory.PropertyEntry;
import org.spongepowered.common.item.inventory.adapter.InventoryAdapter;
import org.spongepowered.common.item.inventory.lens.Fabric;
import org.spongepowered.common.item.inventory.lens.SlotProvider;
import org.spongepowered.common.item.inventory.lens.comp.EquipmentInventoryLens;
import org.spongepowered.common.item.inventory.lens.comp.PrimaryPlayerInventoryLens;
import org.spongepowered.common.item.inventory.lens.impl.AbstractLens;
import org.spongepowered.common.item.inventory.lens.impl.comp.ArmorInventoryLensImpl;
import org.spongepowered.common.item.inventory.lens.impl.comp.EquipmentInventoryLensImpl;
import org.spongepowered.common.item.inventory.lens.impl.comp.HeldHandSlotLensImpl;
import org.spongepowered.common.item.inventory.lens.impl.comp.PrimaryPlayerInventoryLensImpl;
import org.spongepowered.common.item.inventory.lens.slots.SlotLens;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.inventory.container.Container;

public class PlayerInventoryLens extends AbstractLens {

    private static final int ARMOR = 4;
    private static final int OFFHAND = 1;

    private PrimaryPlayerInventoryLensImpl main;
    private EquipmentInventoryLensImpl equipment;
    private ArmorInventoryLensImpl armor;
    private SlotLens offhand;
    private final boolean isContainer;

    public PlayerInventoryLens(int size, Class<? extends Inventory> adapter, SlotProvider slots) {
        super(0, size, adapter);
        this.isContainer = false;
        this.init(slots);
    }

    /**
     * Constructor for ContainerPlayer Inventory
     *
     * @param base The base index
     * @param size The size
     * @param slots The slots
     */
    public PlayerInventoryLens(int base, int size, SlotProvider slots) {
        super(base, size, PlayerInventory.class);
        this.isContainer = true;
        this.init(slots);
    }

    protected void init(SlotProvider slots) {
        // Adding slots
        for (int ord = 0, slot = this.base; ord < this.size; ord++, slot++) {
            this.addChild(slots.getSlotLens(slot), PropertyEntry.slotIndex(ord));
        }

        int base = this.base;
        Map<EquipmentType, SlotLens> equipmentLenses = new LinkedHashMap<>();
        if (this.isContainer) {
            this.armor = new ArmorInventoryLensImpl(base, slots, true);
            equipmentLenses.put(EquipmentTypes.HEADWEAR, slots.getSlotLens(base + 0));
            equipmentLenses.put(EquipmentTypes.CHESTPLATE, slots.getSlotLens(base + 1));
            equipmentLenses.put(EquipmentTypes.LEGGINGS, slots.getSlotLens(base + 2));
            equipmentLenses.put(EquipmentTypes.BOOTS, slots.getSlotLens(base + 3));
            base += ARMOR; // 4
            this.main = new PrimaryPlayerInventoryLensImpl(base, slots, true);
            base += this.main.slotCount();
            this.offhand = slots.getSlotLens(base);

            base += OFFHAND;
            equipmentLenses.put(EquipmentTypes.OFF_HAND, this.offhand);
        } else {
            this.main = new PrimaryPlayerInventoryLensImpl(base, slots, false);
            base += this.main.slotCount();
            this.armor = new ArmorInventoryLensImpl(base, slots, false);

            equipmentLenses.put(EquipmentTypes.BOOTS, slots.getSlotLens(base + 0));
            equipmentLenses.put(EquipmentTypes.LEGGINGS, slots.getSlotLens(base + 1));
            equipmentLenses.put(EquipmentTypes.CHESTPLATE, slots.getSlotLens(base + 2));
            equipmentLenses.put(EquipmentTypes.HEADWEAR, slots.getSlotLens(base + 3));

            base += ARMOR;
            this.offhand = slots.getSlotLens(base);

            base += OFFHAND;
            equipmentLenses.put(EquipmentTypes.OFF_HAND, this.offhand);
        }

        equipmentLenses.put(EquipmentTypes.MAIN_HAND, new HeldHandSlotLensImpl());
        this.equipment = new EquipmentInventoryLensImpl(equipmentLenses);

        this.addSpanningChild(this.main);
        this.addSpanningChild(this.armor);
        this.addSpanningChild(this.offhand);

        for (Map.Entry<EquipmentType, SlotLens> entry : equipmentLenses.entrySet()) {
            this.addChild(entry.getValue(), PropertyEntry.of(InventoryProperties.EQUIPMENT_TYPE, entry.getKey()));
        }
        this.addChild(this.equipment);
        this.addMissingSpanningSlots(base, slots);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public InventoryAdapter getAdapter(Fabric fabric, Inventory parent) {
        if (this.isContainer && fabric instanceof Container) {
            // If Lens is for Container extract the PlayerInventory
            Container container = (Container) fabric;
            Optional carrier = ((CarriedInventory) container).getCarrier();
            if (carrier.isPresent() && carrier.get() instanceof Player) {
                return ((InventoryAdapter) ((Player) carrier.get()).getInventory());
            }
        }
        return fabric.fabric$get(this.base).bridge$getAdapter();
    }

    public PrimaryPlayerInventoryLens getMainLens() {
        return this.main;
    }

    public EquipmentInventoryLens getEquipmentLens() {
        return this.equipment;
    }

    public SlotLens getOffhandLens() {
        return this.offhand;
    }

    public EquipmentInventoryLens getArmorLens() {
        return this.armor;
    }
}
