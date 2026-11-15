/**
 * @author ArcAnc
 * Created at: 15.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.porting.scene;

import com.arcanc.pulselib.content.model.deformer.PChannelReference;
import com.arcanc.pulselib.content.model.deformer.PDeformerInstance;
import com.arcanc.pulselib.content.model.deformer.PDeformerStack;
import com.arcanc.pulselib.content.model.deformer.PMeshDeformation;
import com.arcanc.pulselib.content.model.deformer.PStretchDeformer;
import com.arcanc.pulselib.content.model.deformer.PStretchDefinition;
import com.arcanc.pulselib.content.renderer.PRenderContexts;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.plan.PTransparencyMode;
import com.arcanc.pulselib.content.renderer.gl.PGlDynamicGeometry;
import com.arcanc.pulselib.content.renderer.gl.PGlMultiDrawExecutor;
import com.arcanc.pulselib.content.renderer.gl.PGlResourceRegistry;
import com.arcanc.pulselib.content.model.deformer.gpu.PGpuDeformerBuffers;
import com.arcanc.pulselib.content.renderer.plan.PDrawCommand;
import com.arcanc.pulselib.content.renderer.plan.PDrawGroup;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.content.renderer.plan.PMeshHandle;
import com.arcanc.pulselib.content.renderer.plan.PPipelineHandle;
import com.arcanc.pulselib.content.renderer.plan.PRenderPlan;
import com.arcanc.pulselib.content.renderer.rhi.PRhiOitRenderer;
import com.arcanc.pulselib.content.renderer.rhi.PRhiWeightedBlendedOit;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.arcanc.pulselib.content.renderer.rhi.PRhiRenderHost;
import com.arcanc.pulselib.util.PRenderTypes;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Native OIT component acceptance inside Minecraft; world framegraph/actual weather remain separate. */
public final class OitChecks
{
	private OitChecks()
	{
	}

