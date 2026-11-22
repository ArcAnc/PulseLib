/**
 * @author ArcAnc
 * Created at: 27.01.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.util;


import com.arcanc.pulselib.content.model.textures.PAlphaMode;
import com.arcanc.pulselib.content.renderer.rhi.PRhiInstanceLayout;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.ApiStatus;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Provides support for render types.
 */
public final class PRenderTypes
{
	private PRenderTypes()
	{
	}

	/**
	* Provides render pipelines.
    */
	public static class RenderPipelinesProvider
	{
		private static final Set<RenderPipeline> PIPELINES = new HashSet<>();

		private static final RenderPipeline.Snippet TRIANGLES_SNIPPET = RenderPipeline.builder().
				withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION).
				withVertexShader(PLibDatabase.rl("core/triangles")).
				withShaderDefine("PULSELIB_INSTANCE_CAPACITY", PRhiInstanceLayout.PORTABLE_CAPACITY).
				withFragmentShader(PLibDatabase.rl("core/triangles")).
				withBindGroupLayout(BindGroupLayout.builder().
					withSampler("Sampler0").
					withUniform("InstanceData", UniformType.UNIFORM_BUFFER).
					withUniform("DeformerOperations", UniformType.TEXEL_BUFFER, GpuFormat.RGBA32_FLOAT).
					withUniform("DeformerValues", UniformType.TEXEL_BUFFER, GpuFormat.RGBA32_FLOAT).
					build()).
				withVertexBinding(0, VertexFormatProvider.POSITION_TEX_NORMAL).
				withPrimitiveTopology(PrimitiveTopology.TRIANGLES).
				withDepthStencilState(DepthStencilState.DEFAULT).
				buildSnippet();

		private static final RenderPipeline.Snippet TRIANGLES_LIT_SNIPPET = RenderPipeline.builder(TRIANGLES_SNIPPET).
				withBindGroupLayout(BindGroupLayout.builder().
					withSampler("Sampler2").
					withUniform("Lighting", UniformType.UNIFORM_BUFFER).
					build()).
				buildSnippet();

		public static final RenderPipeline TRIANGLES_SOLID = registerPipeline(RenderPipeline.builder(TRIANGLES_LIT_SNIPPET).
				withLocation(PLibDatabase.rl("pipeline/triangles_solid_no_cull")).
				withBindGroupLayout(BindGroupLayout.builder().
					withSampler("Sampler1").
					build()).
				withShaderDefine("FORCE_OPAQUE").
				withCull(false).
				build());

		public static final RenderPipeline TRIANGLES_CUTOUT = registerPipeline(RenderPipeline.builder(TRIANGLES_LIT_SNIPPET).
				withLocation(PLibDatabase.rl("pipeline/triangles_cutout_no_cull")).
				withBindGroupLayout(BindGroupLayout.builder().
					withSampler("Sampler1").
					build()).
				withShaderDefine("ALPHA_CUTOUT", 0.1F).
				withCull(false).
				build());

