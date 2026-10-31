/**
 * @author ArcAnc
 * Created at: 25.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage62item;

import com.arcanc.pulselib.content.model.baked.PBakedBone;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.model.baked.PBakedMesh;
import com.arcanc.pulselib.content.model.baked.PMeshRenderContext;
import com.arcanc.pulselib.content.model.textures.PAlphaMode;
import com.arcanc.pulselib.content.renderer.PItemRenderer;
import com.arcanc.pulselib.content.renderer.base.PItemRenderState;
import com.arcanc.pulselib.api.plugin.IPulseClientPlugin;
import com.arcanc.pulselib.api.plugin.PulsePlugin;
import com.arcanc.pulselib.api.registration.PModelRegistrar;
import com.arcanc.pulselib.data.gecko.PGeckoModelLoader;
import com.arcanc.pulselib.content.animatable.PItemStackAnimationData;
import com.arcanc.pulselib.test.stage62item.mixin.SpecialTypesAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.resources.Identifier;
import java.nio.file.Files;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.arcanc.pulselib.content.renderer.PRenderContexts;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.content.renderer.plan.PFrameCompiler;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.PResourceCache;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;


/** Native GuiRenderer -> PIP / item atlas -> shared Pulse backend, including deferred collector callbacks. */
public final class ItemClient implements ClientModInitializer
{
	private static final PRenderQueue.RenderStage SENTINEL = new PRenderQueue.RenderStage("stage43b_world_sentinel");
	private static final int FRAMES = 8;
	private static int ticks;
	private static int phase;
	private static int phaseStarted;
	private static int frames;
	private static int pixels;
	private static int guiItems;
	private static int callbacks;
	private static int atlasCalls;
	private static int oversizedCalls;
	private static int recoveryTicks;
	private static boolean started;
	private static boolean advance;
	private static boolean sentinelArmed;
	static boolean failItem;
	static boolean failCallback;
	private static final int[] PEAKS = new int[4];
	private static final int[] NATIVE_BY_PHASE = new int[8];
	public static final PModelData MODEL = PModelData.entity(PLibDatabase.rl("stage42b4_owner"), PGeckoModelLoader.INSTANCE.id());
	private static int packets;
	private static boolean clicked;
	private static boolean identity;
	private static int models;
	private static int hudItems;
	private static boolean finished;
	private static @Nullable CompletableFuture<Void> reload;
	private static @Nullable PBakedModel beforeReload;
	private static ItemStack green = ItemStack.EMPTY;
	private static ItemStack yellow = ItemStack.EMPTY;
	private static final List<String> RECORDS = new ArrayList<>();

	public ItemClient()
	{
	}

	@Override
	public void onInitializeClient()
	{
		SpecialTypesAccessor.types().put(PLibDatabase.rl("stage62item"), Unbaked.MAP_CODEC);
		ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
		ClientLifecycleEvents.CLIENT_STOPPING.register(client ->
		{
			shutdown();
			ItemIdentityProbe.shutdown();
		});
	}

	public static void packet(ClientboundContainerSetContentPacket packet)
	{
		for (var stack : packet.items())
			if (stack.is(ItemFixture.ITEM))
			{
				check(((PItemStackAnimationData)(Object)stack).pulselib$getAnimationId() == 0, "Actual decoded container packet must have no runtime identity");
				packets++;
			}
	}

	@PulsePlugin
	public static final class Plugin implements IPulseClientPlugin
	{
		@Override
		public Identifier getPluginId()
		{
			return PLibDatabase.rl("stage62item/client");
		}

		@Override
		public void registerModels(PModelRegistrar registrar)
		{
			models++;
			registrar.model(MODEL).texture("0", PLibDatabase.rl("lifecycle/stage62a_white"));
		}
	}

