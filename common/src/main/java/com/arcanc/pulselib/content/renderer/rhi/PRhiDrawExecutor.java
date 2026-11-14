/**
 * @author ArcAnc
 * Created at: 13.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.rhi;

import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.arcanc.pulselib.content.renderer.plan.PMeshHandle;
import com.arcanc.pulselib.content.renderer.plan.PPipelineHandle;
import com.arcanc.pulselib.content.renderer.plan.PRenderCapabilities;
import com.arcanc.pulselib.content.renderer.plan.PRenderExecutor;
import com.arcanc.pulselib.content.renderer.plan.PRenderFrame;
import com.arcanc.pulselib.content.renderer.plan.PRenderPlan;
import com.arcanc.pulselib.content.renderer.plan.PTransparencyMode;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.systems.GpuDevice;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Direct instanced RHI execution, adapted from master RhiDrawExecutor to canonical packed plans.
 * Caller supplies current targets/textures/extra uniforms; no RenderSystem globals are changed.
 * OIT composition is a separate pending integration and is rejected explicitly, never alpha-blended.
 */
@ApiStatus.Internal
public final class PRhiDrawExecutor implements PRenderExecutor
{
	private final Thread owner = Thread.currentThread();
	private final GpuDevice device;
	private final PRhiResourceRegistry resources;
	private final PRhiInstanceLayout layout;
	private final PRhiUploadRing stream;

	/** Uses a portable 128-record block; pipeline shaders must declare the matching array size. */
	public PRhiDrawExecutor(GpuDevice device, PRhiResourceRegistry resources)
	{
		this(device, resources, new PRhiInstanceLayout(device.getDeviceInfo().limits().minUniformOffsetAlignment(),
				PRhiInstanceLayout.PORTABLE_CAPACITY, 16384));
	}

	/** Larger blocks require a matching shader and an explicitly verified uniform-byte limit. */
	public PRhiDrawExecutor(GpuDevice device, PRhiResourceRegistry resources, PRhiInstanceLayout layout)
	{
		resources.verifyDevice(device);
		if (layout.alignment() % device.getDeviceInfo().limits().minUniformOffsetAlignment() != 0)
			throw new IllegalArgumentException("RHI uniform layout does not satisfy device alignment");
		this.device = device;
		this.resources = resources;
		this.layout = layout;
		this.stream = new PRhiUploadRing(device);
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
		checkThread();
		if (plan.isEmpty())
			return;
		for (var group : plan.groups())
			if (this.resources.material(group.pipeline()).instanceCapacity() == 1 &&
					BindGroupLayout.flattenUniforms(this.resources.material(group.pipeline()).pipeline().getBindGroupLayouts()).stream().
							anyMatch(uniform -> uniform.name().equals("ColorOverlay")))
				throw new IllegalArgumentException("Instant rendering requires the single-object executor");
		PRhiInstanceLayout.Packet packet = this.layout.pack(plan);
		if (packet.batches().isEmpty())
			return;
		Map<PMeshHandle, PGeometryData> geometry = new HashMap<>();
		for (PRhiInstanceLayout.Batch batch : packet.batches())
		{
			var group = batch.group();
			var material = this.resources.material(group.pipeline());
			if (material.transparency() == PTransparencyMode.OIT)
				throw new UnsupportedOperationException("Four-layer RHI OIT integration is not installed");
			if (material.instanceCapacity() != this.layout.capacity())
				throw new IllegalArgumentException("Shader/CPU instance block capacities differ");
			var depth = material.pipeline().getDepthStencilState();
			if ((depth != null && depth.writeDepth()) != group.writeDepth())
				throw new IllegalArgumentException("Draw depth policy does not match its registered RHI pipeline");
			PGeometryData data = geometry.computeIfAbsent(group.mesh(), this.resources::geometryData);
			if ((long)group.command().firstIndex() + group.command().indexCount() > data.indexCount())
				throw new IllegalArgumentException("RHI command exceeds geometry index range");
			if (material.pipeline().getVertexFormatBinding(0).getVertexSize() != data.vertexStride())
				throw new IllegalArgumentException("RHI pipeline/geometry vertex strides differ");
			for (int i = 0; i < batch.instanceCount(); i++)
				if (plan.instances().get(batch.firstInstance() + i).auxiliaryOperationCount() > 0 &&
						(!material.uniforms().containsKey("DeformerOperations") || !material.uniforms().containsKey("DeformerValues")))
					throw new IllegalArgumentException("Deformed instances require both deformer streams");
		}
		Map<PPipelineHandle, PRhiPassState> passes = new HashMap<>();
		for (PRhiInstanceLayout.Batch batch : packet.batches())
			passes.computeIfAbsent(batch.group().pipeline(), this.resources::prepare);
		Std140Builder.intoBuffer(packet.bytes()).putMat4f(frame.modelView()).putVec4(1, 1, 1, 1).
				putVec3(0, 0, 0).putMat4f(new Matrix4f());
		packet.bytes().position(packet.projectionOffset());
		Std140Builder.intoBuffer(packet.bytes()).putMat4f(frame.projection());
		packet.bytes().clear();
		var encoder = this.device.createCommandEncoder();
		var upload = this.stream.upload(encoder, packet.bytes());
		try
		{
			for (PRhiInstanceLayout.Batch batch : packet.batches())
			{
				var group = batch.group();
				var material = this.resources.material(group.pipeline());
				var buffers = this.resources.resolve(group.mesh(), geometry.get(group.mesh()));
				var state = passes.get(group.pipeline());
				var target = state.target();
				try (var pass = encoder.createRenderPass(() -> "PulseLib RHI " + group.mesh().value(),
						target.color(), Optional.empty(), target.depth(), OptionalDouble.empty()))
				{
					pass.setPipeline(PRhiTargetPipelines.select(material.pipeline(), target.depth() != null));
					var scissor = state.scissor();
					if (scissor.enabled())
						pass.enableScissor(scissor.x(), scissor.y(), scissor.width(), scissor.height());
					else
						pass.disableScissor();
					pass.setUniform("DynamicTransforms", upload.slice(0, 176));
					pass.setUniform("Projection", upload.slice(packet.projectionOffset(), 64));
					pass.setUniform("InstanceData", upload.slice(batch.uniformOffset(), (long)this.layout.capacity() * PRhiInstanceLayout.STRIDE));
					state.uniforms().forEach(pass::setUniform);
					state.textures().forEach((name, texture) -> pass.bindTexture(name, texture.view(), texture.sampler()));
					pass.setVertexBuffer(0, buffers.vertices().slice());
					pass.setIndexBuffer(buffers.indices(), buffers.indexType());
					pass.drawIndexed(group.command().indexCount(), batch.instanceCount(),
							group.command().firstIndex(), group.command().baseVertex(), 0);
				}
			}
		}
		finally
		{
			this.stream.submitted(encoder);
		}
	}

	@Override
	public void cleanup()
	{
		checkThread();
		try
		{
			this.stream.close();
		}
		finally
		{
			this.resources.close();
		}
	}

	private void checkThread()
	{
		if (Thread.currentThread() != this.owner)
			throw new IllegalStateException("RHI executor accessed outside its owning render thread");
	}
}
