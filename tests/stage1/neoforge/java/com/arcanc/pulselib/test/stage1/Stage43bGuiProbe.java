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
import com.arcanc.pulselib.content.registration.entity.TestEntity;
import com.arcanc.pulselib.content.registration.item.TestBlockItem;
import com.arcanc.pulselib.content.registration.item.renderer.renderState.TestBlockItemRenderState;
import com.arcanc.pulselib.content.renderer.PEntityRenderer;
import com.arcanc.pulselib.content.renderer.PItemRenderer;
import com.arcanc.pulselib.content.renderer.PRenderContexts;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.base.PEntityRenderState;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.content.renderer.plan.PFrameCompiler;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.PResourceCache;
import com.arcanc.pulselib.util.helpers.PLibHelper;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.event.GameShuttingDownEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.arcanc.pulselib.test.stage1.Stage41bGeometryProbe.field;

/** Native GuiRenderer -> PIP / item atlas -> shared Pulse backend, including deferred collector callbacks. */
public final class Stage43bGuiProbe
{
	private static final PRenderQueue.RenderStage SENTINEL = new PRenderQueue.RenderStage("stage43b_world_sentinel");
	private static final int FRAMES = 8;
	private static int ticks;
	private static int phase;
	private static int phaseStarted;
	private static int frames;
	private static int pixels;
	private static int guiEntities;
	private static int guiItems;
	private static int callbacks;
	private static int atlasCalls;
	private static int oversizedCalls;
	private static int recoveryTicks;
	private static boolean started;
	private static boolean advance;
	private static boolean sentinelArmed;
	static boolean failEntity;
	static boolean failItem;
	static boolean failCallback;
	private static final int[] PEAKS = new int[4];
	private static boolean finished;
	private static @Nullable CompletableFuture<Void> reload;
	private static ItemStack green = ItemStack.EMPTY;
	private static ItemStack yellow = ItemStack.EMPTY;
	private static final List<String> RECORDS = new ArrayList<>();

	private Stage43bGuiProbe()
	{
	}

