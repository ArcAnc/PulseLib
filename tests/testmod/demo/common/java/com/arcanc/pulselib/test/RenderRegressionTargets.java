/**
 * @author ArcAnc
 * Created at: 10.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test;

import com.arcanc.pulselib.content.renderer.legacy.GlRenderState;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;

/** Native views/readback and a GPU copy of the vanilla PiP output; contains no rendering oracle. */
final class RenderRegressionTargets
{
	private RenderRegressionTargets()
	{
	}

	static void bind(RenderTarget target)
	{
		RenderSystem.outputColorTextureOverride = target.getColorTextureView();
		RenderSystem.outputDepthTextureOverride = target.getDepthTextureView();
		GlStateManager._viewport(0, 0, target.width, target.height);
	}

	static int colored(RenderTarget target)
	{
		try (var binding = new H14GpuTargets.Binding(target, false))
		{
			var pixels = BufferUtils.createByteBuffer(target.width * target.height * 4);
			GL11.glReadPixels(0, 0, target.width, target.height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
			int colored = 0;
			for (int offset = 0; offset < pixels.capacity(); offset += 4)
				if ((pixels.get(offset) & 255) + (pixels.get(offset + 1) & 255) + (pixels.get(offset + 2) & 255) > 15)
					colored++;
			return colored;
		}
	}

	static void copy(GlTexture source, RenderTarget destination)
	{
		int read = GL30.glGenFramebuffers();
		try (var state = new GlRenderState(); var binding = new H14GpuTargets.Binding(destination, true))
		{
			GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, read);
			GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, source.glId(), 0);
			GlStateManager._disableScissorTest();
			GL30.glBlitFramebuffer(0, 0, source.getWidth(0), source.getHeight(0), 0, 0, destination.width, destination.height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
		}
		finally
		{
			GL30.glDeleteFramebuffers(read);
		}
	}
	static int alpha(GlTexture texture)
	{
		int read = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
		int packBuffer = GL11.glGetInteger(GL21.GL_PIXEL_PACK_BUFFER_BINDING);
		int[] parameters = {GL11.GL_PACK_ROW_LENGTH, GL11.GL_PACK_ALIGNMENT, GL11.GL_PACK_SKIP_ROWS, GL11.GL_PACK_SKIP_PIXELS};
		int[] previous = new int[parameters.length];
		for (int i = 0; i < parameters.length; i++)
			previous[i] = GL11.glGetInteger(parameters[i]);
		int fbo = GL30.glGenFramebuffers();
		try
		{
			GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0);
			for (int parameter : parameters)
				GL11.glPixelStorei(parameter, parameter == GL11.GL_PACK_ALIGNMENT ? 1 : 0);
			GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, fbo);
			GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, texture.glId(), 0);
			var pixels = BufferUtils.createByteBuffer(texture.getWidth(0) * texture.getHeight(0) * 4);
			GL11.glReadPixels(0, 0, texture.getWidth(0), texture.getHeight(0), GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
			int nonzero = 0;
			for (int offset = 3; offset < pixels.capacity(); offset += 4)
				if (pixels.get(offset) != 0)
					nonzero++;
			return nonzero;
		}
		finally
		{
			GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, read);
			GL30.glDeleteFramebuffers(fbo);
			GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, packBuffer);
			for (int i = 0; i < parameters.length; i++)
				GL11.glPixelStorei(parameters[i], previous[i]);
		}
	}

}
