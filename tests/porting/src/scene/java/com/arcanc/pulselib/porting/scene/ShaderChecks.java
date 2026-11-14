/**
 * @author ArcAnc
 * Created at: 14.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.porting.scene;

import com.arcanc.pulselib.content.model.deformer.PBendDeformer;
import com.arcanc.pulselib.content.model.deformer.PBendDefinition;
import com.arcanc.pulselib.content.model.deformer.PChannelReference;
import com.arcanc.pulselib.content.model.deformer.PDeformerInstance;
import com.arcanc.pulselib.content.model.deformer.PDeformerStack;
import com.arcanc.pulselib.content.model.deformer.PDeformerValueSource;
import com.arcanc.pulselib.content.model.deformer.PMeshDeformation;
import com.arcanc.pulselib.content.model.deformer.PStretchDeformer;
import com.arcanc.pulselib.content.model.deformer.PStretchDefinition;
import com.arcanc.pulselib.content.model.deformer.PTwistDeformer;
import com.arcanc.pulselib.content.model.deformer.PTwistDefinition;
import com.arcanc.pulselib.content.model.deformer.gpu.PGpuDeformerBuffers;
import com.arcanc.pulselib.content.renderer.gl.PGlMultiDrawExecutor;
import com.arcanc.pulselib.content.renderer.gl.PGlResourceRegistry;
import com.arcanc.pulselib.content.renderer.plan.PDrawCommand;
import com.arcanc.pulselib.content.renderer.plan.PFrameCompiler;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.content.renderer.plan.PPipelineHandle;
import com.arcanc.pulselib.content.renderer.plan.PTransparencyMode;
import com.arcanc.pulselib.content.renderer.rhi.PRhiDrawExecutor;
import com.arcanc.pulselib.content.renderer.rhi.PRhiInstanceLayout;
import com.arcanc.pulselib.content.renderer.rhi.PRhiNativeMaterial;
import com.arcanc.pulselib.content.renderer.rhi.PRhiRenderHost;
import com.arcanc.pulselib.content.renderer.rhi.PRhiResourceRegistry;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Real canonical shaders, native preparation and packed GPU streams; not a world/GUI/OIT acceptance test. */
public final class ShaderChecks
{
	private ShaderChecks()
	{
	}

