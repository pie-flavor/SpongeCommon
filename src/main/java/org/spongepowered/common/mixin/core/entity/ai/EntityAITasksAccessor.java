package org.spongepowered.common.mixin.core.entity.ai;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAITasks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityAITasks.class)
public interface EntityAITasksAccessor {

    @Accessor("owner") EntityLiving accessor$getOwner();

    @Accessor("owner") void accessor$setOwner(EntityLiving owner);

}
