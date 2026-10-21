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
		// Matches Minecraft 26.1's rotation convention. The vanilla call above also
		// invalidates cached view matrices before these orientation values change.
		camera.rotation().rotationYXZ((float)Math.PI - (float)Math.toRadians(yaw),
				-(float)Math.toRadians(pitch), -(float)Math.toRadians(roll));
		((CameraAccessor)camera).pulselib$getForwards().set(0f, 0f, -1f).rotate(camera.rotation());
		((CameraAccessor)camera).pulselib$getUp().set(0f, 1f, 0f).rotate(camera.rotation());
		((CameraAccessor)camera).pulselib$getLeft().set(-1f, 0f, 0f).rotate(camera.rotation());
	}
}
