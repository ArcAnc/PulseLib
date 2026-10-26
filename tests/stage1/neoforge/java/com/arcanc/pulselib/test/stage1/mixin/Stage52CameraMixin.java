/**
 * @author ArcAnc
 * Created at: 26.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1.mixin;

import com.arcanc.pulselib.test.stage1.Stage52CameraProbe;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.client.Camera;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Captures vanilla camera baseline and observes the complete native alignment after Pulse hooks.
 */
@Mixin(Camera.class)
public abstract class Stage52CameraMixin
{
	@WrapMethod(method = "alignWithEntity")
	private void pulselib$observeCamera(float partialTick, Operation<Void> original)
	{
		boolean probe = Boolean.getBoolean("pulselib.stage52bProbe");
		if (probe)
			Stage52CameraProbe.before((Camera)(Object)this);
		original.call(partialTick);
		if (probe)
			Stage52CameraProbe.after((Camera)(Object)this);
	}

	@Inject(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V", shift = At.Shift.AFTER))
	private void pulselib$captureVanillaCamera(float partialTick, CallbackInfo callback)
	{
		if (Boolean.getBoolean("pulselib.stage52bProbe"))
			Stage52CameraProbe.baseline((Camera)(Object)this, partialTick);
	}
}
