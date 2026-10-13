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
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class PlayerMotionCameraMixin
{
	@Inject(method = "setup", at = @At("HEAD"))
	private void pulselibtest$beforeCamera(BlockGetter level, Entity entity, boolean detached,
	                                     boolean reversed, float partialTick, CallbackInfo callback)
	{
		PlayerMotionHarness.beforeCamera(entity);
	}

	@Inject(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V", shift = At.Shift.AFTER))
	private void pulselibtest$cameraBaseline(BlockGetter level, Entity entity, boolean detached,
	                                       boolean reversed, float partialTick, CallbackInfo callback)
	{
		PlayerMotionHarness.cameraBaseline((Camera)(Object)this, entity, detached, partialTick);
	}
}
