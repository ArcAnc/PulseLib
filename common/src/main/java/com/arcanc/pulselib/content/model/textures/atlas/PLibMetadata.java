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
 * Texture metadata controlling emissive shading and alpha-mode overrides.
 *
 * @param isEmissive the is emissive
 * @param alphaMode the material alpha handling policy
 */
public record PLibMetadata (boolean isEmissive, PAlphaMode alphaMode)
{
	/**
	 * Texture metadata key controlling emissive shading.
	 */
	public static final String EMISSIVE = "emissive";
	/**
	 * Texture metadata key overriding alpha classification.
	 */
	public static final String ALPHA_MODE = "alpha_mode";
	/**
	 * Serialization codec for lib metadata.
	 */
	public static final Codec<PLibMetadata> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.optionalFieldOf(EMISSIVE, false).forGetter(PLibMetadata :: isEmissive),
			PAlphaMode.CODEC.optionalFieldOf(ALPHA_MODE, PAlphaMode.AUTO).forGetter(PLibMetadata :: alphaMode)
	).apply(instance, PLibMetadata :: new));
	/**
	 * Texture metadata section type used to decode this configuration.
	 */
	public static final MetadataSectionType<PLibMetadata> TYPE = MetadataSectionType.fromCodec(PLibDatabase.MOD_ID, CODEC);
}
