/**
 * @author ArcAnc
 * Created at: 13.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.rhi;

import com.arcanc.pulselib.content.model.PMaterial;
import com.arcanc.pulselib.content.model.PMeshPrimitive;
import com.arcanc.pulselib.content.model.baked.PBakedMesh;
import com.arcanc.pulselib.content.model.textures.PAlphaMode;
import com.arcanc.pulselib.content.renderer.plan.PDrawCommand;
import com.arcanc.pulselib.content.renderer.plan.PDrawGroup;
import com.arcanc.pulselib.content.renderer.plan.PFrameCompiler;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.content.renderer.plan.PPipelineHandle;
import com.arcanc.pulselib.content.renderer.plan.PRenderPlan;
import com.arcanc.pulselib.content.renderer.plan.PTransparencyMode;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import de.javagl.jgltf.model.GltfConstants;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.UUID;

/** Real production profiles/assets and canonical baked meshes; no fixture replacement shaders. */
final class RhiProfileChecks
{
	private static final int SIZE = 128;
	private static final int GRID = 34;

	static void run(GpuDevice device, int capacity, int uniformLimit)
	{
		var previousLights = RenderSystem.getShaderLights();
		var previousProjection = RenderSystem.getProjectionMatrixBuffer();
		var previousType = RenderSystem.getProjectionType();
		var previousModelView = RenderSystem.getModelViewMatrixCopy();
		var profiles = capacity == PRhiInstanceLayout.PORTABLE_CAPACITY ? PRhiPipelines.portable() : new PRhiPipelines(capacity);
		var ids = new HashSet<Identifier>();
		profiles.register(pipeline -> RhiLayoutChecks.assertTrue(ids.add(pipeline.getLocation()), "Unique production profile IDs"));
		RhiLayoutChecks.assertTrue(ids.size() == 4 && PRhiPipelines.POSITION_TEX_NORMAL.getVertexSize() == 24, "Four profiles, canonical stride 24");
		int sampled = GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST;
		try (var color = device.createTexture("Production profile color", GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_COPY_DST,
				GpuFormat.RGBA8_UNORM, SIZE, SIZE, 1, 1);
				var depth = device.createTexture("Production profile depth", GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_COPY_DST, GpuFormat.D32_FLOAT, SIZE, SIZE, 1, 1);
				var colorView = device.createTextureView(color);
				var depthView = device.createTextureView(depth);
				var diffuse = device.createTexture("Production diffuse UV fixture", sampled, GpuFormat.RGBA8_UNORM, 2, 2, 1, 1);
				var diffuseView = device.createTextureView(diffuse);
				var overlay = device.createTexture("Production overlay fixture", sampled, GpuFormat.RGBA8_UNORM, 16, 16, 1, 1);
				var overlayView = device.createTextureView(overlay);
				var lightmap = device.createTexture("Production lightmap fixture", sampled, GpuFormat.RGBA8_UNORM, 16, 16, 1, 1);
				var lightmapView = device.createTextureView(lightmap);
				var sampler = device.createSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.NEAREST, FilterMode.NEAREST, 1, OptionalDouble.empty());
				var nativeTransforms = device.createBuffer(() -> "Unused native transforms", GpuBuffer.USAGE_UNIFORM, ByteBuffer.allocateDirect(176));
				var lighting = lighting(device);
				var camera = new ProjectionMatrixBuffer("Production profile frame"))
		{
			RenderSystem.setShaderLights(lighting.slice());
			RenderSystem.getModelViewStack().translation(0.25f, 0, 0);
			RenderSystem.setProjectionMatrix(camera.getBuffer(new Matrix4f().translation(-0.25f, 0, 0)), ProjectionType.ORTHOGRAPHIC);
			var encoder = device.createCommandEncoder();
			encoder.writeToTexture(overlay, overlayPixels(), 0, 0, 0, 0, 16, 16);
			encoder.writeToTexture(lightmap, lightPixels(), 0, 0, 0, 0, 16, 16);
			var resources = new PRhiResourceRegistry(device);
			var executor = new PRhiDrawExecutor(device, resources, new PRhiInstanceLayout(device.getDeviceInfo().limits().minUniformOffsetAlignment(), capacity, uniformLimit));
			var target = new PRhiMaterial.Attachments(colorView, depthView);
			try
			{
				new PRhiMaterial(profiles.pipeline(false, true), () -> target, capacity == 128 ? 512 : 128);
				throw new AssertionError("Mismatched shader/CPU capacity accepted");
			}
			catch (IllegalArgumentException expected)
			{
				RhiLayoutChecks.assertTrue(expected.getMessage().contains("capacities differ"), "Capacity checked against shader define");
			}
			var frame = new PRhiRenderHost().captureFrame();
			try
			{
				for (boolean emissive : new boolean[]{false, true})
					for (boolean cutout : new boolean[]{false, true})
					{
						PBakedMesh baked = baked(cutout, emissive);
						var mesh = resources.mesh(baked);
						RhiLayoutChecks.assertTrue(mesh.equals(resources.geometry(baked.geometry())), "Baked and portable geometry share allocation");
						var pipeline = profiles.pipeline(cutout, emissive);
						var type = RenderType.create("production_profile", RenderSetup.builder(pipeline).createRenderSetup());
						var material = new PRhiNativeMaterial(type, capacity, PTransparencyMode.ORDINARY, Map.of(), () ->
								new PreparedRenderType(pipeline, OutputTarget.MAIN_TARGET, nativeTransforms.slice(), new ScissorState(), List.of(
										new PreparedRenderType.Texture("Sampler0", diffuseView, sampler),
										new PreparedRenderType.Texture("Sampler1", overlayView, sampler),
										new PreparedRenderType.Texture("Sampler2", lightmapView, sampler))), ignored -> target);
						PPipelineHandle handle = material.register(resources);
						var compiler = new PFrameCompiler<String>();
						for (int i = 0; i < 1025; i++)
							compiler.submit("production", handle, mesh, baked.geometry().indexCount(), instance(i), false);
						var plan = compiler.compileOpaque("production");
						encoder.writeToTexture(diffuse, diffusePixels(false), 0, 0, 0, 0, 2, 2);
						encoder.clearColorAndDepthTextures(color, new Vector4f(0, 0, 0, 0), depth, 0);
						executor.execute(plan, frame);
						checkPixels(pixels(device, color), cutout);
						encoder.writeToTexture(diffuse, diffusePixels(true), 0, 0, 0, 0, 2, 2);
						encoder.clearColorAndDepthTextures(color, new Vector4f(0, 0, 0, 0), depth, 0);
						executor.execute(plan, frame);
						checkPixels(pixels(device, color), false);
						var dynamic = new PRhiDynamicGeometry(baked.geometry());
						var dynamicHandle = resources.dynamic(dynamic);
						dynamic.update(geometry(-1, true));
						compiler.submit("updated", handle, dynamicHandle, 3, instance(0), false);
						encoder.clearColorAndDepthTextures(color, new Vector4f(0, 0, 0, 0), depth, 0);
						executor.execute(compiler.compileOpaque("updated"), frame);
						checkPixel(pixels(device, color), 0, emissive ? color(0) : shade(color(0)));
						dynamic.update(geometry(1, true, true));
						compiler.submit("backface", handle, dynamicHandle, 3, instance(0), false);
						encoder.clearColorAndDepthTextures(color, new Vector4f(0, 0, 0, 0), depth, 0);
						executor.execute(compiler.compileOpaque("backface"), frame);
						checkPixel(pixels(device, color), 0, emissive ? color(0) : shade(color(0)));
						dynamic.close();
						var deformed = new PInstanceHeader(new Matrix4f(), -1, 0, 0, 0, 0, 1);
						var invalid = new PRenderPlan(List.of(new PDrawGroup(handle, mesh, new PDrawCommand(3, 1, 0, 0, 0), true, List.of(deformed))));
						try
						{
							executor.execute(invalid, frame);
							throw new AssertionError("Geometry-only profile silently accepted GPU deformation");
						}
						catch (IllegalArgumentException expected)
						{
							RhiLayoutChecks.assertTrue(expected.getMessage().contains("deformer streams"), "Explicit unsupported-deformation rejection");
						}
						System.out.println("PASS production " + (emissive ? "emissive " : "lit ") + (cutout ? "cutout" : "solid") +
								": 1025 baked-mesh pixels, native captured frame, 24-byte vertices, UV/color/light/overlay, normal/backface update and forced alpha/cutout");
					}
			}
			finally
			{
				executor.cleanup();
			}
		}
		finally
		{
			RenderSystem.setShaderLights(previousLights);
			RenderSystem.setProjectionMatrix(previousProjection, previousType);
			RenderSystem.getModelViewStack().set(previousModelView);
		}
	}

	private static PBakedMesh baked(boolean cutout, boolean emissive)
	{
		var indices = ByteBuffer.allocate(6).order(ByteOrder.nativeOrder()).putShort((short)0).putShort((short)1).putShort((short)2).flip();
		var source = new PMeshPrimitive(3, FloatBuffer.wrap(new float[]{-1, -1, 0.5f, 1, -1, 0.5f, 0, 1, 0.5f}),
				FloatBuffer.wrap(new float[]{0, 0, 1, 0, 0, 1, 0, 0, 1}), FloatBuffer.wrap(new float[]{0.75f, 0.75f, 0.75f, 0.75f, 0.75f, 0.75f}),
				3, indices, GltfConstants.GL_UNSIGNED_SHORT, new PMaterial("profile"));
		return new PBakedMesh(UUID.randomUUID(), geometry(1, false), "profile", emissive,
				cutout ? PAlphaMode.CUTOUT : PAlphaMode.OPAQUE, source, Identifier.fromNamespaceAndPath("pulselib", "profile_fixture"));
	}

	private static PGeometryData geometry(int normal, boolean integers)
	{
		return geometry(normal, integers, false);
	}

	private static PGeometryData geometry(int normal, boolean integers, boolean reverse)
	{
		var vertices = ByteBuffer.allocate(72).order(ByteOrder.nativeOrder());
		for (float[] p : new float[][]{{-1, -1, 0.5f}, {1, -1, 0.5f}, {0, 1, 0.5f}})
			vertices.putFloat(p[0]).putFloat(p[1]).putFloat(p[2]).putFloat(0.75f).putFloat(0.75f).
					put((byte)0).put((byte)0).put((byte)(normal * 127)).put((byte)0);
		vertices.flip();
		var indices = ByteBuffer.allocate(integers ? 12 : 6).order(ByteOrder.nativeOrder());
		for (int i : reverse ? new int[]{0, 2, 1} : new int[]{0, 1, 2})
			if (integers)
				indices.putInt(i);
			else
				indices.putShort((short)i);
		indices.flip();
		return new PGeometryData(vertices, indices, 24, 3, integers ? PGeometryData.IndexType.UNSIGNED_INT : PGeometryData.IndexType.UNSIGNED_SHORT);
	}

	private static PInstanceHeader instance(int i)
	{
		float x = (i % GRID + 0.5f) / GRID * 2 - 1;
		float y = (i / GRID + 0.5f) / GRID * 2 - 1;
		return new PInstanceHeader(new Matrix4f().translation(x, y, 0).scale(1.2f / GRID, 1.2f / GRID, 1),
				color(i), 0xF000F0, i % 2 == 0 ? 0 : (4 << 16 | 3));
	}

	private static GpuBuffer lighting(GpuDevice device)
	{
		var bytes = ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder());
		for (int i = 0; i < 2; i++)
			bytes.putFloat(0).putFloat(0).putFloat(1).putFloat(0);
		bytes.flip();
		return device.createBuffer(() -> "Actual 26.2 Lighting layout", GpuBuffer.USAGE_UNIFORM, bytes);
	}

	private static ByteBuffer diffusePixels(boolean visible)
	{
		var bytes = ByteBuffer.allocateDirect(16);
		for (int i = 0; i < 4; i++)
			bytes.put((byte)255).put((byte)(i == 3 ? 255 : 0)).put((byte)(i == 3 ? 255 : 0)).put((byte)(i == 3 && !visible ? 0 : 255));
		return bytes.flip();
	}

	private static ByteBuffer overlayPixels()
	{
		var bytes = ByteBuffer.allocateDirect(16 * 16 * 4);
		for (int i = 0; i < 256; i++)
			bytes.put((byte)0).put((byte)(i == 4 * 16 + 3 ? 255 : 0)).put((byte)0).put((byte)(i == 4 * 16 + 3 ? 0 : 255));
		return bytes.flip();
	}

	private static ByteBuffer lightPixels()
	{
		var bytes = ByteBuffer.allocateDirect(16 * 16 * 4);
		for (int i = 0; i < 256; i++)
		{
			byte value = (byte)(i == 255 ? 255 : 0);
			bytes.put(value).put(value).put(value).put((byte)255);
		}
		return bytes.flip();
	}

	private static ByteBuffer pixels(GpuDevice device, GpuTexture color)
	{
		try (var buffer = device.createBuffer(() -> "Production profile pixels", GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_READ, SIZE * SIZE * 4))
		{
			var encoder = device.createCommandEncoder();
			encoder.copyTextureToBuffer(color, buffer, 0, () -> {}, 0);
			try (var fence = encoder.createFence())
			{
				encoder.submit();
				RhiLayoutChecks.assertTrue(fence.awaitCompletion(30_000_000_000L), "Production pixel readback fence");
			}
			try (var mapped = buffer.map(true, false))
			{
				return ByteBuffer.allocate(SIZE * SIZE * 4).put(mapped.data()).flip();
			}
		}
	}

	private static void checkPixels(ByteBuffer pixels, boolean discarded)
	{
		for (int i = 0; i < 1025; i++)
			checkPixel(pixels, i, discarded ? 0 : i % 2 != 0 ? 0xFF00FF00 : color(i));
	}

	private static void checkPixel(ByteBuffer pixels, int i, int expected)
	{
		int x = (int)((i % GRID + 0.5f) / GRID * SIZE);
		int y = (int)((i / GRID + 0.5f) / GRID * SIZE);
		int offset = (y * SIZE + x) * 4;
		int actual = (pixels.get(offset + 3) & 255) << 24 | (pixels.get(offset) & 255) << 16 |
				(pixels.get(offset + 1) & 255) << 8 | pixels.get(offset + 2) & 255;
		RhiLayoutChecks.assertTrue(actual == expected, "Production pixel " + i + ": " + Integer.toHexString(actual) + " expected " + Integer.toHexString(expected));
	}

	private static int color(int i)
	{
		return 0xFF00003F | ((i % 254 + 1) << 16) | ((i / 254 + 1) << 8);
	}

	private static int shade(int color)
	{
		return 0xFF000000 | Math.round((color >> 16 & 255) * 0.4f) << 16 |
				Math.round((color >> 8 & 255) * 0.4f) << 8 | Math.round((color & 255) * 0.4f);
	}
}
