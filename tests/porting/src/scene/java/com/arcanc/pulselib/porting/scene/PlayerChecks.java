/**
 * @author ArcAnc
 * Created at: 20.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.porting.scene;

import com.arcanc.pulselib.content.animatable.ControllerState;
import com.arcanc.pulselib.content.model.animation.PAnimation;
import com.arcanc.pulselib.content.model.animation.PAnimationTrack;
import com.arcanc.pulselib.content.model.animation.PAnimationValue;
import com.arcanc.pulselib.content.model.animation.PBoneAnimation;
import com.arcanc.pulselib.content.model.animation.PInterpolationType;
import com.arcanc.pulselib.content.model.animation.PKeyframe;
import com.arcanc.pulselib.content.model.animation.PRawAnimation;
import com.arcanc.pulselib.content.model.baked.PBakedBone;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.model.baked.PBakedMesh;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationAnchor;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationAnchors;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationBlendMode;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationDefinition;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import com.arcanc.pulselib.content.player.animation.PPlayerPart;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAnimatedAttachmentContext;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAnimatedAttachmentRenderer;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAnimatedAttachments;
import com.arcanc.pulselib.content.player.animation.firstPerson.PFirstPersonRenderContexts;
import com.arcanc.pulselib.content.player.animation.firstPerson.PFirstPersonRenderMode;
import com.arcanc.pulselib.content.player.animation.firstPerson.PFirstPersonTransformMode;
import com.arcanc.pulselib.content.player.animation.firstPerson.PPlayerFirstPersonSettings;
import com.arcanc.pulselib.content.player.animation.firstPerson.PVanillaFirstPersonArmResolver;
import com.arcanc.pulselib.content.player.animation.firstPerson.PVanillaFirstPersonItemResolver;
import com.arcanc.pulselib.content.registration.PLibRegistration.AnimationChannelReg;
import com.arcanc.pulselib.content.renderer.PRenderContexts;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.base.PLivingAttachmentRenderState;
import com.arcanc.pulselib.content.renderer.base.PPlayerRigRenderState;
import com.arcanc.pulselib.content.renderer.base.PPlayerRootRenderState;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.content.renderer.plan.PRenderPlan;
import com.arcanc.pulselib.util.attachments.PLivingAttachmentDefinition;
import com.arcanc.pulselib.util.attachments.PLivingAttachmentSources;
import com.arcanc.pulselib.util.attachments.PLivingAttachments;
import com.arcanc.pulselib.util.attachments.humanoid.PHumanoidBindings;
import com.arcanc.pulselib.util.attachments.humanoid.armor.PArmorModelProvider;
import com.arcanc.pulselib.util.attachments.humanoid.armor.PArmorModelSnapshots;
import com.arcanc.pulselib.util.PRenderTypes;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class PlayerChecks
{
	private static final Identifier RIG = id("rig");
	private static final Identifier OVERLAY = id("overlay");
	private static final int ATTACHMENT_COLOR = 0xFF64D925;
	private static final int HAND_ATTACHMENT_COLOR = 0xFF7001B0;
	private static final int WORLD_ANCHOR_COLOR = 0xFFB07701;
	private static final Identifier IRON_ARMOR_TEXTURE = Identifier.withDefaultNamespace("textures/entity/equipment/humanoid/iron.png");
	private static final int REMOTE_ID = 1_000_007;
	private static final EnumSet<HumanoidArm> hands = EnumSet.noneOf(HumanoidArm.class);
	private static final EnumSet<HumanoidArm> items = EnumSet.noneOf(HumanoidArm.class);
	private static boolean started;
	private static boolean done;
	private static int phase;
	private static long phaseStart;
	private static boolean emptyHand;
	private static boolean localHelmet;
	private static boolean remoteHelmet;
	private static boolean remoteHelmetDraw;
	private static boolean localHelmetDraw;
	private static @Nullable PBakedModel attachmentModel;
	private static int firstPersonAttachments;
	private static int worldAnchorAttachments;
	private static boolean automaticWorld;
	private static boolean automaticHand;
	private static int localReplays;
	private static int remoteReplays;
	private static int armorSetups;
	private static int armorBatches;
	private static int attachmentDraws;
	private static int avatarBatches;
	private static @Nullable RemotePlayer remote;

	private PlayerChecks()
	{
	}

	public static void tick()
	{
		Minecraft client = Minecraft.getInstance();
		if (done || !client.isGameLoadFinished() || client.gui.overlay() != null)
			return;
		if (!started)
		{
			SceneChecks.check(Files.isRegularFile(Path.of("mods/pulselib-client-scene.jar")), "Player fixture requires a fresh scene directory");
			SceneChecks.check(RenderSystem.getDevice().getDeviceInfo().backendName().equalsIgnoreCase(System.getProperty("pulselib.scene.backend")),
					"Player fixture requires the requested native backend");
			client.options.pauseOnLostFocus = false;
			client.options.renderDistance().set(3);
			client.options.simulationDistance().set(5);
			started = true;
			client.schedule(() -> client.createWorldOpenFlows().createFreshLevel("pulselib-stage-seven",
					new LevelSettings("PulseLib player integration", GameType.CREATIVE,
							new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT),
					new WorldOptions(653, false, false),
					registry -> registry.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), new TitleScreen()));
			return;
		}
		var player = client.player;
		var level = client.level;
		if (player == null || level == null || client.gui.screen() != null)
			return;
		player.setYRot(0);
		player.setXRot(0);
		SceneChecks.check(PFirstPersonRenderContexts.current() == null && PFirstPersonRenderContexts.passPresentation() == null,
				"First-person context survived the native hand pass");
		if (phase == 0)
		{
			register(client);
			player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STICK));
			player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.BLAZE_ROD));
			player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
			player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
			var other = new RemotePlayer(level, new GameProfile(UUID.fromString("a214221a-8414-4c77-914d-ed234a0746ff"), "PulseFixture"));
			other.setId(REMOTE_ID);
			other.setPos(player.getX() + 0.8, player.getY(), player.getZ() + 3);
			other.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
			other.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
			level.addEntity(other);
			remote = other;
			play(player, RIG);
			play(player, OVERLAY);
			play(other, RIG);
			phaseStart = level.getGameTime();
			phase = 1;
		}
		else if (phase == 1 && level.getGameTime() - phaseStart >= 16)
		{
			SceneChecks.check(hands.size() == 2 && items.size() == 2, "Native first-person hands/items missed: " + hands + "/" + items);
			var camera = client.gameRenderer.mainCamera();
			var rotation = camera.rotation();
			SceneChecks.check(Math.abs(rotation.z) > 0.04f || Math.abs(rotation.x) > 0.04f, "Animation roll did not reach the native camera");
			var expected = new Matrix4f().rotation(new Quaternionf(rotation).invert());
			SceneChecks.check(camera.getViewRotationMatrix(new Matrix4f()).equals(expected, 1e-4f), "Camera view matrix cache lost animated roll");
			SceneChecks.check(Math.abs(Math.abs(camera.position().x - player.getX()) - 0.125f) < 0.001f, "Animated camera translation was lost");
			var remoteState = client.getEntityRenderDispatcher().extractEntity(Objects.requireNonNull(remote), 0);
			var ownState = client.getEntityRenderDispatcher().extractEntity(player, 0);
			checkState(remoteState, 0.25f);
			checkState(ownState, 0.375f);
			player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
			player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
			phase = 2;
			phaseStart = level.getGameTime();
		}
		else if (phase == 2 && level.getGameTime() - phaseStart >= 10)
		{
			SceneChecks.check(emptyHand, "Native empty main-hand command was not transformed");
			client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
			phase = 3;
			phaseStart = level.getGameTime();
		}
		else if (phase == 3 && level.getGameTime() - phaseStart >= 6)
		{
			SceneChecks.check(!localHelmet && remoteHelmet && !localHelmetDraw && remoteHelmetDraw,
					"Owner-scoped helmet submission/draw local/remote=" + List.of(localHelmet, remoteHelmet, localHelmetDraw, remoteHelmetDraw));
			SceneChecks.check(localReplays > 0 && remoteReplays > 0, "Frozen provider poses did not render for both players");
			SceneChecks.check(firstPersonAttachments > 0 && worldAnchorAttachments > 0, "Animated anchors did not reach both render plans: " + firstPersonAttachments + "/" + worldAnchorAttachments);
			SceneChecks.check(automaticWorld && automaticHand, "Automatic mesh attachments missed a render phase");
			SceneChecks.check(armorSetups > 1 && armorBatches > 1 && avatarBatches > 1 && attachmentDraws > 1,
					"Native world omitted provider/armor/avatar/attachment draws: " + List.of(armorSetups, armorBatches, avatarBatches, attachmentDraws));
			System.out.println("PASS frozen armor geometry for two owners, local replacement and remote vanilla helmet, first-person and world anchor draws");
			System.out.println("PASS native player animation, both hands/items, camera, ROOT/blend, shared armor and attachment RHI on " +
					RenderSystem.getDevice().getDeviceInfo().backendName() + " " + List.of(armorSetups, armorBatches, avatarBatches, attachmentDraws));
			try
			{
				Files.writeString(Path.of("pulselib-scene-result.txt"), "PASS\n");
			}
			catch (IOException failure)
			{
				throw new IllegalStateException(failure);
			}
			done = true;
			client.stop();
		}
	}

	private static void checkState(EntityRenderState state, float rootOffset)
	{
		SceneChecks.check(state instanceof AvatarRenderState, "Expected a native avatar");
		var root = Objects.requireNonNull(((PPlayerRootRenderState)state).pulselib$getPlayerRoot());
		SceneChecks.check(Math.abs(Math.abs(root.m30()) - rootOffset) < 0.001f, "Unexpected layered ROOT translation: " + root.m30());
		var rig = Objects.requireNonNull(((PPlayerRigRenderState)state).pulselib$getPlayerRig());
		SceneChecks.check(!rig.isEmpty(), "Avatar rig did not reach the extracted snapshot");
		var layers = ((PLivingAttachmentRenderState)state).pulselib$getAttachments();
		SceneChecks.check(layers != null && layers.size() == (rootOffset > 0.3f ? 2 : 1), "Loader did not capture the equipped attachments");
		var armor = ((PArmorModelSnapshots.State)state).pulselib$getArmorProviders();
		SceneChecks.check(armor.values().stream().anyMatch(slots -> slots.containsKey(EquipmentSlot.CHEST)),
				"Loader did not capture the armor provider");
	}

	private static void play(Player player, Identifier animation)
	{
		var handle = Objects.requireNonNull(PPlayerAnimations.getHandle(player, animation));
		SceneChecks.check(handle.play("fixture", PRawAnimation.begin().thenLoop("pose").build()), "Fixture animation controller is unavailable");
	}

	private static void register(Minecraft client)
	{
		Identifier texture = SceneChecks.texture(client, "player_attachment", -1);
		checkRootMotion();
		checkArmorPolicyScope();
		var mesh = SceneChecks.baked(SceneChecks.geometry(0), texture, false, true);
		var rig = modelData(RIG, rigModel(0.25f, List.of(mesh)));
		var overlay = modelData(OVERLAY, rigModel(0.25f, List.of()));
		PPlayerAnimations.register(RIG, PPlayerAnimationDefinition.builder(rig).
				bind(PPlayerPart.ROOT, "root").bind(PPlayerPart.RIGHT_ARM, "right").bind(PPlayerPart.LEFT_ARM, "left").
				anchor(PPlayerAnimationAnchors.FIRST_PERSON_CAMERA, "camera").
				anchor(PPlayerAnimationAnchors.RIGHT_HAND, "right").anchor(PPlayerAnimationAnchors.LEFT_HAND, "left").
				anchor(PPlayerAnimationAnchors.RIGHT_ITEM, "right").anchor(PPlayerAnimationAnchors.LEFT_ITEM, "left").
				firstPerson(new PPlayerFirstPersonSettings(true, 0, 0, PPlayerFirstPersonSettings.ENABLED.cameraMode())).
				controllers(registrar -> registrar.add("fixture", () -> state -> ControllerState.PLAY)).build());
		PPlayerAnimations.register(OVERLAY, PPlayerAnimationDefinition.builder(overlay).
				when(player -> player == client.player).bind(PPlayerPart.ROOT, "root").
				blendMode(PPlayerAnimationBlendMode.ADDITIVE_LOCAL).weight(0.5f).priority(1).
				controllers(registrar -> registrar.add("fixture", () -> state -> ControllerState.PLAY)).build());
		attachmentModel = new PBakedModel(List.of(
				new PBakedBone("marker", new Vector3f(), new Quaternionf(), List.of(), null, List.of(mesh))), Map.of());
		var attachment = modelData(texture, attachmentModel);
		PLivingAttachments.register(Items.IRON_CHESTPLATE, new PLivingAttachmentDefinition(attachment,
				PLivingAttachmentSources.equipmentSlot(EquipmentSlot.CHEST), List.of(PHumanoidBindings.body("marker")),
				(owner, stack, bone, geometry, inherited, tick) -> inherited.withColor(ATTACHMENT_COLOR), false));
		PPlayerAnimatedAttachments.register(new PPlayerAnimatedAttachmentRenderer()
		{
			@Override
			public PPlayerAnimationAnchor anchor()
			{
				return PPlayerAnimationAnchors.RIGHT_HAND;
			}

			@Override
			public void render(PPlayerAnimatedAttachmentContext context)
			{
				var expected = context.firstPerson() ? PRenderContexts.Context.FIRST_PERSON : PRenderContexts.Context.WORLD;
				SceneChecks.check(PRenderContexts.current() == expected, "Animated attachment entered the wrong submission scope");
				var transform = new Matrix4f(context.poseStack().last().pose()).scale(0.15f);
				var header = new PInstanceHeader(transform, context.firstPerson() ? HAND_ATTACHMENT_COLOR : WORLD_ANCHOR_COLOR,
						context.packedLight(), OverlayTexture.NO_OVERLAY);
				var type = PRenderTypes.RenderTypeProvider.trianglesEmissiveSolid(texture);
				if (context.firstPerson())
					PRenderQueue.submitFirstPersonMesh(type, mesh.geometry(), header);
				else
					PRenderQueue.submitEntityMesh(type, mesh.geometry(), header);
			}
		});
	}

	/** Runs with native item construction, before NeoForge captures registered client extensions. */
	public static void registerHelmet(Item item)
	{
		var data = new PModelData(id("helmet_attachment"), PModelData.DEFAULT_MODEL_LOADER)
		{
			@Override
			public @Nullable PBakedModel getModel()
			{
				return attachmentModel;
			}
		};
		PLivingAttachments.register(item, new PLivingAttachmentDefinition(data,
				PLivingAttachmentSources.equipmentSlotPredicate(EquipmentSlot.HEAD, owner -> owner == Minecraft.getInstance().player),
				List.of(PHumanoidBindings.head("marker")),
				(owner, stack, bone, geometry, inherited, tick) -> inherited.withColor(ATTACHMENT_COLOR), true));
	}

	private static void checkArmorPolicyScope()
	{
		var helmet = new ItemStack(Items.IRON_HELMET);
		var chest = new ItemStack(Items.IRON_CHESTPLATE);
		var marker = new IllegalStateException("fixture nested armor lookup");
		PArmorModelSnapshots.withCapturedArmor(helmet, () ->
		{
			SceneChecks.check(PArmorModelSnapshots.usesCapturedArmorPolicy(helmet), "Outer armor scope missing");
			try
			{
				PArmorModelSnapshots.withCapturedArmor(chest, () ->
				{
					SceneChecks.check(PArmorModelSnapshots.usesCapturedArmorPolicy(chest) && !PArmorModelSnapshots.usesCapturedArmorPolicy(helmet),
							"Nested armor scope used the outer stack");
					throw marker;
				});
			}
			catch (IllegalStateException failure)
			{
				if (failure != marker)
					throw failure;
			}
			SceneChecks.check(PArmorModelSnapshots.usesCapturedArmorPolicy(helmet), "Armor scope did not restore its caller");
			return null;
		});
		SceneChecks.check(!PArmorModelSnapshots.usesCapturedArmorPolicy(helmet), "Armor lookup survived its native invocation");
	}

	private static void checkRootMotion()
	{
		PAnimationValue<Vector3f> start = (context, destination) -> destination.zero();
		PAnimationValue<Vector3f> end = (context, destination) -> destination.set(1, 0, 0);
		var track = new PAnimationTrack<>(AnimationChannelReg.POSITION,
				List.of(new PKeyframe<>(0, start, start, PInterpolationType.LINEAR), new PKeyframe<>(20, end, end, PInterpolationType.LINEAR)));
		var animation = new PAnimation("motion", 20,
				Map.of("root", new PBoneAnimation(UUID.randomUUID(), Map.of(AnimationChannelReg.POSITION.id().toString(), track))));
		var motion = animation.rootMotion("root", PInterpolationType.LINEAR);
		SceneChecks.check(motion.extractRootMotion(5, 15).translation().equals(new Vector3f(0.5f, 0, 0), 1e-5f) &&
				motion.extractRootMotion(15, 5).translation().equals(new Vector3f(-0.5f, 0, 0), 1e-5f),
				"Forward and reverse root motion deltas changed");
	}

	private static PModelData modelData(Identifier id, PBakedModel model)
	{
		return new PModelData(id, id("coordinates"))
		{
			@Override
			public PBakedModel getModel()
			{
				return model;
			}
		};
	}

	private static PBakedModel rigModel(float offset, List<PBakedMesh> mesh)
	{
		var root = new PBakedBone("root", new Vector3f(), new Quaternionf(), List.of(), null, List.of());
		var camera = new PBakedBone("camera", new Vector3f(0, 1.62f, 0), new Quaternionf(), List.of(), null, List.of());
		var right = new PBakedBone("right", new Vector3f(-0.3f, 1.4f, -0.5f), new Quaternionf(), List.of(), null, List.of());
		var left = new PBakedBone("left", new Vector3f(0.3f, 1.4f, -0.5f), new Quaternionf(), List.of(), null, List.of());
		var automatic = new PBakedBone("automatic", new Vector3f(0, 1.4f, -0.65f), new Quaternionf(), List.of(), null, mesh);
		PAnimationValue<Vector3f> position = (context, destination) -> destination.set(offset, 0, 0);
		PAnimationValue<Quaternionf> roll = (context, destination) -> destination.rotationZ(0.22f);
		PAnimationValue<Vector3f> cameraOffset = (context, destination) -> destination.set(0.125f, 1.62f, 0);
		var cameraTracks = new PBoneAnimation(UUID.randomUUID(), Map.of(
				AnimationChannelReg.POSITION.id().toString(),
				new PAnimationTrack<>(AnimationChannelReg.POSITION, List.of(new PKeyframe<>(0, cameraOffset, cameraOffset, PInterpolationType.LINEAR))),
				AnimationChannelReg.ROTATION.id().toString(),
				new PAnimationTrack<>(AnimationChannelReg.ROTATION, List.of(new PKeyframe<>(0, roll, roll, PInterpolationType.LINEAR)))));
		PAnimationValue<Quaternionf> wrist = (context, destination) -> destination.rotationX(0.2f);
		return new PBakedModel(List.of(root, camera, right, left, automatic), Map.of("pose",
				new PAnimation("pose", 20, Map.of(
						"root", new PBoneAnimation(UUID.randomUUID(), Map.of(AnimationChannelReg.POSITION.id().toString(),
								new PAnimationTrack<>(AnimationChannelReg.POSITION, List.of(new PKeyframe<>(0, position, position, PInterpolationType.LINEAR))))),
						"camera", cameraTracks, "right", rotationTrack(wrist), "left", rotationTrack(wrist), "automatic", rotationTrack(wrist)))));
	}

	private static PBoneAnimation rotationTrack(PAnimationValue<Quaternionf> rotation)
	{
		return new PBoneAnimation(UUID.randomUUID(), Map.of(AnimationChannelReg.ROTATION.id().toString(),
				new PAnimationTrack<>(AnimationChannelReg.ROTATION, List.of(new PKeyframe<>(0, rotation, rotation, PInterpolationType.LINEAR)))));
	}

	private static Identifier id(String path)
	{
		return Identifier.fromNamespaceAndPath("pulselib_player_fixture", path);
	}

	public static void arm(PoseStack poses)
	{
		if (phase != 1 && phase != 2)
			return;
		var context = PFirstPersonRenderContexts.current();
		if (context == null)
			return;
		var pose = context.arm() == HumanoidArm.RIGHT ? context.presentation().rightArm() : context.presentation().leftArm();
		if (pose.mode() == PFirstPersonRenderMode.ANIMATED)
		{
			var expected = new Matrix4f(context.basePose()).mul(PVanillaFirstPersonArmResolver.resolveArmOrigin(context.arm(),
					Objects.requireNonNull(pose.transform())).matrix());
			SceneChecks.check(poses.last().pose().equals(expected, 1e-5f), "Native arm did not receive its transformed pose");
			hands.add(context.arm());

		}
	}

	public static void emptyArm()
	{
		if (phase == 2)
			emptyHand = true;
	}

	public static void item(ItemDisplayContext display, ItemStack stack, PoseStack poses)
	{
		if (phase != 1 || stack.isEmpty())
			return;
		var context = PFirstPersonRenderContexts.current();
		if (context == null)
			return;
		var pose = context.arm() == HumanoidArm.RIGHT ? context.presentation().rightItem() : context.presentation().leftItem();
		if ((display == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || display == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) &&
				pose.mode() == PFirstPersonRenderMode.ANIMATED)
		{
			var expected = new Matrix4f(context.basePose()).mul(PVanillaFirstPersonItemResolver.resolveItemOrigin(context.arm(),
					Objects.requireNonNull(pose.transform())).matrix());
			SceneChecks.check(pose.transformMode() == PFirstPersonTransformMode.OVERRIDE && poses.last().pose().equals(expected, 1e-5f),
					"Native held item did not receive its overridden pose");
			items.add(context.arm());
		}
	}

	public static void equipment(Object state, ItemStack item)
	{
		if (phase == 3 && state instanceof AvatarRenderState avatar && item.is(Items.IRON_HELMET))
		{
			if (avatar.id == REMOTE_ID)
				remoteHelmet = true;
			else
				localHelmet = true;
		}
	}

	public static void modelBatch(List<ModelFeatureRenderer.Submit<?>> submits, Runnable nativeBatch)
	{
		Map<HumanoidModel<?>, Float> caller = new IdentityHashMap<>();
		if (phase > 0)
			for (var submit : submits)
			{
				if (submit.state() instanceof AvatarRenderState)
					avatarBatches++;
				var entry = ((PArmorModelSnapshots.Node)(Object)submit).pulselib$getArmorProvider();
				if (entry != null)
				{
					caller.putIfAbsent(entry.model(), entry.model().body.x);
					armorBatches++;
				}
				else if (phase == 3 && submit.state() instanceof AvatarRenderState avatar &&
						submit.renderType() == RenderTypes.armorCutoutNoCull(IRON_ARMOR_TEXTURE))
				{
					if (avatar.id == REMOTE_ID)
						remoteHelmetDraw = true;
					else
						localHelmetDraw = true;
				}
			}
		nativeBatch.run();
		for (var entry : caller.entrySet())
			SceneChecks.check(entry.getKey().body.x == entry.getValue(), "Deferred armor leaked a pose onto its shared model");
	}

	public static void replay(ModelFeatureRenderer.Submit<?> node)
	{
		if (phase > 0 && ((PArmorModelSnapshots.Node)(Object)node).pulselib$getArmorProvider() != null && node.state() instanceof AvatarRenderState avatar)
		{
			boolean other = avatar.id == REMOTE_ID;
			var model = (HumanoidModel<?>)node.model();
			SceneChecks.check(model.body.x == (other ? 3 : 6), "Wrong frozen provider pose on render node " + avatar.id + ": " + model.body.x);
			if (other)
				remoteReplays++;
			else
				localReplays++;
		}
	}

	public static void automaticSubmission(PInstanceHeader instance, boolean hand)
	{
		if (phase == 0 || instance.packedColor() != -1)
			return;
		var expected = hand ? PRenderContexts.Context.FIRST_PERSON : PRenderContexts.Context.WORLD;
		SceneChecks.check(PRenderContexts.current() == expected, "Automatic attachment entered the wrong submission scope");
		if (hand)
			automaticHand = true;
		else
			automaticWorld = true;
	}

	static void recordPlan(PRenderPlan plan)
	{
		if (phase == 0)
			return;
		for (var group : plan.groups())
			for (var instance : group.instances())
			{
				int color = instance.packedColor();
				if (color == ATTACHMENT_COLOR)
					attachmentDraws++;
				if (color == HAND_ATTACHMENT_COLOR)
					firstPersonAttachments++;
				if (color == WORLD_ANCHOR_COLOR)
					worldAnchorAttachments++;
			}
	}

	public static final class ProviderArmor extends Item implements PArmorModelProvider
	{
		private @Nullable HumanoidModel<?> shared;

		public ProviderArmor(Properties properties)
		{
			super(properties);
		}

		@Override
		public HumanoidModel<?> getPulseArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original)
		{
			if (this.shared == null)
				this.shared = original;
			return this.shared;
		}

		@Override
		public void setupPulseArmorAnimations(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> model, float partialTick)
		{
			armorSetups++;
			model.body.x = entity == remote ? 3 : 6;
		}
	}
}
