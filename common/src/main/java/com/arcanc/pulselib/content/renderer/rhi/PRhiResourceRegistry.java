/**
 * @author ArcAnc
 * Created at: 13.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.rhi;

import com.arcanc.pulselib.content.renderer.plan.PDynamicGeometry;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.arcanc.pulselib.content.renderer.plan.PMeshHandle;
import com.arcanc.pulselib.content.renderer.plan.PPipelineHandle;
import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.GpuDevice;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/** Owns GPU geometry separately from portable model data. Access only on its creating render thread. */
@ApiStatus.Internal
public final class PRhiResourceRegistry implements AutoCloseable
{
	private static final AtomicLong IDS = new AtomicLong();
	private final Thread owner = Thread.currentThread();
	private final GpuDevice device;
	private final Map<PRhiMaterial, PPipelineHandle> pipelines = new IdentityHashMap<>();
	private final Map<PPipelineHandle, PRhiMaterial> materials = new HashMap<>();
	private final Map<Object, PMeshHandle> sources = new IdentityHashMap<>();
	private final Map<PMeshHandle, Geometry> meshes = new HashMap<>();

	public PRhiResourceRegistry(GpuDevice device)
	{
		this.device = Objects.requireNonNull(device);
	}

	void verifyDevice(GpuDevice device)
	{
		checkThread();
		if (this.device != device)
			throw new IllegalArgumentException("RHI registry belongs to a different GPU device");
	}

	public PPipelineHandle pipeline(PRhiMaterial material)
	{
		checkThread();
		return this.pipelines.computeIfAbsent(Objects.requireNonNull(material), source ->
		{
			PPipelineHandle handle = new PPipelineHandle(nextId());
			this.materials.put(handle, source);
			return handle;
		});
	}

	/** Register PBakedMesh.geometry() here; no GPU ownership is added to PBakedMesh. */
	public PMeshHandle geometry(PGeometryData data)
	{
		return register(Objects.requireNonNull(data), () -> data);
	}

	public PMeshHandle dynamic(PDynamicGeometry source)
	{
		if (!(source instanceof PRhiDynamicGeometry dynamic))
			throw new IllegalArgumentException("Use an explicit CPU-data adapter for this dynamic geometry type");
		dynamic.geometry();
		PMeshHandle handle = register(dynamic, dynamic::geometry);
		dynamic.onClose(this, () -> release(handle));
		return handle;
	}

	/** Adapts other canonical dynamic sources without importing their backend implementation. Caller owns release. */
	public PMeshHandle dynamic(PDynamicGeometry source, Supplier<PGeometryData> data)
	{
		return register(Objects.requireNonNull(source), Objects.requireNonNull(data));
	}

	private PMeshHandle register(Object identity, Supplier<PGeometryData> data)
	{
		checkThread();
		return this.sources.computeIfAbsent(identity, source ->
		{
			PMeshHandle handle = new PMeshHandle(nextId());
			this.meshes.put(handle, new Geometry(source, data));
			return handle;
		});
	}

	public PRhiMaterial material(PPipelineHandle handle)
	{
		checkThread();
		PRhiMaterial material = this.materials.get(handle);
		if (material == null)
			throw new IllegalStateException("Unknown/stale RHI pipeline handle");
		return material;
	}

	public PGeometryData geometryData(PMeshHandle handle)
	{
		return Objects.requireNonNull(mesh(handle).source.get(), "Dynamic source returned null geometry");
	}

	Buffers resolve(PMeshHandle handle, PGeometryData data)
	{
		Geometry mesh = mesh(handle);
		if (mesh.uploaded != data)
		{
			if (data.vertices().remaining() == 0 || data.vertices().remaining() % data.vertexStride() != 0 || data.indexCount() == 0)
				throw new IllegalArgumentException("Cannot draw empty/incomplete RHI geometry");
			GpuBuffer vertices = upload("PulseLib RHI vertices " + handle.value(), GpuBuffer.USAGE_VERTEX, data.vertices());
			GpuBuffer indices;
			try
			{
				indices = upload("PulseLib RHI indices " + handle.value(), GpuBuffer.USAGE_INDEX, data.indices());
			}
			catch (RuntimeException | Error failure)
			{
				vertices.close();
				throw failure;
			}
			Buffers replacement = new Buffers(vertices, indices,
					data.indexType() == PGeometryData.IndexType.UNSIGNED_SHORT ? IndexType.SHORT : IndexType.INT);
			Buffers previous = mesh.buffers;
			mesh.buffers = replacement;
			mesh.uploaded = data;
			if (previous != null)
				previous.close();
		}
		return Objects.requireNonNull(mesh.buffers);
	}

	private GpuBuffer upload(String label, int usage, ByteBuffer source)
	{
		ByteBuffer direct = ByteBuffer.allocateDirect(source.remaining());
		direct.put(source.duplicate()).flip();
		return this.device.createBuffer(() -> label, usage, direct);
	}

	public void release(PMeshHandle handle)
	{
		checkThread();
		Geometry mesh = this.meshes.remove(handle);
		if (mesh == null)
			throw new IllegalArgumentException("Unknown/stale RHI mesh handle");
		this.sources.remove(mesh.identity);
		if (mesh.identity instanceof PRhiDynamicGeometry dynamic)
			dynamic.removeListener(this);
		if (mesh.buffers != null)
			mesh.buffers.close();
	}

	private Geometry mesh(PMeshHandle handle)
	{
		checkThread();
		Geometry mesh = this.meshes.get(handle);
		if (mesh == null)
			throw new IllegalStateException("Unknown/stale RHI mesh handle");
		return mesh;
	}

	/** Invalidates handles on reload/disconnect; borrowed material resources remain with their owners. */
	@Override
	public void close()
	{
		checkThread();
		RuntimeException failure = null;
		for (Geometry mesh : this.meshes.values())
		{
			if (mesh.identity instanceof PRhiDynamicGeometry dynamic)
				dynamic.removeListener(this);
			if (mesh.buffers != null)
				try
				{
					mesh.buffers.close();
				}
				catch (RuntimeException exception)
				{
					if (failure == null)
						failure = exception;
					else
						failure.addSuppressed(exception);
				}
		}
		this.meshes.clear();
		this.sources.clear();
		this.materials.clear();
		this.pipelines.clear();
		if (failure != null)
			throw failure;
	}

	private void checkThread()
	{
		if (Thread.currentThread() != this.owner)
			throw new IllegalStateException("RHI registry accessed outside its owning render thread");
	}

	private static long nextId()
	{
		return IDS.updateAndGet(Math::incrementExact);
	}

	private static final class Geometry
	{
		private final Object identity;
		private final Supplier<PGeometryData> source;
		private @Nullable PGeometryData uploaded;
		private @Nullable Buffers buffers;

		private Geometry(Object identity, Supplier<PGeometryData> source)
		{
			this.identity = identity;
			this.source = source;
		}
	}

	record Buffers(GpuBuffer vertices, GpuBuffer indices, IndexType indexType) implements AutoCloseable
	{
		@Override
		public void close()
		{
			try
			{
				this.vertices.close();
			}
			finally
			{
				this.indices.close();
			}
		}
	}
}
