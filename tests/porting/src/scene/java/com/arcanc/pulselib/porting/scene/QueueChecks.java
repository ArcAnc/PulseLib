/**
 * @author ArcAnc
 * Created at: 14.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.porting.scene;

import com.arcanc.pulselib.content.model.PMaterial;
import com.arcanc.pulselib.content.model.PMeshPrimitive;
import com.arcanc.pulselib.content.model.baked.PBakedMesh;
import com.arcanc.pulselib.content.model.baked.PDeformedMeshBuffers;
import com.arcanc.pulselib.content.model.deformer.PBendDeformer;
import com.arcanc.pulselib.content.model.deformer.PBendDefinition;
import com.arcanc.pulselib.content.model.deformer.PChannelReference;
import com.arcanc.pulselib.content.model.deformer.PDeformerInstance;
import com.arcanc.pulselib.content.model.deformer.PDeformerPrepareContext;
import com.arcanc.pulselib.content.model.deformer.PDeformerStack;
import com.arcanc.pulselib.content.model.deformer.PDeformerValueSource;
import com.arcanc.pulselib.content.model.deformer.PHingeDeformer;
import com.arcanc.pulselib.content.model.deformer.PHingeDefinition;
import com.arcanc.pulselib.content.model.deformer.PMeshDeformation;
import com.arcanc.pulselib.content.model.deformer.PMeshDeformer;
import com.arcanc.pulselib.content.model.deformer.PSquashDeformer;
import com.arcanc.pulselib.content.model.deformer.PSquashDefinition;
import com.arcanc.pulselib.content.model.deformer.PStretchDeformer;
import com.arcanc.pulselib.content.model.deformer.PStretchDefinition;
import com.arcanc.pulselib.content.model.deformer.PTaperDeformer;
import com.arcanc.pulselib.content.model.deformer.PTaperDefinition;
import com.arcanc.pulselib.content.model.deformer.PTwistDeformer;
import com.arcanc.pulselib.content.model.deformer.PTwistDefinition;
import com.arcanc.pulselib.content.model.deformer.PWaveDeformer;
import com.arcanc.pulselib.content.model.deformer.PWaveDefinition;
import com.arcanc.pulselib.content.model.deformer.gpu.PGpuDeformerBuffers;
import com.arcanc.pulselib.content.model.deformer.gpu.PGpuDeformerStack;
import com.arcanc.pulselib.content.model.textures.PAlphaMode;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.content.renderer.plan.PRenderFrame;
import com.arcanc.pulselib.content.renderer.plan.PTransparencyMode;
import com.arcanc.pulselib.content.renderer.rhi.PRhiRenderHost;
import com.arcanc.pulselib.util.PRenderTypes;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Public production queue/immediate calls only; observes native dispatch without substituting preparation. */
public final class QueueChecks
{
	private QueueChecks()
	{
	}

