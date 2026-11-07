/**
 * @author ArcAnc
 * Created at: 25.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test;

import com.arcanc.pulselib.content.mixin.GlBufferAccessor;
import com.arcanc.pulselib.content.model.baked.PDeformedMeshBuffers;
import com.arcanc.pulselib.content.model.baked.PGpuDeformedMeshBuffers;
import com.arcanc.pulselib.content.model.deformer.gpu.PGpuDeformerBuffers;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.legacy.GlDynamicGeometry;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.opengl.GlFence;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.opengl.GlTextureView;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.arcanc.pulselib.test.RegressionChecks.check;
import static com.arcanc.pulselib.test.RegressionChecks.field;

/** Captures live names before destruction; never compares deleted names after new allocations can reuse them. */
public final class RegressionResources
{
	private RegressionResources()
	{
	}

	static Object executor()
	{
		return field(PRenderQueue.class, "EXECUTOR", null);
	}

	static @Nullable Object value(Object owner, String name)
	{
		return field(owner.getClass(), name, owner);
	}

	static Map<?, ?> map(Object owner, String name)
	{
		return (Map<?, ?>)value(owner, name);
	}

	static Snapshot all()
	{
		Snapshot snapshot = new Snapshot();
		Object executor = executor();
		Object geometry = value(executor, "geometry");
		for (Object page : (List<?>)value(geometry, "pages"))
			snapshot.page(page);
		for (Object slots : map(geometry, "dynamicPages").values())
			for (Object allocation : ((Map<?, ?>)slots).values())
				snapshot.page(value(allocation, "page"));
		for (String name : List.of("instances", "indirect"))
			snapshot.buffer((int)value(value(executor, name), "buffer"));
		for (long fence : (long[])value(value(executor, "frameArena"), "fences"))
			snapshot.fence(fence);
		for (String name : List.of("stateVertices", "stateIndices"))
			snapshot.gpuBuffer((GpuBuffer)value(executor, name));
		Object projection = value(executor, "projectionBuffer");
		if (projection != null)
			snapshot.gpuBuffer((GpuBuffer)field(ProjectionMatrixBuffer.class, "buffer", projection));
		for (Object oit : map(executor, "weightedBlendedOits").values())
			snapshot.oit(oit);
		snapshot.target((RenderTarget)value(executor, "worldOpaqueDepth"));
		Object upload = field(PGpuDeformerBuffers.class, "UPLOAD_BUFFERS", null);
		for (String name : List.of("operations", "values"))
		{
			Object ring = value(upload, name);
			if (ring == null)
				continue;
			for (GpuBuffer buffer : (GpuBuffer[])field(MappableRingBuffer.class, "buffers", ring))
				snapshot.gpuBuffer(buffer);
			for (Object fence : (Object[])field(MappableRingBuffer.class, "fences", ring))
				if (fence != null)
					snapshot.fence((long)field(GlFence.class, "handle", fence));
		}
		for (Object sources : ((Map<?, ?>)field(PDeformedMeshBuffers.class, "BUFFERS", null)).values())
			for (Object source : ((Map<?, ?>)sources).values())
			{
				var geometrySource = ((GlDynamicGeometry)source).vertexBuffer();
				snapshot.closed.add(() -> check((boolean)value(geometrySource, "closed") && map(geometrySource, "closeListeners").isEmpty(), "CPU source and listeners must close"));
			}
		return snapshot;
	}

	public static Snapshot oit(Object oit)
	{
		Snapshot snapshot = new Snapshot();
		snapshot.oit(oit);
		return snapshot;
	}

	public static Snapshot target(RenderTarget target)
	{
		Snapshot snapshot = new Snapshot();
		snapshot.target(target);
		return snapshot;
	}

	static void empty()
	{
		Object executor = executor();
		Object arena = value(executor, "geometry");
		check(((List<?>)value(arena, "pages")).isEmpty() && map(arena, "geometrySlices").isEmpty() && map(arena, "dynamicPages").isEmpty(), "Geometry arenas must be empty");
		check(map(executor, "weightedBlendedOits").isEmpty() && value(executor, "worldOpaqueDepth") == null, "OIT and snapshot ownership must be empty");
		check(value(executor, "projectionBuffer") == null && value(executor, "stateVertices") == null && value(executor, "stateIndices") == null, "Pass buffers must be detached");
		check(!(boolean)value(executor, "worldDepthValid") && value(executor, "activeWeightedBlendedOit") == null && value(executor, "activeOitAttachments") == null, "No world frame may remain active");
		for (long fence : (long[])value(value(executor, "frameArena"), "fences"))
			check(fence == 0, "Frame fences must be reset");
		for (String name : List.of("instances", "indirect"))
		{
			Object stream = value(executor, name);
			check((int)value(stream, "buffer") == -1 && (int)value(stream, "capacity") == 0, "Stream buffer and capacity must reset");
			check(value(stream, "persistentMapping") == null && value(stream, "staging") == null && !(boolean)value(stream, "persistent"), "No mapping/staging allocation may survive");
		}
		Object registry = field(PRenderQueue.class, "RESOURCES", null);
		for (String name : List.of("pipelines", "pipelineResources", "meshes", "geometry", "dynamic", "meshResources"))
			check(map(registry, name).isEmpty(), "Registry retained " + name);
		Object compiler = field(PRenderQueue.class, "COMPILER", null);
		check(map(compiler, "opaque").isEmpty() && map(compiler, "translucent").isEmpty(), "Compiler must discard queued commands");
		check(((Map<?, ?>)field(PDeformedMeshBuffers.class, "BUFFERS", null)).isEmpty() && ((Map<?, ?>)field(PDeformedMeshBuffers.class, "SUBDIVIDED_SOURCES", null)).isEmpty(), "CPU deformation caches must clear");
		check(((Map<?, ?>)field(PGpuDeformedMeshBuffers.class, "GEOMETRIES", null)).isEmpty(), "GPU subdivision cache must clear");
		Object uploads = field(PGpuDeformerBuffers.class, "UPLOAD_BUFFERS", null);
		check(value(uploads, "operations") == null && value(uploads, "values") == null, "GPU deformation rings must detach");
		Object definitions = field(PGpuDeformerBuffers.class, "STREAM", null);
		check(map(definitions, "stacks").isEmpty() && map(definitions, "operationOffsets").isEmpty() && PGpuDeformerBuffers.operations().isEmpty() && PGpuDeformerBuffers.values().isEmpty(), "Deformer definitions must clear");
	}

