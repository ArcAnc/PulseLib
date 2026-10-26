/**
 * @author ArcAnc
 * Created at: 13.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1;

import com.arcanc.pulselib.api.registration.PAnimatedAttachmentRegistrar;
import com.arcanc.pulselib.api.registration.PModelRegistrar;
import com.arcanc.pulselib.api.registration.PPlayerAnimationRegistrar;
import com.arcanc.pulselib.content.animatable.ControllerState;
import com.arcanc.pulselib.content.model.animation.BoneFrame;
import com.arcanc.pulselib.content.model.animation.PPoseEasing;
import com.arcanc.pulselib.content.model.animation.PRawAnimation;
import com.arcanc.pulselib.content.model.animation.PTransform;
import com.arcanc.pulselib.content.model.animation.PTransitionInterruptionPolicy;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationAnchor;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationAnchors;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationBlendMode;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationDefinition;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationFrame;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationHandle;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationInstance;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationSpace;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import com.arcanc.pulselib.content.player.animation.PPlayerPart;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAnimatedAttachmentContext;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAnimatedAttachmentRenderer;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAnimatedAttachments;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAutomaticMeshAttachments;
import com.arcanc.pulselib.content.player.animation.firstPerson.PFirstPersonCameraMode;
import com.arcanc.pulselib.content.player.animation.firstPerson.PFirstPersonPresentation;
import com.arcanc.pulselib.content.player.animation.firstPerson.PFirstPersonRenderPresentation;
import com.arcanc.pulselib.content.player.animation.firstPerson.PPlayerFirstPersonAnchorPose;
import com.arcanc.pulselib.content.player.animation.firstPerson.PPlayerFirstPersonMeshAttachmentPose;
import com.arcanc.pulselib.content.player.animation.firstPerson.PPlayerFirstPersonSettings;
import com.arcanc.pulselib.content.renderer.PRenderContexts;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.arcanc.pulselib.data.gecko.PGeckoModelLoader;
import com.arcanc.pulselib.data.gltf.PGltfModelLoader;
import com.arcanc.pulselib.util.PLibDatabase;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Attachment subset of the pinned PlayerMotionHarness M-P2 oracles, adapted to
 * native 26.1.2 queue/collector APIs. Camera, server extraction and real respawn
 * witnesses remain in the following native player checkpoint.
 */
public final class Stage52AttachmentProbe
{
	private static final boolean ENABLED = Boolean.getBoolean("pulselib.stage52aProbe");
	private static final List<Identifier> IDS = new ArrayList<>();
	private static final List<Identifier> POLICY_IDS = new ArrayList<>();
	private static final List<PModelData> MODELS = List.of(
		PModelData.direct(PLibDatabase.rl("player/regression/motion"), PGeckoModelLoader.INSTANCE.id()),
		PModelData.direct(PLibDatabase.rl("player/regression/motion_gltf"), PGltfModelLoader.INSTANCE.id()));
	private static final String[] CASES = new String[] {"A", "B", "C", "D"};
	private static @Nullable Identifier selected;
	private static @Nullable Identifier secondary;
	private static int multipleFormat;
	private static int policyScenario;
	private static int lastTransitionTick = -1;
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
	private static int queuedMeshes;
	private static boolean firstPersonProbe;
	private static boolean failCallback;
	private static int failureSuites;
	private static int lastFailurePhase = -1;
	private static int elapsedTicks;
	private static final int[][] nativeThird = new int[2][2];
	private static final int[][] nativeFirst = new int[2][2];
	private static final int[][] gpuFrames = new int[2][2];
	private static int nativePendingTriangles;
	private static int nativePendingFormats;
	private static long gpuPrimitives;
	private static @Nullable CompletableFuture<Void> reload;
	private static boolean reloaded;
	private static boolean completed;

	private Stage52AttachmentProbe()
	{
	}

	public static void registerModels(PModelRegistrar registrar)
	{
		if (!ENABLED)
			return;

		for (PModelData model : MODELS)
			registrar.model(model).texture("0", PLibDatabase.rl("stage42a_white"));
	}

