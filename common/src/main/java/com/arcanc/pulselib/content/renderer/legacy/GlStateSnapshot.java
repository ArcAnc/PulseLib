package com.arcanc.pulselib.content.renderer.legacy;

import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.ARBDrawBuffersBlend;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL40;

/** Restores the state touched by OIT, including indexed blend state and Minecraft's cache. */
final class GlStateSnapshot
{
	private boolean[] blend;
	private int[][] factors;
	private boolean depth;
	private boolean depthMask;
	private boolean scissor;
	private int depthFunc;

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
