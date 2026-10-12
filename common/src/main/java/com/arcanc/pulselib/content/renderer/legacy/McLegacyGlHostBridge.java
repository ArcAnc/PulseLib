/**
 * @author ArcAnc
 * Created at: 13.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.legacy;

import com.arcanc.pulselib.content.renderer.plan.PRenderFrame;
import com.arcanc.pulselib.content.renderer.plan.PRenderHost;
import com.mojang.blaze3d.systems.RenderSystem;

public final class McLegacyGlHostBridge implements PRenderHost
{
	@Override
	public PRenderFrame captureFrame()
	{
		return new PRenderFrame(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix());
	}
}
