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
package org.spongepowered.common.mixin.api.mcp.tileentity;

import net.minecraft.tileentity.TileEntityNote;
import org.spongepowered.api.block.tileentity.Note;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.util.Constants;

import java.util.List;

@NonnullByDefault
@Mixin(TileEntityNote.class)
public abstract class TileEntityNoteMixin_API extends TileEntityMixin_API implements Note {

    @Shadow public byte note;

    @Override
    public DataContainer toContainer() {
        DataContainer container = super.toContainer();
        container.set(Constants.TileEntity.NOTE_ID, this.note);
        return container;
    }

    @Override
    public void supplyVanillaManipulators(List<org.spongepowered.api.data.DataManipulator.Mutable<?, ?>> manipulators) {
        super.supplyVanillaManipulators(manipulators);
        manipulators.add(getNoteData());
    }

}
