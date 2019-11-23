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
package org.spongepowered.common.mixin.realtime.network;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.server.MinecraftServer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.common.SpongeImplHooks;
import org.spongepowered.common.bridge.RealTimeTrackingBridge;
import org.spongepowered.common.bridge.world.WorldBridge;

@Mixin(ServerPlayNetHandler.class)
public abstract class NetHandlerPlayServerMixin_RealTime {

    @Shadow private int chatSpamThresholdCount;
    @Shadow private int itemDropThreshold;
    @Shadow @Final private MinecraftServer server;
    @Shadow public ServerPlayerEntity player;

    @Redirect(
        method = "update",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/network/NetHandlerPlayServer;chatSpamThresholdCount:I",
            opcode = Opcodes.PUTFIELD,
            ordinal = 0
        )
    )
    private void realTimeImpl$adjustForRealTimeChatSpamCheck(final ServerPlayNetHandler self, final int modifier) {
        if (SpongeImplHooks.isFakePlayer(this.player) || ((WorldBridge) this.player.field_70170_p).bridge$isFake()) {
            this.chatSpamThresholdCount = modifier;
            return;
        }
        final int ticks = (int) ((RealTimeTrackingBridge) this.server).realTimeBridge$getRealTimeTicks();
        this.chatSpamThresholdCount = Math.max(0, this.chatSpamThresholdCount - ticks);
    }

    @Redirect(
        method = "update",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/network/NetHandlerPlayServer;itemDropThreshold:I",
            opcode = Opcodes.PUTFIELD, ordinal = 0
        )
    )
    private void realTimeImpl$adjustForRealTimeDropSpamCheck(final ServerPlayNetHandler self, final int modifier) {
        if (SpongeImplHooks.isFakePlayer(this.player) || ((WorldBridge) this.player.field_70170_p).bridge$isFake()) {
            this.itemDropThreshold = modifier;
            return;
        }
        final int ticks = (int) ((RealTimeTrackingBridge) this.server).realTimeBridge$getRealTimeTicks();
        this.itemDropThreshold = Math.max(0, this.itemDropThreshold - ticks);
    }

}