	public static void tick()
	{
		var mc = Minecraft.getInstance();
		check(Files.isRegularFile(mc.gameDirectory.toPath().resolve(".pulselib-items")), "Disposable item client marker");
		if (++ticks > 2400)
			throw new AssertionError("STAGE62C2 item consumer timeout");
		if (mc.level == null || mc.player == null || mc.getOverlay() != null)
			return;
		if (!identity && MODEL.getModel() != null)
		{
			ItemIdentityProbe.verify();
			identity = true;
		}
		if (!clicked && mc.player.containerMenu instanceof ChestMenu menu && menu.getSlot(0).getItem().is(ItemFixture.ITEM))
		{
			var source = menu.getSlot(0).getItem();
			long id = ((PItemStackAnimationData)(Object)source).pulselib$getOrCreateAnimationId();
			mc.gameMode.handleContainerInput(menu.containerId, 0, 0, ContainerInput.QUICK_MOVE, mc.player);
			check(menu.getSlot(0).getItem().isEmpty(), "Client native quick-move emptied source");
			check(((PItemStackAnimationData)(Object)menu.getSlot(27 + 35).getItem()).pulselib$getOrCreateAnimationId() == id, "Native menu temporary copies must preserve identity before network replacement");
			clicked = true;
		}
		if (!clicked || packets < 2)
			return;
		if (!started && ticks >= 30 && MODEL.getModel() != null)
		{
			green = new ItemStack(ItemFixture.ITEM);
			yellow = green.copy();
			green.set(DataComponents.CUSTOM_NAME, Component.literal("green"));
			green.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
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
			if (phase == 2 || phase == 6)
			{
				green.set(DataComponents.ITEM_MODEL, PLibDatabase.rl("stage62item_oversized"));
				yellow.set(DataComponents.ITEM_MODEL, PLibDatabase.rl("stage62item_oversized"));
			}
			if (phase == 4)
			{
				green.set(DataComponents.ITEM_MODEL, PLibDatabase.rl("stage62item"));
				yellow.set(DataComponents.ITEM_MODEL, PLibDatabase.rl("stage62item"));
				mc.setScreen(null);
				beforeReload = MODEL.getModel();
				reload = mc.reloadResourcePacks();
			}
			if (phase == 8)
			{
				mc.setScreen(null);
				finished = true;
			}
		}
		if (phase == 4 && reload != null && reload.isDone() && mc.screen == null)
		{
			reload.join();
			check(MODEL.getModel() != beforeReload, "Reload replaces the baked item model");
			phaseStarted = ticks;
			mc.setScreen(new ProbeScreen());
		}
		if (finished && recoveryTicks == 45)
			Screenshot.grab(mc.gameDirectory, "stage62c2-recovery.png", mc.getMainRenderTarget(), 1,
					message -> PLibDatabase.LOGGER.info("{}", message.getString()));
		if (finished && ++recoveryTicks == 60)
		{
			check(models == 2 && identity && clicked && packets >= 2, "Reload collection, native identity and menu/network witnesses");
			check(guiItems >= 64 && callbacks >= 64 && atlasCalls > 0 && oversizedCalls > 0, "Native item/callback/atlas/PiP coverage");
			writeResults();
			PLibDatabase.LOGGER.info("STAGE62C2 HUD OBSERVER: excluded {} unnamed HUD/other items", hudItems);
			PLibDatabase.LOGGER.info("STAGE62C2 GUI COMPLETE: phases={}, regions={}, items={}, deferred={}, atlas={}, oversized={}, packets={}", phase, pixels, guiItems, callbacks, atlasCalls, oversizedCalls, packets);
			mc.stop();
		}
	}


