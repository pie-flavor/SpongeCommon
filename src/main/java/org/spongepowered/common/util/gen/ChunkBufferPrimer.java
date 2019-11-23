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
package org.spongepowered.common.util.gen;

import net.minecraft.block.Blocks;
import net.minecraft.world.chunk.ChunkPrimer;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.world.volume.block.MutableBlockVolume;
import org.spongepowered.math.vector.Vector3i;

/**
 * Wraps a {@link MutableBlockVolume} within a ChunkPrimer in order to be able
 * to pass the block buffer to vanilla populators. TODO: this should be a mixin
 */
public class ChunkBufferPrimer extends ChunkPrimer {

    private static final net.minecraft.block.BlockState defaultState = Blocks.AIR.getDefaultState();
    private final MutableBlockVolume buffer;
    private final Vector3i min;

    public ChunkBufferPrimer(MutableBlockVolume buffer) {
        this.buffer = buffer;
        this.min = buffer.getBlockMin();
    }

    @Override
    public net.minecraft.block.BlockState getBlockState(int x, int y, int z) {
        return (net.minecraft.block.BlockState) this.buffer.getBlock(this.min.getX() + x, this.min.getY() + y, this.min.getZ() + z);
    }

    @Override
    public void setBlockState(int x, int y, int z, net.minecraft.block.BlockState state) {
        this.buffer.setBlock(this.min.getX() + x, this.min.getY() + y, this.min.getZ() + z, (BlockState) state);
    }

    @Override
    public int findGroundBlockIdx(int x, int z) {
        for (int y = 255; y >= 0; --y) {
            net.minecraft.block.BlockState iblockstate = getBlockState(x, y, z);

            if (iblockstate != null && iblockstate != defaultState) {
                return y;
            }
        }
        return 0;
    }

}
