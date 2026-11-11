/**
 * @author ArcAnc
 * Created at: 14.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.gl;

import com.arcanc.pulselib.content.model.deformer.gpu.PGpuDeformerBuffers;
import com.arcanc.pulselib.content.renderer.plan.PDrawCommand;
import com.arcanc.pulselib.content.renderer.plan.PDrawGroup;
import com.arcanc.pulselib.content.renderer.plan.PRenderCapabilities;
import com.arcanc.pulselib.content.renderer.plan.PRenderCapabilityMatrix;
import com.arcanc.pulselib.content.renderer.plan.PRenderExecutor;
import com.arcanc.pulselib.content.renderer.plan.PRenderFrame;
import com.arcanc.pulselib.content.renderer.plan.PRenderPlan;
import com.arcanc.pulselib.content.renderer.plan.PTransparencyMode;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.PResourceCache;
import com.arcanc.pulselib.util.helpers.PLibRenderHelper;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.*;

import java.util.Objects;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Provides support for gl multi draw executor.
 */
public final class PGlMultiDrawExecutor implements PRenderExecutor
{
	private final PGlResourceRegistry resources;
	private final PGlGeometryArena geometry;
	private final PGlInstanceStream instances = new PGlInstanceStream();
	private final PGlIndirectStream indirect = new PGlIndirectStream();
	private final PGlFrameArena frameArena = new PGlFrameArena(9);
	private final Map<RenderTarget, PGlWeightedBlendedOit> weightedBlendedOits = new IdentityHashMap<>();
	private @Nullable PGlWeightedBlendedOit activeWeightedBlendedOit;
	private @Nullable RenderTargetAttachments activeOitAttachments;
	private @Nullable ProjectionMatrixBuffer projectionBuffer;
	private @Nullable TextureTarget worldOpaqueDepth;
	private boolean worldDepthValid;
	private @Nullable GpuBuffer stateVertices;
	private @Nullable GpuBuffer stateIndices;

	public PGlMultiDrawExecutor()
	{
		this(new PGlResourceRegistry());
	}

	/** Uses resource handles owned by the submitting queue. */
	public PGlMultiDrawExecutor(PGlResourceRegistry resources)
	{
		this(resources, new PGlGeometryArena());
	}

	/** Shares the canonical registry's existing arena with its native executor. */
	public PGlMultiDrawExecutor(PGlResourceRegistry resources, PGlGeometryArena geometry)
	{
		this.resources = resources;
		this.geometry = geometry;
	}

	/** Reports the actual 26.1 backend strategies, without selecting another renderer. */
	@Override
	public PRenderCapabilities capabilities()
	{
		GLCapabilities gl = GL.getCapabilities();
		PRenderCapabilityMatrix matrix = new PRenderCapabilityMatrix(
				gl.OpenGL31 || gl.GL_ARB_texture_buffer_object,
				gl.OpenGL43 || gl.GL_ARB_shader_storage_buffer_object,
				gl.OpenGL43 || gl.GL_ARB_multi_draw_indirect,
				gl.OpenGL42 || gl.GL_ARB_base_instance,
				gl.OpenGL44 || gl.GL_ARB_buffer_storage);
		// Bone poses are evaluated by Common; the existing GPU deformer stream uses UBOs.
		return matrix.selectedCapabilities(PRenderCapabilities.StorageMode.CPU, matrix.persistentMapping());
	}

	/**
	 * Performs the execute operation.
	 * @param plan the plan to use.
	 */
	public void execute(PRenderPlan plan)
	{
		if (!plan.isEmpty())
			this.execute(plan, new PGlRenderHost().captureFrame());
	}

	/** Executes canonical packed commands using the existing instanced GPU paths. */
	@Override
	public void execute(PRenderPlan plan, PRenderFrame frame)
	{
		execute(plan, frame, Minecraft.getInstance().getMainRenderTarget());
	}

