/**
 * @author ArcAnc
 * Created at: 27.01.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.util;


import com.arcanc.pulselib.content.mixin.RenderTypeAccessor;
import com.arcanc.pulselib.content.model.textures.PAlphaMode;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Built-in triangle pipelines, shader accessors and render-type material classification.
 */
public final class PRenderTypes
{
	private PRenderTypes()
	{
	}

	/**
	 * Creates and caches triangle render types for alpha, emissive and isolated rendering policies.
	 */
	public static final class RenderTypeProvider
	{
		private RenderTypeProvider()
		{
		}

		private static final Set<RenderType> OIT_TRANSLUCENT_TYPES = Collections.newSetFromMap(new IdentityHashMap<>());
		private static final Set<RenderType> OIT_TRANSLUCENT_EMISSIVE_TYPES = Collections.newSetFromMap(new IdentityHashMap<>());
		private static final Function<ResourceLocation, RenderType> TRIANGLES_SOLID = Util.memoize(RenderTypeProvider :: createTrianglesSolid);
		private static final Function<ResourceLocation, RenderType> TRIANGLES_CUTOUT = Util.memoize(RenderTypeProvider :: createTrianglesCutout);
		private static final Function<ResourceLocation, RenderType> TRIANGLES_TRANSLUCENT = Util.memoize(RenderTypeProvider :: createTrianglesTranslucent);
		private static final Function<ResourceLocation, RenderType> TRIANGLES_GUI = Util.memoize(RenderTypeProvider :: createTrianglesGui);
		private static final Function<ResourceLocation, RenderType> TRIANGLES_IMMEDIATE = Util.memoize(RenderTypeProvider :: createTrianglesImmediate);
		private static final Function<ResourceLocation, RenderType> TRIANGLES_SOLID_EMISSIVE = Util.memoize(RenderTypeProvider :: createTrianglesSolidEmissive);
		private static final Function<ResourceLocation, RenderType> TRIANGLES_CUTOUT_EMISSIVE = Util.memoize(RenderTypeProvider :: createTrianglesCutoutEmissive);
		private static final Function<ResourceLocation, RenderType> TRIANGLES_TRANSLUCENT_EMISSIVE = Util.memoize(RenderTypeProvider :: createTrianglesTranslucentEmissive);
		private static final Function<ResourceLocation, RenderType> TRIANGLES_GUI_EMISSIVE = Util.memoize(RenderTypeProvider :: createTrianglesGuiEmissive);
		private static final Function<ResourceLocation, RenderType> TRIANGLES_IMMEDIATE_EMISSIVE = Util.memoize(RenderTypeProvider :: createTrianglesImmediateEmissive);
		
		private static RenderType createTrianglesSolid(ResourceLocation location)
		{
			return createTriangles("triangles_solid", location,
					ShadersProvider.StateShard.TRIANGLES_SOLID_STATE_SHARD,
					RenderStateShard.NO_TRANSPARENCY);
		}
		
		private static RenderType createTrianglesCutout(ResourceLocation location)
		{
			return createTriangles("triangles_cutout", location,
					ShadersProvider.StateShard.TRIANGLES_CUTOUT_STATE_SHARD,
					RenderStateShard.NO_TRANSPARENCY);
		}
		
		private static RenderType createTrianglesTranslucent(ResourceLocation location)
		{
			RenderType type = createTriangles("triangles_translucent", location,
					ShadersProvider.StateShard.TRIANGLES_TRANSLUCENT_STATE_SHARD,
					RenderStateShard.TRANSLUCENT_TRANSPARENCY);
			OIT_TRANSLUCENT_TYPES.add(type);
			return type;
		}
		
		private static RenderType createTrianglesGui(ResourceLocation location)
		{
			return createTriangles("triangles_gui", location,
					ShadersProvider.StateShard.TRIANGLES_IMMEDIATE_LIT_STATE_SHARD,
					RenderStateShard.TRANSLUCENT_TRANSPARENCY);
		}
		
