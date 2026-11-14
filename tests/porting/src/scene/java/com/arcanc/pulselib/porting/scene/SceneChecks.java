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
import com.arcanc.pulselib.content.model.textures.PAlphaMode;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.gl.PGlDynamicGeometry;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.content.renderer.plan.PTransparencyMode;
import com.arcanc.pulselib.content.renderer.plan.PRenderPlan;
import com.arcanc.pulselib.content.renderer.plan.PPipelineHandle;
import com.arcanc.pulselib.content.renderer.rhi.PRhiResourceRegistry;
import com.arcanc.pulselib.util.PModelCache;
import com.arcanc.pulselib.util.PRenderTypes;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.function.Function;

/** Full Minecraft/production queue/native preparation; no supplied PreparedRenderType or GPU callback. */
public final class SceneChecks
{
	static final int SIZE = 128;
	private static final int GRID = 34;
	private static boolean done;
	private static int rhiDraws;
	private static int legacyDraws;
	private static int deformedDraws;
	private static int instantDraws;
	private static int instantExecutions;
	private static String lastPipeline = "";
	private static int additionalRhiDraws;
	private static int additionalLegacyDraws;

	private SceneChecks()
	{
	}

	/** Observed at real backend method entry by test-only, non-cancelling Mixins. */
	public static void recordBackend(boolean rhi)
	{
		if (rhi)
			rhiDraws++;
		else
			legacyDraws++;
	}

	public static void recordPlan(PRenderPlan plan,
			PRhiResourceRegistry resources)
	{
		if (plan.instances().stream().anyMatch(instance -> instance.auxiliaryOperationCount() > 0))
			deformedDraws++;
		if (!plan.groups().isEmpty())
			lastPipeline = resources.pipelineType(plan.groups().getLast().pipeline()).pipeline().getLocation().toString();
	}

	public static void recordInstantPlan(PRenderPlan plan, Function<PPipelineHandle, RenderType> types)
	{
		if (plan.isEmpty())
			return;
		recordBackend(true);
		instantExecutions++;
		if (plan.instances().stream().anyMatch(instance -> instance.auxiliaryOperationCount() > 0))
			deformedDraws++;
		lastPipeline = types.apply(plan.groups().getLast().pipeline()).pipeline().getLocation().toString();
	}

	public static void recordInstantDraw()
	{
		instantDraws++;
	}

	public static int[] instantCounters()
	{
		return new int[]{instantExecutions, instantDraws};
	}

	static int[] counters()
	{
		return new int[]{rhiDraws, legacyDraws, deformedDraws};
	}

	static String lastPipeline()
	{
		return lastPipeline;
	}

	public static void tick()
	{
		Minecraft client = Minecraft.getInstance();
		if (done || !client.isGameLoadFinished() || PModelCache.getModels() == null)
			return;
		done = true;
		try
		{
			run(client);
			check(rhiDraws == 20 + additionalRhiDraws && legacyDraws == additionalLegacyDraws, "Unexpected RHI/legacy dispatch: " + rhiDraws + "/" + legacyDraws);
			System.out.println("PASS production dispatch RHI=" + rhiDraws + ", legacy=" + legacyDraws);
			Files.writeString(Path.of("pulselib-scene-result.txt"), "PASS\n");
			System.out.println("PULSELIB_PRODUCTION_SCENE_PASSED actual Minecraft, native textures/prepare, queue/host/compiler/RHI/readback; offscreen, no world acceptance");
		}
		catch (Throwable failure)
		{
			failure.printStackTrace();
			try
			{
				Files.writeString(Path.of("pulselib-scene-result.txt"), "FAIL: " + failure + "\n");
			}
			catch (java.io.IOException recordingFailure)
			{
				failure.addSuppressed(recordingFailure);
			}
		}
		finally
		{
			client.stop();
		}
	}

