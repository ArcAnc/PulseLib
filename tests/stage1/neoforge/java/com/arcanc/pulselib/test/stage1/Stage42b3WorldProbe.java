/**
 * @author ArcAnc
 * Created at: 24.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1;

import com.arcanc.pulselib.content.model.baked.PBakedBone;
import com.arcanc.pulselib.content.model.baked.PBakedMesh;
import com.arcanc.pulselib.content.model.baked.PMeshRenderContext;
import com.arcanc.pulselib.content.registration.block.block_entity.TestBlockEntity;
import com.arcanc.pulselib.content.registration.block.block_entity.ber.renderState.TestBlockEntityRenderState;
import com.arcanc.pulselib.content.registration.entity.TestEntity;
import com.arcanc.pulselib.content.renderer.PBlockRenderer;
import com.arcanc.pulselib.content.renderer.PEntityRenderer;
import com.arcanc.pulselib.content.renderer.base.PEntityRenderState;
import com.arcanc.pulselib.content.renderer.gl.PGlRenderHost;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.data.gecko.PGeckoModelLoader;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.helpers.PLibHelper;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.nio.file.Files;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * H13 through actual owner extraction/submission and native water/glass geometry.
 * Uses the canonical WorldRenderRegressionHarness early/final pixel approach, not synthetic clears.
 */
public final class Stage42b3WorldProbe
{
	public static final PModelData MODEL = PModelData.entity(PLibDatabase.rl("stage42b3_owner"), PGeckoModelLoader.INSTANCE.id());
	private static final boolean[] SEEN = new boolean[8];
	private static final int[][] SAMPLES = new int[8][2];
	private static final float[][] EARLY = new float[8][5];
	private static boolean started;
	private static boolean advance;
	private static boolean earlyReady;
	private static boolean completed;
	private static boolean originalImproved;
	private static boolean originalHideGui;
	private static int phase;
	private static int frames;
	private static int cases;
	private static long deadline;
	private static long modeStart;
	private static boolean lightReady;
	private static CameraType originalCamera = CameraType.FIRST_PERSON;
	private static @Nullable CompletableFuture<Void> reload;

	private Stage42b3WorldProbe()
	{
	}