	public static void registerAnimations(PPlayerAnimationRegistrar registrar)
	{
		if (!ENABLED)
			return;

		for (int format = 0; format < MODELS.size(); format++)
		{
			for (PPlayerAnimationBlendMode mode : PPlayerAnimationBlendMode.values())
			{
				Identifier id = PLibDatabase.rl("motion/" + format + "/" + mode.name().toLowerCase(Locale.ROOT));
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
						controllers(controllers -> controllers.add("motion", () -> state -> state.controller().isStopped() ? ControllerState.STOP : ControllerState.PLAY)).
						build());
			}
			for (boolean masked : new boolean[] {true, false})
			{
				Identifier id = PLibDatabase.rl("motion/" + format + (masked ? "/masked" : "/weighted_root"));
				POLICY_IDS.add(id);
				PPlayerAnimationDefinition.Builder builder = PPlayerAnimationDefinition.builder(MODELS.get(format)).
						when(player -> id.equals(selected)).
						bind(PPlayerPart.ROOT, "root").
						bind(PPlayerPart.BODY, "body").
						bind(PPlayerPart.RIGHT_ARM, "arm").
						anchor(PPlayerAnimationAnchors.RIGHT_ITEM, "item").
						anchor(PPlayerAnimationAnchors.RIGHT_HAND, "item").
						anchor(PPlayerAnimationAnchors.FIRST_PERSON_CAMERA, "camera").
						rootPivot(0.2f, 0.3f, 0.4f).
						firstPerson(new PPlayerFirstPersonSettings(true, 0, 0, PFirstPersonCameraMode.VANILLA)).
						crossfade(2, PPoseEasing.LINEAR, PTransitionInterruptionPolicy.FROM_CURRENT).
						controllers(controllers -> controllers.add("motion", () -> state -> state.controller().isStopped() ? ControllerState.STOP : ControllerState.PLAY));
				if (masked)
					builder.mask(PPlayerPart.BODY, PPlayerPart.RIGHT_ARM);
				else
					builder.partWeight(PPlayerPart.ROOT, 0.4f).boneWeight("root", 0.5f);

				registrar.register(id, builder.build());
			}
		}
	}

	public static void registerAttachments(PAnimatedAttachmentRegistrar registrar)
	{
		if (!ENABLED)
			return;

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
					renderedObservers.add(context.player().getUUID());

				if (!context.animation().equals(selected))
					return;

				check(context.buffers() == context.submitNodeCollector(), "Canonical and retained native collector accessors agree");
				if (failCallback && context.animation().equals(selected))
					throw new IllegalStateException("stage52a expected callback failure");
				if (context.firstPerson())
					firstPersonCallbacks++;
				else
					thirdPersonCallbacks++;

				if (expectedAnchor != null)
					matrix(expectedAnchor, context.poseStack().last().pose(), "native anchor stack");
			}
		});
	}

	/**
	 * Called by a test-only queue probe at the real submission boundary.
	 */
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
		if (!ENABLED || completed || mc.getOverlay() != null || mc.player == null || mc.level == null || MODELS.stream().anyMatch(data -> data.getModel() == null))
			return;

		check(mc.gameDirectory.toPath().toAbsolutePath().normalize().getParent().getFileName().toString().startsWith("stage52a"), "Only the dedicated stage52a disposable run directory");
		if (++elapsedTicks > 2400)
			throw new AssertionError("Stage 5.2a did not complete within 2400 world ticks");
		mc.mouseHandler.releaseMouse();
		KeyMapping.releaseAll();
		createObservers();
		if (reload != null)
		{
			if (!reload.isDone())
				return;

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
					return;

				verifyMultipleDefinitions(mc.player);
				secondary = null;
				multipleFormat++;
				startTick = -1;
				return;
			}
			if (policyScenario < POLICY_IDS.size())
			{
				if (startTick < 0)
				{
					selected = POLICY_IDS.get(policyScenario);
					weight = 1;
					for (Player player : mc.level.players())
					{
						stopFixtures(player);
						PPlayerAnimations.getHandle(player, selected).play("motion", PRawAnimation.begin().thenLoop("A").build());
					}
					mc.options.setCameraType(CameraType.FIRST_PERSON);
					startTick = mc.player.tickCount;
					lastTransitionTick = -1;
					return;
				}
				if (mc.player.tickCount < startTick + 4)
				{
					verifyTransition(mc.player);
					return;
				}
				verify(mc.player);
				policyScenario++;
				startTick = -1;
				return;
			}

			if (!reloaded)
			{
				scenario = 0;
				PLibDatabase.LOGGER.info("STAGE 5.2a PRE-RELOAD PASS: cases={}, nativeThird={}, nativeFirst={}, GPU hand frames={}", checked, Arrays.deepToString(nativeThird), Arrays.deepToString(nativeFirst), Arrays.deepToString(gpuFrames));
				multipleFormat = 0;
				policyScenario = 0;
				reload = mc.reloadResourcePacks();
				return;
			}
			check(renderedObservers.size() == 2, "Both client observer fixtures reach actual native player rendering");
			for (int phase = 0; phase < 2; phase++)
				for (int format = 0; format < 2; format++)
					check(nativeFirst[phase][format] > 0 && nativeThird[phase][format] > 0 && gpuFrames[phase][format] > 0, "Both formats submit through actual native first/third hooks and GPU hand flush before/after reload");
			int expectedCases = 2 * (IDS.size() * CASES.length * 3 * 4 + MODELS.size() + POLICY_IDS.size() * 4);
			check(checked == expectedCases && failureSuites == 2 * MODELS.size(), "All canonical matrix cases, transitions and failure recovery suites completed");
			Stage2PluginProbe.verify(true);
			PLibDatabase.LOGGER.info("STAGE 5.2a ATTACHMENTS PASS: matrixPhysicsCases={}, formats=Gecko/glTF, blendModes={}, weights=0.25/0.6/1, ROOT masks/part/bone weights/transitions/two definitions/reload/queue isolation PASS; nativeThird={}, nativeFirst={}, GPU hand frames={}, generatedPrimitives={}, callbackFailures={}, callbacks third={}, first={}", checked, PPlayerAnimationBlendMode.values().length, Arrays.deepToString(nativeThird), Arrays.deepToString(nativeFirst), Arrays.deepToString(gpuFrames), gpuPrimitives, failureSuites, thirdPersonCallbacks, firstPersonCallbacks);
			completed = true;
			mc.stop();
			return;
		}

		if (startTick < 0)
		{
			for (Player player : mc.level.players())
				stopFixtures(player);

			selected = IDS.get(scenario / (CASES.length * 3));
			weight = new float[] {0.25f, 0.6f, 1}[scenario % 3];
			for (Player player : mc.level.players())
			{
				PPlayerAnimationHandle handle = PPlayerAnimations.getHandle(player, selected);
				check(handle.play("motion", PRawAnimation.begin().thenLoop(CASES[(scenario / 3) % CASES.length]).build()), "motion controller start");
			}
			mc.options.setCameraType(scenario % 2 == 0 ? CameraType.FIRST_PERSON : CameraType.THIRD_PERSON_BACK);
			startTick = mc.player.tickCount;
			lastTransitionTick = -1;
			return;
		}
		if (mc.player.tickCount < startTick + 4)
		{
			verifyTransition(mc.player);
			return;
		}
		verify(mc.player);
		scenario++;
		startTick = -1;
	}

	private static void verify(Player player)
	{
		Object before = pendingWorld();
		try (var scope = PRenderContexts.enter(PRenderContexts.Context.GUI))
		{
			try
			{
				verifyMatrices(player);
			}
			finally
			{
				scope.discard();
			}
		}
		check(before.equals(pendingWorld()), "Direct matrix probes preserve every pending world submission");
	}

	private static void verifyMatrices(Player player)
	{
		Minecraft mc = Minecraft.getInstance();

		List<Player> players = List.copyOf(mc.level.players());
		List<Snapshot> before = players.stream().map(Snapshot::new).toList();
		// Bracket both render paths and the following animation tick in one client frame.
		PPlayerAnimationFrame frame = PPlayerAnimations.getInstance(player, selected).sampleFrame(1);
		check(frame != null, "evaluated player frame");
		PBakedModel model = frame.definition().modelData().getModel();
		Matrix4f full = sequential(frame, model, "item", false);
		Matrix4f relative = sequential(frame, model, "root", false).invert().mul(full);
		Matrix4f actual = new Matrix4f();
		check(frame.modelMatrix("item", actual), "item matrix");
		matrix(full, actual, "all parent matrices");
		check(frame.currentRootRelativeMatrix("item", actual), "root-relative item");
		matrix(relative, actual, "one ROOT");
		PFirstPersonPresentation presentation = PFirstPersonPresentation.create(frame);
		check(presentation != null && presentation.anchorMatrix(PPlayerAnimationAnchors.RIGHT_ITEM, actual), "view-space item");
		matrix(sequential(frame, model, "camera", false).invert().mul(full), actual, "camera/item hierarchy");
		PoseStack stack = new PoseStack();
		PPlayerAnimations.applyRoot(player, stack, 1);
		Matrix4f outer = new Matrix4f(stack.last().pose());
		verifyWeightedRoot(player, frame, outer);
		expectedAnchor = new Matrix4f(outer).mul(PPlayerAnimationSpace.toPlayerSpace(relative, frame.definition()));
		try
		{
			PPlayerAnimatedAttachments.renderThirdPerson(mc.player, stack, mc.gameRenderer.getFeatureRenderDispatcher().getSubmitNodeStorage(), 0xF000F0, 1);
		}
		finally
		{
			expectedAnchor = null;
		}
		var meshes = PPlayerAnimations.meshAttachmentPoses(player, 1).stream().filter(pose -> pose.animation().equals(selected)).toList();
		check(meshes.size() == 1 && meshes.getFirst().root().name().equals("ball"), "automatic mesh inherits animated parent without a BALL channel; scenario=" + scenario + " selected=" + selected + " roots=" + meshes.stream().map(pose -> pose.animation() + ":" + pose.root().name()).toList() + " active=" + frame.resolver().activeAnimationBones());
		Matrix4f ball = sequential(frame, model, "root", false).invert().mul(sequential(frame, model, "ball", false));
		expectedMesh = new Matrix4f(outer).mul(PPlayerAnimationSpace.toPlayerGeometrySpace(ball, frame.definition()));
		capturedMeshes = 0;
		queuedMeshes = 0;
		try
		{
			PPlayerAutomaticMeshAttachments.renderThirdPerson(meshes, stack, 0xF000F0);
			check(capturedMeshes > 0 && queuedMeshes == capturedMeshes, "Real third-person mesh reaches the compiler");
		}
		finally
		{
			expectedMesh = null;
		}
		var commands = PPlayerAnimations.firstPersonPresentation(mc.player, 1);
		check(commands != null && commands.meshAttachments().stream().filter(pose -> pose.animation().equals(selected)).count() == 1, "first-person commands");
		firstPersonProbe = true;
		expectedMesh = sequential(frame, model, "camera", false).invert().mul(sequential(frame, model, "ball", false));
		capturedMeshes = 0;
		queuedMeshes = 0;
		try
		{
			PPlayerAutomaticMeshAttachments.renderFirstPerson(commands.meshAttachments().stream().filter(pose -> pose.animation().equals(selected)).toList(), new PoseStack(), 0xF000F0);
			check(capturedMeshes > 0 && queuedMeshes == capturedMeshes, "Real first-person mesh reaches the FIRST_PERSON compiler stage");
		}
		finally
		{
			expectedMesh = null;
			firstPersonProbe = false;
		}
		expectedAnchor = sequential(frame, model, "camera", false).invert().mul(full);
		try
		{
			PPlayerAnimatedAttachments.renderFirstPerson(mc.player, commands, new PoseStack(), mc.gameRenderer.getFeatureRenderDispatcher().getSubmitNodeStorage(), 0xF000F0, 1);
		}
		finally
		{
			expectedAnchor = null;
		}
		PPlayerAnimations.tick(mc.level);
		for (int index = 0; index < players.size(); index++)
			before.get(index).check(players.get(index));

		int formatSpan = CASES.length * 3 * PPlayerAnimationBlendMode.values().length;
		if (scenario % formatSpan == 10 && lastFailurePhase != (reloaded ? 2 : 0) + scenario / formatSpan)
			failureRecovery(commands, frame);
		checked++;
	}

	private static void createObservers()
	{
		Minecraft mc = Minecraft.getInstance();
		for (int index = 0; index < OBSERVERS.length; index++)
		{
			UUID uuid = OBSERVERS[index];
			if (mc.level.players().stream().anyMatch(player -> player.getUUID().equals(uuid)))
				continue;

			RemotePlayer observer = new RemotePlayer(mc.level, new GameProfile(OBSERVERS[index], "MotionObserver" + index));
			observer.setId(0x505001 + index);
			observer.setPos(mc.player.getX() + (index == 0 ? -1 : 1), mc.player.getY() + 0.5, mc.player.getZ() + 2);
			mc.level.addEntity(observer);
		}
	}

	private static void stopFixtures(Player player)
	{
		for (Identifier id : IDS)
			PPlayerAnimations.getHandle(player, id).stopAll();

		for (Identifier id : POLICY_IDS)
			PPlayerAnimations.getHandle(player, id).stopAll();
	}

	private static void verifyTransition(Player player)
	{
		int tick = player.tickCount;
		if (tick > startTick && tick != lastTransitionTick)
		{
			verify(player);
			lastTransitionTick = tick;
		}
	}

	private static void verifyWeightedRoot(Player player, PPlayerAnimationFrame frame, Matrix4f actual)
	{
		PPlayerAnimationDefinition definition = frame.definition();
		PPlayerAnimationInstance instance = PPlayerAnimations.getInstance(player, selected);
		float activation = (Float)Stage41bGeometryProbe.field(PPlayerAnimationInstance.class, instance, "activation");
		float rootWeight = definition.appliesTo(player, PPlayerPart.ROOT, 1) ? definition.weight(player, 1) * activation * definition.partWeight(player, PPlayerPart.ROOT, 1) * definition.boneWeight(player, "root", 1) : 0;
		BoneFrame root = frame.localTransform("root");
		var bind = definition.modelData().getModel().bone(definition.modelData().getModel().boneIndex("root"));
		Vector3f translation = new Vector3f(root.translation()).sub(bind.basePosition()).mul(rootWeight);
		Vector3f pivot = definition.rootPivot();
		Quaternionf rotation = new Quaternionf().slerp(new Quaternionf(root.rotation()).mul(new Quaternionf(bind.baseRotation()).invert()), rootWeight);
		Vector3f scale = new Vector3f(1).lerp(root.scale(), rootWeight);
		if (definition.modelData().getModelLoaderId().equals(PGltfModelLoader.INSTANCE.id()))
		{
			translation.mul(-1, -1, 1);
			pivot.mul(-1, -1, 1);
			rotation.set(-rotation.x, -rotation.y, rotation.z, rotation.w);
		}
		Matrix4f expected = new Matrix4f().translate(translation).translate(pivot).rotate(rotation).scale(scale).translate(new Vector3f(pivot).negate());
		matrix(expected, actual, "weighted ROOT, mask and fade");
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
		Matrix4f expected = new Matrix4f().translate(pivot).rotateY(sign * (float)Math.PI / 2 * 0.7f).translate(new Vector3f(pivot).negate()).translate(sign * 0.4f, 0, 0);
		matrix(expected, stack.last().pose(), "two weighted visual roots");
		for (Identifier id : List.of(selected, secondary))
		{
			PPlayerAnimationFrame frame = PPlayerAnimations.getInstance(player, id).sampleFrame(1);
			PBakedModel model = frame.definition().modelData().getModel();
			Matrix4f actual = new Matrix4f();
			check(PPlayerAnimations.attachmentMatrix(player, id, PPlayerAnimationAnchors.RIGHT_ITEM, 1, actual), "mixed attachment");
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
			chain.addFirst(index);

		Matrix4f result = new Matrix4f();
		for (int index : chain)
			if (bind)
				result.translate(model.bone(index).basePosition()).rotate(model.bone(index).baseRotation());

			else
			{
				BoneFrame local = frame.localTransform(model.bone(index).name());
				result.translate(local.translation()).rotate(local.rotation()).scale(local.scale());
			}

		return result;
	}

	private static void matrix(Matrix4f expected, Matrix4f actual, String label)
	{
		check(expected.equals(actual, 0.0001f), label + " expected=" + expected + " actual=" + actual);
	}

	public static void register()
	{
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,
																	 (GameShuttingDownEvent event) ->
																	 {
																		 check(completed, "The attachment matrix and native GPU suites must complete before shutdown");
																		 PLibDatabase.LOGGER.info("STAGE 5.2a ATTACHMENTS SHUTDOWN PASS");
																	 });
	}

	/**
	 * Observes the actual compiler append without cancelling or replacing production code.
	 */
	public static void queued(PRenderQueue.RenderStage stage,
							  PGeometryData geometry, Matrix4f transform)
	{
		if (expectedMesh != null)
		{
			matrix(expectedMesh, transform, "compiled mesh matrix");
			check(stage == (firstPersonProbe ? PRenderQueue.RenderStage.FIRST_PERSON : PRenderQueue.RenderStage.ENTITIES), "Mesh reaches the intended render stage");
			queuedMeshes++;
			return;
		}
		int format = fixtureFormat(geometry);
		if (format < 0)
			return;
		int phase = reloaded ? 1 : 0;
		if (stage == PRenderQueue.RenderStage.FIRST_PERSON)
		{
			check(PRenderContexts.current() == PRenderContexts.Context.FIRST_PERSON,
				  "Native first-person mesh belongs to the real hand invocation");
			nativeFirst[phase][format]++;
			nativePendingTriangles += geometry.indexCount() / 3;
			nativePendingFormats |= 1 << format;
		}
		else if (stage == PRenderQueue.RenderStage.ENTITIES)
			nativeThird[phase][format]++;
	}

	public static int beginHandFlush()
	{
		if (nativePendingTriangles == 0)
			return 0;
		int query = GL15.glGenQueries();
		GL15.glBeginQuery(GL30.GL_PRIMITIVES_GENERATED, query);
		return query;
	}

	public static void endHandFlush(int query)
	{
		if (query == 0)
			return;
		try
		{
			GL15.glEndQuery(GL30.GL_PRIMITIVES_GENERATED);
			int primitives = GL15.glGetQueryObjecti(query, GL15.GL_QUERY_RESULT);
			check(primitives >= nativePendingTriangles && primitives % nativePendingTriangles == 0,
				  "Actual GPU hand flush draws the queued attachment triangles, including native OIT passes");
			for (int format = 0; format < 2; format++)
				if ((nativePendingFormats & (1 << format)) != 0)
					gpuFrames[reloaded ? 1 : 0][format]++;
			gpuPrimitives += primitives;
		}
		finally
		{
			GL15.glDeleteQueries(query);
			nativePendingTriangles = 0;
			nativePendingFormats = 0;
		}
	}

	private static int fixtureFormat(PGeometryData geometry)
	{
		for (int format = 0; format < MODELS.size(); format++)
		{
			PBakedModel model = MODELS.get(format).getModel();
			if (model == null)
				continue;
			for (int index = 0; index < model.boneCount(); index++)
				for (var mesh : model.bone(index).meshes())
					if (mesh.geometry() == geometry)
						return format;
		}
		return -1;
	}

	private static Object pendingWorld()
	{
		Object compiler = Stage41bGeometryProbe.field(PRenderQueue.class, null, "COMPILER");
		return List.of(snapshot(Stage41bGeometryProbe.field(compiler.getClass(), compiler, "opaque")),
					   snapshot(Stage41bGeometryProbe.field(compiler.getClass(), compiler, "translucent")));
	}

	private static Object snapshot(Object value)
	{
		if (value instanceof Map<?, ?> map)
		{
			Map<Object, Object> copy = new HashMap<>();
			map.forEach((key, entry) -> copy.put(key, snapshot(entry)));
			return copy;
		}
		return value instanceof List<?> list ? List.copyOf(list) : value;
	}

	private static void failureRecovery(PFirstPersonRenderPresentation commands,
	                                    PPlayerAnimationFrame frame)
	{
		var mc = Minecraft.getInstance();
		var collector = mc.gameRenderer.getFeatureRenderDispatcher().getSubmitNodeStorage();
		PoseStack stack = new PoseStack();
		stack.translate(0.1f, 0.2f, 0.3f);
		stack.mulPose(new Quaternionf().rotationY(0.2f));
		Matrix4f before = new Matrix4f(stack.last().pose());
		for (boolean firstPerson : new boolean[] {false, true})
		{
			failCallback = true;
			try
			{
				if (firstPerson)
					PPlayerAnimatedAttachments.renderFirstPerson(mc.player, commands, stack, collector, 0xF000F0, 1);
				else
					PPlayerAnimatedAttachments.renderThirdPerson(mc.player, stack, collector, 0xF000F0, 1);
				throw new AssertionError("The attachment callback must propagate its exception");
			}
			catch (IllegalStateException expected)
			{
				check(expected.getMessage().equals("stage52a expected callback failure"), "Expected callback failure propagated");
			}
			finally
			{
				failCallback = false;
			}
			matrix(before, stack.last().pose(), "caller restored after callback exception");
			check(stack.isEmpty(), "Failed attachment invocation balances PoseStack");
		}
		PBakedModel model = frame.definition().modelData().getModel();
		Matrix4f exact = sequential(frame, model, "camera", false).invert().mul(sequential(frame, model, "item", false));
		Vector3f offset = new Vector3f(0.125f, 0.25f, -0.15f);
		var sampled = PTransform.fromMatrix(exact);
		var edited = new PTransform(sampled.translation().add(offset), sampled.rotation(), sampled.scale());
		var anchor = new PPlayerFirstPersonAnchorPose(selected, PPlayerAnimationAnchors.RIGHT_HAND, edited, 1);
		var changed = new PFirstPersonRenderPresentation(commands.rightArm(), commands.leftArm(), commands.rightItem(), commands.leftItem(), List.of(anchor), List.of());
		expectedAnchor = new Matrix4f(before).translate(offset).mul(exact);
		try
		{
			PPlayerAnimatedAttachments.renderFirstPerson(mc.player, changed, stack, collector, 0xF000F0, 1);
		}
		finally
		{
			expectedAnchor = null;
		}
		matrix(before, stack.last().pose(), "successful attachment after exception restores caller");
		var pose = commands.meshAttachments().stream().filter(value -> value.animation().equals(selected)).findFirst().orElseThrow();
		Matrix4f ball = sequential(frame, model, "camera", false).invert().mul(sequential(frame, model, "ball", false));
		var meshTransform = PTransform.fromMatrix(ball);
		var meshEdit = new PTransform(meshTransform.translation().add(offset), meshTransform.rotation(), meshTransform.scale());
		var changedMesh = new PPlayerFirstPersonMeshAttachmentPose(pose.animation(), pose.modelData(), pose.root(), pose.frame(), meshEdit, pose.weight());
		expectedMesh = new Matrix4f(before).translate(offset).mul(ball);
		capturedMeshes = 0;
		queuedMeshes = 0;
		firstPersonProbe = true;
		try
		{
			PPlayerAutomaticMeshAttachments.renderFirstPerson(List.of(changedMesh), stack, 0xF000F0);
			check(capturedMeshes > 0 && capturedMeshes == queuedMeshes, "Edited mesh commands retain the exact affine matrix and queue stage");
		}
		finally
		{
			expectedMesh = null;
			firstPersonProbe = false;
		}
		matrix(before, stack.last().pose(), "mesh command restores caller");
		lastFailurePhase = (reloaded ? 2 : 0) + fixtureFormat(pose.root().meshes().getFirst().geometry());
		failureSuites++;
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
			throw new AssertionError("Stage 5.2a: " + message);
	}

	private record Snapshot(Vec3 position, Vec3 movement, AABB bounds, float width, float height)
	{
		Snapshot(Player player)
		{
			this(player.position(), player.getDeltaMovement(), player.getBoundingBox(), player.getBbWidth(), player.getBbHeight());
		}

		void check(Player player)
		{
			Stage52AttachmentProbe.check(this.position.equals(player.position()) && this.movement.equals(player.getDeltaMovement()) &&
											 this.bounds.equals(player.getBoundingBox()) && this.width == player.getBbWidth() && this.height == player.getBbHeight(),
										 "root animation tick/render/extraction must preserve position, movement, bounds and dimensions");
		}
	}
}
