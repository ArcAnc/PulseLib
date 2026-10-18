/**
 * @author ArcAnc
 * Created at: 13.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.legacy;

import com.arcanc.pulselib.content.mixin.VertexBufferAccessor;
import com.arcanc.pulselib.content.model.deformer.gpu.PGpuDeformerBuffers;
import com.arcanc.pulselib.content.renderer.plan.*;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.helpers.PLibRenderHelper;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Executes compiled render plans using OpenGL geometry arenas, instancing and weighted transparency.
 */
public final class GlDrawExecutor implements PRenderExecutor
{
	private static final int STRIDE = 12 * 4 + 4 * 4 + 2 * 4 + 2 * 4 + 4 * 4;
	private static final int FRAMES_IN_FLIGHT = 3;
	private static final int STAGE_SUBMISSIONS_PER_FRAME = 4;
	private static final int RING_SLOTS = FRAMES_IN_FLIGHT * STAGE_SUBMISSIONS_PER_FRAME;
	private static final int MAX_INDIRECT_DRAWS_PER_EXECUTION = 1 + GlWeightedBlendedOit.LAYER_COUNT * 2;
	private static final boolean MULTI_DRAW_INDIRECT_ENABLED = true;

	private final GlResourceRegistry resources;
	private final GlFrameArena frameArena = new GlFrameArena(RING_SLOTS);
	private final InstanceBuffer instanceBuffer = new InstanceBuffer();
	private final IndirectBuffer indirectBuffer = new IndirectBuffer();
	private final GlWeightedBlendedOit weightedBlendedOit = new GlWeightedBlendedOit();
	private @Nullable GlRenderState localTarget;
	private boolean capabilitiesResolved;
	private PRenderCapabilityMatrix capabilityMatrix = PRenderCapabilityMatrix.NONE;
	private boolean persistentMapping;
	private boolean persistentIndirectMapping;
	private boolean multiDrawIndirect;

	/**
	 * Creates an OpenGL executor backed by the supplied resource registry.
	 *
	 * @param resources the resources
	 */
	public GlDrawExecutor(GlResourceRegistry resources)
	{
		this.resources = resources;
	}

	/**
	 * Returns the storage, draw submission and upload modes selected by this executor.
	 *
	 * @return the storage, draw submission and upload modes selected by this executor
	 */
	@Override
	public PRenderCapabilities capabilities()
	{
		if (!this.capabilitiesResolved)
			return new PRenderCapabilities(PRenderCapabilities.StorageMode.TEXEL_BUFFER,
					PRenderCapabilities.SubmissionMode.DIRECT, PRenderCapabilities.UploadMode.WRITE, this.capabilityMatrix);
		return this.capabilityMatrix.selectedCapabilities(PRenderCapabilities.StorageMode.TEXEL_BUFFER, this.persistentMapping);
	}

	/**
	 * Returns the detected OpenGL feature matrix.
	 *
	 * @return the detected OpenGL feature matrix
	 */
	public PRenderCapabilityMatrix capabilityMatrix()
	{
		return this.capabilityMatrix;
	}

	/**
	 * Executes the supplied render plan with the captured frame matrices and optional depth source.
	 *
	 * @param plan the plan
	 * @param frame the frame matrices or sampled animation values used by this operation
	 */
	@Override
	public void execute(PRenderPlan plan, PRenderFrame frame)
	{
		execute(plan, frame, Minecraft.getInstance().getMainRenderTarget());
	}

	/**
	 * Executes the supplied render plan with the captured frame matrices and optional depth source.
	 *
	 * @param plan the plan
	 * @param frame the frame matrices or sampled animation values used by this operation
	 * @param depthSource the render target supplying opaque scene depth
	 */
	public void execute(PRenderPlan plan, PRenderFrame frame, RenderTarget depthSource)
	{
		execute(plan, frame, depthSource, true);
	}

