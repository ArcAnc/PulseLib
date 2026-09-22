package com.arcanc.pulselib.content.model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.Objects;

/** Geometry and material of one model primitive. */
public record PMeshPrimitive(int vertexCount,
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
		int vertices = 0;
		int indices = 0;
		for (PMeshPrimitive primitive : primitives)
		{
			if (!material.equals(primitive.material()))
				throw new IllegalArgumentException("Cannot merge primitives with different materials");
			vertices = Math.addExact(vertices, primitive.vertexCount());
			indices = Math.addExact(indices, primitive.indicesCount());
		}
		FloatBuffer positions = floats(vertices * 3);
		FloatBuffer normals = floats(vertices * 3);
		FloatBuffer uvs = floats(vertices * 2);
		int indexType = vertices <= 0xffff ? 5123 : 5125;
		ByteBuffer indexBuffer = ByteBuffer.allocateDirect(indices * (indexType == 5123 ? Short.BYTES : Integer.BYTES))
				.order(ByteOrder.nativeOrder());
		int offset = 0;
		for (PMeshPrimitive primitive : primitives)
		{
			copy(primitive.positions(), positions, primitive.vertexCount() * 3);
			copy(primitive.normals(), normals, primitive.vertexCount() * 3);
			copy(primitive.uvs(), uvs, primitive.vertexCount() * 2);
			for (int index = 0; index < primitive.indicesCount(); index++)
			{
				int value = offset + indexAt(primitive, index);
				if (indexType == 5123)
					indexBuffer.putShort((short)value);
				else
					indexBuffer.putInt(value);
			}
			offset += primitive.vertexCount();
		}
		positions.flip();
		normals.flip();
		uvs.flip();
		indexBuffer.flip();
		return new PMeshPrimitive(vertices, positions, normals, uvs, indices, indexBuffer, indexType, material);
	}

	private static FloatBuffer floats(int size)
	{
		return ByteBuffer.allocateDirect(size * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
	}

	private static void copy(FloatBuffer source, FloatBuffer target, int count)
	{
		for (int index = 0; index < count; index++)
			target.put(source.get(index));
	}

	private static int indexAt(PMeshPrimitive primitive, int index)
	{
		return switch (primitive.glIndexType())
		{
			case 5121 -> primitive.indices().get(index) & 0xff;
			case 5123 -> primitive.indices().getShort(index * Short.BYTES) & 0xffff;
			case 5125 -> primitive.indices().getInt(index * Integer.BYTES);
			default -> throw new IllegalArgumentException("Unsupported index type: " + primitive.glIndexType());
		};
	}
}
