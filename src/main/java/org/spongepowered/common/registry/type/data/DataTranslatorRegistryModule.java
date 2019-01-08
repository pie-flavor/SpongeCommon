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
package org.spongepowered.common.registry.type.data;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;
import org.spongepowered.api.CatalogKey;
import org.spongepowered.api.data.persistence.DataTranslator;
import org.spongepowered.api.registry.AdditionalCatalogRegistryModule;
import org.spongepowered.api.registry.AlternateCatalogRegistryModule;
import org.spongepowered.common.data.persistence.ConfigurateTranslator;
import org.spongepowered.common.data.persistence.DataSerializers;
import org.spongepowered.common.data.persistence.DataTranslatorTypeSerializer;
import org.spongepowered.common.data.persistence.LegacySchematicTranslator;
import org.spongepowered.common.data.persistence.SchematicTranslator;
import org.spongepowered.common.registry.AbstractCatalogRegistryModule;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings({"rawtypes", "unchecked"})
public class DataTranslatorRegistryModule extends AbstractCatalogRegistryModule<DataTranslator> implements
        AlternateCatalogRegistryModule<DataTranslator>, AdditionalCatalogRegistryModule<DataTranslator> {

    public static DataTranslatorRegistryModule getInstance() {
        return Holder.INSTANCE;
    }

    private final Map<Class<?>, DataTranslator> byClass = new HashMap<>();

    @Override
    public Map<String, DataTranslator> provideCatalogMap() {
        final Map<String, DataTranslator> modifierMap = new HashMap<>();
        for (Map.Entry<CatalogKey, DataTranslator> entry : this.map.entrySet()) {
            if (entry.getKey().getNamespace().equals("sponge")) {
                modifierMap.put(entry.getKey().getValue(), entry.getValue());
            }
        }
        return modifierMap;
    }

    public <T> Optional<DataTranslator<T>> getByClass(Class<T> type) {
        return Optional.ofNullable(this.byClass.get(checkNotNull(type, "type")));
    }

    @Override
    public void registerAdditionalCatalog(DataTranslator dataTranslator) {
        checkNotNull(dataTranslator, "DataTranslator cannot be null");
        checkArgument(!this.map.containsKey(dataTranslator.getKey()), "Duplicate Id");
        final TypeToken<?> typeToken = dataTranslator.getToken();
        checkNotNull(typeToken, "DataTranslator type token cannot be null");
        final Class<?> type = typeToken.getRawType();
        if (this.byClass.containsKey(type)) {
            throw new IllegalStateException("Already registered the DataTranslator for " + type.getCanonicalName());
        }
        this.map.put(dataTranslator.getKey(), dataTranslator);
        this.byClass.put(type, dataTranslator);
        if (TypeSerializers.getDefaultSerializers().get(typeToken) == null) {
            TypeSerializers.getDefaultSerializers().registerType(typeToken, DataTranslatorTypeSerializer.from(dataTranslator));
        }
    }

    @Override
    public void registerDefaults() {
        DataSerializers.registerSerializers(this);

        registerAdditionalCatalog(SchematicTranslator.get());
        registerAdditionalCatalog(ConfigurateTranslator.instance());

        // Just provide a mapping for the following translator, Schematic is already in use
        final LegacySchematicTranslator legacySchematicTranslator = LegacySchematicTranslator.get();
        this.map.put(legacySchematicTranslator.getKey(), legacySchematicTranslator);
    }

    DataTranslatorRegistryModule() {
    }

    private static final class Holder {

        static final DataTranslatorRegistryModule INSTANCE = new DataTranslatorRegistryModule();
    }
}
