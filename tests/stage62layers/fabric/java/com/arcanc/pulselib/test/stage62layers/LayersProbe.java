/**
 * @author ArcAnc
 * Created at: 31.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage62layers;

import com.arcanc.pulselib.api.plugin.IPulseClientPlugin;
import com.arcanc.pulselib.api.plugin.PulsePlugin;
import com.arcanc.pulselib.api.registration.PAttachmentRegistrar;
import com.arcanc.pulselib.api.registration.PAnimatedAttachmentRegistrar;
import com.arcanc.pulselib.api.registration.PModelRegistrar;
import com.arcanc.pulselib.api.registration.PPlayerAnimationRegistrar;
import com.arcanc.pulselib.content.model.animation.PRawAnimation;
import com.arcanc.pulselib.content.model.textures.PAlphaMode;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationAnchor;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationAnchors;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationDefinition;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import com.arcanc.pulselib.content.player.animation.PPlayerPart;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAnimatedAttachmentContext;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAnimatedAttachmentLayer;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAnimatedAttachmentRenderer;
import com.arcanc.pulselib.content.renderer.PRenderContexts;
import com.arcanc.pulselib.content.renderer.PWorldRenderPass;
import com.arcanc.pulselib.content.renderer.base.PLivingAttachmentRenderState;
import com.arcanc.pulselib.content.renderer.base.PRenderState;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.data.gecko.PGeckoModelLoader;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.attachments.PAttachmentAnchorResolvers;
import com.arcanc.pulselib.util.attachments.PLivingAttachmentDefinition;
import com.arcanc.pulselib.util.attachments.PLivingAttachmentLayer;
import com.arcanc.pulselib.util.attachments.PTransform;
import com.arcanc.pulselib.util.attachments.humanoid.PHumanoidAnchors;
import com.arcanc.pulselib.util.attachments.humanoid.PHumanoidAttachmentLayer;
import com.arcanc.pulselib.util.attachments.humanoid.PHumanoidBindings;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.opengl.GlTexture;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.animal.pig.PigModel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Native renderer factories, post-extraction snapshots and real vanilla entity PiP attachment draws. */
public final class LayersProbe implements ClientModInitializer
{
	private static final PModelData MODEL = PModelData.entity(PLibDatabase.rl("stage42b4_owner"), PGeckoModelLoader.INSTANCE.id());
	private static final Identifier ANIMATION = PLibDatabase.rl("stage62layers/anchor");
	private static final List<LivingEntity> OWNERS = new ArrayList<>();
	private static final List<Object> AUDITS = new ArrayList<>();
	private static final List<Object> RECORDS = new ArrayList<>();
	private static final int[][] CALLBACKS = new int[4][4];
	private static int ticks;
	private static int phase;
	private static int frames;
	private static int regions;
	private static int phaseStarted;
	private static int models;
	private static int registrations;
	private static int snapshots;
	private static int activeDraw = -1;
	private static boolean started;
	private static boolean advance;
	private static boolean armed;
	private static boolean completed;
	private static @Nullable CompletableFuture<Void> reload;
	private static @Nullable Object oldModel;
	private static @Nullable Object oldRenderer;

	@Override
	public void onInitializeClient()
	{
		PAttachmentAnchorResolvers.register(PigModel.class, PHumanoidAnchors.BODY, (state, model) -> model.root());
		ClientTickEvents.END_CLIENT_TICK.register(LayersProbe::tick);
		ClientLifecycleEvents.CLIENT_STOPPING.register(mc ->
		{
			check(completed && regions == 224 && AUDITS.size() == 2, "Complete all layer epochs before shutdown");
			PLibDatabase.LOGGER.info("STAGE62D1 SHUTDOWN PASS");
		});
	}

	@PulsePlugin
	public static final class Plugin implements IPulseClientPlugin
	{
		@Override
		public Identifier getPluginId()
		{
			return PLibDatabase.rl("stage62layers/client");
		}

		@Override
		public void registerModels(PModelRegistrar registrar)
		{
			models++;
			registrar.model(MODEL).texture("0", PLibDatabase.rl("lifecycle/stage62a_white"));
		}

