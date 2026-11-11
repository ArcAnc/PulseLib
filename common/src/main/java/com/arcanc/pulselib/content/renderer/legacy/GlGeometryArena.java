/**
 * @author ArcAnc
 * Created at: 13.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.legacy;

import com.arcanc.pulselib.content.renderer.gl.PGlGeometryArena;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import org.lwjgl.opengl.GL30;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Allocates immutable mesh geometry into shared OpenGL pages.
 */
public final class GlGeometryArena
{
	private final PGlGeometryArena arena = new PGlGeometryArena();
	private final Map<PGlGeometryArena.Slice, Slice> slices = new IdentityHashMap<>();

	/** Uploads or reuses immutable geometry in the native shared pages. */
	public Slice register(PGeometryData geometry)
	{
		PGlGeometryArena.Slice slice = this.arena.resolve(geometry);
		return this.slices.computeIfAbsent(slice, source -> new Slice(source.page().vertexArray(), source.indexOffset(), source.indexType(), source.baseVertex()));
	}

	/** Releases the native arena pages and their canonical slice views. */
	public void clear()
	{
		this.arena.clear();
		this.slices.clear();
	}

	PGlGeometryArena nativeArena()
	{
		return this.arena;
	}

	/** Vertex and index range occupied by a mesh in a shared native page. */
	public record Slice(int vertexArray, long indexOffset, int indexType, int baseVertex)
	{
		/** Binds the page containing this geometry slice. */
		public void bind()
		{
			GL30.glBindVertexArray(this.vertexArray);
		}

		/** Converts the byte offset to an aligned index-element offset. */
		public int firstIndex(PGeometryData.IndexType type)
		{
			if (this.indexOffset % type.bytes() != 0)
				throw new IllegalStateException("Geometry slice index offset is not aligned to its index type");
			return Math.toIntExact(this.indexOffset / type.bytes());
		}
	}
}
