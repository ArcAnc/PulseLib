/**
 * @author ArcAnc
 * Created at: 11.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test;

import com.arcanc.pulselib.content.model.textures.atlas.PLibMetadata;
import com.arcanc.pulselib.content.model.textures.atlas.PLibSpriteMetadata;
import com.arcanc.pulselib.content.model.textures.atlas.RuntimeLoader;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PResourceCache;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.PRenderTypes.RenderPipelinesProvider;
import com.arcanc.pulselib.util.PRenderTypes.RenderTypeProvider;
import com.arcanc.pulselib.util.PRenderTypes.ShadersProvider;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/** Exercises canonical material entry points and actual dual atlas metadata on both loaders. */
public final class MaterialApiProbe
{
	private static boolean complete;

	private MaterialApiProbe()
	{
	}

	public static void run()
	{
		if (complete)
			return;
		Identifier texture = PResourceCache.ATLAS_LOCATION;
		DemoChecks.check(RenderTypeProvider.trianglesImmediate(texture) == RenderTypeProvider.trianglesInstantCutout(texture), "Immediate native alias");
		DemoChecks.check(RenderTypeProvider.trianglesSolidEmissive(texture) == RenderTypeProvider.trianglesEmissiveSolid(texture), "Solid emissive native alias");
		DemoChecks.check(RenderTypeProvider.trianglesCutoutEmissive(texture) == RenderTypeProvider.trianglesEmissiveCutout(texture), "Cutout emissive native alias");
		DemoChecks.check(RenderTypeProvider.trianglesTranslucentEmissive(texture) == RenderTypeProvider.trianglesEmissiveTranslucent(texture), "Translucent emissive native alias");
		DemoChecks.check(RenderTypeProvider.trianglesGuiEmissive(texture) == RenderTypeProvider.trianglesInstantEmissiveTranslucent(texture), "GUI emissive native alias");
		DemoChecks.check(RenderTypeProvider.trianglesImmediateEmissive(texture) == RenderTypeProvider.trianglesInstantEmissiveCutout(texture), "Immediate emissive native alias");
		DemoChecks.check(RenderTypeProvider.emissiveVariant(RenderTypeProvider.trianglesGui(texture), texture) == RenderTypeProvider.trianglesGuiEmissive(texture), "GUI emissive variant retains ordinary alpha");
		DemoChecks.check(RenderTypeProvider.emissiveVariant(RenderTypeProvider.trianglesImmediate(texture), texture) == RenderTypeProvider.trianglesImmediateEmissive(texture), "Immediate emissive variant retains immediate layout");
		DemoChecks.check(RenderTypeProvider.emissiveVariant(RenderTypeProvider.trianglesSolid(texture), texture) == RenderTypeProvider.trianglesSolidEmissive(texture), "Opaque emissive selection");
		DemoChecks.check(RenderTypeProvider.emissiveVariant(RenderTypeProvider.trianglesCutout(texture), texture) == RenderTypeProvider.trianglesCutoutEmissive(texture), "Cutout emissive selection");
		DemoChecks.check(RenderTypeProvider.emissiveVariant(RenderTypeProvider.trianglesTranslucent(texture), texture) == RenderTypeProvider.trianglesTranslucentEmissive(texture), "Translucent emissive selection");
		DemoChecks.check(RenderTypeProvider.emissiveVariant(RenderTypeProvider.trianglesInstantSolid(texture), texture) == RenderTypeProvider.trianglesInstantEmissiveSolid(texture), "Native opaque immediate selection");
		DemoChecks.check(RenderTypeProvider.emissiveVariant(RenderTypes.entityCutout(texture), texture) == RenderTypes.entityCutout(texture), "Custom native render types retain ownership");
		var oit = RenderTypeProvider.trianglesTranslucentEmissive(texture);
		var gui = RenderTypeProvider.trianglesGuiEmissive(texture);
		DemoChecks.check(RenderTypeProvider.usesOit(oit) && RenderTypeProvider.usesEmissiveOit(oit), "Canonical emissive OIT capability");
		DemoChecks.check(RenderTypeProvider.usesOit(RenderTypeProvider.trianglesTranslucent(texture)) && !RenderTypeProvider.usesEmissiveOit(RenderTypeProvider.trianglesTranslucent(texture)), "Canonical lit OIT capability");
		DemoChecks.check(!RenderTypeProvider.usesOit(gui) && !RenderTypeProvider.usesEmissiveOit(gui) && PRenderTypes.isTranslucent(gui), "GUI ordinary alpha policy");
		DemoChecks.check(!PRenderTypes.isTranslucent(RenderTypeProvider.trianglesImmediate(texture)) && PRenderTypes.getTransparencyState(RenderTypeProvider.trianglesSolid(texture)).isEmpty(), "Opaque native transparency");
		DemoChecks.check(PRenderTypes.getTransparencyState(gui).equals(gui.pipeline().getColorTargetState().blendFunction()), "Native blend function identity");
		List<RenderPipeline> actual = List.of(ShadersProvider.trianglesSolid(), ShadersProvider.trianglesCutout(),
				ShadersProvider.trianglesTranslucent(), ShadersProvider.trianglesImmediateLit(), ShadersProvider.trianglesSolidEmissive(),
				ShadersProvider.trianglesCutoutEmissive(), ShadersProvider.trianglesTranslucentEmissive(), ShadersProvider.trianglesImmediateEmissive(),
				ShadersProvider.trianglesOit(false), ShadersProvider.trianglesOit(true), ShadersProvider.trianglesOitDepth(false),
				ShadersProvider.trianglesOitDepth(true), ShadersProvider.trianglesOitDepthPeel(false), ShadersProvider.trianglesOitDepthPeel(true),
				ShadersProvider.oitComposite(), ShadersProvider.oitFabulousComposite(), ShadersProvider.oitFabulousMetadata());
		List<RenderPipeline> expected = List.of(RenderPipelinesProvider.TRIANGLES_SOLID, RenderPipelinesProvider.TRIANGLES_CUTOUT,
				RenderPipelinesProvider.TRIANGLES_TRANSLUCENT, RenderPipelinesProvider.TRIANGLES_INSTANT_CUTOUT, RenderPipelinesProvider.TRIANGLES_EMISSIVE_SOLID,
				RenderPipelinesProvider.TRIANGLES_EMISSIVE_CUTOUT, RenderPipelinesProvider.TRIANGLES_EMISSIVE_TRANSLUCENT, RenderPipelinesProvider.TRIANGLES_INSTANT_EMISSIVE_CUTOUT,
				RenderPipelinesProvider.TRIANGLES_OIT, RenderPipelinesProvider.TRIANGLES_OIT_EMISSIVE, RenderPipelinesProvider.TRIANGLES_OIT_DEPTH,
				RenderPipelinesProvider.TRIANGLES_OIT_DEPTH, RenderPipelinesProvider.TRIANGLES_OIT_DEPTH_PEEL, RenderPipelinesProvider.TRIANGLES_OIT_DEPTH_PEEL,
				RenderPipelinesProvider.OIT_COMPOSITE, RenderPipelinesProvider.OIT_FABULOUS_COMPOSITE, RenderPipelinesProvider.OIT_FABULOUS_METADATA);
		for (int index = 0; index < expected.size(); index++)
			DemoChecks.check(actual.get(index) == expected.get(index), "Native shader pipeline " + index);
		DemoChecks.check(RuntimeLoader.TYPE == RuntimeLoader.CODEC && new RuntimeLoader().type() == new RuntimeLoader().codec(), "Native sprite-source codec identity");
		int metadataSprites = metadata();
		DemoChecks.check(metadataSprites > 0, "Canonical atlas metadata must be observed on a real sprite");
		complete = true;
		PLibDatabase.LOGGER.info("MATERIAL API PASS: aliases=6, materialVariants=7, pipelineChecks=17, metadataSprites={}", metadataSprites);
	}

