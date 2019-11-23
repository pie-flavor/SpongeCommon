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
package org.spongepowered.common.world.teleport;

import com.google.common.collect.Sets;
import com.google.inject.Singleton;
import net.minecraft.world.border.WorldBorder;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.TeleportHelper;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.teleport.TeleportHelperFilter;
import org.spongepowered.api.world.teleport.TeleportHelperFilters;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.bridge.world.chunk.ChunkProviderServerBridge;
import org.spongepowered.math.GenericMath;
import org.spongepowered.math.vector.Vector3i;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Singleton
public class SpongeTeleportHelper implements TeleportHelper {

    @Override
    public Optional<Location<World>> getSafeLocation(Location<World> location, int height, int width, int distanceToDrop,
            TeleportHelperFilter filter, TeleportHelperFilter... additionalFilters) {
        final World world = location.getExtent();
        final Set<TeleportHelperFilter> filters = Sets.newHashSet(additionalFilters);
        filters.add(filter);

        if (SpongeImpl.getGlobalConfigAdapter().getConfig().getTeleportHelper().isForceBlacklistOn()) {
            // Always force this into the set if the user has requested it.
            filters.add(TeleportHelperFilters.CONFIG);
        }

        final ChunkProviderServerBridge chunkProviderServer = (ChunkProviderServerBridge)((net.minecraft.world.server.ServerWorld) world).getChunkProvider();
        final boolean previous = chunkProviderServer.bridge$getForceChunkRequests();
        chunkProviderServer.bridge$setForceChunkRequests(true);

        try {
            // Get the vectors to check, and get the block types with them.
            // The vectors should be sorted by distance from the centre of the checking region, so
            // this makes it easier to try to get close, because we can just iterate and get progressively further out.
            Optional<Vector3i> result = getSafeLocation(world, getBlockLocations(location, height, width), distanceToDrop, filters);
            return result.map(vector3i -> new Location<>(world, vector3i.toDouble().add(0.5, 0, 0.5)));
        } finally {
            // Just in case some exception occurs, we want this to disable again.
            chunkProviderServer.bridge$setForceChunkRequests(previous);
        }

    }

