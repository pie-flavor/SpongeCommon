package org.spongepowered.common.mixin.core.advancements;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementList;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.Set;

@Mixin(AdvancementList.class)
public interface AdvancementListAccessor {

    @Accessor("advancements") Map<ResourceLocation, Advancement> accessor$getAdvancements();

    @Accessor("roots") Set<Advancement> accessor$getRoots();

    @Accessor("nonRoots") Set<Advancement> accessor$getNonRoots();

    @Accessor("listener")  AdvancementList.Listener accessor$getListener();

}
