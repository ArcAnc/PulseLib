/**
 * @author ArcAnc
 * Created at: 14.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.gl;

import com.arcanc.pulselib.content.model.baked.PBakedMesh;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.arcanc.pulselib.util.PRenderTypes;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages gl geometry allocations.
 */
public final class PGlGeometryArena
{
	private static final int PAGE_BYTES = 4 * 1024 * 1024;
	private static final int VERTEX_STRIDE = PRenderTypes.VertexFormatProvider.POSITION_TEX_NORMAL.getVertexSize();

	private final List<Page> pages = new ArrayList<>();
	private final Map<PGeometryData, Slice> geometrySlices = new IdentityHashMap<>();
	private final Map<PGlDynamicGeometry, Map<Integer, DynamicAllocation>> dynamicPages = new IdentityHashMap<>();

	/**
	 * Performs the resolve operation.
	 * @param mesh the mesh to use.
	 * @return the value produced by this operation.
	 */
	public Slice resolve(PBakedMesh mesh)
	{
		return resolve(mesh.geometry());
	}

	/** Appends immutable portable geometry into the same static arena pages. */
	public Slice resolve(PGeometryData geometry)
	{
		return this.geometrySlices.computeIfAbsent(geometry, data ->
		{
			validate(data, VERTEX_STRIDE);
			int vertexBytes = data.vertices().remaining();
			int indexBytes = Math.multiplyExact(data.indexCount(), data.indexType().bytes());
			Page page = this.pages.isEmpty() ? null : this.pages.getLast();
			if (page == null || !page.hasSpace(vertexBytes, indexBytes, data.indexType().bytes()))
			{
				page = new Page(Math.max(PAGE_BYTES, vertexBytes), Math.max(PAGE_BYTES, indexBytes));
				this.pages.add(page);
			}
			return page.append(data);
		});
	}

	/** Reuses one allocation per reclaimed executor frame slot, rather than accumulating snapshots. */
	Slice resolve(PGlDynamicGeometry source, int frameSlot)
	{
		PGeometryData data = source.geometry();
		validate(data, VERTEX_STRIDE);
		Map<Integer, DynamicAllocation> slots = this.dynamicPages.computeIfAbsent(source, ignored ->
		{
			source.onClose(this, () ->
			{
				Map<Integer, DynamicAllocation> owned = this.dynamicPages.remove(source);
				if (owned != null)
					owned.values().forEach(allocation -> allocation.page().close());
			});
			return new HashMap<>();
		});
		DynamicAllocation previous = slots.get(frameSlot);
		if (previous != null && previous.data() == data)
			return previous.slice();
		int vertexBytes = data.vertices().remaining();
		int indexBytes = Math.multiplyExact(data.indexCount(), data.indexType().bytes());
		Page page;
		if (previous == null || vertexBytes > previous.page().vertexCapacity || indexBytes > previous.page().indexCapacity)
		{
			if (previous != null)
				previous.page().close();
			page = new Page(Math.max(VERTEX_STRIDE * 64, vertexBytes), Math.max(256, indexBytes));
		}
		else
			page = previous.page();
		page.vertices = 0;
		page.indices = 0;
		Slice slice = page.append(data);
		slots.put(frameSlot, new DynamicAllocation(page, data, slice));
		return slice;
	}

	static void validate(PGeometryData data, int vertexStride)
	{
		if (data.vertexStride() != vertexStride || data.vertices().remaining() % vertexStride != 0)
			throw new IllegalArgumentException("Portable geometry must use PulseLib POSITION_TEX_NORMAL vertex layout");
	}

	/**
	 * Performs the clear operation.
	 */
	public void clear()
	{
		this.pages.forEach(Page :: close);
		this.pages.clear();
		this.geometrySlices.clear();
		this.dynamicPages.values().forEach(slots -> slots.values().forEach(allocation -> allocation.page().close()));
		this.dynamicPages.keySet().forEach(source -> source.removeCloseListener(this));
		this.dynamicPages.clear();
	}

	private record DynamicAllocation(Page page, PGeometryData data, Slice slice)
	{
	}


/**
 * Immutable value object representing slice.
 */
	public record Slice(Page page, long indexOffset, int baseVertex, int indexCount, int indexType)
	{
	}

/**
 * Provides support for page.
 */
	public static final class Page
	{
		private final int vertexBuffer;
		private final int indexBuffer;
		private final int vertexArray;
		private final int vertexCapacity;
		private final int indexCapacity;
		private int vertices;
		private int indices;
		private int instanceBuffer = -1;