	private static void run(Minecraft client) throws Exception
	{
		GpuDevice device = RenderSystem.getDevice();
		System.out.println("Production scene device: " + device.getDeviceInfo());
		String backend = System.getProperty("pulselib.scene.backend", "opengl");
		check(device.getDeviceInfo().backendName().equalsIgnoreCase(backend), "Scene requires actual " + backend + ", not a backend fallback");
		var previousProjection = RenderSystem.getProjectionMatrixBuffer();
		var previousType = RenderSystem.getProjectionType();
		var previousModelView = RenderSystem.getModelViewMatrixCopy();
		@Nullable GpuTextureView previousColor = RenderSystem.outputColorTextureOverride;
		@Nullable GpuTextureView previousDepth = RenderSystem.outputDepthTextureOverride;
		var scissor = RenderSystem.getScissorStateForRenderTypeDraws();
		boolean clipped = scissor.enabled();
		int left = scissor.x(), top = scissor.y(), width = scissor.width(), height = scissor.height();
		Identifier visible = texture(client, "visible", -1);
		Identifier hidden = texture(client, "hidden", 0x00FFFFFF);
		try (var color = device.createTexture("Pulse production scene", GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_COPY_DST,
				GpuFormat.RGBA8_UNORM, SIZE, SIZE, 1, 1);
				var depth = device.createTexture("Pulse production scene depth", GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_COPY_DST,
						GpuFormat.D32_FLOAT, SIZE, SIZE, 1, 1);
				var colorView = device.createTextureView(color);
				var depthView = device.createTextureView(depth);
				var projection = new ProjectionMatrixBuffer("Pulse production scene"))
		{
			try
			{
				RenderSystem.getModelViewStack().identity();
				RenderSystem.setProjectionMatrix(projection.getBuffer(new Matrix4f()), ProjectionType.ORTHOGRAPHIC);
				RenderSystem.disableScissorForRenderTypeDraws();
				RenderSystem.outputColorTextureOverride = colorView;
				RenderSystem.outputDepthTextureOverride = depthView;
				for (boolean emissive : new boolean[]{false, true})
					for (boolean cutout : new boolean[]{false, true})
					{
						String name = (emissive ? "emissive-" : "lit-") + (cutout ? "cutout" : "solid");
						RenderType type = type(visible, cutout, emissive);
						PGeometryData geometry = geometry(0);
						PBakedMesh baked = baked(geometry, visible, cutout, emissive);
						clear(device, color, depth);
						for (int i = 0; i < 1025; i++)
							if (i % 2 == 0)
								PRenderQueue.submit(PRenderQueue.RenderStage.ENTITIES, type, geometry, instance(i));
							else
							{
								var header = instance(i);
								var data = new PRenderQueue.InstanceData(header.transform(), header.packedColor(), header.packedLight(), header.packedOverlay());
								PRenderQueue.submit(PRenderQueue.RenderStage.ENTITIES, type, baked, null, data, PTransparencyMode.ORDINARY);
							}
						PRenderQueue.flushOpaque(PRenderQueue.RenderStage.ENTITIES);
						ByteBuffer pixels = pixels(device, color);
						for (int i = 0; i < 1025; i++)
						{
							int x = (int)((i % GRID + 0.5f) / GRID * SIZE), y = (int)((i / GRID + 0.5f) / GRID * SIZE);
							int actual = pixel(pixels, x, y);
							check((actual >>> 24) == 255 && (actual & 0xFFFFFF) != 0, name + " visible instance " + i + ": " + Integer.toHexString(actual));
							if (emissive)
								check(actual == color(i), name + " exact native texture/overlay color " + i);
						}
						image(pixels, name);
						clear(device, color, depth);
						PRenderQueue.submit(PRenderQueue.RenderStage.ENTITIES, type(hidden, cutout, emissive), geometry,
								new PInstanceHeader(new Matrix4f(), -1, 0xF000F0, OverlayTexture.NO_OVERLAY));
						PRenderQueue.flushOpaque(PRenderQueue.RenderStage.ENTITIES);
						check((pixel(pixels(device, color), 64, 64) >>> 24) == (cutout ? 0 : 255), name + " native alpha policy");
						try (var dynamic = new PGlDynamicGeometry(geometry))
						{
							for (int offset : new int[]{0, 4, 0})
							{
								dynamic.update(geometry(offset));
								clear(device, color, depth);
								PRenderQueue.submit(PRenderQueue.RenderStage.ENTITIES, type, dynamic,
										new PInstanceHeader(new Matrix4f(), -1, 0xF000F0, OverlayTexture.NO_OVERLAY));
								PRenderQueue.flushOpaque(PRenderQueue.RenderStage.ENTITIES);
								check((pixel(pixels(device, color), 64, 64) >>> 24) == (offset == 0 ? 255 : 0), name + " dynamic update " + offset);
							}
						}
						PRenderQueue.cleanup();
						check(!color.isClosed() && !depth.isClosed(), "Queue cleanup must not close borrowed native targets");
						System.out.println("PASS production queue " + name + ": 1025 portable/baked-mesh instance pixels, native alpha, dynamic move/update/close and cleanup");
					}
				if (Boolean.getBoolean("pulselib.scene.shaders"))
				{
					additionalRhiDraws = ShaderChecks.run(device, color, depth, visible);
					if (backend.equals("opengl"))
						additionalLegacyDraws = ShaderChecks.legacy(device, color, depth, visible);
				}
				if (Boolean.getBoolean("pulselib.scene.queue"))
					additionalRhiDraws += QueueChecks.run(device, color, depth, visible, hidden);

				if (Boolean.getBoolean("pulselib.scene.gui"))
					additionalRhiDraws += GuiQueueChecks.run(device, color, depth, visible);
			}
			finally
			{
				PRenderQueue.cleanup();
				RenderSystem.outputColorTextureOverride = previousColor;
				RenderSystem.outputDepthTextureOverride = previousDepth;
				RenderSystem.setProjectionMatrix(previousProjection, previousType);
				RenderSystem.getModelViewStack().set(previousModelView);
				if (clipped)
					RenderSystem.enableScissorForRenderTypeDraws(left, top, width, height);
				else
					RenderSystem.disableScissorForRenderTypeDraws();
			}
		}
		finally
		{
			client.getTextureManager().release(visible);
			client.getTextureManager().release(hidden);
		}
	}