	/**
	 * Draws sorted local transparency directly into the caller's bound target.
	 *
	 * @param plan the plan
	 * @param frame the frame matrices or sampled animation values used by this operation
	 * @param target the target
	 */
	public void executeLocal(PRenderPlan plan, PRenderFrame frame, GlRenderState target)
	{
		GlRenderState previous = this.localTarget;
		this.localTarget = target;
		try
		{
			execute(plan, frame, null, false);
		}
		finally
		{
			this.localTarget = previous;
		}
	}

	private void execute(PRenderPlan plan, PRenderFrame frame, @Nullable RenderTarget depthSource, boolean useOit)
	{
		if (plan.isEmpty())
			return;
		resolveCapabilities();
		int frameSlot = useOit ? this.frameArena.begin(this.persistentMapping) : this.frameArena.beginWaiting(this.persistentMapping);
		if (frameSlot < 0)
			return;
		this.instanceBuffer.upload(plan.instances(), frameSlot, this.persistentMapping, this.frameArena);
		publishPersistentWrites();
		if (this.multiDrawIndirect)
			this.indirectBuffer.prepare(Math.multiplyExact(plan.groups().size(), MAX_INDIRECT_DRAWS_PER_EXECUTION),
					this.persistentIndirectMapping, this.frameArena);
		try
		{
			List<PDrawGroup> standardGroups = new ArrayList<>();
			List<PDrawGroup> oitGroups = new ArrayList<>();
			for (PDrawGroup group : plan.groups())
			{
				RenderType type = this.resources.pipeline(group.pipeline());
				if (useOit && !group.writeDepth() && PRenderTypes.RenderTypeProvider.usesOit(type) &&
						PRenderTypes.ShadersProvider.trianglesOit(PRenderTypes.RenderTypeProvider.usesEmissiveOit(type)) != null)
					oitGroups.add(group);
				else
					standardGroups.add(group);
			}

			int indirectCommandOffset = drawGroups(standardGroups, frame.modelView(), frame.projection(), frameSlot,
					OitPass.NONE, 0);
			if (!oitGroups.isEmpty())
			{
				if (this.weightedBlendedOit.begin(depthSource))
				{
					for (int layer = 0; layer < GlWeightedBlendedOit.LAYER_COUNT; layer++)
					{
						this.weightedBlendedOit.beginDepthPass(layer);
						try
						{
							indirectCommandOffset = drawGroups(oitGroups, frame.modelView(), frame.projection(), frameSlot,
										layer == 0 ? OitPass.DEPTH : OitPass.DEPTH_PEEL, indirectCommandOffset);
						}
						finally
						{
							this.weightedBlendedOit.endPass();
						}

						this.weightedBlendedOit.beginAccumulationPass(layer);
						try
						{
							indirectCommandOffset = drawGroups(oitGroups, frame.modelView(), frame.projection(), frameSlot,
									OitPass.ACCUMULATION, indirectCommandOffset);
							this.weightedBlendedOit.markContent(layer);
						}
						finally
						{
							this.weightedBlendedOit.endPass();
						}
					}
				}
				else
					drawGroups(oitGroups, frame.modelView(), frame.projection(), frameSlot, OitPass.NONE, indirectCommandOffset);
			}
		}
		finally
		{
			this.frameArena.finish(frameSlot, this.persistentMapping);
		}
	}

	/**
	 * Composites accumulated weighted transparency into the destination target.
	 *
	 * @param destination the mutable object receiving the result
	 */
	public void compositeOit(RenderTarget destination)
	{
		this.weightedBlendedOit.composite(destination);
	}

	/**
	 * Composites weighted transparency with the supplied Fabulous render targets.
	 *
	 * @param destination the mutable object receiving the result
	 * @param targets the vanilla Fabulous transparency targets
	 */
	public void compositeFabulousOit(RenderTarget destination, List<RenderTarget> targets)
	{
		this.weightedBlendedOit.compositeFabulous(destination, targets);
	}