	public static int[] run(GpuDevice device, GpuTexture color, GpuTexture depth, Identifier texture) throws Exception
	{
		var geometry = SceneChecks.geometry(0);
		var type = PRenderTypes.RenderTypeProvider.trianglesEmissiveTranslucent(texture);
		var renderer = new PRhiOitRenderer(key -> type, key -> geometry,
				Map.of("DeformerOperations", () -> PGpuDeformerBuffers.upload().operations().slice(),
						"DeformerValues", () -> PGpuDeformerBuffers.upload().values().slice()));
		var frame = new PRhiRenderHost().captureFrame();
		List<PInstanceHeader> layers = List.of(header(0.2f, 0x80FFFF00), header(0.4f, 0x800000FF), header(0.6f, 0x8000FF00), header(0.8f, 0x80FF0000));
		try
		{
			int expected = 0;
			for (int seed = 0; seed < 8; seed++)
			{
				SceneChecks.clear(device, color, depth);
				var shuffled = new ArrayList<>(layers);
				Collections.shuffle(shuffled, new Random(seed));
				// Separate producers must share all depth layers, not peel independently.
				renderer.execute(plan(shuffled.subList(0, 2)), frame);
				renderer.execute(plan(shuffled.subList(2, 4)), frame);
				SceneChecks.check(SceneChecks.pixel(SceneChecks.pixels(device, color), 64, 64) == 0, "OIT waits for explicit composition");
				renderer.composite();
				int actual = SceneChecks.pixel(SceneChecks.pixels(device, color), 64, 64);
				if (seed == 0)
				{
					expected = actual;
					near(actual, over(layers), "Four ordered OIT layers");
					SceneChecks.image(SceneChecks.pixels(device, color), "oit-four-layers");
				}
				SceneChecks.check(actual == expected, "OIT must be order independent across producers/permutations");
			}
			System.out.printf("PASS RHI OIT four layers, 8 shuffled split producers, packed firstInstance=7: %08X%n", expected);

			// A fifth surface must not displace the nearest four. Hidden opaque geometry cannot steal a layer.
			SceneChecks.clear(device, color, depth);
			device.createCommandEncoder().clearDepthTexture(depth, 0.3);
			renderer.execute(plan(layers), frame);
			renderer.composite();
			near(SceneChecks.pixel(SceneChecks.pixels(device, color), 64, 64), over(layers.subList(1, 4)), "Opaque depth excludes hidden peeled layer");
			SceneChecks.clear(device, color, depth);
			device.createCommandEncoder().clearDepthTexture(depth, 0.9);
			renderer.execute(plan(layers), frame);
			renderer.composite();
			SceneChecks.check(SceneChecks.pixel(SceneChecks.pixels(device, color), 64, 64) == 0, "All OIT behind opaque depth must disappear");
			System.out.println("PASS reversed-Z opaque clipping, including hidden layers and empty accumulation");
			SceneChecks.clear(device, color, depth);
			var five = new ArrayList<>(layers);
			five.add(header(0.1f, 0x80FF00FF));
			renderer.execute(plan(five), frame);
			renderer.composite();
			near(SceneChecks.pixel(SceneChecks.pixels(device, color), 64, 64), expected, "Fifth surface cannot displace nearest four");

			var lit = PRenderTypes.RenderTypeProvider.trianglesTranslucent(texture);
			SceneChecks.clear(device, color, depth);
			for (var object : layers)
				PRenderQueue.drawImmediate(lit, SceneChecks.baked(geometry, texture, false, false), null, object, frame);
			int litExpected = SceneChecks.pixel(SceneChecks.pixels(device, color), 64, 64);
			SceneChecks.clear(device, color, depth);
			var litRenderer = new PRhiOitRenderer(key -> lit, key -> geometry,
					Map.of("DeformerOperations", () -> PGpuDeformerBuffers.upload().operations().slice(),
							"DeformerValues", () -> PGpuDeformerBuffers.upload().values().slice()));
			try
			{
				litRenderer.execute(plan(layers), frame);
				litRenderer.composite();
				near(SceneChecks.pixel(SceneChecks.pixels(device, color), 64, 64), litExpected, "Lit OIT compared with direct ordered reference");
			}
			finally
			{
				litRenderer.cleanup();
			}
			System.out.println("PASS nearest-four truncation and lit OIT versus ordered native reference");

			// Coplanar surfaces use weighted color/log transmission, not order-dependent alpha.
			SceneChecks.clear(device, color, depth);
			renderer.execute(plan(List.of(header(0.5f, 0x80FF0000), header(0.5f, 0x800000FF))), frame);
			renderer.composite();
			near(SceneChecks.pixel(SceneChecks.pixels(device, color), 64, 64), 0xC0600060, "Coplanar weighted accumulation and logarithmic transmission");

			// More than one UBO chunk, with independent transforms and a partial last chunk.
			SceneChecks.clear(device, color, depth);
			var many = new ArrayList<PInstanceHeader>();
			for (int i = 0; i < 129; i++)
				many.add(new PInstanceHeader(new Matrix4f().translation((i % 13 + 0.5f) / 13 * 2 - 1, (i / 13 + 0.5f) / 10 * 2 - 1, 0).scale(0.06f, 0.07f, 1),
						0x80FF0000 | (i << 8), 0xF000F0, OverlayTexture.NO_OVERLAY));
			renderer.execute(plan(many), frame);
			renderer.composite();
			var pixels = SceneChecks.pixels(device, color);
			for (int i = 0; i < 129; i++)
				near(SceneChecks.pixel(pixels, (int)((i % 13 + 0.5f) / 13 * 128), (int)((i / 13 + 0.5f) / 10 * 128)),
						0x80800000 | (Math.round(i * 128f / 255) << 8), "OIT packed chunk pixel " + i);
			SceneChecks.image(pixels, "oit-129-instances");
			System.out.println("PASS coplanar weighted/log accumulation and 129 distinct OIT instances across UBO chunks");

			List<RenderTarget> vanilla = new ArrayList<>();
			try
			{
				for (int i = 0; i < 5; i++)
					vanilla.add(new TextureTarget("OIT vanilla input " + i, 128, 128, true, GpuFormat.RGBA8_UNORM));
				for (int slot = 0; slot < 5; slot++)
					for (boolean front : new boolean[]{false, true})
					{
						for (var target : vanilla)
							device.createCommandEncoder().clearColorAndDepthTextures(target.getColorTexture(), new Vector4f(0), target.getDepthTexture(), 0);
						var target = vanilla.get(slot);
						var nativeColor = new Vector4f(0, 0.25f, 0, 0.25f);
						device.createCommandEncoder().clearColorAndDepthTextures(target.getColorTexture(), nativeColor, target.getDepthTexture(), front ? 0.9 : 0.1);
						device.createCommandEncoder().clearColorAndDepthTextures(color, nativeColor, depth, 0);
						renderer.execute(plan(List.of(header(0.5f, 0x80FF0000))), frame);
						try
						{
							renderer.compositeFabulous(RenderSystem.outputColorTextureOverride, vanilla.subList(0, 4));
							throw new AssertionError("Invalid Fabulous input count must be rejected before consuming layers");
						}
						catch (IllegalArgumentException expectedFailure)
						{
						}
						renderer.compositeFabulous(RenderSystem.outputColorTextureOverride, vanilla);
						near(SceneChecks.pixel(SceneChecks.pixels(device, color), 64, 64), front ? 0x40604000 : 0x40802000,
								"Fabulous slot=" + slot + " front=" + front);
					}
				SceneChecks.image(SceneChecks.pixels(device, color), "oit-fabulous-front");
				List<float[]> all = new ArrayList<>();
				List<float[]> nativeLayers = new ArrayList<>();
				for (int i = 0; i < 5; i++)
				{
					var input = new float[]{0.95f - i * 0.2f, (i * 11 + 7) / 255f, (i * 7 + 9) / 255f, (i * 5 + 3) / 255f, 64 / 255f};
					nativeLayers.add(input);
					all.add(input);
					device.createCommandEncoder().clearColorAndDepthTextures(vanilla.get(i).getColorTexture(), new Vector4f(input[1], input[2], input[3], input[4]), vanilla.get(i).getDepthTexture(), input[0]);
				}
				int vanillaImage = merged(nativeLayers);
				device.createCommandEncoder().clearColorAndDepthTextures(color,
						new Vector4f((vanillaImage >> 16 & 255) / 255f, (vanillaImage >> 8 & 255) / 255f, (vanillaImage & 255) / 255f, 1), depth, 0);
				for (var layer : layers)
				{
					int packed = layer.packedColor();
					float alpha = (packed >>> 24) / 255f;
					all.add(new float[]{layer.transform().m32() + 0.5f, (packed >> 16 & 255) / 255f * alpha,
							(packed >> 8 & 255) / 255f * alpha, (packed & 255) / 255f * alpha, alpha});
				}
				renderer.execute(plan(layers), frame);
				renderer.compositeFabulous(RenderSystem.outputColorTextureOverride, vanilla);
				near(SceneChecks.pixel(SceneChecks.pixels(device, color), 64, 64), merged(all), "Five vanilla surfaces interleaved with all four OIT layers");
				SceneChecks.image(SceneChecks.pixels(device, color), "oit-fabulous-interleaved");
				System.out.println("PASS all 5 Fabulous slots before/behind OIT and 9 interleaved surfaces against CPU reference; invalid input retry");
			}
			finally
			{
				vanilla.forEach(RenderTarget::destroyBuffers);
			}
			SceneChecks.clear(device, color, depth);
			renderer.execute(plan(layers), frame);
			renderer.discard();
			renderer.composite();
			SceneChecks.check(SceneChecks.pixel(SceneChecks.pixels(device, color), 64, 64) == 0, "Discarded OIT must not leak into next frame");
			renderer.cleanup();
			renderer.execute(plan(layers), frame);
			renderer.composite();
			near(SceneChecks.pixel(SceneChecks.pixels(device, color), 64, 64), expected, "Cleanup/reinitialization");
			SceneChecks.check(!color.isClosed() && !depth.isClosed(), "OIT never closes host attachments");
			System.out.println("PASS OIT discard, resource cleanup, recreation and borrowed attachment ownership");
			// Public queue integration: opaque snapshot must survive later writes to the source depth.
			var source = new TextureTarget("OIT queue opaque source", 128, 128, true, GpuFormat.RGBA8_UNORM);
			try
			{
				device.createCommandEncoder().clearColorAndDepthTextures(source.getColorTexture(), new Vector4f(0), source.getDepthTexture(), 0.3);
				PRenderQueue.captureWorldDepth(source);
				device.createCommandEncoder().clearDepthTexture(source.getDepthTexture(), 0.9);
				for (var object : layers)
					PRenderQueue.submit(PRenderQueue.RenderStage.ENTITIES, type, geometry, object, PTransparencyMode.OIT);
				PRenderQueue.flushWorld();
				PRenderQueue.compositeTranslucency(source);
				near(SceneChecks.pixel(SceneChecks.pixels(device, source.getColorTexture()), 64, 64), over(layers.subList(1, 4)), "Public queue uses frozen opaque depth and chosen destination");
				PRenderQueue.endWorldFrame();

				SceneChecks.clear(device, color, depth);
				device.createCommandEncoder().clearDepthTexture(source.getDepthTexture(), 0);
				var stack = PDeformerStack.compile(List.of(new PDeformerInstance<>(PStretchDeformer.INSTANCE,
						new PStretchDefinition(new Vector3f(), new Vector3f(1, 0, 0), PChannelReference.constant(0.5f)))));
				var deformation = new PMeshDeformation(stack, reference -> 0.5f, new Object(), 0);
				var baked = SceneChecks.baked(geometry, texture, false, true);
				PRenderQueue.submit(PRenderQueue.RenderStage.ENTITIES, type, baked, deformation,
						new PRenderQueue.InstanceData(new Matrix4f(), 0x80FF0000, 0xF000F0, OverlayTexture.NO_OVERLAY), PTransparencyMode.OIT);
				PRenderQueue.flush(PRenderQueue.RenderStage.ENTITIES, source);
				try (var gui = PRenderContexts.enter(PRenderContexts.Context.GUI))
				{
					// Empty GUI close still attempts finishLocalFrame; pending world values must remain live.
				}
				for (int i = 0; i < 150; i++)
					PGpuDeformerBuffers.submit(new PMeshDeformation(stack, reference -> 1.2f, new Object(), 0));
				PGpuDeformerBuffers.upload();
				PRenderQueue.compositeTranslucency();
				var deformed = SceneChecks.pixels(device, color);
				near(SceneChecks.pixel(deformed, 64, 64), 0x80800000, "Pending OIT after GUI and buffer growth");
				SceneChecks.check(SceneChecks.pixel(deformed, 40, 64) == 0, "Pending sampled stretch survives GUI scope and deformer buffer replacement");
				PRenderQueue.endWorldFrame();
				System.out.println("PASS public queue opaque snapshot/destination; pending GPU deformation survives GUI close and upload-buffer growth");
			}
			finally
			{
				PRenderQueue.cleanup();
				source.destroyBuffers();
			}

			// Snapshot different revisions of the same dynamic source, including close before composition.
			SceneChecks.clear(device, color, depth);
			try (var dynamic = new PGlDynamicGeometry(geometry))
			{
				var dynamicRenderer = new PRhiOitRenderer(key -> type, key -> dynamic,
						Map.of("DeformerOperations", () -> PGpuDeformerBuffers.upload().operations().slice(),
								"DeformerValues", () -> PGpuDeformerBuffers.upload().values().slice()));
				try
				{
					dynamicRenderer.execute(plan(List.of(header(0.5f, 0x80FF0000))), frame);
					dynamic.update(SceneChecks.geometry(4));
					dynamicRenderer.execute(plan(List.of(header(0.6f, 0x8000FF00))), frame);
					dynamic.close();
					dynamicRenderer.composite();
					near(SceneChecks.pixel(SceneChecks.pixels(device, color), 64, 64), 0x80800000, "Dynamic source snapshots survive update/close");
				}
				finally
				{
					dynamicRenderer.cleanup();
				}
			}
			System.out.println("PASS dynamic OIT revisions, close-before-composite and fenced GPU geometry retirement");
		}
		finally
		{
			renderer.cleanup();
		}
		var owner = new PRhiWeightedBlendedOit();
		List<AutoCloseable> previous = new ArrayList<>();
		try
		{
			for (int iteration = 0; iteration < 10; iteration++)
			{
				var host = new TextureTarget("OIT lifecycle host", iteration % 2 == 0 ? 64 : 128, 128, true, GpuFormat.RGBA8_UNORM);
				try
				{
					SceneChecks.check(owner.begin(host.getColorTextureView(), host.getDepthTextureView()), "Resize host accepted");
					for (var resource : previous)
						closed(resource);
					previous = owned(owner);
					SceneChecks.check(previous.size() == 28, "Four layers own 12 textures/views plus metadata and scratch color: " + previous.size());
					SceneChecks.check(owner.layerDepthView(3).getWidth(0) == host.width, "Fourth layer resized");
					owner.discard();
					SceneChecks.check(!host.getColorTexture().isClosed() && !host.getDepthTexture().isClosed(), "Discard preserves host targets");
				}
				finally
				{
					host.destroyBuffers();
				}
			}
		}
		finally
		{
			owner.close();
		}
		for (var resource : previous)
			closed(resource);
		System.out.println("PASS 10 OIT resizes: all 28 owned textures/views closed on replacement/final close; borrowed host targets retained");
		var compatibilityResources = new PGlResourceRegistry();
		var compatibility = new PGlMultiDrawExecutor(compatibilityResources);
		var target = new TextureTarget("OIT compatibility destination", 128, 128, true, GpuFormat.RGBA8_UNORM);
		var previousColor = RenderSystem.outputColorTextureOverride;
		try
		{
			device.createCommandEncoder().clearColorAndDepthTextures(target.getColorTexture(), new Vector4f(0), target.getDepthTexture(), 0);
			var pipeline = compatibilityResources.pipeline(type, PTransparencyMode.OIT);
			var mesh = compatibilityResources.geometry(geometry);
			var compiled = new PRenderPlan(List.of(new PDrawGroup(pipeline, mesh, new PDrawCommand(3, 4, 0, 0, 0), false, layers)));
			RenderSystem.outputColorTextureOverride = null;
			compatibility.execute(compiled, frame, target);
			SceneChecks.check(RenderSystem.outputColorTextureOverride == null, "Compatibility submission restores caller override");
			compatibility.compositeOit();
			near(SceneChecks.pixel(SceneChecks.pixels(device, target.getColorTexture()), 64, 64), over(layers), "Legacy executor API uses native OIT and original destination");
		}
		finally
		{
			RenderSystem.outputColorTextureOverride = previousColor;
			compatibility.cleanup();
			target.destroyBuffers();
		}
		System.out.println("PASS legacy executor OIT API forwards to native backend without a GL context dependency, preserving original destination");
		return new int[]{4, 1};
	}