		private static RenderType createTrianglesImmediate(ResourceLocation location)
		{
			return createTriangles("triangles_immediate", location,
					ShadersProvider.StateShard.TRIANGLES_IMMEDIATE_LIT_STATE_SHARD,
					RenderStateShard.NO_TRANSPARENCY);
		}
		
		private static RenderType createTrianglesSolidEmissive(ResourceLocation location)
		{
			return createTriangles("triangles_solid_emissive", location,
					ShadersProvider.StateShard.TRIANGLES_SOLID_EMISSIVE_STATE_SHARD,
					RenderStateShard.NO_TRANSPARENCY);
		}
		
		private static RenderType createTrianglesCutoutEmissive(ResourceLocation location)
		{
			return createTriangles("triangles_cutout_emissive", location,
					ShadersProvider.StateShard.TRIANGLES_CUTOUT_EMISSIVE_STATE_SHARD,
					RenderStateShard.NO_TRANSPARENCY);
		}
		
		private static RenderType createTrianglesTranslucentEmissive(ResourceLocation location)
		{
			RenderType type = createTriangles("triangles_translucent_emissive", location,
					ShadersProvider.StateShard.TRIANGLES_TRANSLUCENT_EMISSIVE_STATE_SHARD,
					RenderStateShard.TRANSLUCENT_TRANSPARENCY);
			OIT_TRANSLUCENT_EMISSIVE_TYPES.add(type);
			return type;
		}
		
		private static RenderType createTrianglesGuiEmissive(ResourceLocation location)
		{
			return createTriangles("triangles_gui_emissive", location,
					ShadersProvider.StateShard.TRIANGLES_IMMEDIATE_EMISSIVE_STATE_SHARD,
					RenderStateShard.TRANSLUCENT_TRANSPARENCY);
		}
		
		private static RenderType createTrianglesImmediateEmissive(ResourceLocation location)
		{
			return createTriangles("triangles_immediate_emissive", location,
					ShadersProvider.StateShard.TRIANGLES_IMMEDIATE_EMISSIVE_STATE_SHARD,
					RenderStateShard.NO_TRANSPARENCY);
		}

		private static RenderType createTriangles(String name,
		                                          ResourceLocation location,
		                                          RenderStateShard.ShaderStateShard shader,
		                                          RenderStateShard.TransparencyStateShard transparency)
		{
			RenderType.CompositeState.CompositeStateBuilder state = RenderType.CompositeState.builder().
					setShaderState(shader).
					setTextureState(new RenderStateShard.TextureStateShard(location, false, false)).
					setTransparencyState(transparency).
					setCullState(RenderStateShard.NO_CULL).
					setLightmapState(RenderStateShard.LIGHTMAP).
					setOverlayState(RenderStateShard.OVERLAY);
			return RenderType.create(
					PLibDatabase.rl(name).toString(),
					VertexFormatProvider.POSITION_TEX_NORMAL,
					VertexFormat.Mode.TRIANGLES,
					1536,
					true,
					false,
					state.createCompositeState(true));
		}
		
		/**
		 * Returns the cached lit render pipeline for opaque triangle geometry.
		 *
		 * @param location the location
		 * @return the cached lit render pipeline for opaque triangle geometry
		 */
		public static RenderType trianglesSolid(ResourceLocation location)
		{
			return TRIANGLES_SOLID.apply(location);
		}
		
		/**
		 * Returns the cached lit render pipeline for alpha-tested triangle geometry.
		 *
		 * @param location the location
		 * @return the cached lit render pipeline for alpha-tested triangle geometry
		 */
		public static RenderType trianglesCutout(ResourceLocation location)
		{
			return TRIANGLES_CUTOUT.apply(location);
		}
		
		/**
		 * Returns the cached lit render pipeline for translucent triangle geometry.
		 *
		 * @param location the location
		 * @return the cached lit render pipeline for translucent triangle geometry
		 */
		public static RenderType trianglesTranslucent(ResourceLocation location)
		{
			return TRIANGLES_TRANSLUCENT.apply(location);
		}
		
