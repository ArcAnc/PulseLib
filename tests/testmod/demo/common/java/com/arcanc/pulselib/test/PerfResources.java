/**
 * @author ArcAnc
 * Created at: 10.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test;

import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.mojang.blaze3d.opengl.GlTexture;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Native owner/resource adapters. Called outside measured GPU intervals. */
public final class PerfResources
{
	private PerfResources()
	{
	}

	public static @Nullable Object field(Object owner, String name)
	{
		return RegressionChecks.field(owner.getClass(), name, owner);
	}

	static List<Integer> ids()
	{
		var snapshot = RegressionResources.all();
		List<Integer> ids = new ArrayList<>();
		ids.addAll(snapshot.buffers.stream().sorted().toList());
		ids.addAll(snapshot.textures.stream().sorted().toList());
		ids.addAll(snapshot.framebuffers.stream().sorted().toList());
		Object executor = RegressionResources.executor();
		for (String stream : List.of("instances", "indirect"))
			ids.add((int)field(field(executor, stream), "capacity"));
		return List.copyOf(ids);
	}

	static long bytes(boolean metadata)
	{
		long bytes = 0;
		Object executor = RegressionResources.executor();
		for (Object oit : ((Map<?, ?>)field(executor, "weightedBlendedOits")).values())
			if (metadata)
				bytes += bytes((GlTexture)field(oit, "fabulousMetadataTexture"));
			else
				for (String name : List.of("accumulationTextures", "revealageTextures", "layerDepthTextures"))
					for (Object texture : (Object[])field(oit, name))
						bytes += bytes((GlTexture)texture);
		return bytes;
	}

	private static long bytes(@Nullable GlTexture texture)
	{
		return texture == null ? 0 : OitPerformanceHarness.textureBytes(texture.glId());
	}
}