	private static int metadata()
	{
		int observed = 0;
		var textures = PResourceCache.getResourceCache().values().stream().
				flatMap(resource -> resource.textures().values().stream()).collect(Collectors.toSet());
		try
		{
			for (var texture : textures)
			{
				var resource = Minecraft.getInstance().getResourceManager().getResource(SpriteSource.TEXTURE_ID_CONVERTER.idToFile(texture)).orElseThrow();
				var decoded = resource.metadata().getSection(PLibMetadata.TYPE);
				if (decoded.isEmpty())
					continue;
				var sprite = PResourceCache.getTextureAtlas().getSprite(PResourceCache.spriteId(texture)).contents();
				var canonical = sprite.getAdditionalMetadata(PLibMetadata.TYPE).orElseThrow();
				var nativeMetadata = sprite.getAdditionalMetadata(PLibSpriteMetadata.TYPE).orElseThrow();
				DemoChecks.check(canonical.equals(decoded.get()) && canonical.isEmissive() == nativeMetadata.emissive() && canonical.alphaMode() == nativeMetadata.alphaMode(), "Canonical/native sprite metadata parity");
				observed++;
			}
		}
		catch (IOException exception)
		{
			throw new IllegalStateException("Material metadata probe failed", exception);
		}
		return observed;
	}
}