	static RenderType type(Identifier texture, boolean cutout, boolean emissive)
	{
		return emissive ? (cutout ? PRenderTypes.RenderTypeProvider.trianglesEmissiveCutout(texture) : PRenderTypes.RenderTypeProvider.trianglesEmissiveSolid(texture)) :
				(cutout ? PRenderTypes.RenderTypeProvider.trianglesCutout(texture) : PRenderTypes.RenderTypeProvider.trianglesSolid(texture));
	}

	private static Identifier texture(Minecraft client, String name, int color)
	{
		var id = Identifier.fromNamespaceAndPath("pulselib_client_scene", name);
		var image = new NativeImage(1, 1, false);
		image.setPixel(0, 0, color);
		client.getTextureManager().register(id, new DynamicTexture(() -> "Pulse scene " + name, image));
		return id;
	}

	static PGeometryData geometry(float offset)
	{
		var vertices = ByteBuffer.allocate(72).order(ByteOrder.nativeOrder());
		for (float[] p : new float[][]{{-1, -1, 0.5f}, {1, -1, 0.5f}, {0, 1, 0.5f}})
			vertices.putFloat(p[0] + offset).putFloat(p[1]).putFloat(p[2]).putFloat(0.5f).putFloat(0.5f).
					put((byte)0).put((byte)0).put((byte)127).put((byte)0);
		var indices = ByteBuffer.allocate(6).order(ByteOrder.nativeOrder()).putShort((short)0).putShort((short)1).putShort((short)2);
		return new PGeometryData(vertices.flip(), indices.flip(), 24, 3, PGeometryData.IndexType.UNSIGNED_SHORT);
	}

	static PBakedMesh baked(PGeometryData geometry, Identifier texture, boolean cutout, boolean emissive)
	{
		var source = new PMeshPrimitive(3, FloatBuffer.wrap(new float[]{-1, -1, 0.5f, 1, -1, 0.5f, 0, 1, 0.5f}),
				FloatBuffer.wrap(new float[]{0, 0, 1, 0, 0, 1, 0, 0, 1}), FloatBuffer.wrap(new float[]{0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f}),
				3, geometry.indices(), 5123, new PMaterial("scene"));
		return new PBakedMesh(UUID.randomUUID(), geometry, "scene", emissive, cutout ? PAlphaMode.CUTOUT : PAlphaMode.OPAQUE, source, texture);
	}

	static PInstanceHeader instance(int i)
	{
		return new PInstanceHeader(new Matrix4f().translation((i % GRID + 0.5f) / GRID * 2 - 1, (i / GRID + 0.5f) / GRID * 2 - 1, 0).
				scale(1.2f / GRID, 1.2f / GRID, 1), color(i), 0xF000F0, OverlayTexture.NO_OVERLAY);
	}

	private static int color(int i)
	{
		return 0xFF00003F | ((i % 254 + 1) << 16) | ((i / 254 + 1) << 8);
	}

	static void clear(GpuDevice device, GpuTexture color, GpuTexture depth)
	{
		device.createCommandEncoder().clearColorAndDepthTextures(color, new Vector4f(0, 0, 0, 0), depth, 0);
	}

	static ByteBuffer pixels(GpuDevice device, GpuTexture texture)
	{
		try (var buffer = device.createBuffer(() -> "Pulse scene readback", GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_READ, SIZE * SIZE * 4))
		{
			var encoder = device.createCommandEncoder();
			encoder.copyTextureToBuffer(texture, buffer, 0, () -> {}, 0);
			try (var fence = encoder.createFence())
			{
				encoder.submit();
				check(fence.awaitCompletion(30_000_000_000L), "GPU readback timeout");
			}
			try (var mapped = buffer.map(true, false))
			{
				return ByteBuffer.allocate(SIZE * SIZE * 4).put(mapped.data()).flip();
			}
		}
	}

	static int pixel(ByteBuffer pixels, int x, int y)
	{
		int p = (y * SIZE + x) * 4;
		return (pixels.get(p + 3) & 255) << 24 | (pixels.get(p) & 255) << 16 | (pixels.get(p + 1) & 255) << 8 | pixels.get(p + 2) & 255;
	}

	static void image(ByteBuffer pixels, String name) throws Exception
	{
		try (var image = new NativeImage(SIZE, SIZE, false))
		{
			for (int y = 0; y < SIZE; y++)
				for (int x = 0; x < SIZE; x++)
					image.setPixel(x, y, pixel(pixels, x, y));
			image.writeToFile(Path.of("scene-" + name + ".png"));
		}
	}

	static void check(boolean condition, String message)
	{
		if (!condition)
			throw new AssertionError(message);
	}
}