		/**
		 * Returns the cached lit render pipeline for isolated GUI triangle geometry.
		 *
		 * @param location the location
		 * @return the cached lit render pipeline for isolated GUI triangle geometry
		 */
		public static RenderType trianglesGui(ResourceLocation location)
		{
			return TRIANGLES_GUI.apply(location);
		}
		
		/**
		 * Returns the cached lit render pipeline for immediate triangle geometry.
		 *
		 * @param location the location
		 * @return the cached lit render pipeline for immediate triangle geometry
		 */
		public static RenderType trianglesImmediate(ResourceLocation location)
		{
			return TRIANGLES_IMMEDIATE.apply(location);
		}
		
		/**
		 * Returns the cached emissive render pipeline for opaque triangle geometry.
		 *
		 * @param location the location
		 * @return the cached emissive render pipeline for opaque triangle geometry
		 */
		public static RenderType trianglesSolidEmissive(ResourceLocation location)
		{
			return TRIANGLES_SOLID_EMISSIVE.apply(location);
		}
		
		/**
		 * Returns the cached emissive render pipeline for alpha-tested triangle geometry.
		 *
		 * @param location the location
		 * @return the cached emissive render pipeline for alpha-tested triangle geometry
		 */
		public static RenderType trianglesCutoutEmissive(ResourceLocation location)
		{
			return TRIANGLES_CUTOUT_EMISSIVE.apply(location);
		}
		
		/**
		 * Returns the cached emissive render pipeline for translucent triangle geometry.
		 *
		 * @param location the location
		 * @return the cached emissive render pipeline for translucent triangle geometry
		 */
		public static RenderType trianglesTranslucentEmissive(ResourceLocation location)
		{
			return TRIANGLES_TRANSLUCENT_EMISSIVE.apply(location);
		}
		
		/**
		 * Returns the cached emissive render pipeline for isolated GUI triangle geometry.
		 *
		 * @param location the location
		 * @return the cached emissive render pipeline for isolated GUI triangle geometry
		 */
		public static RenderType trianglesGuiEmissive(ResourceLocation location)
		{
			return TRIANGLES_GUI_EMISSIVE.apply(location);
		}
		
		/**
		 * Returns the cached emissive render pipeline for immediate triangle geometry.
		 *
		 * @param location the location
		 * @return the cached emissive render pipeline for immediate triangle geometry
		 */
		public static RenderType trianglesImmediateEmissive(ResourceLocation location)
		{
			return TRIANGLES_IMMEDIATE_EMISSIVE.apply(location);
		}

		/**
		 * Selects the built-in triangle pipeline matching the supplied alpha policy.
		 *
		 * @param alphaMode the material alpha handling policy
		 * @param location the location
		 * @return the vanilla render pipeline
		 */
		public static RenderType forAlphaMode(PAlphaMode alphaMode, ResourceLocation location)
		{
			return switch (alphaMode)
			{
				case OPAQUE -> trianglesSolid(location);
				case CUTOUT -> trianglesCutout(location);
				case TRANSLUCENT -> trianglesTranslucent(location);
				case AUTO -> throw new IllegalArgumentException("AUTO alpha mode must be resolved before selecting a render type");
			};
		}
		
		/**
		 * Selects the emissive variant corresponding to a recognized built-in render type.
		 *
		 * @param renderType the render type
		 * @param location the location
		 * @return the vanilla render pipeline
		 */
		public static RenderType emissiveVariant(RenderType renderType, ResourceLocation location)
		{
			if (renderType == trianglesSolid(location))
				return trianglesSolidEmissive(location);
			if (renderType == trianglesCutout(location))
				return trianglesCutoutEmissive(location);
			if (renderType == trianglesTranslucent(location))
				return trianglesTranslucentEmissive(location);
			if (renderType == trianglesGui(location))
				return trianglesGuiEmissive(location);
			if (renderType == trianglesImmediate(location))
				return trianglesImmediateEmissive(location);
			return renderType;
		}

