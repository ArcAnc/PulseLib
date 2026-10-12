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

public final class PRenderTypes
{
	private PRenderTypes()
	{
	}

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
		
		public static RenderType trianglesSolid(ResourceLocation location)
		{
			return TRIANGLES_SOLID.apply(location);
		}
		
		public static RenderType trianglesCutout(ResourceLocation location)
		{
			return TRIANGLES_CUTOUT.apply(location);
		}
		
		public static RenderType trianglesTranslucent(ResourceLocation location)
		{
			return TRIANGLES_TRANSLUCENT.apply(location);
		}
		
		public static RenderType trianglesGui(ResourceLocation location)
		{
			return TRIANGLES_GUI.apply(location);
		}
		
		public static RenderType trianglesImmediate(ResourceLocation location)
		{
			return TRIANGLES_IMMEDIATE.apply(location);
		}
		
		public static RenderType trianglesSolidEmissive(ResourceLocation location)
		{
			return TRIANGLES_SOLID_EMISSIVE.apply(location);
		}
		
		public static RenderType trianglesCutoutEmissive(ResourceLocation location)
		{
			return TRIANGLES_CUTOUT_EMISSIVE.apply(location);
		}
		
		public static RenderType trianglesTranslucentEmissive(ResourceLocation location)
		{
			return TRIANGLES_TRANSLUCENT_EMISSIVE.apply(location);
		}
		
		public static RenderType trianglesGuiEmissive(ResourceLocation location)
		{
			return TRIANGLES_GUI_EMISSIVE.apply(location);
		}
		
		public static RenderType trianglesImmediateEmissive(ResourceLocation location)
		{
			return TRIANGLES_IMMEDIATE_EMISSIVE.apply(location);
		}

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

		public static boolean usesOit(RenderType renderType)
		{
			return OIT_TRANSLUCENT_TYPES.contains(renderType) || OIT_TRANSLUCENT_EMISSIVE_TYPES.contains(renderType);
		}

		public static boolean usesEmissiveOit(RenderType renderType)
		{
			return OIT_TRANSLUCENT_EMISSIVE_TYPES.contains(renderType);
		}
	}
	
	public static final class VertexFormatProvider
	{
		private VertexFormatProvider()
		{
		}

		public static final VertexFormat POSITION_TEX_NORMAL = VertexFormat.builder().
				add("Position", VertexFormatElement.POSITION).
				add("UV0", VertexFormatElement.UV0).
				add("Normal", VertexFormatElement.NORMAL).
				padding(1).
				build();
	}
	
	public static final class ShadersProvider
	{
		private ShadersProvider()
		{
		}

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
		
		public static @Nullable ShaderInstance trianglesSolid()
		{
			return TRIANGLES_SOLID_SHADER;
		}
		public static @Nullable ShaderInstance trianglesCutout()
		{
			return TRIANGLES_CUTOUT_SHADER;
		}
		public static @Nullable ShaderInstance trianglesTranslucent()
		{
			return TRIANGLES_TRANSLUCENT_SHADER;
		}
		public static @Nullable ShaderInstance trianglesImmediateLit()
		{
			return TRIANGLES_IMMEDIATE_LIT_SHADER;
		}
		public static @Nullable ShaderInstance trianglesSolidEmissive()
		{
			return TRIANGLES_SOLID_EMISSIVE_SHADER;
		}
		public static @Nullable ShaderInstance trianglesCutoutEmissive()
		{
			return TRIANGLES_CUTOUT_EMISSIVE_SHADER;
		}
		public static @Nullable ShaderInstance trianglesTranslucentEmissive()
		{
			return TRIANGLES_TRANSLUCENT_EMISSIVE_SHADER;
		}
		public static @Nullable ShaderInstance trianglesImmediateEmissive()
		{
			return TRIANGLES_IMMEDIATE_EMISSIVE_SHADER;
		}

		public static @Nullable ShaderInstance trianglesOit(boolean emissive)
		{
			return emissive ? TRIANGLES_OIT_EMISSIVE_SHADER : TRIANGLES_OIT_SHADER;
		}

		public static @Nullable ShaderInstance trianglesOitDepth(boolean emissive)
		{
			return emissive ? TRIANGLES_OIT_DEPTH_EMISSIVE_SHADER : TRIANGLES_OIT_DEPTH_SHADER;
		}

		public static @Nullable ShaderInstance trianglesOitDepthPeel(boolean emissive)
		{
			return emissive ? TRIANGLES_OIT_DEPTH_PEEL_EMISSIVE_SHADER : TRIANGLES_OIT_DEPTH_PEEL_SHADER;
		}

		public static @Nullable ShaderInstance oitComposite()
		{
			return OIT_COMPOSITE_SHADER;
		}
		
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
	
	@FunctionalInterface
	public interface ShaderRegistrar
	{
		void register(ResourceLocation id, VertexFormat format, Consumer<ShaderInstance> setter) throws IOException;
	}
	
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
	
	@ApiStatus.Internal
	public static boolean isTranslucent(RenderType renderType)
	{
		return getTransparencyState(renderType).
				map(transparency -> transparency != RenderStateShard.TransparencyStateShard.NO_TRANSPARENCY).
				orElse(false);
	}
}
