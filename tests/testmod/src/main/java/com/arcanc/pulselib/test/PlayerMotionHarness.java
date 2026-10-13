/**
 * @author ArcAnc
 * Created at: 13.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test;

import com.arcanc.pulselib.api.registration.PAnimatedAttachmentRegistrar;
import com.arcanc.pulselib.api.registration.PModelRegistrar;
import com.arcanc.pulselib.api.registration.PPlayerAnimationRegistrar;
import com.arcanc.pulselib.content.animatable.ControllerState;
import com.arcanc.pulselib.content.model.animation.BoneFrame;
import com.arcanc.pulselib.content.model.animation.PInterpolationType;
import com.arcanc.pulselib.content.model.animation.PPoseEasing;
import com.arcanc.pulselib.content.model.animation.PRawAnimation;
import com.arcanc.pulselib.content.model.animation.PTransitionInterruptionPolicy;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationAnchor;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationAnchors;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationBlendMode;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationDefinition;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationFrame;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationHandle;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationSpace;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import com.arcanc.pulselib.content.player.animation.PPlayerPart;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAnimatedAttachmentContext;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAnimatedAttachmentRenderer;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAnimatedAttachments;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAutomaticMeshAttachments;
import com.arcanc.pulselib.content.player.animation.firstPerson.PFirstPersonCameraMode;
import com.arcanc.pulselib.content.player.animation.firstPerson.PFirstPersonPresentation;
import com.arcanc.pulselib.content.player.animation.firstPerson.PPlayerFirstPersonSettings;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.data.gecko.PGeckoModelLoader;
import com.arcanc.pulselib.data.gltf.PGltfModelLoader;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PModelCache;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Opt-in in-game matrix and physics witnesses; never runs in a normal demo or CI. */
public final class PlayerMotionHarness
{
	private static final boolean ENABLED = Boolean.getBoolean("pulselib.test.playerMotion");
	private static final boolean CAMERA = !"mp2".equals(System.getProperty("pulselib.test.motionPhase"));
	private static final List<ResourceLocation> IDS = new ArrayList<>();
	private static final List<PModelData> MODELS = List.of(
			PModelData.direct(PLibDatabase.rl("player/regression/motion"), PGeckoModelLoader.INSTANCE.id()),
			PModelData.direct(PLibDatabase.rl("player/regression/motion_gltf"), PGltfModelLoader.INSTANCE.id()));
	private static final String[] CASES = {"A", "B", "C", "D"};
	private static @Nullable ResourceLocation selected;
	private static @Nullable ResourceLocation secondary;
	private static int multipleFormat;
	private static @Nullable Player beforeRespawn;
	private static @Nullable CompletableFuture<?> serverCheck;
	private static final Set<UUID> renderedObservers = new HashSet<>();
	private static final UUID[] OBSERVERS = {new UUID(0, 0x505001), new UUID(0, 0x505002)};
	private static float weight = 1;
	private static int scenario;
	private static int startTick = -1;
	private static int checked;
	private static int thirdPersonCallbacks;
	private static int firstPersonCallbacks;
	private static @Nullable Matrix4f expectedAnchor;
	private static @Nullable Matrix4f expectedMesh;
	private static int capturedMeshes;
	private static @Nullable CompletableFuture<Void> reload;
	private static boolean reloaded;
	private static boolean completed;

	private PlayerMotionHarness()
	{
	}

	public static void registerModels(PModelRegistrar registrar)
	{
		if (!ENABLED)
		{
			return;
		}
		for (PModelData model : MODELS)
		{
			registrar.model(model).texture("0", PLibDatabase.rl("player/demo/test_ball_model/0"));
		}
	}

	public static void registerAnimations(PPlayerAnimationRegistrar registrar)
	{
		if (!ENABLED)
		{
			return;
		}
		for (int format = 0; format < MODELS.size(); format++)
		{
			for (PPlayerAnimationBlendMode mode : PPlayerAnimationBlendMode.values())
			{
				ResourceLocation id = ClientTestPlugin.id("motion/" + format + "/" + mode.name().toLowerCase(java.util.Locale.ROOT));
				IDS.add(id);
				registrar.register(id, PPlayerAnimationDefinition.builder(MODELS.get(format)).
						when(player -> id.equals(selected) || id.equals(secondary)).
						bind(PPlayerPart.ROOT, "root").
						bind(PPlayerPart.BODY, "body").
						bind(PPlayerPart.RIGHT_ARM, "arm").
						anchor(PPlayerAnimationAnchors.RIGHT_ITEM, "item").
						anchor(PPlayerAnimationAnchors.RIGHT_HAND, "item").
						anchor(PPlayerAnimationAnchors.FIRST_PERSON_CAMERA, "camera").
						firstPerson(new PPlayerFirstPersonSettings(true, 0, 0, PFirstPersonCameraMode.ANIMATED)).
						rootPivot(0.2f, 0.3f, 0.4f).
						blendMode(mode).
						weight((player, partialTick) -> id.equals(secondary) ? 0.7f : weight).
						crossfade(2, PPoseEasing.LINEAR, PTransitionInterruptionPolicy.FROM_CURRENT).
						controllers(controllers -> controllers.add("motion", () -> state ->
								state.controller().isStopped() ? ControllerState.STOP : ControllerState.PLAY)).
						build());
			}
		}
	}

