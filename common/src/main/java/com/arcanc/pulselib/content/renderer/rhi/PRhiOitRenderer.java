/**
 * @author ArcAnc
 * Created at: 15.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.rhi;

import com.arcanc.pulselib.content.model.baked.PBakedMesh;
import com.arcanc.pulselib.content.renderer.plan.PDrawGroup;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.arcanc.pulselib.content.renderer.plan.PGeometrySource;
import com.arcanc.pulselib.content.renderer.plan.PMeshHandle;
import com.arcanc.pulselib.content.renderer.plan.PPipelineHandle;
import com.arcanc.pulselib.content.renderer.plan.PRenderCapabilities;
import com.arcanc.pulselib.content.renderer.plan.PRenderExecutor;
import com.arcanc.pulselib.content.renderer.plan.PRenderFrame;
import com.arcanc.pulselib.content.renderer.plan.PRenderPlan;
import com.arcanc.pulselib.content.renderer.plan.PTransparencyMode;
import com.arcanc.pulselib.util.PRenderTypes;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Collects complete OIT plans until composition. Every producer participates in all four depth
 * passes before any accumulation, so separate stage flushes cannot peel against incomplete depth.
 * Frames and native preparation are captured at submission; host bindings remain borrowed.
 */
@ApiStatus.Internal
public final class PRhiOitRenderer implements PRenderExecutor
{
	private final Function<PPipelineHandle, RenderType> types;
	private final Function<PMeshHandle, Object> geometry;
	private final Map<String, Supplier<GpuBufferSlice>> streams;
	private final Map<RenderType, PPipelineHandle> materials = new IdentityHashMap<>();
	private final Map<PRhiMaterial.Attachments, PRhiWeightedBlendedOit> targets = new LinkedHashMap<>();
	private final List<Retired> retired = new ArrayList<>();
	private final Set<PMeshHandle> transientMeshes = new HashSet<>();
	private final List<Pending> pending = new ArrayList<>();
	private final Map<RenderPipeline, RenderPipeline> emissiveDepth = new IdentityHashMap<>();
	private @Nullable PRhiResourceRegistry resources;
	private @Nullable PRhiUploadRing uploads;

	public PRhiOitRenderer(Function<PPipelineHandle, RenderType> types, Function<PMeshHandle, Object> geometry,
			Map<String, Supplier<GpuBufferSlice>> streams)
	{
		this.types = Objects.requireNonNull(types);
		this.geometry = Objects.requireNonNull(geometry);
		this.streams = Map.copyOf(streams);
		if (!this.streams.keySet().equals(Set.of("DeformerOperations", "DeformerValues")))
			throw new IllegalArgumentException("Both native OIT deformer streams are required");
	}

	/** CPU-only selection; instant and arbitrary foreign shader layouts never enter OIT. */
	public boolean canExecute(PRenderPlan plan)
	{
		for (var group : plan.groups())
			if (group.writeDepth() || !PRenderTypes.usesOit(this.types.apply(group.pipeline())))
				return false;
		return true;
	}

	@Override
	public PRenderCapabilities capabilities()
	{
		return new PRenderCapabilities(PRenderCapabilities.StorageMode.CPU,
				PRenderCapabilities.SubmissionMode.DIRECT, PRenderCapabilities.UploadMode.WRITE);
	}

	@Override
	public void execute(PRenderPlan plan, PRenderFrame frame)
	{
		execute(plan, frame, null);
	}

