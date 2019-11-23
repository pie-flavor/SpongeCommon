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
package org.spongepowered.common.mixin.api.mcp.advancements;

import net.minecraft.advancements.ICriterionTrigger;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.advancement.criteria.trigger.FilteredTriggerConfiguration;
import org.spongepowered.api.advancement.criteria.trigger.Trigger;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.advancement.TriggerBridge;

@Mixin(ICriterionTrigger.class)
public interface ICriterionTriggerMixin_API<C extends FilteredTriggerConfiguration> extends Trigger<C> {

    @Shadow ResourceLocation shadow$getId();

    @Override
    default String getId() {
        return shadow$getId().toString();
    }

    @Override
    default String getName() {
        return shadow$getId().getPath();
    }

    @Override
    default void trigger() {
        trigger(Sponge.getServer().getOnlinePlayers());
    }

    @Override
    default void trigger(final Iterable<Player> players) {
        players.forEach(((TriggerBridge) this)::bridge$trigger);
    }

    @Override
    default void trigger(final Player player) {
        ((TriggerBridge) this).bridge$trigger(player);
        // This could possibly be implemented in all the vanilla triggers
        // and construct trigger method arguments based on context values
        // Not needed for now, just assume it always fails
    }
}
