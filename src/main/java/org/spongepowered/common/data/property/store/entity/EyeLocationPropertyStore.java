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
package org.spongepowered.common.data.property.store.entity;

import com.flowpowered.math.vector.Vector3d;
import net.minecraft.entity.Entity;
import org.spongepowered.api.data.property.entity.EyeLocationProperty;
import org.spongepowered.common.data.property.store.common.AbstractEntityPropertyStore;
import org.spongepowered.common.util.VecHelper;

import java.util.Optional;

public class EyeLocationPropertyStore extends AbstractEntityPropertyStore<EyeLocationProperty> {

    @Override
    protected Optional<EyeLocationProperty> getForEntity(Entity entity) {
        final double eyeHeight = entity.getEyeHeight();
        final Vector3d position = VecHelper.toVector3d(entity.getPositionVector());
        final Vector3d eyeLocation = position.add(0, eyeHeight, 0);
        return Optional.of(new EyeLocationProperty(eyeLocation));
    }

}
