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

public class RuntimeLoader implements SpriteSource
{
	public static final MapCodec<RuntimeLoader> CODEC = MapCodec.unit(RuntimeLoader ::new);
	public static final SpriteSourceType TYPE = new SpriteSourceType(CODEC);
	
	@Override
	public void run(ResourceManager resourceManager, Output output)
	{
		PResourceCache.clear();
		PResourceCache.postEvent();
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
	
	@Override
	public SpriteSourceType type()
	{
		return TYPE;
	}
}