	/** Test-only ownership inspection, without adding resource handles to the production API. */
	private static List<AutoCloseable> owned(PRhiWeightedBlendedOit owner) throws Exception
	{
		List<AutoCloseable> result = new ArrayList<>();
		for (var field : PRhiWeightedBlendedOit.class.getDeclaredFields())
		{
			if (field.getName().startsWith("destination"))
				continue;
			field.setAccessible(true);
			Object value = field.get(owner);
			if (value instanceof GpuTexture texture)
				result.add(texture);
			else if (value instanceof GpuTextureView view)
				result.add(view);
			else if (value instanceof Object[] array)
				for (Object element : array)
					if (element instanceof GpuTexture texture)
						result.add(texture);
					else if (element instanceof GpuTextureView view)
						result.add(view);
		}
		return result;
	}

	private static void closed(AutoCloseable resource)
	{
		SceneChecks.check(resource instanceof GpuTexture texture ? texture.isClosed() : ((GpuTextureView)resource).isClosed(), "Owned OIT resource was not closed");
	}

	private static int merged(List<float[]> surfaces)
	{
		var ordered = new ArrayList<>(surfaces);
		ordered.sort(java.util.Comparator.comparingDouble(item -> item[0]));
		float r = 0, g = 0, b = 0;
		for (var item : ordered)
		{
			r = item[1] + r * (1 - item[4]);
			g = item[2] + g * (1 - item[4]);
			b = item[3] + b * (1 - item[4]);
		}
		return 0xFF000000 | (Math.round(r * 255) << 16) | (Math.round(g * 255) << 8) | Math.round(b * 255);
	}

