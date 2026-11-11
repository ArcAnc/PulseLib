/**
 * @author ArcAnc
 * Created at: 11.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test;

import com.arcanc.pulselib.content.model.deformer.PChannelReference;
import com.arcanc.pulselib.content.model.deformer.PDeformerInstance;
import com.arcanc.pulselib.content.model.deformer.PDeformerStack;
import com.arcanc.pulselib.content.model.deformer.PDeformerValueSource;
import com.arcanc.pulselib.content.model.deformer.PMeshDeformation;
import com.arcanc.pulselib.content.model.deformer.PStretchDeformer;
import com.arcanc.pulselib.content.model.deformer.PStretchDefinition;
import com.arcanc.pulselib.content.model.deformer.gpu.PFrameStreams;
import com.arcanc.pulselib.content.model.deformer.gpu.PGpuDeformerBuffers;
import com.arcanc.pulselib.content.renderer.legacy.GlCapabilityProbe;
import com.arcanc.pulselib.content.renderer.legacy.GlDrawExecutor;
import com.arcanc.pulselib.content.renderer.legacy.GlDynamicGeometry;
import com.arcanc.pulselib.content.renderer.legacy.GlFrameArena;
import com.arcanc.pulselib.content.renderer.legacy.GlRenderState;
import com.arcanc.pulselib.content.renderer.legacy.GlResourceRegistry;
import com.arcanc.pulselib.content.renderer.legacy.GlStreamStorage;
import com.arcanc.pulselib.content.renderer.legacy.GlWeightedBlendedOit;
import com.arcanc.pulselib.content.renderer.legacy.McLegacyGlHostBridge;
import com.arcanc.pulselib.content.renderer.plan.PDrawGroup;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.content.renderer.plan.PMeshHandle;
import com.arcanc.pulselib.content.renderer.plan.PRenderFrame;
import com.arcanc.pulselib.content.renderer.plan.PRenderPlan;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PRenderTypes;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL32;

import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/** Exercises canonical facades through real native allocations, draws and deletion. */
public final class GpuFacadeProbe
{
	private static boolean complete;
	private static int pixelCases;
	private static int fenceChecks;

	private GpuFacadeProbe()
	{
	}

	public static void run()
	{
		if (complete)
			return;
		RenderSystem.getModelViewStack().pushMatrix();
		try (var caller = new GlRenderState(); var projection = new ProjectionMatrixBuffer("GPU facade probe"))
		{
			RenderSystem.getModelViewStack().identity();
			RenderSystem.setProjectionMatrix(projection.getBuffer(new Matrix4f()), ProjectionType.ORTHOGRAPHIC);
			RenderSystem.disableScissorForRenderTypeDraws();
			PRenderFrame frame = new McLegacyGlHostBridge().captureFrame();
			DemoChecks.check(frame.projection().equals(new Matrix4f()), "Facade host captures the actual native projection upload");
			fences();
			TextureTarget target = new TextureTarget("GPU facade pixels", 64, 32, true);
			try
			{
				geometry(target, frame);
				uploads(target);
				oit(target);
			}
			finally
			{
				target.destroyBuffers();
			}
			DemoChecks.check(GL11.glGetError() == GL11.GL_NO_ERROR, "Facade native operations leave no OpenGL error");
			Files.writeString(DemoChecks.runDirectory().resolve("gpu-facades.json"), new GsonBuilder().setPrettyPrinting().create().
					toJson(Map.of("status", "PASS", "pixelCases", pixelCases, "fenceChecks", fenceChecks,
							"nativeStreams", true, "nativeDeletion", true)));
			complete = true;
			PLibDatabase.LOGGER.info("GPU FACADE PASS: pixels={}, fences={}, native stream readback/deletion", pixelCases, fenceChecks);
		}
		catch (ReflectiveOperationException | java.io.IOException exception)
		{
			throw new IllegalStateException("GPU facade probe failed", exception);
		}
		finally
		{
			RenderSystem.getModelViewStack().popMatrix();
		}
	}

	private static void fences() throws ReflectiveOperationException
	{
		var arena = new GlFrameArena(2);
		DemoChecks.check(arena.begin(false) == 0 && arena.begin(false) == 1 && arena.begin(false) == 0, "Canonical ordinary slots rotate");
		arena.reset();
		int slot = arena.beginWaiting(true);
		arena.finish(slot, true);
		long sync = ((long[])field(field(arena, "arena"), "fences"))[slot];
		DemoChecks.check(GL32.glIsSync(sync), "Facade creates a real native GPU fence");
		boolean rejected = false;
		try
		{
			arena.finish(slot, true);
		}
		catch (IllegalStateException expected)
		{
			rejected = true;
		}
		DemoChecks.check(rejected, "Facade rejects double submission of a pending slot");
		arena.await(slot);
		DemoChecks.check(!GL32.glIsSync(sync), "Facade retires and deletes the actual GPU fence");
		arena.reset();
		DemoChecks.check(arena.begin(true) == 0, "Canonical reset restarts the native ring");
		arena.finish(0, true);
		arena.awaitAll();
		fenceChecks = 5;
	}

