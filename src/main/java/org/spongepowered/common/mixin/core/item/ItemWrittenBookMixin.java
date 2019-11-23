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
package org.spongepowered.common.mixin.core.item;

import net.minecraft.item.ItemStack;
import net.minecraft.item.WrittenBookItem;
import org.spongepowered.api.data.manipulator.DataManipulator;
import org.spongepowered.api.data.manipulator.mutable.item.AuthorData;
import org.spongepowered.api.data.manipulator.mutable.item.GenerationData;
import org.spongepowered.api.data.manipulator.mutable.item.PagedData;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(WrittenBookItem.class)

/**
 * This is actually the written, uneditable book class (the MCP name is bad)
 */
public abstract class ItemWrittenBookMixin extends ItemMixin {

    @Override
    public void bridge$gatherManipulators(ItemStack itemStack, List<DataManipulator<?, ?>> list) {
        super.bridge$gatherManipulators(itemStack, list);
        org.spongepowered.api.item.inventory.ItemStack spongeStack = (org.spongepowered.api.item.inventory.ItemStack) itemStack;

        spongeStack.get(AuthorData.class).ifPresent(list::add);
        spongeStack.get(PagedData.class).ifPresent(list::add);
        spongeStack.get(GenerationData.class).ifPresent(list::add);
    }
}
