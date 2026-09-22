package com.arcanc.pulselib.util;

import com.arcanc.pulselib.content.event.PulseLibEvents;
import com.arcanc.pulselib.content.model.PTextureReference;
import com.arcanc.pulselib.content.model.resource.PModelResource;
import com.arcanc.pulselib.util.helpers.PLibRenderHelper;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
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
	public static Optional<PModelResource> getModelResource(ResourceLocation model)
	{
		PModelResource exact = resources.get(model);
		if (exact != null)
			return Optional.of(exact);
		return resources.values().stream().filter(resource ->
				PModelCache.getModelLoader(resource.modelLoaderId())
						.map(loader -> loader.modelResourceCandidates(resource.model()).contains(model))
						.orElse(false)).findFirst();
	}

	@ApiStatus.Internal
	public static Optional<PModelResource> getModelResource(ResourceLocation model, ResourceManager resourceManager)
	{
		PModelResource exact = resources.get(model);
		if (exact != null)
			return Optional.of(exact);
		return resources.values().stream().filter(resource ->
				PModelCache.getModelLoader(resource.modelLoaderId()).map(loader ->
						resourceManager.getResource(resource.model()).isEmpty()
								&& loader.modelResourceCandidates(resource.model()).contains(model))
						.orElse(false)).findFirst();
	}

	@ApiStatus.Internal
	public static void clear()
	{
		resources.clear();
		textures = null;
	}

	public static ResourceLocation resolve(ResourceLocation model, String textureReference)
	{
		PModelResource resource = getModelResource(model)
				.orElseThrow(() -> new IllegalStateException("No resources registered for model " + model));
		ResourceLocation texture = Optional.ofNullable(resource.textures().get(PTextureReference.normalize(textureReference)))
				.orElseThrow(() -> new IllegalStateException(
						"No texture registered for model " + model + ": " + textureReference));
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
