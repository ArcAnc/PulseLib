package com.arcanc.pulselib.content.mixin;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface CameraAccessor
{
	@Invoker("setRotation")
	void pulselib$setRotation(float yaw, float pitch);
}
