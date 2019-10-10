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

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.world.IInteractionObject;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.ContainerType;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.common.data.type.SpongeContainerType;
import org.spongepowered.common.item.inventory.lens.Lens;
import org.spongepowered.common.item.inventory.lens.SlotProvider;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

public class ViewableCustomInventory extends CustomInventory implements IInteractionObject {

    private ContainerType type;
    private boolean vanilla = false;

    private Set<EntityPlayer> viewers = new HashSet<>();

    public ViewableCustomInventory(ContainerType type, int size, Lens lens, SlotProvider provider, List<Inventory> inventories, @Nullable UUID identity, @Nullable Carrier carrier) {
        super(size, lens, provider, inventories, identity, carrier);
        this.type = type;
    }

    public ViewableCustomInventory vanilla() {
        this.vanilla = true;
        return this;
    }

    @Override
    public void openInventory(EntityPlayer player) {
        viewers.add(player); // TODO check if this is always called
    }

    @Override
    public void closeInventory(EntityPlayer player) {
        viewers.remove(player);  // TODO check if this is always called
    }

    // TODO implement fields as properties?

    @Override
    public int getField(int id) {
        return 0;
    }

    @Override
    public void setField(int id, int value) {
    }

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public Container createContainer(InventoryPlayer inventoryPlayer, EntityPlayer player) {
        if (this.vanilla) {
            return ((SpongeContainerType) this.type).provideContainer(this, player);
        }
        return new CustomContainer(player, this);
    }

    @Override
    public String getGuiID() {
        return this.type.getKey().toString();
    }
}