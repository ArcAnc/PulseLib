/**
 * @author ArcAnc
 * Created at: 08.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.deformer;

import com.arcanc.pulselib.content.model.PMesh;
import de.javagl.jgltf.model.GltfConstants;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public final class PMeshTessellator
{
	public static final int MAX_SUBDIVISION_LEVEL = 4;

	/**
	 * Creates an instance of the enclosing type.
	 */
	private PMeshTessellator()
	{
	}
	
	/**
	 * Performs the subdivide operation.
	 * @param source the source to use.
	 * @param level the level to use.
	 * @return the value produced by this operation.
	 */
	public static PMesh subdivide(PMesh source, int level)
	{
		if (level == 0)
			return source;
		if (level < 0 || level > MAX_SUBDIVISION_LEVEL)
			throw new IllegalArgumentException("Subdivision level must be in [0, " + MAX_SUBDIVISION_LEVEL + "]: " + level);
		if (source.indicesCount() % 3 != 0)
			throw new IllegalArgumentException("Only triangle meshes can be subdivided");

		int segments = 1 << level;
		int verticesPerTriangle = (segments + 1) * (segments + 2) / 2;
		int sourceTriangleCount = source.indicesCount() / 3;
		long expectedVertexCount = (long)sourceTriangleCount * verticesPerTriangle;
		if (expectedVertexCount > Integer.MAX_VALUE)
			throw new IllegalArgumentException("Subdivided mesh is too large: " + expectedVertexCount + " vertices");

		List<Float> positions = new ArrayList<>((int)expectedVertexCount * 3);
		List<Float> normals = new ArrayList<>((int)expectedVertexCount * 3);
		List<Float> uvs = new ArrayList<>((int)expectedVertexCount * 2);
		List<Integer> indices = new ArrayList<>(source.indicesCount() * segments * segments);

		for (int triangle = 0; triangle < sourceTriangleCount; triangle++)
		{
			int a = indexAt(source, triangle * 3);
			int b = indexAt(source, triangle * 3 + 1);
			int c = indexAt(source, triangle * 3 + 2);
			validateVertexIndex(source, a);
			validateVertexIndex(source, b);
			validateVertexIndex(source, c);

			int base = positions.size() / 3;
			for (int i = 0; i <= segments; i++)
				for (int j = 0; j <= segments - i; j++)
					appendVertex(source, a, b, c, (float)i / segments, (float)j / segments, positions, normals, uvs);

			for (int i = 0; i < segments; i++)
				for (int j = 0; j < segments - i; j++)
				{
					indices.add(base + gridIndex(i, j, segments));
					indices.add(base + gridIndex(i + 1, j, segments));
					indices.add(base + gridIndex(i, j + 1, segments));
					if (i + j < segments - 1)
					{
						indices.add(base + gridIndex(i + 1, j, segments));
						indices.add(base + gridIndex(i + 1, j + 1, segments));
						indices.add(base + gridIndex(i, j + 1, segments));
					}
				}
		}

		int vertexCount = positions.size() / 3;
		int indexType = vertexCount <= 0xFFFF ? GltfConstants.GL_UNSIGNED_SHORT : GltfConstants.GL_UNSIGNED_INT;
		return new PMesh(source.uuid(), vertexCount, floatBuffer(positions), floatBuffer(normals), floatBuffer(uvs),
				indices.size(), indexBuffer(indices, indexType), indexType, source.texture());
	}

	/**
	 * Performs the grid index operation.
	 * @param i the i to use.
	 * @param j the j to use.
	 * @param segments the segments to use.
	 * @return the value produced by this operation.
	 */
	private static int gridIndex(int i, int j, int segments)
	{
		return i * (segments + 1) - i * (i - 1) / 2 + j;
	}

	/**
	 * Performs the append vertex operation.
	 * @param source the source to use.
	 * @param a the a to use.
	 * @param b the b to use.
	 * @param c the c to use.
	 * @param bWeight the b weight to use.
	 * @param cWeight the c weight to use.
	 * @param positions the positions to use.
	 * @param normals the normals to use.
	 * @param uvs the uvs to use.
	 */
	private static void appendVertex(PMesh source, int a, int b, int c, float bWeight, float cWeight,
									 List<Float> positions, List<Float> normals, List<Float> uvs)
	{
		float aWeight = 1.0f - bWeight - cWeight;
		appendInterpolated(source.positions(), a, b, c, aWeight, bWeight, cWeight, positions, 3, false);
		appendInterpolated(source.normals(), a, b, c, aWeight, bWeight, cWeight, normals, 3, true);
		appendInterpolated(source.uvs(), a, b, c, aWeight, bWeight, cWeight, uvs, 2, false);
	}

	/**
	 * Performs the append interpolated operation.
	 * @param source the source to use.
	 * @param a the a to use.
	 * @param b the b to use.
	 * @param c the c to use.
	 * @param aWeight the a weight to use.
	 * @param bWeight the b weight to use.
	 * @param cWeight the c weight to use.
	 * @param target the target to use.
	 * @param components the components to use.
	 * @param normalize the normalize to use.
	 */
	private static void appendInterpolated(FloatBuffer source, int a, int b, int c, float aWeight, float bWeight,
									 float cWeight, List<Float> target, int components, boolean normalize)
	{
		float x = source.get(a * components) * aWeight + source.get(b * components) * bWeight + source.get(c * components) * cWeight;
		float y = source.get(a * components + 1) * aWeight + source.get(b * components + 1) * bWeight + source.get(c * components + 1) * cWeight;
		float z = components == 3 ? source.get(a * components + 2) * aWeight + source.get(b * components + 2) * bWeight + source.get(c * components + 2) * cWeight : 0.0f;
		if (normalize)
		{
			float length = (float)Math.sqrt(x * x + y * y + z * z);
			if (length > 1.0e-8f)
			{
				x /= length;
				y /= length;
				z /= length;
			}
		}
		target.add(x);
		target.add(y);
		if (components == 3)
			target.add(z);
	}

	/**
	 * Performs the index at operation.
	 * @param source the source to use.
	 * @param index the index to use.
	 * @return the value produced by this operation.
	 */
	private static int indexAt(PMesh source, int index)
	{
		ByteBuffer indices = source.indices().duplicate().order(source.indices().order());
		return switch (source.glIndexType())
		{
			case GltfConstants.GL_UNSIGNED_BYTE -> indices.get(index) & 0xFF;
			case GltfConstants.GL_UNSIGNED_SHORT -> indices.getShort(index * Short.BYTES) & 0xFFFF;
			case GltfConstants.GL_UNSIGNED_INT -> indices.getInt(index * Integer.BYTES);
			default -> throw new IllegalArgumentException("Unsupported GLTF index type: " + source.glIndexType());
		};
	}

	/**
	 * Validates the vertex index.
	 * @param source the source to use.
	 * @param index the index to use.
	 */
	private static void validateVertexIndex(PMesh source, int index)
	{
		if (index < 0 || index >= source.vertexCount())
			throw new IllegalArgumentException("Mesh index is outside its vertex array: " + index);
	}

	/**
	 * Performs the float buffer operation.
	 * @param values the values to use.
	 * @return the value produced by this operation.
	 */
	private static FloatBuffer floatBuffer(List<Float> values)
	{
		FloatBuffer result = FloatBuffer.allocate(values.size());
		for (float value : values)
			result.put(value);
		return result.flip();
	}

	/**
	 * Performs the index buffer operation.
	 * @param values the values to use.
	 * @param indexType the index type to use.
	 * @return the value produced by this operation.
	 */
	private static ByteBuffer indexBuffer(List<Integer> values, int indexType)
	{
		int size = indexType == GltfConstants.GL_UNSIGNED_SHORT ? Short.BYTES : Integer.BYTES;
		ByteBuffer result = ByteBuffer.allocateDirect(values.size() * size).order(ByteOrder.nativeOrder());
		for (int value : values)
			if (indexType == GltfConstants.GL_UNSIGNED_SHORT)
				result.putShort((short)value);
			else
				result.putInt(value);
		return result.flip();
	}
}