	private static void geometry(RenderTarget target, PRenderFrame frame)
	{
		var resources = new GlResourceRegistry();
		var executor = new GlDrawExecutor(resources);
		var dynamic = new GlDynamicGeometry();
		int vao = 0;
		try
		{
			DemoChecks.check(executor.capabilityMatrix().equals(GlCapabilityProbe.currentContext()), "Facade capability probe uses the actual current context");
			DemoChecks.check(executor.capabilities().matrix().equals(GlCapabilityProbe.currentContext()), "Facade retains the native capability matrix");
			resources.geometry(H14OffscreenHarness.atlasQuad());
			PMeshHandle second = resources.geometry(H14OffscreenHarness.atlasQuad());
			var slice = resources.geometry(second);
			vao = slice.vertexArray();
			DemoChecks.check(slice.indexOffset() > 0 && slice.baseVertex() > 0 && GL30.glIsVertexArray(vao), "Second immutable facade mesh occupies a real nonzero native page range");
			DemoChecks.check(resources.geometry(second) == slice, "Facade slice identity is stable");
			H14OffscreenHarness.clear(target, 0, 0, 0, 1, 1);
			rgb(target, 0, 0, 0);
			draw(resources, executor, second, frame, target, false);
			rgb(target, 1, 0, 0);
			PGeometryData source = H14OffscreenHarness.atlasQuad();
			var indices = BufferUtils.createByteBuffer(6 * Integer.BYTES).order(ByteOrder.nativeOrder());
			for (int index = 0; index < 6; index++)
				indices.putInt(Short.toUnsignedInt(source.indices().order(ByteOrder.nativeOrder()).getShort(index * Short.BYTES)));
			indices.flip();
			PMeshHandle wide = resources.geometry(new PGeometryData(source.vertices(), indices, source.vertexStride(), 6, PGeometryData.IndexType.UNSIGNED_INT));
			H14OffscreenHarness.clear(target, 0, 0, 0, 1, 1);
			draw(resources, executor, wide, frame, target, true);
			rgb(target, 1, 0, 0);
			dynamic.vertexBuffer().update(H14OffscreenHarness.atlasQuad());
			PMeshHandle moving = resources.dynamic(dynamic);
			DemoChecks.check(resources.mesh(moving) == dynamic.vertexBuffer() && resources.indexCount(dynamic) == 6, "Dynamic facade and registry share the native source");
			H14OffscreenHarness.clear(target, 0, 0, 0, 1, 1);
			draw(resources, executor, moving, frame, target, true);
			rgb(target, 1, 0, 0);
			dynamic.close();
			boolean rejected = false;
			try
			{
				resources.mesh(moving);
			}
			catch (IllegalStateException expected)
			{
				rejected = true;
			}
			DemoChecks.check(rejected, "Closing dynamic geometry invalidates its facade handle");
			H14OffscreenHarness.clear(target, 0, 0, 0, 1, 1);
			var translucent = resources.pipeline(PRenderTypes.RenderTypeProvider.trianglesEmissiveTranslucent(H14OffscreenHarness.WHITE));
			var alpha = new PInstanceHeader(new Matrix4f(), 0x80FF0000, 0xF000F0, OverlayTexture.NO_OVERLAY);
			var plan = new PRenderPlan(List.of(new PDrawGroup(translucent, second, resources.resolveCommand(second, 6, 1), false, List.of(alpha))));
			try (var binding = new H14GpuTargets.Binding(target, true); var state = new GlRenderState())
			{
				executor.execute(plan, frame, target);
				executor.compositeOit(target);
			}
			rgb(target, 128f / 255, 0, 0);
			executor.cleanup();
			DemoChecks.check(GL30.glIsVertexArray(vao) && resources.isGeometry(second), "Executor cleanup retains registry-owned geometry and handles");
			H14OffscreenHarness.clear(target, 0, 0, 0, 1, 1);
			draw(resources, executor, second, frame, target, true);
			rgb(target, 1, 0, 0);
		}
		finally
		{
			dynamic.close();
			executor.cleanup();
			resources.clear();
		}
		DemoChecks.check(!GL30.glIsVertexArray(vao), "Facade cleanup deletes the same native arena VAO");
	}

	private static void draw(GlResourceRegistry resources, GlDrawExecutor executor, PMeshHandle mesh, PRenderFrame frame, RenderTarget target, boolean local)
	{
		var pipeline = resources.pipeline(PRenderTypes.RenderTypeProvider.trianglesEmissiveSolid(H14OffscreenHarness.WHITE));
		var instance = new PInstanceHeader(new Matrix4f(), 0xFFFF0000, 0xF000F0, OverlayTexture.NO_OVERLAY);
		var plan = new PRenderPlan(List.of(new PDrawGroup(pipeline, mesh, resources.resolveCommand(mesh, 6, 1), true, List.of(instance))));
		try (var binding = new H14GpuTargets.Binding(target, true); var state = new GlRenderState())
		{
			if (local)
				executor.executeLocal(plan, frame, state);
			else
				executor.execute(plan, frame, target);
		}
	}

