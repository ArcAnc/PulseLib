/**
 * @author ArcAnc
 * Created at: 14.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.gl;

import com.arcanc.pulselib.content.model.baked.PBakedMesh;
import com.arcanc.pulselib.content.model.deformer.gpu.PGpuDeformerBuffers;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.plan.PDrawGroup;
import com.arcanc.pulselib.content.renderer.plan.PRenderPlan;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.PTextureCache;
import com.arcanc.pulselib.util.helpers.PLibRenderHelper;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.*;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public final class PGlMultiDrawExecutor
{
	private final PGlGeometryArena geometry = new PGlGeometryArena();
	private final PGlInstanceStream instances = new PGlInstanceStream();
	private final PGlIndirectStream indirect = new PGlIndirectStream();
	private final PGlFrameArena frameArena = new PGlFrameArena(9);
	private final Map<RenderTarget, PGlWeightedBlendedOit> weightedBlendedOits = new IdentityHashMap<>();
	private @Nullable PGlWeightedBlendedOit activeWeightedBlendedOit;

	/**
	 * Performs the execute operation.
	 * @param plan the plan to use.
	 */
	public void execute(PRenderPlan<RenderType, PBakedMesh, PRenderQueue.InstanceData> plan)
	{
		if (plan.isEmpty())
			return;

		List<PRenderQueue.InstanceData> allInstances = new ArrayList<>();
		List<Draw> draws = new ArrayList<>();
		for (PDrawGroup<RenderType, PBakedMesh, PRenderQueue.InstanceData> group : plan.groups())
		{
			if (group.instances().isEmpty())
				continue;
			int baseInstance = allInstances.size();
			allInstances.addAll(group.instances());
			PBakedMesh mesh = group.mesh();
			draws.add(new Draw(group.pipeline(), mesh, this.geometry.resolve(mesh), group.instances().size(), baseInstance, group.writeDepth()));
		}
		if (draws.isEmpty())
			return;

		boolean persistent = GL.getCapabilities().GL_ARB_buffer_storage || GL.getCapabilities().OpenGL44;
		int frameSlot = persistent ? this.frameArena.begin() : 0;
		if (frameSlot < 0)
			return;
		PGlInstanceStream.Upload instanceStream = this.instances.upload(allInstances, frameSlot, persistent);
		boolean multiDraw = GL.getCapabilities().GL_ARB_multi_draw_indirect || GL.getCapabilities().OpenGL43;
		try
		{
			List<Draw> standardDraws = new ArrayList<>();
			List<Draw> oitDraws = new ArrayList<>();
			for (Draw draw : draws)
			{
				if (!draw.writeDepth() && PRenderTypes.usesOit(draw.type()))
					oitDraws.add(draw);
				else
					standardDraws.add(draw);
			}
			int maximumIndirectCommands = standardDraws.size() + oitDraws.size() * PGlWeightedBlendedOit.LAYER_COUNT * 2;
			this.indirect.begin(maximumIndirectCommands, frameSlot, persistent && multiDraw);

			drawGroups(standardDraws, instanceStream, multiDraw, OitPass.NONE);
			Map<RenderTarget, List<Draw>> oitTargetBatches = new LinkedHashMap<>();
			for (Draw draw : oitDraws)
				oitTargetBatches.computeIfAbsent(draw.type().outputTarget().getRenderTarget(), ignored -> new ArrayList<>()).add(draw);
			for (Map.Entry<RenderTarget, List<Draw>> entry : oitTargetBatches.entrySet())
			{
				List<Draw> targetDraws = entry.getValue();
				this.activeWeightedBlendedOit = this.weightedBlendedOits.computeIfAbsent(entry.getKey(), ignored -> new PGlWeightedBlendedOit());
				try
				{
					RenderTargetAttachments attachments = attachments(targetDraws.getFirst().type());
					if (activeOit().begin(attachments.color(), attachments.depth()))
					{
						for (int layer = 0; layer < PGlWeightedBlendedOit.LAYER_COUNT; layer++)
						{
							activeOit().beginDepthPass(layer);
							try
							{
								drawGroups(targetDraws, instanceStream, multiDraw,
										layer == 0 ? OitPass.DEPTH : OitPass.DEPTH_PEEL);
							}
							finally
							{
								activeOit().endPass();
							}

							activeOit().beginAccumulationPass(layer);
							try
							{
								drawGroups(targetDraws, instanceStream, multiDraw, OitPass.ACCUMULATION);
								activeOit().markContent(layer);
							}
							finally
							{
								activeOit().endPass();
							}
						}
					}
					else
						drawGroups(targetDraws, instanceStream, multiDraw, OitPass.NONE);
				}
				finally
				{
					this.activeWeightedBlendedOit = null;
				}
			}
		}
		finally
		{
			if (persistent)
				this.frameArena.finish(frameSlot);
		}
	}

	/**
	 * Performs the composite oit operation.
	 */
	public void compositeOit()
	{
		this.weightedBlendedOits.values().forEach(PGlWeightedBlendedOit :: composite);
	}

	/**
	 * Performs the cleanup operation.
	 */
	public void cleanup()
	{
		this.frameArena.awaitAll();
		this.geometry.clear();
		this.instances.close();
		this.indirect.close();
		this.weightedBlendedOits.values().forEach(PGlWeightedBlendedOit :: close);
		this.weightedBlendedOits.clear();
		this.activeWeightedBlendedOit = null;
	}

	/**
	 * Draws the groups.
	 * @param draws the draws to use.
	 * @param instanceStream the instance stream to use.
	 * @param multiDraw the multi draw to use.
	 * @param oitPass the oit pass to use.
	 */
	private void drawGroups(List<Draw> draws, PGlInstanceStream.Upload instanceStream, boolean multiDraw, OitPass oitPass)
	{
		for (int start = 0; start < draws.size();)
		{
			if (draws.get(start).writeDepth())
			{
				int end = findOpaquePipelineEnd(draws, start);
				drawOpaquePipeline(draws.subList(start, end), instanceStream, multiDraw);
				start = end;
				continue;
			}
			int end = findBatchEnd(draws, start);
			draw(draws.subList(start, end), instanceStream, multiDraw && end - start > 1, oitPass);
			start = end;
		}
	}

	/**
	 * Finds the batch end.
	 * @param draws the draws to use.
	 * @param start the start to use.
	 * @return the value produced by this operation.
	 */
	private static int findBatchEnd(List<Draw> draws, int start)
	{
		Draw first = draws.get(start);
		int end = start + 1;
		while (end < draws.size())
		{
			Draw candidate = draws.get(end);
			if (candidate.type() != first.type() || candidate.slice().page() != first.slice().page() ||
					candidate.slice().indexType() != first.slice().indexType() || candidate.writeDepth() != first.writeDepth())
				break;
			end++;
		}
		return end;
	}

	/**
	 * Finds the opaque pipeline end.
	 * @param draws the draws to use.
	 * @param start the start to use.
	 * @return the value produced by this operation.
	 */
	private static int findOpaquePipelineEnd(List<Draw> draws, int start)
	{
		RenderType type = draws.get(start).type();
		int end = start + 1;
		while (end < draws.size() && draws.get(end).writeDepth() && draws.get(end).type() == type)
			end++;
		return end;
	}

	/**
	 * Draws the opaque pipeline.
	 * @param draws the draws to use.
	 * @param instanceStream the instance stream to use.
	 * @param multiDraw the multi draw to use.
	 */
	private void drawOpaquePipeline(List<Draw> draws, PGlInstanceStream.Upload instanceStream, boolean multiDraw)
	{
		Map<ArenaKey, List<Draw>> batches = new LinkedHashMap<>();
		for (Draw draw : draws)
			batches.computeIfAbsent(new ArenaKey(draw.slice().page(), draw.slice().indexType()), ignored -> new ArrayList<>()).add(draw);
		for (List<Draw> batch : batches.values())
			draw(batch, instanceStream, multiDraw && batch.size() > 1, OitPass.NONE);
	}

	/**
	 * Performs the draw operation.
	 * @param batch the batch to use.
	 * @param instanceStream the instance stream to use.
	 * @param multiDraw the multi draw to use.
	 * @param oitPass the oit pass to use.
	 */
	private void draw(List<Draw> batch, PGlInstanceStream.Upload instanceStream, boolean multiDraw, OitPass oitPass)
	{
		Draw first = batch.getFirst();
		RenderType type = first.type();
		Minecraft mc = PLibRenderHelper.mc();
		TextureAtlas atlas = PTextureCache.getTextureAtlas();
		GpuTextureView lightTexture = mc.gameRenderer.levelLightmap();
		OverlayTexture overlayTexture = mc.gameRenderer.overlayTexture();
		PGpuDeformerBuffers.Bindings deformerBuffers = PGpuDeformerBuffers.upload();
		GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(
				RenderSystem.getModelViewMatrix(), new Vector4f(1.0F), new Vector3f(), new Matrix4f());

		RenderTargetAttachments attachments = attachments(type);

		try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
				first.mesh().uuid() :: toString, attachments.color(), OptionalInt.empty(), attachments.depth(), OptionalDouble.empty()))
		{
			pass.setPipeline(resolvePipeline(type, oitPass));
			RenderSystem.bindDefaultUniforms(pass);
			pass.setUniform("DynamicTransforms", dynamicTransforms);
			pass.setUniform("DeformerOperations", deformerBuffers.operations());
			pass.setUniform("DeformerValues", deformerBuffers.values());
			applyActiveScissor(pass);
			pass.bindTexture("Sampler0", atlas.getTextureView(), atlas.getSampler());
			pass.bindTexture("Sampler1", overlayTexture.getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
			pass.bindTexture("Sampler2", lightTexture, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
			if (oitPass.usesLayerDepth())
				pass.bindTexture("LayerDepthSampler", oitPass == OitPass.DEPTH_PEEL ?
						activeOit().previousLayerDepthView() : activeOit().activeLayerDepthView(),
						RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
			pass.setVertexBuffer(0, first.mesh().vbo());
			pass.setIndexBuffer(first.mesh().indices(), first.mesh().indexType());
			pass.drawIndexed(0, 0, 0, 1);
			if (oitPass != OitPass.NONE)
				activeOit().bindForDraw();

			if (!first.writeDepth() && oitPass == OitPass.NONE)
				GlStateManager._depthMask(false);
			try
			{
				first.slice().page().bind(instanceStream.buffer(), instanceStream.offset());
				if (multiDraw)
				{
					List<PGlIndirectStream.Command> commands = new ArrayList<>(batch.size());
					for (Draw draw : batch)
						commands.add(command(draw));
					PGlIndirectStream.Upload indirectStream = this.indirect.upload(commands);
					ARBMultiDrawIndirect.glMultiDrawElementsIndirect(GL11.GL_TRIANGLES, first.slice().indexType(), indirectStream.offset(), commands.size(), 0);
				}
				else
					for (Draw draw : batch)
					{
						draw.slice().page().setInstanceOffset(instanceStream.offset() + (long)draw.baseInstance() * PGlInstanceStream.STRIDE);
						GL32.glDrawElementsInstancedBaseVertex(GL11.GL_TRIANGLES, draw.slice().indexCount(), draw.slice().indexType(),
								draw.slice().indexOffset(), draw.instanceCount(), draw.slice().baseVertex());
					}
			}
			finally
			{
				if (!first.writeDepth() && oitPass == OitPass.NONE)
					GlStateManager._depthMask(true);
				GL30.glBindVertexArray(0);
				GL15.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, 0);
				GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
			}
		}
	}

	/**
	 * Performs the active oit operation.
	 * @return the value produced by this operation.
	 */
	private PGlWeightedBlendedOit activeOit()
	{
		if (this.activeWeightedBlendedOit == null)
			throw new IllegalStateException("No weighted blended OIT target is active");
		return this.activeWeightedBlendedOit;
	}

	/**
	 * Resolves the pipeline.
	 * @param type the type to use.
	 * @param oitPass the oit pass to use.
	 * @return the value produced by this operation.
	 */
	private RenderPipeline resolvePipeline(RenderType type, OitPass oitPass)
	{
		return switch (oitPass)
		{
			case NONE -> type.pipeline();
			case DEPTH -> PRenderTypes.RenderPipelinesProvider.TRIANGLES_OIT_DEPTH;
			case DEPTH_PEEL -> PRenderTypes.RenderPipelinesProvider.TRIANGLES_OIT_DEPTH_PEEL;
			case ACCUMULATION -> PRenderTypes.oitPipeline(type);
		};
	}

	/**
	 * Performs the attachments operation.
	 * @param type the type to use.
	 * @return the value produced by this operation.
	 */
	private static RenderTargetAttachments attachments(RenderType type)
	{
		RenderTarget target = type.outputTarget().getRenderTarget();
		GpuTextureView color = RenderSystem.outputColorTextureOverride != null ?
				RenderSystem.outputColorTextureOverride : target.getColorTextureView();
		GpuTextureView depth = target.useDepth ?
				(RenderSystem.outputDepthTextureOverride != null ? RenderSystem.outputDepthTextureOverride : target.getDepthTextureView()) : null;
		if (color == null)
			throw new IllegalStateException("Render type " + type + " has no color attachment");
		return new RenderTargetAttachments(color, depth);
	}

	/**
	 * Performs the command operation.
	 * @param draw the draw to use.
	 * @return the value produced by this operation.
	 */
	private static PGlIndirectStream.Command command(Draw draw)
	{
		PGlGeometryArena.Slice slice = draw.slice();
		return new PGlIndirectStream.Command(slice.indexCount(), draw.instanceCount(),
				Math.toIntExact(slice.indexOffset() / (slice.indexType() == GL11.GL_UNSIGNED_SHORT ? Short.BYTES : Integer.BYTES)),
				slice.baseVertex(), draw.baseInstance());
	}

	/**
	 * Applies the active scissor.
	 * @param pass the pass to use.
	 */
	private static void applyActiveScissor(RenderPass pass)
	{
		ScissorState scissor = RenderSystem.getScissorStateForRenderTypeDraws();
		if (scissor.enabled())
			pass.enableScissor(scissor.x(), scissor.y(), scissor.width(), scissor.height());
		else
			pass.disableScissor();
	}

	private record Draw(RenderType type, PBakedMesh mesh, PGlGeometryArena.Slice slice,
	                    int instanceCount, int baseInstance, boolean writeDepth)
	{
	}

	private record ArenaKey(PGlGeometryArena.Page page, int indexType)
	{
	}

	private enum OitPass
	{
		NONE,
		DEPTH,
		DEPTH_PEEL,
		ACCUMULATION;

		/**
		 * Performs the uses layer depth operation.
		 * @return the value produced by this operation.
		 */
		private boolean usesLayerDepth()
		{
			return this == DEPTH_PEEL || this == ACCUMULATION;
		}
	}

	private record RenderTargetAttachments(GpuTextureView color, GpuTextureView depth)
	{
	}
}
