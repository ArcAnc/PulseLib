/**
 * @author ArcAnc
 * Created at: 15.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.porting.scene.mixin;

import com.arcanc.pulselib.content.renderer.PRenderStagesHandler;
import com.arcanc.pulselib.porting.scene.WorldChecks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Submits after the actual loader callback has captured opaque world depth. */
@Mixin(value = PRenderStagesHandler.class, remap = false)
public abstract class WorldStageObserver
{
	@Inject(method = "afterNativeFeatures", at = @At("RETURN"))
	private static void scene$world(CallbackInfo callback)
	{
		WorldChecks.submit();
	}
}