	public static void beforeGui()
	{
		sentinelArmed = false;
		if (!(Minecraft.getInstance().screen instanceof ProbeScreen) || advance || finished || Minecraft.getInstance().getOverlay() != null)
			return;
		ItemFaultProbe.verify(phase);
		sentinelArmed = true;
		PRenderQueue.submit(SENTINEL, PRenderTypes.RenderTypeProvider.trianglesEmissiveSolid(PResourceCache.ATLAS_LOCATION),
				quad(), new PInstanceHeader(new Matrix4f(), -1, 0xF000F0, 0));
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
		check(compiler.isEmpty(), "GUI item/callback leaked submissions into world stages");
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
		check(red == 0 && blue == 0 && clipped == 0 && greenPixels > 1 && yellowPixels > 1,
				"Native GUI pixels phase=" + phase + " red=" + red + " blue=" + blue + " clipped=" + clipped + " green=" + greenPixels + " yellow=" + yellowPixels);
		for (int index : new int[] {1, 3})
			check(phase % 2 == 0 ? PEAKS[index] >= 245 : PEAKS[index] > 70 && PEAKS[index] < 245, "Native GUI alpha response phase=" + phase + " peaks=" + java.util.Arrays.toString(PEAKS));
		pixels += 6;
		RECORDS.add("phase=" + phase + ", frame=" + frames + ", rgbCounts=" + List.of(red, blue, clipped, greenPixels, yellowPixels, textPixels) + ", peaks=" + java.util.Arrays.toString(PEAKS));
		if (++frames == FRAMES)
		{
			check(NATIVE_BY_PHASE[phase] > 0, "Expected native atlas/oversized path in every phase");
			PLibDatabase.LOGGER.info("STAGE62C2 GUI PHASE PASS: phase={}, native={}, alpha={}, regions={}, RGBcounts={}, peaks={}", phase, phase % 4 < 2 ? "atlas" : "oversized PIP", phase % 2 == 1, pixels, List.of(red, blue, clipped, greenPixels, yellowPixels, textPixels), java.util.Arrays.toString(PEAKS));
			if (phase == 7)
				Screenshot.grab(mc.gameDirectory, "stage62c2-items.png", mc.getMainRenderTarget(), 1,
						message -> PLibDatabase.LOGGER.info("{}", message.getString()));
			advance = true;
		}
	}

