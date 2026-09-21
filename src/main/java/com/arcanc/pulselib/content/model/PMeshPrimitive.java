package com.arcanc.pulselib.content.model;

import de.javagl.jgltf.model.GltfConstants;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.Objects;

/** Immutable geometry and material for one glTF mesh primitive. */
public record PMeshPrimitive(
		int vertexCount,
		FloatBuffer positions,
		FloatBuffer normals,
		FloatBuffer uvs,
		int indicesCount,
		ByteBuffer indices,
		int glIndexType,
		PMaterial material)
{
	public static PMeshPrimitive merge(List<PMeshPrimitive> primitives)
	{
		if (primitives.isEmpty())
			throw new IllegalArgumentException("Cannot merge an empty primitive list");
		if (primitives.size() == 1)
			return primitives.getFirst();

		PMaterial material = Objects.requireNonNull(primitives.getFirst().material());
		int vertexCount = 0;
		int indexCount = 0;
		for (PMeshPrimitive primitive : primitives)
		{
			if (!material.equals(primitive.material()))
				throw new IllegalArgumentException("Cannot merge primitives with different materials");
			vertexCount = Math.addExact(vertexCount, primitive.vertexCount());
			indexCount = Math.addExact(indexCount, primitive.indicesCount());
		}

		FloatBuffer positions = floatBuffer(vertexCount * 3);
		FloatBuffer normals = floatBuffer(vertexCount * 3);
		FloatBuffer uvs = floatBuffer(vertexCount * 2);
		int indexType = vertexCount <= 0xFFFF ? GltfConstants.GL_UNSIGNED_SHORT : GltfConstants.GL_UNSIGNED_INT;
		ByteBuffer indices = ByteBuffer.allocateDirect(indexCount * (indexType == GltfConstants.GL_UNSIGNED_SHORT ? Short.BYTES : Integer.BYTES)).
				order(ByteOrder.nativeOrder());
		int vertexOffset = 0;
		for (PMeshPrimitive primitive : primitives)
		{
			append(primitive.positions(), positions, primitive.vertexCount() * 3);
			append(primitive.normals(), normals, primitive.vertexCount() * 3);
			append(primitive.uvs(), uvs, primitive.vertexCount() * 2);
			for (int index = 0; index < primitive.indicesCount(); index++)
			{
				int value = vertexOffset + indexAt(primitive, index);
				if (indexType == GltfConstants.GL_UNSIGNED_SHORT)
					indices.putShort((short)value);
				else
					indices.putInt(value);
			}
			vertexOffset += primitive.vertexCount();
		}
		positions.flip();
		normals.flip();
		uvs.flip();
		indices.flip();
		return new PMeshPrimitive(vertexCount, positions, normals, uvs, indexCount, indices, indexType, material);
	}

	private static FloatBuffer floatBuffer(int size)
	{
		return ByteBuffer.allocateDirect(Math.multiplyExact(size, Float.BYTES)).order(ByteOrder.nativeOrder()).asFloatBuffer();
	}

	private static void append(FloatBuffer source, FloatBuffer target, int count)
	{
		for (int index = 0; index < count; index++)
			target.put(source.get(index));
	}

	private static int indexAt(PMeshPrimitive primitive, int index)
	{
		return switch (primitive.glIndexType())
		{
			case GltfConstants.GL_UNSIGNED_BYTE -> primitive.indices().get(index) & 0xFF;
			case GltfConstants.GL_UNSIGNED_SHORT -> primitive.indices().getShort(index * Short.BYTES) & 0xFFFF;
			case GltfConstants.GL_UNSIGNED_INT -> primitive.indices().getInt(index * Integer.BYTES);
			default -> throw new IllegalArgumentException("Unsupported index type: " + primitive.glIndexType());
		};
	}
}
