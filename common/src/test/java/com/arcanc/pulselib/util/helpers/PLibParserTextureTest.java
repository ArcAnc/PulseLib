/**
 * @author ArcAnc
 * Created at: 26.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.util.helpers;

import de.javagl.jgltf.model.impl.DefaultBufferViewModel;
import de.javagl.jgltf.model.impl.DefaultImageModel;
import de.javagl.jgltf.model.impl.DefaultTextureModel;
import de.javagl.jgltf.model.v2.MaterialModelV2;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PLibParserTextureTest
{
	@Test
	void embeddedImageUsesItsRegisteredLogicalName()
	{
		var image = new DefaultImageModel();
		image.setName("0");
		image.setUri("data:image/png;base64,aGVsbG8=");
		var texture = new DefaultTextureModel();
		texture.setImageModel(image);
		var material = new MaterialModelV2();
		material.setBaseColorTexture(texture);
		assertEquals("0", PLibParserHelper.extractTextureName(material));
		texture.setName("skin.v2.png");
		assertEquals("skin.v2", PLibParserHelper.extractTextureName(material));
	}

	@Test
	void glbBufferViewAndUnnamedImagesKeepCanonicalFallbacks()
	{
		var image = new DefaultImageModel();
		var view = new DefaultBufferViewModel(null);
		view.setName("embedded.png");
		image.setBufferViewModel(view);
		var texture = new DefaultTextureModel();
		texture.setImageModel(image);
		var material = new MaterialModelV2();
		material.setBaseColorTexture(texture);
		assertEquals("embedded", PLibParserHelper.extractTextureName(material));
		view.setName(null);
		assertEquals("default_texture", PLibParserHelper.extractTextureName(material));
	}

	@Test
	void textureWithoutAnImageKeepsItsNameAndAbsentTextureIsEmpty()
	{
		var material = new MaterialModelV2();
		assertEquals("", PLibParserHelper.extractTextureName(material));
		var texture = new DefaultTextureModel();
		texture.setName("standalone.png");
		material.setBaseColorTexture(texture);
		assertEquals("standalone", PLibParserHelper.extractTextureName(material));
	}
}
