/**
 * @author ArcAnc
 * Created at: 13.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.legacy;

import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.mojang.blaze3d.vertex.BufferUploader;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class GlGeometryArena
{
	private static final int PAGE_BYTES = 4 * 1024 * 1024;

	private final Map<PGeometryData, Slice> slices = new Object2ObjectOpenHashMap<>();
	private final List<Page> pages = new ArrayList<>();

	public Slice register(PGeometryData geometry)
	{
		return this.slices.computeIfAbsent(geometry, this :: append);
	}

	public void clear()
	{
		for (Page page : this.pages)
			page.close();
		this.pages.clear();
		this.slices.clear();
	}

	private Slice append(PGeometryData geometry)
	{
		int vertexBytes = geometry.vertices().remaining();
		int indexBytes = geometry.indices().remaining();
		Page page = this.pages.isEmpty() ? null : this.pages.getLast();
		if (page == null || !page.hasSpace(geometry))
		{
			page = new Page(Math.max(PAGE_BYTES, vertexBytes), Math.max(PAGE_BYTES, indexBytes), geometry.vertexStride());
			this.pages.add(page);
		}
		return page.append(geometry);
	}

	public record Slice(int vertexArray, long indexOffset, int indexType, int baseVertex)
	{
		public void bind()
		{
			BufferUploader.invalidate();
			GL30.glBindVertexArray(this.vertexArray);
		}

		public int firstIndex(PGeometryData.IndexType type)
		{
			if (this.indexOffset % type.bytes() != 0)
				throw new IllegalStateException("Geometry slice index offset is not aligned to its index type");
			return Math.toIntExact(this.indexOffset / type.bytes());
		}
	}

	private static final class Page
	{
		private final int vertexBuffer;
		private final int indexBuffer;
		private final int vertexArray;
		private final int vertexCapacity;
		private final int indexCapacity;
		private int vertices;
		private int indices;

		private Page(int vertexCapacity, int indexCapacity, int stride)
		{
			this.vertexCapacity = vertexCapacity;
			this.indexCapacity = indexCapacity;
			this.vertexBuffer = GL15.glGenBuffers();
			this.indexBuffer = GL15.glGenBuffers();
			this.vertexArray = GL30.glGenVertexArrays();
			GL30.glBindVertexArray(this.vertexArray);
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vertexBuffer);
			GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexCapacity, GL15.GL_STATIC_DRAW);
			GL20.glEnableVertexAttribArray(0);
			GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, stride, 0L);
			GL20.glEnableVertexAttribArray(1);
			GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, stride, 12L);
			GL20.glEnableVertexAttribArray(2);
			GL20.glVertexAttribPointer(2, 3, GL11.GL_BYTE, true, stride, 20L);
			GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, this.indexBuffer);
			GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indexCapacity, GL15.GL_STATIC_DRAW);
			GL30.glBindVertexArray(0);
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
			GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
		}

		private boolean hasSpace(PGeometryData geometry)
		{
			int indexOffset = align(this.indices, geometry.indexType().bytes());
			return geometry.vertices().remaining() <= this.vertexCapacity - this.vertices &&
					geometry.indices().remaining() <= this.indexCapacity - indexOffset;
		}

		private Slice append(PGeometryData geometry)
		{
			int indexOffset = align(this.indices, geometry.indexType().bytes());
			int baseVertex = this.vertices / geometry.vertexStride();
			ByteBuffer vertexData = geometry.vertices();
			ByteBuffer indexData = geometry.indices();
			int vertexBytes = vertexData.remaining();
			int indexBytes = indexData.remaining();
			upload(this.vertexBuffer, this.vertices, vertexData);
			upload(this.indexBuffer, indexOffset, indexData);
			this.vertices += vertexBytes;
			this.indices = indexOffset + indexBytes;
			return new Slice(this.vertexArray, indexOffset, switch (geometry.indexType())
			{
				case UNSIGNED_SHORT -> GL11.GL_UNSIGNED_SHORT;
				case UNSIGNED_INT -> GL11.GL_UNSIGNED_INT;
			}, baseVertex);
		}

		private static int align(int value, int alignment)
		{
			return (value + alignment - 1) & -alignment;
		}

		private void close()
		{
			GL30.glDeleteVertexArrays(this.vertexArray);
			GL15.glDeleteBuffers(this.vertexBuffer);
			GL15.glDeleteBuffers(this.indexBuffer);
		}

		private static void upload(int buffer, int offset, ByteBuffer source)
		{
			ByteBuffer packed = MemoryUtil.memAlloc(source.remaining());
			try
			{
				packed.put(source.duplicate()).flip();
				GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
				GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, offset, packed);
			}
			finally
			{
				MemoryUtil.memFree(packed);
			}
		}
	}
}