	private static void uploads(RenderTarget target) throws ReflectiveOperationException
	{
		var streams = new PFrameStreams();
		var definition = new PStretchDefinition(new Vector3f(), new Vector3f(1, 0, 0), PChannelReference.constant(1.5f));
		var stack = PDeformerStack.compile(List.of(new PDeformerInstance<>(PStretchDeformer.INSTANCE, definition)));
		streams.deformers().submit(new PMeshDeformation(stack, PDeformerValueSource.DEFAULTS, stack));
		try
		{
			try (var pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
					() -> "Unprepared stream control", target.getColorTextureView(), OptionalInt.empty(), target.getDepthTextureView(), OptionalDouble.empty()))
			{
				boolean rejected = false;
				try
				{
					GlStreamStorage.bind(pass, streams);
				}
				catch (IllegalStateException expected)
				{
					rejected = true;
				}
				DemoChecks.check(rejected, "Native binding rejects unprepared streams");
			}
			GlStreamStorage.prepare(streams);
			GlStreamStorage.prepare(PGpuDeformerBuffers.streams());
			try (var pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
					() -> "Canonical stream bindings", target.getColorTextureView(), OptionalInt.empty(), target.getDepthTextureView(), OptionalDouble.empty()))
			{
				pass.setPipeline(PRenderTypes.RenderTypeProvider.trianglesEmissiveSolid(H14OffscreenHarness.WHITE).pipeline());
				GlStreamStorage.bind(pass, streams);
				GlStreamStorage.bind(pass, PGpuDeformerBuffers.streams());
			}
			var owners = (Map<?, ?>)field(GlStreamStorage.class, "UPLOADS");
			var owner = (PGpuDeformerBuffers.UploadBuffers)owners.get(streams);
			var buffers = owner.upload();
			int name = (int)field(buffers.values(), "handle");
			int previous = GL11.glGetInteger(GL31.GL_TEXTURE_BUFFER);
			try
			{
				GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, name);
				float[] values = new float[4];
				GL15.glGetBufferSubData(GL31.GL_TEXTURE_BUFFER, 0, values);
				DemoChecks.check(values[0] == 1.5f, "Canonical external stream bytes reach real native texel-buffer storage");
			}
			finally
			{
				GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, previous);
			}
			GlStreamStorage.cleanup();
			DemoChecks.check(buffers.values().isClosed() && !GL15.glIsBuffer(name), "Facade cleanup closes native stream buffers");
			DemoChecks.check(streams.deformers().values().getFirst() == 1.5f, "Upload cleanup preserves canonical CPU stream values");
		}
		finally
		{
			GlStreamStorage.cleanup();
		}
	}

	private static void oit(RenderTarget target)
	{
		var oit = new GlWeightedBlendedOit();
		int name = 0;
		try
		{
			DemoChecks.check(GlWeightedBlendedOit.LAYER_COUNT == 4 && oit.isSupported() && oit.begin(target), "Canonical OIT facade opens the native four-layer owner");
			for (int layer = 0; layer < GlWeightedBlendedOit.LAYER_COUNT; layer++)
			{
				oit.beginDepthPass(layer);
				oit.bindForDraw();
				name = oit.activeLayerDepthTexture();
				DemoChecks.check(GL11.glIsTexture(name), "Facade exposes the actual native depth texture");
				if (layer > 0)
					DemoChecks.check(GL11.glIsTexture(oit.previousLayerDepthTexture()), "Facade exposes the native predecessor layer");
				oit.endPass();
				oit.beginAccumulationPass(layer);
				oit.endPass();
			}
			oit.composite(target);
		}
		finally
		{
			oit.close();
		}
		DemoChecks.check(!GL11.glIsTexture(name), "Facade closes native OIT layer textures");
	}

	private static void rgb(RenderTarget target, float red, float green, float blue)
	{
		float[] pixels = H14OffscreenHarness.pixels(target);
		for (int side = 0; side < 2; side++)
			DemoChecks.check(Math.abs(pixels[side * 4] - red) < 0.025f && Math.abs(pixels[side * 4 + 1] - green) < 0.025f &&
					Math.abs(pixels[side * 4 + 2] - blue) < 0.025f, "Facade draw pixels match the independent color expectation");
		pixelCases++;
	}

	private static Object field(Object owner, String name) throws ReflectiveOperationException
	{
		Class<?> type = owner instanceof Class<?> declared ? declared : owner.getClass();
		var field = type.getDeclaredField(name);
		field.setAccessible(true);
		return field.get(owner instanceof Class<?> ? null : owner);
	}
}
