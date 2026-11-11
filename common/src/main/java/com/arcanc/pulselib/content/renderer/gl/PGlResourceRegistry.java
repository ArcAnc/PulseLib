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
import com.arcanc.pulselib.content.renderer.legacy.GlDynamicGeometry;
import com.arcanc.pulselib.content.renderer.plan.PMeshHandle;
import com.arcanc.pulselib.content.renderer.plan.PPipelineHandle;
import com.arcanc.pulselib.content.renderer.plan.PTransparencyMode;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.arcanc.pulselib.content.renderer.plan.PDynamicGeometry;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Map;

/** Maps canonical plan handles to the existing Minecraft 26.1 GPU resources. */
public final class PGlResourceRegistry
{
	private final Map<RenderType, Map<PTransparencyMode, PPipelineHandle>> pipelines = new IdentityHashMap<>();
	private final Map<PPipelineHandle, PipelineBinding> pipelineResources = new IdentityHashMap<>();
	private final Map<PBakedMesh, PMeshHandle> meshes = new IdentityHashMap<>();
	private final Map<PGeometryData, PMeshHandle> geometry = new IdentityHashMap<>();
	private final Map<PGlDynamicGeometry, PMeshHandle> dynamic = new IdentityHashMap<>();
	private final Map<PMeshHandle, Object> meshResources = new IdentityHashMap<>();
	private long nextHandle;

	/** Registers a pipeline by resource identity without hashing its contents. */
	public PPipelineHandle pipeline(RenderType pipeline)
	{
		return pipeline(pipeline, PRenderTypes.usesOit(pipeline) ? PTransparencyMode.OIT : PTransparencyMode.ORDINARY);
	}

	/** Registers an explicit policy binding; both modes retain the very same render type and GPU pipeline. */
	public PPipelineHandle pipeline(RenderType pipeline, PTransparencyMode transparency)
	{
		Objects.requireNonNull(pipeline);
		Objects.requireNonNull(transparency);
		if (transparency == PTransparencyMode.OIT && !PRenderTypes.usesOit(pipeline))
			throw new IllegalArgumentException("Render type does not support PulseLib OIT: " + pipeline);
		return this.pipelines.computeIfAbsent(pipeline, ignored -> new EnumMap<>(PTransparencyMode.class)).
				computeIfAbsent(transparency, mode ->
				{
					PPipelineHandle handle = new PPipelineHandle(this.nextHandle++);
					this.pipelineResources.put(handle, new PipelineBinding(pipeline, mode));
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
		return binding(handle).type();
	}

	/** Resolves submission policy independently of render-type identity. */
	public PTransparencyMode transparency(PPipelineHandle handle)
	{
		return binding(handle).transparency();
	}

	private PipelineBinding binding(PPipelineHandle handle)
	{
		PipelineBinding binding = this.pipelineResources.get(handle);
		if (binding == null)
			throw new IllegalStateException("Unknown PulseLib pipeline handle: " + handle.value());
		return binding;
	}

	private record PipelineBinding(RenderType type, PTransparencyMode transparency)
	{
	}

	/** Resolves a live mesh handle owned by this registry. */
	public PBakedMesh mesh(PMeshHandle handle)
	{
		Object resource = resource(handle);
		if (resource instanceof PBakedMesh mesh)
			return mesh;
		throw new IllegalArgumentException("Handle refers to portable geometry, not a legacy baked mesh");
	}

	/** Registers immutable geometry by identity, without allocating GPU buffers during submission. */
	public PMeshHandle geometry(PGeometryData geometry)
	{
		return this.geometry.computeIfAbsent(geometry, resource -> register(resource));
	}

	/** Registers the supported Minecraft 26.1 dynamic geometry implementation. */
	public PMeshHandle dynamic(PDynamicGeometry geometry)
	{
		PGlDynamicGeometry source;
		if (geometry instanceof GlDynamicGeometry facade)
			source = facade.vertexBuffer();
		else if (geometry instanceof PGlDynamicGeometry nativeSource)
			source = nativeSource;
		else
			throw new IllegalArgumentException("Unsupported dynamic geometry for Minecraft 26.1: " + geometry.getClass().getName());
		source.geometry();
		return this.dynamic.computeIfAbsent(source, resource ->
		{
			PMeshHandle handle = register(resource);
			resource.onClose(this, () ->
			{
				this.dynamic.remove(resource);
				this.meshResources.remove(handle);
			});
			return handle;
		});
	}

	public int indexCount(PMeshHandle handle)
	{
		return switch (resource(handle))
		{
			case PBakedMesh mesh -> mesh.geometry().indexCount();
			case PGeometryData data -> data.indexCount();
			case PGlDynamicGeometry source -> source.geometry().indexCount();
			default -> throw new IllegalStateException("Unknown geometry resource");
		};
	}

	/** Resolves the native dynamic source behind a canonical facade handle. */
	public PGlDynamicGeometry dynamicSource(PMeshHandle handle)
	{
		if (resource(handle) instanceof PGlDynamicGeometry source)
			return source;
		throw new IllegalArgumentException("Handle does not refer to dynamic geometry");
	}

	/** Resolves immutable source data without allocating another arena. */
	public PGeometryData geometryData(PMeshHandle handle)
	{
		if (resource(handle) instanceof PGeometryData data)
			return data;
		throw new IllegalArgumentException("Handle does not refer to immutable geometry");
	}

	/** Tests the immutable geometry kind without accepting foreign handles. */
	public boolean isGeometry(PMeshHandle handle)
	{
		return this.meshResources.get(handle) instanceof PGeometryData;
	}

	PGlGeometryArena.Slice resolve(PMeshHandle handle, PGlGeometryArena arena, int frameSlot)
	{
		return switch (resource(handle))
		{
			case PBakedMesh mesh -> arena.resolve(mesh);
			case PGeometryData data -> arena.resolve(data);
			case PGlDynamicGeometry source -> arena.resolve(source, frameSlot);
			default -> throw new IllegalStateException("Unknown geometry resource");
		};
	}

	private PMeshHandle register(Object resource)
	{
		PMeshHandle handle = new PMeshHandle(this.nextHandle++);
		this.meshResources.put(handle, resource);
		return handle;
	}

	private Object resource(PMeshHandle handle)
	{
		Object resource = this.meshResources.get(handle);
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
		this.geometry.clear();
		this.dynamic.keySet().forEach(source -> source.removeCloseListener(this));
		this.dynamic.clear();
		this.meshResources.clear();
	}
}
