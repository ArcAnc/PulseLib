/**
 * @author ArcAnc
 * Created at: 31.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage61client;

import com.arcanc.pulselib.api.registration.PModelRegistrar;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.PWorldRenderPass;
import com.arcanc.pulselib.content.renderer.base.PWorldRenderState;
import com.arcanc.pulselib.content.renderer.gl.PGlRenderHost;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.content.renderer.plan.PTransparencyMode;
import com.arcanc.pulselib.data.gecko.PGeckoModelLoader;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.PResourceCache;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.client.CameraType;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Real Fabric native targets and production OIT resolve against a same-frame ordered-alpha oracle. */
public final class TransparencyProbe
{
	private static final Identifier WHITE = PLibDatabase.rl("lifecycle/stage62a_white");
	private static final PModelData DATA = PModelData.direct(PLibDatabase.rl("lifecycle/atlas_gecko_0"), PGeckoModelLoader.INSTANCE.id());
	private static final int FRAMES = 4;
	private static final Identifier AFTER = PLibDatabase.rl("stage62b/after");
	private static final int[][][] PIXELS = new int[8][9][2];
	private static final float[][][] BACKGROUND = new float[8][9][];
	private static final float[][][] WORLD = new float[8][9][];
	private static final float[][] DEPTHS = new float[8][2];
	private static final float[][][] NATIVE = new float[8][5][];
	private static final float[][] RESOLVED = new float[8][];
	private static final int[] CHOSEN = new int[8];
	private static final List<Object> RECORDS = new ArrayList<>();
	private static final int[][] COVERAGE = new int[36][2];
	private static @Nullable CompletableFuture<Void> reload;
	private static @Nullable Object oldModel;
	private static @Nullable ArmorStand marker;
	private static boolean started;
	private static boolean declared;
	private static boolean submitted;
	private static boolean ready;
	private static boolean advance;
	private static boolean completed;
	private static int phase;
	private static int ticks;
	private static int frames;
	private static int accepted;
	private static int cases;
	private static long modeStart;
	private static final PRenderQueue.RenderStage[] SOURCES = {PRenderQueue.RenderStage.ENTITIES,
			PRenderQueue.RenderStage.SOLID_BLOCKS, PRenderQueue.RenderStage.TRANSLUCENT_BLOCKS};

	private TransparencyProbe()
	{
	}

	public static void models(PModelRegistrar registrar)
	{
		registrar.model(DATA).texture("0", WHITE);
	}

	public static void register()
	{
		ClientTickEvents.END_CLIENT_TICK.register(TransparencyProbe::tick);
		LevelRenderEvents.END_EXTRACTION.register(context ->
		{
			if (!started || completed)
				return;
			// The fixture owns queue submissions and declares their extraction flag through the existing contract.
			for (var state : context.levelState().entityRenderStates)
				((PWorldRenderState)state).pulselib$setRequiresSeparateTargets(true);
			declared = PWorldRenderPass.requiresSeparateTargets(context.levelState());
		});
		LevelRenderEvents.COLLECT_SUBMITS.register(context -> submit());
		LevelRenderEvents.AFTER_SOLID_FEATURES.addPhaseOrdering(Event.DEFAULT_PHASE, AFTER);
		LevelRenderEvents.AFTER_SOLID_FEATURES.register(AFTER, context -> early());
		LevelRenderEvents.END_MAIN.addPhaseOrdering(Event.DEFAULT_PHASE, AFTER);
		LevelRenderEvents.END_MAIN.register(AFTER, context -> afterWorld());
		ClientLifecycleEvents.CLIENT_STOPPING.register(mc ->
		{
			check(completed && phase == 36 && cases == 36 * FRAMES * 8, "all policies/modes/effects/epochs before shutdown");
			PLibDatabase.LOGGER.info("STAGE62B SHUTDOWN PASS");
		});
	}

	private static int effect()
	{
		return phase % 9 / 3;
	}

	private static PTransparencyMode policy()
	{
		return phase / 9 % 2 == 0 ? PTransparencyMode.OIT : PTransparencyMode.ORDINARY;
	}

