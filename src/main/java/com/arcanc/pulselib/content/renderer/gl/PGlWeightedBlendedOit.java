/**
 * @author ArcAnc
 * Created at: 21.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.gl;

import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PRenderTypes;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.opengl.GlTextureView;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.ARBDrawBuffersBlend;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL40;

import java.util.OptionalDouble;
import java.util.OptionalInt;

final class PGlWeightedBlendedOit
{
	static final int LAYER_COUNT = 4;
	private static final float[] CLEAR_ACCUMULATION = {0.0F, 0.0F, 0.0F, 0.0F};
	private static final float[] CLEAR_DEPTH = {1.0F, 1.0F, 1.0F, 1.0F};

	private int framebuffer = -1;
	private final @Nullable ExternalGlTexture[] accumulationTextures = new ExternalGlTexture[LAYER_COUNT];
	private final @Nullable ExternalGlTextureView[] accumulationViews = new ExternalGlTextureView[LAYER_COUNT];
	private final @Nullable ExternalGlTexture[] revealageTextures = new ExternalGlTexture[LAYER_COUNT];
	private final @Nullable ExternalGlTextureView[] revealageViews = new ExternalGlTextureView[LAYER_COUNT];
	private final @Nullable ExternalGlTexture[] layerDepthTextures = new ExternalGlTexture[LAYER_COUNT];
	private final @Nullable ExternalGlTextureView[] layerDepthViews = new ExternalGlTextureView[LAYER_COUNT];
	private int width = -1;
	private int height = -1;
	private int depthTexture = -1;
	private boolean depthStencil;
	private boolean disabled;
	private boolean frameOpen;
	private final boolean[] clearNextDepthPass = new boolean[LAYER_COUNT];
	private final boolean[] clearNextAccumulationPass = new boolean[LAYER_COUNT];
	private final boolean[] hasContent = new boolean[LAYER_COUNT];
	private int destinationColorTexture = -1;
	private @Nullable GpuTextureView destinationColor;
	private @Nullable GpuTextureView destinationDepth;
	private @Nullable GlStateSnapshot passState;
	private Pass pass = Pass.NONE;
	private int activeLayer = -1;

	/**
	 * Performs the begin operation.
	 * @param colorAttachment the color attachment to use.
	 * @param depthAttachment the depth attachment to use.
	 * @return the value produced by this operation.
	 */
	public boolean begin(GpuTextureView colorAttachment, @Nullable GpuTextureView depthAttachment)
	{
		if (this.disabled || !isSupported())
			return false;
		if (this.pass != Pass.NONE)
			throw new IllegalStateException("Weighted blended OIT pass is already active");
		if (depthAttachment == null || colorAttachment.getWidth(0) <= 0 || colorAttachment.getHeight(0) <= 0)
			return false;
		if (this.frameOpen && !matchesFrame(colorAttachment, depthAttachment))
			return false;

		try
		{
			this.ensureBuffers(colorAttachment, depthAttachment);
			if (!this.frameOpen)
			{
				this.destinationColor = colorAttachment;
				this.destinationDepth = depthAttachment;
				this.destinationColorTexture = colorTexture(colorAttachment);
				this.frameOpen = true;
				for (int layer = 0; layer < LAYER_COUNT; layer++)
				{
					this.clearNextDepthPass[layer] = true;
					this.clearNextAccumulationPass[layer] = true;
				}
			}
			return true;
		}
		catch (RuntimeException exception)
		{
			PLibDatabase.LOGGER.warn("Disabling weighted blended OIT after framebuffer initialization failed", exception);
			this.disabled = true;
			this.closeBuffers();
			this.finishFrame();
			return false;
		}
	}

	/**
	 * Performs the begin depth pass operation.
	 * @param layer the layer to use.
	 */
	public void beginDepthPass(int layer)
	{
		beginPass(Pass.DEPTH, layer);
	}

	/**
	 * Performs the begin accumulation pass operation.
	 * @param layer the layer to use.
	 */
	public void beginAccumulationPass(int layer)
	{
		beginPass(Pass.ACCUMULATION, layer);
	}

	/**
	 * Binds the for draw.
	 */
	public void bindForDraw()
	{
		if (this.pass == Pass.NONE)
			throw new IllegalStateException("No weighted blended OIT pass is active");
		if (this.pass == Pass.DEPTH)
			configureDepthPass(false);
		else
			configureAccumulationPass(false);
	}

	/**
	 * Performs the end pass operation.
	 */
	public void endPass()
	{
		if (this.pass == Pass.NONE)
			return;
		GlStateSnapshot state = this.passState;
		this.passState = null;
		this.pass = Pass.NONE;
		this.activeLayer = -1;
		if (state != null)
			state.restore();
	}

	/**
	 * Performs the layer depth view operation.
	 * @param layer the layer to use.
	 * @return the value produced by this operation.
	 */
	public GpuTextureView layerDepthView(int layer)
	{
		ExternalGlTextureView view = this.layerDepthViews[layer];
		if (view == null)
			throw new IllegalStateException("Weighted blended OIT has no depth layer " + layer);
		return view;
	}

	/**
	 * Performs the active layer depth view operation.
	 * @return the value produced by this operation.
	 */
	public GpuTextureView activeLayerDepthView()
	{
		if (this.activeLayer < 0)
			throw new IllegalStateException("No weighted blended OIT layer is active");
		return layerDepthView(this.activeLayer);
	}

	/**
	 * Performs the previous layer depth view operation.
	 * @return the value produced by this operation.
	 */
	public GpuTextureView previousLayerDepthView()
	{
		if (this.activeLayer <= 0)
			throw new IllegalStateException("The first weighted blended OIT layer has no predecessor");
		return layerDepthView(this.activeLayer - 1);
	}

	/**
	 * Performs the mark content operation.
	 * @param layer the layer to use.
	 */
	public void markContent(int layer)
	{
		this.hasContent[layer] = true;
	}

	/**
	 * Performs the begin pass operation.
	 * @param nextPass the next pass to use.
	 * @param layer the layer to use.
	 */
	private void beginPass(Pass nextPass, int layer)
	{
		if (!this.frameOpen || layer < 0 || layer >= LAYER_COUNT)
			throw new IllegalStateException("Weighted blended OIT frame is not ready");
		if (this.pass != Pass.NONE)
			throw new IllegalStateException("Weighted blended OIT pass is already active");
		this.passState = GlStateSnapshot.capture();
		this.pass = nextPass;
		this.activeLayer = layer;
		if (nextPass == Pass.DEPTH)
			configureDepthPass(true);
		else
			configureAccumulationPass(true);
	}

	/**
	 * Performs the configure depth pass operation.
	 * @param clear the clear to use.
	 */
	private void configureDepthPass(boolean clear)
	{
		GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.framebuffer);
		GlStateManager._viewport(0, 0, this.width, this.height);
		attachColor(0, layerDepthTexture(this.activeLayer));
		attachColor(1, null);
		attachDepth(this.depthTexture, this.depthStencil);
		GL20.glDrawBuffers(new int[]{GL30.GL_COLOR_ATTACHMENT0});
		GlStateManager._disableScissorTest();
		GlStateManager._enableBlend();
		configureMinimumBlend(0);
		GL30.glDisablei(GL11.GL_BLEND, 1);
		GlStateManager._enableDepthTest();
		GlStateManager._depthFunc(GL11.GL_LEQUAL);
		GlStateManager._depthMask(false);
		if (clear && this.clearNextDepthPass[this.activeLayer])
		{
			GL30.glClearBufferfv(GL11.GL_COLOR, 0, CLEAR_DEPTH);
			this.clearNextDepthPass[this.activeLayer] = false;
		}
		ensureFramebufferComplete();
	}

	/**
	 * Performs the configure accumulation pass operation.
	 * @param clear the clear to use.
	 */
	private void configureAccumulationPass(boolean clear)
	{
		GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.framebuffer);
		GlStateManager._viewport(0, 0, this.width, this.height);
		attachColor(0, accumulationTexture(this.activeLayer));
		attachColor(1, revealageTexture(this.activeLayer));
		attachDepth(this.depthTexture, this.depthStencil);
		GL20.glDrawBuffers(new int[]{GL30.GL_COLOR_ATTACHMENT0, GL30.GL_COLOR_ATTACHMENT1});
		GlStateManager._disableScissorTest();
		GlStateManager._enableBlend();
		configureAdditiveBlend(0);
		configureAdditiveBlend(1);
		GlStateManager._enableDepthTest();
		GlStateManager._depthFunc(GL11.GL_LEQUAL);
		GlStateManager._depthMask(false);
		if (clear && this.clearNextAccumulationPass[this.activeLayer])
		{
			GL30.glClearBufferfv(GL11.GL_COLOR, 0, CLEAR_ACCUMULATION);
			GL30.glClearBufferfv(GL11.GL_COLOR, 1, CLEAR_ACCUMULATION);
			this.clearNextAccumulationPass[this.activeLayer] = false;
		}
		ensureFramebufferComplete();
	}

	/**
	 * Performs the composite operation.
	 */
	public void composite()
	{
		if (!this.frameOpen)
		{
			this.finishFrame();
			return;
		}
		if (this.pass != Pass.NONE)
			throw new IllegalStateException("Cannot composite weighted blended OIT while a pass is active");
		GpuTextureView colorAttachment = this.destinationColor;
		if (colorAttachment == null)
			throw new IllegalStateException("Weighted blended OIT frame has no destination color attachment");
		GpuTextureView depthAttachment = this.destinationDepth;
		if (depthAttachment == null)
			throw new IllegalStateException("Weighted blended OIT frame has no destination depth attachment");
		GlStateSnapshot state = GlStateSnapshot.capture();
		try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
				() -> "PulseLib weighted OIT composite", colorAttachment, OptionalInt.empty(), depthAttachment, OptionalDouble.empty()))
		{
			pass.setPipeline(PRenderTypes.RenderPipelinesProvider.OIT_COMPOSITE);
			pass.disableScissor();
			var sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
			for (int layer = LAYER_COUNT - 1; layer >= 0; layer--)
			{
				if (!this.hasContent[layer])
					continue;
				pass.bindTexture("AccumSampler", accumulationView(layer), sampler);
				pass.bindTexture("RevealSampler", revealageView(layer), sampler);
				pass.bindTexture("DepthSampler", layerDepthView(layer), sampler);
				pass.draw(0, 3);
			}
		}
		finally
		{
			state.restore();
			this.finishFrame();
		}
	}

	/**
	 * Performs the close operation.
	 */
	public void close()
	{
		this.endPass();
		this.closeBuffers();
		this.finishFrame();
		this.disabled = false;
	}

	/**
	 * Performs the finish frame operation.
	 */
	private void finishFrame()
	{
		this.frameOpen = false;
		this.pass = Pass.NONE;
		this.activeLayer = -1;
		this.passState = null;
		for (int layer = 0; layer < LAYER_COUNT; layer++)
		{
			this.clearNextDepthPass[layer] = false;
			this.clearNextAccumulationPass[layer] = false;
			this.hasContent[layer] = false;
		}
		this.destinationColorTexture = -1;
		this.destinationColor = null;
		this.destinationDepth = null;
	}

	/**
	 * Determines whether supported.
	 * @return the value produced by this operation.
	 */
	private static boolean isSupported()
	{
		return GL.getCapabilities().OpenGL40 || GL.getCapabilities().GL_ARB_draw_buffers_blend;
	}

	/**
	 * Performs the ensure buffers operation.
	 * @param colorAttachment the color attachment to use.
	 * @param depthAttachment the depth attachment to use.
	 */
	private void ensureBuffers(GpuTextureView colorAttachment, @Nullable GpuTextureView depthAttachment)
	{
		if (matchesBuffers(colorAttachment, depthAttachment))
			return;

		int targetWidth = colorAttachment.getWidth(0);
		int targetHeight = colorAttachment.getHeight(0);
		int targetDepth = depthTexture(depthAttachment);
		boolean targetDepthStencil = hasDepthStencil(depthAttachment);
		this.closeBuffers();
		this.width = targetWidth;
		this.height = targetHeight;
		this.depthTexture = targetDepth;
		this.depthStencil = targetDepthStencil;
		for (int layer = 0; layer < LAYER_COUNT; layer++)
		{
			this.accumulationTextures[layer] = createTexture("PulseLib OIT accumulation " + layer, TextureFormat.RGBA8,
					GL30.GL_RGBA16F, GL11.GL_RGBA, targetWidth, targetHeight);
			this.accumulationViews[layer] = new ExternalGlTextureView(this.accumulationTextures[layer]);
			this.revealageTextures[layer] = createTexture("PulseLib OIT revealage " + layer, TextureFormat.RED8,
					GL30.GL_R16F, GL11.GL_RED, targetWidth, targetHeight);
			this.revealageViews[layer] = new ExternalGlTextureView(this.revealageTextures[layer]);
			this.layerDepthTextures[layer] = createTexture("PulseLib OIT depth layer " + layer, TextureFormat.RED8,
					GL30.GL_R32F, GL11.GL_RED, targetWidth, targetHeight);
			this.layerDepthViews[layer] = new ExternalGlTextureView(this.layerDepthTextures[layer]);
		}
		this.framebuffer = GlStateManager.glGenFramebuffers();
	}

	/**
	 * Performs the matches buffers operation.
	 * @param colorAttachment the color attachment to use.
	 * @param depthAttachment the depth attachment to use.
	 * @return the value produced by this operation.
	 */
	private boolean matchesBuffers(GpuTextureView colorAttachment, @Nullable GpuTextureView depthAttachment)
	{
		return this.framebuffer >= 0 && this.width == colorAttachment.getWidth(0) &&
				this.height == colorAttachment.getHeight(0) && this.depthTexture == depthTexture(depthAttachment) &&
				this.depthStencil == hasDepthStencil(depthAttachment);
	}

	/**
	 * Performs the matches frame operation.
	 * @param colorAttachment the color attachment to use.
	 * @param depthAttachment the depth attachment to use.
	 * @return the value produced by this operation.
	 */
	private boolean matchesFrame(GpuTextureView colorAttachment, @Nullable GpuTextureView depthAttachment)
	{
		return matchesBuffers(colorAttachment, depthAttachment) &&
				this.destinationColorTexture == colorTexture(colorAttachment);
	}

	/**
	 * Performs the color texture operation.
	 * @param colorAttachment the color attachment to use.
	 * @return the value produced by this operation.
	 */
	private static int colorTexture(GpuTextureView colorAttachment)
	{
		return ((GlTexture)colorAttachment.texture()).glId();
	}

	/**
	 * Performs the depth texture operation.
	 * @param depthAttachment the depth attachment to use.
	 * @return the value produced by this operation.
	 */
	private static int depthTexture(@Nullable GpuTextureView depthAttachment)
	{
		return depthAttachment == null ? 0 : ((GlTexture)depthAttachment.texture()).glId();
	}

	/**
	 * Determines whether the object has depth stencil.
	 * @param depthAttachment the depth attachment to use.
	 * @return the value produced by this operation.
	 */
	private static boolean hasDepthStencil(@Nullable GpuTextureView depthAttachment)
	{
		return depthAttachment != null && depthAttachment.texture().getFormat().hasStencilAspect();
	}

	/**
	 * Closes the buffers.
	 */
	private void closeBuffers()
	{
		for (int layer = 0; layer < LAYER_COUNT; layer++)
		{
			close(this.accumulationViews[layer]);
			close(this.accumulationTextures[layer]);
			close(this.revealageViews[layer]);
			close(this.revealageTextures[layer]);
			close(this.layerDepthViews[layer]);
			close(this.layerDepthTextures[layer]);
			this.accumulationViews[layer] = null;
			this.accumulationTextures[layer] = null;
			this.revealageViews[layer] = null;
			this.revealageTextures[layer] = null;
			this.layerDepthViews[layer] = null;
			this.layerDepthTextures[layer] = null;
		}
		if (this.framebuffer >= 0)
			GlStateManager._glDeleteFramebuffers(this.framebuffer);
		this.framebuffer = -1;
		this.width = -1;
		this.height = -1;
		this.depthTexture = -1;
		this.depthStencil = false;
	}

	/**
	 * Performs the accumulation texture operation.
	 * @param layer the layer to use.
	 * @return the value produced by this operation.
	 */
	private ExternalGlTexture accumulationTexture(int layer)
	{
		return requireTexture(this.accumulationTextures[layer], "accumulation", layer);
	}

	/**
	 * Performs the revealage texture operation.
	 * @param layer the layer to use.
	 * @return the value produced by this operation.
	 */
	private ExternalGlTexture revealageTexture(int layer)
	{
		return requireTexture(this.revealageTextures[layer], "revealage", layer);
	}

	/**
	 * Performs the layer depth texture operation.
	 * @param layer the layer to use.
	 * @return the value produced by this operation.
	 */
	private ExternalGlTexture layerDepthTexture(int layer)
	{
		return requireTexture(this.layerDepthTextures[layer], "depth", layer);
	}

	/**
	 * Performs the accumulation view operation.
	 * @param layer the layer to use.
	 * @return the value produced by this operation.
	 */
	private GpuTextureView accumulationView(int layer)
	{
		return requireView(this.accumulationViews[layer], "accumulation", layer);
	}

	/**
	 * Performs the revealage view operation.
	 * @param layer the layer to use.
	 * @return the value produced by this operation.
	 */
	private GpuTextureView revealageView(int layer)
	{
		return requireView(this.revealageViews[layer], "revealage", layer);
	}

	/**
	 * Performs the require texture operation.
	 * @param texture the texture to use.
	 * @param name the name to use.
	 * @param layer the layer to use.
	 * @return the value produced by this operation.
	 */
	private static ExternalGlTexture requireTexture(@Nullable ExternalGlTexture texture, String name, int layer)
	{
		if (texture == null)
			throw new IllegalStateException("Weighted blended OIT has no " + name + " layer " + layer);
		return texture;
	}

	/**
	 * Performs the require view operation.
	 * @param view the view to use.
	 * @param name the name to use.
	 * @param layer the layer to use.
	 * @return the value produced by this operation.
	 */
	private static GpuTextureView requireView(@Nullable ExternalGlTextureView view, String name, int layer)
	{
		if (view == null)
			throw new IllegalStateException("Weighted blended OIT has no " + name + " view for layer " + layer);
		return view;
	}

	/**
	 * Performs the attach color operation.
	 * @param index the index to use.
	 * @param texture the texture to use.
	 */
	private void attachColor(int index, @Nullable ExternalGlTexture texture)
	{
		GL32.glFramebufferTexture(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0 + index, texture == null ? 0 : texture.glId(), 0);
	}

	/**
	 * Performs the attach depth operation.
	 * @param texture the texture to use.
	 * @param stencil the stencil to use.
	 */
	private void attachDepth(int texture, boolean stencil)
	{
		GL32.glFramebufferTexture(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, 0, 0);
		GL32.glFramebufferTexture(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT, 0, 0);
		if (texture != 0)
			GL32.glFramebufferTexture(GL30.GL_FRAMEBUFFER,
					stencil ? GL30.GL_DEPTH_STENCIL_ATTACHMENT : GL30.GL_DEPTH_ATTACHMENT, texture, 0);
	}

	/**
	 * Performs the ensure framebuffer complete operation.
	 */
	private void ensureFramebufferComplete()
	{
		int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
		if (status != GL30.GL_FRAMEBUFFER_COMPLETE)
			throw new IllegalStateException("Unable to create weighted OIT framebuffer: 0x" + Integer.toHexString(status));
	}

	/**
	 * Performs the configure additive blend operation.
	 * @param target the target to use.
	 */
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

	/**
	 * Performs the configure minimum blend operation.
	 * @param target the target to use.
	 */
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

	/**
	 * Performs the close operation.
	 * @param resource the resource to use.
	 */
	private static void close(@Nullable AutoCloseable resource)
	{
		if (resource == null)
			return;
		try
		{
			resource.close();
		}
		catch (Exception exception)
		{
			throw new RuntimeException(exception);
		}
	}

	private enum Pass
	{
		NONE,
		DEPTH,
		ACCUMULATION
	}

	/**
	 * Creates the texture.
	 * @param label the label to use.
	 * @param declaredFormat the declared format to use.
	 * @param internalFormat the internal format to use.
	 * @param format the format to use.
	 * @param width the width to use.
	 * @param height the height to use.
	 * @return the value produced by this operation.
	 */
	private static ExternalGlTexture createTexture(String label, TextureFormat declaredFormat,
	                                               int internalFormat, int format, int width, int height)
	{
		int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
		int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
		int texture = GL11.glGenTextures();
		try
		{
			GlStateManager._bindTexture(texture);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
			GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, GL11.GL_FLOAT, 0L);
			return new ExternalGlTexture(label, declaredFormat, width, height, texture);
		}
		finally
		{
			GlStateManager._bindTexture(previousTexture);
			GlStateManager._activeTexture(activeTexture);
		}
	}

	private static final class ExternalGlTexture extends GlTexture
	{
		/**
		 * Creates an instance of the enclosing type.
		 * @param label the label to use.
		 * @param format the format to use.
		 * @param width the width to use.
		 * @param height the height to use.
		 * @param id the id to use.
		 */
		private ExternalGlTexture(String label, TextureFormat format, int width, int height, int id)
		{
			super(GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT,
					label, format, width, height, 1, 1, id);
		}
	}

	private static final class ExternalGlTextureView extends GlTextureView
	{
		/**
		 * Creates an instance of the enclosing type.
		 * @param texture the texture to use.
		 */
		private ExternalGlTextureView(ExternalGlTexture texture)
		{
			super(texture, 0, 1);
		}
	}

	private static final class GlStateSnapshot
	{
		private final int drawFramebuffer;
		private final int readFramebuffer;
		private final int[] viewport = new int[4];
		private final int[] scissorBox = new int[4];
		private final boolean scissor;
		private final boolean depthTest;
		private final boolean depthMask;
		private final int depthFunction;
		private final BlendState blend0;
		private final BlendState blend1;
		private final BlendState blend2;

		/**
		 * Creates an instance of the enclosing type.
		 */
		private GlStateSnapshot()
		{
			this.drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
			this.readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
			GL11.glGetIntegerv(GL11.GL_VIEWPORT, this.viewport);
			GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, this.scissorBox);
			this.scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
			this.depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
			this.depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
			this.depthFunction = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
			this.blend0 = BlendState.capture(0);
			this.blend1 = BlendState.capture(1);
			this.blend2 = BlendState.capture(2);
		}

		/**
		 * Performs the capture operation.
		 * @return the value produced by this operation.
		 */
		private static GlStateSnapshot capture()
		{
			return new GlStateSnapshot();
		}

		/**
		 * Performs the restore operation.
		 */
		private void restore()
		{
			GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, this.drawFramebuffer);
			GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, this.readFramebuffer);
			GlStateManager._viewport(this.viewport[0], this.viewport[1], this.viewport[2], this.viewport[3]);
			GlStateManager._scissorBox(this.scissorBox[0], this.scissorBox[1], this.scissorBox[2], this.scissorBox[3]);
			if (this.scissor)
				GlStateManager._enableScissorTest();
			else
				GlStateManager._disableScissorTest();
			if (this.depthTest)
				GlStateManager._enableDepthTest();
			else
				GlStateManager._disableDepthTest();
			GlStateManager._depthMask(this.depthMask);
			GlStateManager._depthFunc(this.depthFunction);
			this.blend0.restorePrimary();
			this.blend1.restoreIndexed(1);
			this.blend2.restoreIndexed(2);
		}
	}

	private record BlendState(boolean enabled, int equationRgb, int equationAlpha,
	                          int sourceRgb, int destinationRgb, int sourceAlpha, int destinationAlpha)
	{
		/**
		 * Performs the capture operation.
		 * @param target the target to use.
		 * @return the value produced by this operation.
		 */
		private static BlendState capture(int target)
		{
			return new BlendState(
					GL30.glIsEnabledi(GL11.GL_BLEND, target),
					GL30.glGetIntegeri(GL20.GL_BLEND_EQUATION_RGB, target),
					GL30.glGetIntegeri(GL20.GL_BLEND_EQUATION_ALPHA, target),
					GL30.glGetIntegeri(GL14.GL_BLEND_SRC_RGB, target),
					GL30.glGetIntegeri(GL14.GL_BLEND_DST_RGB, target),
					GL30.glGetIntegeri(GL14.GL_BLEND_SRC_ALPHA, target),
					GL30.glGetIntegeri(GL14.GL_BLEND_DST_ALPHA, target));
		}

		/**
		 * Performs the restore primary operation.
		 */
		private void restorePrimary()
		{
			if (this.enabled)
				GlStateManager._enableBlend();
			else
				GlStateManager._disableBlend();
			GL20.glBlendEquationSeparate(this.equationRgb, this.equationAlpha);
			GlStateManager._blendFuncSeparate(this.sourceRgb, this.destinationRgb, this.sourceAlpha, this.destinationAlpha);
		}

		/**
		 * Performs the restore indexed operation.
		 * @param target the target to use.
		 */
		private void restoreIndexed(int target)
		{
			if (this.enabled)
				GL30.glEnablei(GL11.GL_BLEND, target);
			else
				GL30.glDisablei(GL11.GL_BLEND, target);
			restoreEquation(target, this.equationRgb, this.equationAlpha);
			restoreFunction(target, this.sourceRgb, this.destinationRgb, this.sourceAlpha, this.destinationAlpha);
		}

		/**
		 * Performs the restore equation operation.
		 * @param target the target to use.
		 * @param rgb the rgb to use.
		 * @param alpha the alpha to use.
		 */
		private static void restoreEquation(int target, int rgb, int alpha)
		{
			if (GL.getCapabilities().OpenGL40)
				GL40.glBlendEquationSeparatei(target, rgb, alpha);
			else
				ARBDrawBuffersBlend.glBlendEquationSeparateiARB(target, rgb, alpha);
		}

		/**
		 * Performs the restore function operation.
		 * @param target the target to use.
		 * @param sourceRgb the source rgb to use.
		 * @param destinationRgb the destination rgb to use.
		 * @param sourceAlpha the source alpha to use.
		 * @param destinationAlpha the destination alpha to use.
		 */
		private static void restoreFunction(int target, int sourceRgb, int destinationRgb,
		                                    int sourceAlpha, int destinationAlpha)
		{
			if (GL.getCapabilities().OpenGL40)
				GL40.glBlendFuncSeparatei(target, sourceRgb, destinationRgb, sourceAlpha, destinationAlpha);
			else
				ARBDrawBuffersBlend.glBlendFuncSeparateiARB(target, sourceRgb, destinationRgb, sourceAlpha, destinationAlpha);
		}
	}
}
