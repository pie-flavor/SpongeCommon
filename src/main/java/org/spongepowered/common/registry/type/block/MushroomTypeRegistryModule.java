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
package org.spongepowered.common.registry.type.block;

import net.minecraft.block.Blocks;
import net.minecraft.world.gen.feature.WorldGenBigMushroom;
import org.spongepowered.api.registry.CatalogRegistryModule;
import org.spongepowered.api.registry.util.RegisterCatalog;
import org.spongepowered.api.world.gen.PopulatorObject;
import org.spongepowered.api.world.gen.type.MushroomType;
import org.spongepowered.api.world.gen.type.MushroomTypes;
import org.spongepowered.common.registry.type.AbstractPrefixAlternateCatalogTypeRegistryModule;
import org.spongepowered.common.world.gen.type.SpongeMushroomType;

@RegisterCatalog(MushroomTypes.class)
public class MushroomTypeRegistryModule
        extends AbstractPrefixAlternateCatalogTypeRegistryModule<MushroomType>
        implements CatalogRegistryModule<MushroomType> {

    public MushroomTypeRegistryModule() {
        super("minecraft");
    }

    @Override
    public void registerDefaults() {
        register(new SpongeMushroomType("minecraft:brown", "brown", (PopulatorObject) new WorldGenBigMushroom(Blocks.BROWN_MUSHROOM_BLOCK)));
        register(new SpongeMushroomType("minecraft:red", "red", (PopulatorObject) new WorldGenBigMushroom(Blocks.RED_MUSHROOM_BLOCK)));
    }

}
