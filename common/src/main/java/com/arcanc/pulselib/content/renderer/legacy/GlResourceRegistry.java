/**
 * @author ArcAnc
 * Created at: 13.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.legacy;

import com.arcanc.pulselib.content.renderer.plan.PMeshHandle;
import com.arcanc.pulselib.content.renderer.plan.PPipelineHandle;
import com.arcanc.pulselib.content.renderer.plan.PDynamicGeometry;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.arcanc.pulselib.content.renderer.plan.PDrawCommand;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.renderer.RenderType;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Maps render pipelines and geometry objects to stable handles used in render plans.
 */
public final class GlResourceRegistry
{
	private final Map<RenderType, PPipelineHandle> pipelines = new IdentityHashMap<>();
	private final Map<PPipelineHandle, RenderType> pipelineResources = new IdentityHashMap<>();
	private final Map<GlDynamicGeometry, PMeshHandle> meshes = new IdentityHashMap<>();
	private final Map<PMeshHandle, GlDynamicGeometry> meshResources = new IdentityHashMap<>();
	private final Map<PGeometryData, PMeshHandle> geometry = new IdentityHashMap<>();
	private final Map<PMeshHandle, GlGeometryArena.Slice> geometryResources = new IdentityHashMap<>();
	private final Map<PMeshHandle, PGeometryData> geometryData = new IdentityHashMap<>();
	private final GlGeometryArena geometryArena = new GlGeometryArena();
	private long nextHandle;

	/**
	 * Registers a render pipeline or resolves an existing pipeline handle.
	 *
	 * @param pipeline the pipeline
	 * @return the registered pipeline handle or resolved render type
	 */
	public PPipelineHandle pipeline(RenderType pipeline)
	{
		return this.pipelines.computeIfAbsent(pipeline, resource ->
		{
			PPipelineHandle handle = new PPipelineHandle(this.nextHandle++);
			this.pipelineResources.put(handle, resource);
			return handle;
		});
	}

	/**
	 * Registers dynamic geometry and returns its render-plan mesh handle.
	 *
	 * @param geometry the dynamically updated geometry
	 * @return the mesh handle registered for the dynamic geometry
	 */
	public PMeshHandle dynamic(PDynamicGeometry geometry)
	{
		if (!(geometry instanceof GlDynamicGeometry mesh))
			throw new IllegalArgumentException("Unsupported dynamic geometry for the legacy OpenGL driver: " + geometry.getClass().getName());
		return this.meshes.computeIfAbsent(mesh, resource ->
		{
			PMeshHandle handle = new PMeshHandle(this.nextHandle++);
			this.meshResources.put(handle, resource);
			return handle;
		});
	}

	/**
	 * Registers a render pipeline or resolves an existing pipeline handle.
	 *
	 * @param handle the handle
	 * @return the registered pipeline handle or resolved render type
	 */
	public RenderType pipeline(PPipelineHandle handle)
	{
		RenderType result = this.pipelineResources.get(handle);
		if (result == null)
			throw new IllegalStateException("Unknown PulseLib legacy pipeline handle: " + handle.value());
		return result;
	}

	/**
	 * Resolves the vanilla vertex buffer associated with a dynamic mesh handle.
	 *
	 * @param handle the handle
	 * @return the vertex buffer associated with the dynamic mesh handle
	 */
	public VertexBuffer mesh(PMeshHandle handle)
	{
		GlDynamicGeometry result = this.meshResources.get(handle);
		if (result == null)
			throw new IllegalStateException("Unknown PulseLib legacy mesh handle: " + handle.value());
		return result.vertexBuffer();
	}

	/**
	 * Returns the draw index count of the supplied dynamic geometry.
	 *
	 * @param geometry the dynamically updated geometry
	 * @return the draw index count of the supplied dynamic geometry
	 */
	public int indexCount(PDynamicGeometry geometry)
	{
		if (!(geometry instanceof GlDynamicGeometry mesh))
			throw new IllegalArgumentException("Unsupported dynamic geometry for the legacy OpenGL driver: " + geometry.getClass().getName());
		return mesh.indexCount();
	}

	/**
	 * Registers immutable geometry or resolves its shared arena slice.
	 *
	 * @param mesh the immutable indexed mesh geometry
	 * @return the registered mesh handle or resolved shared arena slice
	 */
	public PMeshHandle geometry(PGeometryData mesh)
	{
		return this.geometry.computeIfAbsent(mesh, resource ->
		{
			PMeshHandle handle = new PMeshHandle(this.nextHandle++);
			this.geometryResources.put(handle, this.geometryArena.register(resource));
			this.geometryData.put(handle, resource);
			return handle;
		});
	}

	/**
	 * Registers immutable geometry or resolves its shared arena slice.
	 *
	 * @param handle the handle
	 * @return the registered mesh handle or resolved shared arena slice
	 */
	public GlGeometryArena.Slice geometry(PMeshHandle handle)
	{
		GlGeometryArena.Slice result = this.geometryResources.get(handle);
		if (result == null)
			throw new IllegalStateException("Unknown PulseLib geometry-arena handle: " + handle.value());
		return result;
	}

	/**
	 * Returns whether a mesh handle refers to immutable arena geometry.
	 *
	 * @param handle the handle
	 * @return whether a mesh handle refers to immutable arena geometry
	 */
	public boolean isGeometry(PMeshHandle handle)
	{
		return this.geometryResources.containsKey(handle);
	}

	/**
	 * Builds a draw command with arena offsets and the supplied instance count.
	 *
	 * @param handle the handle
	 * @param indexCount the number of indices to draw
	 * @param instanceCount the number of instances to draw
	 * @return the draw command containing resolved geometry and instance offsets
	 */
	public PDrawCommand resolveCommand(PMeshHandle handle, int indexCount, int instanceCount)
	{
		GlGeometryArena.Slice slice = this.geometryResources.get(handle);
		if (slice == null)
			return new PDrawCommand(indexCount, instanceCount, 0, 0, 0);
		PGeometryData data = this.geometryData.get(handle);
		if (data == null)
			throw new IllegalStateException("Geometry handle has no source data: " + handle.value());
		return new PDrawCommand(indexCount, instanceCount, slice.firstIndex(data.indexType()), slice.baseVertex(), 0);
	}

	/**
	 * Releases registered geometry and clears pipeline and mesh handle mappings.
	 */
	public void clear()
	{
		this.pipelines.clear();
		this.pipelineResources.clear();
		this.meshes.clear();
		this.meshResources.clear();
		this.geometry.clear();
		this.geometryResources.clear();
		this.geometryData.clear();
		this.geometryArena.clear();
	}
}
