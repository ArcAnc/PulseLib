/**
 * @author ArcAnc
 * Created at: 22.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer;

import com.arcanc.pulselib.content.animatable.AnimManagerKey;
import com.arcanc.pulselib.content.animatable.ControllerState;
import com.arcanc.pulselib.content.animatable.PAnimatable;
import com.arcanc.pulselib.content.animatable.PAnimationController;
import com.arcanc.pulselib.content.animatable.PAnimationManager;
import com.arcanc.pulselib.content.model.animation.PAnimation;
import com.arcanc.pulselib.content.model.animation.PAnimationTrack;
import com.arcanc.pulselib.content.model.animation.PBoneAnimation;
import com.arcanc.pulselib.content.model.animation.PInterpolationType;
import com.arcanc.pulselib.content.model.animation.PKeyframe;
import com.arcanc.pulselib.content.model.animation.PRawAnimation;
import com.arcanc.pulselib.content.model.animation.PVectorConversion;
import com.arcanc.pulselib.content.model.baked.PBakedBone;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.registration.PLibRegistration;
import com.arcanc.pulselib.data.gecko.MolangParser;
import com.arcanc.pulselib.data.gecko.PMolangVectorValue;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class PRenderPoseContextsTest
{
	@Test
	void boneTraversalEvaluatesStatefulMolangOncePerModelAndRenderContext()
	{
		PBakedModel model = model();
		PAnimationController<Owner> controller = controller(model);
		var controllers = List.of(controller);
		var contexts = new PRenderPoseContexts<Owner>();
		var first = PRenderPoseContexts.resolve(model, controllers, contexts, 0.5f);
		assertEquals(1, first.resolve("first").localTransform().translation().x);
		assertEquals(2, first.resolve("second").localTransform().translation().x);
		assertSame(first, PRenderPoseContexts.resolve(model, controllers, contexts, 0.5f));
		assertSame(first, PRenderPoseContexts.resolve(model, controllers, contexts, 0.5f));
		assertEquals(2, MolangParser.evaluate("v.count", controller.persistentMolangContext()));

		var nextFrame = PRenderPoseContexts.resolve(model, controllers, new PRenderPoseContexts<>(), 0.5f);
		assertNotSame(first, nextFrame);
		assertEquals(3, nextFrame.resolve("first").localTransform().translation().x);
		assertEquals(4, MolangParser.evaluate("v.count", controller.persistentMolangContext()));
	}

	@Test
	void modelsControllersAndPartialTicksDoNotShareAnIncorrectCachedPose()
	{
		PBakedModel model = model();
		var controller = controller(model);
		var controllers = List.of(controller);
		var contexts = new PRenderPoseContexts<Owner>();
		var first = PRenderPoseContexts.resolve(model, controllers, contexts, 0.0f);
		assertNotSame(first, PRenderPoseContexts.resolve(model, controllers, contexts, 1.0f));
		assertNotSame(first, PRenderPoseContexts.resolve(model(), controllers, contexts, 0.0f));
		assertNotSame(first, PRenderPoseContexts.resolve(model, List.of(controller), contexts, 0.0f));
		assertEquals(8, MolangParser.evaluate("v.count", controller.persistentMolangContext()));
	}

	private static PAnimationController<Owner> controller(PBakedModel model)
	{
		PAnimationController<Owner> controller = new PAnimationController<>(state -> ControllerState.PLAY);
		controller.play(PRawAnimation.begin().thenLoop("stateful").build());
		controller.tick(new Owner(), 1, model);
		return controller;
	}

	private static PBakedModel model()
	{
		var value = new PMolangVectorValue(MolangParser.parse("v.count = (v.count ?? 0) + 1; return v.count;"),
				MolangParser.parse("0"), MolangParser.parse("0"), PVectorConversion.IDENTITY);
		var track = new PAnimationTrack<>(PLibRegistration.AnimationChannelReg.POSITION,
				List.of(new PKeyframe<>(0, value, value, PInterpolationType.LINEAR)));
		var clip = new PAnimation("stateful", 20, Map.of(
				"first", new PBoneAnimation(UUID.randomUUID(), Map.of("position", track)),
				"second", new PBoneAnimation(UUID.randomUUID(), Map.of("position", track))));
		return new PBakedModel(List.of(
				new PBakedBone("first", new Vector3f(), new Quaternionf(), List.of(), null, List.of()),
				new PBakedBone("second", new Vector3f(), new Quaternionf(), List.of(), null, List.of())), Map.of("stateful", clip));
	}

	private static final class Owner implements PAnimatable<Owner>
	{
		@Override
		public PAnimationManager<Owner> getAnimationManager(AnimManagerKey key)
		{
			throw new UnsupportedOperationException("Fixture uses an explicit controller");
		}

		@Override
		public void registerAnimationControllers(PAnimationManager.PAnimationRegistrar<Owner> registrar)
		{
		}
	}
}
