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
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PModelCache;
import com.arcanc.pulselib.util.PRenderTypes;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.CameraType;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL40;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Opt-in pixel regression using the actual Minecraft 1.21.1 transparency chain.
 * The left half is an opaque occluder at depth 0.5; the right half is sky at 1.
 * Vanilla target colors are premultiplied, as in transparency.fsh.
 */
public final class H14Harness
{
	private static final ResourceLocation WHITE = ResourceLocation.fromNamespaceAndPath("pulselibtest", "h14_white");
	private static final String[] TARGETS = {"translucent", "itemEntity", "particles", "weather", "clouds"};
	private static boolean completed;
	private static int phase;
	private static int cases;
	private static int originalWidth;
	private static int originalHeight;
	private static int framebufferWidth;
	private static @Nullable CompletableFuture<Void> reloading;
	private static @Nullable CompletableFuture<Void> worldSetup;
	private static @Nullable GraphicsStatus originalGraphics;
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

	private H14Harness()
	{
	}

	public static void frame()
	{
		Minecraft mc = Minecraft.getInstance();
		if (completed || mc.getOverlay() != null || PModelCache.getModels() == null)
		{
			return;
		}
		if (Boolean.getBoolean("pulselib.test.world") && mc.level == null)
		{
			return;
		}
		if (reloading != null)
		{
			if (!reloading.isDone())
			{
				return;
			}
			reloading.join();
			reloading = null;
			if (Boolean.getBoolean("pulselib.test.world"))
			{
				// Repeat actual Fabulous world pixels with the rebuilt targets and shaders.
				worldCase = 9;
				worldFrames = 0;
				worldWarmup = mc.player.tickCount + 10;
			}
		}
		try
		{
			NativeImage white = new NativeImage(1, 1, false);
			white.setPixelRGBA(0, 0, -1);
			mc.getTextureManager().register(WHITE, new DynamicTexture(white));
			if (Boolean.getBoolean("pulselib.test.world") && !worldChecks())
			{
				return;
			}
			if (Boolean.getBoolean("pulselib.test.oitBaseline"))
			{
				baseline();
			}
			else
			{
				suite(phase == 0 ? 64 : 96, phase == 0 ? 32 : 48, phase == 0 ? "initial" : "reloaded-resized");
				if (phase == 0)
				{
					IntBuffer width = BufferUtils.createIntBuffer(1);
					IntBuffer height = BufferUtils.createIntBuffer(1);
					GLFW.glfwGetWindowSize(mc.getWindow().getWindow(), width, height);
					originalWidth = width.get(0);
					originalHeight = height.get(0);
					framebufferWidth = mc.getMainRenderTarget().width;
					GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), originalWidth + 32, originalHeight + 16);
					phase = 1;
					// Deliberately opt-in and bounded: one F3+T-equivalent reload, no world switching.
					reloading = mc.reloadResourcePacks();
					return;
				}
				check(mc.getMainRenderTarget().width != framebufferWidth, "Actual window framebuffer must resize");
				PLibDatabase.LOGGER.info("H14 PIXEL PASS: {} cases, vanilla Fabulous chain, Fancy reference, GL state, resource reload and window resize", cases);
			}
			completed = true;
			if (originalGraphics != null)
			{
				mc.options.graphicsMode().set(originalGraphics);
				mc.options.setCameraType(originalCamera);
				mc.options.hideGui = originalHideGui;
				mc.levelRenderer.graphicsChanged();
			}
			mc.stop();
		}
		catch (IOException exception)
		{
			throw new IllegalStateException("Cannot run H14 pixel regression", exception);
		}
		finally
		{
			PRenderQueue.cleanup();
			mc.getMainRenderTarget().bindWrite(true);
			if (completed && originalWidth > 0)
			{
				GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), originalWidth, originalHeight);
			}
		}
	}

	/** Keeps all world captures at the same position and orientation. */
	public static void lockWorldCamera()
	{
		Minecraft mc = Minecraft.getInstance();
		if (Boolean.getBoolean("pulselib.test.oit") && worldCase >= 0 && worldCase < 12 && mc.player != null)
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
		if (!Boolean.getBoolean("pulselib.test.oit") || worldCase < 0 || worldCase >= 12)
		{
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		var camera = mc.gameRenderer.getMainCamera().getPosition();
		Matrix4f viewProjection = new Matrix4f(RenderSystem.getProjectionMatrix()).mul(RenderSystem.getModelViewMatrix());
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
		{
			PRenderQueue.submitEntityMesh(PRenderTypes.RenderTypeProvider.trianglesTranslucentEmissive(WHITE), quad(),
					new PInstanceHeader(new Matrix4f().translation((float)(11.0 - camera.x), (float)(142.0 - camera.y),
							(float)((surface == 1 ? 15.0 : 19.0) - camera.z)).scale(4f, 2f, 1f),
							surface == 1 ? 0x80FF0000 : 0x800000FF, 0xF000F0, OverlayTexture.NO_OVERLAY));
		}
		worldFrames++;
	}

	/** A probe on the real common hook, independent of the offscreen test calls. */
	public static void fabulousWorldComposite(RenderTarget destination, List<RenderTarget> targets)
	{
		Minecraft mc = Minecraft.getInstance();
		if (Boolean.getBoolean("pulselib.test.oit") && worldCase >= 0 && worldCase < 12 &&
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
						{
							pixel[component] /= pixel[3];
						}
						ordered.add(new ReferenceLayer(pixel[4], pixel));
					}
				}
				if (worldCase % 3 != 0 && worldSurfaceDepth <= worldOpaque[side][4])
				{
					ordered.add(new ReferenceLayer(worldSurfaceDepth, color(worldCase % 3 == 1 ? 0x80FF0000 : 0x800000FF)));
				}
				ordered.sort(Comparator.comparingDouble(ReferenceLayer::depth).reversed());
				for (ReferenceLayer layer : ordered)
				{
					for (int component = 0; component < 3; component++)
					{
						worldExpected[side * 4 + component] = worldExpected[side * 4 + component] * (1f - layer.color()[3]) +
								layer.color()[component] * layer.color()[3];
					}
				}
			}
			fabulousWorldFrames++;
		}
	}

	public static void fancyWorldComposite(RenderTarget destination)
	{
		if (!Boolean.getBoolean("pulselib.test.oit") || worldCase < 0 || worldCase >= 12 || Minecraft.useShaderTransparency())
		{
			return;
		}
		worldExpected = new float[8];
		for (int side = 0; side < 2; side++)
		{
			float[] pixel = worldPixel(destination, side);
			float[] source = color(worldCase % 3 == 1 ? 0x80FF0000 : 0x800000FF);
			boolean visible = worldCase % 3 != 0 && worldSurfaceDepth <= worldOpaque[side][4];
			for (int component = 0; component < 3; component++)
			{
				worldExpected[side * 4 + component] = visible ? pixel[component] * (1f - source[3]) + source[component] * source[3] : pixel[component];
			}
		}
	}

	private static float[] worldPixel(RenderTarget target, int side)
	{
		int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
		try
		{
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, target.frameBufferId);
			FloatBuffer value = BufferUtils.createFloatBuffer(4);
			float[] result = new float[5];
			GL11.glReadPixels(worldSamples[side][0], worldSamples[side][1], 1, 1, GL11.GL_RGBA, GL11.GL_FLOAT, value);
			value.get(result, 0, 4);
			value.clear();
			GL11.glReadPixels(worldSamples[side][0], worldSamples[side][1], 1, 1, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, value);
			result[4] = value.get(0);
			return result;
		}
		finally
		{
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
		}
	}

	private static boolean worldChecks() throws IOException
	{
		Minecraft mc = Minecraft.getInstance();
		if (worldSetup == null)
		{
			DisposableTestWorlds.requireCurrentWorld();
			check(mc.getSingleplayerServer() != null && mc.player != null, "World test needs an integrated server and player");
			originalGraphics = mc.options.graphicsMode().get();
			originalCamera = mc.options.getCameraType();
			originalHideGui = mc.options.hideGui;
			mc.options.graphicsMode().set(GraphicsStatus.FANCY);
			mc.options.setCameraType(CameraType.FIRST_PERSON);
			mc.options.hideGui = true;
			mc.levelRenderer.graphicsChanged();
			var server = mc.getSingleplayerServer();
			check(server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().getFileName().toString().equals("pulselib-smoke"),
					"H14 scene preparation is allowed only in the disposable pulselib-smoke save");
			var uuid = mc.player.getUUID();
			worldSetup = CompletableFuture.runAsync(() ->
			{
				var player = server.getPlayerList().getPlayer(uuid);
				check(player != null, "Server player must exist");
				var level = player.serverLevel();
				// This is deliberately restricted to the disposable pulselib-smoke save.
				for (int x = 5; x <= 16; x++)
				{
					for (int y = 138; y <= 146; y++)
					{
						for (int z = 11; z <= 21; z++)
						{
							level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
						}
					}
				}
				for (int x = 8; x <= 10; x++)
				{
					for (int y = 140; y <= 144; y++)
					{
						level.setBlockAndUpdate(new BlockPos(x, y, 17), Blocks.STONE.defaultBlockState());
					}
				}
				level.setDayTime(6000L);
				level.setWeatherParameters(6000, 0, false, false);
				level.setRainLevel(0f);
				level.setThunderLevel(0f);
				player.setGameMode(GameType.SPECTATOR);
				player.teleportTo(level, 10.5, 140.0, 10.5, 0f, 0f);
			}, server);
			return false;
		}
		if (worldCase < 0)
		{
			if (!worldSetup.isDone())
			{
				return false;
			}
			worldSetup.join();
			mc.level.setRainLevel(0f);
			mc.level.setThunderLevel(0f);
			worldCase = 0;
			worldWarmup = mc.player.tickCount + 50;
			return false;
		}
		if (worldCase >= 12)
		{
			return true;
		}
		if (mc.player.tickCount < worldWarmup || worldFrames < 8)
		{
			return false;
		}
		String name = "world-" + (phase == 1 ? "reloaded-resized-" : "") + (worldCase >= 6 ? "rain-" : "") +
				(worldCase % 6 < 3 ? "fancy" : "fabulous") + "-" +
				new String[]{"control", "front", "behind"}[worldCase % 3];
		float[] result = new float[8];
		FloatBuffer pixel = BufferUtils.createFloatBuffer(4);
		GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, mc.getMainRenderTarget().frameBufferId);
		for (int side = 0; side < 2; side++)
		{
			int x = worldSamples[side][0];
			int y = worldSamples[side][1];
			check(x >= 0 && x < mc.getMainRenderTarget().width && y >= 0 && y < mc.getMainRenderTarget().height,
					"World sample must project into the window");
			pixel.clear();
			GL11.glReadPixels(x, y, 1, 1, GL11.GL_RGBA, GL11.GL_FLOAT, pixel);
			pixel.get(result, side * 4, 4);
		}
		worldColors[worldCase] = result;
		assertRgb(result, worldExpected, name + " same-frame ordered-alpha reference");
		check(GL11.glGetError() == GL11.GL_NO_ERROR, name + " OpenGL error");
		save(mc.getMainRenderTarget(), name);
		PLibDatabase.LOGGER.info("H14 WORLD {}: stone RGB {},{},{}; sky RGB {},{},{}", name,
				result[0], result[1], result[2], result[4], result[5], result[6]);
		worldCase++;
		worldFrames = 0;
		if (worldCase == 3 || worldCase == 9)
		{
			mc.options.graphicsMode().set(GraphicsStatus.FABULOUS);
			mc.levelRenderer.graphicsChanged();
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
				{
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
			}
			assertRgb(worldColors[1], worldColors[4], "Real world Fancy/Fabulous front parity");
			assertRgb(worldColors[2], worldColors[5], "Real world Fancy/Fabulous occlusion parity");
			PLibDatabase.LOGGER.info("H14 WORLD PIXEL PASS: stone wall, front/behind emissive surfaces, sky; {} common Fabulous hooks", fabulousWorldFrames);
			mc.options.graphicsMode().set(GraphicsStatus.FANCY);
			mc.levelRenderer.graphicsChanged();
			mc.getSingleplayerServer().execute(() ->
			{
				var level = mc.getSingleplayerServer().overworld();
				level.setWeatherParameters(0, 6000, true, false);
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

	private static void baseline() throws IOException
	{
		List<Surface> surfaces = List.of(new Surface(0.4f, 0x80FF0000, true));
		float[] fancy = scene(false, false, 64, 32, "fancy-before", surfaces, List.of());
		float[] fabulous = scene(true, true, 64, 32, "fabulous-before", surfaces, List.of());
		check(fancy[0] > 0.55f && fabulous[0] < 0.25f && fabulous[4] > 0.5f, "Original weather routing must reproduce H14");
		PLibDatabase.LOGGER.info("H14 BASELINE REPRODUCED: Fancy opaque {},{},{}; Fabulous opaque {},{},{}; Fabulous sky {},{},{}",
				fancy[0], fancy[1], fancy[2], fabulous[0], fabulous[1], fabulous[2], fabulous[4], fabulous[5], fabulous[6]);
	}

	private static void suite(int width, int height, String prefix) throws IOException
	{
		verify(width, height, prefix + "-front", List.of(new Surface(0.4f, 0x80FF0000, true)), List.of());
		verify(width, height, prefix + "-behind", List.of(new Surface(0.6f, 0x80FF0000, true)), List.of());
		verify(width, height, prefix + "-crossing", List.of(new Surface(0.4f, 0x80FF0000, true, 0.3f),
				new Surface(0.4f, 0x8000FF00, true, -0.3f)), List.of());
		verify(width, height, prefix + "-four-layers", List.of(new Surface(0.2f, 0x40FF0000, true),
				new Surface(0.3f, 0x4000FF00, true), new Surface(0.4f, 0x400000FF, true),
				new Surface(0.45f, 0x40FFFF00, true)), List.of());
		verify(width, height, prefix + "-emissive", List.of(new Surface(0.4f, 0x8060C0FF, true)), List.of());
		verify(width, height, prefix + "-lit", List.of(new Surface(0.4f, 0x80FF0000, false)), List.of());
		for (String target : TARGETS)
		{
			verify(width, height, prefix + "-" + target + "-front", List.of(new Surface(0.4f, 0x80FF0000, true)),
					List.of(new VanillaLayer(target, 0.25f, 0x660000FF)));
			verify(width, height, prefix + "-" + target + "-behind", List.of(new Surface(0.4f, 0x80FF0000, true)),
					List.of(new VanillaLayer(target, 0.65f, 0x660000FF)));
		}
		verify(width, height, prefix + "-interleaved", List.of(new Surface(0.2f, 0x80FF0000, true),
				new Surface(0.4f, 0x8000FF00, true), new Surface(0.7f, 0x800000FF, true)),
				List.of(new VanillaLayer("particles", 0.3f, 0x66FFFF00),
						new VanillaLayer("weather", 0.6f, 0x6600FFFF), new VanillaLayer("clouds", 0.8f, 0x66FF00FF)));
		verify(width, height, prefix + "-opaque-particle", List.of(new Surface(0.4f, 0x80FF0000, true)),
				List.of(new VanillaLayer("particles", 0.3f, 0xFF0000FF)));
		exceptionState(width, height);
	}

	private static void verify(int width, int height, String name, List<Surface> surfaces, List<VanillaLayer> layers) throws IOException
	{
		float[] actual = scene(true, false, width, height, name + "-fabulous", surfaces, layers);
		boolean lit = surfaces.stream().anyMatch(surface -> !surface.emissive());
		if (layers.isEmpty())
		{
			float[] fancy = scene(false, false, width, height, name + "-fancy", surfaces, layers);
			assertRgb(actual, fancy, name + " Fancy/Fabulous parity");
		}
		if (!lit)
		{
			assertRgb(actual, expected(width, surfaces, layers), name + " analytic ordered-alpha reference");
		}
		check(Math.abs(actual[3] - 1f) < 0.01f && Math.abs(actual[7] - 1f) < 0.01f, "Resolved scene alpha must remain opaque");
		cases++;
		PLibDatabase.LOGGER.info("H14 CASE PASS {}: opaque RGB {},{},{}; sky RGB {},{},{}", name,
				actual[0], actual[1], actual[2], actual[4], actual[5], actual[6]);
	}

	private static float[] scene(boolean fabulous, boolean baseline, int width, int height, String name,
	                            List<Surface> surfaces, List<VanillaLayer> layers) throws IOException
	{
		Minecraft mc = Minecraft.getInstance();
		TextureTarget main = new TextureTarget(width, height, true, Minecraft.ON_OSX);
		TextureTarget caller = new TextureTarget(16, 16, true, Minecraft.ON_OSX);
		int callerVao = GL30.glGenVertexArrays();
		int callerVertexBuffer = GL15.glGenBuffers();
		int callerIndexBuffer = GL15.glGenBuffers();
		Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
		VertexSorting sorting = RenderSystem.getVertexSorting();
		RenderSystem.getModelViewStack().pushMatrix();
		try (PostChain chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), main,
				ResourceLocation.withDefaultNamespace("shaders/post/transparency.json")))
		{
			chain.resize(width, height);
			for (String target : TARGETS)
			{
				clear(chain.getTempTarget(target), 0f, 0f, 0f, 0f, 1f);
			}
			for (VanillaLayer layer : layers)
			{
				float[] color = color(layer.color());
				clear(chain.getTempTarget(layer.target()), color[0] * color[3], color[1] * color[3], color[2] * color[3], color[3], layer.depth());
			}
			clear(main, 0.1f, 0.25f, 0.5f, 1f, 1f);
			RenderSystem.enableScissor(0, 0, width / 2, height);
			GL11.glClearColor(0.2f, 0.2f, 0.2f, 1f);
			GL11.glClearDepth(0.5);
			GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
			RenderSystem.disableScissor();
			RenderSystem.getModelViewStack().identity();
			RenderSystem.applyModelViewMatrix();
			RenderSystem.setProjectionMatrix(new Matrix4f(), VertexSorting.ORTHOGRAPHIC_Z);
			main.bindWrite(true);
			PGeometryData geometry = quad();
			for (Surface surface : surfaces)
			{
				var type = surface.emissive() ? PRenderTypes.RenderTypeProvider.trianglesTranslucentEmissive(WHITE) :
						PRenderTypes.RenderTypeProvider.trianglesTranslucent(WHITE);
				PRenderQueue.submitEntityMesh(type, geometry, new PInstanceHeader(
						new Matrix4f().translation(0f, 0f, surface.depth() * 2f - 1f).m02(surface.slope()),
						surface.color(), 0xF000F0, OverlayTexture.NO_OVERLAY));
			}
			PRenderQueue.flush(PRenderQueue.RenderStage.ENTITIES, main);
			List<RenderTarget> targets = Arrays.stream(TARGETS).map(chain::getTempTarget).toList();
			if (fabulous && baseline)
			{
				RenderTarget weather = chain.getTempTarget("weather");
				PRenderQueue.compositeTranslucency(weather);
				float[] weatherPixel = pixels(weather);
				PLibDatabase.LOGGER.info("H14 weather before vanilla: RGB {},{},{} alpha {} depth {}",
						weatherPixel[0], weatherPixel[1], weatherPixel[2], weatherPixel[3], depth(weather, width / 4));
				chain.process(0f);
			}
			else
			{
				if (fabulous)
				{
					chain.process(0f);
				}
				float leftDepth = depth(main, width / 4);
				float rightDepth = depth(main, width * 3 / 4);
				caller.bindWrite(true);
				RenderSystem.viewport(2, 3, 5, 7);
				RenderSystem.enableDepthTest();
				RenderSystem.depthFunc(GL11.GL_GREATER);
				RenderSystem.depthMask(true);
				RenderSystem.enableScissor(0, 0, width, height);
				RenderSystem.enableBlend();
				RenderSystem.blendFuncSeparate(GL11.GL_ONE, GL11.GL_ZERO, GL11.GL_ZERO, GL11.GL_ONE);
				GL30.glBindVertexArray(callerVao);
				GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, callerVertexBuffer);
				GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, callerIndexBuffer);
				if (GL.getCapabilities().OpenGL40)
				{
					GL30.glDisablei(GL11.GL_BLEND, 1);
					GL40.glBlendFuncSeparatei(1, GL11.GL_DST_COLOR, GL11.GL_SRC_COLOR, GL11.GL_ONE, GL11.GL_ZERO);
					GL40.glBlendEquationSeparatei(1, GL14.GL_FUNC_REVERSE_SUBTRACT, GL14.GL_FUNC_SUBTRACT);
				}
				GL20.glUseProgram(PRenderTypes.ShadersProvider.oitComposite().getId());
				for (int unit = 0; unit < 12; unit++)
				{
					RenderSystem.activeTexture(GL13.GL_TEXTURE0 + unit);
					RenderSystem.bindTexture(mc.getTextureManager().getTexture(WHITE).getId());
				}
				if (fabulous)
				{
					RenderSystem.activeTexture(GL13.GL_TEXTURE0 + 15);
					GL11.glBindTexture(GL11.GL_TEXTURE_2D, mc.getTextureManager().getTexture(WHITE).getId());
				}
				try
				{
					var activeShader = RenderSystem.getShader();
					int[] before = glState();
					if (fabulous)
					{
						PRenderQueue.compositeFabulousTranslucency(main, targets);
					}
					else
					{
						PRenderQueue.compositeTranslucency(main);
					}
					check(Arrays.equals(before, glState()), name + " composite must restore GL state");
					check(RenderSystem.getShader() == activeShader, name + " RenderSystem shader must remain unchanged");
					check(leftDepth == depth(main, width / 4) && rightDepth == depth(main, width * 3 / 4), name + " must not write depth");
					for (RenderTarget target : targets)
					{
						// Native target depths must be untouched by OIT insertion.
						float expectedDepth = layers.stream().filter(layer -> target == chain.getTempTarget(layer.target())).
								map(VanillaLayer::depth).findFirst().orElse(1f);
						check(Math.abs(depth(target, width / 4) - expectedDepth) < 0.00001f, name + " native target depth changed");
					}
				}
				finally
				{
					// Vanilla PostChain.close() itself only supports its twelve cached units.
					RenderSystem.activeTexture(GL13.GL_TEXTURE0);
				}
			}
			check(GL11.glGetError() == GL11.GL_NO_ERROR, name + " OpenGL error");
			save(main, name);
			return pixels(main);
		}
		finally
		{
			RenderSystem.activeTexture(GL13.GL_TEXTURE0);
			GL30.glBindVertexArray(0);
			GL30.glDeleteVertexArrays(callerVao);
			GL15.glDeleteBuffers(callerVertexBuffer);
			GL15.glDeleteBuffers(callerIndexBuffer);
			RenderSystem.blendEquation(GL14.GL_FUNC_ADD);
			PRenderQueue.cleanup();
			main.destroyBuffers();
			caller.destroyBuffers();
			RenderSystem.getModelViewStack().popMatrix();
			RenderSystem.applyModelViewMatrix();
			RenderSystem.setProjectionMatrix(projection, sorting);
			RenderSystem.disableScissor();
			RenderSystem.depthFunc(GL11.GL_LEQUAL);
			RenderSystem.depthMask(true);
			GL11.glClearDepth(1.0);
			mc.getMainRenderTarget().bindWrite(true);
		}
	}

	private static void exceptionState(int width, int height)
	{
		TextureTarget target = new TextureTarget(width, height, true, Minecraft.ON_OSX);
		try
		{
			clear(target, 0f, 0f, 0f, 1f, 1f);
			PRenderQueue.submitEntityMesh(PRenderTypes.RenderTypeProvider.trianglesTranslucentEmissive(WHITE), quad(),
					new PInstanceHeader(new Matrix4f(), 0x80FFFFFF, 0xF000F0, OverlayTexture.NO_OVERLAY));
			PRenderQueue.flush(PRenderQueue.RenderStage.ENTITIES, target);
			List<RenderTarget> invalid = new ArrayList<>(List.of(target, target, target, target, target));
			invalid.set(4, null);
			int[] before = glState();
			boolean failed = false;
			try
			{
				PRenderQueue.compositeFabulousTranslucency(target, invalid);
			}
			catch (NullPointerException expected)
			{
				failed = true;
			}
			check(failed && Arrays.equals(before, glState()), "Composite exception must restore GL state");
			cases++;
		}
		finally
		{
			PRenderQueue.cleanup();
			target.destroyBuffers();
		}
	}

	private static float[] expected(int width, List<Surface> surfaces, List<VanillaLayer> layers)
	{
		float[] result = new float[8];
		for (int side = 0; side < 2; side++)
		{
			List<ReferenceLayer> ordered = new ArrayList<>();
			ordered.add(new ReferenceLayer(side == 0 ? 0.5f : 1f,
					side == 0 ? new float[]{0.2f, 0.2f, 0.2f, 1f} : new float[]{0.1f, 0.25f, 0.5f, 1f}));
			for (VanillaLayer layer : layers)
			{
				ordered.add(new ReferenceLayer(layer.depth(), color(layer.color())));
			}
			float x = 2f * (width * (side * 2 + 1) / 4 + 0.5f) / width - 1f;
			for (Surface surface : surfaces)
			{
				float depth = surface.depth() + surface.slope() * x * 0.5f;
				if (side != 0 || depth <= 0.5f)
				{
					ordered.add(new ReferenceLayer(depth, color(surface.color())));
				}
			}
			ordered.sort(Comparator.comparingDouble(ReferenceLayer::depth).reversed());
			for (ReferenceLayer layer : ordered)
			{
				for (int component = 0; component < 3; component++)
				{
					result[side * 4 + component] = result[side * 4 + component] * (1f - layer.color()[3]) +
							layer.color()[component] * layer.color()[3];
				}
			}
			result[side * 4 + 3] = 1f;
		}
		return result;
	}

	private static int[] glState()
	{
		List<Integer> state = new ArrayList<>();
		int[] parameters = {GL30.GL_DRAW_FRAMEBUFFER_BINDING, GL30.GL_READ_FRAMEBUFFER_BINDING,
				GL20.GL_CURRENT_PROGRAM, GL30.GL_VERTEX_ARRAY_BINDING, GL15.GL_ARRAY_BUFFER_BINDING,
				GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING, GL13.GL_ACTIVE_TEXTURE, GL11.GL_DEPTH_FUNC};
		for (int parameter : parameters)
		{
			state.add(GL11.glGetInteger(parameter));
		}
		for (int parameter : new int[]{GL11.GL_VIEWPORT, GL11.GL_SCISSOR_BOX})
		{
			IntBuffer values = BufferUtils.createIntBuffer(4);
			GL11.glGetIntegerv(parameter, values);
			for (int index = 0; index < 4; index++)
			{
				state.add(values.get(index));
			}
		}
		state.add(GL11.glIsEnabled(GL11.GL_DEPTH_TEST) ? 1 : 0);
		state.add(GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK) ? 1 : 0);
		state.add(GL11.glIsEnabled(GL11.GL_SCISSOR_TEST) ? 1 : 0);
		int count = GL11.glGetInteger(GL20.GL_MAX_DRAW_BUFFERS);
		for (int index = 0; index < count; index++)
		{
			state.add(GL11.glGetInteger(GL20.GL_DRAW_BUFFER0 + index));
			state.add(GL30.glIsEnabledi(GL11.GL_BLEND, index) ? 1 : 0);
			for (int parameter : new int[]{GL14.GL_BLEND_SRC_RGB, GL14.GL_BLEND_DST_RGB, GL14.GL_BLEND_SRC_ALPHA,
					GL14.GL_BLEND_DST_ALPHA, GL20.GL_BLEND_EQUATION_RGB, GL20.GL_BLEND_EQUATION_ALPHA})
			{
				state.add(GL30.glGetIntegeri(parameter, index));
			}
		}
		int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
		state.add(GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D));
		for (int unit = 0; unit < 12; unit++)
		{
			RenderSystem.activeTexture(GL13.GL_TEXTURE0 + unit);
			state.add(GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D));
		}
		RenderSystem.activeTexture(active);
		return state.stream().mapToInt(Integer::intValue).toArray();
	}

	static void clear(RenderTarget target, float red, float green, float blue, float alpha, float depth)
	{
		target.bindWrite(true);
		RenderSystem.disableScissor();
		RenderSystem.depthMask(true);
		GL11.glClearColor(red, green, blue, alpha);
		GL11.glClearDepth(depth);
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
	}

	static PGeometryData quad()
	{
		ByteBuffer vertices = BufferUtils.createByteBuffer(4 * 24).order(ByteOrder.nativeOrder());
		for (float[] position : new float[][]{{-0.9f, -0.9f}, {0.9f, -0.9f}, {0.9f, 0.9f}, {-0.9f, 0.9f}})
		{
			vertices.putFloat(position[0]).putFloat(position[1]).putFloat(0f).
					putFloat(0.5f).putFloat(0.5f).put((byte)0).put((byte)0).put((byte)127).put((byte)0);
		}
		vertices.flip();
		ByteBuffer indices = BufferUtils.createByteBuffer(6 * 2).order(ByteOrder.nativeOrder());
		for (short index : new short[]{0, 1, 2, 0, 2, 3})
		{
			indices.putShort(index);
		}
		indices.flip();
		return new PGeometryData(vertices, indices, 24, 6, PGeometryData.IndexType.UNSIGNED_SHORT);
	}

	private static float[] color(int color)
	{
		return new float[]{((color >> 16) & 255) / 255f, ((color >> 8) & 255) / 255f, (color & 255) / 255f, ((color >>> 24) & 255) / 255f};
	}

	private static float[] pixels(RenderTarget target)
	{
		GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, target.frameBufferId);
		FloatBuffer pixel = BufferUtils.createFloatBuffer(4);
		float[] result = new float[8];
		for (int side = 0; side < 2; side++)
		{
			pixel.clear();
			GL11.glReadPixels(target.width * (side * 2 + 1) / 4, target.height / 2, 1, 1, GL11.GL_RGBA, GL11.GL_FLOAT, pixel);
			pixel.get(result, side * 4, 4);
		}
		return result;
	}

	private static float depth(RenderTarget target, int x)
	{
		int previous = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
		GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, target.frameBufferId);
		FloatBuffer value = BufferUtils.createFloatBuffer(1);
		GL11.glReadPixels(x, target.height / 2, 1, 1, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, value);
		GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previous);
		return value.get(0);
	}

	private static void save(RenderTarget target, String name) throws IOException
	{
		Path output = Path.of(System.getProperty("pulselib.test.oitOutput", "h14-results"));
		Files.createDirectories(output);
		ByteBuffer pixels = BufferUtils.createByteBuffer(target.width * target.height * 4);
		GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, target.frameBufferId);
		GL11.glReadPixels(0, 0, target.width, target.height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
		try (NativeImage image = new NativeImage(target.width, target.height, false))
		{
			for (int y = 0; y < target.height; y++)
			{
				for (int x = 0; x < target.width; x++)
				{
					int offset = (y * target.width + x) * 4;
					int rgba = (pixels.get(offset) & 255) | (pixels.get(offset + 1) & 255) << 8 |
							(pixels.get(offset + 2) & 255) << 16 | (pixels.get(offset + 3) & 255) << 24;
					image.setPixelRGBA(x, target.height - y - 1, rgba);
				}
			}
			image.writeToFile(output.resolve(name + ".png"));
		}
	}

	private static void assertRgb(float[] actual, float[] expected, String name)
	{
		for (int side = 0; side < 2; side++)
		{
			for (int component = 0; component < 3; component++)
			{
				int index = side * 4 + component;
				check(Math.abs(actual[index] - expected[index]) < 0.025f,
						name + ": pixel " + side + " component " + component + " expected " + expected[index] + ", got " + actual[index]);
			}
		}
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
		{
			throw new AssertionError("H14: " + message);
		}
	}

	private record Surface(float depth, int color, boolean emissive, float slope)
	{
		private Surface(float depth, int color, boolean emissive)
		{
			this(depth, color, emissive, 0f);
		}
	}

	private record VanillaLayer(String target, float depth, int color)
	{
	}

	private record ReferenceLayer(float depth, float[] color)
	{
	}
}
