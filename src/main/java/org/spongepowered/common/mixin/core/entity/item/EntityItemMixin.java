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
package org.spongepowered.common.mixin.core.entity.item;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.bridge.entity.EntityItemBridge;
import org.spongepowered.common.bridge.inventory.TrackedInventoryBridge;
import org.spongepowered.common.bridge.world.WorldBridge;
import org.spongepowered.common.bridge.world.WorldInfoBridge;
import org.spongepowered.common.event.SpongeCommonEventFactory;
import org.spongepowered.common.mixin.core.entity.EntityMixin;
import org.spongepowered.common.util.Constants;

@Mixin(EntityItem.class)
public abstract class EntityItemMixin extends EntityMixin implements EntityItemBridge {

    private static final int MAGIC_PREVIOUS = -1;
    @Shadow private int pickupDelay;
    @Shadow private int age;
    @Shadow public abstract ItemStack getItem();
    /**
     * A simple cached value of the merge radius for this item.
     * Since the value is configurable, the first time searching for
     * other items, this value is cached.
     */
    private double impl$cachedRadius = -1;

    private int impl$previousPickupDelay = MAGIC_PREVIOUS;
    private boolean impl$infinitePickupDelay;
    private int impl$previousDespawnDelay = MAGIC_PREVIOUS;
    private boolean impl$infiniteDespawnDelay;

    @Override
    public boolean bridge$infinitePickupDelay() {
        return this.impl$infinitePickupDelay;
    }

    @ModifyConstant(method = "searchForOtherItemsNearby", constant = @Constant(doubleValue = 0.5D))
    private double impl$changeSearchRadiusFromConfig(final double originalRadius) {
        if (this.world.isRemote || ((WorldBridge) this.world).bridge$isFake()) {
            return originalRadius;
        }
        if (this.impl$cachedRadius == -1) {
            final double configRadius = ((WorldInfoBridge) this.world.getWorldInfo()).bridge$getConfigAdapter().getConfig().getWorld().getItemMergeRadius();
            this.impl$cachedRadius = configRadius < 0 ? 0 : configRadius;
        }
        return this.impl$cachedRadius;
    }

    @Override
    public int bridge$getPickupDelay() {
        return this.impl$infinitePickupDelay ? this.impl$previousPickupDelay : this.pickupDelay;
    }

    @Override
    public void bridge$setPickupDelay(final int delay, final boolean infinite) {
        this.pickupDelay = delay;
        final boolean previous = this.impl$infinitePickupDelay;
        this.impl$infinitePickupDelay = infinite;
        if (infinite && !previous) {
            this.impl$previousPickupDelay = this.pickupDelay;
            this.pickupDelay = Constants.Entity.Item.MAGIC_NO_PICKUP;
        } else if (!infinite) {
            this.impl$previousPickupDelay = MAGIC_PREVIOUS;
        }
    }

    @Override
    public boolean bridge$infiniteDespawnDelay() {
        return this.impl$infiniteDespawnDelay;
    }

    @Override
    public int bridge$getDespawnDelay() {
        return 6000 - (this.impl$infiniteDespawnDelay ? this.impl$previousDespawnDelay : this.age);
    }

    @Override
    public void bridge$setDespawnDelay(final int delay) {
        this.age = 6000 - delay;
    }

    @Override
    public void bridge$setDespawnDelay(final int delay, final boolean infinite) {
        this.age = 6000 - delay;
        final boolean previous = this.impl$infiniteDespawnDelay;
        this.impl$infiniteDespawnDelay = infinite;
        if (infinite && !previous) {
            this.impl$previousDespawnDelay = this.age;
            this.age = Constants.Entity.Item.MAGIC_NO_DESPAWN;
        } else if (!infinite) {
            this.impl$previousDespawnDelay = MAGIC_PREVIOUS;
        }
    }

