/**
 * @author ArcAnc
 * Created at: 13.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;

import com.arcanc.pulselib.content.animatable.AnimManagerKey;
import com.arcanc.pulselib.content.animatable.ControllerState;
import com.arcanc.pulselib.content.animatable.PAnimatable;
import com.arcanc.pulselib.content.animatable.PAnimationManager;
import com.arcanc.pulselib.content.animatable.PAnimationController;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Timeout (value = 5, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class PCyclicAnimationEventsTest
{
	private final List<String> dispatched = new ArrayList<>();
	private final PAnimationEventType<String> type = new PAnimationEventType<>()
	{
		@Override
		public ResourceLocation id()
		{
			return ResourceLocation.fromNamespaceAndPath("pulselibtest", "numeric_event");
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
			dispatched.add(data);
		}
	};

	@ParameterizedTest
	@CsvSource ({"13.334,280,281", "13.334,-280,-281", "13.334,281,280", "13.334,-281,-280",
			"0.0001,0,0.01", "0.0001,0,-0.01", "0.125,16777216,16777217", "0.125,16777217,16777216",
			"2,1000000000000,1000000000006", "2,-1000000000000,-1000000000006"})
	void witnessesShortClipsAndLargeTimes(float length, double from, double to)
	{
		PAnimation animation = clip(length);
		assertEquals(reference(animation, from, to), collect(animation, from, to));
	}

	@Test
	void hugeRoundedAbsoluteBoundaryDoesNotFireTheEndMarkerEarly() throws ReflectiveOperationException
	{
		PAnimation animation = clip(100000.0f);
		double from = 1.0e20;
		double to = from + 100000.0;
		assertEquals(98304.0, to - from);
		assertEquals(List.of("zero", "early", "late"), collect(animation, from, to));
		assertEquals(List.of("late", "early", "zero"), collect(animation, to, from));
		assertEquals(List.of("zero", "early", "late", "end", "zero", "early", "late"),
				collect(animation, from, from + 200000.0));
		PAnimationGraphRuntime graph = new PAnimationGraphRuntime(new PAnimationGraph(
				List.of(new PAnimationState.Clip("idle", "clip")), List.of()));
		PBakedModel model = new PBakedModel(List.of(), Map.of("clip", animation));
		setGraphTime(graph, to);
		assertEquals(0.98304f, graph.cyclePhase(model));
		assertEquals(98304.0f, graph.layers(model).getFirst().time());
	}

	@Test
	void reverseWrapDoesNotDuplicateOrReorderANearEndEventInAShortClip()
	{
		float length = 0.0001f;
		PAnimation animation = new PAnimation("short", length, Map.of(),
				List.of(event(length - 0.0000005f, "near"), event(length, "end")));
		assertEquals(List.of("end", "near"), collect(animation, 0.000002, -0.000002));
	}

	@Test
	void emptyEventsDoNotVisitEvenPathologicalNumbersOfCycles()
	{
		PAnimation animation = new PAnimation("empty", Float.MIN_VALUE, Map.of());
		assertEquals(List.of(), collect(animation, -Double.MAX_VALUE, Double.MAX_VALUE));
	}

	@Test
	void zeroLengthAndStationaryPlaybackDoNotDispatch()
	{
		assertEquals(List.of(), collect(clip(0.0f), 0.0, 10.0));
		assertEquals(List.of(), collect(clip(2.0f), 0.0, 0.0));
	}

	@Test
	void exactForwardSeamsDoNotDuplicateAcrossTicks()
	{
		PAnimation animation = clip(2.0f);
		List<String> result = new ArrayList<>(collect(animation, 0.0, 2.0));
		result.addAll(collect(animation, 2.0, 4.0));
		assertEquals(List.of("zero", "early", "late", "end", "zero", "early", "late", "end"), result);
		assertEquals(collect(animation, 0.0, 4.0), result);
	}

	@Test
	void reverseSeamsKeepDescendingOrderWithoutEpsilonDuplicates()
	{
		PAnimation animation = clip(2.0f);
		List<String> result = new ArrayList<>(collect(animation, 2.0, 0.0));
		result.addAll(collect(animation, 0.0, -2.0));
		assertEquals(List.of("end", "late", "early", "zero", "end", "late", "early", "zero"), result);
		assertEquals(collect(animation, 2.0, -2.0), result);
	}

	@Test
	void severalCyclesAndEqualTimestampEventsFireOnceEach()
	{
		PAnimation animation = new PAnimation("ties", 1.0f, Map.of(),
				List.of(event(0.5f, "a"), event(0.5f, "b")));
		assertEquals(List.of("a", "b", "a", "b", "a", "b"), collect(animation, 0.0, 3.0));
		assertEquals(List.of("a", "b", "a", "b", "a", "b"), collect(animation, 3.0, 0.0));
	}

	@Test
	void invalidRangesFailBeforeAnyPartialDispatch()
	{
		PAnimation animation = clip(1.0f);
		assertThrows(IllegalArgumentException.class, () -> collect(animation, Double.NaN, 1.0));
		assertThrows(IllegalArgumentException.class, () -> collect(animation, 0.0, Double.POSITIVE_INFINITY));
		assertThrows(IllegalArgumentException.class, () -> animation.forEachCyclicEvent(0.0, 1.0e12,
				event -> this.dispatched.add(event.data().toString())));
		assertTrue(this.dispatched.isEmpty());
		assertThrows(IllegalArgumentException.class, () -> collect(animation, 0x1.0p53, 0x1.0p53 + 2.0));
	}

	@Test
	void randomizedWindowsMatchAnIndependentAbsoluteEventTimeline()
	{
		Random random = new Random(0xC1);
		for (int iteration = 0; iteration < 2000; iteration++)
		{
			float length = new float[]{0.0001f, 0.125f, 1.0f, 13.334f}[iteration % 4];
			PAnimation animation = clip(length);
			double from = (random.nextInt(2000) - 1000 + random.nextDouble()) * length;
			double to = from + (random.nextDouble() - 0.5) * 20.0 * length;
			assertEquals(reference(animation, from, to), collect(animation, from, to), "window " + iteration);
		}
	}

	@Test
	void graphClockAdvancesBeyondFloatPrecisionAndDispatchesThroughController() throws ReflectiveOperationException
	{
		PAnimation animation = clip(2.0f);
		PBakedModel model = new PBakedModel(List.of(), Map.of("clip", animation));
		PAnimationController<TestAnimatable> controller = new PAnimationController<>(new PAnimationGraph(
				List.of(new PAnimationState.Clip("idle", "clip")), List.of()));
		setGraphTime(controller.graphRuntime(), 0x1.0p24);
		controller.tick(new TestAnimatable(), 1.0f, model);
		assertEquals(List.of("zero", "early"), this.dispatched);
		PAnimationGraphRuntime.PreciseEventTrack track = controller.graphRuntime().tickPrecise(model, 1.0f).getFirst();
		assertEquals(0x1.0p24 + 1.0, track.from());
		assertEquals(0x1.0p24 + 2.0, track.to());
	}

	@Test
	void negativeSpeedDispatchesReverseAndRawPlaybackCrossesMultipleCycles()
	{
		PAnimation animation = clip(2.0f);
		PBakedModel model = new PBakedModel(List.of(), Map.of("clip", animation));
		PAnimationController<TestAnimatable> raw = new PAnimationController<>(state -> ControllerState.PLAY);
		raw.play(PRawAnimation.begin().thenLoop("clip").withSpeed(-1.0f).build());
		raw.tick(new TestAnimatable(), 4.0f, model);
		assertEquals(List.of("late", "early", "zero", "end", "late", "early", "zero"), this.dispatched);
		this.dispatched.clear();
		raw.play(PRawAnimation.begin().thenLoop("clip").build());
		raw.tick(new TestAnimatable(), 6.0f, model);
		assertEquals(collect(animation, 0.0, 6.0), this.dispatched);
	}

	@Test
	void rawReverseInitialMarkerIsExcludedButExactWrapIncludesIt()
	{
		PAnimation animation = clip(2.0f);
		PBakedModel model = new PBakedModel(List.of(), Map.of("clip", animation));
		PAnimationController<TestAnimatable> controller = new PAnimationController<>(state -> ControllerState.PLAY);
		controller.play(PRawAnimation.begin().thenLoop("clip").withSpeed(-1.0f).build());
		controller.tick(new TestAnimatable(), 2.0f, model);
		assertEquals(List.of("late", "early", "zero"), this.dispatched);
		this.dispatched.clear();
		controller.tick(new TestAnimatable(), 2.0f, model);
		assertEquals(List.of("end", "late", "early", "zero"), this.dispatched);
	}

	@Test
	void blendSamplesKeepTheirOwnDurationsAtLargeGraphTimes() throws ReflectiveOperationException
	{
		PAnimationState.BlendSpace1D state = new PAnimationState.BlendSpace1D("blend", "blend", List.of(
				new PAnimationState.BlendSpace1D.Point(0.0f, "short"), new PAnimationState.BlendSpace1D.Point(1.0f, "long")),
				PAnimationType.CYCLE, PInterpolationType.LINEAR, 1.0f, false);
		PAnimationGraphRuntime graph = new PAnimationGraphRuntime(new PAnimationGraph(List.of(state), List.of()));
		graph.parameters().set("blend", 0.5f);
		PBakedModel model = new PBakedModel(List.of(), Map.of("short", clip(2.0f), "long", clip(3.0f)));
		setGraphTime(graph, 0x1.0p24);
		List<PAnimationGraphRuntime.PreciseEventTrack> tracks = graph.tickPrecise(model, 1.0f);
		assertEquals(2, tracks.size());
		assertEquals(0x1.0p24 + 1.0, tracks.getFirst().to());
		assertEquals(0x1.0p24 + 1.0, tracks.getLast().to());
		List<PAnimationGraphRuntime.Layer> layers = graph.layers(model);
		assertEquals((float)((0x1.0p24 + 1.0) % 2.0), layers.getFirst().time());
		assertEquals((float)((0x1.0p24 + 1.0) % 3.0), layers.getLast().time());
	}

	@Test
	void floatEventTrackApiKeepsItsRecordShapeAndAdvancesOnlyOnce()
	{
		assertEquals(4, PAnimationGraphRuntime.EventTrack.class.getRecordComponents().length);
		PAnimationGraphRuntime graph = new PAnimationGraphRuntime(new PAnimationGraph(
				List.of(new PAnimationState.Clip("idle", "clip")), List.of()));
		assertEquals(new PAnimationGraphRuntime.EventTrack("clip", 0.0f, 1.0f, PAnimationType.CYCLE),
				graph.tick(null, 1.0f).getFirst());
		assertEquals(1.0f, graph.time());
		assertEquals(1.0, graph.tickPrecise(null, 1.0f).getFirst().from());
	}

	@Test
	void graphRejectsAClockThatCannotAdvance() throws ReflectiveOperationException
	{
		PAnimationGraphRuntime graph = new PAnimationGraphRuntime(new PAnimationGraph(
				List.of(new PAnimationState.Clip("idle", "clip")), List.of()));
		setGraphTime(graph, 0x1.0p54);
		assertThrows(IllegalArgumentException.class, () -> graph.tick(null, 1.0f));
	}

	private static void setGraphTime(PAnimationGraphRuntime graph, double time) throws ReflectiveOperationException
	{
		Field current = PAnimationGraphRuntime.class.getDeclaredField("current");
		current.setAccessible(true);
		Object playback = current.get(graph);
		Field clock = playback.getClass().getDeclaredField("time");
		clock.setAccessible(true);
		clock.setDouble(playback, time);
	}

	private PAnimation clip(float length)
	{
		return new PAnimation("clip", length, Map.of(), List.of(event(0.0f, "zero"),
				event(length / 3.0f, "early"), event(length * 2.0f / 3.0f, "late"), event(length, "end")));
	}

	private PAnimationEvent<String> event(float time, String label)
	{
		return new PAnimationEvent<>(time, this.type, label);
	}

	private static List<String> collect(PAnimation animation, double from, double to)
	{
		List<String> result = new ArrayList<>();
		animation.forEachCyclicEvent(from, to, event -> result.add(event.data().toString()));
		return result;
	}

	private static List<String> reference(PAnimation animation, double from, double to)
	{
		boolean forward = to > from;
		List<Occurrence> result = new ArrayList<>();
		double length = animation.length();
		for (long cycle = (long)Math.floor(Math.min(from, to) / length) - 1L;
		     cycle <= (long)Math.ceil(Math.max(from, to) / length) + 1L; cycle++)
		{
			for (PAnimationEvent<?> event : animation.events())
			{
				double absolute = cycle * length + event.time();
				boolean included = forward ? event.time() == 0.0f ? absolute >= from && absolute < to : absolute > from && absolute <= to :
						event.time() == length ? absolute <= from && absolute > to : absolute < from && absolute >= to;
				if (included)
				{
					result.add(new Occurrence(absolute, event.time(), event.data().toString()));
				}
			}
		}
		Comparator<Occurrence> order = Comparator.comparingDouble(Occurrence::absolute).
				thenComparing(Comparator.comparingDouble(Occurrence::local).reversed());
		result.sort(forward ? order : order.reversed());
		return result.stream().map(Occurrence::label).toList();
	}

	private static final class TestAnimatable implements PAnimatable<TestAnimatable>
	{
		@Override
		public PAnimationManager<TestAnimatable> getAnimationManager(AnimManagerKey key)
		{
			throw new UnsupportedOperationException("Numeric event fixture does not use managers");
		}

		@Override
		public void registerAnimationControllers(PAnimationManager.PAnimationRegistrar<TestAnimatable> registrar)
		{
		}
	}

	private record Occurrence(double absolute, double local, String label)
	{
	}
}
