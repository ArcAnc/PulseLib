/**
 * @author ArcAnc
 * Created at: 18.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;

import com.arcanc.pulselib.content.animatable.AnimManagerKey;
import com.arcanc.pulselib.content.animatable.ControllerState;
import com.arcanc.pulselib.content.animatable.PAnimationController;
import com.arcanc.pulselib.content.model.baked.PBakedBone;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.registration.PLibRegistration;
import com.arcanc.pulselib.content.renderer.PBlockRenderer;
import com.arcanc.pulselib.content.renderer.PEntityRenderer;
import com.arcanc.pulselib.content.renderer.PItemRenderer;
import com.arcanc.pulselib.data.gecko.MolangParser;
import com.arcanc.pulselib.data.gecko.PMolangVectorValue;

import net.minecraft.SharedConstants;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.Entity;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PAnimationTimeTest
{
	@Test
	@SuppressWarnings({"rawtypes", "unchecked"})
	void renderersAndDefaultProviderExposeInterpolatedSeconds() throws ReflectiveOperationException
	{
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		var constructor = EntityRendererProvider.Context.class.getDeclaredConstructors()[0];
		var context = (EntityRendererProvider.Context)constructor.newInstance(new Object[constructor.getParameterCount()]);
		PEntityRenderer entity = new PEntityRenderer(context, null, null)
		{
			@Override
			public ResourceLocation getTextureLocation(Entity owner)
			{
				return ResourceLocation.withDefaultNamespace("unused");
			}
		};
		PBlockRenderer block = new PBlockRenderer(null, null)
		{
		};
		PItemRenderer item = new PItemRenderer(null, null, null, null)
		{
		};
		var expression = MolangParser.parse("query.anim_time");
		for (float ticks : new float[]{0, 10, 20, 30})
		{
			PAnimationController<PTransitionTestSupport.Dummy> controller = new PAnimationController<>(state -> ControllerState.STOP)
			{
				@Override
				public float getInterpolatedTime(float partialTick)
				{
					return ticks + partialTick * 2;
				}
			};
			for (float partialTick : new float[]{0, 0.25f, 0.5f, 0.75f, 1})
			{
				float expected = (ticks + partialTick * 2) / 20;
				assertEquals(expected, expression.evaluate(PAnimationPoseResolver.<PTransitionTestSupport.Dummy>defaultContexts().context(controller, partialTick)), 0.000001f);
				for (Object renderer : List.of(entity, block, item))
				{
					Method prepare = Arrays.stream(renderer.getClass().getSuperclass().getDeclaredMethods()).
							filter(method -> method.getName().equals("prepareMolangContexts")).findFirst().orElseThrow();
					prepare.setAccessible(true);
					Map<PAnimationController<?>, MolangParser.Context> values = (Map<PAnimationController<?>, MolangParser.Context>)
							prepare.invoke(renderer, null, new AnimManagerKey(1), List.of(controller), partialTick);
					assertEquals(expected, expression.evaluate(values.get(controller)), 0.000001f, renderer.getClass().getSuperclass().getSimpleName());
				}
			}
		}
	}

	@Test
	void runtimeDoesNotConvertSuppliedSecondsAgain()
	{
		var value = new PMolangVectorValue(MolangParser.parse("q.anim_time"),
				MolangParser.parse("0"), MolangParser.parse("0"), PVectorConversion.IDENTITY);
		var animation = new PAnimation("time", 100, Map.of("root", new PBoneAnimation(UUID.randomUUID(),
				Map.of("position", new PAnimationTrack<>(PLibRegistration.AnimationChannelReg.POSITION,
						List.of(new PKeyframe<>(0, value, value, PInterpolationType.LINEAR)))))));
		var model = new PBakedModel(List.of(
				new PBakedBone("root", new Vector3f(), new Quaternionf(), List.of(), null, List.of())),
				Map.of("time", animation));
		PAnimationController<PTransitionTestSupport.Dummy> graph = new PAnimationController<>(new PAnimationGraph(
				List.of(new PAnimationState.Clip("time", "time")), List.of()));
		graph.graphRuntime().tickPrecise(model, 10);
		assertEquals(0.25f, PAnimationRuntime.evaluate(model, List.of(graph), PAnimationPoseResolver.defaultContexts(), 0.5f).translation(0).x);
		assertEquals(7.25f, PAnimationRuntime.evaluate(model, List.of(graph),
				(controller, partialTick) -> new MolangParser.Context().query("anim_time", 7.25f), 0.5f).translation(0).x);
		assertEquals(0.5f, animation.calculateBoneTransformations("root", 10, PInterpolationType.LINEAR).translation().x);
	}

	@Test
	void rawControllerSpeedKeepsMolangAndPlaybackTimeConsistent()
	{
		var model = PTransitionTestSupport.model();
		for (float speed : new float[]{0, 0.5f, 1, 2})
		{
			PAnimationController<PTransitionTestSupport.Dummy> controller = new PAnimationController<>(state -> ControllerState.PLAY);
			controller.play(PRawAnimation.begin().thenLoop("idle").withSpeed(speed).build());
			controller.tick(new PTransitionTestSupport.Dummy(), 10, model);
			assertEquals(10 * speed, controller.getTime());
			for (float partialTick : new float[]{0, 0.25f, 0.5f, 1})
			{
				var context = PAnimationPoseResolver.<PTransitionTestSupport.Dummy>defaultContexts().context(controller, partialTick);
				assertEquals(controller.getInterpolatedTime(partialTick) / 20, MolangParser.evaluate("q.anim_time", context), 0.000001f);
			}
		}
	}

	@Test
	void graphSpeedAndClipTimestampsRemainInTicks()
	{
		var model = PTransitionTestSupport.model();
		for (float speed : new float[]{0, 0.5f, 1, 2})
		{
			PAnimationController<PTransitionTestSupport.Dummy> controller = new PAnimationController<>(new PAnimationGraph(
					List.of(new PAnimationState.Clip("idle", "idle", PAnimationType.CYCLE, PInterpolationType.LINEAR, speed, false)), List.of()));
			controller.graphRuntime().tickPrecise(model, 10);
			for (float partialTick : new float[]{0, 0.25f, 0.5f, 1})
			{
				var context = PAnimationPoseResolver.<PTransitionTestSupport.Dummy>defaultContexts().context(controller, partialTick);
				assertEquals(controller.getInterpolatedTime(partialTick) / 20, MolangParser.evaluate("q.anim_time", context), 0.000001f);
			}
			assertEquals(10 * speed, controller.graphRuntime().layers(model).getFirst().time(), 0.000001f);
		}
	}
}
