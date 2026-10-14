/**
 * @author ArcAnc
 * Created at: 14.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;

import com.arcanc.pulselib.content.animatable.AnimManagerKey;
import com.arcanc.pulselib.content.animatable.ControllerState;
import com.arcanc.pulselib.content.animatable.PAnimatable;
import com.arcanc.pulselib.content.animatable.PAnimationController;
import com.arcanc.pulselib.content.animatable.PAnimationManager;
import com.arcanc.pulselib.content.model.baked.PBakedBone;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.registration.PLibRegistration;
import com.arcanc.pulselib.data.gecko.MolangParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static com.arcanc.pulselib.content.model.animation.PTransitionTestSupport.assertPose;
import static com.arcanc.pulselib.content.model.animation.PTransitionTestSupport.expectedBlend;
import static com.arcanc.pulselib.content.model.animation.PTransitionTestSupport.field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PGraphRestartTest
{
	@Test
	void interruptedGraphKeepsTheActualBoneTransform()
	{
		PAnimationController<Dummy> controller = new PAnimationController<>(new PAnimationGraph(
				List.of(new PAnimationState.Clip("walk", "walk"), new PAnimationState.Clip("run", "run"),
						new PAnimationState.Clip("attack", "attack")),
				List.of(new PAnimationTransition(0, 1, PCondition.triggered("run"), 10.0f, 0, PInterruptionPolicy.FROM_CURRENT),
						new PAnimationTransition(1, 2, PCondition.triggered("attack"), 10.0f, 0, PInterruptionPolicy.RESTART))));
		PBakedModel model = new PBakedModel(List.of(new PBakedBone("root", new Vector3f(), new Quaternionf(), List.of(), null, List.of())),
				Map.of("walk", clip("walk", 10.0f), "run", clip("run", 20.0f), "attack", clip("attack", 30.0f)));
		PAnimationGraphRuntime runtime = controller.graphRuntime();
		runtime.parameters().trigger("run");
		runtime.tickPrecise(model, 0.0f);
		runtime.tickPrecise(model, 4.0f);
		float before = evaluate(model, controller).translation(0).x;
		assertEquals(14.0f, before, 0.00001f);
		runtime.parameters().trigger("attack");
		runtime.tickPrecise(model, 0.0f);
		assertEquals(before, evaluate(model, controller).translation(0).x, 0.00001f);
	}

	@Test
	void restartFromIdleHasContinuousTrsAndAnIndependentFrozenSource()
	{
		PBakedModel model = PTransitionTestSupport.model();
		PAnimationController<Dummy> controller = controller(PInterruptionPolicy.RESTART, PInterruptionPolicy.RESTART, 10);
		PAnimationGraphRuntime runtime = controller.graphRuntime();
		runtime.tickPrecise(model, 20);
		PPose before = base(model, runtime, 1);
		request(model, runtime, "walk");
		assertPose(before, base(model, runtime, 0));
		assertPose(before, base(model, runtime, 0.5f));
		assertPose(before, base(model, runtime, 1));
		PPose leaked = base(model, runtime, 1);
		leaked.translation(0).set(900);
		leaked.rotation(0).identity();
		leaked.scale(0).set(9);
		assertPose(before, base(model, runtime, 1));
		runtime.tickPrecise(model, 5);
		PPose target = sampledClip(model, "walk", 5);
		assertPose(expectedBlend(before, target, 0.5f), base(model, runtime, 1));
		assertEquals(1.0f, base(model, runtime, 1).rotation(0).lengthSquared(), 0.00001f);
	}

	@Test
	void captureUsesTheRequestTimeInsteadOfThePreviousRender()
	{
		PBakedModel model = PTransitionTestSupport.model();
		PAnimationGraphRuntime runtime = controller(PInterruptionPolicy.RESTART, PInterruptionPolicy.RESTART, 10).graphRuntime();
		runtime.tickPrecise(model, 20);
		PPose staleRender = base(model, runtime, 0.25f);
		PPose actualRequest = base(model, runtime, 1);
		assertTrue(staleRender.translation(0).distance(actualRequest.translation(0)) > 0.1f);
		request(model, runtime, "walk");
		assertPose(actualRequest, base(model, runtime, 0.25f));
		runtime.tickPrecise(model, 2);
		PPose target = sampledClip(model, "walk", 1);
		assertPose(expectedBlend(actualRequest, target, 0.1f), base(model, runtime, 0.5f));
	}

	@Test
	void fortyPercentInterruptionAndEveryFollowingRestartKeepAllBoneTransforms()
	{
		PBakedModel model = PTransitionTestSupport.model();
		PAnimationController<Dummy> controller = controller(PInterruptionPolicy.FROM_CURRENT, PInterruptionPolicy.RESTART, 10);
		PAnimationGraphRuntime runtime = controller.graphRuntime();
		request(model, runtime, "walk");
		runtime.tickPrecise(model, 4);
		for (String target : List.of("run", "attack", "idle", "walk", "run"))
		{
			PPose before = evaluate(model, controller);
			request(model, runtime, target);
			assertPose(before, evaluate(model, controller));
			runtime.tickPrecise(model, 4);
		}
	}

	@Test
	void thousandsOfRestartsKeepOneSnapshotAndNoLiveOutgoingPlaybacks()
	{
		PBakedModel model = PTransitionTestSupport.model();
		PAnimationGraphRuntime runtime = controller(PInterruptionPolicy.RESTART, PInterruptionPolicy.RESTART, 10).graphRuntime();
		String[] targets = {"walk", "run", "attack", "idle"};
		for (int interruption = 0; interruption < 3000; interruption++)
		{
			PPose before = base(model, runtime, 1);
			request(model, runtime, targets[interruption % 4]);
			assertPose(before, base(model, runtime, 1));
			Object transition = field(runtime, "transition");
			assertTrue(((List<?>)field(transition, "sources")).isEmpty());
			assertEquals(1, field(transition, "sourcePose").getClass().getDeclaredFields().length);
			runtime.tickPrecise(model, 1);
		}
		runtime.tickPrecise(model, 10);
		assertFalse(runtime.isTransitioning());
		assertEquals(null, field(runtime, "transition"));
	}

	@Test
	void movingAndPausedOverlaysRemainSeparateAndAreAppliedOnce()
	{
		for (float speed : new float[]{-1, 0, 1})
		{
			PBakedModel model = PTransitionTestSupport.model();
			PAnimationController<Dummy> controller = controller(PInterruptionPolicy.FROM_CURRENT, PInterruptionPolicy.RESTART, 10);
			PAnimationGraphRuntime runtime = controller.graphRuntime();
			runtime.parameters().trigger("overlay");
			runtime.tickPrecise(model, 0);
			runtime.setOverlaySpeed("extra", speed);
			if (speed < 0)
			{
				runtime.restartOverlay("extra");
			}
			request(model, runtime, "walk");
			runtime.tickPrecise(model, 4);
			PPose before = evaluate(model, controller);
			var overlays = (Map<?, ?>)field(runtime, "overlays");
			Object instance = overlays.get(4);
			float overlayTime = runtime.layers(model).stream().filter(PAnimationGraphRuntime.Layer::overlay).findFirst().orElseThrow().time();
			request(model, runtime, "run");
			assertPose(before, evaluate(model, controller));
			assertTrue(instance == overlays.get(4));
			assertEquals(1, overlays.size());
			assertEquals(overlayTime, runtime.layers(model).stream().filter(PAnimationGraphRuntime.Layer::overlay).findFirst().orElseThrow().time());
			runtime.tickPrecise(model, 1);
			assertEquals(overlayTime + speed, runtime.layers(model).stream().filter(PAnimationGraphRuntime.Layer::overlay).findFirst().orElseThrow().time());
			PPose base = base(model, runtime, 1);
			PPose overlay = sampledClip(model, "overlay", overlayTime + speed);
			PPose visual = evaluate(model, controller);
			assertTrue(new Vector3f(model.bone(0).basePosition()).add(base.translation(0)).add(overlay.translation(0)).equals(visual.translation(0), 0.0001f));
			assertTrue(new Vector3f(base.scale(0)).mul(overlay.scale(0)).equals(visual.scale(0), 0.0001f));
			Quaternionf rotation = new Quaternionf(model.bone(0).baseRotation()).premul(base.rotation(0)).premul(overlay.rotation(0));
			assertEquals(1, Math.abs(rotation.dot(visual.rotation(0))), 0.00001f);
		}
	}

	@Test
	void sparseAndExplicitIdentityChannelsAndVisibilitySurviveRestart()
	{
		PBakedModel sourceModel = PTransitionTestSupport.model();
		Map<String, PAnimation> clips = new LinkedHashMap<>(sourceModel.animations());
		PAnimation sparse = PTransitionTestSupport.clip("idle", 10, 10, false);
		clips.put("idle", new PAnimation("idle", 100, sparse.boneAnimations(), List.of(),
				Map.of("root", new PAnimationVisibilityTrack(List.of(new PAnimationVisibilityTrack.Keyframe(0, false))))));
		clips.put("walk", PTransitionTestSupport.clip("walk", 0, 0, true));
		PBakedModel model = new PBakedModel(sourceModel.bones(), clips);
		PAnimationController<Dummy> controller = controller(PInterruptionPolicy.RESTART, PInterruptionPolicy.RESTART, 10);
		PAnimationGraphRuntime runtime = controller.graphRuntime();
		request(model, runtime, "walk");
		PPose snapshot = base(model, runtime, 1);
		assertTrue(snapshot.hasTranslation(0));
		assertFalse(snapshot.hasRotation(0));
		assertFalse(snapshot.hasScale(0));
		assertFalse(snapshot.isValid(1));
		PAnimationPoseResolver<Dummy> resolver = new PAnimationPoseResolver<>(model, List.of(controller), PAnimationPoseResolver.defaultContexts(), 1);
		assertFalse(resolver.isVisible(model.bone(0)));
		assertTrue(resolver.activeAnimationBones().contains("root"));
		runtime.tickPrecise(model, 10);
		PPose target = base(model, runtime, 1);
		assertTrue(target.hasRotation(0));
		assertTrue(target.hasScale(0));
		assertTrue(target.isVisible(0));
	}

	@Test
	void zeroDurationAndBeginningAndEndInterruptionsAreContinuous()
	{
		PBakedModel model = PTransitionTestSupport.model();
		for (float time : new float[]{0, 0.0001f, 9.9999f, 10})
		{
			PAnimationGraphRuntime runtime = controller(PInterruptionPolicy.RESTART, PInterruptionPolicy.RESTART, 10).graphRuntime();
			request(model, runtime, "walk");
			runtime.tickPrecise(model, time);
			PPose before = base(model, runtime, 1);
			request(model, runtime, "run");
			assertPose(before, base(model, runtime, 1));
		}
		PAnimationGraphRuntime runtime = controller(PInterruptionPolicy.RESTART, PInterruptionPolicy.RESTART, 0).graphRuntime();
		request(model, runtime, "walk");
		assertPose(sampledClip(model, "walk", 0), base(model, runtime, 1));
		assertFalse(runtime.isTransitioning());
	}

	@Test
	void fromCurrentKeepsAnimatingAndCompleteCurrentKeepsItsGate()
	{
		PBakedModel model = PTransitionTestSupport.model();
		PAnimationGraphRuntime runtime = controller(PInterruptionPolicy.FROM_CURRENT, PInterruptionPolicy.FROM_CURRENT, 10).graphRuntime();
		request(model, runtime, "walk");
		runtime.tickPrecise(model, 4);
		request(model, runtime, "run");
		var tracks = runtime.tickPrecise(model, 1);
		assertEquals(3, tracks.stream().filter(track -> !track.animation().equals("overlay")).count());
		// At the next tick outgoing IDLE/WALK remain live, at 60% / 40% of their source mix.
		float expected = (10 + 5 * 0.1f) * 0.6f * 0.9f + (20 + 5 * 0.2f) * 0.4f * 0.9f + (30 + 1 * 0.3f) * 0.1f;
		assertEquals(expected, base(model, runtime, 1).translation(0).x, 0.0001f);
		runtime = controller(PInterruptionPolicy.COMPLETE_CURRENT, PInterruptionPolicy.RESTART, 10).graphRuntime();
		request(model, runtime, "walk");
		runtime.tickPrecise(model, 4);
		request(model, runtime, "run");
		assertEquals("walk", runtime.state().name());
		assertTrue(runtime.parameters().isTriggered("run"));
		runtime.tickPrecise(model, 6);
		assertEquals("run", runtime.state().name());
	}

	@Test
	void fromCurrentAfterRestartRetainsTheLiveTargetWithoutAJolt()
	{
		PBakedModel model = PTransitionTestSupport.model();
		PAnimationGraphRuntime runtime = controller(PInterruptionPolicy.RESTART, PInterruptionPolicy.FROM_CURRENT, 10).graphRuntime();
		request(model, runtime, "walk");
		runtime.tickPrecise(model, 4);
		PPose before = base(model, runtime, 1);
		request(model, runtime, "run");
		assertPose(before, base(model, runtime, 1));
		runtime.tickPrecise(model, 1);
		float outgoing = 10 * 0.6f + (20 + 5 * 0.2f) * 0.4f;
		assertEquals(outgoing * 0.9f + (30 + 1 * 0.3f) * 0.1f, base(model, runtime, 1).translation(0).x, 0.0001f);
	}

	@Test
	void restartPreservesSynchronizedCyclePhase()
	{
		PBakedModel model = PTransitionTestSupport.model();
		PAnimationGraph graph = new PAnimationGraph(List.of(new PAnimationState.Clip("idle", "idle", PAnimationType.CYCLE, PInterpolationType.LINEAR, 1, true),
				new PAnimationState.Clip("walk", "walk", PAnimationType.CYCLE, PInterpolationType.LINEAR, 2, true)),
				List.of(new PAnimationTransition(0, 1, PCondition.triggered("walk"), 10, 0, PInterruptionPolicy.RESTART)));
		PAnimationGraphRuntime runtime = new PAnimationGraphRuntime(graph);
		runtime.tickPrecise(model, 40);
		PPose before = base(model, runtime, 1);
		request(model, runtime, "walk");
		assertEquals(0.4f, runtime.cyclePhase(model), 0.00001f);
		assertPose(before, base(model, runtime, 1));
		runtime.tickPrecise(model, 1);
		assertEquals(0.42f, runtime.cyclePhase(model), 0.00001f);
		runtime.syncCycle(model, 0.7f);
		assertEquals(0.7f, runtime.cyclePhase(model), 0.00001f);
	}

	@Test
	void snapshotDoesNotEmitEventsAndPausedOverlayResumesItsEventWindowOnce()
	{
		PBakedModel original = PTransitionTestSupport.model();
		List<String> events = new ArrayList<>();
		PAnimationEventType<String> type = eventType(events);
		Map<String, PAnimation> clips = new LinkedHashMap<>();
		original.animations().forEach((name, clip) -> clips.put(name, new PAnimation(name, clip.length(), clip.boneAnimations(),
				List.of(new PAnimationEvent<>(1, type, name + "1"), new PAnimationEvent<>(3, type, name + "3")))));
		PBakedModel model = new PBakedModel(original.bones(), clips);
		PAnimationController<Dummy> controller = controller(PInterruptionPolicy.RESTART, PInterruptionPolicy.RESTART, 10);
		PAnimationGraphRuntime runtime = controller.graphRuntime();
		Dummy dummy = new Dummy();
		runtime.parameters().trigger("overlay");
		controller.tick(dummy, 0, model);
		controller.tick(dummy, 1, model);
		runtime.setOverlaySpeed("extra", 0);
		runtime.parameters().trigger("walk");
		controller.tick(dummy, 0, model);
		controller.tick(dummy, 2, model);
		runtime.parameters().trigger("run");
		controller.tick(dummy, 0, model);
		controller.tick(dummy, 2, model);
		runtime.setOverlaySpeed("extra", 1);
		controller.tick(dummy, 2, model);
		assertEquals(List.of("idle1", "overlay1", "walk1", "run1", "run3", "overlay3"), events);
	}

	@Test
	void multipleControllersAndRootMotionKeepLocalAndWorldTransformsSeparate()
	{
		PBakedModel model = PTransitionTestSupport.model();
		PAnimationController<Dummy> first = new PAnimationController<>(new PAnimationGraph(List.of(new PAnimationState.Clip("run", "run")), List.of()));
		PAnimationController<Dummy> second = controller(PInterruptionPolicy.RESTART, PInterruptionPolicy.RESTART, 10);
		List<PAnimationController<Dummy>> controllers = List.of(first, second);
		first.tick(new Dummy(), 20, model, controllers);
		second.tick(new Dummy(), 20, model, controllers);
		PPose before = PAnimationRuntime.evaluate(model, controllers, PAnimationPoseResolver.defaultContexts(), 1);
		second.graphRuntime().parameters().trigger("walk");
		second.tick(new Dummy(), 0, model, controllers);
		assertPose(before, PAnimationRuntime.evaluate(model, controllers, PAnimationPoseResolver.defaultContexts(), 1));
		PRootMotionDelta delta = PAnimationRuntime.extractRootMotion(model.animations().get("walk"), "root", 0, 5, PInterpolationType.LINEAR, null);
		Quaternionf rootStart = new Quaternionf().rotationXYZ(0.4f, -0.6f, 0.8f);
		Vector3f rootExpected = new Vector3f(1, 2, -1).rotate(new Quaternionf(rootStart).invert());
		assertTrue(rootExpected.equals(delta.translation(), 0.0001f));
		Quaternionf rootEnd = new Quaternionf(rootStart).slerp(new Quaternionf().rotationXYZ(0.8f, -1.2f, 1.6f), 0.05f);
		Quaternionf rootRotation = new Quaternionf(rootStart).invert().mul(rootEnd).normalize();
		assertEquals(1, Math.abs(rootRotation.dot(delta.rotation())), 0.00001f);
		PModelPose worldBefore = new PModelPose(model.boneCount());
		worldBefore.update(model, before);
		PModelPose worldAfter = new PModelPose(model.boneCount());
		worldAfter.update(model, PAnimationRuntime.evaluate(model, controllers, PAnimationPoseResolver.defaultContexts(), 1));
		assertTrue(worldBefore.transform(0).matrix().equals(worldAfter.transform(0).matrix(), 0.0001f));
	}

	@Test
	void pausedOverlayFreezesComputedChannelsUntilResumeOrRestart()
	{
		PBakedModel original = PTransitionTestSupport.model();
		AtomicInteger value = new AtomicInteger(5);
		PAnimationValue<Vector3f> expression = (context, destination) -> destination.set(value.get(), 0, 0);
		PAnimationTrack<Vector3f> track = new PAnimationTrack<>(PLibRegistration.AnimationChannelReg.POSITION,
				List.of(new PKeyframe<>(0, expression, expression, PInterpolationType.LINEAR)));
		Map<String, PAnimation> clips = new LinkedHashMap<>(original.animations());
		clips.put("overlay", new PAnimation("overlay", 100, Map.of("root", new PBoneAnimation(UUID.randomUUID(), Map.of("position", track)))));
		PBakedModel model = new PBakedModel(original.bones(), clips);
		PAnimationController<Dummy> controller = controller(PInterruptionPolicy.RESTART, PInterruptionPolicy.RESTART, 10);
		PAnimationGraphRuntime runtime = controller.graphRuntime();
		runtime.parameters().trigger("overlay");
		runtime.tickPrecise(model, 0);
		runtime.tickPrecise(model, 5);
		runtime.setOverlaySpeed("extra", 0);
		PPose paused = evaluate(model, controller);
		value.set(50);
		assertPose(paused, evaluate(model, controller));
		request(model, runtime, "walk");
		assertPose(paused, evaluate(model, controller));
		runtime.setOverlaySpeed("extra", 1);
		assertEquals(paused.translation(0).x + 45, evaluate(model, controller).translation(0).x, 0.0001f);
		runtime.setOverlaySpeed("extra", 0);
		runtime.restartOverlay("extra");
		assertEquals(0, runtime.layers(model).stream().filter(PAnimationGraphRuntime.Layer::overlay).findFirst().orElseThrow().time());
	}

	@Test
	void multipleIndependentOverlaysKeepTheirCombinedPoseDuringRestart()
	{
		PBakedModel model = PTransitionTestSupport.model();
		PAnimationGraph original = controller(PInterruptionPolicy.FROM_CURRENT, PInterruptionPolicy.RESTART, 10).graphRuntime().graph();
		List<PAnimationState> states = new ArrayList<>(original.states());
		states.add(new PAnimationState.OneShotOverlay("second", "overlay", "overlay", 0, 0, PInterpolationType.LINEAR, 0, false));
		PAnimationController<Dummy> controller = new PAnimationController<>(new PAnimationGraph(states, original.transitions()));
		PAnimationGraphRuntime runtime = controller.graphRuntime();
		runtime.parameters().trigger("overlay");
		request(model, runtime, "walk");
		runtime.tickPrecise(model, 4);
		PPose before = evaluate(model, controller);
		request(model, runtime, "run");
		assertPose(before, evaluate(model, controller));
		assertEquals(2, ((Map<?, ?>)field(runtime, "overlays")).size());
	}

	@Test
	void customSnapshotSamplingUsesCurrentProceduralContextAndPrecedingControllers()
	{
		PBakedModel model = PTransitionTestSupport.model();
		PAnimationGraphRuntime runtime = controller(PInterruptionPolicy.RESTART, PInterruptionPolicy.RESTART, 10).graphRuntime();
		PPose custom = new PPose(model.boneCount());
		custom.translation(0, new Vector3f(7, 8, 9));
		runtime.parameters().trigger("walk");
		runtime.tickPrecise(model, 0, (boundModel, graph) -> custom);
		custom.translation(0).set(100);
		assertTrue(base(model, runtime, 1).translation(0).equals(new Vector3f(7, 8, 9), 0.0001f));
	}

	@Test
	void controllerSamplesFreshGameQueriesAtTheRequestBoundary()
	{
		PBakedModel original = PTransitionTestSupport.model();
		MolangParser.Expression expression = MolangParser.parse("query.pose_value");
		PAnimationValue<Vector3f> value = (context, destination) -> destination.set(context.evaluate(expression), 0, 0);
		PAnimationTrack<Vector3f> track = new PAnimationTrack<>(PLibRegistration.AnimationChannelReg.POSITION,
				List.of(new PKeyframe<>(0, value, value, PInterpolationType.LINEAR)));
		Map<String, PAnimation> clips = new LinkedHashMap<>(original.animations());
		clips.put("idle", new PAnimation("idle", 100, Map.of("root", new PBoneAnimation(UUID.randomUUID(), Map.of("position", track)))));
		PBakedModel model = new PBakedModel(original.bones(), clips);
		PAnimationController<Dummy> controller = controller(PInterruptionPolicy.RESTART, PInterruptionPolicy.RESTART, 10);
		controller.persistentMolangContext().query("pose_value", -20);
		AtomicInteger current = new AtomicInteger(42);
		controller.setGraphSnapshotContexts((candidate, partialTick) ->
		{
			assertEquals(1, partialTick);
			return new MolangParser.Context().query("pose_value", current.get());
		});
		controller.trigger("walk");
		controller.tick(new Dummy(), 0, model);
		current.set(100);
		assertEquals(42, base(model, controller.graphRuntime(), 1).translation(0).x);
	}

	@Test
	void rawControllerFrameQueriesSurviveAlongsideAGraphSnapshot()
	{
		PBakedModel original = PTransitionTestSupport.model();
		MolangParser.Expression expression = MolangParser.parse("query.pose_value");
		PAnimationValue<Vector3f> value = (context, destination) -> destination.set(context.evaluate(expression), 0, 0);
		PAnimationTrack<Vector3f> track = new PAnimationTrack<>(PLibRegistration.AnimationChannelReg.POSITION,
				List.of(new PKeyframe<>(0, value, value, PInterpolationType.LINEAR)));
		Map<String, PAnimation> clips = new LinkedHashMap<>(original.animations());
		clips.put("idle", new PAnimation("idle", 100, Map.of("root", new PBoneAnimation(UUID.randomUUID(), Map.of("position", track)))));
		PBakedModel model = new PBakedModel(original.bones(), clips);
		PAnimationController<Dummy> raw = new PAnimationController<>(state -> ControllerState.PLAY);
		raw.play(PRawAnimation.begin().thenLoop("idle").build());
		raw.tick(new Dummy(), 1, model);
		PAnimationPoseResolver.MolangContextProvider<Dummy> contexts = (candidate, partialTick) -> new MolangParser.Context().query("pose_value", 42);
		assertEquals(44, PAnimationRuntime.evaluate(model, List.of(raw), contexts, 1).translation(0).x);
		PAnimationController<Dummy> graph = controller(PInterruptionPolicy.RESTART, PInterruptionPolicy.RESTART, 10);
		graph.setGraphSnapshotContexts(contexts);
		List<PAnimationController<Dummy>> controllers = List.of(raw, graph);
		PPose before = PAnimationRuntime.evaluate(model, controllers, contexts, 1);
		graph.trigger("walk");
		graph.tick(new Dummy(), 0, model, controllers);
		assertPose(before, PAnimationRuntime.evaluate(model, controllers, contexts, 1));
	}

	private static PAnimationEventType<String> eventType(List<String> events)
	{
		return new PAnimationEventType<>()
		{
			@Override
			public ResourceLocation id()
			{
				return ResourceLocation.fromNamespaceAndPath("pulselibtest", "restart_event");
			}

			@Override
			public MapCodec<String> codec()
			{
				return Codec.STRING.fieldOf("data");
			}

			@Override
			public PEventSide side()
			{
				return PEventSide.BOTH;
			}

			@Override
			public void execute(PAnimationEventContext context, String data)
			{
				events.add(data);
			}
		};
	}

	private static PAnimationController<Dummy> controller(PInterruptionPolicy first, PInterruptionPolicy following, float duration)
	{
		return new PAnimationController<>(new PAnimationGraph(List.of(new PAnimationState.Clip("idle", "idle"),
				new PAnimationState.Clip("walk", "walk"), new PAnimationState.Clip("run", "run"), new PAnimationState.Clip("attack", "attack"),
				new PAnimationState.OneShotOverlay("extra", "overlay", "overlay")),
				List.of(new PAnimationTransition(0, 1, PCondition.triggered("walk"), duration, 0, first),
						new PAnimationTransition(1, 2, PCondition.triggered("run"), duration, 0, following),
						new PAnimationTransition(2, 3, PCondition.triggered("attack"), duration, 0, following),
						new PAnimationTransition(3, 0, PCondition.triggered("idle"), duration, 0, following))));
	}

	private static void request(PBakedModel model, PAnimationGraphRuntime runtime, String trigger)
	{
		runtime.parameters().trigger(trigger);
		runtime.tickPrecise(model, 0);
	}

	private static PPose base(PBakedModel model, PAnimationGraphRuntime runtime, float partial)
	{
		return runtime.sampleBasePose(model, partial, new MolangParser.Context(), model.bindPose());
	}

	private static PPose sampledClip(PBakedModel model, String name, float time)
	{
		return PAnimationRuntime.sampleGraphLayers(model,
				List.of(new PAnimationGraphRuntime.Layer(name, time, PInterpolationType.LINEAR, 1, false)),
				new MolangParser.Context(), model.bindPose());
	}

	private static PPose evaluate(PBakedModel model, PAnimationController<Dummy> controller)
	{
		return PAnimationRuntime.evaluate(model, List.of(controller), PAnimationPoseResolver.defaultContexts(), 1.0f);
	}

	private static PAnimation clip(String name, float position)
	{
		PAnimationValue<Vector3f> value = (context, destination) -> destination.set(position, 0, 0);
		PAnimationTrack<Vector3f> track = new PAnimationTrack<>(PLibRegistration.AnimationChannelReg.POSITION,
				List.of(new PKeyframe<>(0.0f, value, value, strength -> strength)));
		return new PAnimation(name, 100.0f, Map.of("root", new PBoneAnimation(UUID.randomUUID(), Map.of("position", track))));
	}

	private static final class Dummy implements PAnimatable<Dummy>
	{
		@Override
		public PAnimationManager<Dummy> getAnimationManager(AnimManagerKey key)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void registerAnimationControllers(PAnimationManager.PAnimationRegistrar<Dummy> registrar)
		{
		}
	}
}
