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
package org.spongepowered.common.mixin.core.world.storage;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.util.datafix.FixTypes;
import net.minecraft.world.storage.SaveHandler;
import net.minecraft.world.storage.WorldInfo;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.asm.util.PrettyPrinter;
import org.spongepowered.common.bridge.world.WorldInfoBridge;
import org.spongepowered.common.bridge.world.storage.SaveHandlerBridge;
import org.spongepowered.common.data.util.DataUtil;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.world.storage.SpongePlayerDataHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

@NonnullByDefault
@Mixin(SaveHandler.class)
public abstract class SaveHandlerMixin implements SaveHandlerBridge {

    @Shadow @Final private File worldDirectory;

    @Nullable private Exception impl$capturedException;
    // player join stuff
    @Nullable private Path impl$file;

    @ModifyArg(method = "checkSessionLock",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/MinecraftException;<init>(Ljava/lang/String;)V", ordinal = 0, remap = false))
    private String modifyMinecraftExceptionOutputIfNotInitializationTime(final String message) {
        return "The save folder for world " + this.worldDirectory + " is being accessed from another location, aborting";
    }

    @ModifyArg(method = "checkSessionLock",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/MinecraftException;<init>(Ljava/lang/String;)V", ordinal = 1, remap = false))
    private String modifyMinecraftExceptionOutputIfIOException(final String message) {
        return "Failed to check session lock for world " + this.worldDirectory + ", aborting";
    }

    @Inject(method = "saveWorldInfoWithPlayer", at = @At("RETURN"))
    private void impl$saveLevelSpongeDataFile(final WorldInfo worldInformation, final CompoundNBT tagCompound, final CallbackInfo ci) {
        try {
            // If the returned NBT is empty, then we should warn the user.
            CompoundNBT spongeRootLevelNBT = ((WorldInfoBridge) worldInformation).bridge$getSpongeRootLevelNbt();
            if (spongeRootLevelNBT.isEmpty()) {
                Integer dimensionId = ((WorldInfoBridge) worldInformation).bridge$getDimensionId();
                String dimensionIdString = dimensionId == null ? "unknown" : String.valueOf(dimensionId);

                // We should warn the user about the NBT being empty, but not saving it.
                new PrettyPrinter().add("Sponge Root Level NBT for world %s is empty!", worldInformation.getWorldName()).centre().hr()
                        .add("When trying to save Sponge data for the world %s, an empty NBT compound was provided. The old Sponge data file was "
                                        + "left intact.",
                                worldInformation.getWorldName())
                        .add()
                        .add("The following information may be useful in debugging:")
                        .add()
                        .add("UUID: ", ((WorldInfoBridge) worldInformation).bridge$getAssignedId())
                        .add("Dimension ID: ", dimensionIdString)
                        .add("Is Modded: ", ((WorldInfoBridge) worldInformation).bridge$getIsMod())
                        .add("Valid flag: ", ((WorldInfoBridge) worldInformation).bridge$isValid())
                        .add()
                        .add("Stack trace:")
                        .add(new Exception())
                        .print(System.err);
                return;
            }

            final File newDataFile = new File(this.worldDirectory, Constants.Sponge.World.LEVEL_SPONGE_DAT_NEW);
            final File oldDataFile = new File(this.worldDirectory, Constants.Sponge.World.LEVEL_SPONGE_DAT_OLD);
            final File dataFile = new File(this.worldDirectory, Constants.Sponge.World.LEVEL_SPONGE_DAT);
            try (final FileOutputStream stream = new FileOutputStream(newDataFile)) {
                CompressedStreamTools.writeCompressed(spongeRootLevelNBT, stream);
            }

            // Before we continue, is the file zero length?
            if (newDataFile.length() == 0) {
                Integer dimensionId = ((WorldInfoBridge) worldInformation).bridge$getDimensionId();
                String dimensionIdString = dimensionId == null ? "unknown" : String.valueOf(dimensionId);
                // Then we just delete the file and tell the user that we didn't save properly.
                new PrettyPrinter().add("Zero length level_sponge.dat file was created for %s!", worldInformation.getWorldName()).centre().hr()
                        .add("When saving the data file for the world %s, a zero length file was written. Sponge has discarded this file.",
                                worldInformation.getWorldName())
                        .add()
                        .add("The following information may be useful in debugging:")
                        .add()
                        .add("UUID: ", ((WorldInfoBridge) worldInformation).bridge$getAssignedId())
                        .add("Dimension ID: ", dimensionIdString)
                        .add("Is Modded: ", ((WorldInfoBridge) worldInformation).bridge$getIsMod())
                        .add("Valid flag: ", ((WorldInfoBridge) worldInformation).bridge$isValid())
                        .add()
                        .add("Stack trace:")
                        .add(new Exception())
                        .print(System.err);
                newDataFile.delete();
                return;
            }
            if (dataFile.exists()) {
                if (oldDataFile.exists()) {
                    oldDataFile.delete();
                }

                dataFile.renameTo(oldDataFile);
                dataFile.delete();
            }

            newDataFile.renameTo(dataFile);

            if (newDataFile.exists()) {
                newDataFile.delete();
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void bridge$loadSpongeDatData(final WorldInfo info) {
        final File spongeFile = new File(this.worldDirectory, Constants.Sponge.World.LEVEL_SPONGE_DAT);
        final File spongeOldFile = new File(this.worldDirectory, Constants.Sponge.World.LEVEL_SPONGE_DAT_OLD);

        if (spongeFile.exists() || spongeOldFile.exists()) {
            final File actualFile = spongeFile.exists() ? spongeFile : spongeOldFile;
            final CompoundNBT compound;
            try (final FileInputStream stream = new FileInputStream(actualFile)) {
                compound = CompressedStreamTools.readCompressed(stream);
            } catch (Exception ex) {
                throw new RuntimeException("Attempt failed when reading Sponge level data for [" + info.getWorldName() + "] from file [" +
                        actualFile.getName() + "]!", ex);
            }
            ((WorldInfoBridge) info).bridge$setSpongeRootLevelNBT(compound);
            if (compound.contains(Constants.Sponge.SPONGE_DATA)) {
                final CompoundNBT spongeCompound = compound.getCompound(Constants.Sponge.SPONGE_DATA);
                DataUtil.spongeDataFixer.process(FixTypes.LEVEL, spongeCompound);
                ((WorldInfoBridge) info).bridge$readSpongeNbt(spongeCompound);
            }
        }
    }

    /**
     * Redirects the {@link File#exists()} checking that if the file exists, grab
     * the file for later usage to read the file attributes for pre-existing data.
     *
     * @param localfile The local file
     * @return True if the file exists
     */
    @Redirect(method = "readPlayerData(Lnet/minecraft/entity/player/EntityPlayer;)Lnet/minecraft/nbt/NBTTagCompound;",
        at = @At(value = "INVOKE", target = "Ljava/io/File;isFile()Z", remap = false))
    private boolean impl$grabFileToField(final File localfile) {
        final boolean isFile = localfile.isFile();
        this.impl$file = isFile ? localfile.toPath() : null;
        return isFile;
    }

    /**
     * Redirects the reader such that since the player file existed already, we can safely assume
     * we can grab the file attributes and check if the first join time exists in the sponge compound,
     * if it does not, then we add it to the sponge data part of the compound.
     *
     * @param inputStream The input stream to direct to compressed stream tools
     * @return The compound that may be modified
     * @throws IOException for reasons
     */
    @Redirect(method = "readPlayerData(Lnet/minecraft/entity/player/EntityPlayer;)Lnet/minecraft/nbt/NBTTagCompound;", at = @At(value = "INVOKE", target =
        "Lnet/minecraft/nbt/CompressedStreamTools;readCompressed(Ljava/io/InputStream;)"
            + "Lnet/minecraft/nbt/NBTTagCompound;"))
    private CompoundNBT impl$readLegacyDataAndOrSpongeData(final InputStream inputStream) throws IOException {
        Instant creation = this.impl$file == null ? Instant.now() : Files.readAttributes(this.impl$file, BasicFileAttributes.class).creationTime().toInstant();
        final CompoundNBT compound = CompressedStreamTools.readCompressed(inputStream);
        Instant lastPlayed = Instant.now();
        // first try to migrate bukkit join data stuff
        if (compound.contains(Constants.Bukkit.BUKKIT, Constants.NBT.TAG_COMPOUND)) {
            final CompoundNBT bukkitCompound = compound.getCompound(Constants.Bukkit.BUKKIT);
            creation = Instant.ofEpochMilli(bukkitCompound.getLong(Constants.Bukkit.BUKKIT_FIRST_PLAYED));
            lastPlayed = Instant.ofEpochMilli(bukkitCompound.getLong(Constants.Bukkit.BUKKIT_LAST_PLAYED));
        }
        // migrate canary join data
        if (compound.contains(Constants.Canary.ROOT, Constants.NBT.TAG_COMPOUND)) {
            final CompoundNBT canaryCompound = compound.getCompound(Constants.Canary.ROOT);
            creation = Instant.ofEpochMilli(canaryCompound.getLong(Constants.Canary.FIRST_JOINED));
            lastPlayed = Instant.ofEpochMilli(canaryCompound.getLong(Constants.Canary.LAST_JOINED));
        }
        UUID playerId = null;
        if (compound.hasUniqueId(Constants.UUID)) {
            playerId = compound.getUniqueId(Constants.UUID);
        }
        if (playerId != null) {
            final Optional<Instant> savedFirst = SpongePlayerDataHandler.getFirstJoined(playerId);
            if (savedFirst.isPresent()) {
                creation = savedFirst.get();
            }
            final Optional<Instant> savedJoined = SpongePlayerDataHandler.getLastPlayed(playerId);
            if (savedJoined.isPresent()) {
                lastPlayed = savedJoined.get();
            }
            SpongePlayerDataHandler.setPlayerInfo(playerId, creation, lastPlayed);
        }
        this.impl$file = null;
        return compound;
    }

    @Inject(method = "writePlayerData",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/nbt/CompressedStreamTools;writeCompressed(Lnet/minecraft/nbt/NBTTagCompound;Ljava/io/OutputStream;)V",
            shift = At.Shift.AFTER))
    private void impl$saveSpongePlayerData(final PlayerEntity player, final CallbackInfo callbackInfo) {
        SpongePlayerDataHandler.savePlayer(player.getUniqueID());
    }

    @Inject(
        method = "writePlayerData",
        at = @At(value = "INVOKE",
            target = "Lorg/apache/logging/log4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;)V",
            remap = false),
        locals = LocalCapture.CAPTURE_FAILHARD)
    private void impl$trackExceptionForLogging(final PlayerEntity player, final CallbackInfo ci, final Exception exception) {
        this.impl$capturedException = exception;
    }

    @Redirect(
        method = "writePlayerData",
        at = @At(
            value = "INVOKE",
            target = "Lorg/apache/logging/log4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;)V",
            remap = false
        )
    )
    private void impl$useStoredException(final Logger logger, final String message, final Object param) {
        logger.warn(message, param, this.impl$capturedException);
        this.impl$capturedException = null;
    }

    // SF overrides getWorldDirectory for mod compatibility.
    // In order to avoid conflicts, we simply use another method to guarantee
    // the sponge world directory is returned for the corresponding save handler.
    // AnvilSaveHandlerMixin#getChunkLoader is one example where we must use this method.
    @Override
    public File bridge$getSpongeWorldDirectory() {
        return this.worldDirectory;
    }
}
