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
package org.spongepowered.common.event.tracking.phase.tick;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.common.world.BlockChange;

import javax.annotation.Nullable;

class NoCaptureEntityTickPhaseState extends EntityTickPhaseState {

    NoCaptureEntityTickPhaseState(String name) {
        super(name);
    }

    @Override
    public boolean tracksBlockSpecificDrops() {
        return false;
    }


    @Override
    public boolean doesBulkBlockCapture() {
        return false;
    }

    @Override
    public boolean doesBlockEventTracking() {
        return false;
    }

    @Override
    public boolean alreadyCapturingItemSpawns() {
        return true;
    }

    @Override
    public boolean alreadyCapturingEntitySpawns() {
        return true;
    }

    @Override
    public void handleBlockChangeWithUser(@Nullable BlockChange blockChange, Transaction<BlockSnapshot> snapshotTransaction,
        EntityTickContext context) {
        super.handleBlockChangeWithUser(blockChange, snapshotTransaction, context);
    }

    @Override
    public boolean doesCaptureEntityDrops() {
        return false;
    }

    @Override
    public boolean performOrCaptureItemDrop(EntityTickContext phaseContext, Entity entity, EntityItem entityitem) {
        return false;
    }

    @Override
    protected void processCaptures(org.spongepowered.api.entity.Entity tickingEntity, EntityTickContext phaseContext, CauseStackManager.StackFrame frame) {
        // We didn't perform any capturing, so there's nothing to process
    }
}
