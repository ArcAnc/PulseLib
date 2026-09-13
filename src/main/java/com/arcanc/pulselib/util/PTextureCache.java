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
import com.arcanc.pulselib.content.model.textures.atlas.PLibSpriteMetadata;
import com.arcanc.pulselib.util.helpers.PLibRenderHelper;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.client.event.RegisterTextureAtlasesEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class PTextureCache
{
	public static final Identifier ATLAS_LOCATION = PLibDatabase.rl("textures/atlas.png");
	@ApiStatus.Internal
	public static final Identifier ATLAS_FILE_LOCATION = PLibDatabase.rl("atlas");
	private static @Nullable TextureAtlas TEXTURES;
	
	private static final Set<Identifier> TEXTURE_CACHE = new HashSet<>();
	
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
		modEventBus.addListener(PTextureCache :: registerAtlas);
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
	
	/**
	 * Returns the texture cache.
	 * @return the value produced by this operation.
	 */
	@ApiStatus.Internal
	public static Set<Identifier> getTextureCache()
	{
		return TEXTURE_CACHE;
	}
	
	/**
	 * Performs the post event operation.
	 */
	@ApiStatus.Internal
	public static void postEvent()
	{
		ModLoader.postEvent(new PulseLibEvents.RegisterTextureEvent(TEXTURE_CACHE));
	}
}
