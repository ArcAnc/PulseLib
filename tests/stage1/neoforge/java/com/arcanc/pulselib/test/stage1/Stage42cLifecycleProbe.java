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
import com.arcanc.pulselib.content.model.baked.PDeformedMeshBuffers;
import com.arcanc.pulselib.content.model.deformer.PChannelReference;
import com.arcanc.pulselib.content.model.deformer.PDeformerInstance;
import com.arcanc.pulselib.content.model.deformer.PDeformerPrepareContext;
import com.arcanc.pulselib.content.model.deformer.PDeformerStack;
import com.arcanc.pulselib.content.model.deformer.PDeformerValueSource;
import com.arcanc.pulselib.content.model.deformer.PHingeDeformer;
import com.arcanc.pulselib.content.model.deformer.PHingeDefinition;
import com.arcanc.pulselib.content.model.deformer.PMeshDeformation;
import com.arcanc.pulselib.content.model.deformer.PMeshDeformer;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.base.PBlockRenderState;
import com.arcanc.pulselib.content.renderer.base.PEntityRenderState;
import com.arcanc.pulselib.content.renderer.gl.PGlMultiDrawExecutor;
import com.arcanc.pulselib.content.renderer.gl.PGlRenderHost;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.PResourceCache;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.LightLayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.arcanc.pulselib.test.stage1.Stage42cResources.check;
import static com.arcanc.pulselib.test.stage1.Stage42cResources.executor;
import static com.arcanc.pulselib.test.stage1.Stage42cResources.map;
import static com.arcanc.pulselib.test.stage1.Stage42cResources.value;

/** Real native resize/reload/disconnect/reconnect lifecycle, with the normal world renderer and CPU/GPU deformation. */
public final class Stage42cLifecycleProbe
{
	private static final PMeshDeformer<Float> SHIFT = new PMeshDeformer<>()
	{
		@Override
		public Identifier id()
		{
			return PLibDatabase.rl("stage42c_shift");
		}

		@Override
		public MapCodec<Float> codec()
		{
			return Codec.FLOAT.fieldOf("shift");
		}

		@Override
		public void prepare(PDeformerPrepareContext context, Float distance)
		{
			context.add((position, values) -> position.add(distance, 0, 0));
		}
	};
	private static final PMeshDeformation CPU = new PMeshDeformation(PDeformerStack.compile(List.of(new PDeformerInstance<>(SHIFT, 0.25f))), PDeformerValueSource.DEFAULTS, new Object(), 0);
	private static final PMeshDeformation GPU = new PMeshDeformation(PDeformerStack.compile(List.of(new PDeformerInstance<>(PHingeDeformer.INSTANCE,
			new PHingeDefinition(new Vector3f(), new Vector3f(0, 1, 0), new Vector3f(1, 0, 0), new PChannelReference<>("", 0.4f))))), PDeformerValueSource.DEFAULTS, new Object(), 1);
	private static Stage42cResources.@Nullable Snapshot cleanupSnapshot;
	private static @Nullable CompletableFuture<Void> reload;
	private static @Nullable ClientLevel previousLevel;
	private static boolean started;
	private static boolean advance;
	private static boolean rendered;
	private static boolean completed;
	private static boolean reconnect;
	private static int phase;
	private static int frames;
	private static int cleanups;
	private static int oitCloses;
	private static int snapshotCloses;
	private static int disconnects;
	private static int targetChecks;
	private static long deadline;
	private static long phaseStart;
	private static int width = 960;
	private static int height = 640;
	private static int sampleX;
	private static int sampleY;
	private static String boundary = "initial";

	private Stage42cLifecycleProbe()
	{
	}

