/**
 * @author ArcAnc
 * Created at: 15.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.legacy;

import com.arcanc.pulselib.util.PRenderTypes;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.ARBDrawBuffersBlend;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL40;

public final class GlWeightedBlendedOit
{
	public static final int LAYER_COUNT = 4;
	private static final float[] CLEAR_ACCUMULATION = {0f, 0f, 0f, 0f};
	private static final float[] CLEAR_DEPTH = {1f, 1f, 1f, 1f};

	private final int[] accumulationTextures = new int[LAYER_COUNT];
	private final int[] revealageTextures = new int[LAYER_COUNT];
	private final int[] layerDepthTextures = new int[LAYER_COUNT];
	private final boolean[] clearNextDepthPass = new boolean[LAYER_COUNT];
	private final boolean[] clearNextAccumulationPass = new boolean[LAYER_COUNT];
	private final boolean[] hasContent = new boolean[LAYER_COUNT];
	private final int[] previousViewport = new int[4];
	private final GlStateSnapshot previousGlState = new GlStateSnapshot();

	private int framebuffer = -1;
	private int width = -1;
	private int height = -1;
	private int depthTexture = -1;
	private boolean frameOpen;
	private Pass pass = Pass.NONE;
	private int activeLayer = -1;
	private int previousDrawFramebuffer;
	private int previousReadFramebuffer;

	public GlWeightedBlendedOit()
	{
		for (int layer = 0; layer < LAYER_COUNT; layer++)
		{
			this.accumulationTextures[layer] = -1;
			this.revealageTextures[layer] = -1;
			this.layerDepthTextures[layer] = -1;
		}
	}

	public boolean isSupported()
	{
		return GL.getCapabilities().OpenGL40 || GL.getCapabilities().GL_ARB_draw_buffers_blend;
	}

	public boolean begin(RenderTarget depthSource)
	{
		if (!isSupported() || PRenderTypes.ShadersProvider.oitComposite() == null)
			return false;
		if (this.pass != Pass.NONE)
			throw new IllegalStateException("Weighted blended OIT pass is already active");
		if (depthSource.width <= 0 || depthSource.height <= 0 || depthSource.getDepthTextureId() < 0)
			return false;
		try
		{
			ensureBuffers(depthSource);
			if (!this.frameOpen)
			{
				this.frameOpen = true;
				for (int layer = 0; layer < LAYER_COUNT; layer++)
				{
					this.clearNextDepthPass[layer] = true;
					this.clearNextAccumulationPass[layer] = true;
				}
			}
			return true;
		}
		catch (RuntimeException ignored)
		{
			close();
			return false;
		}
	}

	public void beginDepthPass(int layer)
	{
		beginPass(Pass.DEPTH, layer);
	}

	public void beginAccumulationPass(int layer)
	{
		beginPass(Pass.ACCUMULATION, layer);
	}

	public void endPass()
	{
		if (this.pass == Pass.NONE)
			return;
		this.pass = Pass.NONE;
		this.activeLayer = -1;
		restoreFramebufferState();
	}

	public void bindForDraw()
	{
		if (this.pass == Pass.NONE)
			throw new IllegalStateException("No weighted blended OIT pass is active");
		if (this.pass == Pass.DEPTH)
			configureDepthPass(false);
		else
			configureAccumulationPass(false);
	}

	public int activeLayerDepthTexture()
	{
		return layerDepthTexture(this.activeLayer);
	}

	public int previousLayerDepthTexture()
	{
		if (this.activeLayer <= 0)
			throw new IllegalStateException("The first weighted blended OIT layer has no predecessor");
		return layerDepthTexture(this.activeLayer - 1);
	}

	public void markContent(int layer)
	{
		this.hasContent[layer] = true;
	}

	public void composite(RenderTarget destination)
	{
		ShaderInstance shader = PRenderTypes.ShadersProvider.oitComposite();
		if (shader == null || !this.frameOpen || this.framebuffer < 0)
		{
			finishFrame();
			return;
		}
		if (this.pass != Pass.NONE)
			throw new IllegalStateException("Cannot composite weighted blended OIT while a pass is active");

		captureFramebufferState();
		destination.bindWrite(true);
		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		try
		{
			for (int layer = LAYER_COUNT - 1; layer >= 0; layer--)
			{
				if (!this.hasContent[layer])
					continue;
				shader.setSampler("AccumSampler", this.accumulationTextures[layer]);
				shader.setSampler("RevealSampler", this.revealageTextures[layer]);
				shader.setSampler("DepthSampler", this.layerDepthTextures[layer]);
				shader.apply();
				try
				{
					BufferBuilder buffer = RenderSystem.renderThreadTesselator().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLIT_SCREEN);
					buffer.addVertex(0f, 0f, 0f);
					buffer.addVertex(1f, 0f, 0f);
					buffer.addVertex(1f, 1f, 0f);
					buffer.addVertex(0f, 1f, 0f);
					BufferUploader.draw(buffer.buildOrThrow());
				}
				finally
				{
					shader.clear();
				}
			}
		}
		finally
		{
			RenderSystem.disableBlend();
			RenderSystem.depthMask(true);
			RenderSystem.enableDepthTest();
			restoreFramebufferState();
			finishFrame();
		}
	}

	public void close()
	{
		endPass();
		for (int layer = 0; layer < LAYER_COUNT; layer++)
		{
			deleteTexture(this.accumulationTextures[layer]);
			deleteTexture(this.revealageTextures[layer]);
			deleteTexture(this.layerDepthTextures[layer]);
			this.accumulationTextures[layer] = -1;
			this.revealageTextures[layer] = -1;
			this.layerDepthTextures[layer] = -1;
		}
		if (this.framebuffer >= 0)
			GL30.glDeleteFramebuffers(this.framebuffer);
		this.framebuffer = -1;
		this.width = -1;
		this.height = -1;
		this.depthTexture = -1;
		finishFrame();
	}

	private void beginPass(Pass nextPass, int layer)
	{
		if (!this.frameOpen || layer < 0 || layer >= LAYER_COUNT)
			throw new IllegalStateException("Weighted blended OIT frame is not ready");
		if (this.pass != Pass.NONE)
			throw new IllegalStateException("Weighted blended OIT pass is already active");
		captureFramebufferState();
		this.pass = nextPass;
		this.activeLayer = layer;
		if (nextPass == Pass.DEPTH)
			configureDepthPass(true);
		else
			configureAccumulationPass(true);
	}

	private void configureDepthPass(boolean clear)
	{
		GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, this.framebuffer);
		GL11.glViewport(0, 0, this.width, this.height);
		attachDepth();
		attachColor(0, layerDepthTexture(this.activeLayer));
		attachColor(1, 0);
		GL20.glDrawBuffers(new int[]{GL30.GL_COLOR_ATTACHMENT0});
		GL11.glDisable(GL11.GL_SCISSOR_TEST);
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(false);
		configureMinimumBlend(0);
		GL30.glDisablei(GL11.GL_BLEND, 1);
		if (clear && this.clearNextDepthPass[this.activeLayer])
		{
			GL30.glClearBufferfv(GL11.GL_COLOR, 0, CLEAR_DEPTH);
			this.clearNextDepthPass[this.activeLayer] = false;
		}
		ensureFramebufferComplete();
	}

	private void configureAccumulationPass(boolean clear)
	{
		GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, this.framebuffer);
		GL11.glViewport(0, 0, this.width, this.height);
		attachDepth();
		attachColor(0, this.accumulationTextures[this.activeLayer]);
		attachColor(1, this.revealageTextures[this.activeLayer]);
		GL20.glDrawBuffers(new int[]{GL30.GL_COLOR_ATTACHMENT0, GL30.GL_COLOR_ATTACHMENT1});
		GL11.glDisable(GL11.GL_SCISSOR_TEST);
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(false);
		configureAdditiveBlend(0);
		configureAdditiveBlend(1);
		if (clear && this.clearNextAccumulationPass[this.activeLayer])
		{
			GL30.glClearBufferfv(GL11.GL_COLOR, 0, CLEAR_ACCUMULATION);
			GL30.glClearBufferfv(GL11.GL_COLOR, 1, CLEAR_ACCUMULATION);
			this.clearNextAccumulationPass[this.activeLayer] = false;
		}
		ensureFramebufferComplete();
	}

	private void ensureBuffers(RenderTarget target)
	{
		if (this.framebuffer >= 0 && this.width == target.width && this.height == target.height && this.depthTexture == target.getDepthTextureId())
			return;
		close();
		this.width = target.width;
		this.height = target.height;
		this.depthTexture = target.getDepthTextureId();
		this.framebuffer = GL30.glGenFramebuffers();
		for (int layer = 0; layer < LAYER_COUNT; layer++)
		{
			this.accumulationTextures[layer] = createTexture(GL30.GL_RGBA16F, GL11.GL_RGBA, this.width, this.height);
			this.revealageTextures[layer] = createTexture(GL30.GL_R16F, GL11.GL_RED, this.width, this.height);
			this.layerDepthTextures[layer] = createTexture(GL30.GL_R32F, GL11.GL_RED, this.width, this.height);
		}
	}

	private void attachColor(int index, int texture)
	{
		GL32.glFramebufferTexture(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0 + index, texture, 0);
	}

	private void attachDepth()
	{
		GL32.glFramebufferTexture(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, this.depthTexture, 0);
	}

	private int layerDepthTexture(int layer)
	{
		if (layer < 0 || layer >= LAYER_COUNT || this.layerDepthTextures[layer] < 0)
			throw new IllegalStateException("Weighted blended OIT has no depth texture for layer " + layer);
		return this.layerDepthTextures[layer];
	}

	private void ensureFramebufferComplete()
	{
		if (GL30.glCheckFramebufferStatus(GL30.GL_DRAW_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE)
			throw new IllegalStateException("Unable to create weighted blended OIT framebuffer");
	}

	private void captureFramebufferState()
	{
		this.previousDrawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
		this.previousReadFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
		GL11.glGetIntegerv(GL11.GL_VIEWPORT, this.previousViewport);
		this.previousGlState.capture();
	}

	private void restoreFramebufferState()
	{
		GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, this.previousDrawFramebuffer);
		GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, this.previousReadFramebuffer);
		GL11.glViewport(this.previousViewport[0], this.previousViewport[1], this.previousViewport[2], this.previousViewport[3]);
		this.previousGlState.restore();
	}

	private void finishFrame()
	{
		this.frameOpen = false;
		this.pass = Pass.NONE;
		this.activeLayer = -1;
		for (int layer = 0; layer < LAYER_COUNT; layer++)
		{
			this.clearNextDepthPass[layer] = false;
			this.clearNextAccumulationPass[layer] = false;
			this.hasContent[layer] = false;
		}
	}

	private static void configureAdditiveBlend(int target)
	{
		GL30.glEnablei(GL11.GL_BLEND, target);
		if (GL.getCapabilities().OpenGL40)
		{
			GL40.glBlendEquationSeparatei(target, GL14.GL_FUNC_ADD, GL14.GL_FUNC_ADD);
			GL40.glBlendFuncSeparatei(target, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ONE);
		}
		else
		{
			ARBDrawBuffersBlend.glBlendEquationSeparateiARB(target, GL14.GL_FUNC_ADD, GL14.GL_FUNC_ADD);
			ARBDrawBuffersBlend.glBlendFuncSeparateiARB(target, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ONE);
		}
	}

	private static void configureMinimumBlend(int target)
	{
		GL30.glEnablei(GL11.GL_BLEND, target);
		if (GL.getCapabilities().OpenGL40)
		{
			GL40.glBlendEquationSeparatei(target, GL14.GL_MIN, GL14.GL_MIN);
			GL40.glBlendFuncSeparatei(target, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ONE);
		}
		else
		{
			ARBDrawBuffersBlend.glBlendEquationSeparateiARB(target, GL14.GL_MIN, GL14.GL_MIN);
			ARBDrawBuffersBlend.glBlendFuncSeparateiARB(target, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ONE);
		}
	}

	private static int createTexture(int internalFormat, int format, int width, int height)
	{
		int texture = GL11.glGenTextures();
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, GL11.GL_FLOAT, 0L);
		return texture;
	}

	private static void deleteTexture(int texture)
	{
		if (texture >= 0)
			GL11.glDeleteTextures(texture);
	}

	private enum Pass
	{
		NONE,
		DEPTH,
		ACCUMULATION
	}
}
