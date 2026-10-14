/**
 * @author ArcAnc
 * Created at: 14.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;

import com.arcanc.pulselib.content.model.baked.PBakedModel;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class POverlayLifecycleTest
{
	private final PBakedModel model = new PBakedModel(List.of(), Map.of(
			"idle", new PAnimation("idle", 100.0f, Map.of()),
			"clip", new PAnimation("clip", 100.0f, Map.of())));

	@Test
	void repeatedTriggersKeepOneInstancePerDefinition()
	{
		PAnimationGraphRuntime runtime = runtime(overlay("statue", "wake", 0.0f, 0.0f, 0.0f));
		for (int tick = 0; tick < 10000; tick++)
		{
			runtime.parameters().trigger("wake");
			runtime.tickPrecise(this.model, 1.0f);
			assertEquals(1, overlays(runtime).size());
			assertEquals(1, instances(runtime).size());
		}
	}

	@Test
	void sharedTriggerActivatesEveryDefinition()
	{
		PAnimationGraphRuntime runtime = runtime(overlay("first", "wake", 0.0f, 0.0f, 0.0f),
				overlay("second", "wake", 0.0f, 0.0f, 0.0f));
		runtime.parameters().trigger("wake");
		runtime.tickPrecise(this.model, 1.0f);
		assertEquals(2, overlays(runtime).size());
	}

	@Test
	void normalPlaybackFadesAndReleasesItsInstance()
	{
		PAnimationGraphRuntime runtime = runtime(overlay("statue", "wake", 1.0f, 10.0f, 10.0f));
		trigger(runtime);
		runtime.tickPrecise(this.model, 5.0f);
		assertEquals(0.5f, overlays(runtime).getFirst().weight());
		assertEquals(5.0f, overlays(runtime).getFirst().time());
		runtime.tickPrecise(this.model, 90.0f);
		assertEquals(0.5f, overlays(runtime).getFirst().weight());
		runtime.tickPrecise(this.model, 5.0f);
		assertTrue(instances(runtime).isEmpty());
	}

	@Test
	void zeroSpeedHoldsForOneHundredThousandTicks()
	{
		PAnimationGraphRuntime runtime = runtime(overlay("statue", "wake", 0.0f, 0.0f, 10.0f));
		trigger(runtime);
		Object instance = instances(runtime).get(1);
		for (int tick = 0; tick < 100000; tick++)
		{
			assertTrue(runtime.tickPrecise(this.model, 1.0f).stream().noneMatch(track -> track.animation().equals("clip")));
		}
		assertEquals(1, instances(runtime).size());
		assertSame(instance, instances(runtime).get(1));
		assertEquals(0.0f, overlays(runtime).getFirst().time());
		assertEquals(1.0f, overlays(runtime).getFirst().weight());
	}

	@Test
	void runtimeSpeedIntegratesWithoutReinterpretingTheElapsedTime()
	{
		PAnimationGraphRuntime runtime = runtime(overlay("statue", "wake", 1.0f, 0.0f, 0.0f));
		trigger(runtime);
		runtime.tickPrecise(this.model, 65.0f);
		runtime.setOverlaySpeed("statue", 0.0f);
		runtime.tickPrecise(this.model, 1000.0f);
		assertEquals(65.0f, overlays(runtime).getFirst().time());
		runtime.setOverlaySpeed(1, 0.5f);
		assertEquals(0.5f, runtime.overlaySpeed("statue"));
		var resumed = runtime.tickPrecise(this.model, 10.0f).getLast();
		assertEquals(65.0, resumed.from());
		assertEquals(70.0, resumed.to());
		runtime.setOverlaySpeed("statue", 1.0f);
		runtime.tickPrecise(this.model, 10.0f);
		assertEquals(80.0f, overlays(runtime).getFirst().time());
	}

	@Test
	void retriggerDuringFadeInRetainsTheInstanceAndRuntimeSpeed()
	{
		PAnimationGraphRuntime runtime = runtime(overlay("statue", "wake", 1.0f, 10.0f, 10.0f));
		trigger(runtime);
		runtime.tickPrecise(this.model, 5.0f);
		runtime.setOverlaySpeed("statue", 0.0f);
		Object instance = instances(runtime).get(1);
		trigger(runtime);
		assertSame(instance, instances(runtime).get(1));
		assertEquals(0.0f, runtime.overlaySpeed(1));
		runtime.tickPrecise(this.model, 5.0f);
		assertEquals(0.0f, overlays(runtime).getFirst().time());
		assertEquals(0.5f, overlays(runtime).getFirst().weight());
	}

	@Test
	void manualFadeOutRunsWhilePausedAndRepeatedCancellationDoesNotResetIt()
	{
		PAnimationGraphRuntime runtime = runtime(overlay("statue", "wake", 0.0f, 0.0f, 10.0f));
		trigger(runtime);
		assertTrue(runtime.stopOverlay("statue"));
		runtime.tickPrecise(this.model, 4.0f);
		assertEquals(0.6f, overlays(runtime).getFirst().weight(), 0.00001f);
		assertTrue(runtime.stopOverlay(1));
		runtime.tickPrecise(this.model, 6.0f);
		assertTrue(instances(runtime).isEmpty());
		assertFalse(runtime.stopOverlay("statue"));
	}

	@Test
	void cancellationFreezesTheClipAndStartsAtItsCurrentFadeInWeight()
	{
		PAnimationGraphRuntime runtime = runtime(overlay("statue", "wake", 1.0f, 10.0f, 10.0f));
		trigger(runtime);
		runtime.tickPrecise(this.model, 5.0f);
		runtime.stopOverlay("statue");
		assertEquals(0.5f, overlays(runtime).getFirst().weight());
		runtime.tickPrecise(this.model, 5.0f);
		assertEquals(5.0f, overlays(runtime).getFirst().time());
		assertEquals(0.25f, overlays(runtime).getFirst().weight());
	}

	@Test
	void restartDuringEitherFadeOutCancelsCompletion()
	{
		for (boolean manual : new boolean[]{false, true})
		{
			PAnimationGraphRuntime runtime = runtime(overlay("statue", "wake", 1.0f, 0.0f, 10.0f));
			trigger(runtime);
			runtime.tickPrecise(this.model, 95.0f);
			if (manual)
			{
				runtime.stopOverlay("statue");
			}
			Object instance = instances(runtime).get(1);
			trigger(runtime);
			assertSame(instance, instances(runtime).get(1));
			assertEquals(1.0f, overlays(runtime).getFirst().weight());
			assertEquals(0.0f, overlays(runtime).getFirst().time());
			runtime.tickPrecise(this.model, 10.0f);
			assertEquals(1, instances(runtime).size());
		}
	}

	@Test
	void immediateCancellationAndResetReleasePausedInstances()
	{
		PAnimationGraphRuntime runtime = runtime(overlay("statue", "wake", 0.0f, 0.0f, 0.0f));
		assertFalse(runtime.restartOverlay("statue"));
		trigger(runtime);
		assertTrue(runtime.restartOverlay("statue"));
		runtime.stopOverlay("statue");
		assertTrue(instances(runtime).isEmpty());
		trigger(runtime);
		runtime.reset();
		assertTrue(instances(runtime).isEmpty());
	}

	@Test
	void definitionsHaveIndependentSpeedAndCancellation()
	{
		PAnimationGraphRuntime runtime = runtime(overlay("first", "wake", 1.0f, 0.0f, 0.0f),
				overlay("second", "wake", 0.5f, 0.0f, 0.0f));
		trigger(runtime);
		runtime.setOverlaySpeed("first", 0.0f);
		runtime.tickPrecise(this.model, 10.0f);
		assertEquals(0.0f, overlays(runtime).getFirst().time());
		assertEquals(5.0f, overlays(runtime).getLast().time());
		runtime.stopOverlay("first");
		assertEquals(1, instances(runtime).size());
		assertEquals(0.5f, runtime.overlaySpeed("second"));
	}

	@Test
	void missingClipFailsBeforeActivationWithTheStateAndClipNames()
	{
		PAnimationGraphRuntime runtime = runtime(overlay("statue", "wake", 0.0f, 0.0f, 0.0f));
		runtime.parameters().trigger("wake");
		var failure = assertThrows(IllegalStateException.class,
				() -> runtime.tickPrecise(new PBakedModel(List.of(), Map.of()), 1.0f));
		assertTrue(failure.getMessage().contains("statue"));
		assertTrue(failure.getMessage().contains("clip"));
		assertTrue(instances(runtime).isEmpty());
		assertTrue(runtime.parameters().isTriggered("wake"));
	}

	@Test
	void stationaryTicksDoNotDispatchAnOverlayEventWindow()
	{
		PAnimationGraphRuntime runtime = runtime(overlay("statue", "wake", 1.0f, 0.0f, 0.0f));
		trigger(runtime);
		assertEquals(0.0, runtime.tickPrecise(this.model, 5.0f).getLast().from());
		runtime.setOverlaySpeed(1, 0.0f);
		for (int tick = 0; tick < 10; tick++)
		{
			assertTrue(runtime.tickPrecise(this.model, 1.0f).stream().noneMatch(track -> track.animation().equals("clip")));
		}
		runtime.setOverlaySpeed(1, 1.0f);
		assertEquals(5.0, runtime.tickPrecise(this.model, 1.0f).getLast().from());
		assertTrue(runtime.tickPrecise(this.model, 0.0f).stream().noneMatch(track -> track.animation().equals("clip")));
	}

	@Test
	void invalidSpeedsAndUnknownOrInactiveStatesFailExplicitly()
	{
		PAnimationGraphRuntime runtime = runtime(overlay("statue", "wake", 1.0f, 0.0f, 0.0f));
		assertThrows(IllegalStateException.class, () -> runtime.overlaySpeed("statue"));
		assertThrows(IllegalArgumentException.class, () -> runtime.stopOverlay("unknown"));
		assertThrows(IllegalArgumentException.class, () -> runtime.stopOverlay(0));
		trigger(runtime);
		for (float speed : new float[]{Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY})
		{
			assertThrows(IllegalArgumentException.class, () -> runtime.setOverlaySpeed(1, speed));
			assertThrows(IllegalArgumentException.class, () -> overlay("bad", "wake", speed, 0.0f, 0.0f));
		}
		assertEquals(1.0f, runtime.overlaySpeed(1));
	}

	@Test
	void ambiguousNamesCanBeControlledByStateIndex()
	{
		PAnimationGraphRuntime runtime = runtime(overlay("same", "wake", 0.0f, 0.0f, 0.0f),
				overlay("same", "wake", 0.0f, 0.0f, 0.0f));
		trigger(runtime);
		assertThrows(IllegalArgumentException.class, () -> runtime.overlaySpeed("same"));
		runtime.stopOverlay(1);
		assertEquals(1, instances(runtime).size());
		assertEquals(0.0f, runtime.overlaySpeed(2));
	}

	@Test
	void overlayFadeWeightsInterpolateIndependentlyOfPausedClipTime()
	{
		PAnimationGraphRuntime runtime = runtime(overlay("statue", "wake", 0, 10, 10));
		trigger(runtime);
		runtime.tickPrecise(this.model, 4);
		assertEquals(0.2f, runtime.layers(this.model, 0.5f).stream().filter(PAnimationGraphRuntime.Layer::overlay).findFirst().orElseThrow().weight());
		runtime.stopOverlay("statue");
		runtime.tickPrecise(this.model, 4);
		assertEquals(0.32f, runtime.layers(this.model, 0.5f).stream().filter(PAnimationGraphRuntime.Layer::overlay).findFirst().orElseThrow().weight(), 0.00001f);
	}

	@Test
	void queuedRetriggerKeepsTheInstanceWhenThisTickWouldOtherwiseFinishIt()
	{
		for (boolean manual : new boolean[]{false, true})
		{
			PAnimationGraphRuntime runtime = runtime(overlay("statue", "wake", 1, 0, 10));
			trigger(runtime);
			runtime.tickPrecise(this.model, 95);
			runtime.setOverlaySpeed("statue", manual ? 0 : 0.5f);
			if (manual)
			{
				runtime.stopOverlay("statue");
			}
			Object instance = instances(runtime).get(1);
			runtime.parameters().trigger("wake");
			runtime.tickPrecise(this.model, 10);
			assertSame(instance, instances(runtime).get(1));
			assertEquals(manual ? 0 : 0.5f, runtime.overlaySpeed("statue"));
			assertEquals(0, overlays(runtime).getFirst().time());
			assertEquals(1, overlays(runtime).getFirst().weight());
		}
	}

	private void trigger(PAnimationGraphRuntime runtime)
	{
		runtime.parameters().trigger("wake");
		runtime.tickPrecise(this.model, 0.0f);
	}

	private static Map<?, ?> instances(PAnimationGraphRuntime runtime)
	{
		try
		{
			Field field = PAnimationGraphRuntime.class.getDeclaredField("overlays");
			field.setAccessible(true);
			return (Map<?, ?>)field.get(runtime);
		}
		catch (ReflectiveOperationException exception)
		{
			throw new AssertionError(exception);
		}
	}

	private List<PAnimationGraphRuntime.Layer> overlays(PAnimationGraphRuntime runtime)
	{
		return runtime.layers(this.model).stream().filter(PAnimationGraphRuntime.Layer::overlay).toList();
	}

	private static PAnimationState.OneShotOverlay overlay(String name, String trigger, float speed, float fadeIn, float fadeOut)
	{
		return new PAnimationState.OneShotOverlay(name, trigger, "clip", fadeIn, fadeOut, PInterpolationType.LINEAR, speed, false);
	}

	private static PAnimationGraphRuntime runtime(PAnimationState.OneShotOverlay... overlays)
	{
		var states = new ArrayList<PAnimationState>();
		states.add(new PAnimationState.Clip("idle", "idle"));
		states.addAll(List.of(overlays));
		return new PAnimationGraphRuntime(new PAnimationGraph(states, List.of()));
	}
}
