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
package org.spongepowered.common.mixin.core.command.server;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.command.server.CommandTeleport;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SPlayerPositionLookPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.cause.entity.teleport.TeleportTypes;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.common.entity.EntityUtil;
import org.spongepowered.math.vector.Vector3d;
import java.util.EnumSet;
import java.util.Set;

@Mixin(CommandTeleport.class)
public abstract class CommandTeleportMixin extends CommandBase {

    // This boolean is added in order to make minimal changes to 'execute'.
    // It is set to true if the events fired in 'func_189863_a' are not cancelled.
    // This allows us to prevent calling 'notifyCommandListener' if the event is cancelled.
    private static boolean impl$shouldNotifyCommandListener = false;

    /**
     * @author Aaron1011 - August 15, 2016
     * @reason Prevent 'notifyCommandListener' from being called the event is cancelled
     */
    @Overwrite
    @Override
    public void execute(final MinecraftServer server, final ICommandSender sender, final String[] args) throws CommandException
    {
        if (args.length < 4)
        {
            throw new WrongUsageException("commands.teleport.usage", new Object[0]);
        }
        else
        {
            final Entity entity = getEntity(server, sender, args[0]);

            if (entity.world != null)
            {
                // int i = 4096;
                final Vec3d vec3d = sender.getPositionVector();
                int j = 1;
                final CommandBase.CoordinateArg commandbase$coordinatearg = parseCoordinate(vec3d.x, args[j++], true);
                final CommandBase.CoordinateArg commandbase$coordinatearg1 = parseCoordinate(vec3d.y, args[j++], -4096, 4096, false);
                final CommandBase.CoordinateArg commandbase$coordinatearg2 = parseCoordinate(vec3d.z, args[j++], true);
                final Entity entity1 = sender.getCommandSenderEntity() == null ? entity : sender.getCommandSenderEntity();
                final CommandBase.CoordinateArg commandbase$coordinatearg3 = parseCoordinate(args.length > j ? (double)entity1.rotationYaw : (double)entity.rotationYaw, args.length > j ? args[j] : "~", false);
                ++j;
                final CommandBase.CoordinateArg commandbase$coordinatearg4 = parseCoordinate(args.length > j ? (double)entity1.rotationPitch : (double)entity.rotationPitch, args.length > j ? args[j] : "~", false);
                // Sponge start - check impl$shouldNotifyCommandListener before calling 'notifyCommandListener'

                // Guard against any possible re-entrance
                final boolean shouldNotify = impl$shouldNotifyCommandListener;

                doTeleport(entity, commandbase$coordinatearg, commandbase$coordinatearg1, commandbase$coordinatearg2, commandbase$coordinatearg3, commandbase$coordinatearg4);
                if (impl$shouldNotifyCommandListener) {
                    notifyCommandListener(sender, this, "commands.tp.success.coordinates", new Object[] {entity.getName(), Double.valueOf(commandbase$coordinatearg.getResult()), Double.valueOf(commandbase$coordinatearg1.getResult()), Double.valueOf(commandbase$coordinatearg2.getResult())});
                }
                impl$shouldNotifyCommandListener = shouldNotify;
                // Sponge end
            }
        }
    }

    /**
     * @author Aaron1011 - August 15, 2016
     * @reason Muliple modification points are needed, so an overwrite is easier
     */
    @Overwrite
    private static void doTeleport(final Entity p_189862_0_, final CommandBase.CoordinateArg p_189862_1_, final CommandBase.CoordinateArg p_189862_2_, final CommandBase.CoordinateArg p_189862_3_, final CommandBase.CoordinateArg p_189862_4_, final CommandBase.CoordinateArg p_189862_5_)
    {
        if (p_189862_0_ instanceof ServerPlayerEntity)
        {
            final Set<SPlayerPositionLookPacket.Flags> set = EnumSet.<SPlayerPositionLookPacket.Flags>noneOf(SPlayerPositionLookPacket.Flags.class);
            float f = (float)p_189862_4_.getAmount();

            if (p_189862_4_.isRelative())
            {
                set.add(SPlayerPositionLookPacket.Flags.Y_ROT);
            }
            else
            {
                f = MathHelper.wrapDegrees(f);
            }

            float f1 = (float)p_189862_5_.getAmount();

            if (p_189862_5_.isRelative())
            {
                set.add(SPlayerPositionLookPacket.Flags.X_ROT);
            }
            else
            {
                f1 = MathHelper.wrapDegrees(f1);
            }

            // Sponge start
            final ServerPlayerEntity player = (ServerPlayerEntity) p_189862_0_;
            final double x = p_189862_1_.getAmount();
            final double y = p_189862_2_.getAmount();
            final double z = p_189862_3_.getAmount();
            try (final CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
                frame.addContext(EventContextKeys.TELEPORT_TYPE, TeleportTypes.COMMAND);
                final MoveEntityEvent.Teleport event = EntityUtil.handleDisplaceEntityTeleportEvent(player, x, y, z, f, f1);
                if (event.isCancelled()) {
                    return;
                }

                p_189862_0_.stopRiding();
                final Vector3d position = event.getToTransform().getPosition();
                ((ServerPlayerEntity)p_189862_0_).connection.setPlayerLocation(position.getX(), position.getY(), position.getZ(), (float) event.getToTransform().getYaw(), (float) event.getToTransform().getPitch(), set);
                p_189862_0_.setRotationYawHead((float) event.getToTransform().getYaw());
            }
            // Sponge end
        }
        else
        {
            final float f2 = (float)MathHelper.wrapDegrees(p_189862_4_.getResult());
            float f3 = (float)MathHelper.wrapDegrees(p_189862_5_.getResult());
            f3 = MathHelper.clamp(f3, -90.0F, 90.0F);

            // Sponge start
            final double x = p_189862_1_.getResult();
            final double y = p_189862_2_.getResult();
            final double z = p_189862_3_.getResult();
            try (final CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
                frame.addContext(EventContextKeys.TELEPORT_TYPE, TeleportTypes.COMMAND);
                final MoveEntityEvent.Teleport event = EntityUtil.handleDisplaceEntityTeleportEvent(p_189862_0_, x, y, z, f2, f3);
                if (event.isCancelled()) {
                    return;
                }

                final Vector3d position = event.getToTransform().getPosition();
                p_189862_0_.setLocationAndAngles(position.getX(), position.getY(), position.getZ(), (float) event.getToTransform().getYaw(), (float) event.getToTransform().getPitch());
                p_189862_0_.setRotationYawHead((float) event.getToTransform().getYaw());
            }
            // Sponge end
        }

        if (!(p_189862_0_ instanceof LivingEntity) || !((LivingEntity)p_189862_0_).isElytraFlying())
        {
            p_189862_0_.motionY = 0.0D;
            p_189862_0_.onGround = true;
        }

        // Sponge start - set 'impl$shouldNotifyCommandListener' to 'true' if we make it to the end of the method (the event wasn't cancelled)
        impl$shouldNotifyCommandListener = true;
        // Sponge end
    }

}
