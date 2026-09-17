/**
 * @author ArcAnc
 * Created at: 01.04.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.util;


import com.arcanc.pulselib.content.event.PulseLibEvents;
import com.arcanc.pulselib.content.model.resource.PModelResource;
import com.arcanc.pulselib.content.model.PTextureReference;
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

/**
 * Caches resource.
 */
public class PResourceCache
{
	private static final String SPRITE_PATH_PREFIX = "runtime/";
	public static final Identifier ATLAS_LOCATION = PLibDatabase.rl("textures/atlas.png");
	@ApiStatus.Internal
	public static final Identifier ATLAS_FILE_LOCATION = PLibDatabase.rl("atlas");
	private static @Nullable TextureAtlas TEXTURES;
	
	private static final Map<Identifier, PModelResource> RESOURCE_CACHE = new LinkedHashMap<>();
	
	/**
	 * Returns the texture atlas.
	 * @return the value produced by this operation.
	 */
	public static TextureAtlas getTextureAtlas()
	{
		if (TEXTURES == null)
			TEXTURES = PLibRenderHelper.mc().getAtlasManager().getAtlasOrThrow(ATLAS_FILE_LOCATION);
		
		return TEXTURES;
	}
	
	/**
	 * Performs the register operation.
	 * @param modEventBus the mod event bus to use.
	 */
	public static void register(IEventBus modEventBus)
	{
		modEventBus.addListener(PResourceCache :: registerAtlas);
	}
	
	/**
	 * Registers the atlas.
	 * @param event the event to use.
	 */
	private static void registerAtlas(final RegisterTextureAtlasesEvent event)
	{
		event.register(new AtlasManager.AtlasConfig(ATLAS_LOCATION, ATLAS_FILE_LOCATION, false));
		event.addAdditionalMetadata(ATLAS_FILE_LOCATION, PLibSpriteMetadata.TYPE);
	}
	
	@ApiStatus.Internal
	public static Map<Identifier, PModelResource> getResourceCache()
	{
		return RESOURCE_CACHE;
	}

	/**
	 * Finds the registration for a loaded model resource. A loader may accept
	 * more than one on-disk representation of the registered model.
	 *
	 * @param model the loaded model resource id.
	 * @return the matching model resource registration, if any.
	 */
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

	/**
	 * Finds the registration for a loaded model resource, using a fallback
	 * representation only when the preferred resource is absent.
	 *
	 * @param model the loaded model resource id.
	 * @param resourceManager the active resource manager.
	 * @return the matching model resource registration, if any.
	 */
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

	/**
	 * Clears resources from the preceding reload.
	 */
	@ApiStatus.Internal
	public static void clear()
	{
		RESOURCE_CACHE.clear();
		TEXTURES = null;
	}

	/**
	 * Resolves a texture reference in the context of its model.
	 * @param model the model resource id.
	 * @param textureReference the reference stored in the model material.
	 * @return the registered atlas texture id.
	 */
	public static Identifier resolve(Identifier model, String textureReference)
	{
		PModelResource resource = getModelResource(model).
				orElseThrow(() -> new IllegalStateException("No resources registered for model " + model));
		Identifier texture = Optional.ofNullable(resource.textures().get(PTextureReference.normalize(textureReference))).
				orElseThrow(() -> new IllegalStateException("No texture registered for model " + model + ": " + textureReference));
		return spriteId(texture);
	}

	/**
	 * Creates the id used for a texture inside PulseLib's atlas.
	 *
	 * The source texture id may also be present in a vanilla or another mod's
	 * atlas.  An atlas-local id prevents the global sprite lookup from treating
	 * that legitimate reuse as a duplicate registration.
	 *
	 * @param texture the id used to load the PNG resource.
	 * @return the id used to look up the sprite in PulseLib's atlas.
	 */
	@ApiStatus.Internal
	public static Identifier spriteId(Identifier texture)
	{
		return PLibDatabase.rl(SPRITE_PATH_PREFIX + texture.getNamespace() + "/" + texture.getPath());
	}
	
	/**
	 * Performs the post event operation.
	 */
	@ApiStatus.Internal
	public static void postEvent()
	{
		PulseLibEvents.RegisterResourceEvent event = new PulseLibEvents.RegisterResourceEvent();
		ModLoader.postEvent(event);
		event.apply(RESOURCE_CACHE);
	}
}
