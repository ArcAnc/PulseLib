/**
 * @author ArcAnc
 * Created at: 10.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test;

import com.arcanc.pulselib.content.model.deformer.gpu.PGpuDeformerBuffers;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.gl.PGlInstanceStream;
import com.arcanc.pulselib.content.renderer.gl.PGlIndirectStream;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.util.PLibDatabase;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Exclusive profile, real storage-hook calibration and disposable-world prerequisites. */
public final class PerfRun
{
	private static int phase;
	private static long deadline;
	private static boolean completed;
	private static long windowDeadline;
	private static final List<GpuTiming.Result> CALIBRATION = new ArrayList<>();

	private PerfRun()
	{
	}

	public static boolean enabled()
	{
		return Boolean.getBoolean("pulselib.test.oitPerf");
	}

	public static boolean world()
	{
		return enabled() && Boolean.getBoolean("pulselib.test.world");
	}

	public static void tick()
	{
		if (deadline == 0)
			deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(15);
		DemoChecks.check(System.nanoTime() < deadline, "GPU profile deadline phase=" + phase);
		Minecraft mc = Minecraft.getInstance();
		if (phase == 0 && mc.getOverlay() == null && ClientTestPlugin.GLTF.getModel() != null)
		{
			DemoChecks.plugins(true);
			if (world())
			{
				GLFW.glfwSetWindowAttrib(mc.getWindow().handle(), GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);
				GLFW.glfwSetWindowSize(mc.getWindow().handle(), 1920, 1080);
				windowDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
			}
			GLFW.glfwHideWindow(mc.getWindow().handle());
			PRenderQueue.cleanup();
			GpuTiming.start();
			var instances = new PGlInstanceStream();
			var indirect = new PGlIndirectStream();
			try
			{
				instances.uploadHeaders(List.of(new PInstanceHeader(new Matrix4f(), -1, 0xF000F0, 0)), 0, false);
				indirect.begin(1, 0, false);
				indirect.upload(List.of(new PGlIndirectStream.Command(6, 1, 0, 0, 0)));
				PGpuDeformerBuffers.upload();
			}
			finally
			{
				instances.close();
				indirect.close();
			}
			GpuTiming.finish(true);
			GL11.glFlush();
			phase = 1;
		}
		if (phase == 1 && GpuTiming.drain(CALIBRATION))
		{
			if (world() && (mc.getMainRenderTarget().width != 1920 || mc.getMainRenderTarget().height != 1080))
			{
				DemoChecks.check(System.nanoTime() < windowDeadline, "Native undecorated world framebuffer must converge to 1920x1080");
				return;
			}
			DemoChecks.check(CALIBRATION.size() == 1 && CALIBRATION.getFirst().bufferReallocations() >= 4,
					"Actual fallback uploads and physical deformer ring creations must reach storage probes");
			PLibDatabase.LOGGER.info("GPU STORAGE CALIBRATION PASS: {} physical storage events", CALIBRATION.getFirst().bufferReallocations());
			PRenderQueue.cleanup();
			if (world())
				DisposableDemoWorlds.create();
			phase = 2;
		}
	}

	public static void frame()
	{
		if (enabled() && phase == 2)
			OitPerformanceHarness.frame();
	}

	static Path output()
	{
		return DemoChecks.runDirectory().resolve("h14-results");
	}

	static void complete(String kind, int configurations, int warmup, int samples)
	{
		completed = true;
		Map<String, Integer> queries = GpuTiming.verifyRetired();
		DemoChecks.check(queries.get("textureAllocations") > 0, "Real warmup OIT texture allocations must reach the probe");
		DemoChecks.writeResult(Map.of("status", "PASS", "fixture", "gpu-performance", "kind", kind,
				"origin", DemoChecks.jar(PerfRun.class), "configurations", configurations, "warmup", warmup, "samples", samples,
				"queries", queries, "calibrationStorageEvents", CALIBRATION.getFirst().bufferReallocations()));
	}

	public static void stopped()
	{
		DemoChecks.check(completed, "Full GPU profile must complete before shutdown");
		GpuTiming.verifyRetired();
		PLibDatabase.LOGGER.info("GPU PERFORMANCE SHUTDOWN PASS");
	}
}
