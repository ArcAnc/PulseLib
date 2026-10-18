/**
 * @author ArcAnc
 * Created at: 23.02.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.util.helpers;


import de.javagl.jgltf.model.*;
import de.javagl.jgltf.model.v2.MaterialModelV2;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Helpers for reading glTF accessor buffers and source material texture references.
 */
public class PLibParserHelper
{
	/**
	 * Reads the supplied glTF accessor as float values.
	 *
	 * @param accessor the glTF accessor whose packed values are read
	 * @return the packed floating-point data
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
	 * Reads the supplied glTF accessor as packed bytes.
	 *
	 * @param accessor the glTF accessor whose packed values are read
	 * @return the packed binary data
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
	
	/**
	 * Extracts the texture reference used by a source glTF material.
	 *
	 * @param material the material
	 * @return the material's source texture reference
	 */
	public static String extractTextureName(MaterialModel material)
	{
		if (!(material instanceof MaterialModelV2 mat))
			return "";
		
		TextureModel texture = mat.getBaseColorTexture();
		if (texture == null)
			return "";
		
		String name = texture.getName();
		if (name == null && texture.getImageModel() != null)
			name = texture.getImageModel().getName();
		
		if (name == null &&
				texture.getImageModel() != null &&
				texture.getImageModel().getBufferViewModel() != null)
			name = texture.getImageModel().getBufferViewModel().getName();
		
		if (name == null)
			return "default_texture";
		
		int lastDot = name.lastIndexOf('.');
		return (lastDot > 0) ? name.substring(0, lastDot) : name;
	}
}
