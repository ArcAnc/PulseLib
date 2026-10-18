/**
 * @author ArcAnc
 * Created at: 25.03.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.textures.atlas;


import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PResourceCache;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Atlas sprite source that discovers textures referenced by registered PulseLib models.
 */
public class RuntimeLoader implements SpriteSource
{
	/**
	 * Serialization codec for runtime loader.
	 */
	public static final MapCodec<RuntimeLoader> CODEC = MapCodec.unit(RuntimeLoader ::new);
	/**
	 * Registered sprite source type for runtime model texture discovery.
	 */
	public static final SpriteSourceType TYPE = new SpriteSourceType(CODEC);
	
	/**
	 * Adds the texture resources required by registered models to the atlas output.
	 *
	 * @param resourceManager the resource manager used to locate source assets
	 * @param output the output
	 */
	@Override
	public void run(ResourceManager resourceManager, Output output)
	{
		PResourceCache.invalidateTextureAtlas();
		List<MetadataSectionSerializer<?>> metadataSections = new ArrayList<>(SpriteLoader.DEFAULT_METADATA_SECTIONS);
		metadataSections.add(PLibMetadata.TYPE);
		SpriteResourceLoader spriteResourceLoader = SpriteResourceLoader.create(metadataSections);
		
		Set<ResourceLocation> textures = PResourceCache.getResourceCache().values().stream()
				.flatMap(resource -> resource.textures().values().stream())
				.collect(Collectors.toSet());
		textures.forEach(texture ->
		{
			ResourceLocation resourcelocation = TEXTURE_ID_CONVERTER.idToFile(texture);
			Optional<Resource> optional = resourceManager.getResource(resourcelocation);
			if (optional.isPresent())
			{
				Resource resource = optional.get();
				ResourceLocation sprite = PResourceCache.spriteId(texture);
				output.add(sprite, loader -> spriteResourceLoader.loadSprite(sprite, resource));
			}
			else
				PLibDatabase.LOGGER.warn("Missing sprite: {}", resourcelocation);
		});
	}
	
	/**
	 * Returns the sprite source type registered for this runtime loader.
	 *
	 * @return the sprite source type registered for this runtime loader
	 */
	@Override
	public SpriteSourceType type()
	{
		return TYPE;
	}
}
