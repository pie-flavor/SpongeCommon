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
package org.spongepowered.common.registry.type.event;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.DamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSources;
import org.spongepowered.api.registry.RegistryModule;
import org.spongepowered.api.registry.util.RegistrationDependency;
import org.spongepowered.common.bridge.util.DamageSourceBridge;
import org.spongepowered.common.mixin.core.util.DamageSourceAccessor;
import org.spongepowered.common.registry.RegistryHelper;

@RegistrationDependency(DamageTypeRegistryModule.class)
public final class DamageSourceRegistryModule implements RegistryModule {

    public static DamageSource IGNORED_DAMAGE_SOURCE;
    public static DamageSource DAMAGESOURCE_POISON;
    public static DamageSource DAMAGESOURCE_MELTING;

    @Override
    public void registerDefaults() {
        try {
            // These need to be instantiated after the DamageTypeRegistryModule has had a chance to register
            // the damage types, otherwise it will fail and have invalid types.
            {
                DAMAGESOURCE_POISON = net.minecraft.util.DamageSource.causeExplosionDamage((LivingEntity) null);
                ((DamageSourceAccessor) DAMAGESOURCE_POISON).accessor$setId("poison");
                ((DamageSourceBridge) DAMAGESOURCE_POISON).bridge$resetDamageType();
                ((DamageSourceAccessor) DAMAGESOURCE_POISON).accessor$setDamageBypassesArmor();
                DAMAGESOURCE_POISON.setMagicDamage();
            }
            {
                DAMAGESOURCE_MELTING = net.minecraft.util.DamageSource.causeExplosionDamage((LivingEntity) null);
                ((DamageSourceAccessor) DAMAGESOURCE_MELTING).accessor$setId("melting");
                ((DamageSourceBridge) DAMAGESOURCE_MELTING).bridge$resetDamageType();
                ((DamageSourceAccessor) DAMAGESOURCE_MELTING).accessor$setDamageBypassesArmor();
                ((DamageSourceAccessor) DAMAGESOURCE_MELTING).accessor$setFireDamage();
            }
            {
                IGNORED_DAMAGE_SOURCE = net.minecraft.util.DamageSource.causeExplosionDamage((LivingEntity) null);
                ((DamageSourceAccessor) IGNORED_DAMAGE_SOURCE).accessor$setId("spongespecific");
                ((DamageSourceBridge) IGNORED_DAMAGE_SOURCE).bridge$resetDamageType();
                ((DamageSourceAccessor) IGNORED_DAMAGE_SOURCE).accessor$setDamageAllowedInCreativeMode();
                ((DamageSourceAccessor) IGNORED_DAMAGE_SOURCE).accessor$setDamageBypassesArmor();
            }
            RegistryHelper.setFinalStatic(DamageSources.class, "DRAGON_BREATH", DamageSource.DRAGON_BREATH);
            RegistryHelper.setFinalStatic(DamageSources.class, "DROWNING", DamageSource.DROWN);
            RegistryHelper.setFinalStatic(DamageSources.class, "FALLING", DamageSource.FALL);
            RegistryHelper.setFinalStatic(DamageSources.class, "FIRE_TICK", DamageSource.ON_FIRE);
            RegistryHelper.setFinalStatic(DamageSources.class, "GENERIC", DamageSource.GENERIC);
            RegistryHelper.setFinalStatic(DamageSources.class, "MAGIC", DamageSource.MAGIC);
            RegistryHelper.setFinalStatic(DamageSources.class, "MELTING", DAMAGESOURCE_MELTING);
            RegistryHelper.setFinalStatic(DamageSources.class, "POISON", DAMAGESOURCE_POISON);
            RegistryHelper.setFinalStatic(DamageSources.class, "STARVATION", DamageSource.STARVE);
            RegistryHelper.setFinalStatic(DamageSources.class, "WITHER", DamageSource.WITHER);
            RegistryHelper.setFinalStatic(DamageSources.class, "VOID", DamageSource.OUT_OF_WORLD);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
