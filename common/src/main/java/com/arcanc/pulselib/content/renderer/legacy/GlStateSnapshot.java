/**
 * @author ArcAnc
 * Created at: 12.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.legacy;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.BufferUploader;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.*;

/**
 * Restores the state touched by OIT, including indexed blend state and Minecraft's cache.
 */
final class GlStateSnapshot
{
	private @Nullable boolean[] blend;
	private @Nullable int[][] factors;
	private boolean depth;
	private boolean depthMask;
	private boolean scissor;
	private int depthFunc;
	private int program;
	private int vertexArray;
	private int arrayBuffer;
	private int elementBuffer;
	private int activeTexture;
	private int activeTextureBinding;
	private int[] textures = new int[0];

	void captureBindings(int textureUnits)
	{
		this.program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
		this.vertexArray = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
		this.arrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
		this.elementBuffer = GL11.glGetInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING);
		this.activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
		this.activeTextureBinding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
		if (this.textures.length != textureUnits)
		{
			this.textures = new int[textureUnits];
		}
		for (int unit = 0; unit < this.textures.length; unit++)
		{
			activeTexture(GL13.GL_TEXTURE0 + unit);
			this.textures[unit] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
		}
		activeTexture(this.activeTexture);
	}

	void restoreBindings()
	{
		// Composite shader.clear() invalidates ShaderInstance's program cache.
		GL20.glUseProgram(this.program);
		BufferUploader.invalidate();
		GL30.glBindVertexArray(this.vertexArray);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.arrayBuffer);
		// In a core profile the element binding belongs to a nonzero VAO.
		if (this.vertexArray != 0)
		{
			GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, this.elementBuffer);
		}
		for (int unit = 0; unit < this.textures.length; unit++)
		{
			activeTexture(GL13.GL_TEXTURE0 + unit);
			bindTexture(this.textures[unit]);
		}
		activeTexture(this.activeTexture);
		if (this.activeTexture < GL13.GL_TEXTURE0 + 12)
		{
			bindTexture(this.activeTextureBinding);
		}
		else
		{
			// Minecraft 1.21.1 caches only twelve units; other GL callers may use more.
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.activeTextureBinding);
		}
	}

	private static void activeTexture(int unit)
	{
		GL13.glActiveTexture(unit);
		GlStateManager._activeTexture(unit);
	}

	private static void bindTexture(int texture)
	{
		GlStateManager._bindTexture(0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
		GlStateManager._bindTexture(texture);
	}

	void capture()
	{
		if (this.blend == null)
		{
			int count = GL11.glGetInteger(GL20.GL_MAX_DRAW_BUFFERS);
			this.blend = new boolean[count];
			this.factors = new int[count][6];
		}
		for (int i = 0; i < this.blend.length; i++)
		{
			this.blend[i] = GL30.glIsEnabledi(GL11.GL_BLEND, i);
			int[] parameters = {GL14.GL_BLEND_SRC_RGB, GL14.GL_BLEND_DST_RGB,
				GL14.GL_BLEND_SRC_ALPHA, GL14.GL_BLEND_DST_ALPHA,
				GL20.GL_BLEND_EQUATION_RGB, GL20.GL_BLEND_EQUATION_ALPHA};
			for (int j = 0; j < parameters.length; j++)
				this.factors[i][j] = GL30.glGetIntegeri(parameters[j], i);
		}
		this.depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
		this.depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
		this.depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
		this.scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
	}

	void restore()
	{
		// Force the cached booleans to observe raw indexed GL changes.
		GlStateManager._enableBlend();
		GlStateManager._disableBlend();
		if (this.blend[0])
			GlStateManager._enableBlend();
		GlStateManager._blendFuncSeparate(GL11.GL_ZERO, GL11.GL_ONE, GL11.GL_ZERO, GL11.GL_ONE);
		GlStateManager._blendFuncSeparate(GL11.GL_ONE, GL11.GL_ZERO, GL11.GL_ONE, GL11.GL_ZERO);
		GlStateManager._blendFuncSeparate(this.factors[0][0], this.factors[0][1], this.factors[0][2], this.factors[0][3]);
		for (int i = 0; i < this.blend.length; i++)
		{
			if (this.blend[i])
				GL30.glEnablei(GL11.GL_BLEND, i);
			else
				GL30.glDisablei(GL11.GL_BLEND, i);
			int[] f = this.factors[i];
			if (GL.getCapabilities().OpenGL40)
			{
				GL40.glBlendFuncSeparatei(i, f[0], f[1], f[2], f[3]);
				GL40.glBlendEquationSeparatei(i, f[4], f[5]);
			}
			else
			{
				ARBDrawBuffersBlend.glBlendFuncSeparateiARB(i, f[0], f[1], f[2], f[3]);
				ARBDrawBuffersBlend.glBlendEquationSeparateiARB(i, f[4], f[5]);
			}
		}
		if (this.depth)
			GlStateManager._enableDepthTest();
		else
			GlStateManager._disableDepthTest();
		GlStateManager._depthMask(this.depthMask);
		GlStateManager._depthFunc(this.depthFunc);
		GlStateManager._enableScissorTest();
		GlStateManager._disableScissorTest();
		if (this.scissor)
			GlStateManager._enableScissorTest();
	}
}
