/**
 * @author ArcAnc
 * Created at: 25.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1.mixin;

import com.arcanc.pulselib.test.stage1.Stage42cLifecycleProbe;
import com.arcanc.pulselib.test.stage1.Stage42cResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.jspecify.annotations.Nullable;

/** Observes only the snapshot owned by PulseLib; native pool ownership stays with Minecraft. */
@Mixin(com.mojang.blaze3d.pipeline.RenderTarget.class)
public abstract class Stage42cDepthMixin
{
	@Unique
	private Stage42cResources.@Nullable Snapshot pulselib$oldDepth;

	@Inject(method = "destroyBuffers", at = @At("HEAD"))
	private void pulselib$captureDepth(CallbackInfo callback)
	{
		if (Boolean.getBoolean("pulselib.stage42cProbe") && Stage42cLifecycleProbe.ownsSnapshot((com.mojang.blaze3d.pipeline.RenderTarget)(Object)this))
			this.pulselib$oldDepth = Stage42cResources.target((com.mojang.blaze3d.pipeline.RenderTarget)(Object)this);
	}

	@Inject(method = "destroyBuffers", at = @At("RETURN"))
	private void pulselib$checkDepth(CallbackInfo callback)
	{
		if (this.pulselib$oldDepth == null)
			return;
		Stage42cLifecycleProbe.snapshotClosed(this.pulselib$oldDepth);
		this.pulselib$oldDepth = null;
	}
}