	public static void register()
	{
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Stage42b3WorldProbe::early);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Stage42b3WorldProbe::finish);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Stage42b3WorldProbe::shutdown);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Stage42b3WorldProbe::camera);
	}

	public static void tick()
	{
		var mc = Minecraft.getInstance();
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
		if (!started)
		{
			check(Files.isRegularFile(mc.gameDirectory.toPath().resolve(".pulselib-stage42b3")), "Missing disposable client marker");
			originalImproved = mc.options.improvedTransparency().get();
			originalCamera = mc.options.getCameraType();
			originalHideGui = mc.options.hideGui;
			mc.options.hideGui = true;
			mc.options.setCameraType(CameraType.FIRST_PERSON);
			started = true;
			deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(120);
			mode(false);
		}
		check(System.nanoTime() < deadline, "Owner/terrain world regression timed out, phase=" + phase + " frames=" + frames + " seen=" + Arrays.toString(SEEN));
		if (reload != null)
		{
			if (!reload.isDone())
				return;
			reload.join();
			reload = null;
			mode(false);
		}
		if (advance)
		{
			advance = false;
			phase++;
			if (phase == 4)
			{
				completed = true;
				mc.options.improvedTransparency().set(originalImproved);
				mc.options.setCameraType(originalCamera);
				mc.options.hideGui = originalHideGui;
				PLibDatabase.LOGGER.info("STAGE 4.2b.3 OWNER TERRAIN PASS: cases={}, actual Entity/BlockEntity, glass/water, front/behind, both Improved Transparency modes before/after reload", cases);
				mc.stop();
				return;
			}
			if (phase == 2)
				reload = mc.reloadResourcePacks();
			else
				mode(true);
		}
	}

	private static void mode(boolean improved)
	{
		var mc = Minecraft.getInstance();
		mc.options.improvedTransparency().set(improved);
		mc.levelRenderer.allChanged();
		frames = 0;
		modeStart = System.nanoTime();
		lightReady = false;
		earlyReady = false;
		Arrays.fill(SEEN, false);
	}

	private static void camera(ViewportEvent.ComputeCameraAngles event)
	{
		if (!started || completed)
			return;
		event.setYaw(0);
		event.setPitch(0);
		event.setRoll(0);
	}

	private static void early(RenderLevelStageEvent.AfterOpaqueFeatures event)
	{
		var mc = Minecraft.getInstance();
		if (!started || advance || completed || reload != null || mc.getOverlay() != null)
			return;
		int glassSky = mc.level.getBrightness(LightLayer.SKY, new BlockPos(-5, 82, 10));
		int waterSky = mc.level.getBrightness(LightLayer.SKY, new BlockPos(2, 82, 9));
		int insideWaterSky = mc.level.getBrightness(LightLayer.SKY, new BlockPos(2, 82, 11));
		if (glassSky != 15 || waterSky != 15 || insideWaterSky == 0)
		{
			frames = 0;
			return;
		}
		if (!lightReady)
		{
			PLibDatabase.LOGGER.info("STAGE 4.2b.3 LIGHT READY: phase={}, glassSky={}, waterSky={}, insideWaterSky={}", phase, glassSky, waterSky, insideWaterSky);
			lightReady = true;
		}
		for (boolean seen : SEEN)
			if (!seen)
				return;
		if (!mc.levelRenderer.isSectionCompiledAndVisible(new BlockPos(-4, 82, 11)) ||
				!mc.levelRenderer.isSectionCompiledAndVisible(new BlockPos(4, 82, 11)) || ++frames < 45 || System.nanoTime() - modeStart < TimeUnit.SECONDS.toNanos(2))
			return;
		check(Minecraft.useShaderTransparency() == (phase % 2 == 1), "Actual native mode differs from requested phase");
		var camera = event.getLevelRenderState().cameraRenderState.pos;
		var frame = new PGlRenderHost().captureFrame();
		var viewProjection = new Matrix4f(frame.projection()).mul(frame.modelView());
		RenderTarget target = mc.getMainRenderTarget();
		for (int lane = 0; lane < 8; lane++)
		{
			double x = -7.5 + lane * 2;
			double z = lane % 2 == 0 ? 9 : 13;
			Vector4f screen = viewProjection.transform(new Vector4f((float)(x - camera.x), (float)(81 - camera.y), (float)(z - camera.z), 1));
			SAMPLES[lane][0] = (int)((screen.x / screen.w + 1) * 0.5f * target.width);
			SAMPLES[lane][1] = (int)((screen.y / screen.w + 1) * 0.5f * target.height);
			check(SAMPLES[lane][0] > 0 && SAMPLES[lane][0] < target.width && SAMPLES[lane][1] > 0 && SAMPLES[lane][1] < target.height, "Projected owner outside viewport, lane=" + lane);
			EARLY[lane] = pixel(target, lane);
			float expectedDepth = (screen.z / screen.w + 1) * 0.5f;
			check(red(EARLY[lane]) && Math.abs(EARLY[lane][4] - expectedDepth) < 0.00001f,
					"Opaque owner RGB/depth must precede native terrain; lane=" + lane + " pixel=" + Arrays.toString(EARLY[lane]) + " expectedDepth=" + expectedDepth);
		}
		earlyReady = true;
	}

	private static void finish(RenderLevelStageEvent.AfterLevel event)
	{
		try
		{
			if (!earlyReady || advance)
				return;
			var mc = Minecraft.getInstance();
			for (int lane = 0; lane < 8; lane++)
			{
				float[] result = pixel(mc.getMainRenderTarget(), lane);
				boolean behind = lane % 2 == 1;
				check(behind ? result[0] < 0.98f && result[1] + result[2] > 0.015f : red(result),
						"Real terrain must tint only owners behind it; lane=" + lane + " final=" + Arrays.toString(result));
				if (!behind)
					check(Math.abs(result[4] - EARLY[lane][4]) < 0.000001f, "Terrain behind an opaque owner must preserve its depth, lane=" + lane);
				PLibDatabase.LOGGER.info("STAGE 4.2b.3 OWNER CASE PASS: phase={}, improved={}, owner={}, terrain={}, behind={}, early={}, final={}",
						phase, Minecraft.useShaderTransparency(), lane % 4 < 2 ? "Entity" : "BlockEntity", lane < 4 ? "blue-glass" : "water", behind,
						Arrays.toString(EARLY[lane]), Arrays.toString(result));
				cases++;
			}
			Screenshot.grab(mc.gameDirectory, "stage42b3-phase" + phase + ".png", mc.getMainRenderTarget(), 1,
					message -> PLibDatabase.LOGGER.info("{}", message.getString()));
			advance = true;
			earlyReady = false;
		}
		finally
		{
			Arrays.fill(SEEN, false);
		}
	}

	private static boolean red(float[] value)
	{
		return value[0] > 0.97f && value[1] + value[2] < 0.025f;
	}

	private static float[] pixel(RenderTarget target, int lane)
	{
		int previous = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
		int fbo = GL30.glGenFramebuffers();
		try
		{
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, fbo);
			GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, ((GlTexture)target.getColorTexture()).glId(), 0);
			GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, ((GlTexture)target.getDepthTexture()).glId(), 0);
			GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
			check(GL30.glCheckFramebufferStatus(GL30.GL_READ_FRAMEBUFFER) == GL30.GL_FRAMEBUFFER_COMPLETE, "Incomplete scene readback framebuffer");
			var rgb = BufferUtils.createFloatBuffer(4);
			var depth = BufferUtils.createFloatBuffer(1);
			GL11.glReadPixels(SAMPLES[lane][0], SAMPLES[lane][1], 1, 1, GL11.GL_RGBA, GL11.GL_FLOAT, rgb);
			GL11.glReadPixels(SAMPLES[lane][0], SAMPLES[lane][1], 1, 1, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, depth);
			return new float[]{rgb.get(0), rgb.get(1), rgb.get(2), rgb.get(3), depth.get(0)};
		}
		finally
		{
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previous);
			GL30.glDeleteFramebuffers(fbo);
		}
	}

	private static PMeshRenderContext material(double x, PMeshRenderContext inherited)
	{
		int lane = (int)Math.round((x + 7.5) / 2);
		check(lane >= 0 && lane < 8, "Unexpected world owner x=" + x);
		SEEN[lane] = true;
		return inherited.withColor(0xFFFF0000).withEmissive(true);
	}

	private static void shutdown(GameShuttingDownEvent event)
	{
		check(completed && cases == 32, "All actual owner/terrain phases must complete before normal shutdown");
		PLibDatabase.LOGGER.info("STAGE 4.2b.3 OWNER SHUTDOWN PASS: 32 cases and normal client stop");
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
			throw new AssertionError(message);
	}

	public static final class EntityRenderer extends PEntityRenderer<TestEntity, PEntityRenderState.LivingImpl<TestEntity>>
	{
		public EntityRenderer(EntityRendererProvider.Context context)
		{
			super(context, MODEL, PRenderTypes.RenderTypeProvider::trianglesEmissiveSolid);
		}

		@Override
		public PEntityRenderState.LivingImpl<TestEntity> createRenderState()
		{
			return PLibHelper.livingRenderState();
		}

		@Override
		protected PMeshRenderContext resolveMeshRender(TestEntity owner, PBakedBone bone, PBakedMesh mesh, PMeshRenderContext inherited, float partialTick)
		{
			return material(owner.getX(), inherited);
		}
	}

	public static final class BlockRenderer extends PBlockRenderer<TestBlockEntity, TestBlockEntityRenderState>
	{
		public BlockRenderer(BlockEntityRendererProvider.Context context)
		{
			super(MODEL, PRenderTypes.RenderTypeProvider::trianglesEmissiveSolid);
		}

		@Override
		public TestBlockEntityRenderState createRenderState()
		{
			return new TestBlockEntityRenderState();
		}

		@Override
		protected PMeshRenderContext resolveMeshRender(TestBlockEntity owner, PBakedBone bone, PBakedMesh mesh, PMeshRenderContext inherited, float partialTick)
		{
			return material(owner.getBlockPos().getX() + 0.5, inherited);
		}
	}
}
