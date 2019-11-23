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
package org.spongepowered.common.data.processor.data.item;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.immutable.ImmutableRepresentedPlayerData;
import org.spongepowered.api.data.manipulator.mutable.RepresentedPlayerData;
import org.spongepowered.api.data.type.SkullTypes;
import org.spongepowered.api.data.value.Value.Immutable;
import org.spongepowered.api.data.value.Value.Mutable;
import org.spongepowered.api.data.value.ValueContainer;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.common.data.manipulator.mutable.SpongeRepresentedPlayerData;
import org.spongepowered.common.data.processor.common.AbstractItemSingleDataProcessor;
import org.spongepowered.common.data.processor.common.SkullUtils;
import org.spongepowered.common.data.value.immutable.ImmutableSpongeValue;
import org.spongepowered.common.data.value.mutable.SpongeValue;
import org.spongepowered.common.util.Constants;

import java.util.Optional;

public class ItemSkullRepresentedPlayerDataProcessor
        extends AbstractItemSingleDataProcessor<GameProfile, Mutable<GameProfile>, RepresentedPlayerData, ImmutableRepresentedPlayerData> {

    public ItemSkullRepresentedPlayerDataProcessor() {
        super(ItemSkullRepresentedPlayerDataProcessor::isSupportedItem, Keys.REPRESENTED_PLAYER);
    }

    private static boolean isSupportedItem(ItemStack stack) {
        return SkullUtils.isValidItemStack(stack) && SkullUtils.getSkullType(stack.getMetadata()).equals(SkullTypes.PLAYER);
    }

    @Override
    public DataTransactionResult removeFrom(ValueContainer<?> container) {
        if (supports(container)) {
            ItemStack stack = (ItemStack) container;
            Optional<GameProfile> old = getVal(stack);
            if (!old.isPresent()) {
                return DataTransactionResult.successNoData();
            }
            if (SkullUtils.setProfile(stack, null)) {
                return DataTransactionResult.successRemove(constructImmutableValue(old.get()));
            }
            return DataTransactionResult.builder().result(DataTransactionResult.Type.ERROR).build();
        }
        return DataTransactionResult.failNoData();
    }

    @Override
    protected boolean set(ItemStack itemStack, GameProfile value) {
        return SkullUtils.setProfile(itemStack, value);
    }

    @Override
    protected Optional<GameProfile> getVal(ItemStack itemStack) {
        if (SkullUtils.isValidItemStack(itemStack) && SkullUtils.getSkullType(itemStack.getMetadata()).equals(SkullTypes.PLAYER)) {
            final CompoundNBT nbt = itemStack.getChildTag(Constants.Item.Skull.ITEM_SKULL_OWNER);
            final com.mojang.authlib.GameProfile mcProfile = nbt == null ? null : NBTUtil.readGameProfileFromNBT(nbt);
            return Optional.ofNullable((GameProfile) mcProfile);
        }
        return Optional.empty();
    }

    @Override
    protected Mutable<GameProfile> constructValue(GameProfile actualValue) {
        return new SpongeValue<>(this.key, SpongeRepresentedPlayerData.NULL_PROFILE, actualValue);
    }

    @Override
    protected Immutable<GameProfile> constructImmutableValue(GameProfile value) {
        return new ImmutableSpongeValue<>(this.key, SpongeRepresentedPlayerData.NULL_PROFILE, value);
    }

    @Override
    protected RepresentedPlayerData createManipulator() {
        return new SpongeRepresentedPlayerData();
    }

}