	public static int run(GpuDevice device, GpuTexture color, GpuTexture depth, Identifier texture, Identifier hidden) throws Exception
	{
		int[] initial = SceneChecks.counters();
		List<PDeformerInstance<?>> definitions = List.of(
				new PDeformerInstance<>(PStretchDeformer.INSTANCE, new PStretchDefinition(new Vector3f(), new Vector3f(1, 0, 0), PChannelReference.constant(0.65f))),
				new PDeformerInstance<>(PSquashDeformer.INSTANCE, new PSquashDefinition(new Vector3f(), new Vector3f(0, 1, 0), PChannelReference.constant(0.7f))),
				new PDeformerInstance<>(PTaperDeformer.INSTANCE, new PTaperDefinition(new Vector3f(), new Vector3f(0, 1, 0), 2, 2, PChannelReference.constant(0.5f))),
				new PDeformerInstance<>(PTwistDeformer.INSTANCE, new PTwistDefinition(new Vector3f(), new Vector3f(0, 1, 0), 2, 2, PChannelReference.constant(0.7f))),
				new PDeformerInstance<>(PBendDeformer.INSTANCE, new PBendDefinition(new Vector3f(), new Vector3f(0, 1, 0), new Vector3f(1, 0, 0), 2, 2, PChannelReference.constant(0.5f))),
				new PDeformerInstance<>(PWaveDeformer.INSTANCE, new PWaveDefinition(new Vector3f(), new Vector3f(0, 1, 0), new Vector3f(1, 0, 0), 2, 2, 4.1f, PChannelReference.constant(0.3f), PChannelReference.constant(0.7f))),
				new PDeformerInstance<>(PHingeDeformer.INSTANCE, new PHingeDefinition(new Vector3f(0.1f, 0, 0.5f), new Vector3f(1, 0, 0), new Vector3f(0, 0, 1), PChannelReference.constant(0.3f))));
		List<PDeformerStack> stacks = new ArrayList<>();
		for (var definition : definitions)
			stacks.add(PDeformerStack.compile(List.of(definition)));
		stacks.add(PDeformerStack.compose(stacks.get(0), stacks.get(3)));
		var small = new PDeformerInstance<>(PStretchDeformer.INSTANCE, new PStretchDefinition(new Vector3f(), new Vector3f(1, 0, 0), PChannelReference.constant(0.98f)));
		stacks.add(PDeformerStack.compile(Collections.nCopies(8, small)));
		stacks.add(PDeformerStack.compile(Collections.nCopies(9, small)));
		stacks.add(PDeformerStack.compile(List.of(new PDeformerInstance<>(new CpuOnly(), 0.25f))));
		try
		{
			for (boolean emissive : new boolean[]{false, true})
				for (boolean cutout : new boolean[]{false, true})
				{
					RenderType type = SceneChecks.type(texture, cutout, emissive);
					PBakedMesh mesh = baked(SceneChecks.geometry(0), texture, cutout, emissive);
					for (int test = 0; test < stacks.size(); test++)
					{
						int level = test % 3;
						var deformation = new PMeshDeformation(stacks.get(test), PDeformerValueSource.DEFAULTS, new Object(), level);
						PGeometryData reference = PDeformedMeshBuffers.resolve(mesh, deformation).vertexBuffer().geometry();
						var header = new PInstanceHeader(new Matrix4f().scale(0.65f), -1, 0xF000F0, OverlayTexture.NO_OVERLAY);
						SceneChecks.clear(device, color, depth);
						queue(type, mesh, null, header);
						ByteBuffer baseline = SceneChecks.pixels(device, color);
						SceneChecks.clear(device, color, depth);
						PRenderQueue.submit(PRenderQueue.RenderStage.ENTITIES, type, reference, header);
						PRenderQueue.flushOpaque(PRenderQueue.RenderStage.ENTITIES);
						ByteBuffer expected = SceneChecks.pixels(device, color);
						PGpuDeformerBuffers.submit(new PMeshDeformation(stacks.getFirst(), ref -> 1.8f, new Object(), 0));
						SceneChecks.clear(device, color, depth);
						queue(type, mesh, deformation, header);
						compare(expected, SceneChecks.pixels(device, color), baseline, "queue-" + test + "-" + emissive + "-" + cutout + "-level" + level);
						PGpuDeformerBuffers.finishFrame();
					}
				}
			instanced(device, color, depth, texture, stacks.getFirst());
			immediate(device, color, depth, texture, hidden, stacks.get(3));
			System.out.println("PASS public queue: all 7 operations, hinge mask, composed stack, 8-operation GPU limit, 9-operation/custom CPU fallbacks, subdivision 0/1/2");
		}
		finally
		{
			PRenderQueue.cleanup();
		}
		int[] after = SceneChecks.counters();
		SceneChecks.check(after[0] - initial[0] == 185 && after[1] == initial[1] && after[2] - initial[2] == 61,
				"All queue/immediate cases must execute through RHI, with exactly 61 nonzero deformation plans");
		return 185;
	}