		/**
		 * Returns whether the pipeline participates in PulseLib weighted transparency.
		 *
		 * @param renderType the render type
		 * @return whether the pipeline participates in PulseLib weighted transparency
		 */
		public static boolean usesOit(RenderType renderType)
		{
			return OIT_TRANSLUCENT_TYPES.contains(renderType) || OIT_TRANSLUCENT_EMISSIVE_TYPES.contains(renderType);
		}

		/**
		 * Returns whether the pipeline uses emissive weighted transparency.
		 *
		 * @param renderType the render type
		 * @return whether the pipeline uses emissive weighted transparency
		 */
		public static boolean usesEmissiveOit(RenderType renderType)
		{
			return OIT_TRANSLUCENT_EMISSIVE_TYPES.contains(renderType);
		}
	}
	
	/**
	 * Vertex layouts shared by baked geometry and PulseLib shaders.
	 */
	public static final class VertexFormatProvider
	{
		private VertexFormatProvider()
		{
		}

		/**
		 * Triangle vertex layout containing position, texture coordinates and normal.
		 */
		public static final VertexFormat POSITION_TEX_NORMAL = VertexFormat.builder().
				add("Position", VertexFormatElement.POSITION).
				add("UV0", VertexFormatElement.UV0).
				add("Normal", VertexFormatElement.NORMAL).
				padding(1).
				build();
	}
	
	/**
	 * Reload-populated shader instances used by the built-in render pipelines.
	 */
	public static final class ShadersProvider
	{
		private ShadersProvider()
		{
		}

		/**
		 * Render-state shard used to access shader registration and selection helpers.
		 */
		private static final class StateShard
		{
			private static final RenderStateShard.ShaderStateShard TRIANGLES_SOLID_STATE_SHARD = new RenderStateShard.ShaderStateShard(() -> ShadersProvider.TRIANGLES_SOLID_SHADER);
			private static final RenderStateShard.ShaderStateShard TRIANGLES_CUTOUT_STATE_SHARD = new RenderStateShard.ShaderStateShard(() -> ShadersProvider.TRIANGLES_CUTOUT_SHADER);
			private static final RenderStateShard.ShaderStateShard TRIANGLES_TRANSLUCENT_STATE_SHARD = new RenderStateShard.ShaderStateShard(() -> ShadersProvider.TRIANGLES_TRANSLUCENT_SHADER);
			private static final RenderStateShard.ShaderStateShard TRIANGLES_IMMEDIATE_LIT_STATE_SHARD = new RenderStateShard.ShaderStateShard(() -> ShadersProvider.TRIANGLES_IMMEDIATE_LIT_SHADER);
			private static final RenderStateShard.ShaderStateShard TRIANGLES_SOLID_EMISSIVE_STATE_SHARD = new RenderStateShard.ShaderStateShard(() -> ShadersProvider.TRIANGLES_SOLID_EMISSIVE_SHADER);
			private static final RenderStateShard.ShaderStateShard TRIANGLES_CUTOUT_EMISSIVE_STATE_SHARD = new RenderStateShard.ShaderStateShard(() -> ShadersProvider.TRIANGLES_CUTOUT_EMISSIVE_SHADER);
			private static final RenderStateShard.ShaderStateShard TRIANGLES_TRANSLUCENT_EMISSIVE_STATE_SHARD = new RenderStateShard.ShaderStateShard(() -> ShadersProvider.TRIANGLES_TRANSLUCENT_EMISSIVE_SHADER);
			private static final RenderStateShard.ShaderStateShard TRIANGLES_IMMEDIATE_EMISSIVE_STATE_SHARD = new RenderStateShard.ShaderStateShard(() -> ShadersProvider.TRIANGLES_IMMEDIATE_EMISSIVE_SHADER);
		}
		
