/**
 * @author ArcAnc
 * Created at: 12.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.fabric;

import com.arcanc.pulselib.content.mixin.CameraAccessor;
import com.arcanc.pulselib.content.player.animation.firstPerson.PFirstPersonCameraSpace;
import com.arcanc.pulselib.platform.IPulseCameraService;
import net.minecraft.client.Camera;

/**
 * Fabric camera rotation bridge that applies yaw, pitch and roll and refreshes orientation vectors.
 */
public final class FabricCameraService implements IPulseCameraService
{
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setRotation(Camera camera, float yaw, float pitch, float roll)
	{
		((CameraAccessor)camera).pulselib$setRotation(yaw, pitch);
		PFirstPersonCameraSpace.fromMinecraftAngles(yaw, pitch, roll, camera.rotation());
		camera.getLookVector().set(0f, 0f, -1f).rotate(camera.rotation());
		camera.getUpVector().set(0f, 1f, 0f).rotate(camera.rotation());
		camera.getLeftVector().set(-1f, 0f, 0f).rotate(camera.rotation());
	}
}
