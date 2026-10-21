/**
 * @author ArcAnc
 * Created at: 01.04.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.util;


import com.arcanc.pulselib.content.model.PTextureReference;
import com.arcanc.pulselib.content.model.resource.PModelResource;
import com.arcanc.pulselib.plugin.PulsePluginManager;
import com.arcanc.pulselib.util.helpers.PLibRenderHelper;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Caches resource.
 */
public final class PResourceCache
{
	private static final String SPRITE_PATH_PREFIX = "runtime/";
	public static final Identifier ATLAS_LOCATION = PLibDatabase.rl("textures/atlas.png");
	@ApiStatus.Internal
	public static final Identifier ATLAS_FILE_LOCATION = PLibDatabase.rl("atlas");
	private static @Nullable TextureAtlas TEXTURES;

	private static final Map<Identifier, PModelResource> RESOURCE_CACHE = new LinkedHashMap<>();

	private PResourceCache()
	{
	}

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
	
	@ApiStatus.Internal
	public static Map<Identifier, PModelResource> getResourceCache()
	{
		return RESOURCE_CACHE;
	}

	/**
	 * Finds the registration for a canonical model id.
	 *
	 * @param modelId the canonical model id.
	 * @return the matching model resource registration, if any.
	 */
	@ApiStatus.Internal
	public static Optional<PModelResource> getModelResource(Identifier modelId)
	{
		return Optional.ofNullable(RESOURCE_CACHE.get(modelId));
	}

	@ApiStatus.Internal
	public static void clear()
	{
		RESOURCE_CACHE.clear();
		invalidateTextureAtlas();
	}

	/**
	 * Drops the cached atlas reference at the beginning of a resource reload.
	 * AtlasManager owns the atlas and performs its disposal.
	 */
	@ApiStatus.Internal
	public static void invalidateTextureAtlas()
	{
		TEXTURES = null;
	}

	/**
	 * Rebuilds registered model resources before client resource reload begins.
	 * Resource registrations describe stable mod content, so reload listeners and
	 * sprite sources only consume the completed cache.
	 */
	@ApiStatus.Internal
	public static void reloadRegistrations()
	{
		Map<Identifier, PModelResource> staged = new LinkedHashMap<>();
		PulsePluginManager.get().registerModels(staged);
		RESOURCE_CACHE.clear();
		RESOURCE_CACHE.putAll(staged);
		invalidateTextureAtlas();
		PLibDatabase.LOGGER.info("PulseLib resource reload: collected {} model declarations", RESOURCE_CACHE.size());
	}

	/**
	 * Resolves a texture reference in the context of its model.
	 * @param modelId the canonical model id.
	 * @param textureReference the reference stored in the model material.
	 * @return the registered atlas texture id.
	 */
	public static Identifier resolve(Identifier modelId, String textureReference)
	{
		PModelResource resource = getModelResource(modelId).
				orElseThrow(() -> new IllegalStateException("No resources registered for model " + modelId));
		Identifier texture = Optional.ofNullable(resource.textures().get(PTextureReference.normalize(textureReference))).
				orElseThrow(() -> new IllegalStateException("No texture registered for model " + modelId + ": " + textureReference));
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
	
}
