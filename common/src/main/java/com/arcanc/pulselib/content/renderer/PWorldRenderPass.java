/**
 * @author ArcAnc
 * Created at: 24.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer;

import com.arcanc.pulselib.content.renderer.base.PBlockRenderState;
import com.arcanc.pulselib.content.renderer.base.PEntityRenderState;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.ResourceHandle;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

/** Retains native transparency targets until PulseLib has inserted its layers after the vanilla chain. */
@ApiStatus.Internal
public final class PWorldRenderPass
{
	private PWorldRenderPass()
	{
	}

	/**
	 * Native color/depth layers must survive until OIT insertion, even with Improved Transparency disabled.
	 * A conservative visible-owner gate also covers material overrides chosen later during submission.
	 */
	public static boolean requiresSeparateTargets(LevelRenderState state)
	{
		if (state.cameraRenderState.isPanoramicMode)
			return false;
		for (var entity : state.entityRenderStates)
			if (entity instanceof PEntityRenderState<?>)
				return true;
		for (var block : state.blockEntityRenderStates)
			if (block instanceof PBlockRenderState<?>)
				return true;
		return false;
	}

	/** Called after the native transparency chain has declared its passes, before late debug rendering. */
	public static void addToFrame(FrameGraphBuilder frame, LevelTargetBundle targets)
	{
		var pass = frame.addPass("pulselib_transparency");
		targets.main = pass.readsAndWrites(targets.main);
		ResourceHandle<RenderTarget> destination = targets.main;
		List<ResourceHandle<RenderTarget>> inputs = targets.translucent != null && targets.itemEntity != null &&
				targets.particles != null && targets.weather != null && targets.clouds != null ?
				List.of(targets.translucent, targets.itemEntity, targets.particles, targets.weather, targets.clouds) : List.of();
		for (ResourceHandle<RenderTarget> input : inputs)
			pass.reads(input);
		pass.disableCulling();
		pass.executes(() ->
		{
			try
			{
				if (inputs.isEmpty())
					PRenderStagesHandler.afterWeather(destination.get());
				else
					PRenderQueue.compositeFabulousTranslucency(destination.get(), inputs.stream().map(ResourceHandle::get).toList());
			}
			finally
			{
				PRenderQueue.endWorldFrame();
			}
		});
	}
}