		public static final RenderPipeline TRIANGLES_TRANSLUCENT = registerPipeline(RenderPipeline.builder(TRIANGLES_LIT_SNIPPET).
				withLocation(PLibDatabase.rl("pipeline/triangles_translucent_no_cull")).
				withBindGroupLayout(BindGroupLayout.builder().
					withSampler("Sampler1").
					build()).
				withShaderDefine("ALPHA_CUTOUT", 0.1F).
				withCull(false).
				withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT)).
				withDepthStencilState(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), false)).
				build());

		public static final RenderPipeline TRIANGLES_EMISSIVE_SOLID = registerPipeline(RenderPipeline.builder(TRIANGLES_SNIPPET).
				withLocation(PLibDatabase.rl("pipeline/triangles_emissive_solid_no_cull")).
				withBindGroupLayout(BindGroupLayout.builder().
					withSampler("Sampler1").
					build()).
				withShaderDefine("EMISSIVE").
				withShaderDefine("FORCE_OPAQUE").
				withCull(false).
				build());

		public static final RenderPipeline TRIANGLES_EMISSIVE_CUTOUT = registerPipeline(RenderPipeline.builder(TRIANGLES_SNIPPET).
				withLocation(PLibDatabase.rl("pipeline/triangles_emissive_cutout_no_cull")).
				withBindGroupLayout(BindGroupLayout.builder().
					withSampler("Sampler1").
					build()).
				withShaderDefine("EMISSIVE").
				withShaderDefine("ALPHA_CUTOUT", 0.1F).
				withCull(false).
				build());

		public static final RenderPipeline TRIANGLES_EMISSIVE_TRANSLUCENT = registerPipeline(RenderPipeline.builder(TRIANGLES_SNIPPET).
				withLocation(PLibDatabase.rl("pipeline/triangles_emissive_translucent_no_cull")).
				withBindGroupLayout(BindGroupLayout.builder().
					withSampler("Sampler1").
					build()).
				withShaderDefine("EMISSIVE").
				withShaderDefine("ALPHA_CUTOUT", 0.1F).
				withCull(false).
				withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT)).
				withDepthStencilState(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), false)).
				build());

		public static final RenderPipeline TRIANGLES_OIT = registerPipeline(RenderPipeline.builder(TRIANGLES_LIT_SNIPPET).
				withLocation(PLibDatabase.rl("pipeline/triangles_oit_no_cull")).
				withFragmentShader(PLibDatabase.rl("core/triangles_oit")).
				withBindGroupLayout(BindGroupLayout.builder().
					withSampler("Sampler1").
					withSampler("LayerDepthSampler").
					build()).
				withShaderDefine("ALPHA_CUTOUT", 0.1F).
				withCull(false).
				withColorTargetState(new ColorTargetState(Optional.of(new BlendFunction(BlendFactor.ONE, BlendFactor.ONE)), GpuFormat.RGBA16_FLOAT, ColorTargetState.WRITE_ALL)).
				withColorTargetState(1, new ColorTargetState(Optional.of(new BlendFunction(BlendFactor.ONE, BlendFactor.ONE)), GpuFormat.R16_FLOAT, ColorTargetState.WRITE_RED)).
				withDepthStencilState(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), false)).
				build());

		public static final RenderPipeline TRIANGLES_OIT_EMISSIVE = registerPipeline(RenderPipeline.builder(TRIANGLES_SNIPPET).
				withLocation(PLibDatabase.rl("pipeline/triangles_oit_emissive_no_cull")).
				withFragmentShader(PLibDatabase.rl("core/triangles_oit")).
				withBindGroupLayout(BindGroupLayout.builder().
					withSampler("Sampler1").
					withSampler("LayerDepthSampler").
					build()).
				withShaderDefine("EMISSIVE").
				withShaderDefine("ALPHA_CUTOUT", 0.1F).
				withCull(false).
				withColorTargetState(new ColorTargetState(Optional.of(new BlendFunction(BlendFactor.ONE, BlendFactor.ONE)), GpuFormat.RGBA16_FLOAT, ColorTargetState.WRITE_ALL)).
				withColorTargetState(1, new ColorTargetState(Optional.of(new BlendFunction(BlendFactor.ONE, BlendFactor.ONE)), GpuFormat.R16_FLOAT, ColorTargetState.WRITE_RED)).
				withDepthStencilState(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), false)).
				build());

		public static final RenderPipeline TRIANGLES_OIT_DEPTH = registerPipeline(RenderPipeline.builder(TRIANGLES_LIT_SNIPPET).
				withLocation(PLibDatabase.rl("pipeline/triangles_oit_depth_no_cull")).
				withFragmentShader(PLibDatabase.rl("core/triangles_oit_depth")).
				withBindGroupLayout(BindGroupLayout.builder().
					withSampler("Sampler1").
					withSampler("SceneDepthSampler").
					build()).
				withShaderDefine("ALPHA_CUTOUT", 0.1F).
				withCull(false).
				withColorTargetState(new ColorTargetState(Optional.empty(), GpuFormat.R8_UNORM, ColorTargetState.WRITE_RED)).
				build());

		public static final RenderPipeline TRIANGLES_OIT_DEPTH_PEEL = registerPipeline(RenderPipeline.builder(TRIANGLES_LIT_SNIPPET).
				withLocation(PLibDatabase.rl("pipeline/triangles_oit_depth_peel_no_cull")).
				withFragmentShader(PLibDatabase.rl("core/triangles_oit_depth_peel")).
				withBindGroupLayout(BindGroupLayout.builder().
					withSampler("Sampler1").
					withSampler("SceneDepthSampler").
					withSampler("LayerDepthSampler").
					build()).
				withShaderDefine("ALPHA_CUTOUT", 0.1F).
				withCull(false).
				withColorTargetState(new ColorTargetState(Optional.empty(), GpuFormat.R8_UNORM, ColorTargetState.WRITE_RED)).
				build());

		public static final RenderPipeline OIT_COMPOSITE = registerPipeline(RenderPipeline.builder().
				withLocation(PLibDatabase.rl("pipeline/oit_composite")).
				withVertexShader(PLibDatabase.rl("core/oit_composite")).
				withFragmentShader(PLibDatabase.rl("core/oit_composite")).
				withBindGroupLayout(BindGroupLayout.builder().
					withSampler("AccumSampler").
					withSampler("RevealSampler").
					withSampler("DepthSampler").
					build()).
				withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT)).
				withCull(false).
				withPrimitiveTopology(PrimitiveTopology.TRIANGLES).
				withDepthStencilState(Optional.empty()).
				build());

		public static final RenderPipeline OIT_FABULOUS_METADATA = registerPipeline(RenderPipeline.builder().
				withLocation(PLibDatabase.rl("pipeline/oit_fabulous_metadata")).
				withVertexShader(PLibDatabase.rl("core/oit_composite")).
				withFragmentShader(PLibDatabase.rl("core/oit_fabulous_metadata")).
				withBindGroupLayout(BindGroupLayout.builder().
					withSampler("RevealSampler").withSampler("DepthSampler").
					build()).
				withCull(false).
				withPrimitiveTopology(PrimitiveTopology.TRIANGLES).
				withDepthStencilState(Optional.empty()).
				withColorTargetState(new ColorTargetState(Optional.empty(), GpuFormat.RG32_FLOAT, ColorTargetState.WRITE_ALL)).
				build());

		public static final RenderPipeline OIT_FABULOUS_COMPOSITE = registerPipeline(RenderPipeline.builder().
				withLocation(PLibDatabase.rl("pipeline/oit_fabulous_composite")).
				withVertexShader(PLibDatabase.rl("core/oit_composite")).
				withFragmentShader(PLibDatabase.rl("core/oit_fabulous_composite")).
				withBindGroupLayout(BindGroupLayout.builder().
					withSampler("AccumSampler").
					withSampler("OitMetadataSampler").
					withSampler("TranslucentSampler").
					withSampler("TranslucentDepthSampler").
					withSampler("ItemEntitySampler").
					withSampler("ItemEntityDepthSampler").
					withSampler("ParticlesSampler").
					withSampler("ParticlesDepthSampler").
					withSampler("WeatherSampler").
					withSampler("WeatherDepthSampler").
					withSampler("CloudsSampler").
					withSampler("CloudsDepthSampler").
					build()).
				withColorTargetState(new ColorTargetState(new BlendFunction(BlendFactor.SRC_ALPHA,
						BlendFactor.ONE_MINUS_SRC_ALPHA, BlendFactor.ZERO, BlendFactor.ONE))).
				withCull(false).
				withPrimitiveTopology(PrimitiveTopology.TRIANGLES).
				withDepthStencilState(Optional.empty()).
				build());

		private static final RenderPipeline.Snippet TRIANGLES_INSTANT_SNIPPET = RenderPipeline.builder().
				withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION).
				withVertexShader(PLibDatabase.rl("core/triangles_instant")).
				withFragmentShader(PLibDatabase.rl("core/triangles_instant")).
				withBindGroupLayout(BindGroupLayout.builder().
					withSampler("Sampler0").
					withSampler("Sampler1").
					withUniform("ColorOverlay", UniformType.UNIFORM_BUFFER).
					withUniform("DeformerOperations", UniformType.TEXEL_BUFFER, GpuFormat.RGBA32_FLOAT).
					withUniform("DeformerValues", UniformType.TEXEL_BUFFER, GpuFormat.RGBA32_FLOAT).
					build()).
				withCull(false).
				withDepthStencilState(DepthStencilState.DEFAULT).
				withVertexBinding(0, VertexFormatProvider.POSITION_TEX_NORMAL).
				withPrimitiveTopology(PrimitiveTopology.TRIANGLES).
				buildSnippet();

		private static final RenderPipeline.Snippet TRIANGLES_INSTANT_LIT_SNIPPET = RenderPipeline.builder(TRIANGLES_INSTANT_SNIPPET).
				withBindGroupLayout(BindGroupLayout.builder().
					withSampler("Sampler2").
					withUniform("Lighting", UniformType.UNIFORM_BUFFER).
					build()).
				buildSnippet();

		public static final RenderPipeline TRIANGLES_INSTANT_SOLID = registerPipeline(RenderPipeline.builder(TRIANGLES_INSTANT_LIT_SNIPPET).
				withLocation(PLibDatabase.rl("pipeline/triangles_instant_solid")).
				withShaderDefine("FORCE_OPAQUE").
				build());

		public static final RenderPipeline TRIANGLES_INSTANT_CUTOUT = registerPipeline(RenderPipeline.builder(TRIANGLES_INSTANT_LIT_SNIPPET).
				withLocation(PLibDatabase.rl("pipeline/triangles_instant_cutout")).
				withShaderDefine("ALPHA_CUTOUT", 0.1F).
				build());

		public static final RenderPipeline TRIANGLES_INSTANT_TRANSLUCENT = registerPipeline(RenderPipeline.builder(TRIANGLES_INSTANT_LIT_SNIPPET).
				withLocation(PLibDatabase.rl("pipeline/triangles_instant_translucent")).
				withShaderDefine("ALPHA_CUTOUT", 0.1F).
				withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT)).
				withDepthStencilState(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), false)).
				build());

		public static final RenderPipeline TRIANGLES_INSTANT_EMISSIVE_SOLID = registerPipeline(RenderPipeline.builder(TRIANGLES_INSTANT_SNIPPET).
				withLocation(PLibDatabase.rl("pipeline/triangles_instant_emissive_solid")).
				withShaderDefine("EMISSIVE").
				withShaderDefine("FORCE_OPAQUE").
				build());

		public static final RenderPipeline TRIANGLES_INSTANT_EMISSIVE_CUTOUT = registerPipeline(RenderPipeline.builder(TRIANGLES_INSTANT_SNIPPET).
				withLocation(PLibDatabase.rl("pipeline/triangles_instant_emissive_cutout")).
				withShaderDefine("EMISSIVE").
				withShaderDefine("ALPHA_CUTOUT", 0.1F).
				build());

		public static final RenderPipeline TRIANGLES_INSTANT_EMISSIVE_TRANSLUCENT = registerPipeline(RenderPipeline.builder(TRIANGLES_INSTANT_SNIPPET).
				withLocation(PLibDatabase.rl("pipeline/triangles_instant_emissive_translucent")).
				withShaderDefine("EMISSIVE").
				withShaderDefine("ALPHA_CUTOUT", 0.1F).
				withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT)).
				withDepthStencilState(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), false)).
				build());

		/**
		 * Registers the pipeline.
		 * @param pipeline the pipeline to use.
		 * @return the value produced by this operation.
		 */
		private static RenderPipeline registerPipeline(RenderPipeline pipeline)
		{
			PIPELINES.add(pipeline);
			return pipeline;
		}

		/**
		 * Registers the custom pipelines.
		 * @param registrar the platform pipeline registry callback.
		 */
		private static void registerCustomPipelines(Consumer<RenderPipeline> registrar)
		{
			PIPELINES.forEach(registrar);
			PIPELINES.clear();
		}
	}

	/**
	* Provides vertex format.
    */
	public static class VertexFormatProvider
	{
		public static final VertexFormat POSITION_TEX_NORMAL = VertexFormat.builder(0).
			addAttribute("Position", GpuFormat.RGB32_FLOAT).
			addAttribute("UV0", GpuFormat.RG32_FLOAT).
			addAttribute("Normal", GpuFormat.RGBA8_SNORM).
			build();
	}

	/**
    * Provides render type.
    */
	public static final class RenderTypeProvider
	{
		private RenderTypeProvider()
		{
		}

		private static final Function<Identifier, RenderType> TRIANGLES_SOLID = Util.memoize(RenderTypeProvider :: createTrianglesSolid);
		private static final Function<Identifier, RenderType> TRIANGLES_CUTOUT = Util.memoize(RenderTypeProvider :: createTrianglesCutout);
		private static final Function<Identifier, RenderType> TRIANGLES_TRANSLUCENT = Util.memoize(RenderTypeProvider :: createTrianglesTranslucent);
		private static final Function<Identifier, RenderType> TRIANGLES_EMISSIVE_SOLID = Util.memoize(RenderTypeProvider :: createTrianglesEmissiveSolid);
		private static final Function<Identifier, RenderType> TRIANGLES_EMISSIVE_CUTOUT = Util.memoize(RenderTypeProvider :: createTrianglesEmissiveCutout);
		private static final Function<Identifier, RenderType> TRIANGLES_EMISSIVE_TRANSLUCENT = Util.memoize(RenderTypeProvider :: createTrianglesEmissiveTranslucent);
		private static final Function<Identifier, RenderType> TRIANGLES_INSTANT_SOLID = Util.memoize(RenderTypeProvider :: createTrianglesInstantSolid);
		private static final Function<Identifier, RenderType> TRIANGLES_INSTANT_CUTOUT = Util.memoize(RenderTypeProvider :: createTrianglesInstantCutout);
		private static final Function<Identifier, RenderType> TRIANGLES_INSTANT_TRANSLUCENT = Util.memoize(RenderTypeProvider :: createTrianglesInstantTranslucent);
		private static final Function<Identifier, RenderType> TRIANGLES_INSTANT_EMISSIVE_SOLID = Util.memoize(RenderTypeProvider :: createTrianglesInstantEmissiveSolid);
		private static final Function<Identifier, RenderType> TRIANGLES_INSTANT_EMISSIVE_CUTOUT = Util.memoize(RenderTypeProvider :: createTrianglesInstantEmissiveCutout);
		private static final Function<Identifier, RenderType> TRIANGLES_INSTANT_EMISSIVE_TRANSLUCENT = Util.memoize(RenderTypeProvider :: createTrianglesInstantEmissiveTranslucent);

		/**
		 * Creates the triangles solid.
		 * @param texture the texture to use.
		 * @return the value produced by this operation.
		 */
		private static RenderType createTrianglesSolid(Identifier texture)
		{
			RenderSetup setup = RenderSetup.builder(RenderPipelinesProvider.TRIANGLES_SOLID).
					withTexture("Sampler0", texture).
					useLightmap().
					useOverlay().
					affectsCrumbling().
					setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE).
					createRenderSetup();

			return RenderType.create(PLibDatabase.rl("triangles_solid").toString(), setup);
		}

		/**
		 * Creates the triangles cutout.
		 * @param texture the texture to use.
		 * @return the value produced by this operation.
		 */
		private static RenderType createTrianglesCutout(Identifier texture)
		{
			RenderSetup setup = RenderSetup.builder(RenderPipelinesProvider.TRIANGLES_CUTOUT).
					withTexture("Sampler0", texture).
					useLightmap().
					useOverlay().
					affectsCrumbling().
					setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE).
					createRenderSetup();

			return RenderType.create(PLibDatabase.rl("triangles_cutout").toString(), setup);
		}

		/**
		 * Creates the triangles translucent.
		 * @param texture the texture to use.
		 * @return the value produced by this operation.
		 */
		private static RenderType createTrianglesTranslucent(Identifier texture)
		{
			RenderSetup setup = RenderSetup.builder(RenderPipelinesProvider.TRIANGLES_TRANSLUCENT).
					withTexture("Sampler0", texture).
					setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET).
					useLightmap().
					useOverlay().
					affectsCrumbling().
					setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE).
					createRenderSetup();

			return RenderType.create(PLibDatabase.rl("triangles_translucent").toString(), setup);
		}

		/**
		 * Creates the triangles emissive solid.
		 * @param texture the texture to use.
		 * @return the value produced by this operation.
		 */
		private static RenderType createTrianglesEmissiveSolid(Identifier texture)
		{
			RenderSetup setup = RenderSetup.builder(RenderPipelinesProvider.TRIANGLES_EMISSIVE_SOLID).
					withTexture("Sampler0", texture).
					useOverlay().
					affectsCrumbling().
					setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE).
					createRenderSetup();

			return RenderType.create(PLibDatabase.rl("triangles_emissive_solid").toString(), setup);
		}

		/**
		 * Creates the triangles emissive cutout.
		 * @param texture the texture to use.
		 * @return the value produced by this operation.
		 */
		private static RenderType createTrianglesEmissiveCutout(Identifier texture)
		{
			RenderSetup setup = RenderSetup.builder(RenderPipelinesProvider.TRIANGLES_EMISSIVE_CUTOUT).
					withTexture("Sampler0", texture).
					useOverlay().
					affectsCrumbling().
					setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE).
					createRenderSetup();

			return RenderType.create(PLibDatabase.rl("triangles_emissive_cutout").toString(), setup);
		}

		/**
		 * Creates the triangles emissive translucent.
		 * @param texture the texture to use.
		 * @return the value produced by this operation.
		 */
		private static RenderType createTrianglesEmissiveTranslucent(Identifier texture)
		{
			RenderSetup setup = RenderSetup.builder(RenderPipelinesProvider.TRIANGLES_EMISSIVE_TRANSLUCENT).
					withTexture("Sampler0", texture).
					setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET).
					useOverlay().
					affectsCrumbling().
					setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE).
					createRenderSetup();

			return RenderType.create(PLibDatabase.rl("triangles_emissive_translucent").toString(), setup);
		}

		/**
		 * Creates the triangles instant solid.
		 * @param texture the texture to use.
		 * @return the value produced by this operation.
		 */
		private static RenderType createTrianglesInstantSolid(Identifier texture)
		{
			RenderSetup setup = RenderSetup.builder(RenderPipelinesProvider.TRIANGLES_INSTANT_SOLID).
					withTexture("Sampler0", texture).
					useLightmap().
					useOverlay().
					setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE).
					createRenderSetup();

			return RenderType.create(PLibDatabase.rl("triangles_instant_solid").toString(), setup);
		}

		/**
		 * Creates the triangles instant cutout.
		 * @param texture the texture to use.
		 * @return the value produced by this operation.
		 */
		private static RenderType createTrianglesInstantCutout(Identifier texture)
		{
			RenderSetup setup = RenderSetup.builder(RenderPipelinesProvider.TRIANGLES_INSTANT_CUTOUT).
					withTexture("Sampler0", texture).
					useLightmap().
					useOverlay().
					setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE).
					createRenderSetup();

			return RenderType.create(PLibDatabase.rl("triangles_instant_cutout").toString(), setup);
		}

		/**
		 * Creates the triangles instant translucent.
		 * @param texture the texture to use.
		 * @return the value produced by this operation.
		 */
		private static RenderType createTrianglesInstantTranslucent(Identifier texture)
		{
			RenderSetup setup = RenderSetup.builder(RenderPipelinesProvider.TRIANGLES_INSTANT_TRANSLUCENT).
					withTexture("Sampler0", texture).
					useLightmap().
					useOverlay().
					setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE).
					createRenderSetup();

			return RenderType.create(PLibDatabase.rl("triangles_instant_translucent").toString(), setup);
		}

		/**
		 * Creates the triangles instant emissive solid.
		 * @param texture the texture to use.
		 * @return the value produced by this operation.
		 */
		private static RenderType createTrianglesInstantEmissiveSolid(Identifier texture)
		{
			RenderSetup setup = RenderSetup.builder(RenderPipelinesProvider.TRIANGLES_INSTANT_EMISSIVE_SOLID).
					withTexture("Sampler0", texture).
					useOverlay().
					setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE).
					createRenderSetup();

			return RenderType.create(PLibDatabase.rl("triangles_instant_emissive_solid").toString(), setup);
		}

		/**
		 * Creates the triangles instant emissive cutout.
		 * @param texture the texture to use.
		 * @return the value produced by this operation.
		 */
		private static RenderType createTrianglesInstantEmissiveCutout(Identifier texture)
		{
			RenderSetup setup = RenderSetup.builder(RenderPipelinesProvider.TRIANGLES_INSTANT_EMISSIVE_CUTOUT).
					withTexture("Sampler0", texture).
					useOverlay().
					setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE).
					createRenderSetup();

			return RenderType.create(PLibDatabase.rl("triangles_instant_emissive_cutout").toString(), setup);
		}

		/**
		 * Creates the triangles instant emissive translucent.
		 * @param texture the texture to use.
		 * @return the value produced by this operation.
		 */
		private static RenderType createTrianglesInstantEmissiveTranslucent(Identifier texture)
		{
			RenderSetup setup = RenderSetup.builder(RenderPipelinesProvider.TRIANGLES_INSTANT_EMISSIVE_TRANSLUCENT).
					withTexture("Sampler0", texture).
					useOverlay().
					setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE).
					createRenderSetup();

			return RenderType.create(PLibDatabase.rl("triangles_instant_emissive_translucent").toString(), setup);
		}

		/**
		 * Performs the triangles solid operation.
		 * @param texture the texture to use.
		 * @return the value produced by this operation.
		 */
		public static RenderType trianglesSolid(Identifier texture)
		{
			return TRIANGLES_SOLID.apply(texture);
		}

		/**
		 * Performs the triangles cutout operation.
		 * @param texture the texture to use.
		 * @return the value produced by this operation.
		 */
		public static RenderType trianglesCutout(Identifier texture)
		{
			return TRIANGLES_CUTOUT.apply(texture);
		}

		/**
		 * Performs the triangles translucent operation.
		 * @param texture the texture to use.
		 * @return the value produced by this operation.
		 */
		public static RenderType trianglesTranslucent(Identifier texture)
		{
			return TRIANGLES_TRANSLUCENT.apply(texture);
		}

		/**
		 * Performs the triangles emissive solid operation.
		 * @param texture the texture to use.
		 * @return the value produced by this operation.
		 */
		public static RenderType trianglesEmissiveSolid(Identifier texture)
		{
			return TRIANGLES_EMISSIVE_SOLID.apply(texture);
		}

		/**
		 * Performs the triangles emissive cutout operation.
		 * @param texture the texture to use.
		 * @return the value produced by this operation.
		 */
		public static RenderType trianglesEmissiveCutout(Identifier texture)
		{
			return TRIANGLES_EMISSIVE_CUTOUT.apply(texture);
		}

		/**
		 * Performs the triangles emissive translucent operation.
		 * @param texture the texture to use.
		 * @return the value produced by this operation.
		 */
		public static RenderType trianglesEmissiveTranslucent(Identifier texture)
		{
			return TRIANGLES_EMISSIVE_TRANSLUCENT.apply(texture);
		}

		/**
		 * Performs the triangles instant solid operation.
		 * @param texture the texture to use.
		 * @return the value produced by this operation.
		 */
		public static RenderType trianglesInstantSolid(Identifier texture)
		{
			return TRIANGLES_INSTANT_SOLID.apply(texture);
		}

		/**
		 * Performs the triangles instant cutout operation.
		 * @param texture the texture to use.
		 * @return the value produced by this operation.
		 */
		public static RenderType trianglesInstantCutout(Identifier texture)
		{
			return TRIANGLES_INSTANT_CUTOUT.apply(texture);
		}

		/**
		 * Performs the triangles instant translucent operation.
		 * @param texture the texture to use.
		 * @return the value produced by this operation.
		 */
		public static RenderType trianglesInstantTranslucent(Identifier texture)
		{
			return TRIANGLES_INSTANT_TRANSLUCENT.apply(texture);
		}

		/**
		 * Performs the triangles instant emissive cutout operation.
		 * @param texture the texture to use.
		 * @return the value produced by this operation.
		 */
		public static RenderType trianglesInstantEmissiveCutout(Identifier texture)
		{
			return TRIANGLES_INSTANT_EMISSIVE_CUTOUT.apply(texture);
		}

		/**
		 * Performs the triangles instant emissive solid operation.
		 * @param texture the texture to use.
		 * @return the value produced by this operation.
		 */
		public static RenderType trianglesInstantEmissiveSolid(Identifier texture)
		{
			return TRIANGLES_INSTANT_EMISSIVE_SOLID.apply(texture);
		}

		/**
		 * Performs the triangles instant emissive translucent operation.
		 * @param texture the texture to use.
		 * @return the value produced by this operation.
		 */
		public static RenderType trianglesInstantEmissiveTranslucent(Identifier texture)
		{
			return TRIANGLES_INSTANT_EMISSIVE_TRANSLUCENT.apply(texture);
		}

		/**
		 * Performs the emissive variant operation.
		 * @param baseType the base type to use.
		 * @param texture the texture to use.
		 * @return the value produced by this operation.
		 */
		public static RenderType emissiveVariant(RenderType baseType, Identifier texture)
		{
			if (baseType.pipeline() == RenderPipelinesProvider.TRIANGLES_SOLID)
				return trianglesEmissiveSolid(texture);
			if (baseType.pipeline() == RenderPipelinesProvider.TRIANGLES_CUTOUT)
				return trianglesEmissiveCutout(texture);
			if (baseType.pipeline() == RenderPipelinesProvider.TRIANGLES_TRANSLUCENT)
				return trianglesEmissiveTranslucent(texture);
			if (baseType.pipeline() == RenderPipelinesProvider.TRIANGLES_INSTANT_TRANSLUCENT)
				return trianglesInstantEmissiveTranslucent(texture);
			if (baseType.pipeline() == RenderPipelinesProvider.TRIANGLES_INSTANT_CUTOUT)
				return trianglesInstantEmissiveCutout(texture);
			if (baseType.pipeline() == RenderPipelinesProvider.TRIANGLES_INSTANT_SOLID)
				return trianglesInstantEmissiveSolid(texture);
			return baseType;
		}

		/**
		 * Performs the instant variant operation.
		 * @param baseType the base type to use.
		 * @param texture the texture to use.
		 * @return the value produced by this operation.
		 */
		public static RenderType instantVariant(RenderType baseType, Identifier texture)
		{
			if (baseType.pipeline() == RenderPipelinesProvider.TRIANGLES_SOLID ||
					baseType.pipeline() == RenderPipelinesProvider.TRIANGLES_INSTANT_SOLID)
				return trianglesInstantSolid(texture);
			return isTransparent(baseType) ? trianglesInstantTranslucent(texture) : trianglesInstantCutout(texture);
		}

		/**
		 * Performs the instant emissive variant operation.
		 * @param baseType the base type to use.
		 * @param texture the texture to use.
		 * @return the value produced by this operation.
		 */
		public static RenderType instantEmissiveVariant(RenderType baseType, Identifier texture)
		{
			if (baseType.pipeline() == RenderPipelinesProvider.TRIANGLES_SOLID ||
					baseType.pipeline() == RenderPipelinesProvider.TRIANGLES_INSTANT_SOLID)
				return trianglesInstantEmissiveSolid(texture);
			return isTransparent(baseType) ? trianglesInstantEmissiveTranslucent(texture) : trianglesInstantEmissiveCutout(texture);
		}

		/**
		 * Performs the for alpha mode operation.
		 * @param alphaMode the alpha mode to use.
		 * @param texture the texture to use.
		 * @return the value produced by this operation.
		 */
		public static RenderType forAlphaMode(PAlphaMode alphaMode, Identifier texture)
		{
			return switch (alphaMode)
			{
				case OPAQUE -> trianglesSolid(texture);
				case CUTOUT -> trianglesCutout(texture);
				case TRANSLUCENT -> trianglesTranslucent(texture);
				case AUTO -> throw new IllegalArgumentException("AUTO alpha mode must be resolved before selecting a render type");
			};
		}

		/**
		 * Performs the for instant alpha mode operation.
		 * @param alphaMode the alpha mode to use.
		 * @param texture the texture to use.
		 * @return the value produced by this operation.
		 */
		public static RenderType forInstantAlphaMode(PAlphaMode alphaMode, Identifier texture)
		{
			return switch (alphaMode)
			{
				case OPAQUE -> trianglesInstantSolid(texture);
				case CUTOUT -> trianglesInstantCutout(texture);
				case TRANSLUCENT -> trianglesInstantTranslucent(texture);
				case AUTO -> throw new IllegalArgumentException("AUTO alpha mode must be resolved before selecting a render type");
			};
		}
	}

	/**
	 * Performs the register operation.
	 * @param registrar the platform pipeline registry callback.
	 */
	public static void register(Consumer<RenderPipeline> registrar)
	{
		RenderPipelinesProvider.registerCustomPipelines(registrar);
	}

	/**
	 * Determines whether transparent.
	 * @param renderType the render type to use.
	 * @return the value produced by this operation.
	 */
	@ApiStatus.Internal
	public static boolean isTransparent(RenderType renderType)
	{
		return renderType.pipeline().getColorTargetState().blendFunction().isPresent();
	}

	/**
	 * Performs the uses oit operation.
	 * @param renderType the render type to use.
	 * @return the value produced by this operation.
	 */
	@ApiStatus.Internal
	public static boolean usesOit(RenderType renderType)
	{
		RenderPipeline pipeline = renderType.pipeline();
		return pipeline == RenderPipelinesProvider.TRIANGLES_TRANSLUCENT ||
				pipeline == RenderPipelinesProvider.TRIANGLES_EMISSIVE_TRANSLUCENT;
	}
}