	private static PInstanceHeader header(float depth, int color)
	{
		return new PInstanceHeader(new Matrix4f().translation(0, 0, depth - 0.5f), color, 0xF000F0, OverlayTexture.NO_OVERLAY);
	}

	private static PRenderPlan plan(List<PInstanceHeader> objects)
	{
		var packed = new ArrayList<PInstanceHeader>();
		for (int i = 0; i < 7; i++)
			packed.add(header(1, 0));
		packed.addAll(objects);
		return new PRenderPlan(List.of(new PDrawGroup(new PPipelineHandle(0), new PMeshHandle(0),
				new PDrawCommand(3, objects.size(), 0, 0, 7), false, objects)), packed);
	}

	private static int over(List<PInstanceHeader> layers)
	{
		float[] result = {0, 0, 0, 0};
		for (var layer : layers)
		{
			int color = layer.packedColor();
			float alpha = (color >>> 24) / 255f;
			for (int channel = 0; channel < 3; channel++)
				result[channel] = ((color >> (16 - channel * 8)) & 255) * alpha + result[channel] * (1 - alpha);
			result[3] = alpha * 255 + result[3] * (1 - alpha);
		}
		return (Math.round(result[3]) << 24) | (Math.round(result[0]) << 16) | (Math.round(result[1]) << 8) | Math.round(result[2]);
	}

	private static void near(int actual, int expected, String name)
	{
		for (int shift : new int[]{0, 8, 16, 24})
			SceneChecks.check(Math.abs((actual >>> shift & 255) - (expected >>> shift & 255)) <= 2,
					name + " actual=" + Integer.toHexString(actual) + " expected=" + Integer.toHexString(expected));
	}
}