	public static int run(GpuDevice device, GpuTexture color, GpuTexture depth, Identifier texture) throws Exception
	{
		List<PDeformerStack> stacks = List.of(
				PDeformerStack.compile(List.of(new PDeformerInstance<>(PStretchDeformer.INSTANCE,
						new PStretchDefinition(new Vector3f(), new Vector3f(1, 0, 0), PChannelReference.constant(0.65f))))),
				PDeformerStack.compile(List.of(new PDeformerInstance<>(PTwistDeformer.INSTANCE,
						new PTwistDefinition(new Vector3f(), new Vector3f(0, 1, 0), 2, 2, PChannelReference.constant(0.7f))))),
				PDeformerStack.compile(List.of(new PDeformerInstance<>(PBendDeformer.INSTANCE,
						new PBendDefinition(new Vector3f(), new Vector3f(0, 1, 0), new Vector3f(1, 0, 0), 2, 2, PChannelReference.constant(0.5f))))));
		int draws = 0;
		try
		{
			for (boolean emissive : new boolean[]{false, true})
				for (int test = 0; test < stacks.size(); test++)
				{
					PDeformerStack stack = stacks.get(test);
					PGpuDeformerBuffers.submit(new PMeshDeformation(stacks.getFirst(), reference -> 1.8f, new Object(), 0));
					var deformation = new PMeshDeformation(stack, PDeformerValueSource.DEFAULTS, stack, 0);
					PGpuDeformerBuffers.Submission submission = PGpuDeformerBuffers.submit(deformation);
					SceneChecks.check(submission.enabled() && submission.valueOffset() > 0, "Fixture must exercise real GPU operations and a nonzero value offset");
					PGeometryData original = SceneChecks.geometry(0);
					PGeometryData cpu = deform(original, stack);
					try (var executor = new Executor(device, texture, emissive))
					{
						var transform = new Matrix4f().scale(0.65f);
						var header = new PInstanceHeader(transform, -1, 0xF000F0, OverlayTexture.NO_OVERLAY);
						SceneChecks.clear(device, color, depth);
						executor.draw(original, List.of(header));
						ByteBuffer baseline = SceneChecks.pixels(device, color);
						SceneChecks.clear(device, color, depth);
						executor.draw(cpu, List.of(header));
						ByteBuffer expected = SceneChecks.pixels(device, color);
						SceneChecks.clear(device, color, depth);
						var gpu = new PInstanceHeader(transform, -1, 0xF000F0, OverlayTexture.NO_OVERLAY,
								submission.operationOffset(), submission.valueOffset(), submission.operationCount());
						executor.draw(original, List.of(gpu));
						ByteBuffer actual = SceneChecks.pixels(device, color);
						compare(expected, actual, baseline, "deformer-" + test + (emissive ? "-emissive" : "-lit"));
						draws += 3;
						if (test == 0)
						{
							List<PInstanceHeader> cpuInstances = new ArrayList<>();
							List<PInstanceHeader> gpuInstances = new ArrayList<>();
							for (int i = 0; i < 513; i++)
							{
								PInstanceHeader instance = SceneChecks.instance(i);
								cpuInstances.add(instance);
								gpuInstances.add(new PInstanceHeader(instance.transform(), instance.packedColor(), instance.packedLight(), instance.packedOverlay(),
										submission.operationOffset(), submission.valueOffset(), submission.operationCount()));
							}
							SceneChecks.clear(device, color, depth);
							executor.draw(cpu, cpuInstances);
							expected = SceneChecks.pixels(device, color);
							SceneChecks.clear(device, color, depth);
							executor.draw(original, gpuInstances);
							actual = SceneChecks.pixels(device, color);
							SceneChecks.check(expected.equals(actual), "513 deformed instances must match CPU across UBO chunks");
							draws += 2;
							System.out.println("PASS canonical 513 GPU/CPU instance chunks emissive=" + emissive);
						}
					}
					PGpuDeformerBuffers.finishFrame();
				}
		}
		finally
		{
			PGpuDeformerBuffers.cleanup();
		}
		return draws;
	}

	/** Checks the isolated legacy consumer of the changed canonical shader ABI, without redirecting the production queue. */
	public static int legacy(GpuDevice device, GpuTexture color, GpuTexture depth, Identifier texture)
	{
		int draws = 0;
		for (boolean emissive : new boolean[]{false, true})
			for (boolean cutout : new boolean[]{false, true})
			{
				var resources = new PGlResourceRegistry();
				var executor = new PGlMultiDrawExecutor(resources);
				try
				{
					var pipeline = resources.pipeline(SceneChecks.type(texture, cutout, emissive));
					var geometry = resources.geometry(SceneChecks.geometry(0));
					var secondGeometry = resources.geometry(SceneChecks.geometry(0));
					var compiler = new PFrameCompiler<Integer>();
					for (int i = 0; i < 1025; i++)
						compiler.submit(0, pipeline, i % 2 == 0 ? geometry : secondGeometry, 3, SceneChecks.instance(i), false);
					compiler.submit(0, pipeline, geometry, new PDrawCommand(0, 1, 0, 0, 0), SceneChecks.instance(0), false);
					SceneChecks.clear(device, color, depth);
					executor.execute(compiler.compile(0), new PRhiRenderHost().captureFrame());
					ByteBuffer pixels = SceneChecks.pixels(device, color);
					for (int i = 0; i < 1025; i++)
					{
						int x = (int)((i % 34 + 0.5f) / 34 * SceneChecks.SIZE), y = (int)((i / 34 + 0.5f) / 34 * SceneChecks.SIZE);
						SceneChecks.check((SceneChecks.pixel(pixels, x, y) >>> 24) == 255, "Legacy UBO instance " + i);
					}
					draws++;
					System.out.println("PASS compatibility canonical UBO: 1025 instances, two mesh offsets, packed ranges, zero-index command, cutout=" + cutout + ", emissive=" + emissive);
				}
				finally
				{
					executor.cleanup();
				}
			}
		return draws;
	}

