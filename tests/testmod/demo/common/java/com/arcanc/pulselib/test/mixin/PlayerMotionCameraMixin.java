/**
 * @author ArcAnc
 * Created at: 13.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.mixin;

import com.arcanc.pulselib.test.PlayerMotionHarness;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** MC26 native alignment: capture the vanilla baseline before the library's TAIL hook. */
@Mixin(Camera.class)
public abstract class PlayerMotionCameraMixin
{
	@WrapMethod(method = "alignWithEntity")
	private void pulselibtest$beforeCamera(float partialTick, Operation<Void> original)
	{
		PlayerMotionHarness.beforeCamera(((Camera)(Object)this).entity());
		original.call(partialTick);
	}

	@Inject(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V", shift = At.Shift.AFTER))
	private void pulselibtest$cameraBaseline(float partialTick, CallbackInfo callback)
	{
		Camera camera = (Camera)(Object)this;
		PlayerMotionHarness.cameraBaseline(camera, camera.entity(), !Minecraft.getInstance().options.getCameraType().isFirstPerson(), partialTick);
	}
}
