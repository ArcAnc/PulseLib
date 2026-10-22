/**
 * @author ArcAnc
 * Created at: 13.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.plan;

import java.nio.ByteBuffer;

/**
 * Immutable renderer-independent indexed geometry with explicit vertex stride and index width.
 */
public final class PGeometryData
{
	private final byte[] vertices;
	private final byte[] indices;
	private final int vertexStride;
	private final int indexCount;
	private final IndexType indexType;

	/**
	 * Copies vertex and index bytes and validates the declared geometry layout.
	 *
	 * @param vertices the vertices
	 * @param indices the indices
	 * @param vertexStride the number of bytes occupied by one vertex
	 * @param indexCount the number of indices to draw
	 * @param indexType the unsigned index element representation
	 */
	public PGeometryData(ByteBuffer vertices, ByteBuffer indices, int vertexStride, int indexCount, IndexType indexType)
	{
		if (vertexStride <= 0 || indexCount < 0)
			throw new IllegalArgumentException("Invalid PulseLib geometry layout");
		this.vertices = copy(vertices);
		this.indices = copy(indices);
		this.vertexStride = vertexStride;
		this.indexCount = indexCount;
		this.indexType = indexType;
		if (this.indices.length < (long)indexCount * indexType.bytes())
			throw new IllegalArgumentException("Index data is shorter than its declared count");
	}

	/**
	 * Returns a view of the immutable packed vertex bytes.
	 *
	 * @return a view of the immutable packed vertex bytes
	 */
	public ByteBuffer vertices()
	{
		return ByteBuffer.wrap(this.vertices).asReadOnlyBuffer();
	}

	/**
	 * Returns a view of the immutable packed index bytes.
	 *
	 * @return a view of the immutable packed index bytes
	 */
	public ByteBuffer indices()
	{
		return ByteBuffer.wrap(this.indices).asReadOnlyBuffer();
	}

	/**
	 * Returns the number of bytes occupied by one vertex.
	 *
	 * @return the number of bytes occupied by one vertex
	 */
	public int vertexStride()
	{
		return this.vertexStride;
	}

	/**
	 * Returns the number of indices to draw.
	 *
	 * @return the number of indices to draw
	 */
	public int indexCount()
	{
		return this.indexCount;
	}

	/**
	 * Returns the unsigned index element representation.
	 *
	 * @return the unsigned index element representation
	 */
	public IndexType indexType()
	{
		return this.indexType;
	}

	private static byte[] copy(ByteBuffer source)
	{
		ByteBuffer copy = source.duplicate();
		byte[] result = new byte[copy.remaining()];
		copy.get(result);
		return result;
	}

	/**
	 * Unsigned index element width used by portable geometry data.
	 */
	public enum IndexType
	{
		/**
		 * Stores each vertex index in two bytes.
		 */
		UNSIGNED_SHORT(2),
		/**
		 * Stores each vertex index in four bytes.
		 */
		UNSIGNED_INT(4);

		private final int bytes;

		/**
		 * Creates an index type from the supplied configuration.
		 *
		 * @param bytes the bytes
		 */
		IndexType(int bytes)
		{
			this.bytes = bytes;
		}

		/**
		 * Returns the bytes.
		 *
		 * @return the bytes
		 */
		public int bytes()
		{
			return this.bytes;
		}
	}
}
