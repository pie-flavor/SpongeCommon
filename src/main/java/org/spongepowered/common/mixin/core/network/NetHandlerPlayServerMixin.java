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
package org.spongepowered.common.mixin.core.network;

import com.flowpowered.math.vector.Vector3d;
import io.netty.util.collection.LongObjectHashMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketThreadUtil;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.play.INetHandlerPlayServer;
import net.minecraft.network.play.client.CPacketAnimation;
import net.minecraft.network.play.client.CPacketClickWindow;
import net.minecraft.network.play.client.CPacketCreativeInventoryAction;
import net.minecraft.network.play.client.CPacketKeepAlive;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.network.play.client.CPacketPlayerTryUseItem;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.network.play.client.CPacketResourcePackStatus;
import net.minecraft.network.play.client.CPacketSpectate;
import net.minecraft.network.play.client.CPacketUpdateSign;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.network.play.client.CPacketVehicleMove;
import net.minecraft.network.play.server.SPacketEntityAttach;
import net.minecraft.network.play.server.SPacketKeepAlive;
import net.minecraft.network.play.server.SPacketMoveVehicle;
import net.minecraft.network.play.server.SPacketPlayerListItem;
import net.minecraft.network.play.server.SPacketResourcePackSend;
import net.minecraft.network.play.server.SPacketSetExperience;
import net.minecraft.network.play.server.SPacketSetSlot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.server.management.PlayerList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.data.manipulator.mutable.tileentity.SignData;
import org.spongepowered.api.data.type.HandType;
import org.spongepowered.api.data.value.mutable.ListValue;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.Humanoid;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.block.tileentity.ChangeSignEvent;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.entity.RotateEntityEvent;
import org.spongepowered.api.event.entity.living.humanoid.AnimateHandEvent;
import org.spongepowered.api.event.item.inventory.ClickInventoryEvent;
import org.spongepowered.api.event.message.MessageEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.resourcepack.ResourcePack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.SpongeImplHooks;
import org.spongepowered.common.bridge.entity.EntityBridge;
import org.spongepowered.common.bridge.entity.player.EntityPlayerBridge;
import org.spongepowered.common.bridge.entity.player.EntityPlayerMPBridge;
import org.spongepowered.common.bridge.entity.player.InventoryPlayerBridge;
import org.spongepowered.common.bridge.inventory.ContainerBridge;
import org.spongepowered.common.bridge.inventory.ContainerPlayerBridge;
import org.spongepowered.common.bridge.network.NetHandlerPlayServerBridge;
import org.spongepowered.common.bridge.packet.SPacketResourcePackSendBridge;
import org.spongepowered.common.bridge.server.management.PlayerInteractionManagerBridge;
import org.spongepowered.common.entity.EntityUtil;
import org.spongepowered.common.entity.player.tab.SpongeTabList;
import org.spongepowered.common.event.ShouldFire;
import org.spongepowered.common.event.SpongeCommonEventFactory;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.phase.packet.PacketContext;
import org.spongepowered.common.event.tracking.phase.packet.PacketPhaseUtil;
import org.spongepowered.common.event.tracking.phase.tick.PlayerTickContext;
import org.spongepowered.common.event.tracking.phase.tick.TickPhase;
import org.spongepowered.common.item.inventory.util.ItemStackUtil;
import org.spongepowered.common.text.SpongeTexts;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.util.VecHelper;

import java.lang.ref.WeakReference;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

@Mixin(NetHandlerPlayServer.class)
public abstract class NetHandlerPlayServerMixin implements NetHandlerPlayServerBridge {

    @Shadow @Final public NetworkManager netManager;
    @Shadow @Final private MinecraftServer server;
    @Shadow public EntityPlayerMP player;
    @Shadow private Entity lowestRiddenEnt;
    @Shadow private int itemDropThreshold;
    // Appears to be the last keep-alive packet ID. Currently the same as
    // field_194402_f, but _f is time (which the ID just so happens to match).
    @Shadow private long field_194404_h;

    @Shadow public abstract void sendPacket(final Packet<?> packetIn);
    @Shadow public abstract void disconnect(ITextComponent reason);
    @Shadow private void captureCurrentPosition() {}
    @Shadow protected abstract long currentTimeMillis();

    private boolean impl$justTeleported = false;
    private long impl$lastTryBlockPacketTimeStamp = 0;
    @Nullable private Location<World> impl$lastMoveLocation = null;
    @Nullable private ResourcePack impl$lastReceivedPack, lastAcceptedPack;
    private final AtomicInteger impl$numResourcePacksInTransit = new AtomicInteger();
    private final LongObjectHashMap<Runnable> impl$customKeepAliveCallbacks = new LongObjectHashMap<>();
    @Nullable private Transform<World> impl$spectatingTeleportLocation;

    @Override
    public void bridge$captureCurrentPlayerPosition() {
        this.captureCurrentPosition();
    }

    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayerMP;onUpdateEntity()V"))
    private void impl$onPlayerTick(final EntityPlayerMP player) {
        if (player.field_70170_p.field_72995_K) {
            player.func_71127_g();
            return;
        }
        try (final CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame();
             final PlayerTickContext context = TickPhase.Tick.PLAYER.createPhaseContext().source(player)) {
            context.buildAndSwitch();
            frame.pushCause(player);
            player.func_71127_g();
        }
    }

