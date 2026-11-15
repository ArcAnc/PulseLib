/**
 * @author ArcAnc
 * Created at: 13.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.rhi;

import com.arcanc.pulselib.util.PRenderTypes;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;

import java.util.HashMap;
import java.util.function.Consumer;

/** Full production catalog initialization and registration, including the public transparency/Fabulous families. */
public final class RhiCatalogChecks
{
	public static void register(Consumer<RenderPipeline> registrar)
	{
		var catalog = new HashMap<String, RenderPipeline>();
		PRenderTypes.register(pipeline ->
		{
			String id = pipeline.getLocation().toString();
			if (catalog.putIfAbsent(id, pipeline) != null)
				throw new AssertionError("Duplicate production pipeline " + id);
			BindGroupLayout.ensureCompatible(pipeline.getBindGroupLayouts());
			if (pipeline.getPrimitiveTopology() != PrimitiveTopology.TRIANGLES)
				throw new AssertionError("Production triangle topology lost: " + id);
			for (var uniform : BindGroupLayout.flattenUniforms(pipeline.getBindGroupLayouts()))
				if (uniform.type() == UniformType.TEXEL_BUFFER && uniform.gpuFormat() != GpuFormat.RGBA32_FLOAT)
					throw new AssertionError("Packed vec4 deformer format differs from shader data: " + id);
			if (pipeline.getVertexShader().getPath().equals("core/triangles"))
			{
				if (!Integer.toString(PRhiInstanceLayout.PORTABLE_CAPACITY).equals(pipeline.getShaderDefines().values().get("PULSELIB_INSTANCE_CAPACITY")) ||
						BindGroupLayout.flattenUniforms(pipeline.getBindGroupLayouts()).stream().
								noneMatch(uniform -> uniform.name().equals("InstanceData") && uniform.type() == UniformType.UNIFORM_BUFFER))
					throw new AssertionError("Canonical shader must declare the portable instance block: " + id);
			}
			if (pipeline.getVertexShader().getPath().equals("core/triangles_instant"))
			{
				var uniforms = BindGroupLayout.flattenUniforms(pipeline.getBindGroupLayouts());
				if (pipeline.getShaderDefines().values().containsKey("PULSELIB_INSTANCE_CAPACITY") ||
						uniforms.stream().anyMatch(uniform -> uniform.name().equals("InstanceData")) ||
						uniforms.stream().noneMatch(uniform -> uniform.name().equals("ColorOverlay")))
					throw new AssertionError("Instant shaders require single-object ColorOverlay only: " + id);
			}

			registrar.accept(pipeline);
		});
		if (catalog.size() != 19)
			throw new AssertionError("Expected all 19 production pipeline definitions, received " + catalog.size());
		try
		{
			int fields = 0;
			for (var field : PRenderTypes.RenderPipelinesProvider.class.getFields())
				if (field.getType() == RenderPipeline.class)
				{
					RenderPipeline pipeline = (RenderPipeline)field.get(null);
					if (catalog.get(pipeline.getLocation().toString()) != pipeline)
						throw new AssertionError("Public pipeline not registered: " + field.getName());
					fields++;
				}
			if (fields != catalog.size())
				throw new AssertionError("Public catalog and registered definitions differ");
		}
		catch (IllegalAccessException exception)
		{
			throw new AssertionError("Cannot inspect public pipeline catalog", exception);
		}
		if (PRenderTypes.VertexFormatProvider.POSITION_TEX_NORMAL != PRhiPipelines.POSITION_TEX_NORMAL ||
				PRenderTypes.VertexFormatProvider.POSITION_TEX_NORMAL.getVertexSize() != 24)
			throw new AssertionError("Production and RHI geometry layouts must share the canonical 24-byte format");
		for (boolean emissive : new boolean[]{false, true})
		{
			var accumulation = PRenderTypes.ShadersProvider.trianglesOit(emissive);
			if (accumulation.getColorTargetStates().length != 2 || accumulation.getColorTargetState().format() != GpuFormat.RGBA16_FLOAT ||
					accumulation.getColorTargetStates()[1].format() != GpuFormat.R16_FLOAT || accumulation.getDepthStencilState().writeDepth())
				throw new AssertionError("OIT requires two floating-point targets and read-only opaque depth");
		}
		for (var depth : new RenderPipeline[]{PRenderTypes.ShadersProvider.trianglesOitDepth(false), PRenderTypes.ShadersProvider.trianglesOitDepthPeel(false)})
			if (!depth.getDepthStencilState().writeDepth() || depth.getColorTargetState().format() != GpuFormat.R8_UNORM ||
					!BindGroupLayout.flattenSamplers(depth.getBindGroupLayouts()).contains("SceneDepthSampler"))
				throw new AssertionError("Native peeling requires D32 writes, scratch R8 and opaque depth sampling");
		if (PRenderTypes.ShadersProvider.oitComposite().getDepthStencilState() != null ||
				PRenderTypes.ShadersProvider.oitFabulousComposite().getDepthStencilState() != null ||
				PRenderTypes.ShadersProvider.oitFabulousMetadata().getColorTargetState().format() != GpuFormat.RG32_FLOAT)
			throw new AssertionError("OIT composites must be depthless; Fabulous metadata must preserve float depth");
		System.out.println("PASS native OIT MRT formats, peeling/depth states and floating-point Fabulous metadata");
		System.out.println("PASS production pipeline catalog: all 19 public definitions, native bind groups, vec4 texel formats and shared 24-byte geometry");
	}
}