	/**
	 * Releases instance, indirect-draw, stream and transparency resources owned by this executor.
	 */
	@Override
	public void cleanup()
	{
		this.frameArena.awaitAll();
		this.instanceBuffer.close();
		this.indirectBuffer.close();
		this.weightedBlendedOit.close();
		GlStreamStorage.cleanup();
		this.frameArena.reset();
		this.capabilitiesResolved = false;
		this.capabilityMatrix = PRenderCapabilityMatrix.NONE;
		this.persistentMapping = false;
		this.persistentIndirectMapping = this.persistentMapping;
		this.multiDrawIndirect = false;
	}

	private int drawGroups(List<PDrawGroup> groups, Matrix4f modelView, Matrix4f projection, int frameSlot, OitPass oitPass,
	                       int indirectCommandOffset)
	{
		for (int groupIndex = 0; groupIndex < groups.size();)
		{
			if (this.multiDrawIndirect && groups.get(groupIndex).writeDepth())
			{
				int nextPipeline = findOpaquePipelineEnd(groups, groupIndex);
				indirectCommandOffset = drawOpaquePipeline(groups.subList(groupIndex, nextPipeline), modelView, projection,
						frameSlot, indirectCommandOffset);
				groupIndex = nextPipeline;
				continue;
			}
			int nextGroup = this.multiDrawIndirect ? this.findMultiDrawEnd(groups, groupIndex) : groupIndex + 1;
			if (nextGroup - groupIndex > 1)
				indirectCommandOffset = drawMulti(groups.subList(groupIndex, nextGroup), modelView, projection, frameSlot, oitPass,
						indirectCommandOffset);
			else
				draw(groups.get(groupIndex), modelView, projection, frameSlot, oitPass);
			groupIndex = nextGroup;
		}
		return indirectCommandOffset;
	}

	private void draw(PDrawGroup group, Matrix4f modelView, Matrix4f projection, int frameSlot, OitPass oitPass)
	{
		if (group.command().instanceCount() == 0)
			return;
		RenderType type = this.resources.pipeline(group.pipeline());
		boolean geometryArena = this.resources.isGeometry(group.mesh());
		VertexBuffer vertexBuffer = geometryArena ? null : this.resources.mesh(group.mesh());
		type.setupRenderState();
		if (this.localTarget != null)
		{
			this.localTarget.bindTarget();
			RenderSystem.depthMask(group.writeDepth());
		}
		if (oitPass != OitPass.NONE)
			this.weightedBlendedOit.bindForDraw();
		if (!group.writeDepth())
			RenderSystem.depthMask(false);
		try
		{
			ShaderInstance shader = shader(type, oitPass);
			if (shader == null)
				return;
			GlGeometryArena.Slice geometry = geometryArena ? this.resources.geometry(group.mesh()) : null;
			if (geometryArena)
				geometry.bind();
			else
				vertexBuffer.bind();
			long instanceOffset = this.instanceBuffer.offset(frameSlot) + (long)group.command().firstInstance() * STRIDE;
			setupInstanceAttributes(this.instanceBuffer.vbo(), instanceOffset);
			try
			{
				VertexBufferAccessor accessor = geometryArena ? null : (VertexBufferAccessor)vertexBuffer;
				shader.setDefaultUniforms(geometryArena ? VertexFormat.Mode.TRIANGLES : accessor.pulselib$getMode(),
						modelView, projection, PLibRenderHelper.mc().getWindow());
				bindLayerDepth(shader, oitPass);
				shader.apply();
				GlStreamStorage.bind(shader, PGpuDeformerBuffers.streams());
				if (geometryArena)
					GL32.glDrawElementsInstancedBaseVertex(GL11.GL_TRIANGLES, group.command().indexCount(), geometry.indexType(),
							geometry.indexOffset(), group.command().instanceCount(), group.command().baseVertex());
				else
					GL32.glDrawElementsInstancedBaseVertex(accessor.pulselib$getMode().asGLMode, group.command().indexCount(),
							accessor.pulselib$getIndexType().asGLType, 0, group.command().instanceCount(), group.command().baseVertex());
			}
			finally
			{
				disableInstanceAttributes();
			}
		}
		finally
		{
			if (!group.writeDepth())
				RenderSystem.depthMask(true);
			type.clearRenderState();
		}
	}

