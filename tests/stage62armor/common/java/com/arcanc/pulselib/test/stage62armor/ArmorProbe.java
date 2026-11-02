/**
 * @author ArcAnc
 * Created at: 02.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage62armor;

import com.arcanc.pulselib.util.attachments.humanoid.armor.PArmorModelSnapshots;
import com.arcanc.pulselib.content.renderer.base.PModelPartSnapshot;
import com.arcanc.pulselib.content.player.deformer.PDeformableCubeBakeScope;
import com.arcanc.pulselib.content.player.deformer.PPlayerMeshDeformers;
import com.arcanc.pulselib.content.player.animation.PPlayerPart;
import com.arcanc.pulselib.content.model.deformer.PChannelReference;
import com.arcanc.pulselib.content.model.deformer.PDeformerInstance;
import com.arcanc.pulselib.content.model.deformer.PDeformerStack;
import com.arcanc.pulselib.content.model.deformer.PStretchDeformer;
import com.arcanc.pulselib.content.model.deformer.PStretchDefinition;
import com.arcanc.pulselib.util.PLibDatabase;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.opengl.GlTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Vector3f;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Same provider/owner-free node/GPU oracle runs through both loaders' real native renderers. */
public final class ArmorProbe
{
	private static final int BASE = 0x62d200;
	private static final List<LivingEntity> OWNERS = new ArrayList<>();
	private static final Map<PArmorModelSnapshots.Entry, float[]> EXPECTED = new IdentityHashMap<>();
	private static final List<Object> RECORDS = new ArrayList<>();
	private static final int[] DRAWS = new int[4];
	public static boolean neo;
	private static boolean queries = true;
	private static boolean armed;
	private static boolean advance;
	private static boolean completed;
	private static boolean nested;
	private static int fail;
	private static int phase;
	private static int ticks;
	private static int phaseStart;
	private static int frames;
	private static int regions;
	private static int selections;
	private static int animations;
	private static int extensions;
	private static int deformations;
	private static int failures;
	private static int nestedChecks;
	private static int retainedChecks;
	private static int ordinary;
	private static @Nullable HumanoidModel<HumanoidRenderState> shared;
	private static @Nullable HumanoidModel<?> failureOriginal;
	private static float failureX;
	private static @Nullable Model<?> baseExtension;
	private static @Nullable CompletableFuture<Void> reload;
	private static @Nullable Object oldRenderer;
	private static PArmorModelSnapshots.@Nullable Entry retained;
	private static float retainedX;

	private ArmorProbe()
	{
	}

	public static Model<?> extension(Model<?> original)
	{
		check(queries, "No generic armor extension during deferred submission/replay");
		extensions++;
		baseExtension = new HumanoidModel<HumanoidRenderState>(original.root());
		return baseExtension;
	}

