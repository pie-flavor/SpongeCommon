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
import net.minecraft.item.Items;
import net.minecraft.nbt.StringNBT;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.immutable.item.ImmutableAuthorData;
import org.spongepowered.api.data.manipulator.mutable.item.AuthorData;
import org.spongepowered.api.data.value.Value.Immutable;
import org.spongepowered.api.data.value.Value.Mutable;
import org.spongepowered.api.data.value.ValueContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.common.data.manipulator.mutable.item.SpongeAuthorData;
import org.spongepowered.common.data.processor.common.AbstractItemSingleDataProcessor;
import org.spongepowered.common.data.value.immutable.ImmutableSpongeValue;
import org.spongepowered.common.data.value.mutable.SpongeValue;
import org.spongepowered.common.text.SpongeTexts;
import org.spongepowered.common.util.Constants;

import java.util.Optional;

public class ItemAuthorDataProcessor extends AbstractItemSingleDataProcessor<Text, Mutable<Text>, AuthorData, ImmutableAuthorData> {

    public ItemAuthorDataProcessor() {
        super(input -> input.getItem() == Items.WRITABLE_BOOK || input.getItem() == Items.WRITTEN_BOOK, Keys.BOOK_AUTHOR);
    }

    @Override
    public DataTransactionResult removeFrom(final ValueContainer<?> container) {
        return DataTransactionResult.failNoData();
    }

    @Override
    protected AuthorData createManipulator() {
        return new SpongeAuthorData();
    }

    @Override
    protected boolean set(final ItemStack itemStack, final Text value) {
        itemStack.setTagInfo(Constants.Item.Book.ITEM_BOOK_AUTHOR, new StringNBT(SpongeTexts.toLegacy(value)));
        return true;
    }

    @Override
    protected Optional<Text> getVal(final ItemStack itemStack) {
        if (!itemStack.hasTag() || !itemStack.getTag().contains(Constants.Item.Book.ITEM_BOOK_AUTHOR)) {
            return Optional.empty();
        }
        final String json = itemStack.getTag().getString(Constants.Item.Book.ITEM_BOOK_AUTHOR);
        final Text author = TextSerializers.JSON.deserializeUnchecked(json);
        return Optional.of(author);
    }

    @Override
    protected Mutable<Text> constructValue(final Text actualValue) {
        return new SpongeValue<>(Keys.BOOK_AUTHOR, Text.of(), actualValue);
    }

    @Override
    protected Immutable<Text> constructImmutableValue(final Text value) {
        return new ImmutableSpongeValue<>(Keys.BOOK_AUTHOR, Text.of(), value);
    }

}
