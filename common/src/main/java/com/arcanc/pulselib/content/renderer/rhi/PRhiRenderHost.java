/**
 * @author ArcAnc
 * Created at: 22.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.rhi;

import com.arcanc.pulselib.content.renderer.plan.PRenderFrame;
import com.arcanc.pulselib.content.renderer.plan.PRenderHost;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

/** CPU mirror of actual projection uploads, shared by native and compatibility rendering paths. */
@ApiStatus.Internal
public final class PRhiRenderHost implements PRenderHost
{
	private static final Map<GpuBufferSlice, Matrix4f> PROJECTIONS = new WeakHashMap<>();

	public static void recordProjection(GpuBufferSlice buffer, Matrix4f projection)
	{
		RenderSystem.assertOnRenderThread();
		if (buffer.buffer().isClosed())
			throw new IllegalStateException("Cannot mirror a closed projection buffer");
		PROJECTIONS.computeIfAbsent(buffer, ignored -> new Matrix4f()).set(projection);
	}

	/** Removes the CPU mirror when its native ProjectionMatrixBuffer owner closes. */
	public static void forgetProjection(GpuBufferSlice buffer)
	{
		RenderSystem.assertOnRenderThread();
		PROJECTIONS.remove(buffer);
	}

	@Override
	public PRenderFrame captureFrame()
	{
		RenderSystem.assertOnRenderThread();
		@Nullable GpuBufferSlice buffer = RenderSystem.getProjectionMatrixBuffer();
		if (buffer == null || buffer.buffer().isClosed())
			throw new IllegalStateException("No live active projection buffer; supply an explicit PRenderFrame");
		@Nullable Matrix4f projection = PROJECTIONS.get(buffer);
		if (projection == null)
			throw new IllegalStateException("Active projection was not uploaded through ProjectionMatrixBuffer; supply an explicit PRenderFrame");
		return new PRenderFrame(RenderSystem.getModelViewMatrixCopy(), projection);
	}
}