	/** Uses opaque depth independently of the eventual transparency destination. */
	public void execute(PRenderPlan plan, PRenderFrame frame, RenderTarget depthSource)
	{
		execute(plan, frame, depthSource, true);
	}

	/** Direct bone/model rendering uses ordinary blending and never composites pending world OIT. */
	public void executeImmediate(PRenderPlan plan, PRenderFrame frame)
	{
		execute(plan, frame, null, false);
	}

	private void execute(PRenderPlan plan, PRenderFrame frame, @Nullable RenderTarget depthSource, boolean useOit)
	{
		if (plan.isEmpty())
			return;

		List<PDrawGroup> groups = new ArrayList<>();
		for (PDrawGroup group : plan.groups())
		{
			if (group.instances().isEmpty())
				continue;
			PDrawCommand command = group.command();
			if ((long)command.firstIndex() + command.indexCount() > this.resources.indexCount(group.mesh()) ||
					(long)command.firstInstance() + command.instanceCount() > plan.instances().size())
				throw new IllegalArgumentException("Draw command exceeds its geometry or instance stream");
			groups.add(group);
		}
		if (groups.isEmpty())
			return;

		PRenderCapabilities capabilities = capabilities();
		boolean persistent = capabilities.uploadMode() == PRenderCapabilities.UploadMode.PERSISTENT_RING;
		int frameSlot = persistent ? this.frameArena.begin() : 0;
		if (frameSlot < 0)
			return;
		boolean multiDraw = capabilities.submissionMode() == PRenderCapabilities.SubmissionMode.MULTI_DRAW_INDIRECT;
		GpuBufferSlice previousProjection = RenderSystem.getProjectionMatrixBuffer();
		ProjectionType previousProjectionType = RenderSystem.getProjectionType();
		try
		{
			List<Draw> draws = new ArrayList<>();
			for (PDrawGroup group : groups)
				draws.add(new Draw(this.resources.pipeline(group.pipeline()), "PulseLib mesh " + group.mesh().value(),
						this.resources.resolve(group.mesh(), this.geometry, frameSlot), group.command(), group.writeDepth(), this.resources.transparency(group.pipeline())));
			if (this.stateVertices == null)
				this.stateVertices = RenderSystem.getDevice().createBuffer(() -> "PulseLib pass state vertices", GpuBuffer.USAGE_VERTEX,
						(long)PRenderTypes.VertexFormatProvider.POSITION_TEX_NORMAL.getVertexSize());
			if (this.stateIndices == null)
				this.stateIndices = RenderSystem.getDevice().createBuffer(() -> "PulseLib pass state indices", GpuBuffer.USAGE_INDEX, (long)Integer.BYTES);
			if (this.projectionBuffer == null)
				this.projectionBuffer = new ProjectionMatrixBuffer("PulseLib render frame");
			RenderSystem.setProjectionMatrix(this.projectionBuffer.getBuffer(frame.projection()),
					frame.projection().m33() == 0.0f ? ProjectionType.PERSPECTIVE : ProjectionType.ORTHOGRAPHIC);
			PGlInstanceStream.Upload instanceStream = this.instances.uploadHeaders(plan.instances(), frameSlot, persistent);
			List<Draw> standardDraws = new ArrayList<>();
			List<Draw> oitDraws = new ArrayList<>();
			for (Draw draw : draws)
			{
				if (useOit && !draw.writeDepth() && draw.transparency() == PTransparencyMode.OIT)
					oitDraws.add(draw);
				else
					standardDraws.add(draw);
			}
			int maximumIndirectCommands = standardDraws.size() + oitDraws.size() * PGlWeightedBlendedOit.LAYER_COUNT * 2;
			this.indirect.begin(maximumIndirectCommands, frameSlot, persistent && multiDraw);

			drawGroups(standardDraws, instanceStream, multiDraw, OitPass.NONE, frame);
			if (!oitDraws.isEmpty())
			{
				RenderTarget source = Objects.requireNonNull(depthSource, "OIT opaque depth source");
				List<Draw> targetDraws = oitDraws;
				this.activeWeightedBlendedOit = this.weightedBlendedOits.computeIfAbsent(source, ignored -> new PGlWeightedBlendedOit());
				try
				{
					RenderTargetAttachments attachments = new RenderTargetAttachments(
							Objects.requireNonNull(source.getColorTextureView(), "OIT depth source color"), source.getDepthTextureView());
					this.activeOitAttachments = attachments;
					if (activeOit().begin(attachments.color(), attachments.depth()))
					{
						for (int layer = 0; layer < PGlWeightedBlendedOit.LAYER_COUNT; layer++)
						{
							activeOit().beginDepthPass(layer);
							try
							{
								drawGroups(targetDraws, instanceStream, multiDraw,
										layer == 0 ? OitPass.DEPTH : OitPass.DEPTH_PEEL, frame);
							}
							finally
							{
								activeOit().endPass();
							}

							activeOit().beginAccumulationPass(layer);
							try
							{
								drawGroups(targetDraws, instanceStream, multiDraw, OitPass.ACCUMULATION, frame);
								activeOit().markContent(layer);
							}
							finally
							{
								activeOit().endPass();
							}
						}
					}
					else
						drawGroups(targetDraws, instanceStream, multiDraw, OitPass.NONE, frame);
				}
				finally
				{
					this.activeWeightedBlendedOit = null;
					this.activeOitAttachments = null;
				}
			}
		}
		finally
		{
			RenderSystem.setProjectionMatrix(previousProjection, previousProjectionType);
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

	/** Resolves pending transparency into the requested color target, preserving its depth. */
	public void compositeOit(RenderTarget destination)
	{
		GpuTextureView color = Objects.requireNonNull(destination.getColorTextureView(), "OIT destination color");
		// Validate every pending source before consuming or writing any of them.
		this.weightedBlendedOits.values().forEach(oit -> oit.validateDestination(color));
		this.weightedBlendedOits.values().forEach(oit -> oit.composite(color));
	}

	/** Inserts pending OIT layers into the resolved vanilla Fabulous image. */
	public void compositeFabulousOit(RenderTarget destination, List<RenderTarget> targets)
	{
		GpuTextureView color = Objects.requireNonNull(destination.getColorTextureView(), "OIT destination color");
		PGlWeightedBlendedOit.validateFabulousTargets(color, targets);
		this.weightedBlendedOits.values().forEach(oit -> oit.validateDestination(color));
		this.weightedBlendedOits.values().forEach(oit -> oit.compositeFabulous(color, targets));
	}

	/** Copies only opaque depth; ownership stays with this executor through reload/disconnect. */
	public void captureWorldDepth(RenderTarget source)
	{
		if (this.worldOpaqueDepth == null || this.worldOpaqueDepth.width != source.width || this.worldOpaqueDepth.height != source.height)
		{
			if (this.worldOpaqueDepth != null)
			{
				PGlWeightedBlendedOit old = this.weightedBlendedOits.remove(this.worldOpaqueDepth);
				if (old != null)
					old.close();
				this.worldOpaqueDepth.destroyBuffers();
			}
			this.worldOpaqueDepth = new TextureTarget("PulseLib opaque world depth", source.width, source.height, true);
		}
		PGlWeightedBlendedOit pending = this.weightedBlendedOits.get(this.worldOpaqueDepth);
		if (pending != null)
			pending.discard();
		this.worldOpaqueDepth.copyDepthFrom(source);
		this.worldDepthValid = true;
	}

	/** World transparency uses the captured opaque depth, including in non-Fabulous mode. */
	public void executeWorld(PRenderPlan plan)
	{
		if (!plan.isEmpty())
			execute(plan, new PGlRenderHost().captureFrame(), this.worldDepthValid && this.worldOpaqueDepth != null ?
					this.worldOpaqueDepth : Minecraft.getInstance().getMainRenderTarget());
	}

	/** Invalidates the snapshot when the world's framegraph pass has completed. */
	public void endWorldFrame()
	{
		this.worldDepthValid = false;
	}

	/**
	 * Performs the cleanup operation.
	 */
	@Override
	public void cleanup()
	{
		cleanup(true);
	}

	/** Releases execution state, optionally retaining caller-owned geometry and handles. */
	public void cleanup(boolean clearGeometry)
	{
		this.frameArena.awaitAll();
		if (clearGeometry)
			this.geometry.clear();
		this.instances.close();
		this.indirect.close();
		this.weightedBlendedOits.values().forEach(PGlWeightedBlendedOit :: close);
		this.weightedBlendedOits.clear();
		if (this.worldOpaqueDepth != null)
			this.worldOpaqueDepth.destroyBuffers();
		this.worldOpaqueDepth = null;
		this.worldDepthValid = false;
		this.activeWeightedBlendedOit = null;
		this.activeOitAttachments = null;
		if (this.projectionBuffer != null)
			this.projectionBuffer.close();
		this.projectionBuffer = null;
		if (this.stateVertices != null)
			this.stateVertices.close();
		if (this.stateIndices != null)
			this.stateIndices.close();
		this.stateVertices = null;
		this.stateIndices = null;
		if (clearGeometry)
			this.resources.clear();
		else
			this.frameArena.reset();
	}

	/**
	 * Draws the groups.
	 * @param draws the draws to use.
	 * @param instanceStream the instance stream to use.
	 * @param multiDraw the multi draw to use.
	 * @param oitPass the oit pass to use.
	 * @param frame the captured frame matrices
	 */
	private void drawGroups(List<Draw> draws, PGlInstanceStream.Upload instanceStream, boolean multiDraw, OitPass oitPass, PRenderFrame frame)
	{
		for (int start = 0; start < draws.size();)
		{
			if (draws.get(start).writeDepth())
			{
				int end = findOpaquePipelineEnd(draws, start);
				drawOpaquePipeline(draws.subList(start, end), instanceStream, multiDraw, frame);
				start = end;
				continue;
			}
			int end = findBatchEnd(draws, start);
			draw(draws.subList(start, end), instanceStream, multiDraw && end - start > 1, oitPass, frame);
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
			if (candidate.type() != first.type() || candidate.transparency() != first.transparency() || candidate.slice().page() != first.slice().page() ||
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
	private void drawOpaquePipeline(List<Draw> draws, PGlInstanceStream.Upload instanceStream, boolean multiDraw, PRenderFrame frame)
	{
		Map<ArenaKey, List<Draw>> batches = new LinkedHashMap<>();
		for (Draw draw : draws)
			batches.computeIfAbsent(new ArenaKey(draw.slice().page(), draw.slice().indexType()), ignored -> new ArrayList<>()).add(draw);
		for (List<Draw> batch : batches.values())
			draw(batch, instanceStream, multiDraw && batch.size() > 1, OitPass.NONE, frame);
	}

	/**
	 * Performs the draw operation.
	 * @param batch the batch to use.
	 * @param instanceStream the instance stream to use.
	 * @param multiDraw the multi draw to use.
	 * @param oitPass the oit pass to use.
	 * @param frame the captured frame matrices
	 */
	private void draw(List<Draw> batch, PGlInstanceStream.Upload instanceStream, boolean multiDraw, OitPass oitPass, PRenderFrame frame)
	{
		Draw first = batch.getFirst();
		RenderType type = first.type();
		Minecraft mc = PLibRenderHelper.mc();
		TextureAtlas atlas = PResourceCache.getTextureAtlas();
		GpuTextureView lightTexture = mc.gameRenderer.levelLightmap();
		OverlayTexture overlayTexture = mc.gameRenderer.overlayTexture();
		PGpuDeformerBuffers.Bindings deformerBuffers = PGpuDeformerBuffers.upload();
		GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(
				frame.modelView(), new Vector4f(1.0F), new Vector3f(), new Matrix4f());

		RenderTargetAttachments attachments = oitPass == OitPass.NONE ? attachments(first) :
				Objects.requireNonNull(this.activeOitAttachments, "OIT pass attachments");

		try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
				first :: label, attachments.color(), OptionalInt.empty(), attachments.depth(), OptionalDouble.empty()))
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
			// A zero-index draw applies Minecraft pipeline/uniform state; arena VAOs supply actual geometry.
			pass.setVertexBuffer(0, this.stateVertices);
			pass.setIndexBuffer(this.stateIndices, VertexFormat.IndexType.INT);
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
						PGlIndirectStream.Command command = command(draw);
						draw.slice().page().setInstanceOffset(instanceStream.offset() + (long)command.baseInstance() * PGlInstanceStream.STRIDE);
						GL32.glDrawElementsInstancedBaseVertex(GL11.GL_TRIANGLES, command.indexCount(), draw.slice().indexType(),
								(long)command.firstIndex() * indexBytes(draw.slice()), command.instanceCount(), command.baseVertex());
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
	 * @param frame the captured frame matrices
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
	 * @param draw the draw and its submission policy.
	 * @return the value produced by this operation.
	 */
	private static RenderTargetAttachments attachments(Draw draw)
	{
		RenderType type = draw.type();
		// Ordinary world alpha runs after the native item/entity target has completed its pass.
		RenderTarget target = draw.transparency() == PTransparencyMode.ORDINARY && PRenderTypes.usesOit(type) ?
				Minecraft.getInstance().getMainRenderTarget() : type.outputTarget().getRenderTarget();
		GpuTextureView color = RenderSystem.outputColorTextureOverride != null ?
				RenderSystem.outputColorTextureOverride : target.getColorTextureView();
		GpuTextureView depth = RenderSystem.outputColorTextureOverride != null ? RenderSystem.outputDepthTextureOverride :
				(target.useDepth ? (RenderSystem.outputDepthTextureOverride != null ? RenderSystem.outputDepthTextureOverride : target.getDepthTextureView()) : null);
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
		return resolveCommand(draw.command(), Math.toIntExact(slice.indexOffset() / indexBytes(slice)), slice.baseVertex());
	}

	/** Resolves mesh-local addressing identically for indirect and direct instanced draws. */
	static PGlIndirectStream.Command resolveCommand(PDrawCommand command, int firstIndex, int baseVertex)
	{
		return new PGlIndirectStream.Command(command.indexCount(), command.instanceCount(),
				Math.addExact(firstIndex, command.firstIndex()),
				Math.addExact(baseVertex, command.baseVertex()), command.firstInstance());
	}

	private static int indexBytes(PGlGeometryArena.Slice slice)
	{
		return slice.indexType() == GL11.GL_UNSIGNED_SHORT ? Short.BYTES : Integer.BYTES;
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

/**
 * Immutable value object representing draw.
 */
	private record Draw(RenderType type, String label, PGlGeometryArena.Slice slice,
	                    PDrawCommand command, boolean writeDepth, PTransparencyMode transparency)
	{
	}

/**
 * Immutable value object representing arena key.
 */
	private record ArenaKey(PGlGeometryArena.Page page, int indexType)
	{
	}

/**
 * Enumerates the available oit pass values.
 */
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

/**
 * Immutable value object representing render target attachments.
 */
	private record RenderTargetAttachments(GpuTextureView color, GpuTextureView depth)
	{
	}
	/** Completes canonical local previews after waiting for their next native slot. */
	public void executeImmediateWaiting(PRenderPlan plan, PRenderFrame frame)
	{
		if (plan.isEmpty())
			return;
		this.frameArena.awaitNext();
		executeImmediate(plan, frame);
	}

}
