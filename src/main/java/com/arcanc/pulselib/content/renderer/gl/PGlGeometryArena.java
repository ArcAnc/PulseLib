/**
 * @author ArcAnc
 * Created at: 14.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.gl;

import com.arcanc.pulselib.content.mixin.GlBufferAccessor;
import com.arcanc.pulselib.content.model.baked.PBakedMesh;
import com.arcanc.pulselib.util.PRenderTypes;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.lwjgl.opengl.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class PGlGeometryArena
{
	private static final int PAGE_BYTES = 4 * 1024 * 1024;
	private static final int VERTEX_STRIDE = PRenderTypes.VertexFormatProvider.POSITION_TEX_NORMAL.getVertexSize();

	private final Map<PBakedMesh, Slice> slices = new Object2ObjectOpenHashMap<>();
	private final List<Page> pages = new ArrayList<>();

	/**
	 * Performs the resolve operation.
	 * @param mesh the mesh to use.
	 * @return the value produced by this operation.
	 */
	public Slice resolve(PBakedMesh mesh)
	{
		return this.slices.computeIfAbsent(mesh, this :: append);
	}

	/**
	 * Performs the clear operation.
	 */
	public void clear()
	{
		this.pages.forEach(Page :: close);
		this.pages.clear();
		this.slices.clear();
	}

	/**
	 * Performs the append operation.
	 * @param mesh the mesh to use.
	 * @return the value produced by this operation.
	 */
	private Slice append(PBakedMesh mesh)
	{
		int vertexBytes = mesh.vertexesAmount() * VERTEX_STRIDE;
		int indexBytes = mesh.indicesCount() * mesh.indexType().bytes;
		Page page = this.pages.isEmpty() ? null : this.pages.getLast();
		if (page == null || !page.hasSpace(vertexBytes, indexBytes, mesh.indexType().bytes))
		{
			page = new Page(Math.max(PAGE_BYTES, vertexBytes), Math.max(PAGE_BYTES, indexBytes));
			this.pages.add(page);
		}
		return page.append(mesh, vertexBytes, indexBytes);
	}

	public record Slice(Page page, long indexOffset, int baseVertex, int indexCount, int indexType)
	{
	}

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

		/**
		 * Performs the append operation.
		 * @param mesh the mesh to use.
		 * @param vertexBytes the vertex bytes to use.
		 * @param indexBytes the index bytes to use.
		 * @return the value produced by this operation.
		 */
		private Slice append(PBakedMesh mesh, int vertexBytes, int indexBytes)
		{
			int indexOffset = align(this.indices, mesh.indexType().bytes);
			int baseVertex = this.vertices / VERTEX_STRIDE;
			copy(((GlBufferAccessor)mesh.vbo()).pulselib$getHandle(), this.vertexBuffer, this.vertices, vertexBytes);
			copy(((GlBufferAccessor)mesh.indices()).pulselib$getHandle(), this.indexBuffer, indexOffset, indexBytes);
			this.vertices += vertexBytes;
			this.indices = indexOffset + indexBytes;
			return new Slice(this, indexOffset, baseVertex, mesh.indicesCount(),
					mesh.indexType() == VertexFormat.IndexType.SHORT ? GL11.GL_UNSIGNED_SHORT : GL11.GL_UNSIGNED_INT);
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
		 * Performs the copy operation.
		 * @param source the source to use.
		 * @param target the target to use.
		 * @param targetOffset the target offset to use.
		 * @param size the size to use.
		 */
		private static void copy(int source, int target, int targetOffset, int size)
		{
			GL15.glBindBuffer(GL31.GL_COPY_READ_BUFFER, source);
			GL15.glBindBuffer(GL31.GL_COPY_WRITE_BUFFER, target);
			GL31.glCopyBufferSubData(GL31.GL_COPY_READ_BUFFER, GL31.GL_COPY_WRITE_BUFFER, 0L, targetOffset, size);
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
