/**
 * @author ArcAnc
 * Created at: 12.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.neoforge;

import com.arcanc.pulselib.content.mixin.NeoForgeCameraAccessor;
import com.arcanc.pulselib.platform.IPulseCameraService;
import net.minecraft.client.Camera;

public final class NeoForgeCameraService implements IPulseCameraService
{
	@Override
	public void setRotation(Camera camera, float yaw, float pitch, float roll)
	{
		((NeoForgeCameraAccessor)camera).pulselib$setRotationWithRoll(yaw, pitch, roll);
	}
}
