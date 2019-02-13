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
package org.spongepowered.common.registry.type.item;

import net.minecraft.item.ItemFishFood;
import org.spongepowered.api.registry.util.AdditionalRegistration;
import org.spongepowered.api.registry.util.RegisterCatalog;
import org.spongepowered.common.registry.type.MinecraftRegistryBasedAlternateCatalogTypeRegistryModule;

@RegisterCatalog(Fishes.class)
public final class FishRegistryModule extends MinecraftRegistryBasedAlternateCatalogTypeRegistryModule<ItemFishFood.FishType, Fish> {

    public FishRegistryModule() {
        super(new String[] {"raw."});
    }

    @Override
    protected ItemFishFood.FishType[] getValues() {
        return ItemFishFood.FishType.values();
    }

    @AdditionalRegistration
    public void registerAdditional() {
        for (ItemFishFood.FishType fishType : ItemFishFood.FishType.values()) {
            final Fish value = enumAs(fishType);
            if (!this.map.containsValue(value)) {
                this.map.put(value.getKey(), value);
            }
        }
    }

}
