/**
 * @author ArcAnc
 * Created at: 14.11.2026
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
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.content.renderer.plan.PMeshHandle;
import com.arcanc.pulselib.content.renderer.plan.PPipelineHandle;
import com.arcanc.pulselib.content.renderer.plan.PRenderCapabilities;
import com.arcanc.pulselib.content.renderer.plan.PRenderExecutor;
import com.arcanc.pulselib.content.renderer.plan.PRenderFrame;
import com.arcanc.pulselib.content.renderer.plan.PRenderPlan;
import com.arcanc.pulselib.content.renderer.plan.PTransparencyMode;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Single-object native draws for triangles_instant. No instance array, instancing layout or deferred
 * world queue is used. Each logical object gets its own ColorOverlay/transform and drawIndexed(..., 1).
 * The caller owns local queue lifetime; this executor completes submission before returning.
 */
@ApiStatus.Internal
public final class PRhiImmediateRenderer implements PRenderExecutor
{
	private final Function<PPipelineHandle, RenderType> types;
	private final Function<PMeshHandle, Object> geometry;
	private final Map<String, Supplier<GpuBufferSlice>> deformerUniforms;
	private final Map<RenderType, PPipelineHandle> bindings = new IdentityHashMap<>();
	private @Nullable PRhiResourceRegistry resources;
	private @Nullable PRhiUploadRing stream;

	public PRhiImmediateRenderer(Function<PPipelineHandle, RenderType> types, Function<PMeshHandle, Object> geometry,
			Map<String, Supplier<GpuBufferSlice>> deformerUniforms)
	{
		this.types = Objects.requireNonNull(types);
		this.geometry = Objects.requireNonNull(geometry);
		this.deformerUniforms = Map.copyOf(deformerUniforms);
		if (!this.deformerUniforms.keySet().equals(Set.of("DeformerOperations", "DeformerValues")))
			throw new IllegalArgumentException("Both instant deformer streams are required");
	}

