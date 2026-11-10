/**
 * @author ArcAnc
 * Created at: 17.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.mixin;

import com.arcanc.pulselib.test.GlErrors;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.opengl.GlDebug;
import org.lwjgl.opengl.GL43;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin (GlDebug.class)
public abstract class GlDebugMixin
{
	@WrapMethod (method = "enableDebugCallback")
	private static @Nullable GlDebug pulselibtest$synchronous(int verbosity, boolean synchronous, java.util.Set<String> extensions, Operation<@Nullable GlDebug> original)
	{
		return original.call(verbosity, synchronous || (Boolean.getBoolean("pulselib.test.smokeRegression") || (Boolean.getBoolean("pulselib.test.h14Offscreen") || (Boolean.getBoolean("pulselib.test.worldRegression") || (Boolean.getBoolean("pulselib.test.renderRegression") || Boolean.getBoolean("pulselib.test.visibilityRegression"))))), extensions);
	}

	@Inject (method = "printDebugLog", at = @At ("HEAD"))
	private void pulselibtest$glError(int source, int type, int id, int severity, int length,
	                                       long message, long user, CallbackInfo callback)
	{
		if (type == GL43.GL_DEBUG_TYPE_ERROR && (Boolean.getBoolean("pulselib.test.smokeRegression") || (Boolean.getBoolean("pulselib.test.h14Offscreen") || (Boolean.getBoolean("pulselib.test.worldRegression") || (Boolean.getBoolean("pulselib.test.renderRegression") || Boolean.getBoolean("pulselib.test.visibilityRegression"))))))
			GlErrors.record(id);
	}
}