	public static @Nullable HumanoidModel<?> select(LivingEntity owner, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original)
	{
		check(queries, "No provider selection after extraction");
		if (neo)
			check(original == baseExtension, "Canonical provider receives the composed generic extension model");
		selections++;
		if (fail == 1)
		{
			failureOriginal = original;
			failureX = original.body.x;
			original.body.x = 999;
			throw new IllegalArgumentException("provider selection failure");
		}
		if (fail == 3)
			return null;
		if (shared == null)
			try (var baking = PDeformableCubeBakeScope.begin())
			{
				shared = new HumanoidModel<>(LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0), 64, 32).bakeRoot());
			}
		return shared;
	}

	private static float x(int owner, EquipmentSlot slot)
	{
		return (owner - BASE - 1) * 6 + slot.ordinal() * 0.1f;
	}

	private static float scale(EquipmentSlot slot)
	{
		return 1 + slot.ordinal() * 0.05f;
	}

	public static void animate(LivingEntity owner, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> model, float partialTick)
	{
		check(queries && partialTick == 0.37f, "Provider setup belongs to exact native extraction sample");
		check(model.head.visible == (slot == EquipmentSlot.HEAD) && model.hat.visible == model.head.visible &&
				model.body.visible == (slot == EquipmentSlot.CHEST || slot == EquipmentSlot.LEGS) &&
				model.leftArm.visible == (slot == EquipmentSlot.CHEST) && model.rightArm.visible == model.leftArm.visible &&
				model.leftLeg.visible == (slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET) && model.rightLeg.visible == model.leftLeg.visible,
				"Canonical equipment slot visibility precedes provider animation");
		animations++;
		for (var part : model.allParts())
			part.visible = false;
		model.root().visible = model.body.visible = true;
		model.body.x = x(owner.getId(), slot);
		model.body.y = model.body.z = model.body.xRot = model.body.yRot = model.body.zRot = 0;
		model.body.xScale = scale(slot);
		if (fail == 2)
			throw new IllegalArgumentException("provider animation failure");
		if (nested && owner == OWNERS.getFirst())
		{
			nested = false;
			var other = Minecraft.getInstance().getEntityRenderDispatcher().extractEntity(OWNERS.get(1), partialTick);
			check(((PArmorModelSnapshots.State)other).pulselib$getArmorProviders().values().stream().mapToInt(Map::size).sum() == 4, "Nested owner receives independent provider entries");
			check(model.body.x == x(owner.getId(), slot), "Nested extraction restores the outer selected model pose");
			nestedChecks++;
		}
	}

	public static void tick(Minecraft mc)
	{
		check(Files.isRegularFile(mc.gameDirectory.toPath().resolve(".pulselib-armor-provider")), "Disposable provider client marker");
		if (++ticks > 1800)
			throw new AssertionError("STAGE62D2A timeout phase=" + phase + " reload=" + reload);
		if (mc.level == null || mc.player == null || mc.getOverlay() != null)
			return;
		if (OWNERS.isEmpty())
		{
			OWNERS.add(new ArmorStand(EntityType.ARMOR_STAND, mc.level));
			for (int i = 1; i < 3; i++)
				OWNERS.add(new RemotePlayer(mc.level, new GameProfile(new UUID(0, BASE + i), "Provider" + i)));
			for (int i = 0; i < OWNERS.size(); i++)
			{
				var owner = OWNERS.get(i);
				owner.setId(BASE + i);
				owner.setPos(30000, 100, 30000);
				owner.setInvisible(true);
				mc.level.addEntity(owner);
			}
			var deformation = PDeformerStack.compile(List.of(new PDeformerInstance<>(PStretchDeformer.INSTANCE,
					new PStretchDefinition(new Vector3f(), new Vector3f(0, 1, 0), PChannelReference.constant(1)))));
			PPlayerMeshDeformers.register(PLibDatabase.rl("stage62armor/body"), PPlayerPart.BODY,
					player -> player.getId() == BASE + 1 || player.getId() == BASE + 2, deformation, (player, reference) ->
					{
						check(queries, "No deformer value queries during deferred provider draw");
						deformations++;
						return 1.3f;
					});
			startEpoch(mc);
		}
		if (advance)
		{
			advance = false;
			phase++;
			frames = 0;
			phaseStart = ticks;
			if (phase == 2)
			{
				oldRenderer = mc.getEntityRenderDispatcher().getRenderer(OWNERS.getFirst());
				mc.setScreen(null);
				reload = mc.reloadResourcePacks();
			}
			if (phase == 4)
			{
				check(failures == 6 && nestedChecks == 2 && retainedChecks == 1 && ordinary > 0 && deformations > 0, "All provider lifetime/failure/deformer/ordinary cases completed");
				completed = true;
				try
				{
					Files.writeString(mc.gameDirectory.toPath().resolve("armor-results.json"), new GsonBuilder().setPrettyPrinting().create().toJson(Map.of(
							"status", "PASS", "phases", phase, "regions", regions, "draws", DRAWS, "selections", selections, "animations", animations,
							"extensions", extensions, "deformations", deformations, "failures", failures, "records", RECORDS)));
				}
				catch (java.io.IOException exception)
				{
					throw new AssertionError(exception);
				}
				PLibDatabase.LOGGER.info("STAGE62D2A COMPLETE: neo={}, regions={}, draws={}, selections={}, animations={}, extensions={}, deformations={}, failures={}, nested={}, retained={}", neo, regions, java.util.Arrays.toString(DRAWS), selections, animations, extensions, deformations, failures, nestedChecks, retainedChecks);
				mc.stop();
			}
		}
		if (phase == 2 && reload != null && reload.isDone())
		{
			reload.join();
			reload = null;
			check(mc.getEntityRenderDispatcher().getRenderer(OWNERS.getFirst()) != oldRenderer, "Native armor renderer replaced after resource reload");
			queries = false;
			try (var caller = PModelPartSnapshot.capture(retained.model().root()))
			{
				retained.model().body.x = 999;
				retained.snapshot().restore();
				check(retained.model().body.x == retainedX, "Pre-reload provider snapshot replays without owner callbacks");
				retainedChecks++;
			}
			queries = true;
			shared = null;
			startEpoch(mc);
		}
	}

	private static void equip(boolean vanilla)
	{
		for (int i = 0; i < OWNERS.size(); i++)
			for (int slot = 0; slot < ArmorContent.SLOTS.size(); slot++)
				OWNERS.get(i).setItemSlot(ArmorContent.SLOTS.get(slot), new ItemStack(vanilla && i == 2 ?
						List.of(Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS, Items.DIAMOND_HELMET).get(slot) : ArmorContent.ITEMS.get(slot)));
	}

	private static void startEpoch(Minecraft mc)
	{
		queries = true;
		equip(false);
		if (shared == null)
			select(OWNERS.getFirst(), new ItemStack(ArmorContent.ITEMS.getFirst()), EquipmentSlot.CHEST,
					neo ? (HumanoidModel<?>)extension(new HumanoidModel<HumanoidRenderState>(LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0), 64, 32).bakeRoot())) :
					new HumanoidModel<HumanoidRenderState>(LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0), 64, 32).bakeRoot()));
		shared.body.x = 100;
		for (int mode = 1; mode <= 3; mode++)
		{
			fail = mode;
			try
			{
				mc.getEntityRenderDispatcher().extractEntity(OWNERS.getFirst(), 0.37f);
				throw new AssertionError("Expected provider callback failure");
			}
			catch (RuntimeException wrapper)
			{
				Throwable expected = wrapper;
				while (expected.getCause() != null)
					expected = expected.getCause();
				check(expected instanceof IllegalArgumentException || expected instanceof NullPointerException, "Provider failure cause type");
				check(mode == 3 ? "Pulse armor model".equals(expected.getMessage()) : expected.getMessage().startsWith("provider "), "Original provider failure remains visible");
				check(shared.body.x == 100, "Failed setup restores shared provider model");
				if (mode == 1)
					check(failureOriginal.body.x == failureX, "Failed selection restores original armor model");
				failures++;
			}
			finally
			{
				fail = 0;
			}
		}
		nested = true;
		var state = (HumanoidRenderState)mc.getEntityRenderDispatcher().extractEntity(OWNERS.getFirst(), 0.37f);
		remember(state, OWNERS.getFirst().getId());
		check(((PArmorModelSnapshots.State)state).pulselib$getArmorProviders().values().stream().mapToInt(Map::size).sum() == 4, "Four equipment slot snapshots");
		if (retained == null)
			for (var slots : ((PArmorModelSnapshots.State)state).pulselib$getArmorProviders().values())
			{
				retained = slots.get(EquipmentSlot.CHEST);
				retainedX = x(OWNERS.getFirst().getId(), EquipmentSlot.CHEST);
			}
		for (var component : PArmorModelSnapshots.Entry.class.getRecordComponents())
			check(!LivingEntity.class.isAssignableFrom(component.getType()) && component.getType() != ItemStack.class, "Frozen provider entry retains neither owner nor stack");
		queries = false;
		phaseStart = ticks;
		mc.setScreen(new ProbeScreen());
	}

	private static void remember(HumanoidRenderState state, int owner)
	{
		for (var slots : ((PArmorModelSnapshots.State)state).pulselib$getArmorProviders().values())
			for (var pair : slots.entrySet())
			{
				check(pair.getValue().model() == shared, "Provider-selected custom model is retained");
				EXPECTED.put(pair.getValue(), new float[] {x(owner, pair.getKey()), scale(pair.getKey())});
			}
	}

	public static void drawing(SubmitNodeStorage.ModelSubmit<?> submit)
	{
		var entry = ((PArmorModelSnapshots.Node)(Object)submit).pulselib$getArmorProvider();
		if (entry == null || !armed)
			return;
		var expected = EXPECTED.get(entry);
		check(expected != null && submit.model() == shared, "Native node uses the captured provider selection");
		check(Math.abs(shared.body.x - expected[0]) < 0.00001f && Math.abs(shared.body.xScale - expected[1]) < 0.00001f,
				"Frozen provider pose before native GPU draw: actual=" + shared.body.x + "/" + shared.body.xScale + " expected=" + java.util.Arrays.toString(expected));
		DRAWS[phase]++;
	}

	private static final class ProbeScreen extends Screen
	{
		private ProbeScreen()
		{
			super(Component.literal("Native armor provider snapshots"));
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
			queries = true;
			try
			{
				equip(phase % 2 == 1);
				for (int i = 0; i < OWNERS.size(); i++)
				{
					int beforeSelections = selections, beforeAnimations = animations, beforeExtensions = extensions;
					var state = (HumanoidRenderState)Minecraft.getInstance().getEntityRenderDispatcher().extractEntity(OWNERS.get(i), 0.37f);
					int expectedCalls = phase % 2 == 1 && i == 2 ? 0 : 4;
					check(selections - beforeSelections == expectedCalls && animations - beforeAnimations == expectedCalls &&
							extensions - beforeExtensions == (neo ? expectedCalls : 0), "Exactly one provider/extension selection and setup per eligible slot");
					remember(state, OWNERS.get(i).getId());
					if (phase % 2 == 1 && i == 2)
					{
						check(((PArmorModelSnapshots.State)state).pulselib$getArmorProviders().isEmpty(), "Ordinary armor does not inherit provider snapshots");
						ordinary++;
					}
					state.shadowPieces.clear();
					state.nameTag = null;
					state.isInvisible = state.isInvisibleToPlayer = true;
					graphics.entity(state, 24, new Vector3f(0, 1, 0), new Quaternionf().rotateZ((float)Math.PI), null, 8 + i * 100, 16, 96 + i * 100, 96);
					for (EquipmentSlot slot : ArmorContent.SLOTS)
						OWNERS.get(i).setItemSlot(slot, ItemStack.EMPTY);
				}
				shared.body.x = 100;
				shared.body.xScale = 77;
			}
			finally
			{
				queries = false;
			}
			graphics.text(Minecraft.getInstance().font, "Native provider / frozen pose / ordinary armor", 8, 112, -1, false);
		}
	}

	public static void beforeGui()
	{
		armed = Minecraft.getInstance().screen instanceof ProbeScreen && !advance && !completed && Minecraft.getInstance().getOverlay() == null;
	}

	public static void afterGui()
	{
		if (!armed || ticks - phaseStart < 10)
			return;
		armed = false;
		check(shared.body.x == 100 && shared.body.xScale == 77, "Deferred feature restores shared model sentinel");
		check(DRAWS[phase] > 0, "Provider nodes reach actual native model drawing");
		var mc = Minecraft.getInstance();
		var target = mc.getMainRenderTarget();
		int previous = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING), row = GL11.glGetInteger(GL11.GL_PACK_ROW_LENGTH);
		int fbo = GL30.glGenFramebuffers();
		var pixels = BufferUtils.createByteBuffer(target.width * target.height * 4);
		try
		{
			GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, 0);
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, fbo);
			GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, ((GlTexture)target.getColorTexture()).glId(), 0);
			GL11.glReadPixels(0, 0, target.width, target.height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
		}
		finally
		{
			GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, row);
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previous);
			GL30.glDeleteFramebuffers(fbo);
		}
		int scale = mc.getWindow().getGuiScale();
		List<Integer> actual = new ArrayList<>();
		for (int i = 0; i < 3; i++)
		{
			int count = 0;
			for (int y = 16 * scale; y < 96 * scale; y++)
				for (int x = (8 + i * 100) * scale; x < (96 + i * 100) * scale; x++)
				{
					int offset = ((target.height - 1 - y) * target.width + x) * 4;
					if (Byte.toUnsignedInt(pixels.get(offset)) > 20 || Byte.toUnsignedInt(pixels.get(offset + 1)) > 20 || Byte.toUnsignedInt(pixels.get(offset + 2)) > 20)
						count++;
				}
			check(count > 20, "Actual native armor pixels phase=" + phase + " owner=" + i + " count=" + count);
			actual.add(count);
			regions++;
		}
		check(GL11.glGetError() == GL11.GL_NO_ERROR, "Provider drawing has no GL errors");
		RECORDS.add(Map.of("phase", phase, "frame", frames, "pixels", actual));
		if (++frames == 8)
		{
			PLibDatabase.LOGGER.info("STAGE62D2A PHASE PASS: phase={}, pixels={}, draws={}", phase, actual, DRAWS[phase]);
			if (phase == 3)
				Screenshot.grab(mc.gameDirectory, "armor-provider.png", target, 1, message -> PLibDatabase.LOGGER.info("{}", message.getString()));
			advance = true;
		}
	}

	public static void shutdown()
	{
		check(completed && regions == 96, "All provider phases completed before normal shutdown");
		PLibDatabase.LOGGER.info("STAGE62D2A SHUTDOWN PASS");
	}

	private static void check(boolean value, String message)
	{
		if (!value)
			throw new AssertionError("STAGE62D2A: " + message);
	}
}