		@Nullable
		private static ShaderInstance TRIANGLES_SOLID_SHADER;
		@Nullable
		private static ShaderInstance TRIANGLES_CUTOUT_SHADER;
		@Nullable
		private static ShaderInstance TRIANGLES_TRANSLUCENT_SHADER;
		@Nullable
		private static ShaderInstance TRIANGLES_IMMEDIATE_LIT_SHADER;
		@Nullable
		private static ShaderInstance TRIANGLES_SOLID_EMISSIVE_SHADER;
		@Nullable
		private static ShaderInstance TRIANGLES_CUTOUT_EMISSIVE_SHADER;
		@Nullable
		private static ShaderInstance TRIANGLES_TRANSLUCENT_EMISSIVE_SHADER;
		@Nullable
		private static ShaderInstance TRIANGLES_IMMEDIATE_EMISSIVE_SHADER;
		@Nullable
		private static ShaderInstance TRIANGLES_OIT_SHADER;
		@Nullable
		private static ShaderInstance TRIANGLES_OIT_EMISSIVE_SHADER;
		@Nullable
		private static ShaderInstance TRIANGLES_OIT_DEPTH_SHADER;
		@Nullable
		private static ShaderInstance TRIANGLES_OIT_DEPTH_EMISSIVE_SHADER;
		@Nullable
		private static ShaderInstance TRIANGLES_OIT_DEPTH_PEEL_SHADER;
		@Nullable
		private static ShaderInstance TRIANGLES_OIT_DEPTH_PEEL_EMISSIVE_SHADER;
		@Nullable
		private static ShaderInstance OIT_COMPOSITE_SHADER;
		@Nullable
		private static ShaderInstance OIT_FABULOUS_COMPOSITE_SHADER;
		@Nullable
		private static ShaderInstance OIT_FABULOUS_METADATA_SHADER;
		
		/**
		 * Returns the triangles solid.
		 *
		 * @return the triangles solid, or {@code null} before the requested shader has been registered
		 */
		public static @Nullable ShaderInstance trianglesSolid()
		{
			return TRIANGLES_SOLID_SHADER;
		}
		/**
		 * Returns the triangles cutout.
		 *
		 * @return the triangles cutout, or {@code null} before the requested shader has been registered
		 */
		public static @Nullable ShaderInstance trianglesCutout()
		{
			return TRIANGLES_CUTOUT_SHADER;
		}
		/**
		 * Returns the triangles translucent.
		 *
		 * @return the triangles translucent, or {@code null} before the requested shader has been registered
		 */
		public static @Nullable ShaderInstance trianglesTranslucent()
		{
			return TRIANGLES_TRANSLUCENT_SHADER;
		}
		/**
		 * Returns the triangles immediate lit.
		 *
		 * @return the triangles immediate lit, or {@code null} before the requested shader has been registered
		 */
		public static @Nullable ShaderInstance trianglesImmediateLit()
		{
			return TRIANGLES_IMMEDIATE_LIT_SHADER;
		}
		/**
		 * Returns the triangles solid emissive.
		 *
		 * @return the triangles solid emissive, or {@code null} before the requested shader has been registered
		 */
		public static @Nullable ShaderInstance trianglesSolidEmissive()
		{
			return TRIANGLES_SOLID_EMISSIVE_SHADER;
		}
		/**
		 * Returns the triangles cutout emissive.
		 *
		 * @return the triangles cutout emissive, or {@code null} before the requested shader has been registered
		 */
		public static @Nullable ShaderInstance trianglesCutoutEmissive()
		{
			return TRIANGLES_CUTOUT_EMISSIVE_SHADER;
		}
		/**
		 * Returns the triangles translucent emissive.
		 *
		 * @return the triangles translucent emissive, or {@code null} before the requested shader has been registered
		 */
		public static @Nullable ShaderInstance trianglesTranslucentEmissive()
		{
			return TRIANGLES_TRANSLUCENT_EMISSIVE_SHADER;
		}
		/**
		 * Returns the triangles immediate emissive.
		 *
		 * @return the triangles immediate emissive, or {@code null} before the requested shader has been registered
		 */
		public static @Nullable ShaderInstance trianglesImmediateEmissive()
		{
			return TRIANGLES_IMMEDIATE_EMISSIVE_SHADER;
		}

