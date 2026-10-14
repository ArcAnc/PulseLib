/**
 * @author ArcAnc
 * Created at: 14.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test;

import com.arcanc.pulselib.content.animatable.AnimManagerKey;
import com.arcanc.pulselib.content.animatable.PAnimatable;
import com.arcanc.pulselib.content.animatable.PAnimationController;
import com.arcanc.pulselib.content.animatable.PAnimationManager;
import com.arcanc.pulselib.content.model.animation.PAnimationGraph;
import com.arcanc.pulselib.content.model.animation.PAnimationGraphRuntime;
import com.arcanc.pulselib.content.model.animation.PAnimationPoseResolver;
import com.arcanc.pulselib.content.model.animation.PAnimationRuntime;
import com.arcanc.pulselib.content.model.animation.PAnimationState;
import com.arcanc.pulselib.content.model.animation.PAnimationTransition;
import com.arcanc.pulselib.content.model.animation.PCondition;
import com.arcanc.pulselib.content.model.animation.PInterruptionPolicy;
import com.arcanc.pulselib.content.model.animation.PInterpolationType;
import com.arcanc.pulselib.content.model.animation.PPose;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.util.PLibDatabase;

import java.util.List;
import java.util.Map;

import static com.arcanc.pulselib.test.SmokeHarness.check;
import static com.arcanc.pulselib.test.SmokeHarness.field;

/** Runs only inside the opt-in client smoke, using the original loaded idle/walk model. */
final class AnimationLifecycleHarness
{
	private AnimationLifecycleHarness()
	{
	}

	static void run(PBakedModel model)
	{
		check(model.animations().containsKey("idle") && model.animations().containsKey("walk"), "L1/L4 smoke requires the original idle/walk clips");
		for (float speed : new float[]{-1, 0, 1})
		{
			PAnimationController<Dummy> controller = new PAnimationController<>(new PAnimationGraph(
					List.of(new PAnimationState.Clip("idle", "idle"), new PAnimationState.Clip("walk", "walk"),
							new PAnimationState.Clip("attack", "idle"),
							new PAnimationState.OneShotOverlay("extra", "extra", "walk", 2, 3, PInterpolationType.LINEAR, speed, false)),
					List.of(new PAnimationTransition(0, 1, PCondition.triggered("walk"), 10, 0, PInterruptionPolicy.FROM_CURRENT),
							new PAnimationTransition(1, 2, PCondition.triggered("attack"), 10, 0, PInterruptionPolicy.RESTART))));
			PAnimationGraphRuntime runtime = controller.graphRuntime();
			runtime.tickPrecise(model, 10);
			runtime.parameters().trigger("extra");
			runtime.parameters().trigger("walk");
			runtime.tickPrecise(model, 0);
			runtime.tickPrecise(model, 2);
			if (speed < 0)
			{
				check(runtime.layers(model).stream().filter(PAnimationGraphRuntime.Layer::overlay).findFirst().orElseThrow().time() ==
						model.animations().get("walk").length() - 2, "Reverse overlay must advance from the clip end");
			}
			runtime.tickPrecise(model, 2);
			PPose before = pose(model, controller, 1);
			runtime.parameters().trigger("attack");
			runtime.tickPrecise(model, 0);
			assertPose(before, pose(model, controller, 1));
			assertPose(before, pose(model, controller, 0.5f));
			Map<?, ?> overlays = (Map<?, ?>)field(PAnimationGraphRuntime.class, "overlays", runtime);
			Object instance = overlays.get(3);
			runtime.setOverlaySpeed("extra", 0);
			if (speed < 0)
			{
				float time = runtime.layers(model).stream().filter(PAnimationGraphRuntime.Layer::overlay).findFirst().orElseThrow().time();
				check(time == model.animations().get("walk").length() - 4, "Reverse clip cursor must decrease");
			}
			for (int trigger = 0; trigger < 1000; trigger++)
			{
				runtime.parameters().trigger("extra");
				runtime.tickPrecise(model, 0);
				check(overlays.size() == 1 && overlays.get(3) == instance, "L1 duplicate overlay instance");
			}
			runtime.tickPrecise(model, 2);
			check(runtime.overlaySpeed("extra") == 0, "L1 retrigger lost paused speed");
			runtime.stopOverlay("extra");
			runtime.tickPrecise(model, 1);
			check(runtime.layers(model).stream().anyMatch(layer -> layer.overlay() && layer.weight() > 0 && layer.weight() < 1), "L1 paused cancellation must fade");
			runtime.tickPrecise(model, 2);
			check(overlays.isEmpty(), "L1 cancellation must release the instance");
			runtime.tickPrecise(model, 10);
			check(!runtime.isTransitioning(), "L4 transition must finish");
			runtime.reset();
			check(overlays.isEmpty(), "L1 reset must release overlays");
		}
		PLibDatabase.LOGGER.info("PULSE TEST L1/L4 PASS: {} bones, loaded clips {}, forward/reverse/paused overlay and pose snapshot", model.boneCount(), model.animations().keySet());
	}

	private static PPose pose(PBakedModel model, PAnimationController<Dummy> controller, float partialTick)
	{
		return PAnimationRuntime.evaluate(model, List.of(controller), PAnimationPoseResolver.defaultContexts(), partialTick);
	}

	private static void assertPose(PPose expected, PPose actual)
	{
		for (int bone = 0; bone < expected.boneCount(); bone++)
		{
			check(expected.translation(bone).equals(actual.translation(bone), 0.0001f), "L4 translation discontinuity at bone " + bone);
			check(expected.scale(bone).equals(actual.scale(bone), 0.0001f), "L4 scale discontinuity at bone " + bone);
			check(Math.abs(Math.abs(expected.rotation(bone).dot(actual.rotation(bone))) - 1) < 0.0001f, "L4 rotation discontinuity at bone " + bone);
			check(expected.isVisible(bone) == actual.isVisible(bone), "L4 visibility discontinuity at bone " + bone);
		}
	}

	private static final class Dummy implements PAnimatable<Dummy>
	{
		@Override
		public PAnimationManager<Dummy> getAnimationManager(AnimManagerKey key)
		{
			throw new UnsupportedOperationException("Direct smoke runtime does not register managers");
		}

		@Override
		public void registerAnimationControllers(PAnimationManager.PAnimationRegistrar<Dummy> registrar)
		{
		}
	}
}
