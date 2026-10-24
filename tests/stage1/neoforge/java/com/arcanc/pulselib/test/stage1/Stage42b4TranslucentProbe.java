/**
 * @author ArcAnc
 * Created at: 25.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1;

import com.arcanc.pulselib.content.model.baked.PBakedBone;
import com.arcanc.pulselib.content.model.baked.PBakedMesh;
import com.arcanc.pulselib.content.model.baked.PMeshRenderContext;
import com.arcanc.pulselib.content.model.textures.PAlphaMode;
import com.arcanc.pulselib.content.registration.block.block_entity.TestBlockEntity;
import com.arcanc.pulselib.content.registration.block.block_entity.ber.renderState.TestBlockEntityRenderState;
import com.arcanc.pulselib.content.registration.entity.TestEntity;
import com.arcanc.pulselib.content.renderer.PBlockRenderer;
import com.arcanc.pulselib.content.renderer.PEntityRenderer;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
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
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Actual H14 owners plus native terrain, particle and rain targets, using a CPU ordered-alpha reference. */
public final class Stage42b4TranslucentProbe
{
	public static final PModelData MODEL = PModelData.entity(PLibDatabase.rl("stage42b4_owner"), PGeckoModelLoader.INSTANCE.id());
	private static final int CANDIDATES = 9;
	private static final int FRAMES_PER_PHASE = 8;
	private static final boolean[] SEEN = new boolean[8];
	private static final boolean[][] ISOLATED = new boolean[8][CANDIDATES];
	private static final int[][][] SAMPLES = new int[8][CANDIDATES][2];
	private static final float[][][] BACKGROUND = new float[8][CANDIDATES][5];
	private static final float[][] EXPECTED = new float[8][3];
	private static final float[][] NATIVE_EXPECTED = new float[8][3];
	private static final float[][] RESOLVED_NATIVE = new float[8][];
	private static final float[] FRONT_DEPTH = new float[8];
	private static final float[] BACK_DEPTH = new float[8];
	private static final int[] CHOSEN = new int[8];
	private static final boolean[] PHASE_PARTICLES = new boolean[8];
	private static final boolean[][] COVERAGE = new boolean[8][2];
	private static final float[] DESTINATION_DEPTH = new float[8];
	private static final List<String> RECORDS = new ArrayList<>();
	private static boolean started;
	private static boolean advance;
	private static boolean earlyReady;
	private static boolean lateReady;
	private static boolean completed;
	private static boolean originalImproved;
	private static boolean originalHideGui;
	private static GraphicsPreset originalPreset = GraphicsPreset.CUSTOM;
	private static CameraType originalCamera = CameraType.FIRST_PERSON;
	private static int phase;
	private static int frames;
	private static int cases;
	private static int checkedFrames;
	private static int particleCases;
	private static int weatherCases;
	private static long deadline;
	private static long modeStart;
	private static @Nullable CompletableFuture<Void> reload;

	private Stage42b4TranslucentProbe()
	{
	}