	/** Captures an explicit opaque depth independently of the eventual composite destination. */
	public void execute(PRenderPlan plan, PRenderFrame frame, @Nullable GpuTextureView opaqueDepth)
	{
		RenderSystem.assertOnRenderThread();
		reap(false);
		if (plan.isEmpty())
			return;
		if (!canExecute(plan))
			throw new UnsupportedOperationException("OIT plan requires a canonical translucent material");
		var device = RenderSystem.getDevice();
		if (this.resources == null)
		{
			this.resources = new PRhiResourceRegistry(device);
			this.uploads = new PRhiUploadRing(device);
		}
		PRhiResourceRegistry resources = Objects.requireNonNull(this.resources);
		List<PDrawGroup> groups = new ArrayList<>();
		Map<PPipelineHandle, PRhiPassState> states = new HashMap<>();
		Map<PMeshHandle, PGeometryData> data = new HashMap<>();
		for (var group : plan.groups())
		{
			RenderType type = this.types.apply(group.pipeline());
			var pipeline = this.materials.computeIfAbsent(type, source -> new PRhiNativeMaterial(source,
					PRhiInstanceLayout.PORTABLE_CAPACITY, PTransparencyMode.OIT, this.streams).register(resources));
			var mesh = switch (this.geometry.apply(group.mesh()))
			{
				case PBakedMesh baked -> resources.mesh(baked);
				case PGeometryData source -> resources.geometry(source);
				case PGeometrySource source ->
				{
					// Freeze this revision: another producer may update/close its source before composition.
					var revision = source.geometry();
					var snapshot = resources.geometry(new PGeometryData(revision.vertices(), revision.indices(), revision.vertexStride(), revision.indexCount(), revision.indexType()));
					this.transientMeshes.add(snapshot);
					yield snapshot;
				}
				default -> throw new IllegalArgumentException("OIT geometry requires a portable CPU source");
			};
			var geometry = data.computeIfAbsent(mesh, resources::geometryData);
			if ((long)group.command().firstIndex() + group.command().indexCount() > geometry.indexCount() || geometry.vertexStride() != 24)
				throw new IllegalArgumentException("OIT geometry range/layout is invalid");
			states.computeIfAbsent(pipeline, key ->
			{
				var prepared = resources.prepare(key);
				var target = prepared.target();
				if (opaqueDepth != null)
					target = new PRhiMaterial.Attachments(target.color(), opaqueDepth);
				if (target.depth() == null || target.color().getWidth(0) != target.depth().getWidth(0) ||
						target.color().getHeight(0) != target.depth().getHeight(0))
					throw new IllegalArgumentException("OIT requires matching opaque depth; no alpha fallback is permitted");
				return new PRhiPassState(target, prepared.uniforms(), prepared.textures(), prepared.scissor());
			});
			groups.add(new PDrawGroup(pipeline, mesh, group.command(), false, group.instances()));
		}
		var local = new PRenderPlan(groups, plan.instances());
		for (var group : groups)
			for (int index = 0; index < group.command().instanceCount(); index++)
			{
				var object = local.instances().get(group.command().firstInstance() + index);
				if (object.auxiliaryOperationCount() < 0 || object.auxiliaryOperationCount() > 8 ||
						(object.auxiliaryOperationCount() > 0 && (object.auxiliaryOperationOffset() < 0 || object.auxiliaryValueOffset() < 0)))
					throw new IllegalArgumentException("Invalid OIT deformation range");
			}
		var layout = new PRhiInstanceLayout(device.getDeviceInfo().limits().minUniformOffsetAlignment(), PRhiInstanceLayout.PORTABLE_CAPACITY, 16384);
		var packet = layout.pack(local);
		Std140Builder.intoBuffer(packet.bytes()).putMat4f(frame.modelView()).putVec4(1, 1, 1, 1).putVec3(0, 0, 0).putMat4f(new Matrix4f());
		packet.bytes().position(packet.projectionOffset());
		Std140Builder.intoBuffer(packet.bytes()).putMat4f(frame.projection());
		packet.bytes().clear();
		this.pending.add(new Pending(packet, states, data));
	}

	/** Composites into original targets. All submissions are consumed, including on an execution error. */
	public void composite()
	{
		composite(null, null);
	}

	public void composite(GpuTextureView destination)
	{
		composite(destination, null);
	}

	public void compositeFabulous(GpuTextureView destination, List<RenderTarget> vanilla)
	{
		PRhiWeightedBlendedOit.validateFabulousTargets(destination, vanilla);
		composite(destination, vanilla);
	}