	private static void mode()
	{
		Minecraft mc = Minecraft.getInstance();
		mc.options.graphicsPreset().set(GraphicsPreset.values()[phase % 3]);
		mc.levelRenderer.allChanged();
		modeStart = System.nanoTime();
		frames = accepted = 0;
		submitted = ready = false;
	}

	private static void tick(Minecraft mc)
	{
		check(Files.isRegularFile(mc.gameDirectory.toPath().resolve(".pulselib-world-transparency")), "disposable transparency client marker");
		if (++ticks > 12000)
			throw new AssertionError("STAGE62B timeout phase=" + phase);
		if (completed || mc.player == null || mc.level == null || mc.getOverlay() != null || DATA.getModel() == null)
			return;
		check(mc.player.isAlive() && mc.player.isCreative(), "isolated creative player remains alive");
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
		mc.level.setRainLevel(effect() == 2 ? 1 : 0);
		mc.level.setThunderLevel(0);
		if (!started)
		{
			mc.options.hideGui = true;
			mc.options.setCameraType(CameraType.FIRST_PERSON);
			marker = new ArmorStand(EntityType.ARMOR_STAND, mc.level);
			marker.setId(0x6262);
			marker.setInvisible(true);
			marker.setNoGravity(true);
			marker.setPos(0, 80, 4);
			mc.level.addEntity(marker);
			started = true;
			mode();
		}
		if (reload != null)
		{
			if (!reload.isDone())
				return;
			reload.join();
			check(DATA.getModel() != oldModel, "real resource reload replaces baked model");
			reload = null;
			mode();
		}
		if (advance)
		{
			advance = false;
			phase++;
			if (phase == 36)
			{
				completed = true;
				try
				{
					Files.writeString(mc.gameDirectory.toPath().resolve("transparency-results.json"), new GsonBuilder().setPrettyPrinting().create().toJson(
							Map.of("status", "PASS", "phases", phase, "cases", cases, "coverage", COVERAGE, "records", RECORDS)));
				}
				catch (java.io.IOException exception)
				{
					throw new AssertionError(exception);
				}
				PLibDatabase.LOGGER.info("STAGE62B WORLD PASS: phases={}, cases={}, modes=Fast/Fancy/Fabulous, policies=OIT/ORDINARY, epochs=2, targets=5", phase, cases);
				mc.getConnection().getConnection().disconnect(Component.literal("Pulse transparency probe"));
				mc.disconnect(new TitleScreen(), false);
				mc.stop();
				return;
			}
			if (phase == 18)
			{
				oldModel = DATA.getModel();
				reload = mc.reloadResourcePacks();
				return;
			}
			mode();
		}
		if (effect() == 1)
			for (int lane = 0; lane < 8; lane++)
			{
				double z = lane % 2 == 0 ? 8 : 15;
				double scale = (z + 6) / (centerZ(lane) + 6);
				var particle = mc.particleEngine.createParticle(ParticleTypes.SOUL, (-7.5 + lane * 2) * scale,
						mc.player.getEyeY() + (81 - mc.player.getEyeY()) * scale, z, 0, 0, 0);
				check(particle != null, "native soul provider exists");
				particle.setLifetime(2);
				particle.setParticleSpeed(0, 0, 0);
			}
	}

	private static float centerZ(int lane)
	{
		return lane % 2 == 0 ? 9.5f : 13.5f;
	}

	private static PInstanceHeader header(int lane, float z, int color)
	{
		var camera = Minecraft.getInstance().gameRenderer.getMainCamera().position();
		return new PInstanceHeader(new Matrix4f().translation((float)(-7.5 + lane * 2 - camera.x),
				(float)(81 - camera.y), (float)(z - camera.z)).scale(0.48f, 0.85f, 1), color, 0xF000F0, OverlayTexture.NO_OVERLAY);
	}