	public static void register()
	{
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Stage42b4TranslucentProbe::early);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Stage42b4TranslucentProbe::nativeLayers);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Stage42b4TranslucentProbe::finish);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Stage42b4TranslucentProbe::camera);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Stage42b4TranslucentProbe::shutdown);
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
		mc.level.setRainLevel(effect() == 2 ? 1 : 0);
		mc.level.setThunderLevel(0);
		if (!started)
		{
			check(Files.isRegularFile(mc.gameDirectory.toPath().resolve(".pulselib-stage42b4")), "Missing disposable H14 client marker");
			originalImproved = mc.options.improvedTransparency().get();
			originalPreset = mc.options.graphicsPreset().get();
			originalCamera = mc.options.getCameraType();
			originalHideGui = mc.options.hideGui;
			mc.options.hideGui = true;
			mc.options.setCameraType(CameraType.FIRST_PERSON);
			deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(360);
			started = true;
			mode();
		}
		check(System.nanoTime() < deadline, "H14 actual-world timeout phase=" + phase + " frames=" + frames);
		if (reload != null)
		{
			if (!reload.isDone())
				return;
			reload.join();
			reload = null;
			mode();
		}
		if (advance)
		{
			advance = false;
			phase++;
			if (phase == 18)
			{
				check(particleCases > 0 && weatherCases > 0, "Every phase needs live native targets and actual particle/weather coverage");
				completed = true;
				mc.options.graphicsPreset().set(originalPreset);
				mc.options.improvedTransparency().set(originalImproved);
				mc.options.setCameraType(originalCamera);
				mc.options.hideGui = originalHideGui;
				PLibDatabase.LOGGER.info("STAGE 4.2b.4 H14 WORLD PASS: cases={}, actual particle cases={}, weather cases={}, Fast/Fancy/Fabulous before/after reload", cases, particleCases, weatherCases);
				mc.stop();
				return;
			}
			if (phase == 9)
				reload = mc.reloadResourcePacks();
			else
				mode();
		}
		if (effect() == 1)
			for (int lane = 0; lane < 8; lane++)
			{
				double particleZ = lane % 2 == 0 ? 8 : 15;
				double ownerZ = lane % 2 == 0 ? 9.5 : 13.5;
				double rayScale = (particleZ + 6) / (ownerZ + 6);
				double eyeY = mc.player.getEyeY();
				var particle = mc.particleEngine.createParticle(ParticleTypes.SOUL, (-7.5 + lane * 2) * rayScale,
						eyeY + (81 - eyeY) * rayScale, particleZ, 0, 0, 0);
				check(particle != null, "Native soul particle provider must exist");
				particle.setLifetime(2);
				particle.setParticleSpeed(0, 0, 0);
			}
	}

	private static int effect()
	{
		return phase % 9 / 3;
	}

	private static void mode()
	{
		var mc = Minecraft.getInstance();
		mc.options.graphicsPreset().set(GraphicsPreset.values()[phase % 3]);
		mc.levelRenderer.allChanged();
		frames = checkedFrames = 0;
		modeStart = System.nanoTime();
		earlyReady = lateReady = false;
		Arrays.fill(SEEN, false);
		Arrays.fill(PHASE_PARTICLES, false);
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
		if (!started || completed || advance || reload != null || mc.getOverlay() != null)
			return;
		for (boolean seen : SEEN)
			if (!seen)
				return;
		if (mc.level.getBrightness(LightLayer.SKY, new BlockPos(-5, 82, 10)) != 15 ||
				mc.level.getBrightness(LightLayer.SKY, new BlockPos(2, 82, 9)) != 15 ||
				!mc.levelRenderer.isSectionCompiledAndVisible(new BlockPos(-4, 82, 11)) ||
				!mc.levelRenderer.isSectionCompiledAndVisible(new BlockPos(4, 82, 11)) ||
				++frames < 45 || System.nanoTime() - modeStart < TimeUnit.SECONDS.toNanos(2))
			return;
		check(Minecraft.useShaderTransparency() == (phase % 3 == 2), "Requested graphics option must remain unchanged by the adapter");
		var camera = event.getLevelRenderState().cameraRenderState.pos;
		var frame = new PGlRenderHost().captureFrame();
		var vp = new Matrix4f(frame.projection()).mul(frame.modelView());
		var inverse = new Matrix4f(vp).invert();
		var target = mc.getMainRenderTarget();
		for (int lane = 0; lane < 8; lane++)
		{
			float centerZ = lane % 2 == 0 ? 9.5f : 13.5f;
			FRONT_DEPTH[lane] = depth(vp, (float)(centerZ - 0.25 - camera.z));
			BACK_DEPTH[lane] = depth(vp, (float)(centerZ + 0.25 - camera.z));
			for (int sample = 0; sample < CANDIDATES; sample++)
			{
				Vector4f screen = vp.transform(new Vector4f((float)(-7.5 + lane * 2 + (sample % 3 - 1) * 0.14 - camera.x),
						(float)(81 + (sample / 3 - 1) * 0.25 - camera.y), (float)(centerZ - camera.z), 1));
				int x = (int)((screen.x / screen.w + 1) * 0.5f * target.width);
				int y = (int)((screen.y / screen.w + 1) * 0.5f * target.height);
				check(x > 0 && y > 0 && x < target.width && y < target.height, "H14 sample outside viewport");
				SAMPLES[lane][sample][0] = x;
				SAMPLES[lane][sample][1] = y;
				BACKGROUND[lane][sample] = pixel(target, x, y);
				var ray = inverse.transform(new Vector4f((x + 0.5f) / target.width * 2 - 1, (y + 0.5f) / target.height * 2 - 1, 1, 1));
				ray.div(ray.w);
				ISOLATED[lane][sample] = true;
				for (int other = 0; other < 8; other++)
					if (other != lane && intersects(ray, camera.x, camera.y, camera.z, other))
						ISOLATED[lane][sample] = false;
			}
		}
		earlyReady = true;
	}

	/** Excludes projected intersections with neighbouring cubes from this single-owner reference. */
	private static boolean intersects(Vector4f ray, double cameraX, double cameraY, double cameraZ, int lane)
	{
		double centerX = -7.5 + lane * 2;
		double centerZ = lane % 2 == 0 ? 9.5 : 13.5;
		double[] origin = {cameraX, cameraY, cameraZ};
		double[] direction = {ray.x, ray.y, ray.z};
		double[] minimum = {centerX - 0.5, 80, centerZ - 0.25};
		double[] maximum = {centerX + 0.5, 82, centerZ + 0.25};
		double near = 0;
		double far = Double.POSITIVE_INFINITY;
		for (int axis = 0; axis < 3; axis++)
		{
			if (Math.abs(direction[axis]) < 1e-12)
			{
				if (origin[axis] < minimum[axis] || origin[axis] > maximum[axis])
					return false;
				continue;
			}
			double first = (minimum[axis] - origin[axis]) / direction[axis];
			double last = (maximum[axis] - origin[axis]) / direction[axis];
			near = Math.max(near, Math.min(first, last));
			far = Math.min(far, Math.max(first, last));
		}
		return far >= near;
	}

	private static float depth(Matrix4f vp, float z)
	{
		var clip = vp.transform(new Vector4f(0, 0, z, 1));
		return (clip.z / clip.w + 1) * 0.5f;
	}

	private static void nativeLayers(RenderLevelStageEvent.AfterWeather event)
	{
		if (!earlyReady || advance)
			return;
		var mc = Minecraft.getInstance();
		var renderer = mc.levelRenderer;
		@Nullable RenderTarget[] targets = {renderer.getTranslucentTarget(), renderer.getItemEntityTarget(), renderer.getParticlesTarget(), renderer.getWeatherTarget(), renderer.getCloudsTarget()};
		for (RenderTarget target : targets)
			check(target != null, "Visible PulseLib world owners require five native targets, phase=" + phase);
		int covered = 0;
		RECORDS.clear();
		for (int lane = 0; lane < 8; lane++)
		{
			int chosen = 4;
			if (effect() != 0)
				for (int sample = 0; sample < CANDIDATES; sample++)
				{
					if (!ISOLATED[lane][sample])
						continue;
					float[] candidate = pixel(targets[effect() == 1 ? 2 : 3], SAMPLES[lane][sample][0], SAMPLES[lane][sample][1]);
					if (candidate[3] <= 0.005f)
						continue;
					chosen = sample;
					covered++;
					break;
				}
			check(ISOLATED[lane][chosen], "Reference ray must intersect only its named owner, lane=" + lane);
			CHOSEN[lane] = chosen;
			float[] background = BACKGROUND[lane][chosen];
			List<Layer> layers = new ArrayList<>();
			layers.add(new Layer(background[4], new float[]{background[0], background[1], background[2], 1}));
			List<float[]> nativePixels = new ArrayList<>();
			for (RenderTarget target : targets)
			{
				float[] value = pixel(target, SAMPLES[lane][chosen][0], SAMPLES[lane][chosen][1]);
				nativePixels.add(value);
				if (value[3] > 0)
					layers.add(new Layer(value[4], new float[]{value[0] / value[3], value[1] / value[3], value[2] / value[3], value[3]}));
			}
			resolve(layers, NATIVE_EXPECTED[lane]);
			for (float depth : new float[]{BACK_DEPTH[lane], FRONT_DEPTH[lane]})
				if (depth <= background[4])
					layers.add(new Layer(depth, new float[]{1, 0, 0, 128f / 255}));
			resolve(layers, EXPECTED[lane]);
			RECORDS.add("background=" + Arrays.toString(background) + ", native=" + nativePixels.stream().map(Arrays::toString).toList() + ", depths=" + FRONT_DEPTH[lane] + "/" + BACK_DEPTH[lane]);
			COVERAGE[lane][0] = nativePixels.get(2)[3] > 0.005f;
			COVERAGE[lane][1] = nativePixels.get(3)[3] > 0.005f;
			if (COVERAGE[lane][1])
				check(nativePixels.get(3)[4] < 1, "Separated native weather must write actual fragment depth");
			DESTINATION_DEPTH[lane] = pixel(mc.getMainRenderTarget(), SAMPLES[lane][chosen][0], SAMPLES[lane][chosen][1])[4];
		}
		if (effect() != 0 && covered == 0)
			return;
		lateReady = true;
	}

	private static void finish(RenderLevelStageEvent.AfterLevel event)
	{
		try
		{
			if (!earlyReady || !lateReady || advance)
				return;
			var mc = Minecraft.getInstance();
			for (int lane = 0; lane < 8; lane++)
			{
				int[] sample = SAMPLES[lane][CHOSEN[lane]];
				float[] actual = pixel(mc.getMainRenderTarget(), sample[0], sample[1]);
				for (int color = 0; color < 3; color++)
					if (Math.abs(actual[color] - EXPECTED[lane][color]) >= 0.035f)
						throw new AssertionError("H14 ordered-alpha mismatch phase=" + phase + " lane=" + lane + " actual=" + Arrays.toString(actual) + " expected=" + Arrays.toString(EXPECTED[lane]) + " " + RECORDS.get(lane) + ", nativeResolved=" + Arrays.toString(RESOLVED_NATIVE[lane]) + ", oit=" + oitDiagnostics(sample[0], sample[1]));
				check(Math.abs(actual[4] - DESTINATION_DEPTH[lane]) < 0.000001f, "H14 composition changed destination depth");
				if (COVERAGE[lane][0])
				{
					particleCases++;
					PHASE_PARTICLES[lane] = true;
				}
				if (COVERAGE[lane][1])
					weatherCases++;
				PLibDatabase.LOGGER.info("STAGE 4.2b.4 H14 CASE PASS: phase={}, frame={}, preset={}, improved={}, effect={}, lane={}, owner={}, terrain={}, behind={}, expected={}, actual={}, {}", phase, checkedFrames,
						mc.options.graphicsPreset().get(), Minecraft.useShaderTransparency(), effect(), lane, lane % 4 < 2 ? "Entity" : "BlockEntity", lane < 4 ? "glass" : "water", lane % 2 == 1,
						Arrays.toString(EXPECTED[lane]), Arrays.toString(actual), RECORDS.get(lane));
				cases++;
			}
			if (++checkedFrames == FRAMES_PER_PHASE)
			{
				if (effect() == 1)
					for (boolean covered : PHASE_PARTICLES)
						check(covered, "Every named owner must overlap a real native particle in this phase");
				Screenshot.grab(mc.gameDirectory, "stage42b4-phase" + phase + ".png", mc.getMainRenderTarget(), 1,
						message -> PLibDatabase.LOGGER.info("{}", message.getString()));
				advance = true;
			}
		}
		finally
		{
			Arrays.fill(SEEN, false);
			earlyReady = lateReady = false;
		}
	}


	private static void resolve(List<Layer> layers, float[] result)
	{
		layers.sort(Comparator.comparingDouble(Layer::depth).reversed());
		Arrays.fill(result, 0);
		for (Layer layer : layers)
			for (int color = 0; color < 3; color++)
				result[color] = layer.color()[color] * layer.color()[3] + result[color] * (1 - layer.color()[3]);
	}

	public static void beforeComposite(RenderTarget destination)
	{
		if (!earlyReady || !lateReady || advance)
			return;
		for (int lane = 0; lane < 8; lane++)
		{
			int[] sample = SAMPLES[lane][CHOSEN[lane]];
			RESOLVED_NATIVE[lane] = pixel(destination, sample[0], sample[1]);
			for (int color = 0; color < 3; color++)
				check(Math.abs(RESOLVED_NATIVE[lane][color] - NATIVE_EXPECTED[lane][color]) < 0.025f,
						"Native resolve mismatch phase=" + phase + " lane=" + lane + " actual=" + Arrays.toString(RESOLVED_NATIVE[lane]) + " expected=" + Arrays.toString(NATIVE_EXPECTED[lane]) + " " + RECORDS.get(lane));
		}
	}

	private static String oitDiagnostics(int x, int y)
	{
		Object executor = Stage41bGeometryProbe.field(PRenderQueue.class, null, "EXECUTOR");
		Object depth = Stage41bGeometryProbe.field(executor.getClass(), executor, "worldOpaqueDepth");
		var buffers = (Map<?, ?>)Stage41bGeometryProbe.field(executor.getClass(), executor, "weightedBlendedOits");
		Object oit = buffers.get(depth);
		if (oit == null)
			return "no opaque snapshot entry";
		List<String> values = new ArrayList<>();
		values.add("metadata=" + Arrays.toString(texturePixel((GlTexture)Stage41bGeometryProbe.field(oit.getClass(), oit, "fabulousMetadataTexture"), x, y)));
		for (String name : List.of("layerDepthTextures", "revealageTextures", "accumulationTextures"))
			for (Object texture : (Object[])Stage41bGeometryProbe.field(oit.getClass(), oit, name))
				values.add(name + "=" + Arrays.toString(texturePixel((GlTexture)texture, x, y)));
		return values.toString();
	}

	private static float[] texturePixel(GlTexture texture, int x, int y)
	{
		int previous = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
		int fbo = GL30.glGenFramebuffers();
		try
		{
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, fbo);
			GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, texture.glId(), 0);
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

	private static PMeshRenderContext material(double x, PMeshRenderContext inherited)
	{
		int lane = (int)Math.round((x + 7.5) / 2);
		check(lane >= 0 && lane < 8, "Unexpected H14 world owner");
		SEEN[lane] = true;
		return inherited.withColor(0x80FF0000).withEmissive(true).withAlphaMode(PAlphaMode.TRANSLUCENT);
	}

	private static void shutdown(GameShuttingDownEvent event)
	{
		check(completed && cases == 18 * 8 * FRAMES_PER_PHASE, "All H14 actual-world phases must complete before shutdown");
		PLibDatabase.LOGGER.info("STAGE 4.2b.4 H14 SHUTDOWN PASS: cases={}, normal stop", cases);
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
			throw new AssertionError(message);
	}

	private record Layer(float depth, float[] color)
	{
	}

	public static final class EntityRenderer extends PEntityRenderer<TestEntity, PEntityRenderState.LivingImpl<TestEntity>>
	{
		public EntityRenderer(EntityRendererProvider.Context context)
		{
			super(context, MODEL, PRenderTypes.RenderTypeProvider::trianglesEmissiveTranslucent);
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
			super(MODEL, PRenderTypes.RenderTypeProvider::trianglesEmissiveTranslucent);
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
