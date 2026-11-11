/**
 * @author ArcAnc
 * Created at: 15.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.legacy;

import com.arcanc.pulselib.content.renderer.gl.PGlWeightedBlendedOit;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import java.util.List;
import java.util.Objects;

/**
 * Owns layered depth and weighted color targets used to composite translucent model geometry.
 */
public final class GlWeightedBlendedOit
{
	public static final int LAYER_COUNT = PGlWeightedBlendedOit.LAYER_COUNT;
	private final PGlWeightedBlendedOit oit = new PGlWeightedBlendedOit();

	/** Returns whether native per-target weighted blending is supported. */
	public boolean isSupported()
	{
		return PGlWeightedBlendedOit.isSupported();
	}

	/** Starts the native owner using the source target's color and opaque depth. */
	public boolean begin(RenderTarget depthSource)
	{
		return this.oit.begin(Objects.requireNonNull(depthSource.getColorTextureView()), depthSource.getDepthTextureView());
	}

	/** Selects the native depth layer and captures caller GL state. */
	public void beginDepthPass(int layer)
	{
		this.oit.beginDepthPass(layer);
	}

	/** Selects the native accumulation/revealage layer. */
	public void beginAccumulationPass(int layer)
	{
		this.oit.beginAccumulationPass(layer);
	}

	/** Restores the caller state after a layer pass. */
	public void endPass()
	{
		this.oit.endPass();
	}

	/** Rebinds the active layer after applying native pipeline state. */
	public void bindForDraw()
	{
		this.oit.bindForDraw();
	}

	/** Returns the actual OpenGL name of the active native depth texture. */
	public int activeLayerDepthTexture()
	{
		return ((GlTexture)this.oit.activeLayerDepthView().texture()).glId();
	}

	/** Returns the actual OpenGL name of the preceding native depth texture. */
	public int previousLayerDepthTexture()
	{
		return ((GlTexture)this.oit.previousLayerDepthView().texture()).glId();
	}

	/** Marks the layer as containing geometry for composition. */
	public void markContent(int layer)
	{
		this.oit.markContent(layer);
	}

	/** Composites into the explicit native color attachment. */
	public void composite(RenderTarget destination)
	{
		this.oit.composite(Objects.requireNonNull(destination.getColorTextureView()));
	}

	/** Composites against the five native Fabulous input targets. */
	public void compositeFabulous(RenderTarget destination, List<RenderTarget> targets)
	{
		this.oit.compositeFabulous(Objects.requireNonNull(destination.getColorTextureView()), targets);
	}

	/** Releases the native per-target textures, views and framebuffer. */
	public void close()
	{
		this.oit.close();
	}
}
