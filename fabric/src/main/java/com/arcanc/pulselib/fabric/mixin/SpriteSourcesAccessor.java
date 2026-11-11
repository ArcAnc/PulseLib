/**
 * @author ArcAnc
 * Created at: 12.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.fabric.mixin;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSources;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes Fabric sprite-source type registration in the vanilla atlas source registry.
 */
@Mixin (SpriteSources.class)
public interface SpriteSourcesAccessor
{
	/**
	 * Returns the sprite source type registry used by vanilla atlas definitions.
	 *
	 * @return the sprite source type registry used by vanilla atlas definitions
	 */
	@Accessor ("ID_MAPPER")
	static ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends SpriteSource>> pulselib$getTypes()
	{
		throw new AssertionError();
	}
}
