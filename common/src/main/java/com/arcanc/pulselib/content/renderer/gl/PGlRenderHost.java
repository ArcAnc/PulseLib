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
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.WeakHashMap;

/** Captures CPU frame matrices while Minecraft 26.1 stores the active projection in a UBO. */
public final class PGlRenderHost implements PRenderHost
{
	private static final Map<GpuBufferSlice, Matrix4f> PROJECTIONS = new WeakHashMap<>();

	/** Mirrors an actual projection upload, without reading back GPU memory. */
	public static void recordProjection(GpuBufferSlice buffer, Matrix4f projection)
	{
		RenderSystem.assertOnRenderThread();
		PROJECTIONS.computeIfAbsent(buffer, ignored -> new Matrix4f()).set(projection);
	}

	/** Captures exactly the projection currently bound by the host. */
	@Override
	public PRenderFrame captureFrame()
	{
		RenderSystem.assertOnRenderThread();
		Matrix4f projection = PROJECTIONS.get(RenderSystem.getProjectionMatrixBuffer());
		if (projection == null)
			throw new IllegalStateException("Active projection was not uploaded through ProjectionMatrixBuffer; supply an explicit PRenderFrame");
		return new PRenderFrame(RenderSystem.getModelViewMatrix(), projection);
	}
}
