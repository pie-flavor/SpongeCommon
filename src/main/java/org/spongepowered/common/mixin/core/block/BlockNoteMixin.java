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
package org.spongepowered.common.mixin.core.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.NoteBlock;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.tileentity.Note;
import org.spongepowered.api.data.property.block.InstrumentProperty;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.sound.PlaySoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.common.event.ShouldFire;
import org.spongepowered.common.event.SpongeCommonEventFactory;
import org.spongepowered.common.registry.type.NotePitchRegistryModule;

@Mixin(NoteBlock.class)
public abstract class BlockNoteMixin extends BlockMixin {


    @Shadow protected abstract SoundEvent getInstrument(int eventId);

    @SuppressWarnings("ConstantConditions")
    @Inject(method = "eventReceived(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;II)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playSound(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/SoundEvent;Lnet/minecraft/util/SoundCategory;FF)V"),
            cancellable = true)
    private void impl$throwNoteBlockSoundEvent(BlockState state, World worldIn, BlockPos pos, int id, int param, CallbackInfoReturnable<Boolean> callbackInfo) {
        if (!ShouldFire.PLAY_SOUND_EVENT_NOTE_BLOCK) {
            return;
        }

        //No noteblock sounds if the block above it isn't air
        if (worldIn.func_180495_p(pos.func_177984_a()).func_185904_a() != Material.field_151579_a) {
            return;
        }

        //InstrumentProperty doesn't return what we wan't for the noteblock directly, so we have to check the block under it.
        InstrumentProperty instrumentProperty = ((org.spongepowered.api.world.World) worldIn).getBlock(pos.func_177958_n(), pos.func_177956_o() - 1, pos.func_177952_p()).getProperty(InstrumentProperty.class).orElse(null);
        if (instrumentProperty == null) {
            return;
        }

        TileEntity tileEntity = worldIn.func_175625_s(pos);
        if (!(tileEntity instanceof Note)) {
            return;
        }

        float pitch = (float) Math.pow(2.0D, (double) (param - 12) / 12.0D);

        try (final CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            final PlaySoundEvent.NoteBlock event = SpongeCommonEventFactory.callPlaySoundNoteBLockEvent(frame.getCurrentCause(), (org.spongepowered.api.world.World) worldIn, (Note) tileEntity, pos, this.getInstrument(id), instrumentProperty.getValue(), NotePitchRegistryModule.getPitch((byte) param), pitch);
            if (event.isCancelled()) {
                callbackInfo.setReturnValue(true);
            }
        }
    }


}