	private static java.nio.ByteBuffer readMain()
	{
		var target = Minecraft.getInstance().getMainRenderTarget();
		int previous = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
		int rowLength = GL11.glGetInteger(GL11.GL_PACK_ROW_LENGTH);
		GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, 0);
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
			GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, rowLength);
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

	private static void shutdown()
	{
		check(atlasCalls > 0 && oversizedCalls > 0 && callbacks > 0 && finished && phase == 8 && pixels == 384 && recoveryTicks >= 60 && ItemFaultProbe.suites() == 2, "Complete eight native GUI phases and recover to world before shutdown");
		PLibDatabase.LOGGER.info("STAGE62C2 GUI SHUTDOWN PASS: {}", RECORDS);
	}

	private static void check(boolean value, String message)
	{
		if (!value)
			throw new AssertionError("STAGE62C2: " + message);
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
			graphics.item(green, 212, 130);
			graphics.item(yellow, 272, 130);
			graphics.text(mc.font, "Fabric native items / " + (phase % 4 < 2 ? "Item atlas" : "Oversized Item PIP"), 16, 100, -1, false);
		}
	}

	public static final class ItemRenderer extends PItemRenderer<ItemFixture.AnimatedItem, PItemRenderState.Impl<ItemFixture.AnimatedItem>>
	{
		public ItemRenderer()
		{
			super(MODEL, PRenderTypes.RenderTypeProvider::trianglesEmissiveSolid);
		}

		@Override
		protected PItemRenderState.Impl<ItemFixture.AnimatedItem> createRenderState()
		{
			return new PItemRenderState.Impl<>();
		}

		@Override
		public void getExtents(Consumer<Vector3fc> output)
		{
			float extent = phase % 4 < 2 ? 0.3f : 2;
			output.accept(new Vector3f(-extent));
			output.accept(new Vector3f(extent));
		}

		@Override
		public void preSubmit(PoseStack poses, PItemRenderState.Impl<ItemFixture.AnimatedItem> state, CameraRenderState camera, SubmitNodeCollector collector)
		{
			poses.translate(0, -0.25f, 0);
			poses.scale(0.25f, 0.25f, 0.25f);
		}

		@Override
		protected PMeshRenderContext resolveMeshRender(ItemFixture.AnimatedItem owner, ItemStack stack, net.minecraft.world.item.ItemDisplayContext context,
		                                             PBakedBone bone, PBakedMesh mesh, PMeshRenderContext inherited, float partialTick)
		{
			return material(inherited, stack.getHoverName().getString().equals("yellow") ? 0xFFFFFF00 : 0xFF00FF00);
		}

		@Override
		public void postSubmit(PoseStack poses, PItemRenderState.Impl<ItemFixture.AnimatedItem> state, CameraRenderState camera, SubmitNodeCollector collector)
		{

			if (((com.arcanc.pulselib.content.mixin.ItemStackRenderStateAccessor)state.itemRenderState()).pulselib$getDisplayContext() != net.minecraft.world.item.ItemDisplayContext.GUI)
				return;
			guiItems++;
			check(state.hasFoil() == state.stack().hasFoil(), "Special item foil forwarding");
			if (failItem)
				throw new IllegalArgumentException("stage43b item failure");
			Matrix4f transform = new Matrix4f(poses.last().pose());
			collector.submitCustomGeometry(poses, PRenderTypes.RenderTypeProvider.trianglesEmissiveSolid(PResourceCache.ATLAS_LOCATION), (ignoredPose, ignoredVertices) ->
			{
				check(PRenderContexts.current() == PRenderContexts.Context.GUI, "Deferred GUI collector callback must execute inside the native local scope");
				callbacks++;
				if (failCallback)
					throw new IllegalArgumentException("stage43b callback failure");
				PRenderQueue.submitEntityMesh(PRenderTypes.RenderTypeProvider.trianglesEmissiveSolid(PResourceCache.ATLAS_LOCATION), quad(),
						new PInstanceHeader(new Matrix4f(transform).translate(0, 0, 0.1f).scale(0.1f), 0xFFFF00FF, 0xF000F0, 0));
			});
		}
	}

	public record Unbaked() implements SpecialModelRenderer.Unbaked<PItemRenderState.Impl<ItemFixture.AnimatedItem>>
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

	public static @Nullable Object field(Class<?> type, @Nullable Object owner, String name)
	{
		try
		{
			var field = type.getDeclaredField(name);
			field.setAccessible(true);
			return field.get(owner);
		}
		catch (ReflectiveOperationException exception)
		{
			throw new AssertionError(exception);
		}
	}

	public static PGeometryData quad()
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


	private static void writeResults()
	{
		try
		{
			Files.writeString(Minecraft.getInstance().gameDirectory.toPath().resolve("item-results.json"), new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(
					java.util.Map.of("status", "PASS", "phases", phase, "regions", pixels, "packets", packets, "nativeAtlas", atlasCalls, "nativeOversized", oversizedCalls, "callbacks", callbacks, "faultSuites", ItemFaultProbe.suites(), "records", RECORDS, "nativeByPhase", NATIVE_BY_PHASE)));
		}
		catch (java.io.IOException exception)
		{
			throw new AssertionError(exception);
		}
	}

	public static void nativeItem(boolean oversized, net.minecraft.client.renderer.item.ItemStackRenderState state)
	{
		if (!sentinelArmed)
			return;
		var layers = (net.minecraft.client.renderer.item.ItemStackRenderState.LayerRenderState[])field(net.minecraft.client.renderer.item.ItemStackRenderState.class, state, "layers");
		for (int index = 0; index < (Integer)field(net.minecraft.client.renderer.item.ItemStackRenderState.class, state, "activeLayerCount"); index++)
		{
			@Nullable Object argument = field(net.minecraft.client.renderer.item.ItemStackRenderState.LayerRenderState.class, layers[index], "argumentForSpecialRendering");
			if (argument instanceof PItemRenderState<?> item && (item.stack().getHoverName().getString().equals("green") || item.stack().getHoverName().getString().equals("yellow")))
			{
				if (ticks - phaseStarted < 20)
					return;
				check(oversized == state.isOversizedInGui() && oversized == (phase % 4 >= 2), "Named native item route phase=" + phase + " oversized=" + oversized + " metadata=" + state.isOversizedInGui());
				NATIVE_BY_PHASE[phase]++;
				if (oversized)
					oversizedCalls++;
				else
					atlasCalls++;
				return;
			}
		}
		hudItems++;
	}
}
