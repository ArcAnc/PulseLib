package com.arcanc.pulselib.neoforge;

import com.arcanc.pulselib.platform.IPulseCameraService;
import net.minecraft.client.Camera;

public final class NeoForgeCameraService implements IPulseCameraService
{
	@Override
	public void setRotation(Camera camera, float yaw, float pitch, float roll)
	{
		((com.arcanc.pulselib.content.mixin.NeoForgeCameraAccessor) camera).pulselib$setRotationWithRoll(yaw, pitch, roll);
	}
}
