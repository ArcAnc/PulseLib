/**
 * @author ArcAnc
 * Created at: 17.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.legacy;

import com.arcanc.pulselib.content.mixin.CommandEncoderAccessor;
import com.arcanc.pulselib.content.mixin.GlCommandEncoderAccessor;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GL40;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.ARBDrawBuffersBlend;

/** Native state touched by a local render, restored through Minecraft's caches where applicable. */
public final class GlRenderState implements AutoCloseable
{
	private static final int TEXTURE_UNITS = 12;
	private final @Nullable GpuTextureView colorOverride = RenderSystem.outputColorTextureOverride;
	private final @Nullable GpuTextureView depthOverride = RenderSystem.outputDepthTextureOverride;
	private final GpuTextureView colorAttachment;
	private final @Nullable GpuTextureView depthAttachment;
	private final GpuBufferSlice projection = RenderSystem.getProjectionMatrixBuffer();
	private final ProjectionType projectionType = RenderSystem.getProjectionType();
	private final Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix());
	private final boolean renderScissor = RenderSystem.getScissorStateForRenderTypeDraws().enabled();
	private final int renderScissorX = RenderSystem.getScissorStateForRenderTypeDraws().x();
	private final int renderScissorY = RenderSystem.getScissorStateForRenderTypeDraws().y();
	private final int renderScissorWidth = RenderSystem.getScissorStateForRenderTypeDraws().width();
	private final int renderScissorHeight = RenderSystem.getScissorStateForRenderTypeDraws().height();
	private final int drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
	private final int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
	private final int[] viewport = new int[4];
	private final int[] scissor = new int[4];
	private final boolean scissorEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
	private final boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
	private final boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
	private final int depthFunction = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
	private final boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
	private final boolean polygonOffset = GL11.glIsEnabled(GL11.GL_POLYGON_OFFSET_FILL);
	private final float polygonFactor = GL11.glGetFloat(GL11.GL_POLYGON_OFFSET_FACTOR);
	private final float polygonUnits = GL11.glGetFloat(GL11.GL_POLYGON_OFFSET_UNITS);
	private final int[] polygonMode = new int[2];
	private final int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
	private final int vertexArray = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
	private final int arrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
	private final boolean indirectSupported = GL.getCapabilities().OpenGL40 || GL.getCapabilities().GL_ARB_draw_indirect;
	private final int indirectBuffer = this.indirectSupported ? GL11.glGetInteger(GL40.GL_DRAW_INDIRECT_BUFFER_BINDING) : 0;
	private final int uniformBuffer = GL11.glGetInteger(GL31.GL_UNIFORM_BUFFER_BINDING);
	private final int textureBuffer = GL11.glGetInteger(GL31.GL_TEXTURE_BUFFER);
	private final int copyReadBuffer = GL11.glGetInteger(GL31.GL_COPY_READ_BUFFER);
	private final int copyWriteBuffer = GL11.glGetInteger(GL31.GL_COPY_WRITE_BUFFER);
	private final int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
	private final int[][] textures = new int[TEXTURE_UNITS][4];
	private final long[][] uniforms = new long[GL11.glGetInteger(GL31.GL_MAX_UNIFORM_BUFFER_BINDINGS)][3];
	private final boolean indexedBlend = GL.getCapabilities().OpenGL40 || GL.getCapabilities().GL_ARB_draw_buffers_blend;
	private final int[][] blends = new int[this.indexedBlend ? GL11.glGetInteger(GL20.GL_MAX_DRAW_BUFFERS) : 1][8];

	/** Capture outside an active RenderPass; native passes cannot be nested in Minecraft 26.1. */
	public GlRenderState()
	{
		RenderSystem.assertOnRenderThread();
		if (((CommandEncoderAccessor)RenderSystem.getDevice().createCommandEncoder()).pulselib$getBackend().isInRenderPass())
			throw new IllegalStateException("Local render scopes cannot enter an active Minecraft RenderPass");
		var target = Minecraft.getInstance().getMainRenderTarget();
		this.colorAttachment = java.util.Objects.requireNonNull(this.colorOverride == null ? target.getColorTextureView() : this.colorOverride);
		this.depthAttachment = this.colorOverride == null && this.depthOverride == null ? target.getDepthTextureView() : this.depthOverride;
		GL11.glGetIntegerv(GL11.GL_POLYGON_MODE, this.polygonMode);
		GL11.glGetIntegerv(GL11.GL_VIEWPORT, this.viewport);
		GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, this.scissor);
		for (int unit = 0; unit < TEXTURE_UNITS; unit++)
		{
			GlStateManager._activeTexture(GL13.GL_TEXTURE0 + unit);
			this.textures[unit][0] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
			this.textures[unit][1] = GL11.glGetInteger(GL13.GL_TEXTURE_BINDING_CUBE_MAP);
			this.textures[unit][2] = GL11.glGetInteger(GL31.GL_TEXTURE_BINDING_BUFFER);
			this.textures[unit][3] = GL11.glGetInteger(GL33.GL_SAMPLER_BINDING);
		}
		GlStateManager._activeTexture(this.activeTexture);
		for (int index = 0; index < this.uniforms.length; index++)
		{
			this.uniforms[index][0] = GL30.glGetIntegeri(GL31.GL_UNIFORM_BUFFER_BINDING, index);
			this.uniforms[index][1] = GL32.glGetInteger64i(GL31.GL_UNIFORM_BUFFER_START, index);
			this.uniforms[index][2] = GL32.glGetInteger64i(GL31.GL_UNIFORM_BUFFER_SIZE, index);
		}
		int[] parameters = {GL14.GL_BLEND_SRC_RGB, GL14.GL_BLEND_DST_RGB, GL14.GL_BLEND_SRC_ALPHA,
				GL14.GL_BLEND_DST_ALPHA, GL20.GL_BLEND_EQUATION_RGB, GL20.GL_BLEND_EQUATION_ALPHA};
		for (int index = 0; index < this.blends.length; index++)
		{
			this.blends[index][0] = GL30.glIsEnabledi(GL11.GL_BLEND, index) ? 1 : 0;
			for (int parameter = 0; parameter < parameters.length; parameter++)
				this.blends[index][parameter + 1] = this.indexedBlend ? GL30.glGetIntegeri(parameters[parameter], index) : GL11.glGetInteger(parameters[parameter]);
			var mask = org.lwjgl.BufferUtils.createByteBuffer(4);
			GL30.glGetBooleani_v(GL11.GL_COLOR_WRITEMASK, index, mask);
			for (int channel = 0; channel < 4; channel++)
				if (mask.get(channel) != 0)
					this.blends[index][7] |= 1 << channel;
		}
	}

	/** Selects the captured 26.1 attachments for the next native render pass. */
	public void bindTarget()
	{
		RenderSystem.assertOnRenderThread();
		RenderSystem.outputColorTextureOverride = this.colorAttachment;
		RenderSystem.outputDepthTextureOverride = this.depthAttachment;
		restoreRenderScissor();
		GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, this.drawFramebuffer);
		GlStateManager._viewport(this.viewport[0], this.viewport[1], this.viewport[2], this.viewport[3]);
	}

	private void restoreRenderScissor()
	{
		if (this.renderScissor)
			RenderSystem.enableScissorForRenderTypeDraws(this.renderScissorX, this.renderScissorY, this.renderScissorWidth, this.renderScissorHeight);
		else
			RenderSystem.disableScissorForRenderTypeDraws();
	}

	/** Restores captured Minecraft inputs and the native state, including the encoder's identity caches. */
	@Override
	public void close()
	{
		RenderSystem.assertOnRenderThread();
		RenderSystem.outputColorTextureOverride = this.colorOverride;
		RenderSystem.outputDepthTextureOverride = this.depthOverride;
		RenderSystem.setProjectionMatrix(this.projection, this.projectionType);
		RenderSystem.getModelViewStack().set(this.modelView);
		restoreRenderScissor();
		var backend = ((CommandEncoderAccessor)RenderSystem.getDevice().createCommandEncoder()).pulselib$getBackend();
		if (backend instanceof GlCommandEncoderAccessor encoder)
		{
			encoder.pulselib$setLastProgram(null);
			encoder.pulselib$setLastPipeline(null);
		}
		GlStateManager._glUseProgram(this.program);
		GL30.glBindVertexArray(this.vertexArray);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.arrayBuffer);
		if (this.indirectSupported)
			GL15.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, this.indirectBuffer);
		for (int index = 0; index < this.uniforms.length; index++)
		{
			long[] binding = this.uniforms[index];
			if (binding[0] == 0 || binding[2] == 0)
				GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, index, (int)binding[0]);
			else
				GL30.glBindBufferRange(GL31.GL_UNIFORM_BUFFER, index, (int)binding[0], binding[1], binding[2]);
		}
		GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, this.uniformBuffer);
		GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this.textureBuffer);
		GL15.glBindBuffer(GL31.GL_COPY_READ_BUFFER, this.copyReadBuffer);
		GL15.glBindBuffer(GL31.GL_COPY_WRITE_BUFFER, this.copyWriteBuffer);
		for (int unit = 0; unit < TEXTURE_UNITS; unit++)
		{
			GlStateManager._activeTexture(GL13.GL_TEXTURE0 + unit);
			GlStateManager._bindTexture(this.textures[unit][0]);
			GL11.glBindTexture(GL13.GL_TEXTURE_CUBE_MAP, this.textures[unit][1]);
			GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this.textures[unit][2]);
			GL33.glBindSampler(unit, this.textures[unit][3]);
		}
		GlStateManager._activeTexture(this.activeTexture);
		if (this.depth)
			GlStateManager._enableDepthTest();
		else
			GlStateManager._disableDepthTest();
		GlStateManager._depthMask(this.depthMask);
		GlStateManager._depthFunc(this.depthFunction);
		if (this.cull)
			GlStateManager._enableCull();
		else
			GlStateManager._disableCull();
		GlStateManager._polygonOffset(this.polygonFactor, this.polygonUnits);
		if (this.polygonOffset)
			GlStateManager._enablePolygonOffset();
		else
			GlStateManager._disablePolygonOffset();
		GlStateManager._polygonMode(GL11.GL_FRONT_AND_BACK, this.polygonMode[0]);
		// Global setters alter every draw buffer; restore the primary cache before indexed state.
		if (this.blends[0][0] != 0)
			GlStateManager._enableBlend();
		else
			GlStateManager._disableBlend();
		GlStateManager._blendFuncSeparate(this.blends[0][1], this.blends[0][2], this.blends[0][3], this.blends[0][4]);
		GlStateManager._colorMask(this.blends[0][7]);
		for (int index = 0; index < this.blends.length; index++)
		{
			int[] blend = this.blends[index];
			if (blend[0] != 0)
				GL30.glEnablei(GL11.GL_BLEND, index);
			else
				GL30.glDisablei(GL11.GL_BLEND, index);
			if (GL.getCapabilities().OpenGL40)
			{
				GL40.glBlendFuncSeparatei(index, blend[1], blend[2], blend[3], blend[4]);
				GL40.glBlendEquationSeparatei(index, blend[5], blend[6]);
			}
			else if (this.indexedBlend)
			{
				ARBDrawBuffersBlend.glBlendFuncSeparateiARB(index, blend[1], blend[2], blend[3], blend[4]);
				ARBDrawBuffersBlend.glBlendEquationSeparateiARB(index, blend[5], blend[6]);
			}
			else
				GL20.glBlendEquationSeparate(blend[5], blend[6]);
			GL30.glColorMaski(index, (blend[7] & 1) != 0, (blend[7] & 2) != 0, (blend[7] & 4) != 0, (blend[7] & 8) != 0);
		}
		GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, this.drawFramebuffer);
		GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, this.readFramebuffer);
		GlStateManager._viewport(this.viewport[0], this.viewport[1], this.viewport[2], this.viewport[3]);
		GlStateManager._scissorBox(this.scissor[0], this.scissor[1], this.scissor[2], this.scissor[3]);
		if (this.scissorEnabled)
			GlStateManager._enableScissorTest();
		else
			GlStateManager._disableScissorTest();
	}
}
