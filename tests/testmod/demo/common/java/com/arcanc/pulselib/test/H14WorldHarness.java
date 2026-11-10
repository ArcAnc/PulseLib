/**
 * @author ArcAnc
 * Created at: 13.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test;

import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PRenderTypes;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import org.jspecify.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Native world slice of canonical H14; the completed offscreen suite is not rerun. */
public final class H14WorldHarness
{
	private static int phase;
	private static int capturedCases;
	private static @Nullable CompletableFuture<Void> worldSetup;
	private static @Nullable WorldGraphics originalGraphics;
	private static @Nullable CameraType originalCamera;
	private static boolean originalHideGui;
	private static int worldCase = -1;
	private static int worldFrames;
	private static int fabulousWorldFrames;
	private static int worldWarmup;
	private static final int[][] worldSamples = new int[2][2];
	private static final float[][] worldColors = new float[12][];
	private static final float[][] worldOpaque = new float[2][];
	private static float worldSurfaceDepth;
	private static float[] worldExpected = new float[8];

	private H14WorldHarness()
	{
	}

	public static boolean frame() throws IOException
	{
		return worldChecks();
	}

	public static void reloaded()
	{
		phase = 1;
		worldCase = 9;
		worldFrames = 0;
		worldWarmup = Minecraft.getInstance().player.tickCount + 10;
	}

	public static int cases()
	{
		return capturedCases;
	}

	public static int hooks()
	{
		return fabulousWorldFrames;
	}

	/** Keeps all world captures at the same position and orientation. */
	public static void lockWorldCamera()
	{
		Minecraft mc = Minecraft.getInstance();
		if (WorldRegressionRun.h14Active() && worldCase >= 0 && worldCase < 12 && mc.player != null)
		{
			KeyMapping.releaseAll();
			mc.player.setPos(10.5, 140.0, 10.5);
			mc.player.xo = mc.player.xOld = 10.5;
			mc.player.yo = mc.player.yOld = 140.0;
			mc.player.zo = mc.player.zOld = 10.5;
			mc.player.setYRot(0f);
			mc.player.setXRot(0f);
			mc.player.yRotO = mc.player.xRotO = 0f;
			mc.player.setDeltaMovement(0.0, 0.0, 0.0);
		}
	}

	/** Called before the regular world entity queue is flushed. */
	public static void queueWorldSurface()
	{
		if (!WorldRegressionRun.h14Active() || worldCase < 0 || worldCase >= 12)
			return;
		Minecraft mc = Minecraft.getInstance();
		var camera = mc.gameRenderer.getMainCamera().position();
		Matrix4f viewProjection = WorldRegressionRun.viewProjection();
		for (int side = 0; side < 2; side++)
		{
			Vector4f point = viewProjection.transform(new Vector4f((float)((side == 0 ? 9.5 : 12.5) - camera.x),
					(float)(142.0 - camera.y), (float)(17.0 - camera.z), 1f));
			worldSamples[side][0] = (int)((point.x / point.w + 1f) * 0.5f * mc.getMainRenderTarget().width);
			worldSamples[side][1] = (int)((point.y / point.w + 1f) * 0.5f * mc.getMainRenderTarget().height);
			worldOpaque[side] = worldPixel(mc.getMainRenderTarget(), side);
			worldOpaque[side][3] = 1f;
		}
		int surface = worldCase % 3;
		Vector4f depth = viewProjection.transform(new Vector4f(0f, 0f, (float)((surface == 1 ? 15.0 : 19.0) - camera.z), 1f));
		worldSurfaceDepth = (depth.z / depth.w + 1f) * 0.5f;
		if (surface != 0)
			PRenderQueue.submitEntityMesh(PRenderTypes.RenderTypeProvider.trianglesEmissiveTranslucent(H14OffscreenHarness.WHITE), H14OffscreenHarness.atlasQuad(),
					new PInstanceHeader(new Matrix4f().translation((float)(11.0 - camera.x), (float)(142.0 - camera.y),
							(float)((surface == 1 ? 15.0 : 19.0) - camera.z)).scale(4f, 2f, 1f),
							surface == 1 ? 0x80FF0000 : 0x800000FF, 0xF000F0, OverlayTexture.NO_OVERLAY));
		worldFrames++;
	}

