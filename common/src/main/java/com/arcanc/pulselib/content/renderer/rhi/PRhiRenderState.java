/**
 * @author ArcAnc
 * Created at: 14.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.rhi;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/** Borrowed native inputs of an isolated GUI/offscreen queue. No backend handles or GL state are required. */
@ApiStatus.Internal
public final class PRhiRenderState implements AutoCloseable
{
	private final @Nullable GpuTextureView colorOverride = RenderSystem.outputColorTextureOverride;
	private final @Nullable GpuTextureView depthOverride = RenderSystem.outputDepthTextureOverride;
	private final GpuTextureView colorAttachment;
	private final @Nullable GpuTextureView depthAttachment;
	private final GpuBufferSlice projection = RenderSystem.getProjectionMatrixBuffer();
	private final ProjectionType projectionType = RenderSystem.getProjectionType();
	private final Matrix4f modelView = RenderSystem.getModelViewMatrixCopy();
	private final PRhiPassState.Scissor scissor = PRhiPassState.Scissor.copyOf(RenderSystem.getScissorStateForRenderTypeDraws());

	public PRhiRenderState()
	{
		RenderSystem.assertOnRenderThread();
		if (PRhiRenderPassTracker.hasActivePass())
			throw new IllegalStateException("Local render scopes cannot enter an active Minecraft RenderPass");
		var target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
		this.colorAttachment = Objects.requireNonNull(this.colorOverride == null ? target.getColorTextureView() : this.colorOverride);
		this.depthAttachment = this.colorOverride == null && this.depthOverride == null ? target.getDepthTextureView() : this.depthOverride;
	}

	public void bindTarget()
	{
		RenderSystem.outputColorTextureOverride = this.colorAttachment;
		RenderSystem.outputDepthTextureOverride = this.depthAttachment;
		restoreScissor();
	}

	private void restoreScissor()
	{
		if (this.scissor.enabled())
			RenderSystem.enableScissorForRenderTypeDraws(this.scissor.x(), this.scissor.y(), this.scissor.width(), this.scissor.height());
		else
			RenderSystem.disableScissorForRenderTypeDraws();
	}

	@Override
	public void close()
	{
		RenderSystem.assertOnRenderThread();
		RenderSystem.outputColorTextureOverride = this.colorOverride;
		RenderSystem.outputDepthTextureOverride = this.depthOverride;
		RenderSystem.setProjectionMatrix(this.projection, this.projectionType);
		RenderSystem.getModelViewStack().set(this.modelView);
		restoreScissor();
	}
}
