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
package org.spongepowered.common.mixin.realtime.mixin;

import net.minecraft.profiler.Profiler;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import net.minecraft.world.storage.WorldSavedDataStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.mixin.realtime.IMixinRealTimeTicking;

import javax.annotation.Nullable;

@Mixin(WorldServer.class)
public abstract class MixinWorldServer extends World implements IMixinRealTimeTicking {

    protected MixinWorldServer(ISaveHandler saveHandler, @Nullable WorldSavedDataStorage storage,
        WorldInfo properties, Dimension dimension, Profiler profiler, boolean isClient) {
        super(saveHandler, storage, properties, dimension, profiler, isClient);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void fixTimeOfDay(CallbackInfo ci) {
        if (this.worldInfo.getGameRulesInstance().getBoolean("doDaylightCycle")) {
            // Subtract the one the original tick method is going to add
            long diff = this.getRealTimeTicks() - 1;
            // Don't set if we're not changing it as other mods might be listening for changes
            if (diff > 0) {
                this.worldInfo.setDayTime(this.worldInfo.getDayTime() + diff);
            }
        }
    }

}