	/** A probe on the real common hook, independent of the offscreen test calls. */
	public static void fabulousWorldComposite(RenderTarget destination, List<RenderTarget> targets)
	{
		Minecraft mc = Minecraft.getInstance();
		if (WorldRegressionRun.h14Active() && worldCase >= 0 && worldCase < 12 &&
				mc.level != null && destination == mc.getMainRenderTarget())
		{
			worldExpected = new float[8];
			for (int side = 0; side < 2; side++)
			{
				List<ReferenceLayer> ordered = new ArrayList<>();
				ordered.add(new ReferenceLayer(worldOpaque[side][4], worldOpaque[side]));
				for (RenderTarget target : targets)
				{
					float[] pixel = worldPixel(target, side);
					if (pixel[3] > 0f)
					{
						for (int component = 0; component < 3; component++)
							pixel[component] /= pixel[3];
						ordered.add(new ReferenceLayer(pixel[4], pixel));
					}
				}
				if (worldCase % 3 != 0 && worldSurfaceDepth <= worldOpaque[side][4])
					ordered.add(new ReferenceLayer(worldSurfaceDepth, H14OffscreenHarness.color(worldCase % 3 == 1 ? 0x80FF0000 : 0x800000FF)));
				ordered.sort(Comparator.comparingDouble(ReferenceLayer::depth).reversed());
				for (ReferenceLayer layer : ordered)
					for (int component = 0; component < 3; component++)
						worldExpected[side * 4 + component] = worldExpected[side * 4 + component] * (1f - layer.color()[3]) +
								layer.color()[component] * layer.color()[3];
			}
			fabulousWorldFrames++;
		}
	}

	public static void fancyWorldComposite(RenderTarget destination)
	{
		if (!WorldRegressionRun.h14Active() || worldCase < 0 || worldCase >= 12 || Minecraft.useShaderTransparency())
			return;
		worldExpected = new float[8];
		for (int side = 0; side < 2; side++)
		{
			float[] pixel = worldPixel(destination, side);
			float[] source = H14OffscreenHarness.color(worldCase % 3 == 1 ? 0x80FF0000 : 0x800000FF);
			boolean visible = worldCase % 3 != 0 && worldSurfaceDepth <= worldOpaque[side][4];
			for (int component = 0; component < 3; component++)
				worldExpected[side * 4 + component] = visible ? pixel[component] * (1f - source[3]) + source[component] * source[3] : pixel[component];
		}
	}

	private static float[] worldPixel(RenderTarget target, int side)
	{
		return WorldRegressionRun.pixel(target, worldSamples[side][0], worldSamples[side][1]);
	}

