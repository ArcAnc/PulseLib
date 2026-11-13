/**
 * @author ArcAnc
 * Created at: 22.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.gl;

import com.arcanc.pulselib.content.renderer.plan.PRenderFrame;
import com.arcanc.pulselib.content.renderer.plan.PRenderHost;
import com.arcanc.pulselib.content.renderer.rhi.PRhiRenderHost;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import org.joml.Matrix4f;

/** Compatibility facade over the shared native frame mirror; no OpenGL state is read. */
public final class PGlRenderHost implements PRenderHost
{

	/** Mirrors an actual projection upload, without reading back GPU memory. */
	public static void recordProjection(GpuBufferSlice buffer, Matrix4f projection)
	{
		PRhiRenderHost.recordProjection(buffer, projection);
	}

	/** Captures exactly the projection currently bound by the host. */
	@Override
	public PRenderFrame captureFrame()
	{
		return new PRhiRenderHost().captureFrame();
	}
}
