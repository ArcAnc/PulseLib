/**
 * @author ArcAnc
 * Created at: 25.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage61client;

import com.arcanc.pulselib.api.registration.PModelRegistrar;
import com.arcanc.pulselib.content.animatable.AnimManagerKey;
import com.arcanc.pulselib.content.animatable.PAnimatable;
import com.arcanc.pulselib.content.animatable.PAnimationController;
import com.arcanc.pulselib.content.animatable.PAnimationManager;
import com.arcanc.pulselib.content.animatable.instance.InstanceAnimationManager;
import com.arcanc.pulselib.content.model.animation.PAnimationGraph;
import com.arcanc.pulselib.content.model.animation.PAnimationState;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.renderer.PModelGuiRenderState;
import com.arcanc.pulselib.content.renderer.PModelGuiRenderer;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.content.renderer.plan.PFrameCompiler;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.data.gecko.PGeckoModelLoader;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.PResourceCache;
import com.arcanc.pulselib.util.helpers.PLibHelper;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Actual helper extraction -> registered native PIP pool -> shared GPU renderer, with reload/recovery. */
public final class GuiProbe
{
	public static final PModelData MODEL = PModelData.entity(PLibDatabase.rl("stage43b2_owner"), PGeckoModelLoader.INSTANCE.id());
	private static final PModelData MISSING = PModelData.entity(PLibDatabase.rl("stage43b2_missing"));
	private static final PRenderQueue.RenderStage SENTINEL = new PRenderQueue.RenderStage("stage43b2_world_sentinel");
	private static final Set<Object> FRAME_RENDERERS = Collections.newSetFromMap(new IdentityHashMap<>());
	private static final Set<Object> ALL_RENDERERS = Collections.newSetFromMap(new IdentityHashMap<>());
	private static int nativePreparations;
	private static final List<String> RECORDS = new ArrayList<>();
	private static int ticks;
	private static int phase;
	private static int frames;
	private static int pixels;
	private static int phaseStarted;
	private static int recoveryTicks;
	private static int faultSuites;
	private static boolean advance;
	private static boolean armed;
	private static boolean started;
	private static boolean finished;
	private static @Nullable CompletableFuture<Void> reload;
	private static @Nullable PBakedModel beforeReload;

	private GuiProbe()
	{
	}

