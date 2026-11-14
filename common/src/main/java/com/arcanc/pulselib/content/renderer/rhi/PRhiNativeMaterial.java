/**
 * @author ArcAnc
 * Created at: 13.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.rhi;

import com.arcanc.pulselib.content.renderer.plan.PPipelineHandle;
import com.arcanc.pulselib.content.renderer.plan.PTransparencyMode;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Native texture, target, scissor and default-uniform preparation for PulseLib's frame-driven shaders.
 * Register once per type/policy, then prepare once per plan execution, never once per instance chunk.
 * Model-view/projection remain owned by PRenderFrame. Native dynamicTransforms are not substituted;
 * this adapter is for PulseLib types without vanilla layering or animated texture-transform semantics.
 * Every GPU resource is borrowed from the host and must remain live through submission.
 */
@ApiStatus.Internal
public final class PRhiNativeMaterial
{
	private static final Set<String> OWNED_UNIFORMS = Set.of("DynamicTransforms", "Projection", "InstanceData", "ColorOverlay");
	private final RenderType type;
	private final Supplier<PreparedRenderType> prepare;
	private final Function<PreparedRenderType, PRhiMaterial.Attachments> target;
	private final PRhiMaterial material;
	private final Supplier<PRhiPassState> state = this::prepareState;

	/** Uses Minecraft's real preparation and attachment overrides at draw time. Extra streams are explicit. */
	public PRhiNativeMaterial(RenderType type, int capacity, PTransparencyMode transparency,
			Map<String, Supplier<GpuBufferSlice>> extraUniforms)
	{
		this(type, capacity, transparency, extraUniforms, type::prepare, PRhiNativeMaterial::attachments);
	}

	/** Allows an explicit native prepared context and output resolver for offscreen/local rendering. */
	public PRhiNativeMaterial(RenderType type, int capacity, PTransparencyMode transparency,
			Map<String, Supplier<GpuBufferSlice>> extraUniforms, Supplier<PreparedRenderType> prepare,
			Function<PreparedRenderType, PRhiMaterial.Attachments> target)
	{
		this.type = Objects.requireNonNull(type);
		this.prepare = Objects.requireNonNull(prepare);
		this.target = Objects.requireNonNull(target);
		Map<String, Supplier<GpuBufferSlice>> uniforms = new HashMap<>(extraUniforms);
		for (var uniform : BindGroupLayout.flattenUniforms(type.pipeline().getBindGroupLayouts()))
			if (!OWNED_UNIFORMS.contains(uniform.name()) && !uniforms.containsKey(uniform.name()))
				uniforms.put(uniform.name(), defaultUniform(uniform.name()));
		Map<String, Supplier<PRhiMaterial.Texture>> textures = new HashMap<>();
		for (String name : BindGroupLayout.flattenSamplers(type.pipeline().getBindGroupLayouts()))
			textures.put(name, () -> texture(prepared(), name));
		this.material = new PRhiMaterial(type.pipeline(), () -> target.apply(prepared()), capacity, uniforms, textures, transparency);
	}

	public PPipelineHandle register(PRhiResourceRegistry resources)
	{
		return resources.pipeline(this.type, this.material, this.state);
	}

	private PreparedRenderType prepared()
	{
		PreparedRenderType prepared = Objects.requireNonNull(this.prepare.get(), "Native material preparation returned null");
		if (prepared.pipeline() != this.type.pipeline())
			throw new IllegalArgumentException("Prepared native pipeline differs from the registered render type");
		return prepared;
	}

	private PRhiPassState prepareState()
	{
		PreparedRenderType prepared = prepared();
		var scissor = PRhiPassState.Scissor.copyOf(prepared.scissorState());
		Map<String, PRhiMaterial.Texture> textures = new HashMap<>();
		for (var texture : prepared.textures())
			if (textures.putIfAbsent(texture.name(), new PRhiMaterial.Texture(texture.textureView(), texture.sampler())) != null)
				throw new IllegalArgumentException("Duplicate prepared native texture " + texture.name());
		Map<String, PRhiMaterial.Texture> required = new HashMap<>();
		for (String name : this.material.textures().keySet())
			required.put(name, Objects.requireNonNull(textures.get(name), "Missing prepared native texture " + name));
		Map<String, GpuBufferSlice> uniforms = new HashMap<>();
		this.material.uniforms().forEach((name, value) -> uniforms.put(name, value.get()));
		return new PRhiPassState(this.target.apply(prepared), uniforms, required, scissor);
	}

	private static PRhiMaterial.Texture texture(PreparedRenderType prepared, String name)
	{
		for (var texture : prepared.textures())
			if (texture.name().equals(name))
				return new PRhiMaterial.Texture(texture.textureView(), texture.sampler());
		throw new IllegalStateException("Missing prepared native texture " + name);
	}

	private static Supplier<GpuBufferSlice> defaultUniform(String name)
	{
		return switch (name)
		{
			case "Fog" -> () -> required(RenderSystem.getShaderFog(), name);
			case "Lighting" -> () -> required(RenderSystem.getShaderLights(), name);
			case "Globals" -> () -> required(RenderSystem.getGlobalSettingsUniform(), name).slice();
			default -> throw new IllegalArgumentException("Native material requires an explicit uniform supplier for " + name);
		};
	}

	private static <T> T required(@Nullable T value, String name)
	{
		return Objects.requireNonNull(value, "Missing native uniform " + name);
	}

	static PRhiMaterial.Attachments attachments(PreparedRenderType prepared)
	{
		var target = prepared.outputTarget().getRenderTarget();
		var color = RenderSystem.outputColorTextureOverride != null ? RenderSystem.outputColorTextureOverride : target.getColorTextureView();
		var depth = RenderSystem.outputColorTextureOverride != null ? RenderSystem.outputDepthTextureOverride :
				(target.useDepth ? (RenderSystem.outputDepthTextureOverride != null ? RenderSystem.outputDepthTextureOverride : target.getDepthTextureView()) : null);
		return new PRhiMaterial.Attachments(Objects.requireNonNull(color, "Native render target has no color view"), depth);
	}
}