		@Override
		public void registerAttachments(PAttachmentRegistrar registrar)
		{
			registrations++;
			for (int kind = 0; kind < 3; kind++)
			{
				int selected = kind;
				var definition = new PLivingAttachmentDefinition(MODEL,
						(entity, slot, stack) -> entity.getId() == 0x62d100 + selected && (selected != 1 || slot == EquipmentSlot.CHEST),
						List.of(PHumanoidBindings.bind(PHumanoidAnchors.BODY, "probe", new PTransform(new Vector3f(0, -0.7f, -0.5f), new Quaternionf(), new Vector3f(0.5f)))),
						(entity, stack, bone, mesh, inherited, partialTick) ->
						{
							if (activeDraw == selected)
								CALLBACKS[phase][selected]++;
							check(PRenderContexts.current() == PRenderContexts.Context.GUI, "Native layer mesh callback belongs to GUI invocation");
							check(selected != 1 || stack.is(Items.DIAMOND_CHESTPLATE) && stack.getCount() == 9, "Item renderer consumes captured stack, despite live owner mutation");
							int rgb = selected == 0 ? 0xFF0000 : selected == 1 ? 0x00FF00 : 0x0000FF;
							return inherited.withColor((phase % 2 == 0 ? 0xFF000000 : 0x80000000) | rgb).withEmissive(true).
									withAlphaMode(phase % 2 == 0 ? PAlphaMode.OPAQUE : PAlphaMode.TRANSLUCENT);
						}, false);
				if (kind == 1)
					registrar.registerLiving(Items.DIAMOND_CHESTPLATE, definition);
				else
					registrar.registerGlobalLiving(definition);
			}
		}

		@Override
		public void registerPlayerAnimations(PPlayerAnimationRegistrar registrar)
		{
			registrar.register(ANIMATION, PPlayerAnimationDefinition.builder(MODEL).
					when(player -> player.getId() == 0x62d102).
					bind(PPlayerPart.LEFT_LEG, "probe").
					anchor(PPlayerAnimationAnchors.RIGHT_HAND, "probe").
					controllers(controllers -> controllers.add("motion", () -> state -> state.controller().getState())).build());
		}

		@Override
		public void registerAnimatedAttachments(PAnimatedAttachmentRegistrar registrar)
		{
			registrar.register(new PPlayerAnimatedAttachmentRenderer()
			{
				@Override
				public PPlayerAnimationAnchor anchor()
				{
					return PPlayerAnimationAnchors.RIGHT_HAND;
				}

				@Override
				public void render(PPlayerAnimatedAttachmentContext context)
				{
					if (activeDraw == 2 && context.player().getId() == 0x62d102)
					{
						check(!context.firstPerson() && context.buffers() == context.submitNodeCollector(), "Native third-person animated layer and canonical collector");
						CALLBACKS[phase][3]++;
					}
				}
			});
		}
	}

	private static void tick(Minecraft mc)
	{
		check(Files.isRegularFile(mc.gameDirectory.toPath().resolve(".pulselib-layers")), "Disposable client marker");
		if (++ticks > 1800)
			throw new AssertionError("STAGE62D1 layer witness timeout: phase=" + phase + " screen=" + mc.screen + " reload=" + reload + " model=" + MODEL.getModel());
		if (mc.level == null || mc.player == null || mc.getOverlay() != null)
			return;
		check(models > 0, "Client plugin completed model registration");
		if (MODEL.getModel() == null)
			return;
		if (!started)
		{
			started = true;
			var pig = new Pig(EntityType.PIG, mc.level);
			var stand = new ArmorStand(EntityType.ARMOR_STAND, mc.level);
			var player = new RemotePlayer(mc.level, new GameProfile(new UUID(0, 0x62d102), "LayerWitness"));
			OWNERS.addAll(List.of(pig, stand, player));
			for (int index = 0; index < OWNERS.size(); index++)
			{
				var entity = OWNERS.get(index);
				entity.setId(0x62d100 + index);
				entity.setPos(30000, 100, 30000);
				entity.setInvisible(true);
				mc.level.addEntity(entity);
			}
			startEpoch();
		}
		if (advance)
		{
			advance = false;
			phase++;
			frames = 0;
			phaseStarted = ticks;
			if (phase == 2)
			{
				oldModel = MODEL.getModel();
				oldRenderer = mc.getEntityRenderDispatcher().getRenderer(OWNERS.getFirst());
				mc.setScreen(null);
				reload = mc.reloadResourcePacks();
			}
			if (phase == 4)
			{
				check(models == 2 && registrations == 1, "Once-only attachment registration, two model collections");
				completed = true;
				writeResults(mc);
				PLibDatabase.LOGGER.info("STAGE62D1 COMPLETE: phases={}, regions={}, snapshots={}, callbacks={}", phase, regions, snapshots, java.util.Arrays.deepToString(CALLBACKS));
				mc.stop();
			}
		}
		if (phase == 2 && reload != null && reload.isDone())
		{
			reload.join();
			reload = null;
			check(MODEL.getModel() != oldModel && mc.getEntityRenderDispatcher().getRenderer(OWNERS.getFirst()) != oldRenderer, "Reload replaces baked model and native entity renderer instances");
			startEpoch();
		}
	}