	public static void register()
	{
		ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> shutdown());
	}

	public static void models(PModelRegistrar registrar)
	{
		registrar.model(MODEL).texture("0", PLibDatabase.rl("lifecycle/stage62a_white"));
	}

	/** Actual native PiP pool instances only; direct fault/recovery renderers are not counted. */
	public static void prepared(Object renderer)
	{
		if (armed)
		{
			FRAME_RENDERERS.add(renderer);
			ALL_RENDERERS.add(renderer);
			nativePreparations++;
		}
	}

	public static void tick()
	{
		var mc = Minecraft.getInstance();
		check(Files.isRegularFile(mc.gameDirectory.toPath().resolve(".pulselib-gui")), "Disposable GUI client marker");
		if (++ticks > 1200)
			throw new AssertionError("GUI helper timeout");
		if (!started && ticks >= 30 && MODEL.getModel() != null && mc.getOverlay() == null)
		{
			started = true;
			beforeReload = MODEL.getModel();
			phaseStarted = ticks;
			mc.setScreen(new ProbeScreen());
		}
		if (advance)
		{
			advance = false;
			frames = 0;
			phase++;
			phaseStarted = ticks;
			mc.setScreen(new net.minecraft.client.gui.screens.TitleScreen());
			if (phase == 1)
				reload = mc.reloadResourcePacks();
			else
				finished = true;
		}
		if (phase == 1 && reload != null && reload.isDone() && !(mc.screen instanceof ProbeScreen) && mc.getOverlay() == null)
		{
			reload.join();
			check(MODEL.getModel() != beforeReload, "Reload must replace the baked helper model");
			reload = null;
			phaseStarted = ticks;
			mc.setScreen(new ProbeScreen());
		}
		if (finished && ++recoveryTicks == 10)
		{
			check(ClientProbe.common == 1 && ClientProbe.client == 1 && ClientProbe.models == 2, "Once-only client registration and two resource collections");
			try
			{
				Files.writeString(mc.gameDirectory.toPath().resolve("gui-results.json"), new GsonBuilder().setPrettyPrinting().create().toJson(
						Map.of("status", "PASS", "epochs", phase, "regions", pixels, "faultSuites", faultSuites,
								"nativePreparations", nativePreparations, "pooledRenderers", ALL_RENDERERS.size(), "records", RECORDS)));
			}
			catch (java.io.IOException exception)
			{
				throw new AssertionError(exception);
			}
			PLibDatabase.LOGGER.info("STAGE62C1 GUI COMPLETE: regions={}, faults={}, nativePreparations={}, pooledRenderers={}", pixels, faultSuites, nativePreparations, ALL_RENDERERS.size());
			mc.stop();
		}
	}

	public static void beforeGui()
	{
		armed = false;
		var mc = Minecraft.getInstance();
		if (!(mc.screen instanceof ProbeScreen) || advance || finished || mc.getOverlay() != null)
			return;
		if (faultSuites == phase)
			faults();
		FRAME_RENDERERS.clear();
		PRenderQueue.submit(SENTINEL, PRenderTypes.RenderTypeProvider.trianglesEmissiveSolid(PResourceCache.ATLAS_LOCATION),
				quad(), new PInstanceHeader(new Matrix4f(), -1, 0xF000F0, 0));
		armed = true;
	}

	public static void afterGui()
	{
		if (!armed)
			return;
		armed = false;
		check(FRAME_RENDERERS.size() == 6, "Native registered PiP factory must provide six independent renderers per GUI frame: " + FRAME_RENDERERS.size());
		var mc = Minecraft.getInstance();
		var compiler = (PFrameCompiler<PRenderQueue.RenderStage>)field(PRenderQueue.class, null, "COMPILER");
		check(compiler.compile(SENTINEL).instances().size() == 1, "Helper must preserve pending world submissions");
		check(compiler.isEmpty(), "Helper must not leak submissions into world stages");
		if (ticks - phaseStarted < 20)
			return;
		var target = mc.getMainRenderTarget();
		int previous = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
		int rowLength = GL11.glGetInteger(GL11.GL_PACK_ROW_LENGTH);
		GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, 0);
		int fbo = GL30.glGenFramebuffers();
		var buffer = BufferUtils.createByteBuffer(target.width * target.height * 4);
		try
		{
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, fbo);
			GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, ((GlTexture)target.getColorTexture()).glId(), 0);
			GL11.glReadPixels(0, 0, target.width, target.height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
		}
		finally
		{
			GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, rowLength);
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previous);
			GL30.glDeleteFramebuffers(fbo);
		}
		if (frames == 0)
			Screenshot.grab(mc.gameDirectory, "stage62c1-phase" + phase + ".png", target, 1, message -> PLibDatabase.LOGGER.info("{}", message.getString()));
		int scale = mc.getWindow().getGuiScale();
		int[][] samples = {{96,40,255,0,0}, {64,40,0,0,0}, {152,24,0,0,255}, {136,24,0,0,0},
				{220,40,0,255,0}, {280,160,192,0,63}, {248,160,0,0,0}, {81,25,255,0,0}, {110,54,255,0,0}, {220,70,0,255,0}, {220,74,0,0,0}, {64,156,77,0,178}};
		List<String> actual = new ArrayList<>();
		for (int[] sample : samples)
		{
			int offset = ((target.height - 1 - sample[1] * scale) * target.width + sample[0] * scale) * 4;
			List<Integer> rgb = List.of(Byte.toUnsignedInt(buffer.get(offset)), Byte.toUnsignedInt(buffer.get(offset + 1)), Byte.toUnsignedInt(buffer.get(offset + 2)));
			for (int channel = 0; channel < 3; channel++)
				check(Math.abs(rgb.get(channel) - sample[channel + 2]) <= 3, "Helper GPU phase=" + phase + " xy=" + sample[0] + "," + sample[1] + " actual=" + rgb + " expected=" + java.util.Arrays.toString(sample));
			actual.add(rgb.toString());
			pixels++;
		}
		int textPixels = 0;
		for (int y = 96 * scale; y < 112 * scale; y++)
			for (int x = 16 * scale; x < Math.min(target.width, 330 * scale); x++)
			{
				int offset = ((target.height - 1 - y) * target.width + x) * 4;
				if (Byte.toUnsignedInt(buffer.get(offset)) > 200 && Byte.toUnsignedInt(buffer.get(offset + 1)) > 200 && Byte.toUnsignedInt(buffer.get(offset + 2)) > 200)
					textPixels++;
			}
		check(textPixels > 100, "Vanilla GUI text must remain visible after helper/reload");
		pixels++;
		RECORDS.add("phase=" + phase + " frame=" + frames + " rgb=" + actual + " text=" + textPixels);
		if (++frames == 8)
		{
			PLibDatabase.LOGGER.info("STAGE62C1 HELPER PHASE PASS: phase={}, pixels={}, actual={}", phase, pixels, actual);
			if (phase == 1)
				Screenshot.grab(mc.gameDirectory, "stage62c1-helper.png", target, 1, message -> PLibDatabase.LOGGER.info("{}", message.getString()));
			advance = true;
		}
	}

	private static void faults()
	{
		int incomingPackRowLength = GL11.glGetInteger(GL11.GL_PACK_ROW_LENGTH);
		var mc = Minecraft.getInstance();
		var gui = new GuiRenderState();
		var graphics = new GuiGraphicsExtractor(mc, gui, 0, 0);
		var originalPose = new Matrix3x2f(graphics.pose());
		var lights = RenderSystem.getShaderLights();
		var color = RenderSystem.outputColorTextureOverride;
		var depth = RenderSystem.outputDepthTextureOverride;
		var projection = RenderSystem.getProjectionMatrixBuffer();
		var modelView = new Matrix4f(RenderSystem.getModelViewMatrix());
		PLibHelper.<Owner>renderModelInGui(graphics, MISSING, List.of(), PRenderTypes.RenderTypeProvider::trianglesEmissiveSolid, 20, 20, new Vector3f(16), -1, 0xF000F0, OverlayTexture.NO_OVERLAY, 1);
		gui.forEachPictureInPicture(state -> { throw new AssertionError("Unloaded model contributed state"); });
		try
		{
			PLibHelper.<Owner>renderModelInGui(graphics, MODEL, List.of(), PRenderTypes.RenderTypeProvider::trianglesEmissiveSolid, 20, 20, new Vector3f(Float.NaN), -1, 0xF000F0, OverlayTexture.NO_OVERLAY, 1);
			throw new AssertionError("Invalid transform accepted");
		}
		catch (IllegalArgumentException expected)
		{
			check(graphics.pose().equals(originalPose), "Extraction failure changed GUI pose");
		}
		PLibHelper.<Owner>renderModelInGui(graphics, MODEL, List.of(), id -> { throw new IllegalArgumentException("helper execution fault"); }, 24, 24, new Vector3f(16), -1, 0xF000F0, OverlayTexture.NO_OVERLAY, 1);
		List<PModelGuiRenderState> states = new ArrayList<>();
		gui.forEachPictureInPicture(state -> states.add((PModelGuiRenderState)state));
		check(states.size() == 1, "Missing helper extraction state");
		int nativeTexture;
		try (var renderer = new PModelGuiRenderer(mc.renderBuffers().bufferSource()))
		{
			try
			{
				renderer.prepare(states.getFirst(), new GuiRenderState(), 1);
				throw new AssertionError("Expected native helper draw failure");
			}
			catch (IllegalArgumentException expected)
			{
				check(expected.getMessage().equals("helper execution fault"), "Wrong native helper failure");
			}
			check(RenderSystem.getShaderLights() == lights && RenderSystem.outputColorTextureOverride == color && RenderSystem.outputDepthTextureOverride == depth &&
					RenderSystem.getProjectionMatrixBuffer() == projection && modelView.equals(RenderSystem.getModelViewMatrix()), "Helper failure did not restore caller");
			var failedTexture = (GlTexture)field(net.minecraft.client.gui.render.pip.PictureInPictureRenderer.class, renderer, "texture");
			check(alpha(failedTexture) == 0, "Failed helper must leave its cleared native target empty");
			var state = states.getFirst();
			var recovered = new PModelGuiRenderState(state.model(), state.animationPose(), state.transform(), PRenderTypes.RenderTypeProvider::trianglesEmissiveSolid,
					state.color(), state.packedLight(), state.packedOverlay(), state.x0(), state.y0(), state.x1(), state.y1(), state.scissorArea(), state.bounds());
			renderer.prepare(recovered, new GuiRenderState(), 1);
			var texture = (GlTexture)field(net.minecraft.client.gui.render.pip.PictureInPictureRenderer.class, renderer, "texture");
			nativeTexture = texture.glId();
			check(GL11.glIsTexture(nativeTexture) && alpha(texture) > 20, "Following actual helper draw must recover GPU pixels");
		}
		check(!GL11.glIsTexture(nativeTexture), "Native PIP close must delete its texture");
		check(RenderSystem.getShaderLights() == lights && GL11.glGetError() == GL11.GL_NO_ERROR, "Helper recovery lights/GL");
		faultSuites++;
		PLibDatabase.LOGGER.info("STAGE62C1 HELPER FAULT PASS: suites={}, incomingPackRowLength={}, missing model, invalid extraction, native execution exception, following PIP recovery, texture close, light/projection/pose/target restoration", faultSuites, incomingPackRowLength);
	}

	private static Object field(Class<?> type, @Nullable Object owner, String name)
	{
		try
		{
			var field = type.getDeclaredField(name);
			field.setAccessible(true);
			return java.util.Objects.requireNonNull(field.get(owner));
		}
		catch (ReflectiveOperationException exception)
		{
			throw new AssertionError(exception);
		}
	}

	private static int alpha(GlTexture texture)
	{
		int previous = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
		int rowLength = GL11.glGetInteger(GL11.GL_PACK_ROW_LENGTH);
		GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, 0);
		int fbo = GL30.glGenFramebuffers();
		try
		{
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, fbo);
			GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, texture.glId(), 0);
			int width = texture.getWidth(0);
			int height = texture.getHeight(0);
			var rgba = BufferUtils.createByteBuffer(width * height * 4);
			GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, rgba);
			int count = 0;
			for (int index = 3; index < rgba.limit(); index += 4)
				if (Byte.toUnsignedInt(rgba.get(index)) > 20)
					count++;
			return count;
		}
		finally
		{
			GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, rowLength);
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previous);
			GL30.glDeleteFramebuffers(fbo);
		}
	}

	private static void shutdown()
	{
		check(finished && phase == 2 && pixels == 208 && faultSuites == 2 && recoveryTicks >= 10, "Incomplete helper run");
		PLibDatabase.LOGGER.info("STAGE62C1 HELPER SHUTDOWN PASS: {}", RECORDS);
	}

	private static void check(boolean value, String text)
	{
		if (!value)
			throw new AssertionError("STAGE62C1: " + text);
	}

	private static PGeometryData quad()
	{
		var sprite = PResourceCache.getTextureAtlas().getSprite(PResourceCache.spriteId(PLibDatabase.rl("lifecycle/stage62a_white")));
		check(sprite.contents().name().equals(PResourceCache.spriteId(PLibDatabase.rl("lifecycle/stage62a_white"))), "White control sprite must be present after each reload");
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

	private static final class Owner implements PAnimatable<Owner>
	{
		private final InstanceAnimationManager<Owner> manager = new InstanceAnimationManager<>(this);

		@Override
		public PAnimationManager<Owner> getAnimationManager(AnimManagerKey key)
		{
			return this.manager.getManager(key);
		}

		@Override
		public void registerAnimationControllers(PAnimationManager.PAnimationRegistrar<Owner> registrar)
		{
		}
	}

	private static final class ProbeScreen extends Screen
	{
		private ProbeScreen()
		{
			super(Component.literal("Pulse GUI helper regression"));
		}

		@Override
		public boolean isPauseScreen()
		{
			return false;
		}

		@Override
		public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick)
		{
			graphics.fill(0, 0, this.width, this.height, 0xFF000000);
			var controller = new PAnimationController<Owner>(new PAnimationGraph(List.of(new PAnimationState.Clip("shift", "shift")), List.of()));
			controller.graphRuntime().tickPrecise(MODEL.getModel(), 0);
			var controllers = new ArrayList<PAnimationController<Owner>>();
			controllers.add(controller);
			PLibHelper.renderModelInGui(graphics, MODEL, controllers, PRenderTypes.RenderTypeProvider::trianglesEmissiveSolid,
					64, 24, new Vector3f(32, 16, 16), 0xFFFF0000, 0xF000F0, OverlayTexture.NO_OVERLAY, 1);
			controllers.clear();
			var pose = new Matrix3x2f(graphics.pose());
			graphics.enableScissor(144, 16, 160, 40);
			graphics.pose().pushMatrix().translate(160, 24).rotate((float)Math.PI / 2);
			PLibHelper.<Owner>renderModelInGui(graphics, MODEL, List.of(), PRenderTypes.RenderTypeProvider::trianglesEmissiveSolid,
					0, 0, new Vector3f(16), 0xFF0000FF, 0xF000F0, OverlayTexture.NO_OVERLAY, 1);
			graphics.pose().popMatrix();
			graphics.disableScissor();
			graphics.pose().pushMatrix().translate(220, 24).scale(-1, 1.5f);
			var scale = new Vector3f(32, 16, 16);
			PLibHelper.<Owner>renderModelInGui(graphics, MODEL, List.of(), PRenderTypes.RenderTypeProvider::trianglesEmissiveSolid,
					0, 0, scale, 0xFF00FF00, 0xF000F0, OverlayTexture.NO_OVERLAY, 1);
			scale.zero();
			graphics.pose().popMatrix();
			PLibHelper.<Owner>renderModelInGui(graphics, MODEL, List.of(), PRenderTypes.RenderTypeProvider::trianglesEmissiveSolid,
					280, 144, new Vector3f(32, 16, 16), 0xFF0000FF, 0xF000F0, OverlayTexture.NO_OVERLAY, 1);
			PLibHelper.<Owner>renderModelInGui(graphics, MODEL, List.of(), PRenderTypes.RenderTypeProvider::trianglesEmissiveTranslucent,
					280, 144, new Vector3f(32, 16, 16), 0x80FF0000, 0xF000F0, OverlayTexture.NO_OVERLAY, 1);
			// Zero selects Minecraft's red hurt overlay; it is deliberately different from NO_OVERLAY.
			PLibHelper.<Owner>renderModelInGui(graphics, MODEL, List.of(), PRenderTypes.RenderTypeProvider::trianglesEmissiveSolid,
					64, 140, new Vector3f(32, 16, 16), 0xFF0000FF, 0xF000F0, 0, 1);
			check(pose.equals(graphics.pose()), "Helper must preserve caller GUI pose");
			graphics.text(Minecraft.getInstance().font, "Canonical GUI helper: animation / scissor / mirrored scale / alpha", 16, 100, -1, false);
		}
	}
}
