/**
 * @author ArcAnc
 * Created at: 10.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;

/** MC26 attachment/readback adapter; does not implement composition or a pixel oracle. */
final class H14GpuTargets
{
	private H14GpuTargets()
	{
	}

	static final class Binding implements AutoCloseable
	{
		private final boolean write;
		private final int draw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
		private final int read = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
		private final int fbo = GL30.glGenFramebuffers();
		private final int packBuffer = GL11.glGetInteger(GL21.GL_PIXEL_PACK_BUFFER_BINDING);
		private final int[] pack = new int[4];
		private final @Nullable GpuTextureView color = RenderSystem.outputColorTextureOverride;
		private final @Nullable GpuTextureView depth = RenderSystem.outputDepthTextureOverride;

		Binding(RenderTarget target, boolean write)
		{
			this.write = write;
			int[] parameters = {GL11.GL_PACK_ROW_LENGTH, GL11.GL_PACK_ALIGNMENT, GL11.GL_PACK_SKIP_ROWS, GL11.GL_PACK_SKIP_PIXELS};
			for (int i = 0; i < parameters.length; i++)
				this.pack[i] = GL11.glGetInteger(parameters[i]);
			GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0);
			GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, 0);
			GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
			GL11.glPixelStorei(GL11.GL_PACK_SKIP_ROWS, 0);
			GL11.glPixelStorei(GL11.GL_PACK_SKIP_PIXELS, 0);
			int binding = write ? GL30.GL_FRAMEBUFFER : GL30.GL_READ_FRAMEBUFFER;
			GlStateManager._glBindFramebuffer(binding, this.fbo);
			GL30.glFramebufferTexture2D(binding, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, ((GlTexture)target.getColorTexture()).glId(), 0);
			if (target.useDepth)
				GL30.glFramebufferTexture2D(binding, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, ((GlTexture)target.getDepthTexture()).glId(), 0);
			if (GL30.glCheckFramebufferStatus(binding) != GL30.GL_FRAMEBUFFER_COMPLETE)
				throw new AssertionError("H14 native read/write framebuffer incomplete");
			if (write)
			{
				GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
				RenderSystem.outputColorTextureOverride = target.getColorTextureView();
				RenderSystem.outputDepthTextureOverride = target.getDepthTextureView();
			}
			GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
		}

		@Override
		public void close()
		{
			if (this.write)
			{
				GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, this.draw);
				RenderSystem.outputColorTextureOverride = this.color;
				RenderSystem.outputDepthTextureOverride = this.depth;
			}
			GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, this.read);
			GL30.glDeleteFramebuffers(this.fbo);
			GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, this.packBuffer);
			int[] parameters = {GL11.GL_PACK_ROW_LENGTH, GL11.GL_PACK_ALIGNMENT, GL11.GL_PACK_SKIP_ROWS, GL11.GL_PACK_SKIP_PIXELS};
			for (int i = 0; i < parameters.length; i++)
				GL11.glPixelStorei(parameters[i], this.pack[i]);
		}
	}

	static final class ClearState implements AutoCloseable
	{
		private final double depth = GL11.glGetDouble(GL11.GL_DEPTH_CLEAR_VALUE);
		private final float[] color = new float[4];
		private final int active = GL11.glGetInteger(org.lwjgl.opengl.GL13.GL_ACTIVE_TEXTURE);
		private final int texture15;

		ClearState()
		{
			GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, this.color);
			GlStateManager._activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0 + 15);
			this.texture15 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
			GlStateManager._activeTexture(this.active);
		}

		@Override
		public void close()
		{
			GlStateManager._activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0 + 15);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.texture15);
			GlStateManager._activeTexture(this.active);
			GL11.glClearDepth(this.depth);
			GL11.glClearColor(this.color[0], this.color[1], this.color[2], this.color[3]);
		}
	}
}
