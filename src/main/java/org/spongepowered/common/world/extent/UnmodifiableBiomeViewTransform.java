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
package org.spongepowered.common.world.extent;

import org.spongepowered.api.util.DiscreteTransform3;
import org.spongepowered.api.world.extent.worker.BiomeVolumeWorker;
import org.spongepowered.api.world.volume.biome.ImmutableBiomeVolume;
import org.spongepowered.api.world.volume.biome.UnmodifiableBiomeVolume;
import org.spongepowered.common.util.gen.ByteArrayImmutableBiomeBuffer;
import org.spongepowered.common.world.extent.worker.SpongeBiomeVolumeWorker;
import org.spongepowered.math.vector.Vector3i;

public class UnmodifiableBiomeViewTransform extends AbstractBiomeViewTransform<UnmodifiableBiomeVolume> implements UnmodifiableBiomeVolume {

    public UnmodifiableBiomeViewTransform(UnmodifiableBiomeVolume volume, DiscreteTransform3 transform) {
        super(volume, transform);
    }

    @Override
    public UnmodifiableBiomeVolume getBiomeView(Vector3i newMin, Vector3i newMax) {
        return new UnmodifiableBiomeViewDownsize(this.volume, this.inverseTransform.transform(newMin),
                this.inverseTransform.transform(newMax)).getBiomeView(this.transform);
    }

    @Override
    public UnmodifiableBiomeVolume getBiomeView(DiscreteTransform3 transform) {
        return new UnmodifiableBiomeViewTransform(this.volume, this.transform.withTransformation(transform));
    }

    @Override
    public ImmutableBiomeVolume getImmutableBiomeCopy() {
        return ByteArrayImmutableBiomeBuffer.newWithoutArrayClone(ExtentBufferUtil.copyToArray(this, this.min, this.max, this.size), this.min,
            this.size);
    }

    @Override
    public BiomeVolumeWorker<? extends UnmodifiableBiomeVolume> getBiomeWorker() {
        return new SpongeBiomeVolumeWorker<>(this);
    }

}
