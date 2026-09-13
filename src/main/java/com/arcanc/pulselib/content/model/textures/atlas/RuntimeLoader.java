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
import com.arcanc.pulselib.util.PTextureCache;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.Optional;

public class RuntimeLoader implements SpriteSource
{
	public static final MapCodec<RuntimeLoader> CODEC = MapCodec.unit(RuntimeLoader ::new);
	
	/**
	 * Performs the run operation.
	 * @param resourceManager the resource manager to use.
	 * @param output the output to use.
	 */
	@Override
	public void run(ResourceManager resourceManager, Output output)
	{
		PTextureCache.getTextureCache().clear();
		PTextureCache.postEvent();
		PTextureCache.getTextureCache().forEach(texture ->
		{
			Identifier fullResourceId = TEXTURE_ID_CONVERTER.idToFile(texture);
			Optional<Resource> resource = resourceManager.getResource(fullResourceId);
			if (resource.isPresent())
				output.add(texture, resource.get());
			else
				PLibDatabase.LOGGER.warn("Missing sprite: {}", fullResourceId);
		});
	}
	
	/**
	 * Performs the codec operation.
	 * @return the value produced by this operation.
	 */
	@Override
	public MapCodec<? extends SpriteSource> codec()
	{
		return CODEC;
	}
}