	public static final class Snapshot
	{
		final Set<Integer> buffers = new HashSet<>();
		final Set<Integer> textures = new HashSet<>();
		final Set<Integer> framebuffers = new HashSet<>();
		private final Set<Integer> vertexArrays = new HashSet<>();
		private final Set<Long> fences = new HashSet<>();
		private final List<Runnable> closed = new ArrayList<>();

		private void buffer(int id)
		{
			if (id <= 0)
				return;
			check(GL15.glIsBuffer(id), "Captured buffer must be live");
			this.buffers.add(id);
		}

		private void gpuBuffer(@Nullable GpuBuffer buffer)
		{
			if (buffer == null)
				return;
			buffer(((GlBufferAccessor)buffer).pulselib$getHandle());
			this.closed.add(() -> check(buffer.isClosed(), "GPU buffer wrapper must close"));
		}

		private void fence(long id)
		{
			if (id == 0)
				return;
			check(GL32.glIsSync(id), "Captured fence must be live");
			this.fences.add(id);
		}

		private void page(Object page)
		{
			buffer((int)value(page, "vertexBuffer"));
			buffer((int)value(page, "indexBuffer"));
			int vao = (int)value(page, "vertexArray");
			check(GL30.glIsVertexArray(vao), "Captured arena VAO must be live");
			this.vertexArrays.add(vao);
		}

		private void fbo(int id)
		{
			if (id <= 0)
				return;
			check(GL30.glIsFramebuffer(id), "Captured FBO must be live");
			this.framebuffers.add(id);
		}

		private void attachment(@Nullable Object attachment)
		{
			if (attachment == null)
				return;
			if (attachment instanceof GpuTexture texture)
			{
				int id = ((GlTexture)texture).glId();
				check(GL11.glIsTexture(id), "Captured texture must be live");
				this.textures.add(id);
				this.closed.add(() -> check(texture.isClosed(), "Texture wrapper must close"));
			}
			else if (attachment instanceof GpuTextureView view)
				this.closed.add(() -> check(view.isClosed(), "Texture view must close"));
			Class<?> type = attachment instanceof GpuTexture ? GlTexture.class : GlTextureView.class;
			fbo((int)field(type, "firstFboId", attachment));
			Object cached = field(type, "fboCache", attachment);
			if (cached != null)
				for (Object id : ((Map<?, ?>)cached).values())
					fbo((int)id);
		}

		private void target(@Nullable RenderTarget target)
		{
			if (target == null)
				return;
			attachment(target.getColorTexture());
			attachment(target.getColorTextureView());
			attachment(target.getDepthTexture());
			attachment(target.getDepthTextureView());
		}

		private void oit(Object oit)
		{
			for (String name : List.of("accumulationTextures", "revealageTextures", "layerDepthTextures", "accumulationViews", "revealageViews", "layerDepthViews"))
				for (Object attachment : (Object[])value(oit, name))
					attachment(attachment);
			attachment(value(oit, "fabulousMetadataTexture"));
			attachment(value(oit, "fabulousMetadataView"));
			fbo((int)value(oit, "framebuffer"));
		}

		public boolean populated()
		{
			return !this.buffers.isEmpty() || !this.textures.isEmpty();
		}

		public void deleted(String boundary)
		{
			for (int id : this.buffers)
				check(!GL15.glIsBuffer(id), boundary + " retained buffer " + id);
			for (int id : this.textures)
				check(!GL11.glIsTexture(id), boundary + " retained texture " + id);
			for (int id : this.framebuffers)
				check(!GL30.glIsFramebuffer(id), boundary + " retained FBO " + id);
			for (int id : this.vertexArrays)
				check(!GL30.glIsVertexArray(id), boundary + " retained VAO " + id);
			for (long id : this.fences)
				check(!GL32.glIsSync(id), boundary + " retained fence " + id);
			this.closed.forEach(Runnable::run);
		}

		@Override
		public String toString()
		{
			return "buffers=" + this.buffers.size() + ", textures=" + this.textures.size() + ", fbos=" + this.framebuffers.size() + ", vaos=" + this.vertexArrays.size() + ", fences=" + this.fences.size();
		}
	}
}
