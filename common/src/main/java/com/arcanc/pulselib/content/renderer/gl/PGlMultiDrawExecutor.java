/**
 * @author ArcAnc
 * Created at: 14.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.gl;

import com.arcanc.pulselib.content.renderer.rhi.PRhiOitRenderer;
import com.arcanc.pulselib.content.model.deformer.gpu.PGpuDeformerBuffers;
import com.arcanc.pulselib.content.mixin.GlBufferAccessor;
import com.arcanc.pulselib.content.renderer.plan.*;
import com.arcanc.pulselib.content.renderer.rhi.PRhiInstanceLayout;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.PResourceCache;
import com.arcanc.pulselib.util.helpers.PLibRenderHelper;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
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

import java.util.*;

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
	private @Nullable ProjectionMatrixBuffer projectionBuffer;
	private final PRhiOitRenderer nativeOit;
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
		this.nativeOit = new PRhiOitRenderer(resources::pipeline, resources::geometryResource,
				Map.of("DeformerOperations", () -> PGpuDeformerBuffers.upload().operations().slice(),
						"DeformerValues", () -> PGpuDeformerBuffers.upload().values().slice()));
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
		execute(plan, frame, PLibRenderHelper.mc().gameRenderer.mainRenderTarget());
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

		// Preserve the legacy executor API, but all OIT is submitted through the same native backend.
		if (useOit)
		{
			var oit = plan.groups().stream().filter(group -> !group.writeDepth() && this.resources.transparency(group.pipeline()) == PTransparencyMode.OIT).toList();
			if (!oit.isEmpty())
			{
				var source = Objects.requireNonNull(depthSource, "OIT opaque depth source");
				var color = RenderSystem.outputColorTextureOverride;
				try
				{
					if (color == null)
						RenderSystem.outputColorTextureOverride = source.getColorTextureView();
					this.nativeOit.execute(new PRenderPlan(oit, plan.instances()), frame, source.getDepthTextureView());
				}
				finally
				{
					RenderSystem.outputColorTextureOverride = color;
				}
				var ordinary = plan.groups().stream().filter(group -> group.writeDepth() || this.resources.transparency(group.pipeline()) != PTransparencyMode.OIT).toList();
				plan = new PRenderPlan(ordinary, plan.instances());
				if (plan.isEmpty())
					return;
			}
		}

		List<PDrawGroup> groups = new ArrayList<>();
		for (PDrawGroup group : plan.groups())
		{
			if (group.instances().isEmpty() || group.command().indexCount() == 0)
				continue;
			if (BindGroupLayout.flattenUniforms(this.resources.pipeline(group.pipeline()).pipeline().getBindGroupLayouts()).stream().
					anyMatch(uniform -> uniform.name().equals("ColorOverlay")))
				throw new UnsupportedOperationException("Single-object instant draws require the immediate renderer, not the GL instancing executor");
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
		@Nullable GpuBuffer instanceUniforms = null;
		GpuBufferSlice previousProjection = RenderSystem.getProjectionMatrixBuffer();
		ProjectionType previousProjectionType = RenderSystem.getProjectionType();
		try
		{
			List<Draw> draws = new ArrayList<>();
			for (PDrawGroup group : groups)
				draws.add(new Draw(this.resources.pipeline(group.pipeline()), "PulseLib mesh " + group.mesh().value(),
						this.geometry.resolve(this.resources.geometryResource(group.mesh()), frameSlot), group.command(), group.writeDepth(), this.resources.transparency(group.pipeline()), group));
			if (this.stateVertices == null)
				this.stateVertices = RenderSystem.getDevice().createBuffer(() -> "PulseLib pass state vertices", GpuBuffer.USAGE_VERTEX,
						PRenderTypes.VertexFormatProvider.POSITION_TEX_NORMAL.getVertexSize());
			if (this.stateIndices == null)
				this.stateIndices = RenderSystem.getDevice().createBuffer(() -> "PulseLib pass state indices", GpuBuffer.USAGE_INDEX, (long)Integer.BYTES);
			if (this.projectionBuffer == null)
				this.projectionBuffer = new ProjectionMatrixBuffer("PulseLib render frame");
			RenderSystem.setProjectionMatrix(this.projectionBuffer.getBuffer(frame.projection()),
					frame.projection().m33() == 0.0f ? ProjectionType.PERSPECTIVE : ProjectionType.ORTHOGRAPHIC);
			PRhiInstanceLayout layout = new PRhiInstanceLayout(RenderSystem.getDevice().getDeviceInfo().limits().minUniformOffsetAlignment(),
					PRhiInstanceLayout.PORTABLE_CAPACITY, 16_384);
			PRhiInstanceLayout.Packet packet = layout.pack(plan);
			instanceUniforms = RenderSystem.getDevice().createBuffer(() -> "PulseLib legacy instance uniforms",
					GpuBuffer.USAGE_UNIFORM, packet.bytes());
			Map<PDrawGroup, List<PRhiInstanceLayout.Batch>> chunks = new IdentityHashMap<>();
			for (PRhiInstanceLayout.Batch chunk : packet.batches())
				chunks.computeIfAbsent(chunk.group(), ignored -> new ArrayList<>()).add(chunk);
			boolean attributeInstances = draws.stream().anyMatch(draw -> !usesInstanceUniforms(draw.type().pipeline()));
			InstanceUploads instanceStream = new InstanceUploads(attributeInstances ?
					this.instances.uploadHeaders(plan.instances(), frameSlot, persistent) : null, instanceUniforms, chunks);
			this.indirect.begin(draws.size(), frameSlot, persistent && multiDraw);
			drawGroups(draws, instanceStream, multiDraw, frame);
		}
		finally
		{
			if (instanceUniforms != null)
				instanceUniforms.close();
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
		this.nativeOit.composite();
	}

	/** Resolves pending transparency into the requested color target, preserving its depth. */
	public void compositeOit(RenderTarget destination)
	{
		this.nativeOit.composite(Objects.requireNonNull(destination.getColorTextureView()));
	}

	/** Inserts pending OIT layers into the resolved vanilla Fabulous image. */
	public void compositeFabulousOit(RenderTarget destination, List<RenderTarget> targets)
	{
		this.nativeOit.compositeFabulous(Objects.requireNonNull(destination.getColorTextureView()), targets);
	}

	/** Copies only opaque depth; ownership stays with this executor through reload/disconnect. */
	public void captureWorldDepth(RenderTarget source)
	{
		if (this.worldOpaqueDepth == null || this.worldOpaqueDepth.width != source.width || this.worldOpaqueDepth.height != source.height)
		{
			if (this.worldOpaqueDepth != null)
			{
				this.worldOpaqueDepth.destroyBuffers();
			}
			this.worldOpaqueDepth = new TextureTarget("PulseLib opaque world depth", source.width, source.height, true, GpuFormat.RGBA8_UNORM);
		}
		this.nativeOit.discard();
		this.worldOpaqueDepth.copyDepthFrom(source);
		this.worldDepthValid = true;
	}

	/** World transparency uses the captured opaque depth, including in non-Fabulous mode. */
	public void executeWorld(PRenderPlan plan)
	{
		if (!plan.isEmpty())
			execute(plan, new PGlRenderHost().captureFrame(), this.worldDepthValid && this.worldOpaqueDepth != null ?
					this.worldOpaqueDepth : Minecraft.getInstance().gameRenderer.mainRenderTarget());
	}

	/** Invalidates the snapshot when the world's framegraph pass has completed. */
	public void endWorldFrame()
	{
		this.nativeOit.discard();
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
		this.nativeOit.cleanup();
		this.frameArena.awaitAll();
		if (clearGeometry)
			this.geometry.clear();
		this.instances.close();
		this.indirect.close();
		if (this.worldOpaqueDepth != null)
			this.worldOpaqueDepth.destroyBuffers();
		this.worldOpaqueDepth = null;
		this.nativeOit.discard();
		this.worldDepthValid = false;
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
	 * @param frame the captured frame matrices
	 */
	private void drawGroups(List<Draw> draws, InstanceUploads instanceStream, boolean multiDraw, PRenderFrame frame)
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
			draw(draws.subList(start, end), instanceStream, multiDraw && end - start > 1, frame);
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
	private void drawOpaquePipeline(List<Draw> draws, InstanceUploads instanceStream, boolean multiDraw, PRenderFrame frame)
	{
		Map<ArenaKey, List<Draw>> batches = new LinkedHashMap<>();
		for (Draw draw : draws)
			batches.computeIfAbsent(new ArenaKey(draw.slice().page(), draw.slice().indexType()), ignored -> new ArrayList<>()).add(draw);
		for (List<Draw> batch : batches.values())
			draw(batch, instanceStream, multiDraw && batch.size() > 1, frame);
	}

	/**
	 * Performs the draw operation.
	 * @param batch the batch to use.
	 * @param instanceStream the instance stream to use.
	 * @param multiDraw the multi draw to use.
	 * @param frame the captured frame matrices
	 */
	private void draw(List<Draw> batch, InstanceUploads instanceStream, boolean multiDraw, PRenderFrame frame)
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

		RenderTargetAttachments attachments = attachments(first);

		try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
				first :: label, attachments.color(), Optional.empty(), attachments.depth(), OptionalDouble.empty()))
		{
			RenderPipeline pipeline = type.pipeline();
			boolean uniformInstances = usesInstanceUniforms(pipeline);
			pass.setPipeline(pipeline);
			if (uniformInstances)
				pass.setUniform("InstanceData", instanceStream.uniforms().slice(
						instanceStream.chunks().get(first.group()).getFirst().uniformOffset(),
						(long)PRhiInstanceLayout.PORTABLE_CAPACITY * PRhiInstanceLayout.STRIDE));
			RenderSystem.bindDefaultUniforms(pass);
			pass.setUniform("DynamicTransforms", dynamicTransforms);
			pass.setUniform("DeformerOperations", deformerBuffers.operations());
			pass.setUniform("DeformerValues", deformerBuffers.values());
			applyActiveScissor(pass);
			pass.bindTexture("Sampler0", atlas.getTextureView(), atlas.getSampler());
			pass.bindTexture("Sampler1", overlayTexture.getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
			pass.bindTexture("Sampler2", lightTexture, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
			pass.setVertexBuffer(0, this.stateVertices.slice());
			pass.setIndexBuffer(this.stateIndices, IndexType.INT);
			pass.drawIndexed(0, 1, 0, 0, 0);

			if (!first.writeDepth())
				GlStateManager._depthMask(false);
			try
			{
				if (uniformInstances)
				{
					// This compatibility backend still owns raw GL pages. The universal RHI executor
					// binds the same blocks exclusively through RenderPass.setUniform.
					int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
					int block = GL31.glGetUniformBlockIndex(program, "InstanceData");
					int binding = GL31.glGetActiveUniformBlocki(program, block, GL31.GL_UNIFORM_BLOCK_BINDING);
					first.slice().page().bindGeometry();
					for (Draw draw : batch)
						for (PRhiInstanceLayout.Batch chunk : instanceStream.chunks().get(draw.group()))
						{
							GL30.glBindBufferRange(GL31.GL_UNIFORM_BUFFER, binding,
									((GlBufferAccessor)instanceStream.uniforms()).pulselib$getHandle(), chunk.uniformOffset(),
									(long)PRhiInstanceLayout.PORTABLE_CAPACITY * PRhiInstanceLayout.STRIDE);
							PGlIndirectStream.Command command = command(draw);
							GL32.glDrawElementsInstancedBaseVertex(GL11.GL_TRIANGLES, command.indexCount(), draw.slice().indexType(),
									(long)command.firstIndex() * indexBytes(draw.slice()), chunk.instanceCount(), command.baseVertex());
						}
				}
				else
					drawAttributeInstances(batch, Objects.requireNonNull(instanceStream.attributes()), multiDraw);
			}
			finally
			{
				if (!first.writeDepth())
					GlStateManager._depthMask(true);
				GL30.glBindVertexArray(0);
				GL15.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, 0);
				GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
			}
		}
	}

	private static boolean usesInstanceUniforms(RenderPipeline pipeline)
	{
		return BindGroupLayout.flattenUniforms(pipeline.getBindGroupLayouts()).stream().
				anyMatch(uniform -> uniform.name().equals("InstanceData"));
	}

	private void drawAttributeInstances(List<Draw> batch, PGlInstanceStream.Upload instanceStream, boolean multiDraw)
	{
		Draw first = batch.getFirst();
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
				Minecraft.getInstance().gameRenderer.mainRenderTarget() : type.outputTarget().getRenderTarget();
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
	                    PDrawCommand command, boolean writeDepth, PTransparencyMode transparency, PDrawGroup group)
	{
	}

	private record InstanceUploads(PGlInstanceStream.@Nullable Upload attributes, GpuBuffer uniforms,
			Map<PDrawGroup, List<PRhiInstanceLayout.Batch>> chunks)
	{
	}

	/**
	 * Immutable value object representing arena key.
    */
	private record ArenaKey(PGlGeometryArena.Page page, int indexType)
	{
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
