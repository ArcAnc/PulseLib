package com.arcanc.pulselib.platform;

import net.minecraft.client.Camera;

public interface IPulseCameraService
{
	void setRotation(Camera camera, float yaw, float pitch, float roll);
}
