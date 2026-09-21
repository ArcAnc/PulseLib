package com.arcanc.pulselib.util;

import com.arcanc.pulselib.content.event.PulseLibEvents;
import com.arcanc.pulselib.content.model.PTextureReference;
import com.arcanc.pulselib.content.model.resource.PModelResource;
import com.arcanc.pulselib.content.model.textures.atlas.PLibSpriteMetadata;
import com.arcanc.pulselib.util.helpers.PLibRenderHelper;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.client.event.RegisterTextureAtlasesEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Owns the model-resource registrations and the atlas built from them. */
public final class PResourceCache
{
	private static final String SPRITE_PATH_PREFIX = "runtime/";
	public static final Identifier ATLAS_LOCATION = PLibDatabase.rl("textures/atlas.png");
	@ApiStatus.Internal
	public static final Identifier ATLAS_FILE_LOCATION = PLibDatabase.rl("atlas");
	private static @Nullable TextureAtlas textures;
	private static final Map<Identifier, PModelResource> RESOURCE_CACHE = new LinkedHashMap<>();

	private PResourceCache()
	{
	}

	public static TextureAtlas getTextureAtlas()
	{
		if (textures == null)
			textures = PLibRenderHelper.mc().getAtlasManager().getAtlasOrThrow(ATLAS_FILE_LOCATION);
		return textures;
	}

	public static void register(IEventBus modEventBus)
	{
		modEventBus.addListener(PResourceCache :: registerAtlas);
	}

	private static void registerAtlas(RegisterTextureAtlasesEvent event)
	{
		event.register(new AtlasManager.AtlasConfig(ATLAS_LOCATION, ATLAS_FILE_LOCATION, false));
		event.addAdditionalMetadata(ATLAS_FILE_LOCATION, PLibSpriteMetadata.TYPE);
	}

	@ApiStatus.Internal
	public static Map<Identifier, PModelResource> getResourceCache()
	{
		return RESOURCE_CACHE;
	}

	@ApiStatus.Internal
	public static Optional<PModelResource> getModelResource(Identifier model)
	{
		PModelResource exact = RESOURCE_CACHE.get(model);
		if (exact != null)
			return Optional.of(exact);
		return RESOURCE_CACHE.values().stream().filter(resource ->
				PModelCache.getModelLoader(resource.modelLoaderId()).
						map(loader -> loader.modelResourceCandidates(resource.model()).contains(model)).
						orElse(false)).findFirst();
	}

	@ApiStatus.Internal
	public static Optional<PModelResource> getModelResource(Identifier model, ResourceManager resourceManager)
	{
		PModelResource exact = RESOURCE_CACHE.get(model);
		if (exact != null)
			return Optional.of(exact);
		return RESOURCE_CACHE.values().stream().filter(resource ->
				PModelCache.getModelLoader(resource.modelLoaderId()).
						map(loader -> resourceManager.getResource(resource.model()).isEmpty() &&
								loader.modelResourceCandidates(resource.model()).contains(model)).
						orElse(false)).findFirst();
	}

	@ApiStatus.Internal
	public static void clear()
	{
		RESOURCE_CACHE.clear();
		textures = null;
	}

	public static Identifier resolve(Identifier model, String textureReference)
	{
		PModelResource resource = getModelResource(model).
				orElseThrow(() -> new IllegalStateException("No resources registered for model " + model));
		Identifier texture = Optional.ofNullable(resource.textures().get(PTextureReference.normalize(textureReference))).
				orElseThrow(() -> new IllegalStateException("No texture registered for model " + model + ": " + textureReference));
		return spriteId(texture);
	}

	@ApiStatus.Internal
	public static Identifier spriteId(Identifier texture)
	{
		return PLibDatabase.rl(SPRITE_PATH_PREFIX + texture.getNamespace() + "/" + texture.getPath());
	}

	@ApiStatus.Internal
	public static void postEvent()
	{
		PulseLibEvents.RegisterResourceEvent event = new PulseLibEvents.RegisterResourceEvent();
		ModLoader.postEvent(event);
		event.apply(RESOURCE_CACHE);
	}
}
