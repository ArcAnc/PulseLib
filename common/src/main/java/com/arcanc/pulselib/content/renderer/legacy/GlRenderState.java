/**
 * @author ArcAnc
 * Created at: 17.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.legacy;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL;
import net.minecraft.client.renderer.ShaderInstance;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL40;

/** Saves the caller's target and the GL/Minecraft state touched by a local render. */
public final class GlRenderState implements AutoCloseable
{
	private final GlStateSnapshot state = new GlStateSnapshot();
	private final int drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
	private final int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
	private final int[] viewport = new int[4];
	private final int[] scissor = new int[4];
	private final int[] shaderTextures = new int[12];
	private final int[] bufferTextures = new int[12];
	private final int textureBuffer = GL11.glGetInteger(GL31.GL_TEXTURE_BUFFER);
	private final boolean indirectSupported = GL.getCapabilities().OpenGL40 || GL.getCapabilities().GL_ARB_draw_indirect;
	private final int indirectBuffer = this.indirectSupported ? GL11.glGetInteger(GL40.GL_DRAW_INDIRECT_BUFFER_BINDING) : 0;
	private final Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix());
	private final Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
	private final VertexSorting sorting = RenderSystem.getVertexSorting();
	private final boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
	private final float[] shaderColor = RenderSystem.getShaderColor().clone();
	private final @Nullable ShaderInstance shader = RenderSystem.getShader();

	/**
	 * Captures the caller's current render target and OpenGL state for later restoration.
	 */
	public GlRenderState()
	{
		RenderSystem.assertOnRenderThread();
		GL11.glGetIntegerv(GL11.GL_VIEWPORT, this.viewport);
		GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, this.scissor);
		this.state.capture();
		this.state.captureBindings(12);
		int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
		for (int unit = 0; unit < this.shaderTextures.length; unit++)
		{
			this.shaderTextures[unit] = RenderSystem.getShaderTexture(unit);
			GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
			this.bufferTextures[unit] = GL11.glGetInteger(GL31.GL_TEXTURE_BINDING_BUFFER);
		}
		GL13.glActiveTexture(activeTexture);
	}

	/**
	 * Binds the captured render target for local model rendering.
	 */
	public void bindTarget()
	{
		GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, this.drawFramebuffer);
		GL11.glViewport(this.viewport[0], this.viewport[1], this.viewport[2], this.viewport[3]);
	}

	/**
	 * Restores the render state captured when this scope was created.
	 */
	@Override
	public void close()
	{
		RenderSystem.assertOnRenderThread();
		ShaderInstance current = RenderSystem.getShader();
		if (current != null)
		{
			current.clear();
		}
		RenderSystem.setShader(() -> this.shader);
		RenderSystem.getModelViewStack().set(this.modelView);
		RenderSystem.applyModelViewMatrix();
		RenderSystem.setProjectionMatrix(this.projection, this.sorting);
		for (int unit = 0; unit < this.shaderTextures.length; unit++)
		{
			RenderSystem.setShaderTexture(unit, this.shaderTextures[unit]);
			GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
			GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this.bufferTextures[unit]);
		}
		GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this.textureBuffer);
		if (this.indirectSupported)
		{
			GL15.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, this.indirectBuffer);
		}
		this.state.restoreBindings();
		this.state.restore();
		if (this.cull)
		{
			RenderSystem.enableCull();
		}
		else
		{
			RenderSystem.disableCull();
		}
		RenderSystem.setShaderColor(this.shaderColor[0], this.shaderColor[1], this.shaderColor[2], this.shaderColor[3]);
		GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, this.drawFramebuffer);
		GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, this.readFramebuffer);
		RenderSystem.viewport(this.viewport[0], this.viewport[1], this.viewport[2], this.viewport[3]);
		GL11.glScissor(this.scissor[0], this.scissor[1], this.scissor[2], this.scissor[3]);
	}
}
