/**
 * @author ArcAnc
 * Created at: 22.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.util;

import com.arcanc.pulselib.content.event.PulseLibEvents;
import com.arcanc.pulselib.content.model.PTextureReference;
import com.arcanc.pulselib.content.model.resource.PModelResource;
import com.arcanc.pulselib.util.helpers.PLibRenderHelper;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.client.event.RegisterMaterialAtlasesEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Owns registered model resources and the PulseLib texture atlas.
 */
public final class PResourceCache
{
	private static final String SPRITE_PREFIX = "runtime/";
	public static final ResourceLocation ATLAS_LOCATION = PLibDatabase.rl("textures/atlas.png");
	@ApiStatus.Internal
	public static final ResourceLocation ATLAS_FILE_LOCATION = PLibDatabase.rl("atlas");
	private static @Nullable TextureAtlas textures;
	private static final Map<ResourceLocation, PModelResource> resources = new LinkedHashMap<>();

	private PResourceCache()
	{
	}

	public static TextureAtlas getTextureAtlas()
	{
		if (textures == null)
			textures = PLibRenderHelper.mc().getModelManager().getAtlas(ATLAS_LOCATION);
		return textures;
	}

	public static void register(IEventBus modEventBus)
	{
		modEventBus.addListener(PResourceCache::registerAtlas);
	}

	private static void registerAtlas(RegisterMaterialAtlasesEvent event)
	{
		event.register(ATLAS_LOCATION, ATLAS_FILE_LOCATION);
	}

	@ApiStatus.Internal
	public static Map<ResourceLocation, PModelResource> getResourceCache()
	{
		return resources;
	}

	@ApiStatus.Internal
	public static void clear()
	{
		resources.clear();
		textures = null;
	}

	/**
	 * Rebuilds the logical model-resource registrations before any model or
	 * atlas reload work consumes them.
	 */
	@ApiStatus.Internal
	public static void reloadRegistrations()
	{
		clear();
		postEvent();
	}

	/**
	 * Discards the atlas instance while retaining logical model registrations.
	 */
	@ApiStatus.Internal
	public static void invalidateTextureAtlas()
	{
		textures = null;
	}

	public static ResourceLocation resolve(ResourceLocation modelId, String textureReference)
	{
		PModelResource resource = resources.get(modelId);
		if (resource == null)
			throw new IllegalStateException("No resources registered for model " + modelId);
		ResourceLocation texture = Optional.ofNullable(resource.textures().get(PTextureReference.normalize(textureReference))).
				orElseThrow(() -> new IllegalStateException(
						"No texture registered for model " + modelId + ": " + textureReference));
		return spriteId(texture);
	}

	@ApiStatus.Internal
	public static ResourceLocation spriteId(ResourceLocation texture)
	{
		return PLibDatabase.rl(SPRITE_PREFIX + texture.getNamespace() + "/" + texture.getPath());
	}

	@ApiStatus.Internal
	public static void postEvent()
	{
		PulseLibEvents.RegisterResourceEvent event = new PulseLibEvents.RegisterResourceEvent();
		ModLoader.postEvent(event);
		event.apply(resources);
	}
}
