/**
 * @author ArcAnc
 * Created at: 25.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1;

import com.arcanc.pulselib.content.model.baked.PBakedMesh;
import com.arcanc.pulselib.content.model.deformer.PChannelReference;
import com.arcanc.pulselib.content.model.deformer.PDeformerInstance;
import com.arcanc.pulselib.content.model.deformer.PDeformerStack;
import com.arcanc.pulselib.content.model.deformer.PDeformerValueSource;
import com.arcanc.pulselib.content.model.deformer.PHingeDeformer;
import com.arcanc.pulselib.content.model.deformer.PHingeDefinition;
import com.arcanc.pulselib.content.model.deformer.PMeshDeformation;
import com.arcanc.pulselib.content.model.deformer.gpu.PGpuDeformerBuffers;
import com.arcanc.pulselib.content.registration.block.block_entity.ber.TestBlockEntityRenderer;
import com.arcanc.pulselib.content.renderer.PRenderContexts;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.gl.PGlDynamicGeometry;
import com.arcanc.pulselib.content.renderer.legacy.GlRenderState;
import com.arcanc.pulselib.content.renderer.plan.PFrameCompiler;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.PResourceCache;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GL40;
import org.lwjgl.opengl.GL32;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.arcanc.pulselib.test.stage1.Stage41bGeometryProbe.field;
import static com.arcanc.pulselib.test.stage1.Stage42b1OitProbe.clear;
import static com.arcanc.pulselib.test.stage1.Stage42b1OitProbe.pixel;

/** Actual GPU ownership, nested target/matrix capture, ordered alpha, discard and failure recovery. */
public final class Stage43aContextProbe
{
	private static final PRenderQueue.RenderStage SENTINEL = new PRenderQueue.RenderStage("stage43a_world_sentinel");
	private static int ticks;
	private static @Nullable CompletableFuture<Void> reload;
	private static int pending;
	private static int passes;
	private static int cases;

	private Stage43aContextProbe()
	{
	}