	private static void queue(RenderType type, PBakedMesh mesh, @Nullable PMeshDeformation deformation, PInstanceHeader header)
	{
		int[] before = SceneChecks.counters();
		PRenderQueue.submit(PRenderQueue.RenderStage.ENTITIES, type, mesh, deformation,
				new PRenderQueue.InstanceData(header.transform(), header.packedColor(), header.packedLight(), header.packedOverlay()), PTransparencyMode.ORDINARY);
		PRenderQueue.flushOpaque(PRenderQueue.RenderStage.ENTITIES);
		boolean gpu = deformation != null && PGpuDeformerStack.compile(deformation.stack()).filter(stack -> stack.operationCount() > 0).isPresent();
		checkDispatch(before, type, gpu);
	}

	private static void checkDispatch(int[] before, RenderType type, boolean gpu)
	{
		int[] after = SceneChecks.counters();
		SceneChecks.check(after[0] == before[0] + 1 && after[1] == before[1] && after[2] == before[2] + (gpu ? 1 : 0), "Public API must dispatch exactly once through RHI with the expected deformation header");
		SceneChecks.check(SceneChecks.lastPipeline().equals(type.pipeline().getLocation().toString()), "Native canonical pipeline must be retained");
	}

	private static void instanced(GpuDevice device, GpuTexture color, GpuTexture depth, Identifier texture, PDeformerStack stack)
	{
		RenderType type = SceneChecks.type(texture, false, true);
		PBakedMesh mesh = baked(SceneChecks.geometry(0), texture, false, true);
		var a = new PMeshDeformation(stack, ref -> 0.65f, new Object(), 1);
		var b = new PMeshDeformation(stack, ref -> 0.9f, new Object(), 1);
		var cpuA = PDeformedMeshBuffers.resolve(mesh, a);
		var cpuB = PDeformedMeshBuffers.resolve(mesh, b);
		SceneChecks.clear(device, color, depth);
		for (int i = 0; i < 513; i++)
			PRenderQueue.submit(PRenderQueue.RenderStage.ENTITIES, type, i % 2 == 0 ? cpuA : cpuB, SceneChecks.instance(i));
		PRenderQueue.flushOpaque(PRenderQueue.RenderStage.ENTITIES);
		ByteBuffer expected = SceneChecks.pixels(device, color);
		SceneChecks.clear(device, color, depth);
		int[] before = SceneChecks.counters();
		for (int i = 0; i < 513; i++)
		{
			var header = SceneChecks.instance(i);
			PRenderQueue.submit(PRenderQueue.RenderStage.ENTITIES, type, mesh, i % 2 == 0 ? a : b,
					new PRenderQueue.InstanceData(header.transform(), header.packedColor(), header.packedLight(), header.packedOverlay()), PTransparencyMode.ORDINARY);
		}
		PRenderQueue.flushOpaque(PRenderQueue.RenderStage.ENTITIES);
		checkDispatch(before, type, true);
		SceneChecks.check(expected.equals(SceneChecks.pixels(device, color)), "513 production queue instances with distinct deformer value offsets must match CPU");
		PGpuDeformerBuffers.finishFrame();
		System.out.println("PASS 513 public queue instances: alternating sampled values, subdivision, packed value offsets and chunk boundaries");
	}

