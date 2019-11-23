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

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.item.ItemMonsterPlacer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.immutable.item.ImmutableSpawnableData;
import org.spongepowered.api.data.manipulator.mutable.item.SpawnableData;
import org.spongepowered.api.data.value.Value.Immutable;
import org.spongepowered.api.data.value.Value.Mutable;
import org.spongepowered.api.data.value.ValueContainer;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.common.SpongeImplHooks;
import org.spongepowered.common.data.manipulator.mutable.item.SpongeSpawnableData;
import org.spongepowered.common.data.processor.common.AbstractItemSingleDataProcessor;
import org.spongepowered.common.data.value.immutable.ImmutableSpongeValue;
import org.spongepowered.common.data.value.mutable.SpongeValue;
import org.spongepowered.common.registry.type.entity.EntityTypeRegistryModule;
import org.spongepowered.common.util.Constants;

import java.util.Optional;

public class SpawnableDataProcessor extends AbstractItemSingleDataProcessor<EntityType, Mutable<EntityType>, SpawnableData, ImmutableSpawnableData> {

    public SpawnableDataProcessor() {
        super(input -> input.getItem().equals(Items.SPAWN_EGG), Keys.SPAWNABLE_ENTITY_TYPE);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean set(final ItemStack itemStack, final EntityType value) {
        final ResourceLocation name = EntityList.getKey((Class<? extends Entity>) value.getEntityClass());
        if (EntityList.ENTITY_EGGS.containsKey(name)) {
            itemStack.getOrCreateChildTag(Constants.TileEntity.Spawner.SPAWNABLE_ENTITY_TAG)
                .putString(Constants.Entity.ENTITY_TYPE_ID, name.toString());
            return true;
        }
        return false;
    }

    @Override
    public Optional<EntityType> getVal(final ItemStack itemStack) {
        final ResourceLocation name = ItemMonsterPlacer.getNamedIdFrom(itemStack);
        if (name != null) {
            final Class<? extends Entity> entity = SpongeImplHooks.getEntityClass(name);
            return Optional.ofNullable(EntityTypeRegistryModule.getInstance().getForClass(entity));
        }
        return Optional.empty();
    }

    @Override
    protected Mutable<EntityType> constructValue(final EntityType actualValue) {
        return new SpongeValue<>(Keys.SPAWNABLE_ENTITY_TYPE, EntityTypes.CREEPER, actualValue);
    }

    @Override
    public Immutable<EntityType> constructImmutableValue(final EntityType value) {
        return ImmutableSpongeValue.cachedOf(Keys.SPAWNABLE_ENTITY_TYPE, EntityTypes.CREEPER, value);
    }

    @Override
    public SpawnableData createManipulator() {
        return new SpongeSpawnableData();
    }

    @Override
    public DataTransactionResult removeFrom(final ValueContainer<?> container) {
        if (!supports(container)) {
            return DataTransactionResult.failNoData();
        }
        final ItemStack itemStack = (ItemStack) container;
        final Optional<EntityType> old = getVal(itemStack);
        if (!old.isPresent()) {
            return DataTransactionResult.successNoData();
        }
        try {
            final CompoundNBT tag = itemStack.getTag();
            if (tag != null) {
                tag.remove(Constants.TileEntity.Spawner.SPAWNABLE_ENTITY_TAG);
            }
            return DataTransactionResult.successRemove(constructImmutableValue(old.get()));
        } catch (final Exception e) {
            return DataTransactionResult.builder().result(DataTransactionResult.Type.ERROR).build();
        }
    }
}
