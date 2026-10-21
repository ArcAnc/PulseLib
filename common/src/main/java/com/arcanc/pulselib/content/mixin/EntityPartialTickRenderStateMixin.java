/**
 * @author ArcAnc
 * Created at: 21.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.arcanc.pulselib.content.renderer.base.PPartialTickRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Preserves extraction time without a loader-added EntityRenderState field. */
@Mixin(EntityRenderState.class)
public abstract class EntityPartialTickRenderStateMixin implements PPartialTickRenderState
{
	@Unique
	private float pulselib$partialTick;

	@Override
	public float pulselib$getPartialTick()
	{
		return this.pulselib$partialTick;
	}

	@Override
	public void pulselib$setPartialTick(float partialTick)
	{
		this.pulselib$partialTick = partialTick;
	}
}
