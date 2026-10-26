/**
 * @author ArcAnc
 * Created at: 26.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.arcanc.pulselib.content.renderer.base.PWorldRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Carries the extraction decision until LevelRenderer chooses its native transparency chain. */
@Mixin(EntityRenderState.class)
public abstract class WorldEntityRenderStateMixin implements PWorldRenderState
{
	@Unique
	private boolean pulselib$separateTargets;

	@Override
	public boolean pulselib$requiresSeparateTargets()
	{
		return this.pulselib$separateTargets;
	}

	@Override
	public void pulselib$setRequiresSeparateTargets(boolean required)
	{
		this.pulselib$separateTargets = required;
	}
}
