/**
 * @author ArcAnc
 * Created at: 17.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test;

import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PRenderTypes;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.CameraType;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;
import java.util.concurrent.CompletableFuture;

/** Uses real water/glass terrain and real loader stages in the marked disposable smoke save. */
public final class WorldRenderRegressionHarness
{
	private static final PGeometryData QUAD = H14Harness.quad();
	private static final ResourceLocation WHITE = DemoContent.id("world_render_white");
	private static boolean started;
	private static boolean completed;
	private static long deadline;
	private static int scenario;
	private static int frames;
	private static int earlyChecks;
	private static int sampleX;
	private static int sampleY;
	private static float expectedDepth;
	private static @Nullable CompletableFuture<Void> setup;
	private static @Nullable GraphicsStatus originalGraphics;
	private static @Nullable CameraType originalCamera;
	private static boolean originalHideGui;

	private WorldRenderRegressionHarness()
	{
	}

	public static boolean frame()
	{
		if (completed)
		{
			return true;
		}
		Minecraft mc = Minecraft.getInstance();
		check(!started || System.nanoTime() < deadline, "World regression timed out");
		if (!started)
		{
			DisposableTestWorlds.requireCurrentWorld();
			originalGraphics = mc.options.graphicsMode().get();
			originalCamera = mc.options.getCameraType();
			originalHideGui = mc.options.hideGui;
			mc.options.hideGui = true;
			NativeImage white = new NativeImage(1, 1, false);
			white.setPixelRGBA(0, 0, -1);
			mc.getTextureManager().register(WHITE, new DynamicTexture(white));
			started = true;
			deadline = System.nanoTime() + java.util.concurrent.TimeUnit.MINUTES.toNanos(1);
		}
		if (setup == null)
		{
			prepare();
			return false;
		}
		if (!setup.isDone())
		{
			return false;
		}
		setup.join();
		if (frames < 24 || !mc.levelRenderer.isSectionCompiled(new BlockPos(11, 143, 17)))
		{
			return false;
		}
		float[] pixel = pixel(false);
		boolean behind = scenario % 2 == 1;
		if (behind)
		{
			check(pixel[0] < 0.98f && pixel[1] + pixel[2] > 0.015f,
					"Real translucent terrain must tint the opaque entity behind it: " + java.util.Arrays.toString(pixel));
		}
		else
		{
			check(pixel[0] > 0.97f && pixel[1] + pixel[2] < 0.025f,
					"Opaque entity before translucent terrain stays red: " + java.util.Arrays.toString(pixel));
		}
		PLibDatabase.LOGGER.info("H13 WORLD CASE PASS {} {} {}: RGB {},{},{}; {} early depth checks",
				mc.options.graphicsMode().get(), scenario % 4 < 2 ? "water" : "blue glass", behind ? "behind" : "front",
				pixel[0], pixel[1], pixel[2], earlyChecks);
		scenario++;
		if (scenario < 12)
		{
			setup = null;
			return false;
		}
		completed = true;
		mc.options.graphicsMode().set(originalGraphics);
		mc.options.setCameraType(originalCamera);
		mc.options.hideGui = originalHideGui;
		mc.levelRenderer.graphicsChanged();
		PLibDatabase.LOGGER.info("H13 WORLD PIXEL PASS: 12 real water/glass front/behind cases, Fast/Fancy/Fabulous, early RGB/depth and final scene pixels");
		return true;
	}

	private static void prepare()
	{
		Minecraft mc = Minecraft.getInstance();
		DisposableTestWorlds.requireCurrentWorld();
		mc.options.graphicsMode().set(GraphicsStatus.values()[scenario / 4]);
		mc.levelRenderer.graphicsChanged();
		frames = 0;
		earlyChecks = 0;
		boolean water = scenario % 4 < 2;
		setup = CompletableFuture.runAsync(() ->
		{
			var level = mc.getSingleplayerServer().overworld();
			for (int x = 7; x <= 15; x++)
			{
				for (int y = 140; y <= 146; y++)
				{
					for (int z = 16; z <= 18; z++)
					{
						boolean border = x == 7 || x == 15 || y == 140 || y == 146 || z != 17;
						var block = water ? (border ? Blocks.GLASS : Blocks.WATER) : (z == 17 ? Blocks.BLUE_STAINED_GLASS : Blocks.AIR);
						level.setBlock(new BlockPos(x, y, z), block.defaultBlockState(), 2);
					}
				}
			}
		}, mc.getSingleplayerServer());
	}

	public static void lockCamera()
	{
		Minecraft mc = Minecraft.getInstance();
		if (!started || completed || mc.player == null)
		{
			return;
		}
		KeyMapping.releaseAll();
		mc.options.setCameraType(CameraType.FIRST_PERSON);
		mc.player.setPos(10.5, 140, 10.5);
		mc.player.xo = mc.player.xOld = 10.5;
		mc.player.yo = mc.player.yOld = 140;
		mc.player.zo = mc.player.zOld = 10.5;
		mc.player.setYRot(0);
		mc.player.yRotO = 0;
		mc.player.setXRot(0);
		mc.player.xRotO = 0;
		mc.player.setDeltaMovement(0, 0, 0);
	}

	public static void submit()
	{
		if (!started || completed || setup == null || !setup.isDone())
		{
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		var camera = mc.gameRenderer.getMainCamera().getPosition();
		double z = scenario % 2 == 0 ? 15 : (scenario % 4 < 2 ? 17.5 : 19);
		Matrix4f transform = new Matrix4f().translation((float)(11.5 - camera.x), (float)(143 - camera.y), (float)(z - camera.z));
		Matrix4f vp = new Matrix4f(RenderSystem.getProjectionMatrix()).mul(RenderSystem.getModelViewMatrix());
		Vector4f screen = vp.transform(new Vector4f(transform.m30(), transform.m31(), transform.m32(), 1));
		sampleX = (int)((screen.x / screen.w + 1) * 0.5f * mc.getMainRenderTarget().width);
		sampleY = (int)((screen.y / screen.w + 1) * 0.5f * mc.getMainRenderTarget().height);
		expectedDepth = (screen.z / screen.w + 1) * 0.5f;
		PRenderQueue.submitEntityMesh(PRenderTypes.RenderTypeProvider.trianglesSolidEmissive(WHITE), QUAD,
				new PInstanceHeader(transform.scale(0.5f), 0xFFFF0000, 0xF000F0, OverlayTexture.NO_OVERLAY));
		frames++;
	}

	public static void earlyDepth()
	{
		if (!started || completed || frames == 0 || setup == null || !setup.isDone())
		{
			return;
		}
		float[] rgb = pixel(false);
		float depth = pixel(true)[0];
		check(rgb[0] > 0.97f && rgb[1] + rgb[2] < 0.025f, "Opaque entity RGB must precede terrain transparency");
		check(Math.abs(depth - expectedDepth) < 0.00001f, "Opaque entity depth must precede terrain transparency");
		earlyChecks++;
	}

	private static float[] pixel(boolean depth)
	{
		int previous = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
		GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, Minecraft.getInstance().getMainRenderTarget().frameBufferId);
		FloatBuffer pixel = BufferUtils.createFloatBuffer(4);
		GL11.glReadPixels(sampleX, sampleY, 1, 1, depth ? GL11.GL_DEPTH_COMPONENT : GL11.GL_RGBA, GL11.GL_FLOAT, pixel);
		float[] result = new float[depth ? 1 : 4];
		pixel.get(result);
		GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previous);
		return result;
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
		{
			throw new AssertionError("H13 world case " + scenario + ": " + message);
		}
	}
}