		/**
		 * Returns the lit or emissive weighted-transparency accumulation shader.
		 *
		 * @param emissive the emissive
		 * @return the lit or emissive weighted-transparency accumulation shader, or {@code null} before the requested shader has been registered
		 */
		public static @Nullable ShaderInstance trianglesOit(boolean emissive)
		{
			return emissive ? TRIANGLES_OIT_EMISSIVE_SHADER : TRIANGLES_OIT_SHADER;
		}

		/**
		 * Returns the lit or emissive translucent-depth shader.
		 *
		 * @param emissive the emissive
		 * @return the lit or emissive translucent-depth shader, or {@code null} before the requested shader has been registered
		 */
		public static @Nullable ShaderInstance trianglesOitDepth(boolean emissive)
		{
			return emissive ? TRIANGLES_OIT_DEPTH_EMISSIVE_SHADER : TRIANGLES_OIT_DEPTH_SHADER;
		}

		/**
		 * Returns the lit or emissive depth-peeling shader.
		 *
		 * @param emissive the emissive
		 * @return the lit or emissive depth-peeling shader, or {@code null} before the requested shader has been registered
		 */
		public static @Nullable ShaderInstance trianglesOitDepthPeel(boolean emissive)
		{
			return emissive ? TRIANGLES_OIT_DEPTH_PEEL_EMISSIVE_SHADER : TRIANGLES_OIT_DEPTH_PEEL_SHADER;
		}

		/**
		 * Returns the oit fabulous composite.
		 *
		 * @return the oit fabulous composite, or {@code null} before the requested shader has been registered
		 */
		@Nullable
		public static ShaderInstance oitFabulousComposite()
		{
			return OIT_FABULOUS_COMPOSITE_SHADER;
		}

		/**
		 * Returns the oit fabulous metadata.
		 *
		 * @return the oit fabulous metadata, or {@code null} before the requested shader has been registered
		 */
		@Nullable
		public static ShaderInstance oitFabulousMetadata()
		{
			return OIT_FABULOUS_METADATA_SHADER;
		}

		/**
		 * Returns the oit composite.
		 *
		 * @return the oit composite, or {@code null} before the requested shader has been registered
		 */
		public static @Nullable ShaderInstance oitComposite()
		{
			return OIT_COMPOSITE_SHADER;
		}
		
