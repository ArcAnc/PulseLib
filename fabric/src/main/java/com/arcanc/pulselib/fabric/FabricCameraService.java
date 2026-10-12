package com.arcanc.pulselib.fabric;

import com.arcanc.pulselib.content.mixin.CameraAccessor;
import com.arcanc.pulselib.platform.IPulseCameraService;
import net.minecraft.client.Camera;

public final class FabricCameraService implements IPulseCameraService
{
	@Override
	public void setRotation(Camera camera, float yaw, float pitch, float roll)
	{
		((CameraAccessor) camera).pulselib$setRotation(yaw, pitch);
		camera.rotation().rotationYXZ((float) Math.PI - (float) Math.toRadians(yaw), -(float) Math.toRadians(pitch), (float) Math.toRadians(roll));
		camera.getLookVector().set(0f, 0f, -1f).rotate(camera.rotation());
		camera.getUpVector().set(0f, 1f, 0f).rotate(camera.rotation());
		camera.getLeftVector().set(-1f, 0f, 0f).rotate(camera.rotation());
	}
}
