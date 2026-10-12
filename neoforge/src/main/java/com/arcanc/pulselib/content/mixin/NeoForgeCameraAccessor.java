package com.arcanc.pulselib.content.mixin;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface NeoForgeCameraAccessor
{
	@Invoker("setRotation")
	void pulselib$setRotationWithRoll(float yaw, float pitch, float roll);
}