		/**
		 * Declares every built-in shader through the active loader's shader registration bridge.
		 *
		 * @param event the typed animation event to dispatch
		 * @throws IOException if the source resource cannot be read
		 */
		public static void registerShaders(final ShaderRegistrar event) throws IOException
		{
			{
				registerShader(event, "triangles_solid", VertexFormatProvider.POSITION_TEX_NORMAL,
						shader -> TRIANGLES_SOLID_SHADER = shader);
				registerShader(event, "triangles_cutout", VertexFormatProvider.POSITION_TEX_NORMAL,
						shader -> TRIANGLES_CUTOUT_SHADER = shader);
				registerShader(event, "triangles_translucent", VertexFormatProvider.POSITION_TEX_NORMAL,
						shader -> TRIANGLES_TRANSLUCENT_SHADER = shader);

				registerShader(event, "triangles_solid_emissive", VertexFormatProvider.POSITION_TEX_NORMAL,
						shader -> TRIANGLES_SOLID_EMISSIVE_SHADER = shader);
				registerShader(event, "triangles_cutout_emissive", VertexFormatProvider.POSITION_TEX_NORMAL,
						shader -> TRIANGLES_CUTOUT_EMISSIVE_SHADER = shader);
				registerShader(event, "triangles_translucent_emissive", VertexFormatProvider.POSITION_TEX_NORMAL,
						shader -> TRIANGLES_TRANSLUCENT_EMISSIVE_SHADER = shader);

				registerShader(event, "triangles_immediate_lit", VertexFormatProvider.POSITION_TEX_NORMAL,
						shader -> TRIANGLES_IMMEDIATE_LIT_SHADER = shader);
				registerShader(event, "triangles_immediate_emissive", VertexFormatProvider.POSITION_TEX_NORMAL,
						shader -> TRIANGLES_IMMEDIATE_EMISSIVE_SHADER = shader);

				registerShader(event, "triangles_oit", VertexFormatProvider.POSITION_TEX_NORMAL,
						shader -> TRIANGLES_OIT_SHADER = shader);
				registerShader(event, "triangles_oit_emissive", VertexFormatProvider.POSITION_TEX_NORMAL,
						shader -> TRIANGLES_OIT_EMISSIVE_SHADER = shader);
				registerShader(event, "triangles_oit_depth", VertexFormatProvider.POSITION_TEX_NORMAL,
						shader -> TRIANGLES_OIT_DEPTH_SHADER = shader);
				registerShader(event, "triangles_oit_depth_emissive", VertexFormatProvider.POSITION_TEX_NORMAL,
						shader -> TRIANGLES_OIT_DEPTH_EMISSIVE_SHADER = shader);
				registerShader(event, "triangles_oit_depth_peel", VertexFormatProvider.POSITION_TEX_NORMAL,
						shader -> TRIANGLES_OIT_DEPTH_PEEL_SHADER = shader);
				registerShader(event, "triangles_oit_depth_peel_emissive", VertexFormatProvider.POSITION_TEX_NORMAL,
						shader -> TRIANGLES_OIT_DEPTH_PEEL_EMISSIVE_SHADER = shader);
				registerShader(event, "oit_composite", DefaultVertexFormat.BLIT_SCREEN,
						shader -> OIT_COMPOSITE_SHADER = shader);
				registerShader(event, "oit_fabulous_composite", DefaultVertexFormat.BLIT_SCREEN,
						shader -> OIT_FABULOUS_COMPOSITE_SHADER = shader);
				registerShader(event, "oit_fabulous_metadata", DefaultVertexFormat.BLIT_SCREEN,
						shader -> OIT_FABULOUS_METADATA_SHADER = shader);
			}
		}

		private static void registerShader(ShaderRegistrar event,
		                                   String name,
		                                   VertexFormat format,
		                                   Consumer<ShaderInstance> setter) throws IOException
		{
			event.register(PLibDatabase.rl(name), format, shader ->
			{
				setter.accept(shader);
				PLibDatabase.LOGGER.debug("PulseLib initialized shader {}", name);
			});
		}
	}
	
	/**
	 * Loader-neutral callback for registering and receiving a shader instance.
	 */
	@FunctionalInterface
	public interface ShaderRegistrar
	{
		/**
		 * Registers a shader resource with its vertex layout and reload assignment callback.
		 *
		 * @param id the namespaced resource or registration identifier
		 * @param format the vertex layout used to interpret vertex bytes
		 * @param setter the setter
		 * @throws IOException if the source resource cannot be read
		 */
		void register(ResourceLocation id, VertexFormat format, Consumer<ShaderInstance> setter) throws IOException;
	}
	
	/**
	 * Reads the transparency shard of a compatible composite render type.
	 *
	 * @param renderType the render type
	 * @return the transparency shard, or an empty optional for an incompatible render type
	 */
	@ApiStatus.Internal
	public static Optional<RenderStateShard.TransparencyStateShard> getTransparencyState(RenderType renderType)
	{
		if (renderType instanceof RenderTypeAccessor.CompositeRenderTypeAccessor composite)
		{
			RenderType.CompositeState state = composite.pulselib$getState();
			return Optional.of(((RenderTypeAccessor.CompositeStateAccessor)(Object)state).pulselib$getTransparencyState());
		}
		return Optional.empty();
	}
	
	/**
	 * Returns whether the render type uses a translucent transparency state.
	 *
	 * @param renderType the render type
	 * @return whether the render type uses a translucent transparency state
	 */
	@ApiStatus.Internal
	public static boolean isTranslucent(RenderType renderType)
	{
		return getTransparencyState(renderType).
				map(transparency -> transparency != RenderStateShard.TransparencyStateShard.NO_TRANSPARENCY).
				orElse(false);
	}
}