	private static void submit()
	{
		submitted = ready = false;
		var mc = Minecraft.getInstance();
		if (!started || !declared || completed || advance || reload != null || mc.getOverlay() != null ||
				!mc.levelRenderer.isSectionCompiledAndVisible(new BlockPos(-4, 82, 11)) ||
				!mc.levelRenderer.isSectionCompiledAndVisible(new BlockPos(4, 82, 11)) ||
				++frames < 45 || System.nanoTime() - modeStart < 2_000_000_000L)
			return;
		check(Minecraft.useShaderTransparency() == (phase % 3 == 2), "native graphics preset must remain unchanged");
		var geometry = quad();
		var type = PRenderTypes.RenderTypeProvider.trianglesEmissiveTranslucent(PResourceCache.ATLAS_LOCATION);
		check(PRenderTypes.usesOit(type), "same existing material supports both submission policies");
		for (int lane = 0; lane < 8; lane++)
		{
			if (lane >= 6)
				PRenderQueue.submit(SOURCES[0], PRenderTypes.RenderTypeProvider.trianglesEmissiveSolid(PResourceCache.ATLAS_LOCATION), geometry,
						header(lane, centerZ(lane), 0xFF0000FF));
			// Deliberately submit front first and back second through different sources.
			PRenderQueue.submit(SOURCES[lane % 3], type, geometry, header(lane, centerZ(lane) - 0.25f, 0x80FF0000), policy());
			PRenderQueue.submit(SOURCES[(lane + 1) % 3], type, geometry, header(lane, centerZ(lane) + 0.25f, 0x8000FF00), policy());
		}
		submitted = true;
	}

	private static void early()
	{
		if (!submitted)
			return;
		var mc = Minecraft.getInstance();
		var frame = new PGlRenderHost().captureFrame();
		var vp = new Matrix4f(frame.projection()).mul(frame.modelView());
		var camera = mc.gameRenderer.getMainCamera().position();
		for (int lane = 0; lane < 8; lane++)
		{
			for (int layer = 0; layer < 2; layer++)
			{
				var clip = vp.transform(new Vector4f(0, 0, (float)(centerZ(lane) + (layer == 0 ? -0.25 : 0.25) - camera.z), 1));
				DEPTHS[lane][layer] = (clip.z / clip.w + 1) * 0.5f;
			}
			for (int candidate = 0; candidate < 9; candidate++)
			{
				var clip = vp.transform(new Vector4f((float)(-7.5 + lane * 2 + (candidate % 3 - 1) * 0.14 - camera.x),
						(float)(81 + (candidate / 3 - 1) * 0.25 - camera.y), (float)(centerZ(lane) - camera.z), 1));
				int x = (int)((clip.x / clip.w + 1) * 0.5f * mc.getMainRenderTarget().width);
				int y = (int)((clip.y / clip.w + 1) * 0.5f * mc.getMainRenderTarget().height);
				check(x > 0 && y > 0 && x < mc.getMainRenderTarget().width && y < mc.getMainRenderTarget().height, "sample inside viewport");
				PIXELS[lane][candidate] = new int[]{x, y};
				BACKGROUND[lane][candidate] = pixel(mc.getMainRenderTarget(), x, y);
			}
		}
	}

	private static List<Layer> base(float[] background)
	{
		List<Layer> result = new ArrayList<>();
		result.add(new Layer(background[4], new float[]{background[0], background[1], background[2], 1}));
		return result;
	}

	private static void pulse(List<Layer> layers, int lane, float opaqueDepth)
	{
		for (int layer = 0; layer < 2; layer++)
			if (DEPTHS[lane][layer] <= opaqueDepth)
				layers.add(new Layer(DEPTHS[lane][layer], layer == 0 ? new float[]{1, 0, 0, 128f / 255} : new float[]{0, 1, 0, 128f / 255}));
	}

