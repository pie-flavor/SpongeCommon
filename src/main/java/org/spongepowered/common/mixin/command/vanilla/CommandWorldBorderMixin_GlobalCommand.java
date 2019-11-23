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
package org.spongepowered.common.mixin.command.vanilla;

import net.minecraft.command.CommandWorldBorder;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.dimension.NetherDimension;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.common.SpongeImpl;

@Mixin(CommandWorldBorder.class)
public abstract class CommandWorldBorderMixin_GlobalCommand {

    @Redirect(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/border/WorldBorder;setTransition(DDJ)V"))
    private void globalCommand$setAllTransitions(final WorldBorder border, final double oldSize, final double newSize, final long time) {
        for (final ServerWorld world : SpongeImpl.getServer().worlds) {
            world.getWorldBorder().setTransition(time);
        }
    }

    @Redirect(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/border/WorldBorder;setCenter(DD)V"))
    private void globalCommand$setAllCenters(final WorldBorder border, final double x, final double z) {
        for (final ServerWorld world : SpongeImpl.getServer().worlds) {
            if (world.dimension instanceof NetherDimension) {
                // Unlike Vanilla, Sponge uses separate world borders per world.
                // Because of that, Vanilla stores the world border center as overworld coordinates.
                // We store the nether world border as nether coordinates, therefore we need to
                // divide the center by 8 so it will be equal to the overworld world border.
                world.getWorldBorder().setCenter(x / 8, z / 8);
            } else {
                world.getWorldBorder().setCenter(x, z);
            }
        }
    }

    @Redirect(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/border/WorldBorder;setDamageBuffer(D)V"))
    private void globalCommand$setAllDamageBuffers(final WorldBorder border, final double buffer) {
        for (final ServerWorld world : SpongeImpl.getServer().worlds) {
            world.getWorldBorder().setDamageBuffer(buffer);
        }
    }

    @Redirect(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/border/WorldBorder;setDamageAmount(D)V"))
    private void globalCommand$setAllDamageAmount(final WorldBorder border, final double amount) {
        for (final ServerWorld world : SpongeImpl.getServer().worlds) {
            world.getWorldBorder().setDamagePerBlock(amount);
        }
    }

    @Redirect(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/border/WorldBorder;setWarningTime(I)V"))
    private void globalCommand$setAllWarningTime(final WorldBorder border, final int time) {
        for (final ServerWorld world : SpongeImpl.getServer().worlds) {
            world.getWorldBorder().setWarningTime(time);
        }
    }

    @Redirect(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/border/WorldBorder;setWarningDistance(I)V"))
    private void globalCommand$setAllWarningDistance(final WorldBorder border, final int distance) {
        for (final ServerWorld world : SpongeImpl.getServer().worlds) {
            world.getWorldBorder().setWarningDistance(distance);
        }
    }
}
