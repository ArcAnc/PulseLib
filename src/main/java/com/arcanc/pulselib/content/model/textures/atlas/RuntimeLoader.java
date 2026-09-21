/**
 * @author ArcAnc
 * Created at: 01.04.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.textures.atlas;


import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PResourceCache;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class RuntimeLoader implements SpriteSource
{
	public static final MapCodec<RuntimeLoader> CODEC = MapCodec.unit(RuntimeLoader ::new);
	
	@Override
	public void run(ResourceManager resourceManager, Output output)
	{
		PResourceCache.clear();
		PResourceCache.postEvent();
		Set<Identifier> textures = PResourceCache.getResourceCache().values().stream().
				flatMap(resource -> resource.textures().values().stream()).
				collect(Collectors.toSet());
		textures.forEach(texture ->
		{
			Identifier fullResourceId = TEXTURE_ID_CONVERTER.idToFile(texture);
			Optional<Resource> resource = resourceManager.getResource(fullResourceId);
			if (resource.isPresent())
				output.add(PResourceCache.spriteId(texture), resource.get());
			else
				PLibDatabase.LOGGER.warn("Missing sprite: {}", fullResourceId);
		});
	}
	
	@Override
	public MapCodec<? extends SpriteSource> codec()
	{
		return CODEC;
	}
}