	public static void register()
	{
		NeoForge.EVENT_BUS.addListener(Stage43aContextProbe::render);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Stage43aContextProbe::shutdown);
	}

	public static void tick()
	{
		var mc = Minecraft.getInstance();
		if (mc.level == null)
			return;
		ticks++;
		if (ticks == 115 || ticks >= 245 && passes == 1 && reload != null && reload.isDone())
			pending = ticks;
		if (ticks == 120)
			reload = mc.reloadResourcePacks();
		if (ticks == 270 && passes == 2)
			net.minecraft.client.Screenshot.grab(mc.gameDirectory, "stage43a-after-reload.png", mc.getMainRenderTarget(), 1,
					message -> PLibDatabase.LOGGER.info("{}", message.getString()));
		if (ticks >= 300 && passes == 2)
		{
			reload.join();
			Stage2PluginProbe.verify(true);
			check(Stage1Client.entitySubmissions > 20 && Stage1Client.blockSubmissions > 20 && Stage1Client.itemSubmissions > 20, "Real world owners must keep rendering");
			mc.stop();
		}
		if (ticks > 1200)
			throw new AssertionError("Stage 4.3a suites did not complete");
	}

	private static void render(RenderLevelStageEvent.AfterOpaqueBlocks event)
	{
		if (pending == 0)
			return;
		var projection = RenderSystem.getProjectionMatrixBuffer();
		var projectionType = RenderSystem.getProjectionType();
		var modelView = new Matrix4f(RenderSystem.getModelViewMatrix());
		var color = RenderSystem.outputColorTextureOverride;
		var depth = RenderSystem.outputDepthTextureOverride;
		var caller = new GlRenderState();
		var a = new TextureTarget("stage43a outer", 64, 48, true);
		var b = new TextureTarget("stage43a inner", 32, 24, true);
		var c = new TextureTarget("stage43a color only", 48, 32, false);
		try (var identity = new ProjectionMatrixBuffer("stage43a identity");
		     var decoyProjection = new ProjectionMatrixBuffer("stage43a unrelated projection"))
		{
			RenderSystem.setProjectionMatrix(identity.getBuffer(new Matrix4f()), ProjectionType.ORTHOGRAPHIC);
			RenderSystem.getModelViewStack().identity();
			RenderSystem.disableScissorForRenderTypeDraws();
			var quad = Stage42aWorldStageProbe.quad();
			var type = PRenderTypes.RenderTypeProvider.trianglesEmissiveSolid(PResourceCache.ATLAS_LOCATION);
			var alpha = PRenderTypes.RenderTypeProvider.trianglesEmissiveTranslucent(PResourceCache.ATLAS_LOCATION);
			var header = header(0, 0xFF00FF00);
			PRenderQueue.submit(SENTINEL, type, quad, header);
			var world = (PFrameCompiler<?>)field(PRenderQueue.class, null, "COMPILER");
			check(!world.isEmpty(), "World sentinel must be pending before local rendering");
			clear(a, 0xFF000000, 1);
			clear(b, 0xFF000000, 1);
			clear(c, 0xFF000000, 1);
			bind(a);
			var entry = RenderSystem.getProjectionMatrixBuffer();
			GlStateManager._viewport(2, 3, 7, 11);
			GlStateManager._enableScissorTest();
			GlStateManager._scissorBox(0, 0, 1, 1);
			GlStateManager._depthFunc(GL11.GL_GREATER);
			GlStateManager._enableCull();
			GlStateManager._blendFuncSeparate(GL11.GL_ONE, GL11.GL_ZERO, GL11.GL_ZERO, GL11.GL_ONE);
			var state = nativeState();
			var outer = PRenderContexts.enter(PRenderContexts.Context.GUI);
			check(PRenderContexts.current() == PRenderContexts.Context.GUI && PRenderContexts.enterEntity() == null, "Explicit GUI owns direct entity submissions");
			PRenderQueue.submitEntityMesh(type, quad, header);
			bind(b);
			try (var inner = PRenderContexts.enter(PRenderContexts.Context.OFFSCREEN))
			{
				check(PRenderContexts.current() == PRenderContexts.Context.OFFSCREEN, "Inner offscreen context");
				try
				{
					outer.close();
					throw new AssertionError("Out-of-order scope close accepted");
				}
				catch (IllegalStateException expected)
				{
					check(expected.getMessage().contains("reverse order"), "LIFO failure reason");
				}
				PRenderQueue.submitItem(ItemDisplayContext.GUI, type, quad, header(0, 0xFF0000FF));
			}
			bind(c);
			RenderSystem.setProjectionMatrix(decoyProjection.getBuffer(new Matrix4f().scaling(0)), ProjectionType.ORTHOGRAPHIC);
			RenderSystem.getModelViewStack().translation(99, 99, 99);
			outer.close();
			outer.close();
			check(RenderSystem.outputColorTextureOverride == a.getColorTextureView() && RenderSystem.outputDepthTextureOverride == a.getDepthTextureView(), "Outer output override restoration");
			check(RenderSystem.getProjectionMatrixBuffer() == entry && new Matrix4f().equals(RenderSystem.getModelViewMatrix()), "Entry frame must be restored");
			check(state.equals(nativeState()), "Native framebuffer/viewport/scissor/program/bindings/pipeline state restoration");
			rgb(a, 0, 1, 0, "outer captured matrix/target");
			rgb(b, 0, 0, 1, "nested item GUI target");
			rgb(c, 0, 0, 0, "unrelated color-only target");
			check(!world.isEmpty(), "Local close consumed world submissions");

			// All portable queue overloads (including GUI dynamic geometry) stay local.
			try (var dynamic = new PGlDynamicGeometry(quad))
			{
				for (int kind = 0; kind < 11; kind++)
				{
					clear(c, 0xFF000000, 1);
					bind(c);
					try (var scope = PRenderContexts.enter(PRenderContexts.Context.OFFSCREEN))
					{
						switch (kind)
						{
							case 0 -> PRenderQueue.submitBlockEntityMesh(type, dynamic, header);
							case 1 -> PRenderQueue.submitBlockEntityMesh(type, quad, header);
							case 2 -> PRenderQueue.submitBlockEntityTranslucentMesh(alpha, dynamic, header);
							case 3 -> PRenderQueue.submitBlockEntityTranslucentMesh(alpha, quad, header);
							case 4 -> PRenderQueue.submitItem(ItemDisplayContext.GUI, type, dynamic, header);
							case 5 -> PRenderQueue.submitItem(ItemDisplayContext.GUI, type, quad, header);
							case 6 -> PRenderQueue.submitEntityMesh(type, dynamic, header);
							case 7 -> PRenderQueue.submitEntityMesh(type, quad, header);
							case 8 -> PRenderQueue.submitFirstPersonMesh(type, quad, header);
							case 9 -> PRenderQueue.submit(SENTINEL, type, dynamic, header);
							case 10 -> PRenderQueue.submit(SENTINEL, type, quad, header);
							default -> throw new AssertionError("Unknown overload");
						}
					}
					rgb(c, 0, 1, 0, "local geometry overload " + kind);
				}
			}
			clear(c, 0xFF000000, 1);
			bind(c);
			try (var scope = PRenderContexts.enter(PRenderContexts.Context.OFFSCREEN))
			{
				PRenderQueue.submitEntityMesh(alpha, quad, header(-0.5f, 0x80FF0000));
				PRenderQueue.submitBlockEntityMesh(alpha, quad, header(0.5f, 0x800000FF));
			}
			float opacity = 128f / 255;
			rgb(c, opacity, 0, opacity * (1 - opacity), "global local alpha order");
			RenderSystem.setProjectionMatrix(decoyProjection.getBuffer(new Matrix4f().perspective((float)Math.toRadians(70), 1, 0.1f, 100)), ProjectionType.PERSPECTIVE);
			clear(c, 0xFF000000, 1);
			try (var scope = PRenderContexts.enter(PRenderContexts.Context.OFFSCREEN))
			{
				PRenderQueue.submitEntityMesh(alpha, quad, header(-1, 0x80FF0000));
				PRenderQueue.submitBlockEntityMesh(alpha, quad, header(-2, 0x800000FF));
			}
			rgb(c, opacity, 0, opacity * (1 - opacity), "perspective local alpha order");
			RenderSystem.setProjectionMatrix(entry, ProjectionType.ORTHOGRAPHIC);
			clear(c, 0xFF000000, 1);
			try (var scope = PRenderContexts.enter(PRenderContexts.Context.OFFSCREEN))
			{
				try
				{
					PRenderQueue.submitEntityMesh(type, quad, header);
					throw new IllegalArgumentException("fixture renderer failure");
				}
				catch (IllegalArgumentException expected)
				{
					scope.discard();
				}
			}
			rgb(c, 0, 0, 0, "discard incomplete renderer");
			try (var dynamic = new PGlDynamicGeometry(quad))
			{
				try
				{
					try (var scope = PRenderContexts.enter(PRenderContexts.Context.OFFSCREEN))
					{
						PRenderQueue.submitEntityMesh(type, dynamic, header);
						dynamic.close();
					}
					throw new AssertionError("Closed pending dynamic source must fail");
				}
				catch (IllegalStateException expected)
				{
					check(expected.getMessage().contains("Unknown PulseLib mesh"), "Unexpected executor failure: " + expected);
				}
			}
			check(PRenderContexts.current() == PRenderContexts.Context.OFFSCREEN, "Failure must pop local scope and preserve target");
			rgb(c, 0, 0, 0, "failed flush did not leave partial geometry");
			try (var scope = PRenderContexts.enterEntity())
			{
				check(scope != null, "Direct entity caller gets automatic local scope");
				PRenderQueue.submitEntityMesh(type, quad, header);
			}
			rgb(c, 0, 1, 0, "successful scope after failed flush");
			PBakedMesh mesh = TestBlockEntityRenderer.MODEL_DATA.getModel().bones().stream().flatMap(bone -> bone.meshes().stream()).findFirst().orElseThrow();
			var gpu = new PMeshDeformation(PDeformerStack.compile(List.of(new PDeformerInstance<>(PHingeDeformer.INSTANCE,
					new PHingeDefinition(new Vector3f(), new Vector3f(0, 1, 0), new Vector3f(1, 0, 0), new PChannelReference<>("", 0.3f))))),
					PDeformerValueSource.DEFAULTS, new Object(), 1);
			var gpuInstance = new PRenderQueue.InstanceData(new Matrix4f().scaling(0.1f), -1, 0xF000F0, 0);
			PRenderQueue.submit(SENTINEL, type, mesh, gpu, gpuInstance);
			var worldOperations = List.copyOf(PGpuDeformerBuffers.operations());
			var worldValues = List.copyOf(PGpuDeformerBuffers.values());
			query(mesh.geometry().indexCount() / 3 * 9, () ->
			{
				try (var scope = PRenderContexts.enter(PRenderContexts.Context.GUI))
				{
					var instance = new PRenderQueue.InstanceData(new Matrix4f().scaling(0.1f), -1, 0xF000F0, 0);
					PRenderQueue.submitEntityMesh(type, mesh, null, instance);
					PRenderQueue.submitEntityMesh(type, mesh, gpu, gpuInstance);
					PRenderQueue.submitBlockEntityMesh(type, mesh, null, instance);
					PRenderQueue.submitBlockEntityTranslucentMesh(alpha, mesh, null, instance);
					PRenderQueue.submitItem(ItemDisplayContext.GUI, type, mesh, null, instance);
					PRenderQueue.submit(SENTINEL, type, mesh, null, instance);
				}
			});
			check(!world.isEmpty(), "Baked facade must also leave world compiler intact");
			check(PGpuDeformerBuffers.operations().size() >= worldOperations.size() &&
					PGpuDeformerBuffers.operations().subList(0, worldOperations.size()).equals(worldOperations), "Local finish must retain pending world GPU operations");
			check(PGpuDeformerBuffers.values().size() >= worldValues.size() &&
					PGpuDeformerBuffers.values().subList(0, worldValues.size()).equals(worldValues), "Local finish must retain pending world GPU values/offsets");
			query(2 + mesh.geometry().indexCount() / 3 * 4, () -> PRenderQueue.flush(SENTINEL));
			try (var scope = PRenderContexts.enter(PRenderContexts.Context.WORLD))
			{
				check(PRenderContexts.current() == PRenderContexts.Context.WORLD && PRenderContexts.enterEntity() == null, "Explicit world remains staged");
			}
			try (var scope = PRenderContexts.enter(PRenderContexts.Context.FIRST_PERSON))
			{
				PRenderQueue.submitEntityMesh(type, quad, header);
			}
			query(2, () -> PRenderQueue.flush(PRenderQueue.RenderStage.FIRST_PERSON));
			clear(a, 0xFF000000, 1);
			clear(c, 0xFF000000, 1);
			PRenderQueue.submit(SENTINEL, alpha, quad, header(0, 0x800000FF));
			PRenderQueue.flush(SENTINEL, a);
			bind(c);
			try (var scope = PRenderContexts.enter(PRenderContexts.Context.GUI))
			{
				PRenderQueue.submitEntityMesh(type, quad, header);
			}
			rgb(a, 0, 0, 0, "local close must not composite pending world OIT");
			PRenderQueue.compositeTranslucency(a);
			rgb(a, 0, 0, opacity, "world OIT survives local close");
			rgb(c, 0, 1, 0, "world composite ignores local target override");
			try (var pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
					() -> "stage43a native pass rejection", c.getColorTextureView(), java.util.OptionalInt.empty()))
			{
				try
				{
					PRenderContexts.enter(PRenderContexts.Context.GUI);
					throw new AssertionError("Cannot create a local context inside an active native RenderPass");
				}
				catch (IllegalStateException expected)
				{
					check(expected.getMessage().contains("active Minecraft RenderPass"), "Native pass rejection reason");
				}
			}
			try (var scope = PRenderContexts.enter(PRenderContexts.Context.OFFSCREEN))
			{
				var failure = new java.util.concurrent.atomic.AtomicReference<Throwable>();
				var worker = new Thread(() ->
				{
					try
					{
						scope.close();
					}
					catch (Throwable expected)
					{
						failure.set(expected);
					}
				}, "stage43a wrong-thread close");
				worker.start();
				try
				{
					worker.join(2000);
				}
				catch (InterruptedException interrupted)
				{
					Thread.currentThread().interrupt();
					throw new AssertionError("Wrong-thread close probe interrupted", interrupted);
				}
				check(!worker.isAlive() && failure.get() instanceof IllegalStateException, "Cross-thread close must reject before touching GL or the context stack");
			}
			check(GL11.glGetError() == GL11.GL_NO_ERROR, "Local contexts produced a GL error");
			passes++;
			PLibDatabase.LOGGER.info("STAGE 4.3a CONTEXT PASS: tick={}, pixelCases={}, nested captured frames/targets, 11 geometry overloads, sorted alpha, world sentinel, discard, LIFO/idempotence, failed flush recovery, first-person routing, native state", pending, cases);
			pending = 0;
		}
		finally
		{
			RenderSystem.outputColorTextureOverride = color;
			RenderSystem.outputDepthTextureOverride = depth;
			RenderSystem.setProjectionMatrix(projection, projectionType);
			RenderSystem.getModelViewStack().set(modelView);
			a.destroyBuffers();
			b.destroyBuffers();
			c.destroyBuffers();
			caller.close();
		}
	}

	private static void query(int expected, Runnable draw)
	{
		int id = GL15.glGenQueries();
		try
		{
			GL15.glBeginQuery(GL30.GL_PRIMITIVES_GENERATED, id);
			try
			{
				draw.run();
			}
			finally
			{
				GL15.glEndQuery(GL30.GL_PRIMITIVES_GENERATED);
			}
			check(GL15.glGetQueryObjecti(id, GL15.GL_QUERY_RESULT) == expected, "Unexpected GPU primitive count");
		}
		finally
		{
			GL15.glDeleteQueries(id);
		}
	}

	private static void bind(RenderTarget target)
	{
		RenderSystem.outputColorTextureOverride = target.getColorTextureView();
		RenderSystem.outputDepthTextureOverride = target.getDepthTextureView();
	}

	private static PInstanceHeader header(float z, int color)
	{
		return new PInstanceHeader(new Matrix4f().translation(0, 0, z), color, 0xF000F0, OverlayTexture.NO_OVERLAY);
	}

	private static void rgb(RenderTarget target, float red, float green, float blue, String name)
	{
		float[] actual = pixel(target);
		float[] expected = {red, green, blue};
		for (int channel = 0; channel < 3; channel++)
			check(Math.abs(actual[channel] - expected[channel]) < 0.016f, name + " expected=" + Arrays.toString(expected) + " actual=" + Arrays.toString(actual));
		cases++;
		PLibDatabase.LOGGER.info("STAGE 4.3a PIXEL PASS: tick={}, case={}, {}, RGBA/depth={}", pending, cases, name, Arrays.toString(actual));
	}

	/** Independent readback; no production snapshot is inspected to judge restoration. */
	private static List<Integer> nativeState()
	{
		var result = new ArrayList<Integer>();
		int[] scalar = {GL30.GL_DRAW_FRAMEBUFFER_BINDING, GL30.GL_READ_FRAMEBUFFER_BINDING, GL11.GL_DEPTH_FUNC,
				GL14.GL_BLEND_SRC_RGB, GL14.GL_BLEND_DST_RGB, GL14.GL_BLEND_SRC_ALPHA, GL14.GL_BLEND_DST_ALPHA,
				GL20.GL_CURRENT_PROGRAM, GL30.GL_VERTEX_ARRAY_BINDING, GL15.GL_ARRAY_BUFFER_BINDING,
				GL40.GL_DRAW_INDIRECT_BUFFER_BINDING, GL31.GL_UNIFORM_BUFFER_BINDING, GL31.GL_TEXTURE_BUFFER};
		for (int token : scalar)
			result.add(GL11.glGetInteger(token));
		for (int token : new int[]{GL11.GL_VIEWPORT, GL11.GL_SCISSOR_BOX})
		{
			int[] values = new int[4];
			GL11.glGetIntegerv(token, values);
			for (int value : values)
				result.add(value);
		}
		for (int token : new int[]{GL11.GL_DEPTH_TEST, GL11.GL_BLEND, GL11.GL_CULL_FACE, GL11.GL_SCISSOR_TEST})
			result.add(GL11.glIsEnabled(token) ? 1 : 0);
		result.add(GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK) ? 1 : 0);
		int active = GL11.glGetInteger(org.lwjgl.opengl.GL13.GL_ACTIVE_TEXTURE);
		for (int unit = 0; unit < 12; unit++)
		{
			GlStateManager._activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0 + unit);
			result.add(GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D));
			result.add(GL11.glGetInteger(GL31.GL_TEXTURE_BINDING_BUFFER));
			result.add(GL11.glGetInteger(GL33.GL_SAMPLER_BINDING));
		}
		GlStateManager._activeTexture(active);
		result.add(active);
		for (int index = 0; index < GL11.glGetInteger(GL31.GL_MAX_UNIFORM_BUFFER_BINDINGS); index++)
		{
			result.add(GL30.glGetIntegeri(GL31.GL_UNIFORM_BUFFER_BINDING, index));
			result.add((int)GL32.glGetInteger64i(GL31.GL_UNIFORM_BUFFER_START, index));
			result.add((int)GL32.glGetInteger64i(GL31.GL_UNIFORM_BUFFER_SIZE, index));
		}
		for (int index = 0; index < GL11.glGetInteger(GL20.GL_MAX_DRAW_BUFFERS); index++)
		{
			result.add(GL30.glIsEnabledi(GL11.GL_BLEND, index) ? 1 : 0);
			for (int token : new int[]{GL14.GL_BLEND_SRC_RGB, GL14.GL_BLEND_DST_RGB, GL14.GL_BLEND_SRC_ALPHA,
					GL14.GL_BLEND_DST_ALPHA, GL20.GL_BLEND_EQUATION_RGB, GL20.GL_BLEND_EQUATION_ALPHA})
				result.add(GL30.glGetIntegeri(token, index));
			var mask = org.lwjgl.BufferUtils.createByteBuffer(4);
			GL30.glGetBooleani_v(GL11.GL_COLOR_WRITEMASK, index, mask);
			for (int channel = 0; channel < 4; channel++)
				result.add((int)mask.get(channel));
		}
		return result;
	}

	private static void shutdown(GameShuttingDownEvent event)
	{
		check(passes == 2 && cases == 44 && pending == 0, "Both pre/post reload context suites must complete: " + passes + "/" + cases);
		PLibDatabase.LOGGER.info("STAGE 4.3a CONTEXT SHUTDOWN PASS: suites={}, pixels={}", passes, cases);
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
			throw new AssertionError("Stage 4.3a: " + message);
	}
}
