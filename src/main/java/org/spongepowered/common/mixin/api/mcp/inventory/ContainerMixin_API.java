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
package org.spongepowered.common.mixin.api.mcp.inventory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.IContainerListener;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.type.CarriedInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.bridge.inventory.ContainerBridge;
import org.spongepowered.common.item.inventory.util.InventoryUtil;
import org.spongepowered.common.item.inventory.util.ItemStackUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Mixin(value = Container.class, priority = 998)
public abstract class ContainerMixin_API implements org.spongepowered.api.item.inventory.Container, CarriedInventory<Carrier> {

    @Shadow public List<Slot> inventorySlots;
    @Shadow protected List<IContainerListener> listeners;
    @Shadow public abstract NonNullList<ItemStack> getInventory();

    @Override
    public Optional<Carrier> getCarrier() {
        return ((ContainerBridge) this).bridge$getCarrier();
    }

    @Override
    public boolean isViewedSlot(final org.spongepowered.api.item.inventory.Slot slot) {
        if (slot instanceof Slot) {
            final Set<Slot> set = ((ContainerBridge) this).bridge$getInventories().get(((Slot) slot).field_75224_c);
            if (set != null) {
                if (set.contains(slot)) {
                    if (((ContainerBridge) this).bridge$getInventories().size() == 1) {
                        return true;
                    }
                    // TODO better detection of viewer inventory - needs tracking of who views a container
                    // For now assume that a player inventory is always the viewers inventory
                    if (((Slot) slot).field_75224_c.getClass() != PlayerInventory.class) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public List<Inventory> getViewed() {
        List<Inventory> list = new ArrayList<>();
        for (IInventory inv : ((ContainerBridge) this).bridge$getInventories().keySet()) {
            Inventory inventory = InventoryUtil.toInventory(inv, null);
            list.add(inventory);
        }
        return list;
    }

    @Override
    public boolean setCursor(org.spongepowered.api.item.inventory.ItemStack item) {
        if (!this.isOpen()) {
            return false;
        }
        ItemStack nativeStack = ItemStackUtil.toNative(item);
        this.listeners().stream().findFirst()
                .ifPresent(p -> p.inventory.setItemStack(nativeStack));
        return true;
    }

    @Override
    public Optional<org.spongepowered.api.item.inventory.ItemStack> getCursor() {
        return this.listeners().stream().findFirst()
                .map(p -> p.inventory.getItemStack())
                .map(ItemStackUtil::fromNative);
    }

    @Override
    public Player getViewer() {
        return this.listeners().stream().filter(Player.class::isInstance).map(Player.class::cast).findFirst().orElseThrow(() -> new IllegalStateException("Container without viewer"));
    }

    @Override
    public boolean isOpen() {
        org.spongepowered.api.item.inventory.Container thisContainer = this;
        return this.getViewer().getOpenInventory().map(c -> c == thisContainer).orElse(false);
    }

    public List<ServerPlayerEntity> listeners() {
        return this.listeners.stream()
                .filter(ServerPlayerEntity.class::isInstance)
                .map(ServerPlayerEntity.class::cast)
                .collect(Collectors.toList());
    }



    @Inject(method = "onContainerClosed", at = @At(value = "HEAD"))
    private void onOnContainerClosed(PlayerEntity player, CallbackInfo ci) {
        ((ContainerBridge) this).setViewed(null);
    }

}