	private static void immediate(GpuDevice device, GpuTexture color, GpuTexture depth, Identifier texture, Identifier hidden, PDeformerStack stack) throws Exception
	{
		var deformation = new PMeshDeformation(stack, PDeformerValueSource.DEFAULTS, new Object(), 1);
		for (boolean instant : new boolean[]{false, true})
			for (boolean emissive : new boolean[]{false, true})
				for (int mode = 0; mode < 3; mode++)
				{
					RenderType type = type(texture, instant, emissive, mode);
					PBakedMesh mesh = baked(SceneChecks.geometry(0), texture, mode == 1, emissive);
					PGeometryData cpu = PDeformedMeshBuffers.resolve(mesh, deformation).vertexBuffer().geometry();
					var reference = baked(cpu, texture, mode == 1, emissive);
					var header = new PInstanceHeader(new Matrix4f().translation(0.1f, 0, 0).scale(0.65f), mode == 2 ? 0x80FFFFFF : -1, 0xF000F0, OverlayTexture.NO_OVERLAY);
					var frame = new PRenderFrame(new Matrix4f().rotateY(0.12f), new PRhiRenderHost().captureFrame().projection());
					SceneChecks.clear(device, color, depth);
					PRenderQueue.drawImmediate(type, mesh, null, header, frame);
					ByteBuffer baseline = SceneChecks.pixels(device, color);
					SceneChecks.clear(device, color, depth);
					PRenderQueue.drawImmediate(type, reference, null, header, frame);
					ByteBuffer expected = SceneChecks.pixels(device, color);
					SceneChecks.clear(device, color, depth);
					int[] before = SceneChecks.counters();
					PRenderQueue.drawImmediate(type, mesh, deformation, header, frame);
					checkDispatch(before, type, true);
					ByteBuffer actual = SceneChecks.pixels(device, color);
					SceneChecks.check((SceneChecks.pixel(actual, 64, 64) >>> 24) == (mode == 2 ? 128 : 255), "Immediate native alpha must match its material policy");
					compare(expected, actual, baseline, "immediate-" + instant + "-" + emissive + "-" + mode);
					SceneChecks.clear(device, color, depth);
					PRenderQueue.drawImmediate(type(hidden, instant, emissive, mode), mesh, deformation, header, frame);
					SceneChecks.check((SceneChecks.pixel(SceneChecks.pixels(device, color), 64, 64) >>> 24) == (mode == 0 ? 255 : 0), "Immediate cutout/alpha discard must use the prepared diffuse texture");
					PGpuDeformerBuffers.finishFrame();
				}
		SceneChecks.clear(device, color, depth);
		PRenderQueue.drawImmediate(type(texture, true, true, 0), baked(SceneChecks.geometry(0), texture, false, true), null,
				new PInstanceHeader(new Matrix4f().translation(4, 0, 0), -1, 0xF000F0, OverlayTexture.NO_OVERLAY), new PRhiRenderHost().captureFrame());
		SceneChecks.check((SceneChecks.pixel(SceneChecks.pixels(device, color), 64, 64) >>> 24) == 0, "Immediate shader must apply its instance transform");

		// A translucent primitive in front must not occlude a subsequently rendered opaque one.
		PBakedMesh mesh = baked(SceneChecks.geometry(0), texture, false, true);
		SceneChecks.clear(device, color, depth);
		var frame = new PRhiRenderHost().captureFrame();
		PRenderQueue.drawImmediate(type(texture, true, true, 2), mesh, null, new PInstanceHeader(new Matrix4f().translation(0, 0, 0.25f), 0x80FF0000, 0xF000F0, OverlayTexture.NO_OVERLAY), frame);
		PRenderQueue.drawImmediate(type(texture, true, true, 0), mesh, null, new PInstanceHeader(new Matrix4f().translation(0, 0, -0.25f), 0xFF00FF00, 0xF000F0, OverlayTexture.NO_OVERLAY), frame);
		SceneChecks.check(SceneChecks.pixel(SceneChecks.pixels(device, color), 64, 64) == 0xFF00FF00, "Immediate alpha must not write depth under reversed-Z");
		System.out.println("PASS public drawImmediate: 12 canonical ordinary/instant lit/emissive solid/cutout/alpha pipelines, frame+instance transforms and no alpha depth write");
	}