	private static void startEpoch()
	{
		audit();
		snapshotChecks();
		var player = (RemotePlayer)OWNERS.get(2);
		var handle = PPlayerAnimations.getHandle(player, ANIMATION);
		check(handle.play("motion", PRawAnimation.begin().thenLoop("shift").build()), "Native player anchor controller starts");
		phaseStarted = ticks;
		Minecraft.getInstance().setScreen(new ProbeScreen());
	}

	private static void audit()
	{
		var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
		var seen = new IdentityHashMap<Object, Boolean>();
		int living = 0;
		int avatars = 0;
		for (String name : List.of("renderers", "playerRenderers", "mannequinRenderers"))
			for (Object value : ((Map<?, ?>)field(EntityRenderDispatcher.class, dispatcher, name)).values())
				if (value instanceof LivingEntityRenderer<?, ?, ?> renderer && seen.put(value, true) == null)
				{
					var layers = (List<?>)field(LivingEntityRenderer.class, renderer, "layers");
					long base = layers.stream().filter(layer -> layer instanceof PLivingAttachmentLayer<?, ?>).count();
					long humanoid = layers.stream().filter(layer -> layer instanceof PHumanoidAttachmentLayer<?, ?>).count();
					long animated = layers.stream().filter(layer -> layer instanceof PPlayerAnimatedAttachmentLayer).count();
					check(base == 1, "Exactly one shared living layer on " + renderer.getClass().getName() + ": " + base);
					check(humanoid == (renderer.getModel() instanceof HumanoidModel<?> ? 1 : 0), "Humanoid layer follows actual model type");
					check(animated == (renderer instanceof AvatarRenderer<?> ? 1 : 0), "Animated layer follows native avatar renderer type");
					living++;
					if (renderer instanceof AvatarRenderer<?>)
						avatars++;
				}
		check(living > 50 && avatars >= 4, "Audit native living and both player/mannequin skins");
		AUDITS.add(Map.of("epoch", phase / 2, "living", living, "avatars", avatars));
		PLibDatabase.LOGGER.info("STAGE62D1 LAYERS PASS: epoch={}, living={}, avatars={}", phase / 2, living, avatars);
	}

