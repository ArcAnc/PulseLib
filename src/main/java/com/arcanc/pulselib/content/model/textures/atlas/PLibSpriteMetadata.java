/**
 * @author ArcAnc
 * Created at: 26.06.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.textures.atlas;


import com.arcanc.pulselib.content.model.textures.PAlphaMode;
import com.arcanc.pulselib.util.PLibDatabase;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.packs.metadata.MetadataSectionType;

/**
 * Immutable value object representing lib sprite metadata.
 */
public record PLibSpriteMetadata (boolean emissive, PAlphaMode alphaMode)
{
	public static final Codec<PLibSpriteMetadata> CODEC = RecordCodecBuilder.create(instance -> instance.
			group(
					Codec.BOOL.optionalFieldOf("emissive", false).forGetter(PLibSpriteMetadata :: emissive),
					PAlphaMode.CODEC.optionalFieldOf("alpha_mode", PAlphaMode.AUTO).forGetter(PLibSpriteMetadata :: alphaMode)
		).apply(instance, PLibSpriteMetadata :: new));
	
	public static final MetadataSectionType<PLibSpriteMetadata> TYPE =
			new MetadataSectionType<>(PLibDatabase.MOD_ID, CODEC);
	
}