	public static void registerAttachments(PAnimatedAttachmentRegistrar registrar)
	{
		if (!ENABLED)
		{
			return;
		}
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
				if (!context.firstPerson() && expectedAnchor == null && List.of(OBSERVERS).contains(context.player().getUUID()))
				{
					renderedObservers.add(context.player().getUUID());
				}
				if (!context.animation().equals(selected))
				{
					return;
				}
				if (context.firstPerson())
				{
					firstPersonCallbacks++;
				}
				else
				{
					thirdPersonCallbacks++;
				}
				if (expectedAnchor != null)
				{
					matrix(expectedAnchor, context.poseStack().last().pose(), "native anchor stack");
				}
			}
		});
	}

	/** Called by a test-only queue probe at the real submission boundary. */
	public static void submitted(Matrix4f transform)
	{
		if (expectedMesh != null)
		{
			matrix(expectedMesh, transform, "mesh submission");
			capturedMeshes++;
		}
	}

	public static void frame()
	{
		Minecraft mc = Minecraft.getInstance();
		if (!ENABLED || completed || mc.getOverlay() != null || mc.player == null || mc.level == null || PModelCache.getModels() == null)
		{
			return;
		}
		DisposableTestWorlds.requireCurrentWorld();
		createObservers();
		if (reload != null)
		{
			if (!reload.isDone())
			{
				return;
			}
			reload.join();
			reload = null;
			reloaded = true;
			startTick = -1;
		}
		int total = IDS.size() * CASES.length * 3;
		if (scenario == total)
		{
			if (multipleFormat < MODELS.size())
			{
				if (startTick < 0)
				{
					selected = IDS.get(multipleFormat * PPlayerAnimationBlendMode.values().length);
					secondary = IDS.get(multipleFormat * PPlayerAnimationBlendMode.values().length + 1);
					weight = 0.4f;
					for (Player player : mc.level.players())
					{
						stopFixtures(player);
						PPlayerAnimations.getHandle(player, selected).play("motion", PRawAnimation.begin().thenLoop("A").build());
						PPlayerAnimations.getHandle(player, secondary).play("motion", PRawAnimation.begin().thenLoop("B").build());
					}
					startTick = mc.player.tickCount;
					return;
				}
				if (mc.player.tickCount < startTick + 4)
				{
					return;
				}
				verifyMultipleDefinitions(mc.player);
				secondary = null;
				multipleFormat++;
				startTick = -1;
				return;
			}
			if (!reloaded)
			{
				scenario = 0;
				multipleFormat = 0;
				reload = mc.reloadResourcePacks();
				return;
			}
			if (serverCheck == null)
			{
				beforeRespawn = mc.player;
				UUID uuid = mc.player.getUUID();
				var server = mc.getSingleplayerServer();
				serverCheck = server.submit(() ->
				{
					var player = server.getPlayerList().getPlayer(uuid);
					Snapshot snapshot = new Snapshot(player);
					for (PModelData data : MODELS)
					{
						for (var animation : data.getModel().animations().values())
						{
							animation.rootMotion("root", PInterpolationType.LINEAR).extractRootMotion(0, 10);
						}
					}
					snapshot.check(player);
					server.getPlayerList().respawn(player, false, Entity.RemovalReason.KILLED);
				});
				return;
			}
			if (!serverCheck.isDone() || mc.player == beforeRespawn)
			{
				return;
			}
			serverCheck.join();
			SmokeHarness.check(renderedObservers.size() == 2, "both client observer fixtures must reach actual world player rendering");
			PPlayerAnimations.tick(mc.level);
			SmokeHarness.check(PPlayerAnimations.getInstance(mc.player, selected).player() == mc.player, "instance must follow real respawn");
			PLibDatabase.LOGGER.info("PLAYER MOTION M-P2 PASS: {} matrix/physics cases; Gecko + embedded glTF; all blend modes; weights; crossfade; two definitions; first/third person; reload; server extraction isolation; real respawn; two rendered client observers; callbacks third={}, first={}",
					checked, thirdPersonCallbacks, firstPersonCallbacks);
			completed = true;
			mc.stop();
			return;
		}

		if (startTick < 0)
		{
			for (Player player : mc.level.players())
			{
				stopFixtures(player);
			}
			selected = IDS.get(scenario / (CASES.length * 3));
			weight = new float[]{0.25f, 0.6f, 1}[scenario % 3];
			for (Player player : mc.level.players())
			{
				PPlayerAnimationHandle handle = PPlayerAnimations.getHandle(player, selected);
				SmokeHarness.check(handle.play("motion", PRawAnimation.begin().thenLoop(CASES[(scenario / 3) % CASES.length]).build()), "motion controller start");
			}
			mc.options.setCameraType(scenario % 2 == 0 ? CameraType.FIRST_PERSON : CameraType.THIRD_PERSON_BACK);
			startTick = mc.player.tickCount;
			return;
		}
		if (mc.player.tickCount < startTick + 4)
		{
			return;
		}
		verify(mc.player);
		scenario++;
		startTick = -1;
	}

	private static void verify(Player player)
	{
		Minecraft mc = Minecraft.getInstance();
		List<Player> players = List.copyOf(mc.level.players());
		List<Snapshot> before = players.stream().map(Snapshot::new).toList();
		// Bracket the library's animation tick and both render paths in one client frame.
		PPlayerAnimations.tick(mc.level);
		PPlayerAnimationFrame frame = PPlayerAnimations.getInstance(player, selected).sampleFrame(1);
		SmokeHarness.check(frame != null, "evaluated player frame");
		PBakedModel model = frame.definition().modelData().getModel();
		Matrix4f full = sequential(frame, model, "item", false);
		Matrix4f relative = sequential(frame, model, "root", false).invert().mul(full);
		Matrix4f actual = new Matrix4f();
		SmokeHarness.check(frame.modelMatrix("item", actual), "item matrix");
		matrix(full, actual, "all parent matrices");
		SmokeHarness.check(frame.currentRootRelativeMatrix("item", actual), "root-relative item");
		matrix(relative, actual, "one ROOT");
		PFirstPersonPresentation presentation = PFirstPersonPresentation.create(frame);
		SmokeHarness.check(presentation != null && presentation.anchorMatrix(PPlayerAnimationAnchors.RIGHT_ITEM, actual), "view-space item");
		matrix(sequential(frame, model, "camera", false).invert().mul(full), actual, "camera/item hierarchy");
		PoseStack stack = new PoseStack();
		PPlayerAnimations.applyRoot(player, stack, 1);
		Matrix4f outer = new Matrix4f(stack.last().pose());
		expectedAnchor = new Matrix4f(outer).mul(PPlayerAnimationSpace.toPlayerSpace(relative, frame.definition()));
		try
		{
			PPlayerAnimatedAttachments.renderThirdPerson(mc.player, stack, mc.renderBuffers().bufferSource(), 0xF000F0, 1);
		}
		finally
		{
			expectedAnchor = null;
		}
		var meshes = PPlayerAnimations.meshAttachmentPoses(player, 1).stream().filter(pose -> pose.animation().equals(selected)).toList();
		SmokeHarness.check(meshes.size() == 1 && meshes.getFirst().root().name().equals("ball"), "automatic mesh inherits animated parent without a BALL channel; scenario=" + scenario + " selected=" + selected + " roots=" + meshes.stream().map(pose -> pose.animation() + ":" + pose.root().name()).toList() + " active=" + frame.resolver().activeAnimationBones());
		Matrix4f ball = sequential(frame, model, "root", false).invert().mul(sequential(frame, model, "ball", false));
		expectedMesh = new Matrix4f(outer).mul(PPlayerAnimationSpace.toPlayerGeometrySpace(ball, frame.definition()));
		capturedMeshes = 0;
		try
		{
			PPlayerAutomaticMeshAttachments.renderThirdPerson(meshes, stack, 0xF000F0);
			SmokeHarness.check(capturedMeshes > 0, "real third-person mesh submission");
		}
		finally
		{
			expectedMesh = null;
		}
		var commands = PPlayerAnimations.firstPersonPresentation(mc.player, 1);
		SmokeHarness.check(commands != null && commands.meshAttachments().stream().filter(pose -> pose.animation().equals(selected)).count() == 1, "first-person commands");
		expectedMesh = sequential(frame, model, "camera", false).invert().mul(sequential(frame, model, "ball", false));
		capturedMeshes = 0;
		try
		{
			PPlayerAutomaticMeshAttachments.renderFirstPerson(commands.meshAttachments().stream().filter(pose -> pose.animation().equals(selected)).toList(), new PoseStack(), 0xF000F0);
			SmokeHarness.check(capturedMeshes > 0, "real first-person mesh submission");
		}
		finally
		{
			expectedMesh = null;
		}
		expectedAnchor = sequential(frame, model, "camera", false).invert().mul(full);
		try
		{
			PPlayerAnimatedAttachments.renderFirstPerson(mc.player, commands, new PoseStack(), mc.renderBuffers().bufferSource(), 0xF000F0, 1);
		}
		finally
		{
			expectedAnchor = null;
		}
		for (var animation : model.animations().values())
		{
			animation.rootMotion("root", PInterpolationType.LINEAR).extractRootMotion(0, 10);
		}
		for (int index = 0; index < players.size(); index++)
		{
			before.get(index).check(players.get(index));
		}
		checked++;
	}

	private static void createObservers()
	{
		Minecraft mc = Minecraft.getInstance();
		for (int index = 0; index < OBSERVERS.length; index++)
		{
			UUID uuid = OBSERVERS[index];
			if (mc.level.players().stream().anyMatch(player -> player.getUUID().equals(uuid)))
			{
				continue;
			}
			RemotePlayer observer = new RemotePlayer(mc.level, new GameProfile(OBSERVERS[index], "MotionObserver" + index));
			observer.setId(0x505001 + index);
			observer.setPos(mc.player.getX() + (index == 0 ? -1 : 1), mc.player.getY() + 0.5, mc.player.getZ() + 2);
			mc.level.addEntity(observer);
		}
	}

	private static void stopFixtures(Player player)
	{
		for (ResourceLocation id : IDS)
		{
			PPlayerAnimations.getHandle(player, id).stopAll();
		}
	}

	private static void verifyMultipleDefinitions(Player player)
	{
		Minecraft mc = Minecraft.getInstance();
		Snapshot before = new Snapshot(player);
		PPlayerAnimations.tick(mc.level);
		PoseStack stack = new PoseStack();
		PPlayerAnimations.applyRoot(player, stack, 1);
		float sign = multipleFormat == 0 ? 1 : -1;
		Vector3f pivot = new Vector3f(0.2f * sign, 0.3f * sign, 0.4f);
		// Sorted definition IDs apply ADDITIVE_LOCAL before OVERRIDE. Keep that existing order.
		Matrix4f expected = new Matrix4f().translate(pivot).rotateY(sign * (float)Math.PI / 2 * 0.7f).
				translate(new Vector3f(pivot).negate()).translate(sign * 0.4f, 0, 0);
		matrix(expected, stack.last().pose(), "two weighted visual roots");
		for (ResourceLocation id : List.of(selected, secondary))
		{
			PPlayerAnimationFrame frame = PPlayerAnimations.getInstance(player, id).sampleFrame(1);
			PBakedModel model = frame.definition().modelData().getModel();
			Matrix4f actual = new Matrix4f();
			SmokeHarness.check(PPlayerAnimations.attachmentMatrix(player, id, PPlayerAnimationAnchors.RIGHT_ITEM, 1, actual), "mixed attachment");
			Matrix4f relative = sequential(frame, model, "root", false).invert().mul(sequential(frame, model, "item", false));
			matrix(new Matrix4f(expected).mul(PPlayerAnimationSpace.toPlayerSpace(relative, frame.definition())),
					new Matrix4f(stack.last().pose()).mul(actual), "mixed root must not leak into the socket");
		}
		before.check(player);
		checked++;
	}

	private static Matrix4f sequential(PPlayerAnimationFrame frame, PBakedModel model, String bone, boolean bind)
	{
		List<Integer> chain = new ArrayList<>();
		for (int index = model.boneIndex(bone); index >= 0; index = model.parentIndex(index))
		{
			chain.addFirst(index);
		}
		Matrix4f result = new Matrix4f();
		for (int index : chain)
		{
			if (bind)
			{
				result.translate(model.bone(index).basePosition()).rotate(model.bone(index).baseRotation());
			}
			else
			{
				BoneFrame local = frame.localTransform(model.bone(index).name());
				result.translate(local.translation()).rotate(local.rotation()).scale(local.scale());
			}
		}
		return result;
	}

	private static void matrix(Matrix4f expected, Matrix4f actual, String label)
	{
		SmokeHarness.check(expected.equals(actual, 0.0001f), label + " expected=" + expected + " actual=" + actual);
	}

	private record Snapshot(Vec3 position, Vec3 movement, AABB bounds, float width, float height)
	{
		Snapshot(Player player)
		{
			this(player.position(), player.getDeltaMovement(), player.getBoundingBox(), player.getBbWidth(), player.getBbHeight());
		}

		void check(Player player)
		{
			SmokeHarness.check(this.position.equals(player.position()) && this.movement.equals(player.getDeltaMovement()) &&
					this.bounds.equals(player.getBoundingBox()) && this.width == player.getBbWidth() && this.height == player.getBbHeight(),
					"root animation tick/render/extraction must preserve position, movement, bounds and dimensions");
		}
	}
}