	private static void snapshotChecks()
	{
		var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
		var owner = OWNERS.get(1);
		owner.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE, 9));
		var state = (LivingEntityRenderState)dispatcher.extractEntity(owner, 0.37f);
		var entries = ((PLivingAttachmentRenderState)state).pulselib$getAttachments();
		check(entries != null && entries.size() == 1 && entries.getFirst().entity() == owner && entries.getFirst().stack().getCount() == 9, "Native final extraction captures the eligible item snapshot");
		check(PRenderState.partialTick(state) == 0.37f, "Partial tick precedes attachment extraction");
		owner.getItemBySlot(EquipmentSlot.CHEST).setCount(1);
		check(entries.getFirst().stack().getCount() == 9, "Caller item mutation leaves captured stack unchanged");
		owner.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
		var empty = dispatcher.extractEntity(owner, 0.73f);
		check(((PLivingAttachmentRenderState)empty).pulselib$getAttachments() == null, "Ineligible next extraction clears attachment entries");
		var level = new LevelRenderState();
		level.entityRenderStates.add(state);
		check(PWorldRenderPass.requiresSeparateTargets(level), "Extracted attachment retains native world targets");
		level.entityRenderStates.clear();
		level.entityRenderStates.add(empty);
		check(!PWorldRenderPass.requiresSeparateTargets(level), "Following ordinary state does not inherit attachment eligibility");
		snapshots += 6;
	}

	public static void beforeGui()
	{
		armed = Minecraft.getInstance().screen instanceof ProbeScreen && !advance && !completed && Minecraft.getInstance().getOverlay() == null;
	}

	public static void afterGui()
	{
		if (!armed || ticks - phaseStarted < 20)
			return;
		armed = false;
		var mc = Minecraft.getInstance();
		var target = mc.getMainRenderTarget();
		var pixels = readMain();
		int scale = mc.getWindow().getGuiScale();
		List<Object> counts = new ArrayList<>();
		for (int kind = 0; kind < 3; kind++)
		{
			int count = 0;
			int peak = 0;
			for (int y = 16 * scale; y < 88 * scale; y++)
				for (int x = (16 + kind * 100) * scale; x < (88 + kind * 100) * scale; x++)
				{
					int offset = ((target.height - 1 - y) * target.width + x) * 4;
					int a = Byte.toUnsignedInt(pixels.get(offset + kind));
					int b = Byte.toUnsignedInt(pixels.get(offset + (kind + 1) % 3));
					int c = Byte.toUnsignedInt(pixels.get(offset + (kind + 2) % 3));
					if (a > 70 && b < 12 && c < 12)
					{
						count++;
						peak = Math.max(peak, a);
					}
				}
			check(count > 20 && (phase % 2 == 0 ? peak >= 245 : peak > 70 && peak < 245), "Native layer pixels phase=" + phase + " owner=" + kind + " count=" + count + " peak=" + peak);
			check(CALLBACKS[phase][kind] > 0, "Every native layer invokes its mesh resolver");
			counts.add(List.of(count, peak));
			regions++;
		}
		check(CALLBACKS[phase][3] > 0, "Registered animated player callback reached through native layer");
		// Independent empty controls below all three native entity viewports.
		for (int kind = 0; kind < 3; kind++)
		{
			int offset = ((target.height - 1 - 92 * scale) * target.width + (50 + kind * 100) * scale) * 4;
			check(pixels.get(offset) == 0 && pixels.get(offset + 1) == 0 && pixels.get(offset + 2) == 0, "No attachment leakage outside native entity viewport");
			regions++;
		}
		int text = 0;
		for (int y = 100 * scale; y < 114 * scale; y++)
			for (int x = 16 * scale; x < 310 * scale; x++)
			{
				int offset = ((target.height - 1 - y) * target.width + x) * 4;
				if (Byte.toUnsignedInt(pixels.get(offset)) > 200 && Byte.toUnsignedInt(pixels.get(offset + 1)) > 200 && Byte.toUnsignedInt(pixels.get(offset + 2)) > 200)
					text++;
			}
		check(text > 100 && GL11.glGetError() == GL11.GL_NO_ERROR, "Native GUI text and GL state survive attachment layers");
		regions++;
		RECORDS.add(Map.of("phase", phase, "frame", frames, "rgbCountsAndPeaks", counts, "text", text));
		if (++frames == 8)
		{
			PLibDatabase.LOGGER.info("STAGE62D1 PHASE PASS: phase={}, regions={}, pixels={}", phase, regions, counts);
			if (phase == 3)
				Screenshot.grab(mc.gameDirectory, "stage62d1-layers.png", target, 1, message -> PLibDatabase.LOGGER.info("{}", message.getString()));
			advance = true;
		}
	}

	private static final class ProbeScreen extends Screen
	{
		private ProbeScreen()
		{
			super(Component.literal("PulseLib Fabric native layer witness"));
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
			for (int kind = 0; kind < 3; kind++)
			{
				var owner = OWNERS.get(kind);
				if (kind == 1)
					owner.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE, 9));
				var state = (LivingEntityRenderState)mc.getEntityRenderDispatcher().extractEntity(owner, 0.37f);
				check(((PLivingAttachmentRenderState)state).pulselib$getAttachments() != null, "Native entity extraction supplies attachment state for every owner");
				state.shadowPieces.clear();
				state.nameTag = null;
				state.isInvisible = state.isInvisibleToPlayer = true;
				if (state instanceof HumanoidRenderState humanoid)
					humanoid.chestEquipment = ItemStack.EMPTY;
				if (kind == 1)
					owner.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
				int x = 16 + kind * 100;
				graphics.entity(state, 24, new Vector3f(0, 1, 0), new Quaternionf().rotateZ((float)Math.PI), null, x, 16, x + 72, 88);
			}
			graphics.text(mc.font, "Native living / humanoid / animated player layers", 16, 102, -1, false);
		}
	}

	public static void drawing(LivingEntityRenderState state)
	{
		activeDraw = -1;
		if (armed && OWNERS.size() == 3)
		{
			var entries = ((PLivingAttachmentRenderState)state).pulselib$getAttachments();
			if (entries != null && !entries.isEmpty())
				activeDraw = entries.getFirst().entity().getId() - 0x62d100;
		}
	}

	private static ByteBuffer readMain()
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
			var pixels = BufferUtils.createByteBuffer(target.width * target.height * 4);
			GL11.glReadPixels(0, 0, target.width, target.height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
			return pixels;
		}
		finally
		{
			GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, rowLength);
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previous);
			GL30.glDeleteFramebuffers(fbo);
		}
	}

	private static Object field(Class<?> type, Object owner, String name)
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

	private static void writeResults(Minecraft mc)
	{
		try
		{
			Files.writeString(mc.gameDirectory.toPath().resolve("layers-results.json"), new GsonBuilder().setPrettyPrinting().create().toJson(
					Map.of("status", "PASS", "phases", phase, "regions", regions, "snapshots", snapshots, "audits", AUDITS, "callbacks", CALLBACKS, "records", RECORDS)));
		}
		catch (java.io.IOException exception)
		{
			throw new AssertionError(exception);
		}
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
			throw new AssertionError("STAGE62D1: " + message);
	}
}
