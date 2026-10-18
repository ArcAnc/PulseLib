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

/**
 * Captures vanilla Minecraft render matrices for the shared render-plan backend.
 */
public final class McLegacyGlHostBridge implements PRenderHost
{
	/**
	 * Copies the current model-view and projection matrices into a render frame.
	 *
	 * @return copies of the current host model-view and projection matrices
	 */
	@Override
	public PRenderFrame captureFrame()
	{
		return new PRenderFrame(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix());
	}
}
