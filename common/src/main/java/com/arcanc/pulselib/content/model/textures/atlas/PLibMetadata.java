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

public record PLibMetadata (boolean isEmissive, PAlphaMode alphaMode)
{
	public static final String EMISSIVE = "emissive";
	public static final String ALPHA_MODE = "alpha_mode";
	public static final Codec<PLibMetadata> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.optionalFieldOf(EMISSIVE, false).forGetter(PLibMetadata :: isEmissive),
			PAlphaMode.CODEC.optionalFieldOf(ALPHA_MODE, PAlphaMode.AUTO).forGetter(PLibMetadata :: alphaMode)
	).apply(instance, PLibMetadata :: new));
	public static final MetadataSectionType<PLibMetadata> TYPE = MetadataSectionType.fromCodec(PLibDatabase.MOD_ID, CODEC);
}
