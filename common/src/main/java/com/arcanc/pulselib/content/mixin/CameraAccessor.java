/**
 * @author ArcAnc
 * Created at: 12.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes the vanilla camera yaw and pitch rotation method to the Fabric camera bridge.
 */
@Mixin (Camera.class)
public interface CameraAccessor
{
	/**
	 * Invokes the vanilla camera rotation method with yaw and pitch in degrees.
	 *
	 * @param yaw the yaw angle in degrees
	 * @param pitch the pitch angle in degrees
	 */
	@Invoker ("setRotation")
	void pulselib$setRotation(float yaw, float pitch);
}
