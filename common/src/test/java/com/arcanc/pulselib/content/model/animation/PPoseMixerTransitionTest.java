/**
 * @author ArcAnc
 * Created at: 14.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.BitSet;
import java.util.List;

import static com.arcanc.pulselib.content.model.animation.PTransitionTestSupport.assertPose;
import static com.arcanc.pulselib.content.model.animation.PTransitionTestSupport.expectedBlend;
import static com.arcanc.pulselib.content.model.animation.PTransitionTestSupport.field;
import static com.arcanc.pulselib.content.model.animation.PTransitionTestSupport.pose;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PPoseMixerTransitionTest
{
	@Test
	void restartWithoutAnActiveTransitionKeepsTheInitialPose()
	{
		PPoseMixer mixer = new PPoseMixer(1);
		PPoseMixer.Transition transition = new PPoseMixer.Transition(layer(10.0f));
		transition.transitionTo(layer(20.0f), 10.0f, PPoseEasing.LINEAR, PTransitionInterruptionPolicy.RESTART);
		assertEquals(10.0f, mixer.mix(new PPose(1), transition.layers()).translation(0).x, 0.00001f);
	}

	@Test
	void restartAtFortyPercentKeepsTheComputedPose()
	{
		PPoseMixer mixer = new PPoseMixer(1);
		PPoseMixer.Transition transition = new PPoseMixer.Transition(layer(10.0f));
		transition.transitionTo(layer(20.0f), 10.0f, PPoseEasing.LINEAR, PTransitionInterruptionPolicy.FROM_CURRENT);
		transition.advance(4.0f);
		float before = mixer.mix(new PPose(1), transition.layers()).translation(0).x;
		assertEquals(14.0f, before, 0.00001f);
		transition.transitionTo(layer(30.0f), 10.0f, PPoseEasing.LINEAR, PTransitionInterruptionPolicy.RESTART);
		assertEquals(before, mixer.mix(new PPose(1), transition.layers()).translation(0).x, 0.00001f);
	}

	@Test
	void restartFreezesTrsAndScalarChannelsAndUsesQuaternionInterpolation()
	{
		PPose reference = new PPose(2, 1);
		PPoseMixer mixer = new PPoseMixer(2);
		PPose initial = pose(10);
		PPose incoming = pose(20);
		PPose target = pose(30);
		PPoseMixer.Transition transition = new PPoseMixer.Transition(mixer, reference, PTransitionTestSupport.layer(initial));
		transition.transitionTo(PTransitionTestSupport.layer(incoming), 10, PPoseEasing.LINEAR, PTransitionInterruptionPolicy.FROM_CURRENT);
		transition.advance(4);
		PPose before = mixer.mix(reference, transition.layers());
		transition.transitionTo(PTransitionTestSupport.layer(target), 10, PPoseEasing.LINEAR, PTransitionInterruptionPolicy.RESTART);
		assertPose(before, mixer.mix(reference, transition.layers()));
		initial.setAnimated(0, new Vector3f(900), new Quaternionf(), new Vector3f(8));
		incoming.translation(0).set(700);
		incoming.scalarChannel(0, 800);
		assertPose(before, mixer.mix(reference, transition.layers()));
		transition.layers().getFirst().pose().translation(0).set(1234);
		assertPose(before, mixer.mix(reference, transition.layers()));
		transition.advance(5);
		assertPose(expectedBlend(before, target, 0.5f), mixer.mix(reference, transition.layers()));
		transition.advance(5);
		assertPose(mixer.mix(reference, List.of(PTransitionTestSupport.layer(target))), mixer.mix(reference, transition.layers()));
		assertTrue(((List<?>)field(transition, "sourceLayers")).isEmpty());
	}

	@Test
	void sparseChannelsAndExplicitIdentityRemainDistinct()
	{
		PPose reference = new PPose(2);
		PPose initial = new PPose(2);
		initial.translation(0, new Vector3f(10, 0, 0));
		PPose target = new PPose(2);
		target.rotation(0, new Quaternionf());
		PPoseMixer mixer = new PPoseMixer(2);
		PPoseMixer.Transition transition = new PPoseMixer.Transition(mixer, reference, PTransitionTestSupport.layer(initial));
		transition.transitionTo(PTransitionTestSupport.layer(target), 10, PPoseEasing.LINEAR, PTransitionInterruptionPolicy.RESTART);
		PPose start = transition.layers().getFirst().pose();
		assertTrue(start.hasTranslation(0));
		assertFalse(start.hasRotation(0));
		assertFalse(start.hasScale(0));
		assertFalse(start.isValid(1));
		transition.advance(5);
		PPose middle = mixer.mix(reference, transition.layers());
		assertEquals(5, middle.translation(0).x, 0.00001f);
		assertTrue(middle.hasRotation(0));
		assertFalse(middle.hasScale(0));
		transition.advance(5);
		PPose end = mixer.mix(reference, transition.layers());
		assertFalse(end.hasTranslation(0));
		assertTrue(end.hasRotation(0));
	}

	@Test
	void repeatedRestartsBoundSourcesAndDoNotAccumulateTransforms()
	{
		PPose reference = new PPose(2, 1);
		PPoseMixer mixer = new PPoseMixer(2);
		PPoseMixer.Transition transition = new PPoseMixer.Transition(mixer, reference, PTransitionTestSupport.layer(pose(10)));
		for (int interruption = 0; interruption < 3000; interruption++)
		{
			PPose before = mixer.mix(reference, transition.layers());
			transition.transitionTo(PTransitionTestSupport.layer(pose(10 + interruption % 4)), 10, PPoseEasing.LINEAR, PTransitionInterruptionPolicy.RESTART);
			assertPose(before, mixer.mix(reference, transition.layers()));
			assertEquals(1, ((List<?>)field(transition, "sourceLayers")).size());
			assertEquals(1, transition.layers().size());
			transition.advance(1);
		}
		transition.advance(10);
		assertTrue(((List<?>)field(transition, "sourceLayers")).isEmpty());
	}

	@Test
	void fromCurrentSourcesStayLiveAndCompleteCurrentRejectsInterruptions()
	{
		PPoseMixer mixer = new PPoseMixer(1);
		PPoseMixer.Layer initial = layer(10);
		PPoseMixer.Layer incoming = layer(20);
		PPoseMixer.Transition transition = new PPoseMixer.Transition(initial);
		transition.transitionTo(incoming, 10, PPoseEasing.LINEAR, PTransitionInterruptionPolicy.FROM_CURRENT);
		transition.advance(4);
		assertFalse(transition.transitionTo(layer(100), 10, PPoseEasing.LINEAR, PTransitionInterruptionPolicy.COMPLETE_CURRENT));
		assertEquals(14, mixer.mix(new PPose(1), transition.layers()).translation(0).x, 0.0001f);
		transition.transitionTo(layer(30), 10, PPoseEasing.LINEAR, PTransitionInterruptionPolicy.FROM_CURRENT);
		initial.pose().translation(0).x = 20;
		incoming.pose().translation(0).x = 40;
		assertEquals(28, mixer.mix(new PPose(1), transition.layers()).translation(0).x, 0.0001f);
	}

	@Test
	void fromCurrentAfterRestartKeepsThePreviousTargetLive()
	{
		PPoseMixer mixer = new PPoseMixer(1);
		PPoseMixer.Layer incoming = layer(20);
		PPoseMixer.Transition transition = new PPoseMixer.Transition(layer(10));
		transition.transitionTo(incoming, 10, PPoseEasing.LINEAR, PTransitionInterruptionPolicy.RESTART);
		transition.advance(4);
		transition.transitionTo(layer(30), 10, PPoseEasing.LINEAR, PTransitionInterruptionPolicy.FROM_CURRENT);
		incoming.pose().translation(0).x = 40;
		assertEquals(22, mixer.mix(new PPose(1), transition.layers()).translation(0).x, 0.0001f);
	}

	@Test
	void zeroDurationAndTransitionEndpointsReleaseSnapshots()
	{
		for (float elapsed : new float[]{0, 0.0001f, 9.9999f, 10})
		{
			PPoseMixer mixer = new PPoseMixer(1);
			PPoseMixer.Transition transition = new PPoseMixer.Transition(layer(10));
			transition.transitionTo(layer(20), 10, PPoseEasing.LINEAR, PTransitionInterruptionPolicy.RESTART);
			transition.advance(elapsed);
			float before = mixer.mix(new PPose(1), transition.layers()).translation(0).x;
			transition.transitionTo(layer(30), 10, PPoseEasing.LINEAR, PTransitionInterruptionPolicy.RESTART);
			assertEquals(before, mixer.mix(new PPose(1), transition.layers()).translation(0).x, 0.0001f);
			transition.transitionTo(layer(40), 0, PPoseEasing.LINEAR, PTransitionInterruptionPolicy.RESTART);
			assertEquals(40, mixer.mix(new PPose(1), transition.layers()).translation(0).x, 0.0001f);
			assertFalse(transition.isTransitioning());
			assertTrue(((List<?>)field(transition, "sourceLayers")).isEmpty());
		}
	}

	@Test
	void restartCapturesMaskedAndAdditiveLayersAgainstTheActualReference()
	{
		PPose reference = new PPose(2, 1);
		reference.set(0, new Vector3f(3, 2, 1), new Quaternionf().rotationY(0.3f), new Vector3f(2, 3, 4));
		PPoseMixer mixer = new PPoseMixer(2, new int[]{-1, 0});
		for (PPoseBlendMode mode : PPoseBlendMode.values())
		{
			BitSet mask = new BitSet();
			mask.set(0);
			PPoseMixer.Layer initial = new PPoseMixer.Layer(pose(10), reference, mask, bone -> 0.75f, mode, 2, 0.8f);
			PPoseMixer.Transition transition = new PPoseMixer.Transition(mixer, reference, initial);
			PPose before = mixer.mix(reference, transition.layers());
			transition.transitionTo(PTransitionTestSupport.layer(pose(30)), 10, PPoseEasing.LINEAR, PTransitionInterruptionPolicy.RESTART);
			assertPose(before, mixer.mix(reference, transition.layers()));
		}
	}

	@Test
	void scalarChannelsAreRetainedEvenWhenTheReferenceHasNoScalarStorage()
	{
		PPose source = pose(10);
		PPoseMixer mixer = new PPoseMixer(2);
		PPoseMixer.Transition transition = new PPoseMixer.Transition(mixer, new PPose(2), PTransitionTestSupport.layer(source));
		transition.transitionTo(PTransitionTestSupport.layer(pose(20)), 10, PPoseEasing.LINEAR, PTransitionInterruptionPolicy.RESTART);
		assertEquals(10, mixer.mix(new PPose(2), transition.layers()).scalarChannel(0));
		transition.advance(5);
		assertEquals(15, mixer.mix(new PPose(2), transition.layers()).scalarChannel(0));
	}

	@Test
	void copyRetainsMasksDirtyFlagsVisibilityAndScalarsWithoutSharingMutableValues()
	{
		PPose source = new PPose(2, 1);
		source.translation(0, new Vector3f(2, 3, 4));
		source.rotation(1, new Quaternionf());
		source.visibility(0, false);
		source.scalarChannel(0, 17);
		PPose copy = source.copy();
		source.translation(0).set(100);
		source.visibility(0, true);
		source.scalarChannel(0, 200);
		assertTrue(copy.translation(0).equals(new Vector3f(2, 3, 4), 0.0001f));
		assertTrue(copy.hasTranslation(0));
		assertFalse(copy.hasRotation(0));
		assertFalse(copy.hasScale(0));
		assertTrue(copy.hasRotation(1));
		assertTrue(copy.isDirty(1));
		assertFalse(copy.isVisible(0));
		assertEquals(17, copy.scalarChannel(0));
	}

	@Test
	void legacyMutableVectorAccessStillContributesToMixingAndSnapshotCapture()
	{
		PPose source = new PPose(1);
		source.translation(0).set(2, 3, 4);
		source.rotation(0).rotationXYZ(0.2f, 0.3f, 0.4f);
		source.scale(0).set(2, 3, 4);
		PPoseMixer mixer = new PPoseMixer(1);
		PPoseMixer.Transition transition = new PPoseMixer.Transition(PTransitionTestSupport.layer(source));
		PPose before = mixer.mix(new PPose(1), transition.layers());
		assertPose(source, before);
		transition.transitionTo(layer(10), 10, PPoseEasing.LINEAR, PTransitionInterruptionPolicy.RESTART);
		source.translation(0).set(100);
		assertPose(before, mixer.mix(new PPose(1), transition.layers()));
	}

	@Test
	void restartTargetStaysLiveWhileTheSnapshotRemainsFixed()
	{
		PPose source = pose(10);
		PPose target = pose(20);
		PPoseMixer mixer = new PPoseMixer(2);
		PPose reference = new PPose(2, 1);
		PPoseMixer.Transition transition = new PPoseMixer.Transition(mixer, reference, PTransitionTestSupport.layer(source));
		PPose frozen = source.copy();
		transition.transitionTo(PTransitionTestSupport.layer(target), 10, PPoseEasing.LINEAR, PTransitionInterruptionPolicy.RESTART);
		source.translation(0).set(100);
		PPose changedTarget = pose(40);
		target.setAnimated(0, changedTarget.translation(0), changedTarget.rotation(0), changedTarget.scale(0));
		target.scalarChannel(0, 40);
		assertPose(frozen, mixer.mix(reference, transition.layers()));
		transition.advance(5);
		assertPose(expectedBlend(frozen, changedTarget, 0.5f), mixer.mix(reference, transition.layers()));
	}

	private static PPoseMixer.Layer layer(float translation)
	{
		PPose pose = new PPose(1);
		pose.setAnimated(0, new Vector3f(translation, 0, 0), new Quaternionf(), new Vector3f(1));
		return new PPoseMixer.Layer(pose, PPoseBlendMode.OVERRIDE, 0, 1.0f);
	}
}