    private Stream<Vector3i> getBlockLocations(Location<World> worldLocation, int height, int width) {
        // We don't want to warp outside of the world border, so we want to check that we're within it.
        WorldBorder worldBorder = (WorldBorder) worldLocation.getExtent().getWorldBorder();
        int worldBorderMinX = GenericMath.floor(worldBorder.minX());
        int worldBorderMinZ = GenericMath.floor(worldBorder.minZ());
        int worldBorderMaxX = GenericMath.floor(worldBorder.maxX());
        int worldBorderMaxZ = GenericMath.floor(worldBorder.maxZ());

        // Get the World and get the maximum Y value.
        int worldMaxY = worldLocation.getExtent().getBlockMax().getY();

        Vector3i vectorLocation = worldLocation.getBlockPosition();

        // We use clamp to remain within the world confines, so we don't waste time checking blocks outside of the
        // world border and the world height.
        int minY = GenericMath.clamp(vectorLocation.getY() - height, 0, worldMaxY);
        int maxY = GenericMath.clamp(vectorLocation.getY() + height, 0, worldMaxY);

        int minX = GenericMath.clamp(vectorLocation.getX() - width, worldBorderMinX, worldBorderMaxX);
        int maxX = GenericMath.clamp(vectorLocation.getX() + width, worldBorderMinX, worldBorderMaxX);

        int minZ = GenericMath.clamp(vectorLocation.getZ() - width, worldBorderMinZ, worldBorderMaxZ);
        int maxZ = GenericMath.clamp(vectorLocation.getZ() + width, worldBorderMinZ, worldBorderMaxZ);

        // We now iterate over all possible x, y and z positions to get all possible vectors.
        List<Vector3i> vectors = new ArrayList<>();
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    vectors.add(new Vector3i(x, y, z));
                }
            }
        }

        Comparator<Vector3i> c = Comparator.comparingInt(vectorLocation::distanceSquared);

        // The compiler seems to need this to be a new line.
        // We check to see what the y location is, preferring changes in Y over X and Z, and higher over lower locations.
        c = c.thenComparing(x -> -Math.abs(vectorLocation.getY() - x.getY())).thenComparing(x -> -x.getY());

        // Sort them according to the distance to the provided worldLocation.
        return vectors.stream().sorted(c);
    }

    private Optional<Vector3i> getSafeLocation(World world, Stream<Vector3i> positionsToCheck, int floorDistanceCheck,
            Collection<TeleportHelperFilter> filters) {
        // We cache the various block lookup results so we don't check a block twice.
        final Map<Vector3i, BlockData> blockCache = new HashMap<>();

        return positionsToCheck.filter(currentTarget -> {
            List<TeleportHelperFilter> undefinedResults = new ArrayList<>();
            for (TeleportHelperFilter filter : filters) {
                // If any return Tristate.FALSE, we're not safe.
                Tristate isValid = filter.isValidLocation(world, currentTarget);
                if (isValid == Tristate.FALSE) {
                    // Completely fails the AND check at this point.
                    return false;
                }

                if (isValid == Tristate.UNDEFINED) {
                    undefinedResults.add(filter);
                }
            }

            // If we don't have any undefined results, then we return true here.
            if (undefinedResults.isEmpty()) {
                return true;
            }

            // Get the block, add it to the cache.
            BlockData block = getBlockData(currentTarget, world, blockCache, undefinedResults);

            // If the block isn't safe, no point in continuing on this run.
            if (block.isSafeBody) {

                // Check the block ABOVE is safe for the body, and the two BELOW are safe too.
                if (getBlockData(
                    currentTarget.add(0, 1, 0), world, blockCache, undefinedResults).isSafeBody
                        && (floorDistanceCheck <= 0 || isFloorSafe(currentTarget, world, blockCache, undefinedResults, floorDistanceCheck))) {

                    // This position should be safe. Get the center of the block to spawn into.
                    return true;
                }
            }

            return false;
        }).findFirst();
    }

    private boolean isFloorSafe(Vector3i currentTarget, World world, Map<Vector3i, BlockData> blockCache, Collection<TeleportHelperFilter> filters,
            int floorDistanceCheck) {
        for (int i = 1; i < floorDistanceCheck; ++i) {
            BlockData data = getBlockData(currentTarget.sub(0, i, 0), world, blockCache, filters);

            // If it's a safe floor, we can just say yes now.
            if (data.isSafeFloor) {
                return true;
            }

            // If it's not safe for the body, then we don't want to go through it anyway.
            if (!data.isSafeBody) {
                return false;
            }
        }

        // Check the next block down, if it's a floor, then we're good to go, otherwise we'd fall too far for our liking.
        return getBlockData(currentTarget.sub(0, floorDistanceCheck, 0), world, blockCache, filters).isSafeFloor;
    }

    private BlockData getBlockData(Vector3i vector3i, World world, Map<Vector3i, BlockData> cache, Collection<TeleportHelperFilter> filters) {
        if (vector3i.getY() < 0) {
            // Anything below this isn't safe, no point going further.
            return new BlockData();
        }

        if (cache.containsKey(vector3i)) {
            return cache.get(vector3i);
        }

        BlockData data = new BlockData(world.getBlock(vector3i), filters);
        cache.put(vector3i, data);
        return data;
    }

    private class BlockData {

        private final boolean isSafeFloor;
        private final boolean isSafeBody;

        private BlockData() {
            this.isSafeFloor = false;
            this.isSafeBody = false;
        }

        private BlockData(BlockState blockState, Collection<TeleportHelperFilter> filters) {
            this.isSafeFloor = filters.stream().allMatch(x -> x.isSafeFloorMaterial(blockState));
            this.isSafeBody = filters.stream().allMatch(x -> x.isSafeBodyMaterial(blockState));
        }

    }

}
