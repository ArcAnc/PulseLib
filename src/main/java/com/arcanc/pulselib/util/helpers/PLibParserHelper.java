/**
 * @author ArcAnc
 * Created at: 23.02.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.util.helpers;


import de.javagl.jgltf.model.AccessorData;
import de.javagl.jgltf.model.AccessorModel;
import de.javagl.jgltf.model.GltfConstants;
import de.javagl.jgltf.model.ImageModel;
import de.javagl.jgltf.model.MaterialModel;
import de.javagl.jgltf.model.TextureModel;
import de.javagl.jgltf.model.v2.MaterialModelV2;

import java.nio.*;

/**
 * Provides support for lib parser helper.
 */
public class PLibParserHelper
{
	/**
	 * Extracts the texture name.
	 * @param material the material to use.
	 * @return the value produced by this operation.
	 */
	public static String extractTextureName(MaterialModel material)
	{
		if (!(material instanceof MaterialModelV2 v2))
			return "";
		TextureModel texture = v2.getBaseColorTexture();
		if (texture == null)
			return "";
		ImageModel image = texture.getImageModel();
		if (image != null)
		{
			String uri = image.getUri();
			if (uri != null && !uri.isBlank())
				return uri;
			String imageName = image.getName();
			if (imageName != null && !imageName.isBlank())
				return imageName;
		}
		String textureName = texture.getName();
		return textureName == null ? "" : textureName;
	}

	/**
	 * Returns the float buffer.
	 * @param accessor the accessor to use.
	 * @return the value produced by this operation.
	 */
	public static FloatBuffer getFloatBuffer(AccessorModel accessor)
	{
		if (accessor == null)
			return FloatBuffer.allocate(0);
		
		if (accessor.getComponentType() != GltfConstants.GL_FLOAT)
			throw new IllegalArgumentException("Accessor has wrong componentType. Type: " + accessor.getComponentType());
		
		AccessorData data = accessor.getAccessorData();
		ByteBuffer bb = data.createByteBuffer();
		bb.order(ByteOrder.LITTLE_ENDIAN);
		FloatBuffer floatBuffer = bb.asFloatBuffer();
		
		int componentCount = accessor.getElementType().getNumComponents();
		int totalFloats = accessor.getCount() * componentCount;
		
		floatBuffer.limit(totalFloats);
		
		return floatBuffer;
	}
	
	/**
	 * Returns the byte buffer.
	 * @param accessor the accessor to use.
	 * @return the value produced by this operation.
	 */
	public static ByteBuffer getByteBuffer(AccessorModel accessor)
	{
		if (accessor == null)
			return null;
		
		AccessorData data = accessor.getAccessorData();
		
		ByteBuffer buffer = data.createByteBuffer();
		
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.rewind();
		
		int componentSize = switch (accessor.getComponentType())
		{
			case GltfConstants.GL_UNSIGNED_BYTE -> 1;
			case GltfConstants.GL_UNSIGNED_SHORT -> 2;
			case GltfConstants.GL_UNSIGNED_INT -> 4;
			default -> throw new IllegalStateException("Unsupported component type: " + accessor.getComponentType());
		};
		
		int componentCount =
				accessor.getElementType().getNumComponents();
		
		int expectedSize = accessor.getCount() * componentCount * componentSize;
		
		buffer.limit(expectedSize);
		
		return buffer;
	}
}