    @Override
    public void spongeImpl$readFromSpongeCompound(final NBTTagCompound compound) {
        super.spongeImpl$readFromSpongeCompound(compound);

        this.impl$infinitePickupDelay = compound.getBoolean(Constants.Sponge.Entity.Item.INFINITE_PICKUP_DELAY);
        if (compound.hasKey(Constants.Sponge.Entity.Item.PREVIOUS_PICKUP_DELAY, Constants.NBT.TAG_ANY_NUMERIC)) {
            this.impl$previousPickupDelay = compound.getInteger(Constants.Sponge.Entity.Item.PREVIOUS_PICKUP_DELAY);
        } else {
            this.impl$previousPickupDelay = MAGIC_PREVIOUS;
        }
        this.impl$infiniteDespawnDelay = compound.getBoolean(Constants.Sponge.Entity.Item.INFINITE_DESPAWN_DELAY);
        if (compound.hasKey(Constants.Sponge.Entity.Item.PREVIOUS_DESPAWN_DELAY, Constants.NBT.TAG_ANY_NUMERIC)) {
            this.impl$previousDespawnDelay = compound.getInteger(Constants.Sponge.Entity.Item.PREVIOUS_DESPAWN_DELAY);
        } else {
            this.impl$previousDespawnDelay = MAGIC_PREVIOUS;
        }

        if (this.impl$infinitePickupDelay) {
            if (this.impl$previousPickupDelay != this.pickupDelay) {
                this.impl$previousPickupDelay = this.pickupDelay;
            }

            this.pickupDelay = Constants.Entity.Item.MAGIC_NO_PICKUP;
        } else if (this.pickupDelay == Constants.Entity.Item.MAGIC_NO_PICKUP && this.impl$previousPickupDelay != MAGIC_PREVIOUS) {
            this.pickupDelay = this.impl$previousPickupDelay;
            this.impl$previousPickupDelay = MAGIC_PREVIOUS;
        }

        if (this.impl$infiniteDespawnDelay) {
            if (this.impl$previousDespawnDelay != this.age) {
                this.impl$previousDespawnDelay = this.age;
            }

            this.age = Constants.Entity.Item.MAGIC_NO_DESPAWN;
        } else if (this.age == Constants.Entity.Item.MAGIC_NO_DESPAWN && this.impl$previousDespawnDelay != MAGIC_PREVIOUS) {
            this.age = this.impl$previousDespawnDelay;
            this.impl$previousDespawnDelay = MAGIC_PREVIOUS;
        }
    }

    @Override
    public void spongeImpl$writeToSpongeCompound(final NBTTagCompound compound) {
        super.spongeImpl$writeToSpongeCompound(compound);

        compound.setBoolean(Constants.Sponge.Entity.Item.INFINITE_PICKUP_DELAY, this.impl$infinitePickupDelay);
        compound.setShort(Constants.Sponge.Entity.Item.PREVIOUS_PICKUP_DELAY, (short) this.impl$previousPickupDelay);
        compound.setBoolean(Constants.Sponge.Entity.Item.INFINITE_DESPAWN_DELAY, this.impl$infiniteDespawnDelay);
        compound.setShort(Constants.Sponge.Entity.Item.PREVIOUS_DESPAWN_DELAY, (short) this.impl$previousDespawnDelay);
    }

    @Inject(
        method = "onCollideWithPlayer",
        at = @At(
            value = "INVOKE",
            ordinal = 0,
            target = "Lnet/minecraft/entity/item/EntityItem;getItem()Lnet/minecraft/item/ItemStack;"),
        cancellable = true
    )
    private void spongeImpl$ThrowPickupEvent(final EntityPlayer entityIn, final CallbackInfo ci) {
        if (!SpongeCommonEventFactory.callPlayerChangeInventoryPickupPreEvent(entityIn, (EntityItem) (Object) this, this.pickupDelay)) {
            ci.cancel();
        }
    }

    @Redirect(method = "onCollideWithPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/InventoryPlayer;addItemStackToInventory(Lnet/minecraft/item/ItemStack;)Z"))
    private boolean spongeImpl$throwPikcupEventForAddItem(final InventoryPlayer inventory, final ItemStack itemStack, final EntityPlayer player) {
        final TrackedInventoryBridge inv = (TrackedInventoryBridge) inventory;
        inv.bridge$setCaptureInventory(true);
        final boolean added = inventory.addItemStackToInventory(itemStack);
        inv.bridge$setCaptureInventory(false);
        inv.bridge$getCapturedSlotTransactions();
        if (!SpongeCommonEventFactory.callPlayerChangeInventoryPickupEvent(player, inv)) {
            return false;
        }
        return added;
    }

}
