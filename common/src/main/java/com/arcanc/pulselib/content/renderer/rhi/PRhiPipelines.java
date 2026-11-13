/**
 * @author ArcAnc
 * Created at: 13.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.rhi;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Consumer;

/**
 * Basic depth-writing RHI profiles for immutable or CPU-deformed geometry. No GPU deformer streams
 * are declared, so PRhiDrawExecutor rejects nonzero operation counts rather than dropping deformation.
 * These profiles do not replace the canonical transparent, immediate or OIT pipeline registrations.
 */
@ApiStatus.Internal
public final class PRhiPipelines
{
	public static final VertexFormat POSITION_TEX_NORMAL = VertexFormat.builder(0).
			addAttribute("Position", GpuFormat.RGB32_FLOAT).
			addAttribute("UV0", GpuFormat.RG32_FLOAT).
			addAttribute("Normal", GpuFormat.RGBA8_SNORM).
			build();

	private final int instanceCapacity;
	private final RenderPipeline[] pipelines = new RenderPipeline[4];

	public PRhiPipelines(int instanceCapacity)
	{
		if (instanceCapacity < 1 || instanceCapacity > 512)
			throw new IllegalArgumentException("RHI instance capacity must be between 1 and 512");
		this.instanceCapacity = instanceCapacity;
		for (int emissive = 0; emissive < 2; emissive++)
			for (int cutout = 0; cutout < 2; cutout++)
				this.pipelines[emissive * 2 + cutout] = create(cutout != 0, emissive != 0);
	}

	public int instanceCapacity()
	{
		return this.instanceCapacity;
	}

	public RenderPipeline pipeline(boolean cutout, boolean emissive)
	{
		return this.pipelines[(emissive ? 2 : 0) + (cutout ? 1 : 0)];
	}

	/** Register with the loader's native pipeline event before resource preparation. */
	public void register(Consumer<RenderPipeline> registrar)
	{
		for (RenderPipeline pipeline : this.pipelines)
			registrar.accept(pipeline);
	}

	private RenderPipeline create(boolean cutout, boolean emissive)
	{
		var layout = BindGroupLayout.builder().withUniform("InstanceData", UniformType.UNIFORM_BUFFER).
				withSampler("Sampler0").withSampler("Sampler1");
		if (!emissive)
			layout.withSampler("Sampler2").withUniform("Lighting", UniformType.UNIFORM_BUFFER);
		String variant = (emissive ? "emissive_" : "lit_") + (cutout ? "cutout" : "solid");
		var builder = RenderPipeline.builder().
				withLocation(id("pipeline/rhi/" + variant + "_instances_" + this.instanceCapacity)).
				withVertexShader(id("core/rhi/triangles")).
				withFragmentShader(id("core/triangles")).
				withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION).
				withBindGroupLayout(layout.build()).
				withVertexBinding(0, POSITION_TEX_NORMAL).
				withPrimitiveTopology(PrimitiveTopology.TRIANGLES).
				withDepthStencilState(DepthStencilState.DEFAULT).
				withCull(false).
				withShaderDefine("PULSELIB_INSTANCE_CAPACITY", this.instanceCapacity);
		if (cutout)
			builder.withShaderDefine("ALPHA_CUTOUT", 0.1F);
		else
			builder.withShaderDefine("FORCE_OPAQUE");
		if (emissive)
			builder.withShaderDefine("EMISSIVE");
		return builder.build();
	}

	private static Identifier id(String path)
	{
		return Identifier.fromNamespaceAndPath("pulselib", path);
	}
}
