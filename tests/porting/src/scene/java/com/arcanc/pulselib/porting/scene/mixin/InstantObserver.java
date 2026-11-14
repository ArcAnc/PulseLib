/**
 * @author ArcAnc
 * Created at: 14.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.porting.scene.mixin;

import com.arcanc.pulselib.content.renderer.plan.PPipelineHandle;
import com.arcanc.pulselib.content.renderer.plan.PRenderFrame;
import com.arcanc.pulselib.content.renderer.plan.PRenderPlan;
import com.arcanc.pulselib.content.renderer.rhi.PRhiImmediateRenderer;
import com.arcanc.pulselib.porting.scene.SceneChecks;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;

/** Does not cancel, defer or replace the immediate renderer. */
@Mixin(value = PRhiImmediateRenderer.class, remap = false)
public abstract class InstantObserver
{
	@Shadow
	@Final
	private Function<PPipelineHandle, RenderType> types;

	@Inject(method = "execute", at = @At("HEAD"))
	private void scene$count(PRenderPlan plan, PRenderFrame frame, CallbackInfo callback)
	{
		SceneChecks.recordInstantPlan(plan, this.types);
	}
}