	public static void register()
	{
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Stage43bGuiProbe::shutdown);
	}

	public static void tick()
	{
		var mc = Minecraft.getInstance();
		if (mc.level == null)
			return;
		if (++ticks > 1200)
			throw new AssertionError("Stage 4.3b GUI timeout");
		if (!started && ticks >= 65 && Stage42b4TranslucentProbe.MODEL.getModel() != null)
		{
			green = new ItemStack(Stage1Content.ItemReg.TEST_ITEM.get());
			yellow = green.copy();
			green.set(DataComponents.CUSTOM_NAME, Component.literal("green"));
			yellow.set(DataComponents.CUSTOM_NAME, Component.literal("yellow"));
			started = true;
			phaseStarted = ticks;
			mc.setScreen(new ProbeScreen());
		}
		if (advance)
		{
			advance = false;
			frames = 0;
			phase++;
			phaseStarted = ticks;
			if (phase == 2)
			{
				green.set(DataComponents.ITEM_MODEL, PLibDatabase.rl("stage43b_oversized"));
				yellow.set(DataComponents.ITEM_MODEL, PLibDatabase.rl("stage43b_oversized"));
				mc.setScreen(null);
				reload = mc.reloadResourcePacks();
			}
			if (phase == 4)
			{
				mc.setScreen(null);
				finished = true;
			}
		}
		if (phase == 2 && reload != null && reload.isDone() && mc.screen == null)
		{
			reload.join();
			phaseStarted = ticks;
			mc.setScreen(new ProbeScreen());
		}
		if (finished && recoveryTicks == 45)
			Screenshot.grab(mc.gameDirectory, "stage43b-world-recovery.png", mc.getMainRenderTarget(), 1,
					message -> PLibDatabase.LOGGER.info("{}", message.getString()));
		if (finished && ++recoveryTicks == 60)
		{
			Stage2PluginProbe.verify(true);
			if (Boolean.getBoolean("pulselib.stage43cGlintProbe"))
				Stage43cGlintProbe.finish();
			check(guiEntities >= 64 && guiItems >= 64 && callbacks >= 64 && atlasCalls > 0 && oversizedCalls > 0, "Missing native GUI owner/callback/atlas/PIP coverage");
			check(Stage1Client.itemSubmissions > 20 && Stage1Client.entitySubmissions > 20, "World and hand rendering must continue after GUI");
			PLibDatabase.LOGGER.info("STAGE 4.3b GUI COMPLETE: phases={}, regions={}, entity={}, item={}, deferred={}, atlas={}, oversized={}, world={}, hand/items={}", phase, pixels, guiEntities, guiItems, callbacks, atlasCalls, oversizedCalls, Stage1Client.entitySubmissions, Stage1Client.itemSubmissions);
			mc.stop();
		}
	}

	public static void atlas(boolean oversized)
	{
		if (oversized)
			oversizedCalls++;
		else
			atlasCalls++;
	}

	public static void beforeGui()
	{
		sentinelArmed = false;
		if (!(Minecraft.getInstance().screen instanceof ProbeScreen) || advance || finished || Minecraft.getInstance().getOverlay() != null)
			return;
		Stage43bGuiFaultProbe.verify(phase);
		sentinelArmed = true;
		PRenderQueue.submit(SENTINEL, PRenderTypes.RenderTypeProvider.trianglesEmissiveSolid(PResourceCache.ATLAS_LOCATION),
				Stage42aWorldStageProbe.quad(), new PInstanceHeader(new Matrix4f(), -1, 0xF000F0, 0));
	}

	public static void afterGui()
	{
		var mc = Minecraft.getInstance();
		if (!sentinelArmed)
			return;
		sentinelArmed = false;
		var compiler = (PFrameCompiler<PRenderQueue.RenderStage>)field(PRenderQueue.class, null, "COMPILER");
		var sentinel = compiler.compile(SENTINEL);
		check(sentinel.instances().size() == 1, "GUI must never consume the world sentinel");
		check(compiler.isEmpty(), "GUI Entity/Item/callback leaked submissions into world stages");
		if (!(mc.screen instanceof ProbeScreen) || mc.getOverlay() != null || ticks - phaseStarted < 20)
			return;
		var buffer = readMain();
		java.util.Arrays.fill(PEAKS, 0);
		int width = mc.getWindow().getWidth();
		int height = mc.getWindow().getHeight();
		int scale = mc.getWindow().getGuiScale();
		int red = count(buffer, width, height, scale, 16, 16, 80, 80, 0);
		int blue = count(buffer, width, height, scale, 112, 16, 144, 80, 2);
		int clipped = count(buffer, width, height, scale, 145, 16, 176, 80, 2);
		int greenPixels = count(buffer, width, height, scale, 200, 118, 240, 162, 1);
		int yellowPixels = count(buffer, width, height, scale, 260, 118, 300, 162, 3);
		int textPixels = count(buffer, width, height, scale, 16, 96, 330, 112, 4);
		check(textPixels > 100, "Vanilla GUI text must recover alongside PIP/atlas after reload; phase=" + phase + " pixels=" + textPixels);
		check(red > 20 && blue > 10 && clipped == 0 && greenPixels > 1 && yellowPixels > 1,
				"Native GUI pixels phase=" + phase + " red=" + red + " blue=" + blue + " clipped=" + clipped + " green=" + greenPixels + " yellow=" + yellowPixels);
		for (int peak : PEAKS)
			check(phase % 2 == 0 ? peak >= 245 : peak > 70 && peak < 245, "Native GUI alpha response phase=" + phase + " peaks=" + java.util.Arrays.toString(PEAKS));
		if (Boolean.getBoolean("pulselib.stage43cGlintProbe"))
			Stage43cGlintProbe.verify(phase);
		pixels += 6;
		RECORDS.add("phase=" + phase + ", frame=" + frames + ", rgbCounts=" + List.of(red, blue, clipped, greenPixels, yellowPixels, textPixels) + ", peaks=" + java.util.Arrays.toString(PEAKS));
		if (++frames == FRAMES)
		{
			PLibDatabase.LOGGER.info("STAGE 4.3b GUI PHASE PASS: phase={}, native={}, alpha={}, regions={}, RGBcounts={}, peaks={}", phase, phase < 2 ? "atlas" : "oversized PIP", phase % 2 == 1, pixels, List.of(red, blue, clipped, greenPixels, yellowPixels, textPixels), java.util.Arrays.toString(PEAKS));
			if (phase == 3)
				Screenshot.grab(mc.gameDirectory, "stage43b-gui.png", mc.getMainRenderTarget(), 1,
						message -> PLibDatabase.LOGGER.info("{}", message.getString()));
			advance = true;
		}
	}

	private static java.nio.ByteBuffer readMain()
	{
		var target = Minecraft.getInstance().getMainRenderTarget();
		int previous = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
		int fbo = GL30.glGenFramebuffers();
		try
		{
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, fbo);
			GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, ((GlTexture)target.getColorTexture()).glId(), 0);
			var rgba = BufferUtils.createByteBuffer(target.width * target.height * 4);
			GL11.glReadPixels(0, 0, target.width, target.height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, rgba);
			return rgba;
		}
		finally
		{
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previous);
			GL30.glDeleteFramebuffers(fbo);
		}
	}

	private static int count(java.nio.ByteBuffer pixels, int width, int height, int scale, int x0, int y0, int x1, int y1, int color)
	{
		int count = 0;
		for (int y = y0 * scale; y < Math.min(height, y1 * scale); y++)
			for (int x = x0 * scale; x < Math.min(width, x1 * scale); x++)
			{
				int offset = ((height - 1 - y) * width + x) * 4;
				int r = Byte.toUnsignedInt(pixels.get(offset));
				int g = Byte.toUnsignedInt(pixels.get(offset + 1));
				int b = Byte.toUnsignedInt(pixels.get(offset + 2));
				if (color == 0 && r > 70 && g < 12 && b < 12 || color == 1 && g > 70 && r < 12 && b < 12 ||
						color == 2 && b > 70 && r < 12 && g < 12 || color == 3 && r > 70 && g > 70 && b < 12 || color == 4 && r > 200 && g > 200 && b > 200)
				{
					count++;
					if (color < 4)
						PEAKS[color] = Math.max(PEAKS[color], color == 0 ? r : color == 1 ? g : color == 2 ? b : Math.min(r, g));
				}
			}
		return count;
	}

	private static PMeshRenderContext material(PMeshRenderContext inherited, int color)
	{
		return inherited.withColor(phase % 2 == 1 ? (color & 0xFFFFFF) | 0x80000000 : color).
				withEmissive(true).withAlphaMode(phase % 2 == 1 ? PAlphaMode.TRANSLUCENT : PAlphaMode.OPAQUE);
	}

	private static void shutdown(GameShuttingDownEvent event)
	{
		check(finished && phase == 4 && pixels == 192 && recoveryTicks >= 60 && Stage43bGuiFaultProbe.suites() == 2, "Complete four native GUI phases and recover to world before shutdown");
		PLibDatabase.LOGGER.info("STAGE 4.3b GUI SHUTDOWN PASS: {}", RECORDS);
	}

	private static void check(boolean value, String message)
	{
		if (!value)
			throw new AssertionError("Stage 4.3b: " + message);
	}

	private static final class ProbeScreen extends Screen
	{
		private ProbeScreen()
		{
			super(Component.literal("PulseLib native GUI regression"));
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
			var mc = Minecraft.getInstance();
			var entities = mc.level.entitiesForRendering();
			var owners = new ArrayList<TestEntity>();
			for (var entity : entities)
				if (entity instanceof TestEntity owner)
					owners.add(owner);
			owners.sort(Comparator.comparingDouble(TestEntity::getX));
			check(owners.size() >= 2, "Two actual Pulse entities required");
			for (int index = 0; index < 2; index++)
			{
				var state = mc.getEntityRenderDispatcher().extractEntity(owners.get(index), 0);
				state.shadowPieces.clear();
				state.outlineColor = 0;
				int x = index == 0 ? 16 : 112;
				if (index == 1)
					graphics.enableScissor(x, 16, 144, 80);
				graphics.entity(state, 16, new Vector3f(0, 1, 0), new Quaternionf().rotateZ((float)Math.PI), null, x, 16, x + 64, 80);
				if (index == 1)
					graphics.disableScissor();
			}
			if (Boolean.getBoolean("pulselib.stage43cGlintProbe"))
				Stage43cGlintProbe.extract(graphics, green);
			graphics.item(green, 212, 130);
			graphics.item(yellow, 272, 130);
			graphics.text(mc.font, "Native Entity PIP / clipped Entity / " + (phase < 2 ? "Item atlas" : "Oversized Item PIP"), 16, 100, -1, false);
		}
	}

	public static final class EntityRenderer extends PEntityRenderer<TestEntity, PEntityRenderState.LivingImpl<TestEntity>>
	{
		public EntityRenderer(EntityRendererProvider.Context context)
		{
			super(context, Stage42b4TranslucentProbe.MODEL, PRenderTypes.RenderTypeProvider::trianglesEmissiveSolid);
		}

		@Override
		public PEntityRenderState.LivingImpl<TestEntity> createRenderState()
		{
			return PLibHelper.livingRenderState();
		}

		@Override
		protected PMeshRenderContext resolveMeshRender(TestEntity owner, PBakedBone bone, PBakedMesh mesh, PMeshRenderContext inherited, float partialTick)
		{
			return material(inherited, owner.getX() < 0 ? 0xFFFF0000 : 0xFF0000FF);
		}

		@Override
		public void postSubmit(PoseStack poses, PEntityRenderState.LivingImpl<TestEntity> state, CameraRenderState camera, SubmitNodeCollector collector)
		{
			Stage1Client.entitySubmissions++;
			if (PRenderContexts.current() == PRenderContexts.Context.GUI)
				guiEntities++;
			if (failEntity)
				throw new IllegalArgumentException("stage43b entity failure");
		}
	}

	public static final class ItemRenderer extends PItemRenderer<TestBlockItem, TestBlockItemRenderState>
	{
		public ItemRenderer()
		{
			super(Stage42b4TranslucentProbe.MODEL, PRenderTypes.RenderTypeProvider::trianglesEmissiveSolid);
		}

		@Override
		protected TestBlockItemRenderState createRenderState()
		{
			return new TestBlockItemRenderState();
		}

		@Override
		public void getExtents(Consumer<Vector3fc> output)
		{
			float extent = phase < 2 ? 0.3f : 2;
			output.accept(new Vector3f(-extent));
			output.accept(new Vector3f(extent));
		}

		@Override
		public void preSubmit(PoseStack poses, TestBlockItemRenderState state, CameraRenderState camera, SubmitNodeCollector collector)
		{
			poses.translate(0, -0.25f, 0);
			poses.scale(0.25f, 0.25f, 0.25f);
		}

		@Override
		protected PMeshRenderContext resolveMeshRender(TestBlockItem owner, ItemStack stack, net.minecraft.world.item.ItemDisplayContext context,
		                                             PBakedBone bone, PBakedMesh mesh, PMeshRenderContext inherited, float partialTick)
		{
			return material(inherited, stack.getHoverName().getString().equals("yellow") ? 0xFFFFFF00 : 0xFF00FF00);
		}

		@Override
		public void postSubmit(PoseStack poses, TestBlockItemRenderState state, CameraRenderState camera, SubmitNodeCollector collector)
		{
			Stage1Client.itemSubmissions++;
			if (((com.arcanc.pulselib.content.mixin.ItemStackRenderStateAccessor)state.itemRenderState()).pulselib$getDisplayContext() != net.minecraft.world.item.ItemDisplayContext.GUI)
				return;
			guiItems++;
			if (Boolean.getBoolean("pulselib.stage43cGlintProbe"))
				Stage43cGlintProbe.item(state);
			if (failItem)
				throw new IllegalArgumentException("stage43b item failure");
			Matrix4f transform = new Matrix4f(poses.last().pose());
			collector.submitCustomGeometry(poses, PRenderTypes.RenderTypeProvider.trianglesEmissiveSolid(PResourceCache.ATLAS_LOCATION), (ignoredPose, ignoredVertices) ->
			{
				check(PRenderContexts.current() == PRenderContexts.Context.GUI, "Deferred GUI collector callback must execute inside the native local scope");
				callbacks++;
				if (failCallback)
					throw new IllegalArgumentException("stage43b callback failure");
				PRenderQueue.submitEntityMesh(PRenderTypes.RenderTypeProvider.trianglesEmissiveSolid(PResourceCache.ATLAS_LOCATION), Stage42aWorldStageProbe.quad(),
						new PInstanceHeader(new Matrix4f(transform).translate(0, 0, 0.1f).scale(0.1f), 0xFFFF00FF, 0xF000F0, 0));
			});
		}
	}

	public record Unbaked() implements SpecialModelRenderer.Unbaked<TestBlockItemRenderState>
	{
		public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

		@Override
		public ItemRenderer bake(SpecialModelRenderer.BakingContext context)
		{
			return new ItemRenderer();
		}

		@Override
		public MapCodec<Unbaked> type()
		{
			return MAP_CODEC;
		}
	}
}
