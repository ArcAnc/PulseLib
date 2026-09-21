/**
 * @author ArcAnc
 * Created at: 13.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer;

import com.arcanc.pulselib.content.model.baked.PBakedMesh;
import com.arcanc.pulselib.content.model.deformer.gpu.PGpuDeformerBuffers;
import com.arcanc.pulselib.content.renderer.plan.PDrawGroup;
import com.arcanc.pulselib.content.renderer.plan.PRenderPlan;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.PResourceCache;
import com.arcanc.pulselib.util.helpers.PLibRenderHelper;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

final class RhiDrawExecutor
{
	private static final int INSTANCE_STRIDE = new Std140SizeCalculator().putMat4f().putVec4().putVec2().putVec2().putIVec4().get();
	private static final int MAX_INSTANCES_PER_DRAW = 512;
	private final Map<RenderTarget, RhiWeightedBlendedOit> weightedBlendedOit = new IdentityHashMap<>();
	private @Nullable MappableRingBuffer instanceBuffer;

	void execute(PRenderPlan plan)
	{
		if (plan.isEmpty())
			return;
		Minecraft minecraft = PLibRenderHelper.mc();
		Matrix4f modelView = RenderSystem.getModelViewMatrixCopy();
		TextureAtlas atlas = PResourceCache.getTextureAtlas();
		GpuTextureView lightTexture = minecraft.gameRenderer.levelLightmap();
		OverlayTexture overlayTexture = minecraft.gameRenderer.overlayTexture();
		PGpuDeformerBuffers.Bindings bindings = PGpuDeformerBuffers.upload();
		GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(
				modelView, new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f());
		UploadedInstances instanceData = this.uploadInstances(plan.groups());
		List<IndexedGroup> standardGroups = new ArrayList<>();
		List<IndexedGroup> oitGroups = new ArrayList<>();
		for (int groupIndex = 0; groupIndex < plan.groups().size(); groupIndex++)
		{
			PDrawGroup group = plan.groups().get(groupIndex);
			IndexedGroup indexed = new IndexedGroup(groupIndex, group);
			if (!group.writeDepth() && PRenderTypes.usesOit(group.pipeline()))
				oitGroups.add(indexed);
			else
				standardGroups.add(indexed);
		}
		try
		{
			drawStandardGroups(standardGroups, dynamicTransforms, instanceData, atlas, lightTexture, overlayTexture, bindings);
			drawOitGroups(oitGroups, dynamicTransforms, instanceData, atlas, lightTexture, overlayTexture, bindings);
		}
		finally
		{
			this.instanceBuffer.rotate();
		}
	}

	void compositeOit()
	{
		this.weightedBlendedOit.values().forEach(RhiWeightedBlendedOit :: composite);
	}

	private void drawStandardGroups(List<IndexedGroup> groups, GpuBufferSlice dynamicTransforms,
	                                UploadedInstances instanceData, TextureAtlas atlas, GpuTextureView lightTexture,
	                                OverlayTexture overlayTexture, PGpuDeformerBuffers.Bindings bindings)
	{
		RenderTarget activeTarget = null;
		RenderPass activePass = null;
		try
		{
			for (IndexedGroup indexed : groups)
			{
				PDrawGroup group = indexed.group();
				RenderTarget target = group.pipeline().outputTarget().getRenderTarget();
				if (target != activeTarget)
				{
					if (activePass != null)
						activePass.close();
					activeTarget = target;
					activePass = createPass(target, group.mesh());
				}
				draw(activePass, group, group.pipeline().pipeline(), dynamicTransforms,
						instanceData.slice(indexed.index()), atlas, lightTexture, overlayTexture, bindings);
			}
		}
		finally
		{
			if (activePass != null)
				activePass.close();
		}
	}

	private void drawOitGroups(List<IndexedGroup> groups, GpuBufferSlice dynamicTransforms,
	                           UploadedInstances instanceData, TextureAtlas atlas, GpuTextureView lightTexture,
	                           OverlayTexture overlayTexture, PGpuDeformerBuffers.Bindings bindings)
	{
		Map<RenderTarget, OitTargetBatch> targetBatches = new LinkedHashMap<>();
		for (IndexedGroup indexed : groups)
		{
			RenderTarget target = indexed.group().pipeline().outputTarget().getRenderTarget();
			RenderTargetAttachments attachments = attachments(target, indexed.group().pipeline());
			if (attachments.depth() == null)
			{
				drawFallback(indexed, dynamicTransforms, instanceData, atlas, lightTexture, overlayTexture, bindings);
				continue;
			}
			targetBatches.computeIfAbsent(target, ignored -> new OitTargetBatch(attachments)).groups().add(indexed);
		}
		for (Map.Entry<RenderTarget, OitTargetBatch> entry : targetBatches.entrySet())
			drawOitTarget(entry.getKey(), entry.getValue(), dynamicTransforms, instanceData, atlas, lightTexture, overlayTexture, bindings);
	}

	private void drawOitTarget(RenderTarget target, OitTargetBatch batch, GpuBufferSlice dynamicTransforms,
	                           UploadedInstances instanceData, TextureAtlas atlas, GpuTextureView lightTexture,
	                           OverlayTexture overlayTexture, PGpuDeformerBuffers.Bindings bindings)
	{
		RhiWeightedBlendedOit oit = this.weightedBlendedOit.computeIfAbsent(target, ignored -> new RhiWeightedBlendedOit());
		RenderTargetAttachments attachments = batch.attachments();
		if (!oit.begin(attachments.color(), attachments.depth()))
		{
			for (IndexedGroup indexed : batch.groups())
				drawFallback(indexed, dynamicTransforms, instanceData, atlas, lightTexture, overlayTexture, bindings);
			return;
		}

		try (RenderPass depthPass = oit.createDepthPass(0, batch.groups().getFirst().group().mesh().uuid()::toString))
		{
			for (IndexedGroup indexed : batch.groups())
			{
				PDrawGroup group = indexed.group();
				draw(depthPass, group, PRenderTypes.RenderPipelinesProvider.TRIANGLES_OIT_DEPTH, dynamicTransforms,
						instanceData.slice(indexed.index()), atlas, lightTexture, overlayTexture, bindings);
			}
		}

		try (RenderPass depthPass = oit.createDepthPass(1, batch.groups().getFirst().group().mesh().uuid()::toString))
		{
			for (IndexedGroup indexed : batch.groups())
			{
				PDrawGroup group = indexed.group();
				draw(depthPass, group, PRenderTypes.RenderPipelinesProvider.TRIANGLES_OIT_DEPTH_PEEL, dynamicTransforms,
						instanceData.slice(indexed.index()), atlas, lightTexture, overlayTexture, bindings, oit.layerDepthView(0));
			}
		}

		for (int layer = 0; layer < RhiWeightedBlendedOit.LAYER_COUNT; layer++)
		{
			try (RenderPass accumulationPass = oit.createAccumulationPass(layer, batch.groups().getFirst().group().mesh().uuid()::toString))
			{
				for (IndexedGroup indexed : batch.groups())
				{
					PDrawGroup group = indexed.group();
					draw(accumulationPass, group, PRenderTypes.oitPipeline(group.pipeline()), dynamicTransforms,
							instanceData.slice(indexed.index()), atlas, lightTexture, overlayTexture, bindings, oit.layerDepthView(layer));
					oit.markContent(layer);
				}
			}
		}
	}

	private static void drawFallback(IndexedGroup indexed, GpuBufferSlice dynamicTransforms, UploadedInstances instanceData,
	                                 TextureAtlas atlas, GpuTextureView lightTexture, OverlayTexture overlayTexture,
	                                 PGpuDeformerBuffers.Bindings bindings)
	{
		PDrawGroup group = indexed.group();
		RenderTarget target = group.pipeline().outputTarget().getRenderTarget();
		try (RenderPass pass = createPass(target, group.mesh()))
		{
			draw(pass, group, group.pipeline().pipeline(), dynamicTransforms,
					instanceData.slice(indexed.index()), atlas, lightTexture, overlayTexture, bindings);
		}
	}

	void cleanup()
	{
		this.weightedBlendedOit.values().forEach(RhiWeightedBlendedOit :: close);
		this.weightedBlendedOit.clear();
		if (this.instanceBuffer != null)
		{
			this.instanceBuffer.close();
			this.instanceBuffer = null;
		}
	}

	private UploadedInstances uploadInstances(List<PDrawGroup> groups)
	{
		int size = 0;
		List<Integer> offsets = new ArrayList<>(groups.size());
		for (PDrawGroup group : groups)
		{
			size = alignUniformOffset(size);
			offsets.add(size);
			size += group.instances().size() * INSTANCE_STRIDE;
		}
		size = Math.max(INSTANCE_STRIDE, size);
		if (this.instanceBuffer == null || this.instanceBuffer.size() < size)
		{
			if (this.instanceBuffer != null)
				this.instanceBuffer.close();
			this.instanceBuffer = new MappableRingBuffer(() -> PLibDatabase.rl("instance_data").toLanguageKey(),
					GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE, size);
		}
		try (GpuBufferSlice.MappedView view = this.instanceBuffer.currentBuffer().map(false, true))
		{
			for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++)
			{
				ByteBuffer groupBytes = view.data().duplicate();
				groupBytes.order(view.data().order());
				groupBytes.position(offsets.get(groupIndex));
				Std140Builder builder = Std140Builder.intoBuffer(groupBytes);
				for (PRenderQueue.InstanceData instance : groups.get(groupIndex).instances())
					builder.putMat4f(instance.posMatrix()).
							putVec4(ARGB.red(instance.packedColor()) / 255f, ARGB.green(instance.packedColor()) / 255f,
									ARGB.blue(instance.packedColor()) / 255f, ARGB.alpha(instance.packedColor()) / 255f).
							putVec2(LightCoordsUtil.block(instance.packedLight()), LightCoordsUtil.sky(instance.packedLight())).
							putVec2(instance.packedOverlay() & 0xFFFF, instance.packedOverlay() >> 16 & 0xFFFF).
							putIVec4(instance.deformerOperationOffset(), instance.deformerValueOffset(), instance.deformerOperationCount(), 0);
			}
		}
		return new UploadedInstances(this.instanceBuffer.currentBuffer(), offsets, groups);
	}

	private static int alignUniformOffset(int offset)
	{
		return (offset + 255) & ~255;
	}

	private static RenderPass createPass(RenderTarget target, PBakedMesh mesh)
	{
		GpuTextureView colorTexture = RenderSystem.outputColorTextureOverride != null
				? RenderSystem.outputColorTextureOverride : target.getColorTextureView();
		GpuTextureView depthTexture = target.useDepth
				? (RenderSystem.outputDepthTextureOverride != null ? RenderSystem.outputDepthTextureOverride : target.getDepthTextureView())
				: null;
		return RenderSystem.getDevice().createCommandEncoder().createRenderPass(mesh.uuid()::toString,
				colorTexture, Optional.empty(), depthTexture, OptionalDouble.empty());
	}

	private static void draw(RenderPass pass, PDrawGroup group, RenderPipeline pipeline,
	                         GpuBufferSlice dynamicTransforms, GpuBufferSlice instanceData,
						 TextureAtlas atlas, GpuTextureView lightTexture, OverlayTexture overlayTexture, PGpuDeformerBuffers.Bindings deformerBuffers)
	{
		draw(pass, group, pipeline, dynamicTransforms, instanceData, atlas, lightTexture, overlayTexture, deformerBuffers, null);
	}

	private static void draw(RenderPass pass, PDrawGroup group, RenderPipeline pipeline,
	                         GpuBufferSlice dynamicTransforms, GpuBufferSlice instanceData,
	                         TextureAtlas atlas, GpuTextureView lightTexture, OverlayTexture overlayTexture,
	                         PGpuDeformerBuffers.Bindings deformerBuffers, @Nullable GpuTextureView layerDepth)
	{
		if (group.instances().isEmpty())
			return;
		PBakedMesh mesh = group.mesh();
		pass.setPipeline(pipeline);
		RenderSystem.bindDefaultUniforms(pass);
		pass.setUniform("DynamicTransforms", dynamicTransforms);
		pass.setUniform("DeformerOperations", deformerBuffers.operations());
		pass.setUniform("DeformerValues", deformerBuffers.values());
		applyActiveScissor(pass);
		pass.bindTexture("Sampler0", atlas.getTextureView(), atlas.getSampler());
		pass.bindTexture("Sampler1", overlayTexture.getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
		pass.bindTexture("Sampler2", lightTexture, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
		if (layerDepth != null)
			pass.bindTexture("LayerDepthSampler", layerDepth, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
		pass.setVertexBuffer(0, mesh.vbo().slice());
		pass.setIndexBuffer(mesh.indices(), mesh.indexType());
		for (int offset = 0; offset < group.instances().size();)
		{
			int count = Math.min(MAX_INSTANCES_PER_DRAW, group.instances().size() - offset);
			pass.setUniform("InstanceData", instanceData.slice((long)offset * INSTANCE_STRIDE, (long)count * INSTANCE_STRIDE));
			pass.drawIndexed(mesh.indicesCount(), count, 0, 0, 0);
			offset += count;
		}
	}

	private static RenderTargetAttachments attachments(RenderTarget target, RenderType type)
	{
		GpuTextureView color = RenderSystem.outputColorTextureOverride != null ?
				RenderSystem.outputColorTextureOverride : target.getColorTextureView();
		GpuTextureView depth = target.useDepth ?
				(RenderSystem.outputDepthTextureOverride != null ? RenderSystem.outputDepthTextureOverride : target.getDepthTextureView()) : null;
		if (color == null)
			throw new IllegalStateException("Render type " + type + " has no color attachment");
		return new RenderTargetAttachments(color, depth);
	}

	private record UploadedInstances(GpuBuffer buffer, List<Integer> offsets, List<PDrawGroup> groups)
	{
		private GpuBufferSlice slice(int groupIndex)
		{
			return this.buffer.slice(this.offsets.get(groupIndex), (long)this.groups.get(groupIndex).instances().size() * INSTANCE_STRIDE);
		}
	}

	private record IndexedGroup(int index, PDrawGroup group)
	{
	}

	private record RenderTargetAttachments(GpuTextureView color, @Nullable GpuTextureView depth)
	{
	}

	private record OitTargetBatch(RenderTargetAttachments attachments, List<IndexedGroup> groups)
	{
		private OitTargetBatch(RenderTargetAttachments attachments)
		{
			this(attachments, new ArrayList<>());
		}
	}

	private static void applyActiveScissor(RenderPass pass)
	{
		ScissorState scissor = RenderSystem.getScissorStateForRenderTypeDraws();
		if (scissor.enabled())
			pass.enableScissor(scissor.x(), scissor.y(), scissor.width(), scissor.height());
		else
			pass.disableScissor();
	}

}