		/**
		 * Creates an instance of the enclosing type.
		 * @param vertexCapacity the vertex capacity to use.
		 * @param indexCapacity the index capacity to use.
		 */
		private Page(int vertexCapacity, int indexCapacity)
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
			GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, VERTEX_STRIDE, 0L);
			GL20.glEnableVertexAttribArray(1);
			GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, VERTEX_STRIDE, 12L);
			GL20.glEnableVertexAttribArray(2);
			GL20.glVertexAttribPointer(2, 3, GL11.GL_BYTE, true, VERTEX_STRIDE, 20L);
			GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, this.indexBuffer);
			GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indexCapacity, GL15.GL_STATIC_DRAW);
			GL30.glBindVertexArray(0);
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
			GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
		}

		/** Returns the existing native vertex-array name. */
		public int vertexArray()
		{
			return this.vertexArray;
		}

		/**
		 * Performs the bind operation.
		 * @param instanceBuffer the instance buffer to use.
		 * @param instanceOffset the instance offset to use.
		 */
		public void bind(int instanceBuffer, long instanceOffset)
		{
			GL30.glBindVertexArray(this.vertexArray);
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, instanceBuffer);
			if (this.instanceBuffer != instanceBuffer)
			{
				this.instanceBuffer = instanceBuffer;
				for (int row = 0; row < 3; row++)
				{
					GL20.glEnableVertexAttribArray(4 + row);
					GL20.glVertexAttribPointer(4 + row, 4, GL11.GL_FLOAT, false, PGlInstanceStream.STRIDE, (long)row * 16L);
					GL33.glVertexAttribDivisor(4 + row, 1);
				}
				GL20.glEnableVertexAttribArray(7);
				GL20.glVertexAttribPointer(7, 4, GL11.GL_FLOAT, false, PGlInstanceStream.STRIDE, 48L);
				GL33.glVertexAttribDivisor(7, 1);
				GL20.glEnableVertexAttribArray(8);
				GL20.glVertexAttribPointer(8, 2, GL11.GL_FLOAT, false, PGlInstanceStream.STRIDE, 64L);
				GL33.glVertexAttribDivisor(8, 1);
				GL20.glEnableVertexAttribArray(9);
				GL20.glVertexAttribPointer(9, 2, GL11.GL_FLOAT, false, PGlInstanceStream.STRIDE, 72L);
				GL33.glVertexAttribDivisor(9, 1);
				GL20.glEnableVertexAttribArray(10);
				GL30.glVertexAttribIPointer(10, 4, GL11.GL_INT, PGlInstanceStream.STRIDE, 80L);
				GL33.glVertexAttribDivisor(10, 1);
			}
			this.setInstanceOffset(instanceOffset);
		}

		/**
		 * Sets the instance offset.
		 * @param instanceOffset the instance offset to use.
		 */
		public void setInstanceOffset(long instanceOffset)
		{
			for (int row = 0; row < 3; row++)
				GL20.glVertexAttribPointer(4 + row, 4, GL11.GL_FLOAT, false, PGlInstanceStream.STRIDE, instanceOffset + (long)row * 16L);
			GL20.glVertexAttribPointer(7, 4, GL11.GL_FLOAT, false, PGlInstanceStream.STRIDE, instanceOffset + 48L);
			GL20.glVertexAttribPointer(8, 2, GL11.GL_FLOAT, false, PGlInstanceStream.STRIDE, instanceOffset + 64L);
			GL20.glVertexAttribPointer(9, 2, GL11.GL_FLOAT, false, PGlInstanceStream.STRIDE, instanceOffset + 72L);
			GL30.glVertexAttribIPointer(10, 4, GL11.GL_INT, PGlInstanceStream.STRIDE, instanceOffset + 80L);
		}

		/**
		 * Determines whether the object has space.
		 * @param vertexBytes the vertex bytes to use.
		 * @param indexBytes the index bytes to use.
		 * @param alignment the alignment to use.
		 * @return the value produced by this operation.
		 */
		private boolean hasSpace(int vertexBytes, int indexBytes, int alignment)
		{
			int indexOffset = align(this.indices, alignment);
			return vertexBytes <= this.vertexCapacity - this.vertices && indexBytes <= this.indexCapacity - indexOffset;
		}

		private Slice append(PGeometryData geometry)
		{
			int indexOffset = align(this.indices, geometry.indexType().bytes());
			int baseVertex = this.vertices / VERTEX_STRIDE;
			ByteBuffer vertices = geometry.vertices();
			ByteBuffer indices = geometry.indices();
			int indexBytes = Math.multiplyExact(geometry.indexCount(), geometry.indexType().bytes());
			indices.limit(indexBytes);
			upload(this.vertexBuffer, this.vertices, vertices);
			upload(this.indexBuffer, indexOffset, indices);
			this.vertices += vertices.remaining();
			this.indices = indexOffset + indexBytes;
			return new Slice(this, indexOffset, baseVertex, geometry.indexCount(),
					geometry.indexType() == PGeometryData.IndexType.UNSIGNED_SHORT ? GL11.GL_UNSIGNED_SHORT : GL11.GL_UNSIGNED_INT);
		}

		private static void upload(int target, int offset, ByteBuffer source)
		{
			if (!source.hasRemaining())
				return;
			int previous = GL11.glGetInteger(GL31.GL_COPY_WRITE_BUFFER);
			ByteBuffer direct = MemoryUtil.memAlloc(source.remaining());
			try
			{
				direct.put(source.duplicate()).flip();
				GL15.glBindBuffer(GL31.GL_COPY_WRITE_BUFFER, target);
				GL15.glBufferSubData(GL31.GL_COPY_WRITE_BUFFER, offset, direct);
			}
			finally
			{
				GL15.glBindBuffer(GL31.GL_COPY_WRITE_BUFFER, previous);
				MemoryUtil.memFree(direct);
			}
		}

		/**
		 * Performs the align operation.
		 * @param value the value to use.
		 * @param alignment the alignment to use.
		 * @return the value produced by this operation.
		 */
		private static int align(int value, int alignment)
		{
			return (value + alignment - 1) & -alignment;
		}

		/**
		 * Performs the close operation.
		 */
		private void close()
		{
			GL30.glDeleteVertexArrays(this.vertexArray);
			GL15.glDeleteBuffers(this.vertexBuffer);
			GL15.glDeleteBuffers(this.indexBuffer);
		}
	}
}
