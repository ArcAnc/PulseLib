/**
 * @author ArcAnc
 * Created at: 11.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.util.helpers;

import com.arcanc.pulselib.content.model.textures.PAlphaMode;
import com.arcanc.pulselib.content.model.textures.atlas.PLibMetadata;
import com.arcanc.pulselib.content.model.textures.atlas.PLibSpriteMetadata;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.server.packs.resources.ResourceMetadata;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PLibMetadataTest
{
	@Test
	void defaultsKeepAutoAlphaAndNonEmissiveMaterial()
	{
		var metadata = PLibMetadata.CODEC.parse(JsonOps.INSTANCE, new JsonObject()).getOrThrow();
		assertFalse(metadata.isEmissive());
		assertEquals(PAlphaMode.AUTO, metadata.alphaMode());
		assertEquals("emissive", PLibMetadata.EMISSIVE);
		assertEquals("alpha_mode", PLibMetadata.ALPHA_MODE);
	}

	@Test
	void canonicalAndNativeCodecsAgreeForEveryExplicitMode()
	{
		for (var mode : PAlphaMode.values())
			for (boolean emissive : new boolean[]{false, true})
			{
				var original = new PLibMetadata(emissive, mode);
				var json = PLibMetadata.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
				assertEquals(original, PLibMetadata.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
				assertEquals(new PLibSpriteMetadata(emissive, mode), PLibSpriteMetadata.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
			}
	}

	@Test
	void oneResourceSectionDecodesBothTypedMetadataViews() throws Exception
	{
		var json = "{\"pulselib\":{\"emissive\":true,\"alpha_mode\":\"cutout\"}}";
		var resource = ResourceMetadata.fromJsonStream(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
		assertEquals(new PLibMetadata(true, PAlphaMode.CUTOUT), resource.getSection(PLibMetadata.TYPE).orElseThrow());
		assertEquals(new PLibSpriteMetadata(true, PAlphaMode.CUTOUT), resource.getSection(PLibSpriteMetadata.TYPE).orElseThrow());
		assertEquals(2, resource.getTypedSections(java.util.Set.of(PLibMetadata.TYPE, PLibSpriteMetadata.TYPE)).size());
	}

	@Test
	void invalidAlphaModeIsRejected()
	{
		var json = new JsonObject();
		json.addProperty(PLibMetadata.ALPHA_MODE, "invalid");
		assertTrue(PLibMetadata.CODEC.parse(JsonOps.INSTANCE, json).isError());
	}
}
