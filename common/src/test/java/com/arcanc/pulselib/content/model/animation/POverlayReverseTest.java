/**
 * @author ArcAnc
 * Created at: 14.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;

import com.arcanc.pulselib.content.animatable.PAnimationController;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.data.gecko.MolangParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.arcanc.pulselib.content.model.animation.PTransitionTestSupport.assertPose;
import static com.arcanc.pulselib.content.model.animation.PTransitionTestSupport.field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class POverlayReverseTest
{
	private final PBakedModel model = new PBakedModel(PTransitionTestSupport.model().bones(), Map.of(
			"idle", new PAnimation("idle", 100, Map.of()), "clip", PTransitionTestSupport.clip("clip", 0, 100, true)));

	@Test
	void negativeDefinitionStartsAtTheActualClipEnd()
	{
		PAnimationGraphRuntime runtime = runtime(-1, 0, 0);
		trigger(runtime);
		assertEquals(100, layer(runtime).time());
		assertPose(sampled(100), pose(runtime, 1));
		assertEquals(100, pose(runtime, 1).translation(0).x);
		assertEquals(200, pose(runtime, 1).translation(0).y);
		assertEquals(-100, pose(runtime, 1).translation(0).z);
		assertEquals(2, pose(runtime, 1).scale(0).x);
		assertEquals(3, pose(runtime, 1).scale(0).y);
		assertEquals(4, pose(runtime, 1).scale(0).z);
		runtime.tickPrecise(this.model, 10);
		assertEquals(90, layer(runtime).time());
		assertPose(sampled(90), pose(runtime, 1));
	}

	@Test
	void changingDirectionPreservesCursorAndInterpolatesBackwards()
	{
		PAnimationGraphRuntime runtime = runtime(1, 0, 0);
		trigger(runtime);
		runtime.tickPrecise(this.model, 40);
		runtime.setOverlaySpeed("action", -2);
		assertEquals(40, layer(runtime).time());
		var track = runtime.tickPrecise(this.model, 5).getLast();
		assertEquals(40, track.from());
		assertEquals(30, track.to());
		assertEquals(35, runtime.layers(this.model, 0.5f).getLast().time());
		assertPose(sampled(35), pose(runtime, 0.5f));
		runtime.setOverlaySpeed("action", 0.5f);
		assertEquals(30, layer(runtime).time());
		runtime.tickPrecise(this.model, 6);
		assertEquals(33, layer(runtime).time());
	}

	@Test
	void reverseCompletionClampsAtZeroAndReleasesTheInstance()
	{
		PAnimationGraphRuntime runtime = runtime(-2, 0, 0);
		trigger(runtime);
		var track = runtime.tickPrecise(this.model, 150).getLast();
		assertEquals("clip", track.animation());
		assertEquals(100, track.from());
		assertEquals(0, track.to());
		assertTrue(instances(runtime).isEmpty());
		assertTrue(runtime.tickPrecise(this.model, 1).stream().noneMatch(window -> window.animation().equals("clip")));
	}

	@Test
	void reversePauseHoldsForOneHundredThousandTicksAndResumesWithoutSeeking()
	{
		PAnimationGraphRuntime runtime = runtime(-1, 4, 10);
		trigger(runtime);
		runtime.tickPrecise(this.model, 35);
		runtime.setOverlaySpeed("action", 0);
		Object instance = instances(runtime).get(1);
		PPose frozen = pose(runtime, 1);
		for (int tick = 0; tick < 100000; tick++)
		{
			assertTrue(runtime.tickPrecise(this.model, 1).stream().noneMatch(track -> track.animation().equals("clip")));
		}
		assertEquals(65, layer(runtime).time());
		assertPose(frozen, pose(runtime, 1));
		assertSame(instance, instances(runtime).get(1));
		runtime.setOverlaySpeed(1, -0.5f);
		var track = runtime.tickPrecise(this.model, 10).getLast();
		assertEquals(65, track.from());
		assertEquals(60, track.to());
		assertPose(sampled(60), pose(runtime, 1));
	}

	@Test
	void restartAndRetriggerRememberReverseDirectionWhilePaused()
	{
		PAnimationGraphRuntime runtime = runtime(-0.5f, 0, 0);
		trigger(runtime);
		runtime.tickPrecise(this.model, 40);
		runtime.setOverlaySpeed("action", 0);
		Object instance = instances(runtime).get(1);
		assertTrue(runtime.restartOverlay("action"));
		assertEquals(100, layer(runtime).time());
		for (int trigger = 0; trigger < 10000; trigger++)
		{
			trigger(runtime);
			assertEquals(1, instances(runtime).size());
			assertSame(instance, instances(runtime).get(1));
		}
		assertEquals(0, runtime.overlaySpeed("action"));
		assertEquals(100, layer(runtime).time());
		runtime.setOverlaySpeed("action", -1);
		runtime.tickPrecise(this.model, 5);
		assertEquals(95, layer(runtime).time());
		runtime.restartOverlay(1);
		assertEquals(100, layer(runtime).time());
		runtime.setOverlaySpeed("action", 1);
		runtime.restartOverlay(1);
		assertEquals(0, layer(runtime).time());
		assertEquals(-0.5f, runtime.graph().states().get(1).speed());
	}

	@Test
	void reverseFadeInAndOutUseWallTimeAndDistanceToTheStart()
	{
		PAnimationGraphRuntime runtime = runtime(-2, 10, 10);
		trigger(runtime);
		runtime.tickPrecise(this.model, 5);
		assertEquals(90, layer(runtime).time());
		assertEquals(0.5f, layer(runtime).weight());
		runtime.tickPrecise(this.model, 40);
		assertEquals(10, layer(runtime).time());
		assertEquals(0.5f, layer(runtime).weight());
		runtime.tickPrecise(this.model, 5);
		assertTrue(instances(runtime).isEmpty());
	}

	@Test
	void reverseRetriggerDuringFadesOrAtCompletionKeepsTheInstanceAndSpeed()
	{
		PAnimationGraphRuntime runtime = runtime(-1, 10, 10);
		trigger(runtime);
		Object instance = instances(runtime).get(1);
		runtime.tickPrecise(this.model, 5);
		trigger(runtime);
		assertSame(instance, instances(runtime).get(1));
		assertEquals(100, (float)((Number)field(field(instance, "playback"), "time")).doubleValue());
		runtime.tickPrecise(this.model, 95);
		assertEquals(0.5f, layer(runtime).weight());
		runtime.setOverlaySpeed("action", -0.5f);
		runtime.parameters().trigger("action");
		runtime.tickPrecise(this.model, 20);
		assertSame(instance, instances(runtime).get(1));
		assertEquals(-0.5f, runtime.overlaySpeed("action"));
		runtime.tickPrecise(this.model, 5);
		assertEquals(97.5f, layer(runtime).time());
		assertEquals(0.5f, layer(runtime).weight());
	}

	@Test
	void reversingDuringAutomaticFadeOutRecoversWeightWithoutAJump()
	{
		for (float speed : new float[]{1, -1})
		{
			PAnimationGraphRuntime runtime = runtime(speed, 0, 10);
			trigger(runtime);
			runtime.tickPrecise(this.model, 95);
			PPose before = pose(runtime, 1);
			assertEquals(0.5f, layer(runtime).weight());
			runtime.setOverlaySpeed("action", -speed);
			assertPose(before, pose(runtime, 1));
			assertEquals(0.5f, layer(runtime).weight());
			runtime.tickPrecise(this.model, 5);
			assertEquals(0.75f, layer(runtime).weight());
			assertEquals(speed > 0 ? 90 : 10, layer(runtime).time());
			runtime.setOverlaySpeed("action", 0);
			runtime.tickPrecise(this.model, 5);
			assertEquals(1, layer(runtime).weight());
			runtime.setOverlaySpeed("action", -speed);
			runtime.tickPrecise(this.model, 85);
			assertEquals(0.5f, layer(runtime).weight());
			runtime.tickPrecise(this.model, 5);
			assertTrue(instances(runtime).isEmpty());
		}
	}

	@Test
	void changingDirectionDoesNotCancelAnExplicitStop()
	{
		PAnimationGraphRuntime runtime = runtime(-1, 0, 10);
		trigger(runtime);
		runtime.tickPrecise(this.model, 20);
		runtime.setOverlaySpeed("action", 0);
		runtime.stopOverlay("action");
		runtime.tickPrecise(this.model, 4);
		assertEquals(80, layer(runtime).time());
		assertEquals(0.6f, layer(runtime).weight(), 0.00001f);
		runtime.setOverlaySpeed("action", 1);
		assertEquals(0.6f, layer(runtime).weight(), 0.00001f);
		assertTrue(runtime.tickPrecise(this.model, 6).stream().noneMatch(track -> track.animation().equals("clip")));
		assertTrue(instances(runtime).isEmpty());
	}

	@Test
	void reverseRetriggerCancelsManualFadeOutAndResetReleasesAllState()
	{
		PAnimationGraphRuntime runtime = runtime(-1, 0, 10);
		trigger(runtime);
		Object instance = instances(runtime).get(1);
		runtime.tickPrecise(this.model, 50);
		runtime.stopOverlay("action");
		runtime.tickPrecise(this.model, 5);
		trigger(runtime);
		assertSame(instance, instances(runtime).get(1));
		assertEquals(100, layer(runtime).time());
		assertEquals(1, layer(runtime).weight());
		runtime.setOverlaySpeed("action", 0);
		runtime.reset();
		assertTrue(instances(runtime).isEmpty());
		trigger(runtime);
		assertEquals(-1, runtime.overlaySpeed("action"));
		assertEquals(100, layer(runtime).time());
	}

	@Test
	void signedSpeedsAreAcceptedButNonFiniteValuesAndNegativeBaseSpeedsStayInvalid()
	{
		PAnimationGraphRuntime runtime = runtime(-1, 0, 0);
		trigger(runtime);
		for (float speed : new float[]{-Float.MAX_VALUE, -Float.MIN_VALUE, -2, -0.0f, 0, 0.5f, 2})
		{
			runtime.setOverlaySpeed("action", speed);
			assertEquals(speed, runtime.overlaySpeed("action"));
			assertEquals(speed, runtime(speed, 0, 0).graph().states().get(1).speed());
		}
		for (float speed : new float[]{Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY})
		{
			assertThrows(IllegalArgumentException.class, () -> runtime.setOverlaySpeed("action", speed));
			assertThrows(IllegalArgumentException.class, () -> runtime(speed, 0, 0));
		}
		assertThrows(IllegalArgumentException.class, () -> new PAnimationState.Clip("base", "idle", PAnimationType.CYCLE,
				PInterpolationType.LINEAR, -1, false));
	}

	@Test
	void reverseEventsAreDescendingAndPauseOrDirectionChangesDoNotRepeatThePivot()
	{
		List<String> events = new ArrayList<>();
		PAnimationEventType<String> type = eventType(events);
		PAnimation clip = this.model.animations().get("clip");
		PBakedModel model = new PBakedModel(this.model.bones(), Map.of("idle", this.model.animations().get("idle"),
				"clip", new PAnimation("clip", 100, clip.boneAnimations(), List.of(new PAnimationEvent<>(0, type, "zero"),
						new PAnimationEvent<>(10, type, "early"), new PAnimationEvent<>(50, type, "a"),
						new PAnimationEvent<>(50, type, "b"), new PAnimationEvent<>(90, type, "late"), new PAnimationEvent<>(100, type, "end")))));
		PAnimationController<PTransitionTestSupport.Dummy> controller = new PAnimationController<>(runtime(-1, 0, 0).graph());
		PAnimationGraphRuntime runtime = controller.graphRuntime();
		PTransitionTestSupport.Dummy dummy = new PTransitionTestSupport.Dummy();
		controller.trigger("action");
		controller.tick(dummy, 0, model);
		controller.tick(dummy, 10, model);
		controller.tick(dummy, 40, model);
		runtime.setOverlaySpeed("action", 0);
		for (int tick = 0; tick < 10; tick++)
		{
			controller.tick(dummy, 1, model);
		}
		runtime.setOverlaySpeed("action", 1);
		controller.tick(dummy, 10, model);
		runtime.setOverlaySpeed("action", -1);
		controller.tick(dummy, 10, model);
		controller.tick(dummy, 50, model);
		assertEquals(List.of("late", "a", "b", "a", "b", "early", "zero"), events);
		assertTrue(instances(runtime).isEmpty());
	}

	@Test
	void differentReverseDefinitionsShareATriggerAndKeepIndependentDirectionAndTime()
	{
		PAnimationGraphRuntime runtime = new PAnimationGraphRuntime(new PAnimationGraph(List.of(new PAnimationState.Clip("idle", "idle"),
				new PAnimationState.OneShotOverlay("first", "action", "clip", 0, 0, PInterpolationType.LINEAR, -1, false),
				new PAnimationState.OneShotOverlay("second", "action", "clip", 0, 0, PInterpolationType.LINEAR, -0.5f, false)), List.of()));
		trigger(runtime);
		runtime.tickPrecise(this.model, 20);
		List<PAnimationGraphRuntime.Layer> layers = runtime.layers(this.model).stream().filter(PAnimationGraphRuntime.Layer::overlay).toList();
		assertEquals(80, layers.getFirst().time());
		assertEquals(90, layers.getLast().time());
		runtime.setOverlaySpeed("first", 0);
		runtime.restartOverlay("second");
		runtime.tickPrecise(this.model, 10);
		layers = runtime.layers(this.model).stream().filter(PAnimationGraphRuntime.Layer::overlay).toList();
		assertEquals(80, layers.getFirst().time());
		assertEquals(95, layers.getLast().time());
		runtime.stopOverlay("first");
		assertEquals(1, instances(runtime).size());
		assertEquals(-0.5f, runtime.overlaySpeed("second"));
	}

	@Test
	void zeroLengthClipsCompleteWithoutEventsUnlessPlaybackIsPaused()
	{
		PBakedModel model = new PBakedModel(this.model.bones(), Map.of("idle", this.model.animations().get("idle"),
				"clip", new PAnimation("clip", 0, Map.of())));
		for (float speed : new float[]{-1, 1, 0})
		{
			PAnimationGraphRuntime runtime = runtime(speed, 0, 0);
			runtime.parameters().trigger("action");
			runtime.tickPrecise(model, 0);
			assertTrue(runtime.tickPrecise(model, 1).stream().noneMatch(track -> track.animation().equals("clip")));
			assertEquals(speed == 0 ? 1 : 0, instances(runtime).size());
		}
	}

	private static PAnimationEventType<String> eventType(List<String> events)
	{
		return new PAnimationEventType<>()
		{
			@Override
			public ResourceLocation id()
			{
				return ResourceLocation.fromNamespaceAndPath("pulselibtest", "overlay_reverse");
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

	private static Map<?, ?> instances(PAnimationGraphRuntime runtime)
	{
		return (Map<?, ?>)field(runtime, "overlays");
	}

	private PPose sampled(float time)
	{
		return PAnimationRuntime.sampleGraphLayers(this.model,
				List.of(new PAnimationGraphRuntime.Layer("clip", time, PInterpolationType.LINEAR, 1, false)),
				new MolangParser.Context(), this.model.bindPose());
	}

	private PPose pose(PAnimationGraphRuntime runtime, float partialTick)
	{
		return runtime.samplePose(this.model, partialTick, new MolangParser.Context(), this.model.bindPose());
	}

	private PAnimationGraphRuntime.Layer layer(PAnimationGraphRuntime runtime)
	{
		return runtime.layers(this.model).stream().filter(PAnimationGraphRuntime.Layer::overlay).findFirst().orElseThrow();
	}

	private void trigger(PAnimationGraphRuntime runtime)
	{
		runtime.parameters().trigger("action");
		runtime.tickPrecise(this.model, 0);
	}

	private static PAnimationGraphRuntime runtime(float speed, float fadeIn, float fadeOut)
	{
		return new PAnimationGraphRuntime(new PAnimationGraph(List.of(new PAnimationState.Clip("idle", "idle"),
				new PAnimationState.OneShotOverlay("action", "action", "clip", fadeIn, fadeOut, PInterpolationType.LINEAR, speed, false)), List.of()));
	}
}