	private static void afterWorld()
	{
		if (!submitted)
			return;
		for (int lane = 0; lane < 8; lane++)
			for (int candidate = 0; candidate < 9; candidate++)
			{
				int[] point = PIXELS[lane][candidate];
				float[] background = BACKGROUND[lane][candidate];
				List<Layer> layers = base(background);
				if (policy() == PTransparencyMode.ORDINARY)
					pulse(layers, lane, background[4]);
				WORLD[lane][candidate] = pixel(Minecraft.getInstance().getMainRenderTarget(), point[0], point[1]);
				compare(WORLD[lane][candidate], resolve(layers), 0.025f, "world flush " + lane + "/" + candidate);
				check(Math.abs(WORLD[lane][candidate][4] - background[4]) < 0.000001f, "alpha world pass preserves depth");
			}
		ready = true;
	}

	/** Invoked only by the test observer at production composite HEAD/RETURN. */
	public static void composite(RenderTarget destination, List<RenderTarget> targets, boolean after)
	{
		if (!ready || advance)
			return;
		check(targets.size() == 5 && targets.stream().allMatch(target -> target.getColorTexture() != null && !target.getColorTexture().isClosed()), "five live native targets retained through resolve");
		if (!after)
		{
			boolean effectVisible = effect() == 0;
			for (int lane = 0; lane < 8; lane++)
			{
				CHOSEN[lane] = 4;
				if (effect() != 0)
					for (int candidate = 0; candidate < 9; candidate++)
					{
						int[] point = PIXELS[lane][candidate];
						if (pixel(targets.get(effect() == 1 ? 2 : 3), point[0], point[1])[3] > 0.005f)
						{
							CHOSEN[lane] = candidate;
							effectVisible = true;
							break;
						}
					}
			}
			if (!effectVisible)
			{
				ready = false;
				return;
			}
			for (int lane = 0; lane < 8; lane++)
			{
				int[] point = PIXELS[lane][CHOSEN[lane]];
				List<Layer> layers = base(WORLD[lane][CHOSEN[lane]]);
				for (int index = 0; index < 5; index++)
				{
					float[] value = pixel(targets.get(index), point[0], point[1]);
					NATIVE[lane][index] = value;
					if (value[3] > 0)
						layers.add(new Layer(value[4], new float[]{value[0] / value[3], value[1] / value[3], value[2] / value[3], value[3]}));
				}
				RESOLVED[lane] = pixel(destination, point[0], point[1]);
				compare(RESOLVED[lane], resolve(layers), 0.025f, "native postchain phase=" + phase + " lane=" + lane);
			}
			return;
		}
		for (int lane = 0; lane < 8; lane++)
			verify(destination, targets, lane);
		if (++accepted == FRAMES)
		{
			check(effect() != 1 || COVERAGE[phase][0] > 0, "native particle coverage in each phase");
			check(effect() != 2 || COVERAGE[phase][1] > 0, "native rain coverage in each phase");
			PLibDatabase.LOGGER.info("STAGE62B PHASE PASS: phase={}, policy={}, mode={}, effect={}, cases={}, coverage={}", phase, policy(), phase % 3, effect(), cases, Arrays.toString(COVERAGE[phase]));
			advance = true;
		}
		ready = submitted = false;
	}

	private static void verify(RenderTarget destination, List<RenderTarget> targets, int lane)
	{
		int candidate = CHOSEN[lane];
		int[] point = PIXELS[lane][candidate];
		float[] world = WORLD[lane][candidate];
		List<Layer> layers = base(world);
		List<float[]> nativePixels = new ArrayList<>();
		for (int index = 0; index < targets.size(); index++)
		{
			float[] value = NATIVE[lane][index];
			nativePixels.add(value);
			if (value[3] > 0)
				layers.add(new Layer(value[4], new float[]{value[0] / value[3], value[1] / value[3], value[2] / value[3], value[3]}));
			if (index == 2 && value[3] > 0.005f)
				COVERAGE[phase][0]++;
			if (index == 3 && value[3] > 0.005f)
			{
				check(value[4] < 1, "real weather fragment depth in retained target");
				COVERAGE[phase][1]++;
			}
		}
		if (policy() == PTransparencyMode.OIT)
			pulse(layers, lane, BACKGROUND[lane][candidate][4]);
		float[] expected = resolve(layers);
		float[] actual = pixel(destination, point[0], point[1]);
		compare(actual, expected, 0.035f, "final phase=" + phase + " lane=" + lane + " native=" + nativePixels.stream().map(Arrays::toString).toList());
		check(Math.abs(actual[4] - world[4]) < 0.000001f, "native/OIT resolve preserves destination depth");
		if (accepted == 0 && (lane == 0 || lane == 5))
			RECORDS.add(Map.of("phase", phase, "lane", lane, "policy", policy().name(), "pixel", point,
					"native", nativePixels, "world", world, "expected", expected, "actual", actual, "pulseDepth", DEPTHS[lane].clone(), "nativeResolved", RESOLVED[lane]));
		cases++;
	}

