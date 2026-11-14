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
			if (pipeline.getVertexShader().getPath().equals("core/triangles") || pipeline.getVertexShader().getPath().equals("core/triangles_instant"))
			{
				if (!Integer.toString(PRhiInstanceLayout.PORTABLE_CAPACITY).equals(pipeline.getShaderDefines().values().get("PULSELIB_INSTANCE_CAPACITY")) ||
						BindGroupLayout.flattenUniforms(pipeline.getBindGroupLayouts()).stream().
								noneMatch(uniform -> uniform.name().equals("InstanceData") && uniform.type() == UniformType.UNIFORM_BUFFER))
					throw new AssertionError("Canonical shader must declare the portable instance block: " + id);
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
		System.out.println("PASS production pipeline catalog: all 19 public definitions, native bind groups, vec4 texel formats and shared 24-byte geometry");
	}
}
