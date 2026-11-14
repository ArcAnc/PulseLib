/**
 * @author ArcAnc
 * Created at: 14.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.rhi;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.WeakHashMap;
import java.util.Map;
import java.util.Optional;

/** Native pipelines must agree with target depth availability on every backend. */
final class PRhiTargetPipelines
{
	// Pure descriptors are shared across executor cleanup; Minecraft owns their compiled GPU cache.
	private static final Map<RenderPipeline, RenderPipeline> WITHOUT_DEPTH = Collections.synchronizedMap(new WeakHashMap<>());

	private PRhiTargetPipelines()
	{
	}

	static RenderPipeline select(RenderPipeline source, boolean hasDepth)
	{
		if (hasDepth || source.getDepthStencilState() == null)
			return source;
		return WITHOUT_DEPTH.computeIfAbsent(source, pipeline ->
		{
			var snippet = new RenderPipeline.Snippet(Optional.of(pipeline.getVertexShader()), Optional.of(pipeline.getFragmentShader()),
					Optional.of(pipeline.getShaderDefines()), Optional.of(pipeline.getBindGroupLayouts()), pipeline.getColorTargetStates(),
					pipeline.getColorTargetStates().length, Optional.empty(), Optional.of(pipeline.getPolygonMode()), Optional.of(pipeline.isCull()),
					pipeline.getVertexFormatBindings(), Optional.of(pipeline.getPrimitiveTopology()));
			var id = pipeline.getLocation();
			return RenderPipeline.builder(snippet).
					withLocation(Identifier.fromNamespaceAndPath(id.getNamespace(), id.getPath() + "_local_no_depth")).
					withDepthStencilState(Optional.empty()).
					build();
		});
	}

}