	public static void register()
	{
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Stage42cLifecycleProbe::workload);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Stage42cLifecycleProbe::finish);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Stage42cLifecycleProbe::camera);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Stage42cLifecycleProbe::shutdown);
	}

	public static boolean active()
	{
		return started;
	}

	public static void tick()
	{
		var mc = Minecraft.getInstance();
		if (!started)
		{
			if (mc.level == null || mc.player == null)
				return;
			check(Files.isRegularFile(mc.gameDirectory.toPath().resolve(".pulselib-stage42c")), "Disposable lifecycle client marker required");
			started = true;
			deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(360);
			mc.options.pauseOnLostFocus = false;
			mc.options.hideGui = true;
			mc.options.graphicsPreset().set(GraphicsPreset.FAST);
			begin();
		}
		check(System.nanoTime() < deadline, "GPU lifecycle timeout phase=" + phase + ", frames=" + frames);
		if (reconnect)
		{
			reconnect = false;
			Stage42cResources.empty();
			ConnectScreen.startConnecting(new TitleScreen(), mc, ServerAddress.parseString("127.0.0.1:25576"),
					new ServerData("Pulse lifecycle disposable", "127.0.0.1:25576", ServerData.Type.OTHER), false, null);
			return;
		}
		if (reload != null)
		{
			if (!reload.isDone())
				return;
			reload.join();
			reload = null;
			begin();
		}
		if (advance)
		{
			advance = false;
			phase++;
			boundary = "phase-" + phase;
			if (phase <= 3)
			{
				width = phase == 1 ? 800 : phase == 2 ? 1280 : 960;
				height = phase == 1 ? 600 : phase == 2 ? 720 : 640;
				mc.getWindow().setWindowed(width, height);
			}
			else if (phase == 4 || phase == 5)
			{
				mc.options.improvedTransparency().set(phase == 4);
				reload = mc.reloadResourcePacks();
			}
			else if (phase == 6 || phase == 8)
			{
				int before = cleanups;
				mc.disconnect(new TitleScreen(), false);
				check(cleanups > before, "Native disconnect must invoke production cleanup");
				Stage42cResources.empty();
				disconnects++;
				reconnect = true;
			}
			else if (phase == 9)
			{
				check(cleanups >= 4 && oitCloses >= 7 && snapshotCloses >= 7 && disconnects == 2, "All resource boundaries must be observed");
				completed = true;
				boundary = "shutdown";
				mc.stop();
				return;
			}
			begin();
		}
		if (mc.level != previousLevel)
		{
			previousLevel = mc.level;
			if (previousLevel != null)
				begin();
		}
		if (mc.player == null || mc.level == null)
			return;
		KeyMapping.releaseAll();
		mc.player.setPos(0, 80, -6);
		mc.player.xo = mc.player.xOld = 0;
		mc.player.yo = mc.player.yOld = 80;
		mc.player.zo = mc.player.zOld = -6;
		mc.player.setYRot(0);
		mc.player.yRotO = 0;
		mc.player.setXRot(0);
		mc.player.xRotO = 0;
		mc.player.setDeltaMovement(0, 0, 0);
	}

	private static void begin()
	{
		frames = 0;
		rendered = false;
		phaseStart = System.nanoTime();
	}

	private static void camera(ViewportEvent.ComputeCameraAngles event)
	{
		if (!started || completed)
			return;
		event.setYaw(0);
		event.setPitch(0);
		event.setRoll(0);
	}

	private static void workload(RenderLevelStageEvent.AfterOpaqueFeatures event)
	{
		var mc = Minecraft.getInstance();
		if (!started || completed || advance || reload != null || reconnect || mc.getOverlay() != null || mc.player == null)
			return;
		if (System.nanoTime() - phaseStart < TimeUnit.SECONDS.toNanos(2))
			return;
		if (mc.getMainRenderTarget().width != width || mc.getMainRenderTarget().height != height ||
				mc.level.getBrightness(LightLayer.SKY, new BlockPos(-5, 82, 10)) != 15 ||
				!mc.levelRenderer.isSectionCompiledAndVisible(new BlockPos(-4, 82, 11)))
			return;
		if (event.getLevelRenderState().entityRenderStates.stream().filter(state -> state instanceof PEntityRenderState<?>).count() != 4 ||
				event.getLevelRenderState().blockEntityRenderStates.stream().filter(state -> state instanceof PBlockRenderState<?>).count() != 4)
			return;
		var model = Stage42b4TranslucentProbe.MODEL.getModel();
		if (model == null)
			return;
		PBakedMesh mesh = model.bones().stream().flatMap(bone -> bone.meshes().stream()).findFirst().orElseThrow();
		var camera = event.getLevelRenderState().cameraRenderState.pos;
		var host = new PGlRenderHost().captureFrame();
		var header = new PInstanceHeader(new Matrix4f().translation((float)(10 - camera.x), (float)(82 - camera.y), (float)(12 - camera.z)).scale(0.4f), -1, 0xF000F0, 0);
		var type = PRenderTypes.RenderTypeProvider.trianglesSolid(PResourceCache.ATLAS_LOCATION);
		PDeformedMeshBuffers.resolve(mesh, CPU);
		int query = GL15.glGenQueries();
		try
		{
			GL15.glBeginQuery(GL30.GL_PRIMITIVES_GENERATED, query);
			try
			{
				PRenderQueue.drawImmediate(type, mesh, CPU, header, host);
				PRenderQueue.drawImmediate(type, mesh, GPU, header, host);
			}
			finally
			{
				GL15.glEndQuery(GL30.GL_PRIMITIVES_GENERATED);
			}
			check(GL15.glGetQueryObjecti(query, GL15.GL_QUERY_RESULT) == mesh.geometry().indexCount() / 3 * 5, "CPU and subdivided GPU deformation must generate actual triangles");
		}
		finally
		{
			GL15.glDeleteQueries(query);
		}
		var clip = new Matrix4f(host.projection()).mul(host.modelView()).transform(new Vector4f((float)(-7.5 - camera.x), (float)(81 - camera.y), (float)(9.5 - camera.z), 1));
		sampleX = (int)((clip.x / clip.w + 1) * width / 2);
		sampleY = (int)((clip.y / clip.w + 1) * height / 2);
		rendered = true;
	}

	public static void nativeTargets(RenderTarget destination, List<RenderTarget> targets)
	{
		if (!started || !rendered || completed)
			return;
		check(targets.size() == 5, "Five native targets must survive until OIT composition");
		for (var target : targets)
			check(!target.getColorTexture().isClosed() && !target.getDepthTexture().isClosed() && target.width == destination.width && target.height == destination.height, "Native target lifetime/dimensions mismatch");
		targetChecks++;
	}

	private static void finish(RenderLevelStageEvent.AfterLevel event)
	{
		if (!rendered)
			return;
		rendered = false;
		Object executor = executor();
		for (String name : List.of("instances", "indirect"))
			check((boolean)value(value(executor, name), "persistent") && value(value(executor, name), "persistentMapping") != null, "The real persistent streams must remain enabled");
		check(((PGlMultiDrawExecutor)executor).capabilities().submissionMode() == com.arcanc.pulselib.content.renderer.plan.PRenderCapabilities.SubmissionMode.MULTI_DRAW_INDIRECT, "Indirect instancing must remain enabled");
		check(!(boolean)value(executor, "worldDepthValid") && map(executor, "weightedBlendedOits").size() == 1, "World frame must end with one bounded OIT owner");
		for (Object oit : map(executor, "weightedBlendedOits").values())
		{
			check(!(boolean)value(oit, "frameOpen") && value(oit, "fabulousMetadataTexture") != null, "OIT must resolve through live metadata and end its frame");
			check((int)value(oit, "width") == width && (int)value(oit, "height") == height, "OIT attachments must follow native resize");
		}
		Object arena = value(executor, "geometry");
		check(map(arena, "geometrySlices").size() <= 12 && map(arena, "dynamicPages").size() == 1, "Geometry identity caches must remain bounded");
		for (Object slots : map(arena, "dynamicPages").values())
			check(((java.util.Map<?, ?>)slots).size() <= 9, "Dynamic geometry must stay inside the nine frame slots");
		Object registry = value(executor, "resources");
		check(map(registry, "meshResources").size() <= 16 && map(registry, "pipelineResources").size() <= 8, "Registry handles must stay bounded");
		float[] pixel = pixel(Minecraft.getInstance().getMainRenderTarget(), sampleX, sampleY);
		check(pixel[0] > 0.55f && pixel[0] > pixel[1] + 0.3f, "World translucent owner must recover after each boundary; phase=" + phase + ", pixel=" + java.util.Arrays.toString(pixel));
		if (++frames < 90 || System.nanoTime() - phaseStart < TimeUnit.SECONDS.toNanos(2))
			return;
		PLibDatabase.LOGGER.info("STAGE 4.2c LIVE PASS: phase={}, frames={}, size={}x{}, improved={}, {}, geometry={}, dynamic={}, handles={}, pixel={}", phase, frames, width, height,
				Minecraft.useShaderTransparency(), Stage42cResources.all(), map(arena, "geometrySlices").size(), map(arena, "dynamicPages").size(), map(registry, "meshResources").size(), java.util.Arrays.toString(pixel));
		if (phase == 7)
			Screenshot.grab(Minecraft.getInstance().gameDirectory, "stage42c-reconnected.png", Minecraft.getInstance().getMainRenderTarget(), 1,
					message -> PLibDatabase.LOGGER.info("{}", message.getString()));
		advance = true;
	}

	private static float[] pixel(RenderTarget target, int x, int y)
	{
		int previous = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
		int fbo = GL30.glGenFramebuffers();
		try
		{
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, fbo);
			GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, ((GlTexture)target.getColorTexture()).glId(), 0);
			GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
			var rgba = BufferUtils.createFloatBuffer(4);
			GL11.glReadPixels(x, y, 1, 1, GL11.GL_RGBA, GL11.GL_FLOAT, rgba);
			return new float[]{rgba.get(0), rgba.get(1), rgba.get(2), rgba.get(3)};
		}
		finally
		{
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previous);
			GL30.glDeleteFramebuffers(fbo);
		}
	}

	public static void beforeCleanup()
	{
		if (started)
			cleanupSnapshot = Stage42cResources.all();
	}

	public static void afterCleanup()
	{
		if (cleanupSnapshot == null)
			return;
		cleanupSnapshot.deleted(boundary);
		Stage42cResources.empty();
		if (cleanupSnapshot.populated())
		{
			cleanups++;
			PLibDatabase.LOGGER.info("STAGE 4.2c CLEANUP PASS: {}, deleted {}, maps/mappings/caches empty", boundary, cleanupSnapshot);
		}
		cleanupSnapshot = null;
	}

	public static void oitClosed(Stage42cResources.Snapshot snapshot)
	{
		snapshot.deleted("OIT closeBuffers");
		if (snapshot.populated())
		{
			oitCloses++;
			PLibDatabase.LOGGER.info("STAGE 4.2c OIT CLOSE PASS: {}, deleted {}", boundary, snapshot);
		}
	}

	public static boolean ownsSnapshot(RenderTarget target)
	{
		return started && target == value(executor(), "worldOpaqueDepth");
	}

	public static void snapshotClosed(Stage42cResources.Snapshot snapshot)
	{
		snapshot.deleted("Opaque snapshot destroyBuffers");
		if (snapshot.populated())
		{
			snapshotCloses++;
			PLibDatabase.LOGGER.info("STAGE 4.2c DEPTH CLOSE PASS: {}, deleted {}", boundary, snapshot);
		}
	}

	private static void shutdown(GameShuttingDownEvent event)
	{
		check(completed && cleanups >= 5 && disconnects == 2 && targetChecks > 700, "All lifecycle phases and normal shutdown cleanup must complete");
		Stage42cResources.empty();
		PLibDatabase.LOGGER.info("STAGE 4.2c SHUTDOWN PASS: cleanup={}, oitClose={}, snapshotClose={}, disconnects={}, nativeTargetFrames={}", cleanups, oitCloses, snapshotCloses, disconnects, targetChecks);
	}
}
