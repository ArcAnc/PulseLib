/**
 * @author ArcAnc
 * Created at: 13.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.arcanc.pulselib.content.animatable.instance.InstanceAnimationManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Removes instance animation state when a client block entity is removed.
 */
@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin
{
	/**
	 * Removes client animation state before the block entity is marked as removed.
	 *
	 * @param callback the Mixin callback used to inspect or replace the intercepted result
	 */
	@Inject(method = "setRemoved", at = @At("HEAD"))
	private void pulselib$removeAnimationManager(CallbackInfo callback)
	{
		@Nullable Level level = ((BlockEntity)(Object)this).getLevel();
		if (level != null && level.isClientSide())
		{
			InstanceAnimationManager.removeManager(this);
		}
	}
}