    /**
     * @param manager The player network connection
     * @param packet The original packet to be sent
     * @author kashike
     */
    @Redirect(method = "sendPacket(Lnet/minecraft/network/Packet;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkManager;sendPacket(Lnet/minecraft/network/Packet;)V"))
    private void impl$onSendPacket(final NetworkManager manager, Packet<?> packet) {
        // Update the tab list data
        if (packet instanceof SPacketPlayerListItem) {
            ((SpongeTabList) ((Player) this.player).getTabList()).updateEntriesOnSend((SPacketPlayerListItem) packet);
        } else if (packet instanceof SPacketResourcePackSend) {
            // Send a custom keep-alive packet that doesn't match vanilla.
            long now = this.currentTimeMillis() - 1;
            while (now == this.field_194404_h || this.impl$customKeepAliveCallbacks.containsKey(now)) {
                now--;
            }
            final ResourcePack resourcePack = ((SPacketResourcePackSendBridge) packet).bridge$getSpongePack();
            this.impl$numResourcePacksInTransit.incrementAndGet();
            this.impl$customKeepAliveCallbacks.put(now, () -> {
                this.impl$lastReceivedPack = resourcePack; // TODO do something with the old value
                this.impl$numResourcePacksInTransit.decrementAndGet();
            });
            this.netManager.func_179290_a(new SPacketKeepAlive(now));
        } else if (packet instanceof SPacketSetExperience) {
            // Ensures experience is in sync server-side.
            ((EntityPlayerBridge) this.player).bridge$recalculateTotalExperience();
        }

        packet = packet;
        if (packet != null) {
            manager.func_179290_a(packet);
        }
    }

    @Inject(method = "processKeepAlive", at = @At("HEAD"), cancellable = true)
    private void impl$checkSpongeKeepAlive(final CPacketKeepAlive packetIn, final CallbackInfo ci) {
        final Runnable callback = this.impl$customKeepAliveCallbacks.get(packetIn.func_149460_c());
        if (callback != null) {
            PacketThreadUtil.func_180031_a(packetIn, (INetHandlerPlayServer) this, this.player.func_71121_q());
            this.impl$customKeepAliveCallbacks.remove(packetIn.func_149460_c());
            callback.run();
            ci.cancel();
        }
    }

    /**
     * @author Zidane
     *
     * Invoke before {@code System.arraycopy(packetIn.getLines(), 0, tileentitysign.signText, 0, 4);} (line 1156 in source) to call SignChangeEvent.
     * @param packetIn Injected packet param
     * @param ci Info to provide mixin on how to handle the callback
     * @param worldserver Injected world param
     * @param blockpos Injected blockpos param
     * @param tileentity Injected tilentity param
     * @param tileentitysign Injected tileentitysign param
     */
    @Inject(method = "processUpdateSign", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/play/client/CPacketUpdateSign;getLines()[Ljava/lang/String;"), cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD)
    private void impl$callSignChangeEvent(final CPacketUpdateSign packetIn, final CallbackInfo ci, final WorldServer worldserver, final BlockPos blockpos, final IBlockState iblockstate, final TileEntity tileentity, final TileEntitySign tileentitysign) {
        ci.cancel();
        final Optional<SignData> existingSignData = ((Sign) tileentitysign).get(SignData.class);
        if (!existingSignData.isPresent()) {
            // TODO Unsure if this is the best to do here...
            throw new RuntimeException("Critical error! Sign data not present on sign!");
        }
        final SignData changedSignData = existingSignData.get().copy();
        final ListValue<Text> lines = changedSignData.lines();
        for (int i = 0; i < packetIn.func_187017_b().length; i++) {
            lines.set(i, SpongeTexts.toText(new TextComponentString(packetIn.func_187017_b()[i])));
        }
        changedSignData.set(lines);
        // I pass changedSignData in here twice to emulate the fact that even-though the current sign data doesn't have the lines from the packet
        // applied, this is what it "is" right now. If the data shown in the world is desired, it can be fetched from Sign.getData
        Sponge.getCauseStackManager().pushCause(this.player);
        final ChangeSignEvent event =
                SpongeEventFactory.createChangeSignEvent(Sponge.getCauseStackManager().getCurrentCause(),
                    changedSignData.asImmutable(), changedSignData, (Sign) tileentitysign);
        if (!SpongeImpl.postEvent(event)) {
            ((Sign) tileentitysign).offer(event.getText());
        } else {
            // If cancelled, I set the data back that was fetched from the sign. This means that if its a new sign, the sign will be empty else
            // it will be the text of the sign that was showing in the world
            ((Sign) tileentitysign).offer(existingSignData.get());
        }
        Sponge.getCauseStackManager().popCause();
        tileentitysign.func_70296_d();
        worldserver.func_184164_w().func_180244_a(blockpos);
    }

    /**
     * @author blood - June 6th, 2016
     * @author gabizou - June 20th, 2016 - Update for 1.9.4 and minor refactors.
     * @reason Since mojang handles creative packets different than survival, we need to
     * restructure this method to prevent any packets being sent to client as we will
     * not be able to properly revert them during drops.
     *
     * @param packetIn The creative inventory packet
     */
    @Overwrite
    public void processCreativeInventoryAction(final CPacketCreativeInventoryAction packetIn) {
        PacketThreadUtil.func_180031_a(packetIn, (NetHandlerPlayServer) (Object) this, this.player.func_71121_q());

        if (this.player.field_71134_c.func_73083_d()) {
            final PacketContext<?> context = (PacketContext<?>) PhaseTracker.getInstance().getCurrentContext();
            final boolean ignoresCreative = context.getIgnoringCreative();
            final boolean clickedOutside = packetIn.func_149627_c() < 0;
            final ItemStack itemstack = packetIn.func_149625_d();

            if (!itemstack.func_190926_b() && itemstack.func_77942_o() && itemstack.func_77978_p().func_150297_b(Constants.Item.BLOCK_ENTITY_TAG, 10)) {
                final NBTTagCompound nbttagcompound = itemstack.func_77978_p().func_74775_l(Constants.Item.BLOCK_ENTITY_TAG);

                if (nbttagcompound.func_74764_b("x") && nbttagcompound.func_74764_b("y") && nbttagcompound.func_74764_b("z")) {
                    final BlockPos blockpos = new BlockPos(nbttagcompound.func_74762_e("x"), nbttagcompound.func_74762_e("y"), nbttagcompound.func_74762_e("z"));
                    final TileEntity tileentity = this.player.field_70170_p.func_175625_s(blockpos);

                    if (tileentity != null) {
                        final NBTTagCompound nbttagcompound1 = new NBTTagCompound();
                        tileentity.func_189515_b(nbttagcompound1);
                        nbttagcompound1.func_82580_o("x");
                        nbttagcompound1.func_82580_o("y");
                        nbttagcompound1.func_82580_o("z");
                        itemstack.func_77983_a(Constants.Item.BLOCK_ENTITY_TAG, nbttagcompound1);
                    }
                }
            }

            final boolean clickedInsideNotOutput = packetIn.func_149627_c() >= 1 && packetIn.func_149627_c() <= 45;
            final boolean itemValidCheck = itemstack.func_190926_b() || itemstack.func_77960_j() >= 0 && itemstack.func_190916_E() <= itemstack.func_77976_d() && !itemstack.func_190926_b();

            // Sponge start - handle CreativeInventoryEvent
            if (itemValidCheck) {
                if (!ignoresCreative) {
                    final ClickInventoryEvent.Creative clickEvent = SpongeCommonEventFactory.callCreativeClickInventoryEvent(this.player, packetIn);
                    if (clickEvent.isCancelled()) {
                        // Reset slot on client
                        if (packetIn.func_149627_c() >= 0 && packetIn.func_149627_c() < this.player.field_71069_bz.field_75151_b.size()) {
                            this.player.field_71135_a.func_147359_a(
                                    new SPacketSetSlot(this.player.field_71069_bz.field_75152_c, packetIn.func_149627_c(),
                                            this.player.field_71069_bz.func_75139_a(packetIn.func_149627_c()).func_75211_c()));
                            this.player.field_71135_a.func_147359_a(new SPacketSetSlot(-1, -1, ItemStack.field_190927_a));
                        }
                        return;
                    }
                }

                if (clickedInsideNotOutput) {
                    if (itemstack.func_190926_b()) {
                        this.player.field_71069_bz.func_75141_a(packetIn.func_149627_c(), ItemStack.field_190927_a);
                    } else {
                        this.player.field_71069_bz.func_75141_a(packetIn.func_149627_c(), itemstack);
                    }

                    this.player.field_71069_bz.func_75128_a(this.player, true);
                } else if (clickedOutside && this.itemDropThreshold < 200) {
                    this.itemDropThreshold += 20;
                    final EntityItem entityitem = this.player.func_71019_a(itemstack, true);

                    if (entityitem != null)
                    {
                        entityitem.func_70288_d();
                    }
                }
            }
            // Sponge end
        }
    }

    @Inject(method = "processClickWindow", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/IntHashMap;addKey(ILjava/lang/Object;)V"))
    private void impl$updateOpenContainer(final CPacketClickWindow packet, final CallbackInfo ci) {
        // We want to treat an 'invalid' click just like a regular click - we still fire events, do restores, etc.

        // Vanilla doesn't call detectAndSendChanges for 'invalid' clicks, since it restores the entire inventory
        // Passing 'captureOnly' as 'true' allows capturing to happen for event firing, but doesn't send any pointless packets
        ((ContainerBridge) this.player.field_71070_bA).bridge$detectAndSendChanges(true);
    }

    @Redirect(method = "processChatMessage",
        at = @At(
            value = "INVOKE",
            target = "Lorg/apache/commons/lang3/StringUtils;normalizeSpace(Ljava/lang/String;)Ljava/lang/String;",
            remap = false))
    private String impl$provideinputNoNormalization(final String input) {
        return input;
    }

    @Inject(method = "setPlayerLocation(DDDFFLjava/util/Set;)V", at = @At(value = "RETURN"))
    private void impl$setTeleported(
        final double x, final double y, final double z, final float yaw, final float pitch, final Set<?> relativeSet, final CallbackInfo ci) {
        this.impl$justTeleported = true;
    }

    /**
     * @author gabizou - June 22nd, 2016
     * @reason Sponge has to throw the movement events before we consider moving the player and there's
     * no clear way to go about it with the target position being null and the last position update checks.
     * @param packetIn
     */
    @Redirect(method = "processPlayer", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/EntityPlayerMP;queuedEndExit:Z"))
    private boolean throwMoveEvent(final EntityPlayerMP playerMP, final CPacketPlayer packetIn) {
        if (!playerMP.field_71136_j) {

            // During login, minecraft sends a packet containing neither the 'moving' or 'rotating' flag set - but only once.
            // We don't fire an event to avoid confusing plugins.
            if (!packetIn.field_149480_h && !packetIn.field_149481_i) {
                return playerMP.field_71136_j;
            }

            // Sponge Start - Movement event
            final Player player = (Player) this.player;
            final EntityPlayerMPBridge mixinPlayer = (EntityPlayerMPBridge) this.player;
            final Vector3d fromRotation = player.getRotation();

            // If Sponge used the player's current location, the delta might never be triggered which could be exploited
            Location<World> fromLocation = player.getLocation();
            if (this.impl$lastMoveLocation != null) {
                fromLocation = this.impl$lastMoveLocation;
            }

            Location<World> toLocation = new Location<>(player.getWorld(), packetIn.field_149479_a, packetIn.field_149477_b, packetIn.field_149478_c);
            Vector3d toRotation = new Vector3d(packetIn.field_149473_f, packetIn.field_149476_e, 0);

            // If we have zero movement, we have rotation only, we might as well note that now.
            final boolean zeroMovement = !packetIn.field_149480_h || toLocation.getPosition().equals(fromLocation.getPosition());

            // Minecraft does the same with rotation when it's only a positional update
            // Branch executed for CPacketPlayer.Position
            boolean firePositionEvent = packetIn.field_149480_h && !packetIn.field_149481_i;
            if (firePositionEvent) {
                // Correct the new rotation to match the old rotation
                toRotation = fromRotation;

                firePositionEvent = !zeroMovement && ShouldFire.MOVE_ENTITY_EVENT_POSITION;
            }

            // Minecraft sends a 0, 0, 0 position when rotation only update occurs, this needs to be recognized and corrected
            // Branch executed for CPacketPlayer.Rotation
            boolean fireRotationEvent = !packetIn.field_149480_h && packetIn.field_149481_i;

            if (fireRotationEvent) {
                // Correct the to location so it's not misrepresented to plugins, only when player rotates without moving
                // In this case it's only a rotation update, which isn't related to the to location
                fromLocation = player.getLocation();
                toLocation = fromLocation;

                fireRotationEvent = ShouldFire.ROTATE_ENTITY_EVENT;
            }

            // Branch executed for CPacketPlayer.PositionRotation
            if (packetIn.field_149480_h && packetIn.field_149481_i) {
                firePositionEvent = !zeroMovement && ShouldFire.MOVE_ENTITY_EVENT_POSITION;
                fireRotationEvent = ShouldFire.ROTATE_ENTITY_EVENT;
            }

            mixinPlayer.bridge$setVelocityOverride(toLocation.getPosition().sub(fromLocation.getPosition()));

            // These magic numbers are sad but help prevent excessive lag from this event.
            // eventually it would be nice to not have them
            final boolean significantMovement =
                    !zeroMovement && toLocation.getPosition().distanceSquared(fromLocation.getPosition())  > ((1f / 16) * (1f / 16));
            final boolean significantRotation = fromRotation.distanceSquared(toRotation) > (.15f * .15f);

            if (significantMovement || significantRotation) {
                final Transform<World> fromTransform = player.getTransform().setLocation(fromLocation).setRotation(fromRotation);
                Transform<World> toTransform = player.getTransform().setLocation(toLocation).setRotation(toRotation);
                final Transform<World> originalToTransform = toTransform;

                // We should only have fireRotationEvent set to true only if there is no movement and so we are not
                // firing the MoveEntityEvent.Position event anyway. Otherwise, there would be a bug (pointed out in
                // https://github.com/SpongePowered/SpongeCommon/pull/2373#issuecomment-541351230) where the rotate event will be
                // fired if ShouldFire.MOVE_ENTITY_EVENT_POSITION is false and the event would normally be the MoveEntityEvent.Position,
                // rather than a RotateEntityEvent.
                //
                // Note that, as the code is written above, if there is a significant rotation but NOT a significant movement (but still
                // non-zero), then a MoveEntityEvent.Position will be fired, not a rotation event, as some movement is still involved.
                //
                // See the API javadocs for RotateEntityEvent for this restriction.
                fireRotationEvent = fireRotationEvent && zeroMovement;

                if (fireRotationEvent || firePositionEvent) {
                    final Event event;
                    if (fireRotationEvent) {
                        event = SpongeEventFactory.createRotateEntityEvent(Sponge.getCauseStackManager().getCurrentCause(), fromTransform, toTransform, player);
                    } else {
                        event = SpongeEventFactory.createMoveEntityEventPosition(Sponge.getCauseStackManager().getCurrentCause(), fromTransform, toTransform, player);
                    }
                    if (SpongeImpl.postEvent(event)) {
                        ((EntityBridge) mixinPlayer).bridge$setLocationAndAngles(fromTransform);
                        this.impl$lastMoveLocation = fromLocation;
                        mixinPlayer.bridge$setVelocityOverride(null);
                        return true;
                    }
                    if (fireRotationEvent) {
                        toTransform = ((RotateEntityEvent) event).getToTransform();
                    } else {
                        toTransform = ((MoveEntityEvent) event).getToTransform();
                    }
                }
                if (!toTransform.equals(originalToTransform)) {
                    ((EntityBridge) mixinPlayer).bridge$setLocationAndAngles(toTransform);
                    this.impl$lastMoveLocation = toTransform.getLocation();
                    mixinPlayer.bridge$setVelocityOverride(null);
                    return true;
                } else if (!fromTransform.getLocation().equals(player.getLocation()) && this.impl$justTeleported) {
                    this.impl$lastMoveLocation = player.getLocation();
                    // Prevent teleports during the move event from causing odd behaviors
                    this.impl$justTeleported = false;
                    mixinPlayer.bridge$setVelocityOverride(null);
                    return true;
                } else {
                    this.impl$lastMoveLocation = toTransform.getLocation();
                }
                this.bridge$resendLatestResourcePackRequest();
            }
        }
        return playerMP.field_71136_j;
    }

    @Inject(
            method = "handleSpectate",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/entity/Entity;world:Lnet/minecraft/world/World;",
                    ordinal = 0
            ),
            locals = LocalCapture.CAPTURE_FAILHARD,
            cancellable = true
    )
    private void impl$onSpectateTeleportCallMoveEvent(CPacketSpectate packetIn, CallbackInfo ci, Entity spectatingEntity) {
        final MoveEntityEvent.Teleport event = EntityUtil.handleDisplaceEntityTeleportEvent(
                this.player,
                spectatingEntity.field_70165_t,
                spectatingEntity.field_70163_u,
                spectatingEntity.field_70161_v,
                spectatingEntity.field_70177_z,
                spectatingEntity.field_70125_A);
        if (event.isCancelled()) {
            ci.cancel();
        } else {
            this.impl$spectatingTeleportLocation = event.getToTransform();
        }
    }

    @Redirect(method = "handleSpectate", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;world:Lnet/minecraft/world/World;", ordinal = 1))
    private net.minecraft.world.World impl$onSpectateGetEntityWorld(Entity entity) {
        return (net.minecraft.world.World) this.impl$spectatingTeleportLocation.getExtent();
    }

    @Inject(method = "handleSpectate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayerMP;getServerWorld()Lnet/minecraft/world/WorldServer;", ordinal = 1), cancellable = true)
    private void impl$cancelIfSameWorld(CallbackInfo ci) {
        //noinspection ConstantConditions
        if (this.player.func_71121_q() == (WorldServer) this.impl$spectatingTeleportLocation.getExtent()) {
            final Vector3d position = this.impl$spectatingTeleportLocation.getPosition();
            this.impl$spectatingTeleportLocation = null;
            player.func_70634_a(position.getX(), position.getY(), position.getZ());
            ci.cancel();
        }
    }

    @Redirect(method = "handleSpectate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayerMP;setLocationAndAngles(DDDFF)V"))
    private void impl$onSpectateLocationAndAnglesUpdate(EntityPlayerMP player, double x, double y, double z, float yaw, float pitch) {
        //noinspection ConstantConditions
        player.field_71093_bK = ((WorldServer) this.impl$spectatingTeleportLocation.getExtent()).field_73011_w.func_186058_p().func_186068_a();
        final Vector3d position = this.impl$spectatingTeleportLocation.getPosition();
        player.func_70012_b(
                position.getX(), position.getY(), position.getZ(),
                (float) this.impl$spectatingTeleportLocation.getYaw(),
                (float) this.impl$spectatingTeleportLocation.getPitch()
        );
    }

    @Redirect(method = "handleSpectate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayerMP;setPositionAndUpdate(DDD)V"))
    private void impl$onSpectatePositionUpdate(EntityPlayerMP player, double x, double y, double z) {
        //noinspection ConstantConditions
        final Vector3d position = this.impl$spectatingTeleportLocation.getPosition();
        player.func_70634_a(position.getX(), position.getY(), position.getZ());
        this.impl$spectatingTeleportLocation = null;
    }

    /**
     * @author gabizou - June 22nd, 2016
     * @author blood - May 6th, 2017
     * @reason Redirects the {@link Entity#getLowestRidingEntity()} call to throw our
     * {@link MoveEntityEvent}. The peculiarity of this redirect is that the entity
     * returned is perfectly valid to be {@link this#player} since, if the player
     * is NOT riding anything, the lowest riding entity is themselves. This way, if
     * the event is cancelled, the player can be returned instead of the actual riding
     * entity.
     *
     * @param playerMP The player
     * @param packetIn The packet movement
     * @return The lowest riding entity
     */
    @Redirect(method = "processVehicleMove", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayerMP;getLowestRidingEntity()Lnet/minecraft/entity/Entity;"))
    private Entity processVehicleMoveEvent(final EntityPlayerMP playerMP, final CPacketVehicleMove packetIn) {
        final Entity ridingEntity = this.player.func_184208_bv();
        if (ridingEntity == this.player || ridingEntity.func_184179_bs() != this.player || ridingEntity != this.lowestRiddenEnt) {
            return ridingEntity;
        }

        // Sponge Start - Movement event
        final org.spongepowered.api.entity.Entity spongeEntity = (org.spongepowered.api.entity.Entity) ridingEntity;
        final Vector3d fromrot = spongeEntity.getRotation();

        final Location<World> from = spongeEntity.getLocation();
        final Vector3d torot = new Vector3d(packetIn.func_187005_e(), packetIn.func_187006_d(), 0);
        final Location<World> to = new Location<>(spongeEntity.getWorld(), packetIn.func_187004_a(), packetIn.func_187002_b(), packetIn.func_187003_c());
        final Transform<World> fromTransform = spongeEntity.getTransform().setLocation(from).setRotation(fromrot);
        final Transform<World> toTransform = spongeEntity.getTransform().setLocation(to).setRotation(torot);
        final MoveEntityEvent event = SpongeEventFactory.createMoveEntityEvent(Sponge.getCauseStackManager().getCurrentCause(), fromTransform, toTransform, (Player) this.player);
        SpongeImpl.postEvent(event);
        if (event.isCancelled()) {
            // There is no need to change the current riding entity position as it hasn't changed yet.
            // Send packet to client in order to update rider position.
            this.netManager.func_179290_a(new SPacketMoveVehicle(ridingEntity));
            return this.player;
        }
        return ridingEntity;
    }


    @Redirect(method = "onDisconnect", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/management/PlayerList;sendMessage(Lnet/minecraft/util/text/ITextComponent;)V"))
    public void onDisconnectHandler(final PlayerList this$0, final ITextComponent component) {
        // If this happens, the connection has not been fully established yet so we've kicked them during ClientConnectionEvent.Login,
        // but FML has created this handler earlier to send their handshake. No message should be sent, no disconnection event should
        // be fired either.
        if (this.player.field_71135_a == null) {
            return;
        }
        final Player player = ((Player) this.player);
        final Text message = SpongeTexts.toText(component);
        final MessageChannel originalChannel = player.getMessageChannel();
        Sponge.getCauseStackManager().pushCause(player);
        final ClientConnectionEvent.Disconnect event = SpongeEventFactory.createClientConnectionEventDisconnect(
                Sponge.getCauseStackManager().getCurrentCause(), originalChannel, Optional.of(originalChannel), new MessageEvent.MessageFormatter(message),
                player, false
        );
        SpongeImpl.postEvent(event);
        Sponge.getCauseStackManager().popCause();
        if (!event.isMessageCancelled()) {
            event.getChannel().ifPresent(channel -> channel.send(player, event.getMessage()));
        }
        ((EntityPlayerMPBridge) this.player).bridge$getWorldBorderListener().onPlayerDisconnect();
    }

    @Redirect(method = "processTryUseItemOnBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/management/PlayerInteractionManager;processRightClickBlock(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/EnumHand;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/EnumFacing;FFF)Lnet/minecraft/util/EnumActionResult;"))
    private EnumActionResult impl$checkState(final PlayerInteractionManager interactionManager, final EntityPlayer player, final net.minecraft.world.World worldIn, @Nullable final ItemStack stack, final EnumHand hand, final BlockPos pos, final EnumFacing facing, final float hitX, final float hitY, final float hitZ) {
        final EnumActionResult actionResult = interactionManager.func_187251_a(this.player, worldIn, stack, hand, pos, facing, hitX, hitY, hitZ);
        if (PhaseTracker.getInstance().getCurrentContext().isEmpty()) {
            return actionResult;
        }
        final PacketContext<?> context = ((PacketContext<?>) PhaseTracker.getInstance().getCurrentContext());

        // If a plugin or mod has changed the item, avoid restoring
        if (!context.getInteractItemChanged()) {
            final ItemStack itemStack = ItemStackUtil.toNative(context.getItemUsed());

            // Only do a restore if something actually changed. The client does an identity check ('==')
            // to determine if it should continue using an itemstack. If we always resend the itemstack, we end up
            // cancelling item usage (e.g. eating food) that occurs while targeting a block
            if (!ItemStack.func_77989_b(itemStack, player.func_184586_b(hand)) && ((PlayerInteractionManagerBridge) this.player.field_71134_c).bridge$isInteractBlockRightClickCancelled()) {
                PacketPhaseUtil.handlePlayerSlotRestore((EntityPlayerMP) player, itemStack, hand);
            }
        }
        context.interactItemChanged(false);
        ((PlayerInteractionManagerBridge) this.player.field_71134_c).bridge$setInteractBlockRightClickCancelled(false);
        return actionResult;
    }

    @Redirect(method = "processTryUseItem",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/management/PlayerInteractionManager;processRightClick(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/EnumHand;)Lnet/minecraft/util/EnumActionResult;"))
    private EnumActionResult impl$checkStateAfter(final PlayerInteractionManager interactionManager, final EntityPlayer player, final net.minecraft.world.World worldIn, @Nullable final ItemStack stack, final EnumHand hand) {
        final EnumActionResult actionResult = interactionManager.func_187250_a(this.player, worldIn, stack, hand);
        // If a plugin or mod has changed the item, avoid restoring
        if (PhaseTracker.getInstance().getCurrentContext().isEmpty()) {
            return actionResult;
        }
        final PacketContext<?> packetContext = (PacketContext<?>) PhaseTracker.getInstance().getCurrentContext();
        if (!packetContext.getInteractItemChanged()) {
            final ItemStack itemStack = ItemStackUtil.toNative(packetContext.getItemUsed());

            // Only do a restore if something actually changed. The client does an identity check ('==')
            // to determine if it should continue using an itemstack. If we always resend the itemstack, we end up
            // cancelling item usage (e.g. eating food) that occurs while targeting a block
            if (!ItemStack.func_77989_b(itemStack, player.func_184586_b(hand))  && ((PlayerInteractionManagerBridge) this.player.field_71134_c).bridge$isInteractBlockRightClickCancelled()) {
                PacketPhaseUtil.handlePlayerSlotRestore((EntityPlayerMP) player, itemStack, hand);
            }
        }
        packetContext.interactItemChanged(false);
        ((PlayerInteractionManagerBridge) this.player.field_71134_c).bridge$setInteractBlockRightClickCancelled(false);
        return actionResult;
    }

    @Nullable
    @Redirect(method = "processPlayerDigging", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayerMP;dropItem(Z)Lnet/minecraft/entity/item/EntityItem;"))
    private EntityItem impl$performDropThroughPhase(final EntityPlayerMP player, final boolean dropAll) {
        EntityItem item = null;
        final ItemStack stack = this.player.field_71071_by.func_70448_g();
        if (!stack.func_190926_b()) {
            final int size = stack.func_190916_E();
            item = this.player.func_71040_bB(dropAll);
            // force client itemstack update if drop event was cancelled
            if (item == null && ((EntityPlayerBridge) player).bridge$shouldRestoreInventory()) {
                final Slot slot = this.player.field_71070_bA.func_75147_a(this.player.field_71071_by, this.player.field_71071_by.field_70461_c);
                final int windowId = this.player.field_71070_bA.field_75152_c;
                stack.func_190920_e(size);
                this.sendPacket(new SPacketSetSlot(windowId, slot.field_75222_d, stack));
            }
        }

        return item;
    }

    @Inject(method = "handleAnimation",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayerMP;markPlayerActive()V"), cancellable = true)
    private void impl$throwAnimationEvent(final CPacketAnimation packetIn, final CallbackInfo ci) {
        if (PhaseTracker.getInstance().getCurrentContext().isEmpty()) {
            return;
        }
        SpongeCommonEventFactory.lastAnimationPacketTick = SpongeImpl.getServer().func_71259_af();
        SpongeCommonEventFactory.lastAnimationPlayer = new WeakReference<>(this.player);
        if (ShouldFire.ANIMATE_HAND_EVENT) {
            final HandType handType = (HandType) (Object) packetIn.func_187018_a();
            final ItemStack heldItem = this.player.func_184586_b(packetIn.func_187018_a());
            Sponge.getCauseStackManager().addContext(EventContextKeys.USED_ITEM, ItemStackUtil.snapshotOf(heldItem));
            Sponge.getCauseStackManager().addContext(EventContextKeys.USED_HAND, handType);
            final AnimateHandEvent event =
                SpongeEventFactory.createAnimateHandEvent(Sponge.getCauseStackManager().getCurrentCause(), handType, (Humanoid) this.player);
            if (SpongeImpl.postEvent(event)) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "processPlayerDigging", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getWorld(I)Lnet/minecraft/world/WorldServer;"))
    private void impl$updateLastPrimaryPacket(final CPacketPlayerDigging packetIn, final CallbackInfo ci) {
        if (PhaseTracker.getInstance().getCurrentContext().isEmpty()) {
            return;
        }
        SpongeCommonEventFactory.lastPrimaryPacketTick = SpongeImpl.getServer().func_71259_af();
    }

    @Inject(method = "processPlayerDigging", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayerMP;dropItem(Z)Lnet/minecraft/entity/item/EntityItem;"))
    private void onProcessPlayerDiggingDropItem(final CPacketPlayerDigging packetIn, final CallbackInfo ci) {
        final ItemStack stack = this.player.func_184614_ca();
        if (!stack.func_190926_b()) {
            ((EntityPlayerMPBridge) this.player).bridge$setPacketItem(stack.func_77946_l());
        }
    }

    @Inject(method = "processTryUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getWorld(I)Lnet/minecraft/world/WorldServer;"), cancellable = true)
    private void onProcessTryUseItem(final CPacketPlayerTryUseItem packetIn, final CallbackInfo ci) {
        SpongeCommonEventFactory.lastSecondaryPacketTick = SpongeImpl.getServer().func_71259_af();
        final long packetDiff = System.currentTimeMillis() - this.impl$lastTryBlockPacketTimeStamp;
        // If the time between packets is small enough, use the last result.
        if (packetDiff < 100) {
            // Use previous result and avoid firing a second event
            if (((PlayerInteractionManagerBridge) this.player.field_71134_c).bridge$isLastInteractItemOnBlockCancelled()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "processTryUseItemOnBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getWorld(I)Lnet/minecraft/world/WorldServer;"))
    private void onProcessTryUseItemOnBlockSetCountersForSponge(final CPacketPlayerTryUseItemOnBlock packetIn, final CallbackInfo ci) {
        // InteractItemEvent on block must be handled in PlayerInteractionManager to support item/block results.
        // Only track the timestamps to support our block animation events
        this.impl$lastTryBlockPacketTimeStamp = System.currentTimeMillis();
        SpongeCommonEventFactory.lastSecondaryPacketTick = SpongeImpl.getServer().func_71259_af();

    }

    /**
     * @author blood - April 5th, 2016
     *
     * @reason Due to all the changes we now do for this packet, it is much easier
     * to read it all with an overwrite. Information detailing on why each change
     * was made can be found in comments below.
     *
     * @param packetIn The entity use packet
     */
    @Overwrite
    public void processUseEntity(final CPacketUseEntity packetIn) {
        // Sponge start
        // All packets received by server are handled first on the Netty Thread
        if (!SpongeImpl.getServer().func_152345_ab()) {
            if (packetIn.func_149565_c() == CPacketUseEntity.Action.INTERACT) {
                // This packet is only sent by client when CPacketUseEntity.Action.INTERACT_AT is
                // not successful. We can safely ignore this packet as we handle the INTERACT logic
                // when INTERACT_AT does not return a successful result.
                return;
            } else { // queue packet for main thread
                PacketThreadUtil.func_180031_a(packetIn, (NetHandlerPlayServer) (Object) this, this.player.func_71121_q());
                return;
            }
        }
        // Sponge end

        final WorldServer worldserver = this.server.func_71218_a(this.player.field_71093_bK);
        final Entity entity = packetIn.func_149564_a(worldserver);
        this.player.func_143004_u();

        if (entity != null) {
            final boolean flag = this.player.func_70685_l(entity);
            double d0 = 36.0D; // 6 blocks

            if (!flag) {
                d0 = 9.0D; // 1.5 blocks
            }

            if (this.player.func_70068_e(entity) < d0) {
                // Sponge start - Ignore CPacketUseEntity.Action.INTERACT
                /*if (packetIn.getAction() == CPacketUseEntity.Action.INTERACT) {
                    // The client will only send this packet if INTERACT_AT is not successful.
                    // We can safely ignore this as we handle interactOn below during INTERACT_AT.
                    //EnumHand enumhand = packetIn.getHand();
                    //this.player.interactOn(entity, enumhand);
                } else */
                // Sponge end

                if (packetIn.func_149565_c() == CPacketUseEntity.Action.INTERACT_AT) {

                    // Sponge start - Fire interact events
                    try (final CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
                        final EnumHand hand = packetIn.func_186994_b();
                        final ItemStack itemstack = hand != null ? this.player.func_184586_b(hand) : ItemStack.field_190927_a;

                        SpongeCommonEventFactory.lastSecondaryPacketTick = this.server.func_71259_af();

                        // Is interaction allowed with item in hand
                        if (SpongeCommonEventFactory.callInteractEntityEventSecondary(this.player, itemstack,
                            entity, hand, VecHelper.toVector3d(entity.func_174791_d().func_178787_e(packetIn.func_179712_b()))).isCancelled()) {

                            // Restore held item in hand
                            final int index = ((InventoryPlayerBridge) this.player.field_71071_by).bridge$getHeldItemIndex(hand);

                            if (hand == EnumHand.OFF_HAND) {
                                // A window id of -2 can be used to set the off hand, even if a container is open.
                                sendPacket(new SPacketSetSlot(-2, ((ContainerPlayerBridge) this.player.field_71069_bz).bridge$getOffHandSlot(), itemstack));
                            } else {
                                final Slot slot = this.player.field_71070_bA.func_75147_a(this.player.field_71071_by, index);
                                sendPacket(new SPacketSetSlot(this.player.field_71070_bA.field_75152_c, slot.field_75222_d, itemstack));
                            }


                            // Handle a few special cases where the client assumes that the interaction is successful,
                            // which means that we need to force an update
                            if (itemstack.func_77973_b() == Items.field_151058_ca) {
                                // Detach entity again
                                sendPacket(new SPacketEntityAttach(entity, null));
                            } else {
                                // Other cases may involve a specific DataParameter of the entity
                                // We fix the client state by marking it as dirty so it will be updated on the client the next tick
                                final DataParameter<?> parameter = PacketPhaseUtil.findModifiedEntityInteractDataParameter(itemstack, entity);
                                if (parameter != null) {
                                    entity.func_184212_Q().func_187217_b(parameter);
                                }
                            }

                            return;
                        }

                        // If INTERACT_AT is not successful, run the INTERACT logic
                        if (entity.func_184199_a(this.player, packetIn.func_179712_b(), hand) != EnumActionResult.SUCCESS) {
                            this.player.func_190775_a(entity, hand);
                        }
                    }
                    // Sponge end
                } else if (packetIn.func_149565_c() == CPacketUseEntity.Action.ATTACK) {
                    // Sponge start - Call interact event
                    final EnumHand hand = EnumHand.MAIN_HAND; // Will be null in the packet during ATTACK
                    final ItemStack itemstack = this.player.func_184586_b(hand);
                    SpongeCommonEventFactory.lastPrimaryPacketTick = this.server.func_71259_af();

                    Vector3d hitVec = null;

                    if (packetIn.func_179712_b() == null) {
                        final RayTraceResult result = SpongeImplHooks.rayTraceEyes(this.player, SpongeImplHooks.getBlockReachDistance(this.player));
                        hitVec = result == null ? null : VecHelper.toVector3d(result.field_72307_f);
                    }

                    if (SpongeCommonEventFactory.callInteractItemEventPrimary(this.player, itemstack, hand, hitVec, entity).isCancelled()) {
                        ((EntityPlayerMPBridge) this.player).bridge$restorePacketItem(hand);
                        return;
                    }
                    // Sponge end

                    if (entity instanceof EntityItem || entity instanceof EntityXPOrb || entity instanceof EntityArrow || entity == this.player) {
                        this.disconnect(new TextComponentTranslation("multiplayer.disconnect.invalid_entity_attacked"));
                        this.server.func_71236_h("Player " + this.player.func_70005_c_() + " tried to attack an invalid entity");
                        return;
                    }

                    // Sponge start
                    if (SpongeCommonEventFactory.callInteractEntityEventPrimary(this.player, itemstack, entity, hand, hitVec).isCancelled()) {
                        ((EntityPlayerMPBridge) this.player).bridge$restorePacketItem(hand);
                        return;
                    }
                    // Sponge end

                    this.player.func_71059_n(entity);
                }
            }
        }
    }

    @Override
    public void bridge$setLastMoveLocation(final Location<World> location) {
        this.impl$lastMoveLocation = location;
    }

    @Inject(method = "handleResourcePackStatus(Lnet/minecraft/network/play/client/CPacketResourcePackStatus;)V", at = @At("HEAD"))
    private void onProcessResourcePackStatus(final CPacketResourcePackStatus packet, final CallbackInfo ci) {
        // Propagate the packet to the main thread so the cause tracker picks
        // it up. See PacketThreadUtil_1Mixin.
        PacketThreadUtil.func_180031_a(packet, (INetHandlerPlayServer) this, this.player.func_71121_q());
    }

    @Override
    public void bridge$resendLatestResourcePackRequest() {
        final ResourcePack pack = this.impl$lastReceivedPack;
        if (this.impl$numResourcePacksInTransit.get() > 0 || pack == null) {
            return;
        }
        this.impl$lastReceivedPack = null;
        ((Player) this.player).sendResourcePack(pack);
    }

    @Override
    public ResourcePack bridge$popReceivedResourcePack(final boolean markAccepted) {
        final ResourcePack pack = this.impl$lastReceivedPack;
        this.impl$lastReceivedPack = null;
        if (markAccepted) {
            this.lastAcceptedPack = pack; // TODO do something with the old value
        }
        return pack;
    }

    @Override
    public ResourcePack bridge$popAcceptedResourcePack() {
        final ResourcePack pack = this.lastAcceptedPack;
        this.lastAcceptedPack = null;
        return pack;
    }

    @Override
    public long bridge$getLastTryBlockPacketTimeStamp() {
        return this.impl$lastTryBlockPacketTimeStamp;
    }
}
