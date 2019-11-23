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
package org.spongepowered.common.data.manipulator.mutable.block;

import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.immutable.block.ImmutableDropData;
import org.spongepowered.api.data.manipulator.mutable.block.DropData;
import org.spongepowered.api.data.value.Value.Mutable;
import org.spongepowered.common.data.manipulator.immutable.block.ImmutableSpongeDropData;
import org.spongepowered.common.data.manipulator.mutable.common.AbstractBooleanData;

public class SpongeDropData extends AbstractBooleanData<DropData, ImmutableDropData> implements DropData {

    public SpongeDropData(boolean value) {
        super(DropData.class, value, Keys.SHOULD_DROP, ImmutableSpongeDropData.class, false);
    }

    public SpongeDropData() {
        this(true);
    }

    @Override
    public Mutable<Boolean> willDrop() {
        return getValueGetter();
    }
}
