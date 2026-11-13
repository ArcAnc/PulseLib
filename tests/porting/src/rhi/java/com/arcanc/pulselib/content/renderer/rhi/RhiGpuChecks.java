/**
 * @author ArcAnc
 * Created at: 13.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.rhi;

import com.arcanc.pulselib.content.renderer.gl.PGlDynamicGeometry;
import com.arcanc.pulselib.content.renderer.legacy.GlDynamicGeometry;
import com.arcanc.pulselib.content.renderer.plan.PDrawCommand;
import com.arcanc.pulselib.content.renderer.plan.PDrawGroup;
import com.arcanc.pulselib.content.renderer.plan.PFrameCompiler;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.arcanc.pulselib.content.renderer.plan.PGeometrySource;
import com.arcanc.pulselib.content.renderer.plan.PMeshHandle;
import com.arcanc.pulselib.content.renderer.plan.PPipelineHandle;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.content.renderer.plan.PRenderCapabilities;
import com.arcanc.pulselib.content.renderer.plan.PRenderFrame;
import com.arcanc.pulselib.content.renderer.plan.PRenderPlan;
import com.arcanc.pulselib.content.renderer.plan.PTransparencyMode;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.opengl.GlBackend;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.NativeLibrariesBootstrap;
import com.mojang.blaze3d.shaders.GpuDebugOptions;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.GpuBackend;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vulkan.VulkanBackend;
import net.minecraft.SharedConstants;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL31;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

/** LWJGL is confined to fixture window/bootstrap and GL limit query; production executor uses only RHI. */
public final class RhiGpuChecks
{
	private static final int SIZE = 128;
	private static final int GRID = 34;

	public static void main(String[] args) throws Exception
	{
		SharedConstants.tryDetectVersion();
		RhiRegistrationChecks.run();
		NativeLibrariesBootstrap.loadLibraries();
		RenderSystem.initRenderThread();
		var errors = GLFWErrorCallback.createPrint(System.err).set();
		long window = 0;
		GpuDevice device = null;
		try
		{
			RhiLayoutChecks.assertTrue(GLFW.glfwInit(), "GLFW initialized");
			GpuBackend backend = args[0].equals("Vulkan") ? new VulkanBackend() : new GlBackend();
			GLFW.glfwDefaultWindowHints();
			backend.setWindowHints();
			GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
			window = GLFW.glfwCreateWindow(SIZE, SIZE, "PulseLib RHI offscreen fixture", 0, 0);
			RhiLayoutChecks.assertTrue(window != 0, "Hidden GPU window");
			int capacity = Integer.parseInt(args[1]);
			ShaderSource shaders = (id, type) ->
			{
				if (id.getNamespace().equals("pulselib"))
					return RhiProductionShaderSource.load(id, type);
				if (!id.equals(Identifier.fromNamespaceAndPath("porting_fixture", "test")))
					throw new IllegalArgumentException("Unexpected fixture shader " + id);
				return type == ShaderType.VERTEX ? vertexShader(capacity) : fragmentShader();
			};
			device = backend.createDevice(window, shaders, new GpuDebugOptions(1, true, true, false), () -> {});
			RenderSystem.initRenderer(device);
			int uniformLimit = args[0].equals("Vulkan") ? Integer.parseInt(args[2]) : GL31.glGetInteger(GL31.GL_MAX_UNIFORM_BLOCK_SIZE);
			System.out.println("GPU " + device.getDeviceInfo().name() + " / " + device.getDeviceInfo().backendName() +
					" alignment=" + device.getDeviceInfo().limits().minUniformOffsetAlignment() + " blockLimit=" + uniformLimit);
			render(device, capacity, uniformLimit);
			RhiProfileChecks.run(device, capacity, uniformLimit);
			System.out.println("PULSELIB_RHI_GPU_PASSED " + args[0] + " (offscreen fixture, not full Minecraft client)");
		}
		finally
		{
			if (device != null)
				device.close();
			if (window != 0)
				GLFW.glfwDestroyWindow(window);
			GLFW.glfwTerminate();
			errors.close();
		}
	}

