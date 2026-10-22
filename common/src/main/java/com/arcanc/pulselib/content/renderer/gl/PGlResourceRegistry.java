/**
 * @author ArcAnc
 * Created at: 22.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.gl;

import com.arcanc.pulselib.content.model.baked.PBakedMesh;
import com.arcanc.pulselib.content.renderer.plan.PMeshHandle;
import com.arcanc.pulselib.content.renderer.plan.PPipelineHandle;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.util.IdentityHashMap;
import java.util.Map;

/** Maps canonical plan handles to the existing Minecraft 26.1 GPU resources. */
public final class PGlResourceRegistry
{
	private final Map<RenderType, PPipelineHandle> pipelines = new IdentityHashMap<>();
	private final Map<PPipelineHandle, RenderType> pipelineResources = new IdentityHashMap<>();
	private final Map<PBakedMesh, PMeshHandle> meshes = new IdentityHashMap<>();
	private final Map<PMeshHandle, PBakedMesh> meshResources = new IdentityHashMap<>();
	private long nextHandle;

	/** Registers a pipeline by resource identity without hashing its contents. */
	public PPipelineHandle pipeline(RenderType pipeline)
	{
		return this.pipelines.computeIfAbsent(pipeline, resource ->
		{
			PPipelineHandle handle = new PPipelineHandle(this.nextHandle++);
			this.pipelineResources.put(handle, resource);
			return handle;
		});
	}

	/** Registers baked geometry without creating another geometry arena or buffer. */
	public PMeshHandle mesh(PBakedMesh mesh)
	{
		return this.meshes.computeIfAbsent(mesh, resource ->
		{
			PMeshHandle handle = new PMeshHandle(this.nextHandle++);
			this.meshResources.put(handle, resource);
			return handle;
		});
	}

	/** Resolves a live pipeline handle owned by this registry. */
	public RenderType pipeline(PPipelineHandle handle)
	{
		RenderType resource = this.pipelineResources.get(handle);
		if (resource == null)
			throw new IllegalStateException("Unknown PulseLib pipeline handle: " + handle.value());
		return resource;
	}

	/** Resolves a live mesh handle owned by this registry. */
	public PBakedMesh mesh(PMeshHandle handle)
	{
		PBakedMesh resource = this.meshResources.get(handle);
		if (resource == null)
			throw new IllegalStateException("Unknown PulseLib mesh handle: " + handle.value());
		return resource;
	}

	/** Drops handles after their executor has released its GPU allocations. */
	public void clear()
	{
		this.pipelines.clear();
		this.pipelineResources.clear();
		this.meshes.clear();
		this.meshResources.clear();
	}
}
