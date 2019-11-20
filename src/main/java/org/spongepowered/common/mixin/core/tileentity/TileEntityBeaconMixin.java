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
package org.spongepowered.common.mixin.core.tileentity;

import net.minecraft.item.ItemStack;
import net.minecraft.potion.Effect;
import net.minecraft.tileentity.BeaconTileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.common.bridge.tileentity.TileEntityBeaconBridge;
import org.spongepowered.common.bridge.data.CustomNameableBridge;
import org.spongepowered.common.item.inventory.adapter.InventoryAdapter;
import org.spongepowered.common.item.inventory.adapter.impl.slots.InputSlotAdapter;
import org.spongepowered.common.item.inventory.fabric.Fabric;
import org.spongepowered.common.item.inventory.lens.impl.slot.SlotLensProvider;
import org.spongepowered.common.item.inventory.lens.impl.ReusableLens;
import org.spongepowered.common.item.inventory.lens.impl.slot.SlotLensCollection;
import org.spongepowered.common.item.inventory.lens.impl.slot.InputSlotLens;

import javax.annotation.Nullable;

@Mixin(BeaconTileEntity.class)
public abstract class TileEntityBeaconMixin extends TileEntityLockableMixin implements CustomNameableBridge, TileEntityBeaconBridge {

    @Shadow private Effect primaryEffect;
    @Shadow private Effect secondaryEffect;
    @Shadow private String customName;
    @Shadow public abstract boolean isItemValidForSlot(int index, ItemStack stack);

    @SuppressWarnings({"rawtypes"})
    @Override
    public ReusableLens<?> bridge$generateReusableLens(final Fabric fabric, final InventoryAdapter adapter) {
        return ReusableLens.getLens(InputSlotLens.class, this, this::impl$generateBeaconSlotProvider, this::impl$generateBeaconRootLens);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private SlotLensProvider impl$generateBeaconSlotProvider() {
        final InputSlotLens lens = new InputSlotLens(0, ((Class) BeaconTileEntity.class), itemStack -> isItemValidForSlot(0, (ItemStack) itemStack),
                itemType -> isItemValidForSlot(0, (ItemStack) org.spongepowered.api.item.inventory.ItemStack.of(itemType, 1)));
        return new SlotLensCollection.Builder()
                .add(InputSlotAdapter.class, i -> lens)
                .build();
    }

    @SuppressWarnings({"rawtypes"})
    private InputSlotLens impl$generateBeaconRootLens(final SlotLensProvider slots) {
        return ((InputSlotLens) slots.getSlotLens(0));
    }

    /**
     * @author gabizou - June 11th, 2019 - 1.12.2
     * @reason We want to avoid validating the potion type and just set the
     * potion anyways. This allows for plugins to set custom potions on beacons
     * that will persist.
     *
     * @param id The id
     * @return The potion by id, no validation
     */
    @Redirect(method = "readFromNBT",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/tileentity/TileEntityBeacon;isBeaconEffect(I)Lnet/minecraft/potion/Potion;")
    )
    @Nullable
    private Effect impl$UsePotionUtilInsteadOfCheckingValidPotions(final int id) {
        return Effect.get(id);
    }

    @Override
    public void bridge$setCustomDisplayName(final String customName) {
        this.customName = customName;
    }

    @Override
    public void bridge$forceSetPrimaryEffect(final Effect potion) {
        this.primaryEffect = potion;
    }

    @Override
    public void bridge$forceSetSecondaryEffect(final Effect potion) {
        this.secondaryEffect = potion;
    }
}
