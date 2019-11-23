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
package org.spongepowered.common.event.tracking.phase.packet.player;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.ServerPlayNetHandler;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.entity.living.humanoid.player.ResourcePackStatusEvent;
import org.spongepowered.api.resourcepack.ResourcePack;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.event.tracking.phase.packet.BasicPacketContext;
import org.spongepowered.common.event.tracking.phase.packet.BasicPacketState;
import org.spongepowered.common.bridge.network.NetHandlerPlayServerBridge;
import org.spongepowered.common.mixin.core.network.play.client.CPacketResourcePackStatusAccessor;

public final class ResourcePackState extends BasicPacketState {

    @Override
    public void unwind(BasicPacketContext phaseContext) {
        final ServerPlayerEntity player = phaseContext.getPacketPlayer();

        final ServerPlayNetHandler connection = player.connection;
        final NetHandlerPlayServerBridge mixinHandler = (NetHandlerPlayServerBridge) connection;
        final CPacketResourcePackStatusAccessor resource = phaseContext.getPacket();
        final ResourcePackStatusEvent.ResourcePackStatus status;
        ResourcePack pack;
        switch (resource.accessor$getAction()) {
            case ACCEPTED:
                pack = mixinHandler.bridge$popReceivedResourcePack(true);
                status = ResourcePackStatusEvent.ResourcePackStatus.ACCEPTED;
                break;
            case DECLINED:
                pack = mixinHandler.bridge$popReceivedResourcePack(false);
                status = ResourcePackStatusEvent.ResourcePackStatus.DECLINED;
                break;
            case SUCCESSFULLY_LOADED:
                pack = mixinHandler.bridge$popAcceptedResourcePack();
                status = ResourcePackStatusEvent.ResourcePackStatus.SUCCESSFULLY_LOADED;
                break;
            case FAILED_DOWNLOAD:
                pack = mixinHandler.bridge$popAcceptedResourcePack();
                status = ResourcePackStatusEvent.ResourcePackStatus.FAILED;
                break;
            default:
                throw new AssertionError();
        }
        if (pack == null) {
            return;
        }
        SpongeImpl.postEvent(
            SpongeEventFactory.createResourcePackStatusEvent(Sponge.getCauseStackManager().getCurrentCause(), pack, (Player) player, status));
    }
}