	private int drawMulti(List<PDrawGroup> groups, Matrix4f modelView, Matrix4f projection, int frameSlot, OitPass oitPass,
	                      int indirectCommandOffset)
	{
		PDrawGroup first = groups.getFirst();
		RenderType type = this.resources.pipeline(first.pipeline());
		GlGeometryArena.Slice geometry = this.resources.geometry(first.mesh());
		type.setupRenderState();
		if (this.localTarget != null)
		{
			this.localTarget.bindTarget();
			RenderSystem.depthMask(first.writeDepth());
		}
		if (oitPass != OitPass.NONE)
			this.weightedBlendedOit.bindForDraw();
		if (!first.writeDepth())
			RenderSystem.depthMask(false);
		try
		{
			ShaderInstance shader = shader(type, oitPass);
			if (shader == null)
				return indirectCommandOffset;
			geometry.bind();
			setupInstanceAttributes(this.instanceBuffer.vbo(), this.instanceBuffer.offset(frameSlot));
			try
			{
				shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, modelView, projection, PLibRenderHelper.mc().getWindow());
				bindLayerDepth(shader, oitPass);
				shader.apply();
				GlStreamStorage.bind(shader, PGpuDeformerBuffers.streams());
				this.indirectBuffer.upload(groups, frameSlot, indirectCommandOffset, this.persistentIndirectMapping, this.frameArena);
				if (this.persistentIndirectMapping)
					publishPersistentWrites();
				ARBMultiDrawIndirect.glMultiDrawElementsIndirect(GL11.GL_TRIANGLES, geometry.indexType(),
						this.indirectBuffer.offset(frameSlot, indirectCommandOffset), groups.size(), IndirectBuffer.STRIDE);
			}
			finally
			{
				disableInstanceAttributes();
			}
		}
		finally
		{
			if (!first.writeDepth())
				RenderSystem.depthMask(true);
			type.clearRenderState();
		}
		return Math.addExact(indirectCommandOffset, groups.size());
	}

	private int drawOpaquePipeline(List<PDrawGroup> groups, Matrix4f modelView, Matrix4f projection, int frameSlot,
	                               int indirectCommandOffset)
	{
		Map<ArenaBatchKey, List<PDrawGroup>> batches = new LinkedHashMap<>();
		List<PDrawGroup> directGroups = new ArrayList<>();
		for (PDrawGroup group : groups)
		{
			if (!this.resources.isGeometry(group.mesh()))
			{
				directGroups.add(group);
				continue;
			}
			GlGeometryArena.Slice slice = this.resources.geometry(group.mesh());
			batches.computeIfAbsent(new ArenaBatchKey(slice.vertexArray(), slice.indexType()), ignored -> new ArrayList<>()).add(group);
		}
		for (List<PDrawGroup> batch : batches.values())
		{
			if (batch.size() > 1)
				indirectCommandOffset = drawMulti(batch, modelView, projection, frameSlot, OitPass.NONE, indirectCommandOffset);
			else
				draw(batch.getFirst(), modelView, projection, frameSlot, OitPass.NONE);
		}
		for (PDrawGroup group : directGroups)
			draw(group, modelView, projection, frameSlot, OitPass.NONE);
		return indirectCommandOffset;
	}

	private @Nullable ShaderInstance shader(RenderType type, OitPass oitPass)
	{
		boolean emissive = PRenderTypes.RenderTypeProvider.usesEmissiveOit(type);
		return switch (oitPass)
		{
			case NONE -> RenderSystem.getShader();
			case DEPTH -> PRenderTypes.ShadersProvider.trianglesOitDepth(emissive);
			case DEPTH_PEEL -> PRenderTypes.ShadersProvider.trianglesOitDepthPeel(emissive);
			case ACCUMULATION -> PRenderTypes.ShadersProvider.trianglesOit(emissive);
		};
	}

	private void bindLayerDepth(ShaderInstance shader, OitPass oitPass)
	{
		if (oitPass == OitPass.DEPTH_PEEL)
			shader.setSampler("LayerDepthSampler", this.weightedBlendedOit.previousLayerDepthTexture());
		else if (oitPass == OitPass.ACCUMULATION)
			shader.setSampler("LayerDepthSampler", this.weightedBlendedOit.activeLayerDepthTexture());
	}

	private int findMultiDrawEnd(List<PDrawGroup> groups, int start)
	{
		PDrawGroup first = groups.get(start);
		if (!this.resources.isGeometry(first.mesh()))
			return start + 1;
		GlGeometryArena.Slice firstSlice = this.resources.geometry(first.mesh());
		int next = start + 1;
		while (next < groups.size())
		{
			PDrawGroup candidate = groups.get(next);
			if (!candidate.pipeline().equals(first.pipeline()) || candidate.writeDepth() != first.writeDepth() ||
					!this.resources.isGeometry(candidate.mesh()))
				break;
			GlGeometryArena.Slice slice = this.resources.geometry(candidate.mesh());
			if (slice.vertexArray() != firstSlice.vertexArray() || slice.indexType() != firstSlice.indexType())
				break;
			next++;
		}
		return next;
	}

	private static int findOpaquePipelineEnd(List<PDrawGroup> groups, int start)
	{
		PPipelineHandle pipeline = groups.get(start).pipeline();
		int next = start + 1;
		while (next < groups.size() && groups.get(next).writeDepth() && groups.get(next).pipeline().equals(pipeline))
			next++;
		return next;
	}

	private void resolveCapabilities()
	{
		if (this.capabilitiesResolved)
			return;
		this.capabilitiesResolved = true;
		this.capabilityMatrix = GlCapabilityProbe.currentContext();
		if (!this.capabilityMatrix.texelBuffer())
			throw new IllegalStateException("PulseLib's current OpenGL deformer driver requires texture-buffer support");
		if (!GL.getCapabilities().OpenGL33 && !GL.getCapabilities().GL_ARB_instanced_arrays)
			throw new IllegalStateException("PulseLib requires OpenGL 3.3 or ARB_instanced_arrays for instanced rendering");
		this.persistentMapping = this.capabilityMatrix.persistentMapping();
		this.persistentIndirectMapping = this.persistentMapping;
		this.multiDrawIndirect = MULTI_DRAW_INDIRECT_ENABLED && this.capabilityMatrix.supportsMultiDrawIndirect();
	}

	private void publishPersistentWrites()
	{
		if (this.persistentMapping)
			GL42.glMemoryBarrier(GL44.GL_CLIENT_MAPPED_BUFFER_BARRIER_BIT);
	}

	private static void setupInstanceAttributes(int vbo, long baseOffset)
	{
		long offset = baseOffset;
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
		for (int row = 0; row < 3; row++)
		{
			GL20.glEnableVertexAttribArray(4 + row);
			GL20.glVertexAttribPointer(4 + row, 4, GL11.GL_FLOAT, false, STRIDE, offset);
			instanceDivisor(4 + row, 1);
			offset += 16;
		}
		GL20.glEnableVertexAttribArray(7);
		GL20.glVertexAttribPointer(7, 4, GL11.GL_FLOAT, false, STRIDE, offset);
		instanceDivisor(7, 1);
		offset += 16;
		GL20.glEnableVertexAttribArray(8);
		GL20.glVertexAttribPointer(8, 2, GL11.GL_FLOAT, false, STRIDE, offset);
		instanceDivisor(8, 1);
		offset += 8;
		GL20.glEnableVertexAttribArray(9);
		GL20.glVertexAttribPointer(9, 2, GL11.GL_FLOAT, false, STRIDE, offset);
		instanceDivisor(9, 1);
		offset += 8;
		GL20.glEnableVertexAttribArray(10);
		GL30.glVertexAttribIPointer(10, 3, GL11.GL_INT, STRIDE, offset);
		instanceDivisor(10, 1);
	}

	private static void instanceDivisor(int attribute, int divisor)
	{
		if (GL.getCapabilities().OpenGL33)
			GL33.glVertexAttribDivisor(attribute, divisor);
		else
			ARBInstancedArrays.glVertexAttribDivisorARB(attribute, divisor);
	}

	private static void disableInstanceAttributes()
	{
		for (int attribute = 4; attribute <= 10; attribute++)
		{
			GL20.glDisableVertexAttribArray(attribute);
			instanceDivisor(attribute, 0);
		}
	}

	/**
	 * Batch identity for draw groups sharing a geometry arena and compatible render state.
	 *
	 * @param vertexArray the vertex array
	 * @param indexType the unsigned index element representation
	 */
	private record ArenaBatchKey(int vertexArray, int indexType)
	{
	}

	/**
	 * Transparency subpass selected while executing a render plan.
	 */
	private enum OitPass
	{
		/**
		 * Uses conventional rendering without an OIT subpass.
		 */
		NONE,
		/**
		 * Finds the nearest translucent depth layer.
		 */
		DEPTH,
		/**
		 * Finds the next translucent depth layer behind a preceding layer.
		 */
		DEPTH_PEEL,
		/**
		 * Accumulates weighted translucent color and revealage.
		 */
		ACCUMULATION
	}

	/**
	 * Streaming storage for per-instance transforms, colors, light and deformation offsets.
	 */
	private static final class InstanceBuffer
	{
		private int vbo = -1;
		private @Nullable ByteBuffer fallbackBuffer;
		private int capacity;
		private boolean persistent;
		private @Nullable ByteBuffer[] persistentMappings;

		private int vbo()
		{
			return this.vbo;
		}

		private long offset(int frameSlot)
		{
			return this.persistent ? (long)frameSlot * this.capacity : 0L;
		}

		private void upload(List<PInstanceHeader> instances, int frameSlot, boolean usePersistentMapping, GlFrameArena frameArena)
		{
			int required = instances.size() * STRIDE;
			if (usePersistentMapping)
			{
				ensurePersistentCapacity(required, frameArena);
				ByteBuffer target = this.persistentMappings[frameSlot];
				target.clear();
				for (PInstanceHeader instance : instances)
					write(target, instance);
				return;
			}
			ensureFallbackCapacity(required, frameArena);
			this.fallbackBuffer.clear();
			for (PInstanceHeader instance : instances)
				write(this.fallbackBuffer, instance);
			this.fallbackBuffer.flip();
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vbo);
			GL15.glBufferData(GL15.GL_ARRAY_BUFFER, this.fallbackBuffer, GL15.GL_DYNAMIC_DRAW);
		}

		private void ensureFallbackCapacity(int required, GlFrameArena frameArena)
		{
			if (this.persistent)
			{
				frameArena.awaitAll();
				closeGpuResources();
			}
			if (this.fallbackBuffer == null)
			{
				this.capacity = Math.max(required, 1024);
				this.fallbackBuffer = MemoryUtil.memAlloc(this.capacity);
				this.vbo = GL15.glGenBuffers();
				return;
			}
			if (this.vbo == -1)
				this.vbo = GL15.glGenBuffers();
			if (required > this.capacity)
			{
				this.capacity = Math.max(required, this.capacity * 2);
				this.fallbackBuffer = MemoryUtil.memRealloc(this.fallbackBuffer, this.capacity);
			}
		}

		private void ensurePersistentCapacity(int required, GlFrameArena frameArena)
		{
			if (this.persistent && required <= this.capacity)
				return;
			frameArena.awaitAll();
			closeGpuResources();
			if (this.fallbackBuffer != null)
			{
				MemoryUtil.memFree(this.fallbackBuffer);
				this.fallbackBuffer = null;
			}
			this.capacity = Math.max(required, 1024);
			this.vbo = GL15.glGenBuffers();
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vbo);
			long totalSize = (long)this.capacity * RING_SLOTS;
			int flags = GL30.GL_MAP_WRITE_BIT | ARBBufferStorage.GL_MAP_PERSISTENT_BIT | ARBBufferStorage.GL_MAP_COHERENT_BIT;
			ARBBufferStorage.glBufferStorage(GL15.GL_ARRAY_BUFFER, totalSize, flags);
			ByteBuffer mapping = GL30.glMapBufferRange(GL15.GL_ARRAY_BUFFER, 0, totalSize, flags, null);
			if (mapping == null)
				throw new IllegalStateException("Failed to persistently map PulseLib instance buffer");
			this.persistentMappings = new ByteBuffer[RING_SLOTS];
			for (int slot = 0; slot < RING_SLOTS; slot++)
			{
				ByteBuffer slice = mapping.duplicate();
				slice.position(slot * this.capacity).limit((slot + 1) * this.capacity);
				this.persistentMappings[slot] = slice.slice().order(ByteOrder.nativeOrder());
			}
			this.persistent = true;
		}

		private static void write(ByteBuffer target, PInstanceHeader instance)
		{
			Matrix4f matrix = instance.transform();
			target.putFloat(matrix.m00()).putFloat(matrix.m10()).putFloat(matrix.m20()).putFloat(matrix.m30());
			target.putFloat(matrix.m01()).putFloat(matrix.m11()).putFloat(matrix.m21()).putFloat(matrix.m31());
			target.putFloat(matrix.m02()).putFloat(matrix.m12()).putFloat(matrix.m22()).putFloat(matrix.m32());
			target.putFloat(FastColor.ARGB32.red(instance.packedColor()) / 255f);
			target.putFloat(FastColor.ARGB32.green(instance.packedColor()) / 255f);
			target.putFloat(FastColor.ARGB32.blue(instance.packedColor()) / 255f);
			target.putFloat(FastColor.ARGB32.alpha(instance.packedColor()) / 255f);
			target.putFloat(LightTexture.block(instance.packedLight()));
			target.putFloat(LightTexture.sky(instance.packedLight()));
			target.putFloat(instance.packedOverlay() & 0xFFFF);
			target.putFloat((instance.packedOverlay() >> 16) & 0xFFFF);
			target.putInt(instance.auxiliaryOperationOffset());
			target.putInt(instance.auxiliaryValueOffset());
			target.putInt(instance.auxiliaryOperationCount());
			target.putInt(0);
		}

		private void close()
		{
			closeGpuResources();
			if (this.fallbackBuffer != null)
				MemoryUtil.memFree(this.fallbackBuffer);
			this.fallbackBuffer = null;
		}

		private void closeGpuResources()
		{
			if (this.vbo != -1)
			{
				GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vbo);
				if (this.persistent)
					GL15.glUnmapBuffer(GL15.GL_ARRAY_BUFFER);
				GL15.glDeleteBuffers(this.vbo);
			}
			this.vbo = -1;
			this.persistent = false;
			this.persistentMappings = null;
		}
	}

	/**
	 * Streaming storage for packed indirect draw commands.
	 */
	private static final class IndirectBuffer
	{
		private static final int INTS_PER_COMMAND = 5;
		private static final int STRIDE = INTS_PER_COMMAND * Integer.BYTES;

		private int buffer = -1;
		private int capacity;
		private boolean persistent;
		private @Nullable ByteBuffer[] persistentMappings;

		private long offset(int frameSlot, int commandOffset)
		{
			if (commandOffset < 0)
				throw new IllegalArgumentException("Indirect command offset must not be negative");
			return this.persistent ? ((long)frameSlot * this.capacity + commandOffset) * STRIDE : 0L;
		}

		private void prepare(int required, boolean usePersistentMapping, GlFrameArena frameArena)
		{
			if (usePersistentMapping)
				ensurePersistentCapacity(required, frameArena);
		}

		private void upload(List<PDrawGroup> groups, int frameSlot, int commandOffset, boolean usePersistentMapping,
		                    GlFrameArena frameArena)
		{
			if (usePersistentMapping)
			{
				int required = Math.addExact(commandOffset, groups.size());
				if (!this.persistent || this.persistentMappings == null || required > this.capacity)
					throw new IllegalStateException("PulseLib indirect buffer was not prepared for all commands in this submission");
				ByteBuffer target = this.persistentMappings[frameSlot].duplicate().order(ByteOrder.nativeOrder());
				target.clear();
				target.position(Math.multiplyExact(commandOffset, STRIDE));
				write(target, groups);
				GL15.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, this.buffer);
				return;
			}
			ensureFallbackBuffer(frameArena);
			IntBuffer commands = MemoryUtil.memAllocInt(groups.size() * INTS_PER_COMMAND);
			try
			{
				for (PDrawGroup group : groups)
					write(commands, group.command());
				commands.flip();
				GL15.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, this.buffer);
				GL15.glBufferData(GL40.GL_DRAW_INDIRECT_BUFFER, commands, GL15.GL_DYNAMIC_DRAW);
			}
			finally
			{
				MemoryUtil.memFree(commands);
			}
		}

		private void ensureFallbackBuffer(GlFrameArena frameArena)
		{
			if (this.persistent)
			{
				frameArena.awaitAll();
				closeGpuResources();
			}
			if (this.buffer == -1)
				this.buffer = GL15.glGenBuffers();
		}

		private void ensurePersistentCapacity(int required, GlFrameArena frameArena)
		{
			if (this.persistent && required <= this.capacity)
				return;
			frameArena.awaitAll();
			closeGpuResources();
			this.capacity = Math.max(required, 64);
			this.buffer = GL15.glGenBuffers();
			GL15.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, this.buffer);
			long totalSize = (long)this.capacity * STRIDE * RING_SLOTS;
			int flags = GL30.GL_MAP_WRITE_BIT | ARBBufferStorage.GL_MAP_PERSISTENT_BIT | ARBBufferStorage.GL_MAP_COHERENT_BIT;
			ARBBufferStorage.glBufferStorage(GL40.GL_DRAW_INDIRECT_BUFFER, totalSize, flags);
			ByteBuffer mapping = GL30.glMapBufferRange(GL40.GL_DRAW_INDIRECT_BUFFER, 0, totalSize, flags, null);
			if (mapping == null)
				throw new IllegalStateException("Failed to persistently map PulseLib indirect buffer");
			this.persistentMappings = new ByteBuffer[RING_SLOTS];
			int frameBytes = this.capacity * STRIDE;
			for (int slot = 0; slot < RING_SLOTS; slot++)
			{
				ByteBuffer slice = mapping.duplicate();
				slice.position(slot * frameBytes).limit((slot + 1) * frameBytes);
				this.persistentMappings[slot] = slice.slice().order(ByteOrder.nativeOrder());
			}
			this.persistent = true;
		}

		private static void write(ByteBuffer target, List<PDrawGroup> groups)
		{
			for (PDrawGroup group : groups)
				write(target, group.command());
		}

		private static void write(ByteBuffer target, PDrawCommand command)
		{
			target.putInt(command.indexCount()).putInt(command.instanceCount()).putInt(command.firstIndex()).
					putInt(command.baseVertex()).putInt(command.firstInstance());
		}

		private static void write(IntBuffer target, PDrawCommand command)
		{
			target.put(command.indexCount()).put(command.instanceCount()).put(command.firstIndex()).
					put(command.baseVertex()).put(command.firstInstance());
		}

		private void close()
		{
			closeGpuResources();
		}

		private void closeGpuResources()
		{
			if (this.buffer != -1)
			{
				GL15.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, this.buffer);
				if (this.persistent)
					GL15.glUnmapBuffer(GL40.GL_DRAW_INDIRECT_BUFFER);
				GL15.glDeleteBuffers(this.buffer);
			}
			this.buffer = -1;
			this.capacity = 0;
			this.persistent = false;
			this.persistentMappings = null;
		}
	}
}
