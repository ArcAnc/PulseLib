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
import com.arcanc.pulselib.content.renderer.plan.PGeometrySource;
import com.arcanc.pulselib.content.renderer.plan.PMeshHandle;
import com.arcanc.pulselib.content.renderer.plan.PPipelineHandle;
import com.arcanc.pulselib.content.renderer.plan.PTransparencyMode;
import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.GpuDevice;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.EnumMap;
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
	private final Map<RenderType, Map<PTransparencyMode, PPipelineHandle>> renderTypes = new IdentityHashMap<>();
	private final Map<PPipelineHandle, RenderType> pipelineTypes = new HashMap<>();
	private final Map<PPipelineHandle, Supplier<PRhiPassState>> preparedPasses = new HashMap<>();
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
		return this.pipelines.computeIfAbsent(Objects.requireNonNull(material), this::registerPipeline);
	}

	/**
	 * Binds the queue's native render-type identity and explicit transparency policy to RHI pass resources.
	 * Reuse one material with target/texture suppliers that resolve the current context. Changing pass
	 * resources requires cleanup/rebinding; silently retaining a previous target would leak GUI/world state.
	 */
	public PPipelineHandle pipeline(RenderType type, PRhiMaterial material)
	{
		checkThread();
		Objects.requireNonNull(type);
		Objects.requireNonNull(material);
		if (type.pipeline() != material.pipeline())
			throw new IllegalArgumentException("RHI material does not belong to this native render type");
		var bindings = this.renderTypes.computeIfAbsent(type, ignored -> new EnumMap<>(PTransparencyMode.class));
		PPipelineHandle handle = bindings.get(material.transparency());
		if (handle != null)
		{
			if (this.materials.get(handle) != material)
				throw new IllegalStateException("Native render type/policy already has different RHI pass resources");
			return handle;
		}
		handle = registerPipeline(material);
		bindings.put(material.transparency(), handle);
		this.pipelineTypes.put(handle, type);
		return handle;
	}

	/** Resolves a native queue binding without encoding its transparency policy in a replacement type. */
	public RenderType pipelineType(PPipelineHandle handle)
	{
		checkThread();
		RenderType type = this.pipelineTypes.get(handle);
		if (type == null)
			throw new IllegalStateException("Unknown/stale native RHI render-type binding");
		return type;
	}

	/** Installs a stable preparation callback; it runs once per used binding in each plan execution. */
	public PPipelineHandle pipeline(RenderType type, PRhiMaterial material, Supplier<PRhiPassState> prepare)
	{
		Objects.requireNonNull(prepare);
		PPipelineHandle handle = pipeline(type, material);
		Supplier<PRhiPassState> previous = this.preparedPasses.putIfAbsent(handle, prepare);
		if (previous != null && previous != prepare)
			throw new IllegalStateException("Native RHI binding already has a different pass preparation callback");
		return handle;
	}

	PRhiPassState prepare(PPipelineHandle handle)
	{
		PRhiMaterial material = material(handle);
		Supplier<PRhiPassState> prepare = this.preparedPasses.get(handle);
		PRhiPassState state = prepare == null ? PRhiPassState.resolve(material) : Objects.requireNonNull(prepare.get());
		state.validate(material);
		return state;
	}

	public PTransparencyMode transparency(PPipelineHandle handle)
	{
		return material(handle).transparency();
	}

	private PPipelineHandle registerPipeline(PRhiMaterial material)
	{
		PPipelineHandle handle = new PPipelineHandle(nextId());
		this.materials.put(handle, material);
		return handle;
	}

	/** Register PBakedMesh.geometry() here; no GPU ownership is added to PBakedMesh. */
	public PMeshHandle geometry(PGeometryData data)
	{
		return register(Objects.requireNonNull(data), () -> data);
	}

	public PMeshHandle dynamic(PDynamicGeometry source)
	{
		if (!(source instanceof PGeometrySource dynamic))
			throw new IllegalArgumentException("Use an explicit CPU-data adapter for this dynamic geometry type");
		PGeometrySource canonical = Objects.requireNonNull(dynamic.geometrySource());
		if (canonical.geometrySource() != canonical)
			throw new IllegalArgumentException("Dynamic geometry facade must resolve to a non-delegating source");
		canonical.geometry();
		PMeshHandle handle = register(canonical, canonical::geometry);
		canonical.onClose(mesh(handle).closeOwner, () -> release(handle));
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

	public int indexCount(PMeshHandle handle)
	{
		return geometryData(handle).indexCount();
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
		if (mesh.identity instanceof PGeometrySource dynamic)
			dynamic.removeCloseListener(mesh.closeOwner);
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
			if (mesh.identity instanceof PGeometrySource dynamic)
				dynamic.removeCloseListener(mesh.closeOwner);
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
		this.renderTypes.clear();
		this.pipelineTypes.clear();
		this.preparedPasses.clear();
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
		private final Object closeOwner = new Object();
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
