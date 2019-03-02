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
package org.spongepowered.common.item.inventory.custom;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.IInteractionObject;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.property.Property;
import org.spongepowered.api.event.item.inventory.container.InteractContainerEvent;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.ContainerType;
import org.spongepowered.api.item.inventory.ContainerTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.InventoryArchetype;
import org.spongepowered.api.item.inventory.InventoryProperty;
import org.spongepowered.api.item.inventory.gui.ContainerType;
import org.spongepowered.api.item.inventory.gui.ContainerTypes;
import org.spongepowered.api.item.inventory.property.AbstractInventoryProperty;
import org.spongepowered.api.item.inventory.property.GuiId;
import org.spongepowered.api.item.inventory.property.GuiIdProperty;
import org.spongepowered.api.item.inventory.property.GuiIds;
import org.spongepowered.api.item.inventory.property.InventoryCapacity;
import org.spongepowered.api.item.inventory.property.InventoryDimension;
import org.spongepowered.api.item.inventory.property.InventoryTitle;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.TranslatableText;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.data.type.SpongeContainerType;
import org.spongepowered.common.item.inventory.archetype.CompositeInventoryArchetype;
import org.spongepowered.common.item.inventory.lens.Fabric;
import org.spongepowered.common.item.inventory.lens.Lens;
import org.spongepowered.common.item.inventory.lens.SlotProvider;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class CustomInventory implements IInventory, IInteractionObject {

    private final List<Inventory> inventories;
    private SlotProvider slots;
    private Lens lens;
    private Fabric fabric;
    private Map<Property<?>, Object> properties = new HashMap<>();
    private int size;
    private Carrier carrier;
    private final PluginContainer plugin;


    public static final String INVENTORY_CAPACITY = InventoryCapacity.class.getSimpleName().toLowerCase(Locale.ENGLISH);
    public static final String INVENTORY_DIMENSION = InventoryDimension.class.getSimpleName().toLowerCase(Locale.ENGLISH);
    public static final String TITLE = InventoryTitle.class.getSimpleName().toLowerCase(Locale.ENGLISH);
    private final Map<Class<? extends InteractContainerEvent>, List<Consumer<? extends InteractContainerEvent>>> listeners;

    private net.minecraft.inventory.Inventory inv;
    protected InventoryArchetype archetype;

    private Set<PlayerEntity> viewers = new HashSet<>();
    private boolean registered;
    private ITextComponent customInventory = new TextComponentString("Custom Inventory");

    private void doRegistration() {
        for (final Map.Entry<Class<? extends InteractContainerEvent>, List<Consumer<? extends InteractContainerEvent>>> entry: this.listeners.entrySet()) {
            Sponge.getEventManager().registerListener(this.plugin, entry.getKey(), new CustomInventoryListener((Inventory) this, entry.getValue()));
        }
        this.registered = true;
    }


    public CustomInventory(int size, Lens lens, SlotProvider provider, List<Inventory> inventories, @Nullable UUID identity, @Nullable Carrier carrier) {
        this.size = size;
        this.properties.put(InventoryProperties.UNIQUE_ID, identity);
        this.carrier = carrier;
        this.lens = lens;
        this.slots = provider;
        this.inventories = inventories;
        this.plugin = SpongeImplHooks.getActiveModContainer();
    }

    // IInventory delegation


    @Nullable
    @Override
    public ITextComponent getCustomName()
    {
        return this.customInventory
    }

    @Override
    public String getName() {
        return this.customInventory;
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public int getSizeInventory() {
        return this.size;
    }

    @Override
    public boolean isEmpty() {
        return this.size != 0;
    }

    @Override
    public ItemStack getStackInSlot(final int index) {
        int offset = 0;
        for (Inventory inv : this.inventories) {
            if (inv.capacity() > index - offset) {
                offset += inv.capacity();
                continue;
            }
            return inv.peek(SlotIndex.of(index - offset)).map(ItemStackUtil::toNative).orElse(ItemStack.EMPTY);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack decrStackSize(final int index, final int count) {
        int offset = 0;
        for (Inventory inv : this.inventories) {
            if (inv.capacity() > index - offset) {
                offset += inv.capacity();
                continue;
            }
            return inv.poll(SlotIndex.of(index - offset), count).map(ItemStackUtil::toNative).orElse(ItemStack.EMPTY);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStackFromSlot(final int index) {
        int offset = 0;
        for (Inventory inv : this.inventories) {
            if (inv.capacity() > index - offset) {
                offset += inv.capacity();
                continue;
            }
            return inv.poll(SlotIndex.of(index - offset)).map(ItemStackUtil::toNative).orElse(ItemStack.EMPTY);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setInventorySlotContents(final int index, final ItemStack stack) {
        int offset = 0;
        for (Inventory inv : this.inventories) {
            if (inv.capacity() > index - offset) {
                offset += inv.capacity();
                continue;
            }
            inv.set(SlotIndex.of(index - offset), ItemStackUtil.fromNative(stack));
        }
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public void markDirty() {
        for (Inventory inventory : this.inventories) {
            if (inventory instanceof IInventory) {
                ((IInventory) inventory).markDirty();
            }
        }
    }

    @Override
    public boolean isUsableByPlayer(final PlayerEntity player) {
        return true;
    }

    @Override
    public void openInventory(final PlayerEntity player) {
        // ? this.ensureListenersRegistered();
    }

    @Override
    public void closeInventory(final PlayerEntity player) {
/* ?
        if (this.viewers.isEmpty()) {
            Task.builder().execute(() -> {
                if (this.viewers.isEmpty()) {
                    Sponge.getEventManager().unregisterListeners(this);
                    this.registered = false;
                }
            }).delayTicks(1).submit(SpongeImpl.getPlugin());
        }

 */
    }

    @Override
    public boolean isItemValidForSlot(final int index, final ItemStack stack) {
        return true;
    }

    @Override
    public int getField(final int id) {
        return 0;
    }

    @Override
    public void setField(final int id, final int value) {
    }

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public void clear() {
        for (Inventory inventory : this.inventories) {
            inventory.clear();
        }
    }

    public Map<Property<?>, ?> getProperties() {
        return this.properties;
    }

    public Carrier getCarrier() {
        return this.carrier;
    }

    public void ensureListenersRegistered() {
        if (!this.registered) {
            this.doRegistration();
        }
    }
}
