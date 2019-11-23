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
package org.spongepowered.common.world.border;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.server.SWorldBorderPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.border.IBorderListener;
import net.minecraft.world.border.WorldBorder;
import org.spongepowered.api.entity.living.player.Player;

public final class PlayerBorderListener implements IBorderListener {

    private final int dimensionId;
    private final MinecraftServer server;

    public PlayerBorderListener(MinecraftServer server, int dimensionId) {
        this.server = server;
        this.dimensionId = dimensionId;
    }

    @Override
    public void onSizeChanged(WorldBorder border, double newSize) {
        sendBorderPacket(new SWorldBorderPacket(border, SWorldBorderPacket.Action.SET_SIZE));
    }

    @Override
    public void onTransitionStarted(WorldBorder border, double oldSize, double newSize, long time) {
        sendBorderPacket(new SWorldBorderPacket(border, SWorldBorderPacket.Action.LERP_SIZE));
    }

    @Override
    public void onCenterChanged(WorldBorder border, double x, double z) {
        sendBorderPacket(new SWorldBorderPacket(border, SWorldBorderPacket.Action.SET_CENTER));
    }

    @Override
    public void onWarningTimeChanged(WorldBorder border, int newTime) {
        sendBorderPacket(new SWorldBorderPacket(border, SWorldBorderPacket.Action.SET_WARNING_TIME));
    }

    @Override
    public void onWarningDistanceChanged(WorldBorder border, int newDistance) {
        sendBorderPacket(new SWorldBorderPacket(border, SWorldBorderPacket.Action.SET_WARNING_BLOCKS));
    }

    @Override
    public void onDamageAmountChanged(WorldBorder border, double newAmount) {
    }

    @Override
    public void onDamageBufferChanged(WorldBorder border, double newSize) {
    }

    private void sendBorderPacket(IPacket<?> packet) {
        for (ServerPlayerEntity player : this.server.getPlayerList().getPlayers()) {
            if (player.dimension == this.dimensionId && !((Player) player).getWorldBorder().isPresent()) {
                player.connection.sendPacket(packet);
            }
        }
    }
}