	private void composite(@Nullable GpuTextureView destination, @Nullable List<RenderTarget> vanilla)
	{
		RenderSystem.assertOnRenderThread();
		if (this.pending.isEmpty())
			return;
		if (destination != null)
			for (var execution : this.pending)
				for (var state : execution.states().values())
					if (destination.isClosed() || destination.getWidth(0) != state.target().color().getWidth(0) ||
							destination.getHeight(0) != state.target().color().getHeight(0))
						throw new IllegalArgumentException("OIT destination dimensions differ; frame remains pending");
		var resources = Objects.requireNonNull(this.resources);
		var device = RenderSystem.getDevice();
		int alignment = device.getDeviceInfo().limits().minUniformOffsetAlignment();
		int size = 0;
		Map<PRhiMaterial.Attachments, List<Draw>> draws = new LinkedHashMap<>();
		Map<String, GpuBufferSlice> latestStreams = new HashMap<>();
		this.streams.forEach((name, supplier) -> latestStreams.put(name, supplier.get()));
		for (var execution : this.pending)
		{
			size = Math.toIntExact(((long)size + alignment - 1) / alignment * alignment);
			for (var batch : execution.packet().batches())
			{
				var captured = execution.states().get(batch.group().pipeline());
				var uniforms = new HashMap<>(captured.uniforms());
				uniforms.putAll(latestStreams);
				var state = new PRhiPassState(captured.target(), uniforms, captured.textures(), captured.scissor());
				var buffers = resources.resolve(batch.group().mesh(), execution.geometry().get(batch.group().mesh()));
				draws.computeIfAbsent(state.target(), key -> new ArrayList<>()).add(new Draw(batch, state, buffers, size));
			}
			size = Math.addExact(size, execution.packet().bytes().capacity());
		}
		ByteBuffer bytes = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
		for (var execution : this.pending)
		{
			int offset = Math.toIntExact(((long)bytes.position() + alignment - 1) / alignment * alignment);
			bytes.position(offset);
			bytes.put(execution.packet().bytes().duplicate().clear());
		}
		bytes.flip();
		var encoder = device.createCommandEncoder();
		var uploads = Objects.requireNonNull(this.uploads);
		var upload = uploads.upload(encoder, bytes);
		try
		{
			for (var entry : draws.entrySet())
			{
				var oit = this.targets.computeIfAbsent(entry.getKey(), key -> new PRhiWeightedBlendedOit());
				if (!oit.begin(entry.getKey().color(), entry.getKey().depth()))
					throw new IllegalStateException("Native OIT frame could not begin");
				for (int layer = 0; layer < PRhiWeightedBlendedOit.LAYER_COUNT; layer++)
				{
					try (var pass = oit.createDepthPass(layer, () -> "PulseLib RHI OIT depth"))
					{
						for (var draw : entry.getValue())
						{
							boolean emissive = resources.material(draw.batch().group().pipeline()).pipeline().getShaderDefines().flags().contains("EMISSIVE");
							var pipeline = layer == 0 ? PRenderTypes.ShadersProvider.trianglesOitDepth(emissive) : PRenderTypes.ShadersProvider.trianglesOitDepthPeel(emissive);
							if (emissive)
								pipeline = emissive(pipeline);
							bind(pass, draw, pipeline, upload);
							pass.bindTexture("SceneDepthSampler", Objects.requireNonNull(entry.getKey().depth()), deviceSampler());
							if (layer > 0)
								pass.bindTexture("LayerDepthSampler", oit.layerDepthView(layer - 1), deviceSampler());
							draw(pass, draw);
						}
					}
				}
				for (int layer = 0; layer < PRhiWeightedBlendedOit.LAYER_COUNT; layer++)
				{
					try (var pass = oit.createAccumulationPass(layer, () -> "PulseLib RHI OIT accumulation"))
					{
						for (var draw : entry.getValue())
						{
							boolean emissive = resources.material(draw.batch().group().pipeline()).pipeline().getShaderDefines().flags().contains("EMISSIVE");
							bind(pass, draw, PRenderTypes.ShadersProvider.trianglesOit(emissive), upload);
							pass.bindTexture("LayerDepthSampler", oit.layerDepthView(layer), deviceSampler());
							draw(pass, draw);
						}
					}
					oit.markContent(layer);
				}
				if (vanilla != null)
					oit.compositeFabulous(Objects.requireNonNull(destination), vanilla);
				else if (destination != null)
					oit.composite(destination);
				else
					oit.composite();
			}
		}
		finally
		{
			try
			{
				uploads.submitted(encoder);
				if (!this.transientMeshes.isEmpty())
				{
					this.retired.add(new Retired(encoder.createFence(), List.copyOf(this.transientMeshes)));
					this.transientMeshes.clear();
				}
			}
			finally
			{
				discard();
			}
		}
	}