	private static RenderType type(Identifier texture, boolean instant, boolean emissive, int mode)
	{
		if (instant)
			if (emissive)
				return mode == 0 ? PRenderTypes.RenderTypeProvider.trianglesInstantEmissiveSolid(texture) : mode == 1 ? PRenderTypes.RenderTypeProvider.trianglesInstantEmissiveCutout(texture) : PRenderTypes.RenderTypeProvider.trianglesInstantEmissiveTranslucent(texture);
			else
				return mode == 0 ? PRenderTypes.RenderTypeProvider.trianglesInstantSolid(texture) : mode == 1 ? PRenderTypes.RenderTypeProvider.trianglesInstantCutout(texture) : PRenderTypes.RenderTypeProvider.trianglesInstantTranslucent(texture);
		if (emissive)
			return mode == 0 ? PRenderTypes.RenderTypeProvider.trianglesEmissiveSolid(texture) : mode == 1 ? PRenderTypes.RenderTypeProvider.trianglesEmissiveCutout(texture) : PRenderTypes.RenderTypeProvider.trianglesEmissiveTranslucent(texture);
		return mode == 0 ? PRenderTypes.RenderTypeProvider.trianglesSolid(texture) : mode == 1 ? PRenderTypes.RenderTypeProvider.trianglesCutout(texture) : PRenderTypes.RenderTypeProvider.trianglesTranslucent(texture);
	}

	private static PBakedMesh baked(PGeometryData geometry, Identifier texture, boolean cutout, boolean emissive)
	{
		int count = geometry.vertices().remaining() / 24;
		FloatBuffer positions = FloatBuffer.allocate(count * 3), normals = FloatBuffer.allocate(count * 3), uvs = FloatBuffer.allocate(count * 2);
		ByteBuffer vertices = geometry.vertices().order(ByteOrder.nativeOrder());
		while (vertices.hasRemaining())
		{
			positions.put(vertices.getFloat()).put(vertices.getFloat()).put(vertices.getFloat());
			uvs.put(vertices.getFloat()).put(vertices.getFloat());
			normals.put(vertices.get() / 127f).put(vertices.get() / 127f).put(vertices.get() / 127f);
			vertices.get();
		}
		var source = new PMeshPrimitive(count, positions.flip(), normals.flip(), uvs.flip(), geometry.indexCount(), geometry.indices().order(ByteOrder.nativeOrder()),
				geometry.indexType() == PGeometryData.IndexType.UNSIGNED_SHORT ? 5123 : 5125, new PMaterial("queue fixture"));
		return new PBakedMesh(UUID.randomUUID(), geometry, "queue fixture", emissive, cutout ? PAlphaMode.CUTOUT : PAlphaMode.OPAQUE, source, texture);
	}

	private static void compare(ByteBuffer expected, ByteBuffer actual, ByteBuffer baseline, String name) throws Exception
	{
		int visible = 0, coverage = 0, changed = 0, max = 0;
		long error = 0;
		for (int p = 0; p < expected.limit(); p += 4)
		{
			int a = expected.get(p + 3) & 255, b = actual.get(p + 3) & 255;
			if (a != b)
				coverage++;
			if (a != (baseline.get(p + 3) & 255))
				changed++;
			if (a == 0 || b == 0)
				continue;
			visible++;
			for (int channel = 0; channel < 3; channel++)
			{
				int delta = Math.abs((expected.get(p + channel) & 255) - (actual.get(p + channel) & 255));
				max = Math.max(max, delta);
				error += delta;
			}
		}
		System.out.println(name + " CPU/GPU visible=" + visible + " coverage=" + coverage + " maxRGB=" + max + " totalRGB=" + error + " changed=" + changed);
		SceneChecks.check(visible > 100 && changed > 50 && coverage <= 8 && max <= 5 && error <= visible * 3L, name + " actual production deformation/normal output differs");
		SceneChecks.image(actual, name);
	}

	private static final class CpuOnly implements PMeshDeformer<Float>
	{
		@Override
		public Identifier id()
		{
			return Identifier.fromNamespaceAndPath("pulselib_client_scene", "cpu_only");
		}

		@Override
		public MapCodec<Float> codec()
		{
			return MapCodec.unit(0.25f);
		}

		@Override
		public void prepare(PDeformerPrepareContext context, Float definition)
		{
			context.add((position, values) -> position.add(definition, 0, 0));
		}
	}
}
