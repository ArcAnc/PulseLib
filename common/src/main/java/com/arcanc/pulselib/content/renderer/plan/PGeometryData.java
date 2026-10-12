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

public final class PGeometryData
{
	private final byte[] vertices;
	private final byte[] indices;
	private final int vertexStride;
	private final int indexCount;
	private final IndexType indexType;

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

	public ByteBuffer vertices()
	{
		return ByteBuffer.wrap(this.vertices).asReadOnlyBuffer();
	}

	public ByteBuffer indices()
	{
		return ByteBuffer.wrap(this.indices).asReadOnlyBuffer();
	}

	public int vertexStride()
	{
		return this.vertexStride;
	}

	public int indexCount()
	{
		return this.indexCount;
	}

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

	public enum IndexType
	{
		UNSIGNED_SHORT(2),
		UNSIGNED_INT(4);

		private final int bytes;

		IndexType(int bytes)
		{
			this.bytes = bytes;
		}

		public int bytes()
		{
			return this.bytes;
		}
	}
}