	private RenderPipeline emissive(RenderPipeline source)
	{
		return this.emissiveDepth.computeIfAbsent(source, pipeline ->
		{
			// EMISSIVE eliminates light sampling from the vertex shader. Remove its whole optional
			// lighting layout as well: NeoForge validates every declared binding even if optimized out.
			var layouts = pipeline.getBindGroupLayouts().stream().filter(layout ->
					BindGroupLayout.flattenUniforms(List.of(layout)).stream().noneMatch(uniform -> uniform.name().equals("Lighting"))).toList();
			var snippet = new RenderPipeline.Snippet(Optional.of(pipeline.getVertexShader()), Optional.of(pipeline.getFragmentShader()),
					Optional.of(pipeline.getShaderDefines()), Optional.of(layouts), pipeline.getColorTargetStates(),
					pipeline.getColorTargetStates().length, Optional.ofNullable(pipeline.getDepthStencilState()), Optional.of(pipeline.getPolygonMode()),
					Optional.of(pipeline.isCull()), pipeline.getVertexFormatBindings(), Optional.of(pipeline.getPrimitiveTopology()));
			return RenderPipeline.builder(snippet).withLocation(pipeline.getLocation().withSuffix("_emissive")).withShaderDefine("EMISSIVE").build();
		});
	}

	private static com.mojang.blaze3d.textures.GpuSampler deviceSampler()
	{
		return RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
	}

	private static void bind(RenderPass pass, Draw draw, RenderPipeline pipeline, com.mojang.blaze3d.buffers.GpuBuffer upload)
	{
		pass.setPipeline(pipeline);
		var scissor = draw.state().scissor();
		if (scissor.enabled())
			pass.enableScissor(scissor.x(), scissor.y(), scissor.width(), scissor.height());
		else
			pass.disableScissor();
		pass.setUniform("DynamicTransforms", upload.slice(draw.offset(), 176));
		// Projection follows DynamicTransforms at the layout's native-aligned offset.
		int alignment = RenderSystem.getDevice().getDeviceInfo().limits().minUniformOffsetAlignment();
		int projectionOffset = (176 + alignment - 1) / alignment * alignment;
		pass.setUniform("Projection", upload.slice(draw.offset() + projectionOffset, 64));
		pass.setUniform("InstanceData", upload.slice(draw.offset() + draw.batch().uniformOffset(), (long)PRhiInstanceLayout.PORTABLE_CAPACITY * PRhiInstanceLayout.STRIDE));
		draw.state().uniforms().forEach(pass::setUniform);
		draw.state().textures().forEach((name, texture) -> pass.bindTexture(name, texture.view(), texture.sampler()));
		pass.setVertexBuffer(0, draw.buffers().vertices().slice());
		pass.setIndexBuffer(draw.buffers().indices(), draw.buffers().indexType());
	}

	private static void draw(RenderPass pass, Draw draw)
	{
		var command = draw.batch().group().command();
		pass.drawIndexed(command.indexCount(), draw.batch().instanceCount(), command.firstIndex(), command.baseVertex(), 0);
	}

	private void reap(boolean wait)
	{
		if (wait && !this.retired.isEmpty())
			RenderSystem.getDevice().createCommandEncoder().submit();
		this.retired.removeIf(entry ->
		{
			if (!entry.fence().awaitCompletion(wait ? Long.MAX_VALUE : 0))
			{
				if (wait)
					throw new IllegalStateException("OIT geometry retirement fence did not complete");
				return false;
			}
			entry.fence().close();
			for (var mesh : entry.meshes())
				Objects.requireNonNull(this.resources).release(mesh);
			return true;
		});
	}

	public boolean hasPending()
	{
		return !this.pending.isEmpty();
	}

	public void discard()
	{
		this.pending.clear();
		if (this.resources != null)
			for (var mesh : this.transientMeshes)
				this.resources.release(mesh);
		this.transientMeshes.clear();
		this.targets.values().forEach(PRhiWeightedBlendedOit::discard);
		this.targets.entrySet().removeIf(entry ->
		{
			if (entry.getKey().color().isClosed() || Objects.requireNonNull(entry.getKey().depth()).isClosed())
			{
				if (this.uploads != null)
					this.uploads.close();
				reap(true);
				entry.getValue().close();
				return true;
			}
			return false;
		});
	}

	@Override
	public void cleanup()
	{
		discard();
		reap(true);
		if (this.uploads != null)
			this.uploads.close();
		this.targets.values().forEach(PRhiWeightedBlendedOit::close);
		this.targets.clear();
		if (this.resources != null)
			this.resources.close();
		this.uploads = null;
		this.resources = null;
		this.materials.clear();
	}

	private record Retired(GpuFence fence, List<PMeshHandle> meshes)
	{
	}

	private record Pending(PRhiInstanceLayout.Packet packet, Map<PPipelineHandle, PRhiPassState> states, Map<PMeshHandle, PGeometryData> geometry)
	{
	}

	private record Draw(PRhiInstanceLayout.Batch batch, PRhiPassState state, PRhiResourceRegistry.Buffers buffers, int offset)
	{
	}
}