	/** Metadata selection never uploads or consumes a queue. */
	public boolean canExecute(PRenderPlan plan)
	{
		for (PDrawGroup group : plan.groups())
		{
			var pipeline = this.types.apply(group.pipeline()).pipeline();
			var shader = pipeline.getVertexShader();
			if (!shader.getNamespace().equals("pulselib") || !shader.getPath().equals("core/triangles_instant") ||
					pipeline.getDepthStencilState() == null || pipeline.getDepthStencilState().writeDepth() != group.writeDepth() ||
					pipeline.getVertexFormatBinding(0).getVertexSize() != 24)
				return false;
			var uniforms = BindGroupLayout.flattenUniforms(pipeline.getBindGroupLayouts());
			if (uniforms.stream().noneMatch(uniform -> uniform.name().equals("ColorOverlay")) ||
					uniforms.stream().anyMatch(uniform -> uniform.name().equals("InstanceData")))
				return false;
			if ((long)group.command().firstInstance() + group.command().instanceCount() > plan.instances().size())
				throw new IllegalArgumentException("Instant draw exceeds its local object range");
			for (int i = 0; i < group.command().instanceCount(); i++)
			{
				PInstanceHeader object = plan.instances().get(group.command().firstInstance() + i);
				if (object.auxiliaryOperationCount() < 0 || object.auxiliaryOperationCount() > 8 ||
						(object.auxiliaryOperationCount() > 0 && (object.auxiliaryOperationOffset() < 0 || object.auxiliaryValueOffset() < 0)))
					throw new IllegalArgumentException("Invalid instant deformation range");
			}
		}
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
		RenderSystem.assertOnRenderThread();
		if (plan.isEmpty())
			return;
		if (!canExecute(plan))
			throw new UnsupportedOperationException("Plan is not a single-object instant plan");
		var device = RenderSystem.getDevice();
		if (this.resources == null)
		{
			this.resources = new PRhiResourceRegistry(device);
			this.stream = new PRhiUploadRing(device);
		}
		PRhiResourceRegistry resources = Objects.requireNonNull(this.resources);
		List<PDrawGroup> groups = new ArrayList<>();
		Map<PMeshHandle, PGeometryData> data = new HashMap<>();
		for (PDrawGroup group : plan.groups())
		{
			RenderType type = this.types.apply(group.pipeline());
			PPipelineHandle pipeline = this.bindings.computeIfAbsent(type,
					source -> new PRhiNativeMaterial(source, 1, PTransparencyMode.ORDINARY, this.deformerUniforms).register(resources));
			PMeshHandle mesh = switch (this.geometry.apply(group.mesh()))
			{
				case PBakedMesh baked -> resources.mesh(baked);
				case PGeometryData geometry -> resources.geometry(geometry);
				case PGeometrySource source -> resources.dynamic(source);
				default -> throw new IllegalArgumentException("Instant mesh requires portable geometry");
			};
			PGeometryData geometry = data.computeIfAbsent(mesh, resources::geometryData);
			if (geometry.vertexStride() != 24 || (long)group.command().firstIndex() + group.command().indexCount() > geometry.indexCount())
				throw new IllegalArgumentException("Instant geometry range/layout is invalid");
			groups.add(new PDrawGroup(pipeline, mesh, group.command(), group.writeDepth(), group.instances()));
		}
		PRenderPlan local = new PRenderPlan(groups, plan.instances());
		int alignment = device.getDeviceInfo().limits().minUniformOffsetAlignment();
		List<Draw> draws = new ArrayList<>();
		int size = 64;
		for (PDrawGroup group : groups)
			if (group.command().indexCount() != 0)
				for (int i = 0; i < group.command().instanceCount(); i++)
				{
					int transformOffset = align(size, alignment);
					int colorOffset = align(Math.addExact(transformOffset, 176), alignment);
					size = Math.addExact(colorOffset, 48);
					draws.add(new Draw(group, group.command().firstInstance() + i, transformOffset, colorOffset));
				}
		if (draws.isEmpty())
			return;
		ByteBuffer bytes = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
		Std140Builder.intoBuffer(bytes).putMat4f(frame.projection());
		for (Draw draw : draws)
		{
			PInstanceHeader object = local.instances().get(draw.objectIndex());
			bytes.position(draw.transformOffset());
			Std140Builder.intoBuffer(bytes).putMat4f(new Matrix4f(frame.modelView()).mul(object.transform())).
					putVec4(1, 1, 1, 1).putVec3(0, 0, 0).putMat4f(new Matrix4f());
			bytes.position(draw.colorOffset());
			writeColorOverlay(bytes, object);
		}
		bytes.clear();
		Map<PPipelineHandle, PRhiPassState> passes = new HashMap<>();
		for (Draw draw : draws)
			passes.computeIfAbsent(draw.group().pipeline(), resources::prepare);
		var encoder = device.createCommandEncoder();
		PRhiUploadRing stream = Objects.requireNonNull(this.stream);
		var upload = stream.upload(encoder, bytes);
		try
		{
			for (Draw draw : draws)
			{
				PDrawGroup group = draw.group();
				var buffers = resources.resolve(group.mesh(), data.get(group.mesh()));
				var state = passes.get(group.pipeline());
				try (var pass = encoder.createRenderPass(() -> "PulseLib instant object", state.target().color(), Optional.empty(), state.target().depth(), OptionalDouble.empty()))
				{
					pass.setPipeline(PRhiTargetPipelines.select(resources.material(group.pipeline()).pipeline(), state.target().depth() != null));
					if (state.scissor().enabled())
						pass.enableScissor(state.scissor().x(), state.scissor().y(), state.scissor().width(), state.scissor().height());
					else
						pass.disableScissor();
					pass.setUniform("Projection", upload.slice(0, 64));
					pass.setUniform("DynamicTransforms", upload.slice(draw.transformOffset(), 176));
					pass.setUniform("ColorOverlay", upload.slice(draw.colorOffset(), 48));
					state.uniforms().forEach(pass::setUniform);
					state.textures().forEach((name, texture) -> pass.bindTexture(name, texture.view(), texture.sampler()));
					pass.setVertexBuffer(0, buffers.vertices().slice());
					pass.setIndexBuffer(buffers.indices(), buffers.indexType());
					pass.drawIndexed(group.command().indexCount(), 1, group.command().firstIndex(), group.command().baseVertex(), 0);
				}
			}
		}
		finally
		{
			stream.submitted(encoder);
		}
	}

	private static int align(int offset, int alignment)
	{
		return Math.toIntExact(((long)offset + alignment - 1) / alignment * alignment);
	}

	private static void writeColorOverlay(ByteBuffer bytes, PInstanceHeader object)
	{
		int color = object.packedColor();
		bytes.putFloat((color >> 16 & 255) / 255f).putFloat((color >> 8 & 255) / 255f).
				putFloat((color & 255) / 255f).putFloat((color >>> 24) / 255f);
		bytes.putFloat(object.packedLight() >> 4 & 15).putFloat(object.packedLight() >> 20 & 15);
		bytes.putInt(object.packedOverlay() & 65535).putInt(object.packedOverlay() >>> 16);
		bytes.putInt(object.auxiliaryOperationOffset()).putInt(object.auxiliaryValueOffset()).putInt(object.auxiliaryOperationCount()).putInt(0);
	}

	@Override
	public void cleanup()
	{
		RenderSystem.assertOnRenderThread();
		try
		{
			if (this.stream != null)
				this.stream.close();
		}
		finally
		{
			try
			{
				if (this.resources != null)
					this.resources.close();
			}
			finally
			{
				this.stream = null;
				this.resources = null;
				this.bindings.clear();
			}
		}
	}

	private record Draw(PDrawGroup group, int objectIndex, int transformOffset, int colorOffset)
	{
	}
}
