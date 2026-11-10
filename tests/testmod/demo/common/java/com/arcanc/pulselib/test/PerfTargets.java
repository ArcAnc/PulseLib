/**
 * @author ArcAnc
 * Created at: 10.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test;

import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/** Persistent real MC26 postchain inputs and pooled internal targets; no pixel readback. */
final class PerfTargets implements AutoCloseable
{
	final List<RenderTarget> targets = new ArrayList<>();
	private final CrossFrameResourcePool pool = new CrossFrameResourcePool(2);
	int passes;
	private final int width;
	private final int height;

	PerfTargets(int width, int height)
	{
		this.width = width;
		this.height = height;
		for (int i = 0; i < 5; i++)
			this.targets.add(new TextureTarget("GPU profile input " + i, width, height, true));
	}

	static void clear(RenderTarget target, int color)
	{
		RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(target.getColorTexture(), color, target.getDepthTexture(), 1);
	}

	void clear()
	{
		for (RenderTarget target : this.targets)
			clear(target, 0);
	}

	void process()
	{
		FrameGraphBuilder frame = new FrameGraphBuilder();
		LevelTargetBundle bundle = new LevelTargetBundle();
		bundle.main = frame.importExternal("main", OitPerformanceHarness.main());
		bundle.translucent = frame.importExternal("translucent", this.targets.get(0));
		bundle.itemEntity = frame.importExternal("item_entity", this.targets.get(1));
		bundle.particles = frame.importExternal("particles", this.targets.get(2));
		bundle.weather = frame.importExternal("weather", this.targets.get(3));
		bundle.clouds = frame.importExternal("clouds", this.targets.get(4));
		var chain = Minecraft.getInstance().getShaderManager().getPostChain(Identifier.withDefaultNamespace("transparency"), LevelTargetBundle.SORTING_TARGETS);
		RegressionChecks.check(chain != null, "Native profile postchain must load");
		chain.addToFrame(frame, this.width, this.height, bundle);
		this.passes = 0;
		frame.execute(this.pool, new FrameGraphBuilder.Inspector()
		{
			@Override
			public void beforeExecutePass(String name)
			{
				PerfTargets.this.passes++;
			}
		});
		this.pool.endFrame();
	}

	long bytes()
	{
		long bytes = 0;
		for (RenderTarget target : this.targets)
			bytes += OitPerformanceHarness.textureBytes(((com.mojang.blaze3d.opengl.GlTexture)target.getColorTexture()).glId()) +
					OitPerformanceHarness.textureBytes(((com.mojang.blaze3d.opengl.GlTexture)target.getDepthTexture()).glId());
		// Include the actual pooled internal targets, not an assumed legacy target count.
		for (Object entry : (java.util.Collection<?>)RegressionChecks.field(CrossFrameResourcePool.class, "pool", this.pool))
		{
			Object resource = PerfResources.field(entry, "value");
			if (resource instanceof RenderTarget target)
				bytes += OitPerformanceHarness.textureBytes(((com.mojang.blaze3d.opengl.GlTexture)target.getColorTexture()).glId()) +
						(target.useDepth ? OitPerformanceHarness.textureBytes(((com.mojang.blaze3d.opengl.GlTexture)target.getDepthTexture()).glId()) : 0);
		}
		return bytes;
	}

	@Override
	public void close()
	{
		this.pool.close();
		this.targets.forEach(RenderTarget::destroyBuffers);
	}
}
