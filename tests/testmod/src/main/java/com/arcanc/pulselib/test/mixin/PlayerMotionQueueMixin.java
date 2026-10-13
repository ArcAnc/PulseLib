/**
 * @author ArcAnc
 * Created at: 13.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.mixin;

import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.test.PlayerMotionHarness;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PRenderQueue.class)
public abstract class PlayerMotionQueueMixin
{
	@Inject(method = {"submitEntityMesh(Lnet/minecraft/client/renderer/RenderType;Lcom/arcanc/pulselib/content/renderer/plan/PGeometryData;Lcom/arcanc/pulselib/content/renderer/plan/PInstanceHeader;)V",
			"submitFirstPersonMesh"}, at = @At("HEAD"))
	private static void pulselibtest$motionMatrix(RenderType type, PGeometryData geometry, PInstanceHeader data, CallbackInfo callback)
	{
		PlayerMotionHarness.submitted(data.transform());
	}
}