	private static void render(GpuDevice device, int capacity, int uniformLimit)
	{
		try (var color = device.createTexture("RHI fixture color", GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_COPY_DST,
				GpuFormat.RGBA8_UNORM, SIZE, SIZE, 1, 1);
				var depth = device.createTexture("RHI fixture depth", GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_COPY_DST, GpuFormat.D32_FLOAT, SIZE, SIZE, 1, 1);
				var colorView = device.createTextureView(color);
				var depthView = device.createTextureView(depth);
				var mask = device.createTexture("RHI fixture mask", GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST,
						GpuFormat.RGBA8_UNORM, 1, 1, 1, 1);
				var maskView = device.createTextureView(mask);
				var sampler = device.createSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.NEAREST,
						FilterMode.NEAREST, 1, OptionalDouble.empty()))
		{
			var registry = new PRhiResourceRegistry(device);
			var executor = new PRhiDrawExecutor(device, registry,
					new PRhiInstanceLayout(device.getDeviceInfo().limits().minUniformOffsetAlignment(), capacity, uniformLimit));
			RhiLayoutChecks.assertTrue(executor.capabilities().submissionMode() == PRenderCapabilities.SubmissionMode.DIRECT &&
					!executor.capabilities().matrix().persistentMapping() && !executor.capabilities().matrix().multiDrawIndirect(), "Honest capabilities");
			var pipeline = pipeline(false);
			var target = new PRhiMaterial.Attachments(colorView, depthView);
			var type = RenderType.create("rhi_fixture_solid", RenderSetup.builder(pipeline).createRenderSetup());
			var material = new PRhiMaterial(pipeline, () -> target, capacity);
			var solid = registry.pipeline(type, material);
			RhiLayoutChecks.assertTrue(solid.equals(registry.pipeline(type, material)) && registry.pipelineType(solid) == type,
					"Native render-type identity binding/cache");
			var otherType = RenderType.create("rhi_fixture_other", RenderSetup.builder(pipeline).createRenderSetup());
			var other = registry.pipeline(otherType, material);
			RhiLayoutChecks.assertTrue(!solid.equals(other) && registry.pipelineType(other) == otherType, "Separate native type identities");
			expect(IllegalArgumentException.class, () -> registry.pipeline(type, new PRhiMaterial(pipeline(false), () -> target, capacity)));
			expect(IllegalStateException.class, () -> registry.pipeline(type, new PRhiMaterial(pipeline, () -> target, capacity)));
			expect(IllegalArgumentException.class, () -> new PRhiMaterial(pipeline(true), () -> target, capacity));
			var cutout = registry.pipeline(new PRhiMaterial(pipeline(true), () -> target, capacity, Map.of(),
					Map.of("Sampler0", () -> new PRhiMaterial.Texture(maskView, sampler)), com.arcanc.pulselib.content.renderer.plan.PTransparencyMode.ORDINARY));
			PGeometryData data = geometry(1f);
			var mesh = registry.geometry(data);
			var cached = registry.resolve(mesh, data);
			RhiLayoutChecks.assertTrue(registry.resolve(mesh, data) == cached, "Static geometry cached");
			List<PInstanceHeader> packed = new ArrayList<>();
			for (int i = 0; i < 9; i++)
				packed.add(new PInstanceHeader(new Matrix4f(), 0, 0, 0));
			for (int i = 0; i < 1025; i++)
			{
				float x = (i % GRID + 0.5f) / GRID * 2 - 1;
				float y = (i / GRID + 0.5f) / GRID * 2 - 1;
				packed.add(new PInstanceHeader(new Matrix4f().translation(x, y, 0).scale(1.2f / GRID, 1.2f / GRID, 1), color(i), 0, 0));
			}
			var command = new PDrawCommand(3, 1025, 3, -1, 9);
			var group = new PDrawGroup(solid, mesh, command, true, packed.subList(9, packed.size()));
			var plan = new PRenderPlan(List.of(group), packed);
			var frame = new PRenderFrame(new Matrix4f().translation(0.25f, 0, 0), new Matrix4f().translation(-0.25f, 0, 0));
			try
			{
				// Repeat across all ring slots to exercise fence-protected reuse.
				for (int repeat = 0; repeat < 4; repeat++)
				{
					device.createCommandEncoder().clearColorAndDepthTextures(color, new Vector4f(0, 0, 0, 0), depth, 0);
					executor.execute(plan, frame);
				}
				ByteBuffer pixels = pixels(device, color);
				for (int i = 0; i < 1025; i++)
					assertPixel(pixels, i, color(i));
				System.out.println("PASS 1025 unique instances, packed firstInstance=9, firstIndex=3, baseVertex=-1, frame uniforms and ring reuse");
				var compiler = new PFrameCompiler<String>();
				for (int i = 0; i < 1025; i++)
					compiler.submit("world", solid, mesh, new PDrawCommand(3, 1, 3, -1, 0), packed.get(9 + i), false);
				device.createCommandEncoder().clearColorAndDepthTextures(color, new Vector4f(0, 0, 0, 0), depth, 0);
				executor.execute(compiler.compileOpaque("world"), frame);
				pixels = pixels(device, color);
				for (int i = 0; i < 1025; i++)
					assertPixel(pixels, i, color(i));
				RhiLayoutChecks.assertTrue(compiler.compileOpaque("world").isEmpty(), "Compiler consumed its submissions");
				System.out.println("PASS canonical PFrameCompiler -> PRenderPlan -> RHI, 1025 readback pixels");
				expect(IllegalArgumentException.class, () -> executor.execute(new PRenderPlan(List.of(new PDrawGroup(solid, mesh,
						new PDrawCommand(4, 1, 3, -1, 0), true, List.of(packed.get(9))))), frame));
				var oit = registry.pipeline(type, new PRhiMaterial(pipeline, () -> target, capacity, Map.of(), Map.of(),
						com.arcanc.pulselib.content.renderer.plan.PTransparencyMode.OIT));
				RhiLayoutChecks.assertTrue(!solid.equals(oit) && registry.pipelineType(oit) == type &&
						registry.transparency(oit) == com.arcanc.pulselib.content.renderer.plan.PTransparencyMode.OIT,
						"One native type preserves independent transparency policies");
				expect(UnsupportedOperationException.class, () -> executor.execute(new PRenderPlan(List.of(new PDrawGroup(oit, mesh,
						new PDrawCommand(3, 1, 3, -1, 0), true, List.of(packed.get(9))))), frame));
				System.out.println("PASS invalid index bounds and unsupported OIT rejected before drawing (no alpha fallback)");
				device.createCommandEncoder().clearColorAndDepthTextures(color, new Vector4f(0, 0, 0, 0), depth, 0);
				device.createCommandEncoder().clearColorTexture(mask, new Vector4f(1, 1, 1, 0));
				executor.execute(new PRenderPlan(List.of(new PDrawGroup(cutout, mesh,
						new PDrawCommand(3, 1, 3, -1, 0), true, List.of(packed.get(9))))), frame);
				assertPixel(pixels(device, color), 0, 0);
				device.createCommandEncoder().clearColorTexture(mask, new Vector4f(1, 1, 1, 1));
				executor.execute(new PRenderPlan(List.of(new PDrawGroup(cutout, mesh,
						new PDrawCommand(3, 1, 3, -1, 0), true, List.of(packed.get(9))))), frame);
				assertPixel(pixels(device, color), 0, color(0));
				System.out.println("PASS texture/sampler binding and cutout discard/visible pixel");
				var source = new PRhiDynamicGeometry(data);
				var dynamic = registry.dynamic(source);
				var before = registry.resolve(dynamic, data);
				source.update(geometry(-1f, true));
				var after = registry.resolve(dynamic, source.geometry());
				RhiLayoutChecks.assertTrue(before.vertices().isClosed() && before.indices().isClosed() && after != before, "Dynamic replacement cleanup");
				device.createCommandEncoder().clearColorAndDepthTextures(color, new Vector4f(0, 0, 0, 0), depth, 0);
				executor.execute(new PRenderPlan(List.of(new PDrawGroup(solid, dynamic, new PDrawCommand(3, 1, 3, -1, 0), true,
						List.of(packed.get(9))))), frame);
				assertPixel(pixels(device, color), 0, color(0));
				source.close();
				RhiLayoutChecks.assertTrue(after.vertices().isClosed() && after.indices().isClosed(), "Dynamic close releases buffers");
				expect(IllegalStateException.class, () -> registry.geometryData(dynamic));
				expect(IllegalStateException.class, () -> registry.dynamic(source));
				System.out.println("PASS real dynamic geometry upload/update/draw/close");
				checkCanonicalSources(device, registry, executor, solid, frame, data, packed.get(9), color, depth);
				checkNativeMaterial(device, registry, executor, mesh, frame, packed, target, color, maskView, sampler, capacity);
			}
			finally
			{
				executor.cleanup();
			}
			RhiLayoutChecks.assertTrue(cached.vertices().isClosed() && cached.indices().isClosed(), "Static GPU cleanup");
			expect(IllegalStateException.class, () -> registry.geometryData(mesh));
			expect(IllegalStateException.class, () -> registry.pipelineType(solid));
			expect(IllegalStateException.class, () -> registry.pipelineType(other));
			RhiLayoutChecks.assertTrue(!color.isClosed() && !mask.isClosed(), "Borrowed material resources retained");
			System.out.println("PASS static GPU buffers closed, stale handles rejected; borrowed target resources remain open until fixture cleanup");
		}
	}

	private static void checkNativeMaterial(GpuDevice device, PRhiResourceRegistry registry, PRhiDrawExecutor executor,
			PMeshHandle mesh, PRenderFrame frame, List<PInstanceHeader> packed, PRhiMaterial.Attachments firstTarget,
			GpuTexture firstColor, GpuTextureView visibleMask, GpuSampler sampler, int capacity)
	{
		var oldLights = RenderSystem.getShaderLights();
		var oldFog = RenderSystem.getShaderFog();
		var oldGlobals = RenderSystem.getGlobalSettingsUniform();
		try (var secondColor = device.createTexture("RHI native second target", GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_COPY_DST,
				GpuFormat.RGBA8_UNORM, SIZE, SIZE, 1, 1);
				var secondView = device.createTextureView(secondColor);
				var hidden = device.createTexture("RHI native hidden mask", GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST,
						GpuFormat.RGBA8_UNORM, 1, 1, 1, 1);
				var hiddenView = device.createTextureView(hidden);
				var nativeTransforms = device.createBuffer(() -> "Unused native transforms", GpuBuffer.USAGE_UNIFORM, ByteBuffer.allocateDirect(176));
				var lighting = uniform(device, 1, 0, 1);
				var fog = uniform(device, 1, 1, 0);
				var globals = uniform(device, 0, 1, 1);
				var white = uniform(device, 1, 1, 1))
		{
			RenderSystem.setShaderLights(lighting.slice());
			RenderSystem.setShaderFog(fog.slice());
			RenderSystem.setGlobalSettingsUniform(globals);
			device.createCommandEncoder().clearColorTexture(hidden, new Vector4f(0, 0, 0, 0));
			var pipeline = pipeline(true, true);
			var type = RenderType.create("rhi_native_prepared", RenderSetup.builder(pipeline).createRenderSetup());
			var scissor = new ScissorState();
			scissor.enable(0, 0, SIZE / 2, SIZE);
			int[] calls = {0, 0};
			int[] mode = {0};
			var secondTarget = new PRhiMaterial.Attachments(secondView, firstTarget.depth());
			var nativeMaterial = new PRhiNativeMaterial(type, capacity, PTransparencyMode.ORDINARY, Map.of(), () ->
			{
				calls[0]++;
				var texture = new PreparedRenderType.Texture("Sampler0", mode[0] == 1 ? hiddenView : visibleMask, sampler);
				return new PreparedRenderType(pipeline, OutputTarget.MAIN_TARGET, nativeTransforms.slice(), scissor,
						mode[0] == 3 ? List.of() : mode[0] == 4 ? List.of(texture, texture) :
								List.of(texture, new PreparedRenderType.Texture("Sampler1", visibleMask, sampler),
										new PreparedRenderType.Texture("Sampler2", visibleMask, sampler)));
			}, prepared ->
			{
				calls[1]++;
				// Mutating the original native object cannot change an already captured scissor.
				scissor.disable();
				return mode[0] == 0 ? firstTarget : secondTarget;
			});
			var handle = nativeMaterial.register(registry);
			RhiLayoutChecks.assertTrue(handle.equals(nativeMaterial.register(registry)), "Native factory registration cached");
			var command = new PDrawCommand(3, 1025, 3, -1, 9);
			var plan = new PRenderPlan(List.of(new PDrawGroup(handle, mesh, command, true, packed.subList(9, packed.size()))), packed);
			device.createCommandEncoder().clearColorAndDepthTextures(firstColor, new Vector4f(0, 0, 0, 0), firstTarget.depth().texture(), 0);
			executor.execute(plan, frame);
			ByteBuffer firstPixels = pixels(device, firstColor);
			for (int i = 0; i < 1025; i++)
				assertPixel(firstPixels, i, i % GRID < GRID / 2 ? 0xFF000000 : 0);
			RhiLayoutChecks.assertTrue(calls[0] == 1 && calls[1] == 1, "Prepare and output resolution once across all chunks");
			mode[0] = 1;
			device.createCommandEncoder().clearColorAndDepthTextures(secondColor, new Vector4f(0, 0, 0, 0), firstTarget.depth().texture(), 0);
			executor.execute(plan, frame);
			ByteBuffer hiddenPixels = pixels(device, secondColor);
			for (int i = 0; i < 1025; i++)
				assertPixel(hiddenPixels, i, 0);
			mode[0] = 2;
			RenderSystem.setShaderLights(white.slice());
			RenderSystem.setShaderFog(white.slice());
			RenderSystem.setGlobalSettingsUniform(white);
			executor.execute(plan, frame);
			ByteBuffer secondPixels = pixels(device, secondColor);
			for (int i = 0; i < 1025; i++)
				assertPixel(secondPixels, i, color(i));
			ByteBuffer retained = pixels(device, firstColor);
			for (int i = 0; i < 1025; i++)
				assertPixel(retained, i, i % GRID < GRID / 2 ? 0xFF000000 : 0);
			RhiLayoutChecks.assertTrue(calls[0] == 3 && calls[1] == 3, "Fresh native bindings for each execution");
			mode[0] = 3;
			expect(NullPointerException.class, () -> executor.execute(plan, frame));
			RhiLayoutChecks.assertTrue(calls[0] == 4 && calls[1] == 3, "Missing native texture rejected before target resolution/draw");
			mode[0] = 4;
			expect(IllegalArgumentException.class, () -> executor.execute(plan, frame));
			RhiLayoutChecks.assertTrue(calls[0] == 5 && calls[1] == 3, "Duplicate native texture rejected before target resolution/draw");
			RhiLayoutChecks.assertTrue(RenderSystem.getShaderLights().equals(white.slice()) &&
					RenderSystem.getShaderFog().equals(white.slice()) && RenderSystem.getGlobalSettingsUniform() == white,
					"Executor does not replace host uniform bindings");
			System.out.println("PASS prepared native material: one snapshot per execution, scissor, 1025 pixels, targets, Sampler0/1/2, default uniforms, frame ownership and missing/duplicate binding rejection");
		}
		finally
		{
			RenderSystem.setShaderLights(oldLights);
			RenderSystem.setShaderFog(oldFog);
			RenderSystem.setGlobalSettingsUniform(oldGlobals);
		}
	}

	private static GpuBuffer uniform(GpuDevice device, float red, float green, float blue)
	{
		var bytes = ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder());
		bytes.putFloat(red).putFloat(green).putFloat(blue).putFloat(1).flip();
		return device.createBuffer(() -> "Native uniform fixture", GpuBuffer.USAGE_UNIFORM, bytes);
	}

	private static void checkCanonicalSources(GpuDevice device, PRhiResourceRegistry registry, PRhiDrawExecutor executor,
			PPipelineHandle pipeline, PRenderFrame frame,
			PGeometryData data, PInstanceHeader header, GpuTexture color, GpuTexture depth)
	{
		var facade = new GlDynamicGeometry();
		facade.vertexBuffer().update(data);
		var canonical = new PGlDynamicGeometry(data);
		for (PGeometrySource source : List.of(canonical, facade))
		{
			var mesh = registry.dynamic(source);
			RhiLayoutChecks.assertTrue(mesh.equals(registry.dynamic(source)) && registry.indexCount(mesh) == data.indexCount(), "Canonical source cached");
			var before = registry.resolve(mesh, data);
			PGeometryData updated = geometry(-1, true);
			if (source == facade)
				facade.vertexBuffer().update(updated);
			else
				canonical.update(updated);
			device.createCommandEncoder().clearColorAndDepthTextures(color, new Vector4f(0, 0, 0, 0), depth, 0);
			var compiler = new PFrameCompiler<String>();
			compiler.submit("canonical", pipeline, mesh, new PDrawCommand(3, 1, 3, -1, 0), header, false);
			executor.execute(compiler.compileOpaque("canonical"), frame);
			assertPixel(pixels(device, color), 0, color(0));
			var after = registry.resolve(mesh, updated);
			RhiLayoutChecks.assertTrue(before.vertices().isClosed() && before.indices().isClosed(), "Canonical update closes previous allocation");
			if (source == facade)
			{
				var alias = registry.dynamic(facade.vertexBuffer());
				var aliasBuffers = registry.resolve(alias, updated);
				RhiLayoutChecks.assertTrue(alias.equals(mesh) && aliasBuffers == after, "Facade/native aliases share one handle and GPU allocation");
				try (var detached = new PRhiResourceRegistry(device))
				{
					var detachedMesh = detached.dynamic(facade);
					detached.resolve(detachedMesh, updated);
					// Value-equivalent handles must also detach the original identity listener.
					detached.release(new PMeshHandle(detachedMesh.value()));
					detached.dynamic(facade);
				}
				RhiLayoutChecks.assertTrue(!after.vertices().isClosed() && facade.geometry() == updated, "Other registry cleanup retains source/allocations");
				facade.close();
				RhiLayoutChecks.assertTrue(aliasBuffers.vertices().isClosed() && aliasBuffers.indices().isClosed(), "Facade closes shared allocation");
				expect(IllegalStateException.class, () -> registry.geometryData(alias));
			}
			else
			{
				canonical.onClose(new Object(), () ->
				{
					throw new IllegalStateException("Expected unrelated owner failure");
				});
				expect(IllegalStateException.class, canonical::close);
			}
			RhiLayoutChecks.assertTrue(after.vertices().isClosed() && after.indices().isClosed(), "Canonical close releases RHI GPU allocation");
			expect(IllegalStateException.class, () -> registry.geometryData(mesh));
			expect(IllegalStateException.class, () -> registry.dynamic(source));
			source.close();
		}
		System.out.println("PASS canonical PGlDynamicGeometry/GlDynamicGeometry -> compiler -> RHI pixels, updates, aliases, cross-registry release and listener-failure cleanup");
	}

	private static void expect(Class<? extends RuntimeException> type, Runnable action)
	{
		try
		{
			action.run();
		}
		catch (RuntimeException failure)
		{
			if (type.isInstance(failure))
				return;
			throw failure;
		}
		throw new AssertionError("Expected " + type.getName());
	}

	private static PGeometryData geometry(float flip)
	{
		return geometry(flip, false);
	}

	private static PGeometryData geometry(float flip, boolean integers)
	{
		ByteBuffer vertices = ByteBuffer.allocate(48).order(ByteOrder.nativeOrder());
		vertices.putFloat(-99).putFloat(-99).putFloat(0);
		vertices.putFloat(-flip).putFloat(-1).putFloat(0.5f);
		vertices.putFloat(flip).putFloat(-1).putFloat(0.5f);
		vertices.putFloat(0).putFloat(1).putFloat(0.5f).flip();
		ByteBuffer indices = ByteBuffer.allocate(integers ? 24 : 12).order(ByteOrder.nativeOrder());
		for (int value : new int[]{0, 0, 0, 2, 3, 4})
			if (integers)
				indices.putInt(value);
			else
				indices.putShort((short)value);
		indices.flip();
		return new PGeometryData(vertices, indices, 12, 6,
				integers ? PGeometryData.IndexType.UNSIGNED_INT : PGeometryData.IndexType.UNSIGNED_SHORT);
	}

	private static RenderPipeline pipeline(boolean cutout)
	{
		return pipeline(cutout, false);
	}

	private static RenderPipeline pipeline(boolean cutout, boolean nativeDefaults)
	{
		var layout = BindGroupLayout.builder().withUniform("InstanceData", UniformType.UNIFORM_BUFFER);
		if (cutout)
			layout.withSampler("Sampler0");
		var pipeline = RenderPipeline.builder().withLocation(Identifier.fromNamespaceAndPath("porting_fixture", cutout ? "cutout" : "solid")).
				withVertexShader(Identifier.fromNamespaceAndPath("porting_fixture", "test")).
				withFragmentShader(Identifier.fromNamespaceAndPath("porting_fixture", "test")).
				withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION).withBindGroupLayout(layout.build()).
				withVertexBinding(0, VertexFormat.builder(0).addAttribute("Position", GpuFormat.RGB32_FLOAT).build()).
				withPrimitiveTopology(PrimitiveTopology.TRIANGLES).withDepthStencilState(DepthStencilState.DEFAULT).withCull(false);
		if (cutout)
			pipeline.withShaderDefine("CUTOUT");
		if (nativeDefaults)
			pipeline.withBindGroupLayout(BindGroupLayout.builder().withSampler("Sampler1").withSampler("Sampler2").
					withUniform("Lighting", UniformType.UNIFORM_BUFFER).
					withUniform("Fog", UniformType.UNIFORM_BUFFER).withUniform("Globals", UniformType.UNIFORM_BUFFER).build()).
					withShaderDefine("NATIVE_BINDINGS");
		return pipeline.build();
	}

	private static ByteBuffer pixels(GpuDevice device, GpuTexture color)
	{
		try (GpuBuffer buffer = device.createBuffer(() -> "RHI fixture readback", GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_READ, SIZE * SIZE * 4))
		{
			var encoder = device.createCommandEncoder();
			encoder.copyTextureToBuffer(color, buffer, 0, () -> {}, 0);
			try (var fence = encoder.createFence())
			{
				encoder.submit();
				RhiLayoutChecks.assertTrue(fence.awaitCompletion(30_000_000_000L), "GPU readback fence");
			}
			try (var map = buffer.map(true, false))
			{
				ByteBuffer copy = ByteBuffer.allocate(SIZE * SIZE * 4);
				copy.put(map.data()).flip();
				return copy;
			}
		}
	}

	private static int color(int i)
	{
		return 0xFF00003F | ((i % 254 + 1) << 16) | ((i / 254 + 1) << 8);
	}

	private static void assertPixel(ByteBuffer pixels, int i, int expected)
	{
		int x = (int)((i % GRID + 0.5f) / GRID * SIZE);
		int y = (int)((i / GRID + 0.5f) / GRID * SIZE);
		int offset = (y * SIZE + x) * 4;
		int actual = (pixels.get(offset + 3) & 255) << 24 | (pixels.get(offset) & 255) << 16 |
				(pixels.get(offset + 1) & 255) << 8 | pixels.get(offset + 2) & 255;
		RhiLayoutChecks.assertTrue(actual == expected, "Instance " + i + " pixel: expected " + Integer.toHexString(expected) + " actual " + Integer.toHexString(actual));
	}

	private static String vertexShader(int capacity)
	{
		return """
				#version 330
				layout(std140) uniform DynamicTransforms { mat4 ModelViewMat; vec4 ColorModulator; vec3 ModelOffset; mat4 TextureMat; };
				layout(std140) uniform Projection { mat4 ProjMat; };
				struct Instance { mat4 model; vec4 color; vec2 light; vec2 overlay; ivec4 deformer; };
				layout(std140) uniform InstanceData { Instance data[%d]; };
				in vec3 Position;
				out vec4 vertexColor;
				void main() { gl_Position = ProjMat * ModelViewMat * data[gl_InstanceID].model * vec4(Position, 1); vertexColor = data[gl_InstanceID].color; }
				""".formatted(capacity);
	}

	private static String fragmentShader()
	{
		return """
				#version 330
				in vec4 vertexColor;
				out vec4 fragColor;
				#ifdef CUTOUT
				uniform sampler2D Sampler0;
				#endif
				#ifdef NATIVE_BINDINGS
				uniform sampler2D Sampler1;
				uniform sampler2D Sampler2;
				layout(std140) uniform Lighting { vec4 TestLight; };
				layout(std140) uniform Fog { vec4 TestFog; };
				layout(std140) uniform Globals { vec4 TestGlobals; };
				#endif
				void main() {
				#ifdef CUTOUT
				if (texture(Sampler0, vec2(0.5)).a < 0.5) discard;
				#endif
				fragColor = vertexColor;
				#ifdef NATIVE_BINDINGS
				fragColor *= TestLight * TestFog * TestGlobals * texture(Sampler1, vec2(0.5)) * texture(Sampler2, vec2(0.5));
				#endif
				}
				""";
	}
}
