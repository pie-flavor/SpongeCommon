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

import net.minecraft.item.ItemFishFood;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.immutable.item.ImmutableFishData;
import org.spongepowered.api.data.manipulator.mutable.item.FishData;
import org.spongepowered.api.data.type.Fish;
import org.spongepowered.api.data.type.Fishes;
import org.spongepowered.api.data.value.Value.Immutable;
import org.spongepowered.api.data.value.Value.Mutable;
import org.spongepowered.api.data.value.ValueContainer;
import org.spongepowered.common.data.manipulator.mutable.item.SpongeFishData;
import org.spongepowered.common.data.processor.common.AbstractItemSingleDataProcessor;
import org.spongepowered.common.data.value.immutable.ImmutableSpongeValue;
import org.spongepowered.common.data.value.mutable.SpongeValue;

import java.util.Optional;

public class FishDataProcessor extends AbstractItemSingleDataProcessor<Fish, Mutable<Fish>, FishData, ImmutableFishData> {

    public FishDataProcessor() {
        super(stack -> stack.getItem().equals(Items.FISH), Keys.FISH_TYPE);
    }

    @Override
    protected FishData createManipulator() {
        return new SpongeFishData();
    }

    @Override
    protected boolean set(ItemStack itemStack, Fish value) {
        itemStack.setItemDamage(((ItemFishFood.FishType) (Object) value).getMetadata());
        return true;
    }

    @Override
    protected Optional<Fish> getVal(ItemStack itemStack) {
        return Optional.of((Fish) (Object) ItemFishFood.FishType.byItemStack(itemStack));
    }

    @Override
    protected Mutable<Fish> constructValue(Fish actualValue) {
        return new SpongeValue<>(Keys.FISH_TYPE, Fishes.COD, actualValue);
    }

    @Override
    protected Immutable<Fish> constructImmutableValue(Fish value) {
        return ImmutableSpongeValue.cachedOf(Keys.FISH_TYPE, Fishes.COD, value);
    }

    @Override
    public DataTransactionResult removeFrom(ValueContainer<?> container) {
        return DataTransactionResult.failNoData();
    }

}