	private static boolean worldChecks() throws IOException
	{
		Minecraft mc = Minecraft.getInstance();
		if (worldSetup == null)
		{
			WorldRegressionRun.requireWorld();
			check(mc.getSingleplayerServer() != null && mc.player != null, "World test needs an integrated server and player");
			originalGraphics = WorldGraphics.current();
			originalCamera = mc.options.getCameraType();
			originalHideGui = mc.options.hideGui;
			WorldGraphics.FANCY.apply();
			mc.options.setCameraType(CameraType.FIRST_PERSON);
			mc.options.hideGui = true;
			mc.levelRenderer.allChanged();
			var server = mc.getSingleplayerServer();
			check(server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().getFileName().toString().equals(DemoChecks.WORLD),
					"H14 scene preparation is allowed only in the disposable pulselib-smoke save");
			var uuid = mc.player.getUUID();
			worldSetup = CompletableFuture.runAsync(() ->
			{
				var player = server.getPlayerList().getPlayer(uuid);
				check(player != null, "Server player must exist");
				var level = player.level();
				// This is deliberately restricted to the disposable pulselib-smoke save.
				for (int x = 5; x <= 16; x++)
					for (int y = 138; y <= 146; y++)
						for (int z = 11; z <= 21; z++)
							level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
				for (int x = 8; x <= 10; x++)
					for (int y = 140; y <= 144; y++)
						level.setBlockAndUpdate(new BlockPos(x, y, 17), Blocks.STONE.defaultBlockState());
				server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set 6000");
				WorldRegressionRun.weather(level, false);
				level.setRainLevel(0f);
				level.setThunderLevel(0f);
				player.setGameMode(GameType.SPECTATOR);
				player.teleportTo(level, 10.5, 140.0, 10.5, java.util.Set.of(), 0f, 0f, true);
			}, server);
			return false;
		}
		if (worldCase < 0)
		{
			if (!worldSetup.isDone())
				return false;
			worldSetup.join();
			mc.level.setRainLevel(0f);
			mc.level.setThunderLevel(0f);
			worldCase = 0;
			worldWarmup = mc.player.tickCount + 50;
			return false;
		}
		if (worldCase >= 12)
			return true;
		if (mc.player.tickCount < worldWarmup || worldFrames < 8)
			return false;
		String name = "world-" + (phase == 1 ? "reloaded-resized-" : "") + (worldCase >= 6 ? "rain-" : "") +
				(worldCase % 6 < 3 ? "fancy" : "fabulous") + "-" +
				new String[]{"control", "front", "behind"}[worldCase % 3];
		float[] result = new float[8];
		for (int side = 0; side < 2; side++)
		{
			int x = worldSamples[side][0];
			int y = worldSamples[side][1];
			check(x >= 0 && x < mc.getMainRenderTarget().width && y >= 0 && y < mc.getMainRenderTarget().height,
					"World sample must project into the window");
			System.arraycopy(WorldRegressionRun.pixel(mc.getMainRenderTarget(), x, y), 0, result, side * 4, 4);
		}

		worldColors[worldCase] = result;
		H14OffscreenHarness.assertRgb(result, worldExpected, name + " same-frame ordered-alpha reference");
		check(GL11.glGetError() == GL11.GL_NO_ERROR, name + " OpenGL error");
		H14OffscreenHarness.save(mc.getMainRenderTarget(), name);
		PLibDatabase.LOGGER.info("H14 WORLD {}: stone RGB {},{},{}; sky RGB {},{},{}", name,
				result[0], result[1], result[2], result[4], result[5], result[6]);
		capturedCases++;
		worldCase++;
		worldFrames = 0;
		if (worldCase == 3 || worldCase == 9)
		{
			WorldGraphics.FABULOUS.apply();
			mc.levelRenderer.allChanged();
		}
		if (worldCase == 6)
		{
			check(fabulousWorldFrames > 0, "Real Fabulous hook must execute in LevelRenderer.renderLevel");
			for (int mode = 0; mode < 2; mode++)
			{
				float[] control = worldColors[mode * 3];
				float[] front = worldColors[mode * 3 + 1];
				float[] behind = worldColors[mode * 3 + 2];
				for (int side = 0; side < 2; side++)
					for (int component = 0; component < 3; component++)
					{
						int index = side * 4 + component;
						float frontExpected = control[index] * (127f / 255f) + (component == 0 ? 128f / 255f : 0f);
						float behindExpected = side == 0 ? control[index] :
								control[index] * (127f / 255f) + (component == 2 ? 128f / 255f : 0f);
						check(Math.abs(front[index] - frontExpected) < 0.04f, name + " world front blending");
						check(Math.abs(behind[index] - behindExpected) < 0.04f, name + " world occlusion / sky blending");
					}
			}
			H14OffscreenHarness.assertRgb(worldColors[1], worldColors[4], "Real world Fancy/Fabulous front parity");
			H14OffscreenHarness.assertRgb(worldColors[2], worldColors[5], "Real world Fancy/Fabulous occlusion parity");
			PLibDatabase.LOGGER.info("H14 WORLD PIXEL PASS: stone wall, front/behind emissive surfaces, sky; {} common Fabulous hooks", fabulousWorldFrames);
			WorldGraphics.FANCY.apply();
			mc.levelRenderer.allChanged();
			mc.getSingleplayerServer().execute(() ->
			{
				var level = mc.getSingleplayerServer().overworld();
				WorldRegressionRun.weather(level, true);
				level.setRainLevel(1f);
			});
			mc.level.setRainLevel(1f);
			worldWarmup = mc.player.tickCount + 100;
		}
		if (worldCase == 12)
		{
			check(mc.level.getRainLevel(1f) > 0.9f, "Live rain must be active");
			PLibDatabase.LOGGER.info("H14 WEATHER PIXEL PASS: live rain and clouds, Fancy/Fabulous, same-frame ordered-alpha reference");
		}
		return false;
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
			throw new AssertionError("H14: " + message);
	}

	private record ReferenceLayer(float depth, float[] color)
	{
	}
}