	private static PGeometryData deform(PGeometryData original, PDeformerStack stack)
	{
		ByteBuffer source = original.vertices().order(ByteOrder.nativeOrder());
		ByteBuffer vertices = ByteBuffer.allocate(source.remaining()).order(ByteOrder.nativeOrder());
		while (source.hasRemaining())
		{
			var position = new Vector3f(source.getFloat(), source.getFloat(), source.getFloat());
			float u = source.getFloat(), v = source.getFloat();
			var normal = new Vector3f(source.get() / 127f, source.get() / 127f, source.get() / 127f);
			source.get();
			Vector3f deformed = stack.deform(position, PDeformerValueSource.DEFAULTS);
			normal = stack.deformNormal(position, normal, PDeformerValueSource.DEFAULTS);
			vertices.putFloat(deformed.x).putFloat(deformed.y).putFloat(deformed.z).putFloat(u).putFloat(v).
					put((byte)Math.round(normal.x * 127)).put((byte)Math.round(normal.y * 127)).put((byte)Math.round(normal.z * 127)).put((byte)0);
		}
		return new PGeometryData(vertices.flip(), original.indices(), 24, original.indexCount(), original.indexType());
	}

	private static void compare(ByteBuffer expected, ByteBuffer actual, ByteBuffer baseline, String name) throws Exception
	{
		int visible = 0, coverage = 0, changed = 0, maximum = 0;
		long error = 0;
		for (int p = 0; p < expected.limit(); p += 4)
		{
			int alpha = expected.get(p + 3) & 255;
			if (alpha != (actual.get(p + 3) & 255))
				coverage++;
			if (alpha != (baseline.get(p + 3) & 255))
				changed++;
			if (alpha == 0 || actual.get(p + 3) == 0)
				continue;
			visible++;
			for (int c = 0; c < 3; c++)
			{
				int delta = Math.abs((expected.get(p + c) & 255) - (actual.get(p + c) & 255));
				maximum = Math.max(maximum, delta);
				error += delta;
			}
		}
		System.out.println(name + " CPU/GPU visible=" + visible + " coverageDiff=" + coverage + " maxRGB=" + maximum + " totalRGB=" + error + " changedCoverage=" + changed);
		SceneChecks.check(visible > 100 && changed > 50 && coverage <= 8 && maximum <= 5 && error <= visible * 3L,
				name + " CPU/GPU deformation or normal lighting differs");
		SceneChecks.image(actual, name);
		System.out.println("PASS canonical " + name + " CPU/GPU position/normal lighting readback");
	}

	private static final class Executor implements AutoCloseable
	{
		private final PRhiResourceRegistry resources;
		private final PRhiDrawExecutor executor;
		private final PPipelineHandle pipeline;

		private Executor(GpuDevice device, Identifier texture, boolean emissive)
		{
			this.resources = new PRhiResourceRegistry(device);
			this.executor = new PRhiDrawExecutor(device, this.resources);
			this.pipeline = new PRhiNativeMaterial(SceneChecks.type(texture, false, emissive), PRhiInstanceLayout.PORTABLE_CAPACITY, PTransparencyMode.ORDINARY,
					Map.of("DeformerOperations", () -> PGpuDeformerBuffers.upload().operations().slice(),
							"DeformerValues", () -> PGpuDeformerBuffers.upload().values().slice())).register(this.resources);
		}

		private void draw(PGeometryData geometry, List<PInstanceHeader> instances)
		{
			var mesh = this.resources.geometry(geometry);
			var compiler = new PFrameCompiler<Integer>();
			for (var instance : instances)
				compiler.submit(0, this.pipeline, mesh, geometry.indexCount(), instance, false);
			this.executor.execute(compiler.compile(0), new PRhiRenderHost().captureFrame());
		}

		@Override
		public void close()
		{
			this.executor.cleanup();
		}
	}
}