	private static float[] resolve(List<Layer> layers)
	{
		layers.sort(Comparator.comparingDouble(Layer::depth).reversed());
		float[] result = new float[3];
		for (Layer layer : layers)
			for (int channel = 0; channel < 3; channel++)
				result[channel] = layer.color()[channel] * layer.color()[3] + result[channel] * (1 - layer.color()[3]);
		return result;
	}

	private static void compare(float[] actual, float[] expected, float tolerance, String boundary)
	{
		for (int channel = 0; channel < 3; channel++)
			check(Math.abs(actual[channel] - expected[channel]) < tolerance,
					boundary + " expected=" + Arrays.toString(expected) + " actual=" + Arrays.toString(actual));
	}

	private static PGeometryData quad()
	{
		var sprite = PResourceCache.getTextureAtlas().getSprite(PResourceCache.spriteId(WHITE));
		check(sprite.contents().name().equals(PResourceCache.spriteId(WHITE)), "White control sprite must be present after each reload");
		float u = (sprite.getU0() + sprite.getU1()) * 0.5f;
		float v = (sprite.getV0() + sprite.getV1()) * 0.5f;
		int stride = PRenderTypes.VertexFormatProvider.POSITION_TEX_NORMAL.getVertexSize();
		ByteBuffer vertices = ByteBuffer.allocate(stride * 4).order(ByteOrder.nativeOrder());
		for (int i = 0; i < 4; i++)
		{
			int start = vertices.position();
			vertices.putFloat(i == 0 || i == 3 ? -1 : 1).putFloat(i < 2 ? -1 : 1).putFloat(0);
			vertices.putFloat(u).putFloat(v).put((byte)0).put((byte)0).put((byte)127);
			vertices.position(start + stride);
		}
		vertices.flip();
		ByteBuffer indices = ByteBuffer.allocate(12).order(ByteOrder.nativeOrder());
		for (int index : new int[]{0, 1, 2, 0, 2, 3})
			indices.putShort((short)index);
		indices.flip();
		return new PGeometryData(vertices, indices, stride, 6, PGeometryData.IndexType.UNSIGNED_SHORT);
	}

	private static float[] pixel(RenderTarget target, int x, int y)
	{
		int previous = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
		int fbo = GL30.glGenFramebuffers();
		try
		{
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, fbo);
			GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, ((GlTexture)target.getColorTexture()).glId(), 0);
			GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, ((GlTexture)target.getDepthTexture()).glId(), 0);
			GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
			check(GL30.glCheckFramebufferStatus(GL30.GL_READ_FRAMEBUFFER) == GL30.GL_FRAMEBUFFER_COMPLETE, "Incomplete native H14 readback framebuffer");
			var rgba = BufferUtils.createFloatBuffer(4);
			var depth = BufferUtils.createFloatBuffer(1);
			GL11.glReadPixels(x, y, 1, 1, GL11.GL_RGBA, GL11.GL_FLOAT, rgba);
			GL11.glReadPixels(x, y, 1, 1, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, depth);
			return new float[]{rgba.get(0), rgba.get(1), rgba.get(2), rgba.get(3), depth.get(0)};
		}
		finally
		{
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previous);
			GL30.glDeleteFramebuffers(fbo);
		}
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
			throw new AssertionError("STAGE62B: " + message);
	}

	private record Layer(float depth, float[] color)
	{
	}
}
