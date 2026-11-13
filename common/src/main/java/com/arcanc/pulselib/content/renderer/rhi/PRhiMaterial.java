/**
 * @author ArcAnc
 * Created at: 13.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.rhi;

import com.arcanc.pulselib.content.renderer.plan.PTransparencyMode;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** Explicit pass resources. Textures, targets and extra uniforms are borrowed, never closed by the executor. */
@ApiStatus.Internal
public record PRhiMaterial(RenderPipeline pipeline, Supplier<Attachments> target, int instanceCapacity,
		Map<String, Supplier<GpuBufferSlice>> uniforms, Map<String, Supplier<Texture>> textures,
		PTransparencyMode transparency)
{
	public PRhiMaterial
	{
		Objects.requireNonNull(pipeline);
		Objects.requireNonNull(target);
		Objects.requireNonNull(transparency);
		if (instanceCapacity < 1 || instanceCapacity > 512)
			throw new IllegalArgumentException("RHI shader instance capacity must be between 1 and 512");
		uniforms = Map.copyOf(uniforms);
		textures = Map.copyOf(textures);
		Set<String> owned = Set.of("Projection", "DynamicTransforms", "InstanceData");
		var declarations = BindGroupLayout.flattenUniforms(pipeline.getBindGroupLayouts());
		Set<String> declared = declarations.stream().map(BindGroupLayout.UniformDescription::name).collect(Collectors.toSet());
		Set<String> samplers = Set.copyOf(BindGroupLayout.flattenSamplers(pipeline.getBindGroupLayouts()));
		for (String reserved : owned)
		{
			if (uniforms.containsKey(reserved))
				throw new IllegalArgumentException("RHI owns uniform " + reserved);
			if (declarations.stream().noneMatch(uniform -> uniform.name().equals(reserved) && uniform.type() == UniformType.UNIFORM_BUFFER))
				throw new IllegalArgumentException("RHI pipeline must declare uniform buffer " + reserved);
		}
		for (String name : declared)
			if (!owned.contains(name) && !uniforms.containsKey(name))
				throw new IllegalArgumentException("Missing RHI uniform binding " + name);
		if (!declared.containsAll(uniforms.keySet()) || !samplers.equals(textures.keySet()))
			throw new IllegalArgumentException("RHI material bindings do not match pipeline declarations");
	}

	public PRhiMaterial(RenderPipeline pipeline, Supplier<Attachments> target, int instanceCapacity)
	{
		this(pipeline, target, instanceCapacity, Map.of(), Map.of(), PTransparencyMode.ORDINARY);
	}

	public record Attachments(GpuTextureView color, @Nullable GpuTextureView depth)
	{
		public Attachments
		{
			Objects.requireNonNull(color);
		}
	}

	public record Texture(GpuTextureView view, GpuSampler sampler)
	{
		public Texture
		{
			Objects.requireNonNull(view);
			Objects.requireNonNull(sampler);
		}
	}
}
