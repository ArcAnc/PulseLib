/**
 * @author ArcAnc
 * Created at: 13.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.legacy;

import com.arcanc.pulselib.content.renderer.gl.PGlDynamicGeometry;
import com.arcanc.pulselib.content.renderer.gl.PGlResourceRegistry;
import com.arcanc.pulselib.content.renderer.plan.PDrawCommand;
import com.arcanc.pulselib.content.renderer.plan.PDynamicGeometry;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.arcanc.pulselib.content.renderer.plan.PMeshHandle;
import com.arcanc.pulselib.content.renderer.plan.PPipelineHandle;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * Maps render pipelines and geometry objects to stable handles used in render plans.
 */
public final class GlResourceRegistry
{
	private final PGlResourceRegistry resources = new PGlResourceRegistry();
	private final GlGeometryArena geometryArena = new GlGeometryArena();

	/** Registers a render pipeline or resolves its existing identity handle. */
	public PPipelineHandle pipeline(RenderType pipeline)
	{
		return this.resources.pipeline(pipeline);
	}

	/** Registers supported dynamic geometry in the native registry. */
	public PMeshHandle dynamic(PDynamicGeometry geometry)
	{
		return this.resources.dynamic(geometry);
	}

	/** Resolves a live render pipeline owned by this registry. */
	public RenderType pipeline(PPipelineHandle handle)
	{
		return this.resources.pipeline(handle);
	}

	/** Resolves the native dynamic source replacing Minecraft VertexBuffer. */
	public PGlDynamicGeometry mesh(PMeshHandle handle)
	{
		return this.resources.dynamicSource(handle);
	}

	/** Returns the index count of a registered dynamic source. */
	public int indexCount(PDynamicGeometry geometry)
	{
		return this.resources.indexCount(this.resources.dynamic(geometry));
	}

	/** Registers immutable geometry and resolves its native page allocation. */
	public PMeshHandle geometry(PGeometryData geometry)
	{
		this.geometryArena.register(geometry);
		return this.resources.geometry(geometry);
	}

	/** Returns the canonical view of a live immutable page slice. */
	public GlGeometryArena.Slice geometry(PMeshHandle handle)
	{
		return this.geometryArena.register(this.resources.geometryData(handle));
	}

	/** Returns whether the handle refers to immutable geometry in this registry. */
	public boolean isGeometry(PMeshHandle handle)
	{
		return this.resources.isGeometry(handle);
	}

	/** Builds canonical page-relative draw addressing for the requested instances. */
	public PDrawCommand resolveCommand(PMeshHandle handle, int indexCount, int instanceCount)
	{
		if (!isGeometry(handle))
			return new PDrawCommand(indexCount, instanceCount, 0, 0, 0);
		GlGeometryArena.Slice slice = geometry(handle);
		return new PDrawCommand(indexCount, instanceCount, slice.firstIndex(this.resources.geometryData(handle).indexType()), slice.baseVertex(), 0);
	}

	/** Releases native pages, handles and dynamic close listeners. */
	public void clear()
	{
		this.resources.clear();
		this.geometryArena.clear();
	}

	PGlResourceRegistry nativeResources()
	{
		return this.resources;
	}

	GlGeometryArena geometryArena()
	{
		return this.geometryArena;
	}
}
