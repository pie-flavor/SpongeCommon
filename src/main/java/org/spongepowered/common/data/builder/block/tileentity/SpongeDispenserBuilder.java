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
package org.spongepowered.common.data.builder.block.tileentity;

import net.minecraft.tileentity.TileEntityDispenser;
import org.spongepowered.api.block.tileentity.carrier.Dispenser;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.common.util.Constants;

import java.util.Optional;

public class SpongeDispenserBuilder extends SpongeLockableBuilder<Dispenser> {

    public SpongeDispenserBuilder() {
        super(Dispenser.class, 1);
    }

    @Override
    protected Optional<Dispenser> buildContent(DataView container) throws InvalidDataException {
        return super.buildContent(container).map(dispenser -> {
            if (container.contains(Constants.TileEntity.CUSTOM_NAME)) {
                ((TileEntityDispenser) dispenser).func_190575_a(container.getString(Constants.TileEntity.CUSTOM_NAME).get());
            }
            ((TileEntityDispenser) dispenser).func_145829_t();
            return dispenser;
        });
    }
}
