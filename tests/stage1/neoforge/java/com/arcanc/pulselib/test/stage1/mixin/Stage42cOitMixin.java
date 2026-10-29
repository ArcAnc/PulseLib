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
import com.arcanc.pulselib.test.stage1.Stage52ArmorOcclusionProbe;
import com.arcanc.pulselib.test.stage1.Stage52AnimatedOcclusionProbe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.jspecify.annotations.Nullable;

/** Checks destruction before the following resize allocation can reuse texture/FBO names. */
@Mixin(targets = "com.arcanc.pulselib.content.renderer.gl.PGlWeightedBlendedOit")
public abstract class Stage42cOitMixin
{
	@Unique
	private Stage42cResources.@Nullable Snapshot pulselib$oldAttachments;

	@Inject(method = "beginDepthPass", at = @At("HEAD"))
	private void pulselib$observeArmorDepthPass(int layer, CallbackInfo callback)
	{
		if (Boolean.getBoolean("pulselib.stage52b4aProbe") && layer == 0)
			Stage52ArmorOcclusionProbe.depthPass();
		if (Boolean.getBoolean("pulselib.stage52b4bProbe") && layer == 0)
			Stage52AnimatedOcclusionProbe.depthPass();
	}

	@Inject(method = "closeBuffers", at = @At("HEAD"))
	private void pulselib$captureAttachments(CallbackInfo callback)
	{
		if (Boolean.getBoolean("pulselib.stage42cProbe") && Stage42cLifecycleProbe.active())
			this.pulselib$oldAttachments = Stage42cResources.oit(this);
	}

	@Inject(method = "closeBuffers", at = @At("RETURN"))
	private void pulselib$checkAttachments(CallbackInfo callback)
	{
		if (this.pulselib$oldAttachments == null)
			return;
		Stage42cLifecycleProbe.oitClosed(this.pulselib$oldAttachments);
		this.pulselib$oldAttachments = null;
	}
}
