/**
 * @author ArcAnc
 * Created at: 20.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.porting.bench;

import com.arcanc.pulselib.content.renderer.PRenderContexts;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.PRenderQueue.RenderStage;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.content.renderer.plan.PTransparencyMode;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.PResourceCache;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.sun.management.ThreadMXBean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.opengl.ARBDirectStateAccess;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.NVXGPUMemoryInfo;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class RendererBenchmark
{
	private static final int SIZE = 512;
	private static final int WARMUP = 180;
	private static final int SAMPLES = 120;
	private static final int STRESS_FRAMES = 2400;
	private static boolean done;
	private static boolean measuring;
	private static int draws;
	private static int batches;
	private static long uploads;

	private RendererBenchmark()
	{
	}

	public static void draw(int count)
	{
		if (measuring && count > 0)
		{
			draws++;
			batches += count;
		}
	}

	public static void upload(int bytes)
	{
		if (measuring)
			uploads += bytes;
	}

	public static void tick()
	{
		var client = Minecraft.getInstance();
		if (done || !client.isGameLoadFinished())
			return;
		done = true;
		try
		{
			check(GL.getCapabilities().GL_ARB_timer_query && GL.getCapabilities().GL_NVX_gpu_memory_info, "GL timer and memory stats required");
			System.out.println("PulseLib benchmark renderer: " + GL11.glGetString(GL11.GL_RENDERER) + " / " + GL11.glGetString(GL11.GL_VERSION));
			run();
			System.out.println("PASS equivalent queue workloads and 2400-frame resize/cleanup stress");
			client.stop();
		}
		catch (IOException failure)
		{
			throw new IllegalStateException(failure);
		}
	}

	private static void run() throws IOException
	{
		var previousMatrix = RenderSystem.getProjectionMatrixBuffer();
		var previousProjection = RenderSystem.getProjectionType();
		var color = RenderSystem.outputColorTextureOverride;
		var depth = RenderSystem.outputDepthTextureOverride;
		var stack = RenderSystem.getModelViewStack();
		stack.pushMatrix().identity();
		try (var buffer = new ProjectionMatrixBuffer("PulseLib benchmark");
				var samples = Files.newBufferedWriter(Path.of("pulselib-benchmark.csv")))
		{
			// Reference uses forward depth; 26.2 uses reversed depth.
			var matrix = new Matrix4f().scaling(-1, 1, BenchmarkCounters.REVERSED ? -1 : 1);
			RenderSystem.setProjectionMatrix(buffer.getBuffer(matrix), ProjectionType.ORTHOGRAPHIC);
			samples.write("mode,frame,cpu_ns,gpu_ns,synchronized_ns,thread_alloc_bytes,draw_calls,batches,stream_bytes\n");
			for (Mode mode : Mode.values())
				measure(mode, scene(mode), samples);
			stress(scene(Mode.OIT));
		}
		finally
		{
			measuring = false;
			PRenderQueue.cleanup();
			RenderSystem.outputColorTextureOverride = color;
			RenderSystem.outputDepthTextureOverride = depth;
			RenderSystem.setProjectionMatrix(previousMatrix, previousProjection);
			stack.popMatrix();
		}
	}

	private static void measure(Mode mode, List<Submission> submissions, BufferedWriter samples) throws IOException
	{
		var target = BenchmarkCounters.target(SIZE);
		select(target);
		int query = GL15.glGenQueries();
		try
		{
			for (int i = 0; i < WARMUP; i++)
			{
				BenchmarkCounters.clear(target);
				frame(mode, target, submissions);
				GL11.glFinish();
				BenchmarkCounters.finishFrame();
			}
			checkPixels(target);
			ThreadMXBean threads = (ThreadMXBean)ManagementFactory.getThreadMXBean();
			threads.setThreadAllocatedMemoryEnabled(true);
			long thread = Thread.currentThread().threadId();
			for (int i = 0; i < SAMPLES; i++)
			{
				BenchmarkCounters.clear(target);
				GL11.glFinish();
				draws = batches = 0;
				uploads = 0;
				long bytes = threads.getThreadAllocatedBytes(thread);
				GL15.glBeginQuery(GL33.GL_TIME_ELAPSED, query);
				long begin = System.nanoTime();
				measuring = true;
				frame(mode, target, submissions);
				measuring = false;
				long cpu = System.nanoTime() - begin;
				GL15.glEndQuery(GL33.GL_TIME_ELAPSED);
				long gpu = GL33.glGetQueryObjectui64(query, GL15.GL_QUERY_RESULT);
				long elapsed = System.nanoTime() - begin;
				long allocated = threads.getThreadAllocatedBytes(thread) - bytes;
				BenchmarkCounters.finishFrame();
				check(draws > 0 && uploads > 0 && gpu > 0, "Missing actual buffer upload, native draw or GPU timing");
				samples.write(mode + "," + i + "," + cpu + "," + gpu + "," + elapsed + "," + allocated + "," + draws + "," + batches + "," + uploads + "\n");
			}
			System.out.println("BENCH coverage " + mode + " " + checkPixels(target));
		}
		finally
		{
			GL15.glDeleteQueries(query);
			release(target);
		}
	}

	private static void stress(List<Submission> submissions)
	{
		for (int cycle = 0; cycle < 12; cycle++)
		{
			var target = BenchmarkCounters.target(cycle % 2 == 0 ? SIZE : SIZE / 2);
			select(target);
			try
			{
				for (int i = 0; i < STRESS_FRAMES / 12; i++)
				{
					BenchmarkCounters.clear(target);
					frame(Mode.OIT, target, submissions);
					GL11.glFinish();
					BenchmarkCounters.finishFrame();
				}
				checkPixels(target);
				System.out.println("BENCH memory active KiB " + cycle + " " + availableMemory());
			}
			finally
			{
				release(target);
			}
			System.out.println("BENCH memory released KiB " + cycle + " " + availableMemory());
		}
	}

	private static int availableMemory()
	{
		GL11.glFinish();
		return GL11.glGetInteger(NVXGPUMemoryInfo.GL_GPU_MEMORY_INFO_CURRENT_AVAILABLE_VIDMEM_NVX);
	}

	private static void select(TextureTarget target)
	{
		RenderSystem.outputColorTextureOverride = target.getColorTextureView();
		RenderSystem.outputDepthTextureOverride = target.getDepthTextureView();
	}

	private static void release(TextureTarget target)
	{
		PRenderQueue.cleanup();
		RenderSystem.outputColorTextureOverride = null;
		RenderSystem.outputDepthTextureOverride = null;
		target.destroyBuffers();
	}

	private static int checkPixels(TextureTarget target)
	{
		var texture = (GlTexture)target.getColorTexture();
		int size = texture.getWidth(0);
		var bytes = ByteBuffer.allocateDirect(size * size * 4);
		ARBDirectStateAccess.glGetTextureImage(texture.glId(), 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, bytes);
		int coverage = 0;
		for (int index = 3; index < bytes.capacity(); index += 4)
			if ((bytes.get(index) & 255) > 10)
				coverage++;
		check(coverage > size * size / 16 && coverage < size * size / 2, "Unexpected output coverage " + coverage + " for size " + size);
		return coverage;
	}

	private static List<Submission> scene(Mode mode)
	{
		var type = mode == Mode.OPAQUE ? PRenderTypes.RenderTypeProvider.trianglesEmissiveSolid(PResourceCache.ATLAS_LOCATION) :
				PRenderTypes.RenderTypeProvider.trianglesEmissiveTranslucent(PResourceCache.ATLAS_LOCATION);
		var atlas = PResourceCache.getTextureAtlas();
		var sprite = atlas.getSprite(Identifier.withDefaultNamespace("missingno"));
		List<PGeometryData> meshes = new ArrayList<>();
		for (int index = 0; index < 16; index++)
		{
			var vertices = ByteBuffer.allocate(72).order(ByteOrder.nativeOrder());
			for (float[] point : new float[][]{{-1, -1}, {0, 1}, {1, -1}})
				vertices.putFloat(point[0]).putFloat(point[1]).putFloat(0).
						putFloat(sprite.getU0() * 0.75f + sprite.getU1() * 0.25f).
						putFloat(sprite.getV0() * 0.75f + sprite.getV1() * 0.25f).
						put((byte)0).put((byte)0).put((byte)127).put((byte)0);
			var indices = ByteBuffer.allocate(6).order(ByteOrder.nativeOrder()).putShort((short)0).putShort((short)1).putShort((short)2);
			meshes.add(new PGeometryData(vertices.flip(), indices.flip(), 24, 3, PGeometryData.IndexType.UNSIGNED_SHORT));
		}
		List<Submission> submissions = new ArrayList<>();
		for (int index = 0; index < 768; index++)
		{
			int columns = mode == Mode.OPAQUE ? 32 : 16;
			int grid = mode == Mode.OPAQUE ? index : index % 192;
			float depth = mode == Mode.OPAQUE ? -0.4f : -0.8f + 0.2f * (index / 192);
			float x = (grid % columns + 0.5f) * 2 / columns - 1;
			float y = (grid / columns + 0.5f) * 2 / (columns * 3 / 4) - 1;
			var transform = new Matrix4f().translation(x, y, depth).scale(0.65f / columns);
			var header = new PInstanceHeader(transform, mode == Mode.OPAQUE ? -1 : 0x80FFFFFF, 0xF000F0, OverlayTexture.NO_OVERLAY);
			submissions.add(new Submission(type, meshes.get(index % 16), header));
		}
		return submissions;
	}

	private static void frame(Mode mode, TextureTarget target, List<Submission> submissions)
	{
		try (var scope = PRenderContexts.enter(PRenderContexts.Context.WORLD))
		{
			for (var submission : submissions)
				PRenderQueue.submit(RenderStage.ENTITIES, submission.type(), submission.mesh(), submission.header(),
						mode == Mode.OIT ? PTransparencyMode.OIT : PTransparencyMode.ORDINARY);
			if (mode == Mode.OPAQUE)
				PRenderQueue.flushOpaque(RenderStage.ENTITIES);
			else
			{
				PRenderQueue.captureWorldDepth(target);
				PRenderQueue.flushWorld();
				if (mode == Mode.OIT)
					PRenderQueue.compositeTranslucency(target);
			}
			PRenderQueue.endWorldFrame();
		}
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
			throw new AssertionError(message);
	}

	private enum Mode
	{
		OPAQUE,
		ALPHA,
		OIT
	}

	private record Submission(RenderType type, PGeometryData mesh, PInstanceHeader header)
	{
	}
}
