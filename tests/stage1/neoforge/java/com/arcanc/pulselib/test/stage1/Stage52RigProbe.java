/**
 * @author ArcAnc
 * Created at: 27.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1;

import com.arcanc.pulselib.content.mixin.PoseStackAccessor;
import com.arcanc.pulselib.content.model.animation.PRawAnimation;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import com.arcanc.pulselib.content.renderer.PRenderContexts;
import com.arcanc.pulselib.content.renderer.base.PPlayerRigRenderState;
import com.arcanc.pulselib.util.PLibDatabase;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

/** Actual transformed native submit/deferred draw boundaries, using independent model fingerprints. */
public final class Stage52RigProbe
{
	private static @Nullable BiConsumer<PlayerModel, AvatarRenderState> setupObserver;
	private static boolean verifying;
	private static int suites;
	private static int vertices;
	private static int worldDeferred;
	private static int failures;

	private Stage52RigProbe()
	{
	}

	public static void afterSetup(PlayerModel model, AvatarRenderState state)
	{
		if (setupObserver != null)
			setupObserver.accept(model, state);
	}

	public static void deferred(Object state)
	{
		if (state instanceof AvatarRenderState && !verifying)
			worldDeferred++;
	}

	public static void completed()
	{
		check(suites == 4 && vertices > 0 && worldDeferred > 0 && failures == 16,
				"Both formats/phases, 16 nested failures, native vertices and world deferred submissions");
		PLibDatabase.LOGGER.info("STAGE 5.2b.2a RIG PASS: suites={}, vertices={}, worldDeferred={}, expectedFailures={}, extraction/owner isolation, native submit/deferred nested failure/recovery/reload PASS", suites, vertices, worldDeferred, failures);
	}

	public static void verify(Identifier definition, String clip)
	{
		var mc = Minecraft.getInstance();
		var renderer = mc.getEntityRenderDispatcher().getPlayerRenderer(mc.player);
		PlayerModel model = renderer.getModel();
		List<SavedPart> caller = save(model);
		var state = renderer.createRenderState(mc.player, 0.65f);
		var rig = ((PPlayerRigRenderState)state).pulselib$getPlayerRig();
		check(rig != null, "Rig captured on native extraction");
		((PPlayerRigRenderState)state).pulselib$setPlayerRig(null);
		model.setupAnim(state);
		List<SavedPart> vanilla = save(model);
		PPlayerAnimations.apply(mc.player, model, 0.65f, PPlayerAnimations.allParts());
		List<SavedPart> expected = save(model);
		check(!expected.equals(vanilla), "Canonical oracle actually contributes");
		((PPlayerRigRenderState)state).pulselib$setPlayerRig(rig);
		state.id = Integer.MAX_VALUE;
		var other = new RemotePlayer(mc.level, new GameProfile(new UUID(0, 0x525b20 + suites), "Stage52RigOther"));
		other.setPos(mc.player.position());
		var inner = renderer.createRenderState(other, 0.65f);
		inner.id = Integer.MAX_VALUE;
		model.setupAnim(inner);
		List<SavedPart> expectedInner = save(model);
		PPlayerAnimations.getHandle(mc.player, definition).stopAll();
		restore(caller);
		verifying = true;
		try (var scope = PRenderContexts.enter(PRenderContexts.Context.GUI))
		{
			try
			{
				model.setupAnim(state);
				matches(expected, model, "Frozen rig after owner controller stop and missing state.id");
				restore(caller);
				var poses = new PoseStack();
				var camera = new CameraRenderState();
				SubmitNodeCollector discard = collector();
				renderer.submit(state, poses, discard, camera);
				matches(caller, model, "Normal native submit restores caller");
				setupObserver = (current, submitted) ->
				{
					if (submitted == inner)
					{
						matches(expectedInner, current, "Inner owner uses its own rig");
						throw new IllegalStateException("rig inner submit");
					}
					if (submitted != state)
						return;
					matches(expected, current, "Outer submit uses extraction snapshot");
					expect("rig inner submit", () -> renderer.submit(inner, poses, discard, camera));
					matches(expected, current, "Nested submit restores outer model");
					current.jacket.visible = false;
					current.rightSleeve.xScale = 7;
					throw new IllegalStateException("rig outer submit");
				};
				expect("rig outer submit", () -> renderer.submit(state, poses, discard, camera));
				setupObserver = null;
				matches(caller, model, "Failed native submit restores transforms/scale/visibility");
				renderer.submit(state, poses, discard, camera);
				matches(caller, model, "Native submit recovery");
				verifyDeferred(model, state, inner, expected, expectedInner, caller);
				suites++;
			}
			finally
			{
				scope.discard();
			}
		}
		finally
		{
			setupObserver = null;
			verifying = false;
			restore(caller);
			check(PPlayerAnimations.getHandle(mc.player, definition).play("motion", PRawAnimation.begin().thenLoop(clip).build()), "Restore fixture controller");
		}
	}

	private static void verifyDeferred(PlayerModel model, AvatarRenderState state, AvatarRenderState inner,
			List<SavedPart> expected, List<SavedPart> expectedInner, List<SavedPart> caller)
	{
		var feature = new ModelFeatureRenderer();
		var poses = new PoseStack();
		poses.translate(0.4f, 0.2f, -0.3f);
		var submit = new SubmitNodeStorage.ModelSubmit<>(poses.last().copy(), model, state, 15728880, 0, -1, null, 0, null);
		var nested = new SubmitNodeStorage.ModelSubmit<>(poses.last().copy(), model, inner, 15728880, 0, -1, null, 0, null);
		PoseStack featurePoses;
		try
		{
			var field = ModelFeatureRenderer.class.getDeclaredField("poseStack");
			field.setAccessible(true);
			featurePoses = (PoseStack)field.get(feature);
		}
		catch (ReflectiveOperationException failure)
		{
			throw new AssertionError(failure);
		}
		featurePoses.pushPose();
		featurePoses.translate(2, 3, 4);
		featurePoses.scale(2, 0.5f, -1);
		var entry = featurePoses.last().copy();
		int depth = ((PoseStackAccessor)featurePoses).pulselib$getDepth();
		VertexConsumer innerFailure = consumer(() ->
		{
			matches(expectedInner, model, "Deferred inner rig at actual native vertex emission");
			featurePoses.pushPose();
			throw new IllegalStateException("rig inner draw");
		});
		VertexConsumer outerFailure = consumer(() ->
		{
			matches(expected, model, "Deferred outer rig at actual native vertex emission");
			expect("rig inner draw", () -> draw(feature, nested, innerFailure));
			matches(expected, model, "Nested deferred draw restores outer rig");
			model.leftSleeve.skipDraw = true;
			featurePoses.pushPose();
			throw new IllegalStateException("rig outer draw");
		});
		expect("rig outer draw", () -> draw(feature, submit, outerFailure));
		matches(caller, model, "Deferred failure restores caller model");
		check(((PoseStackAccessor)featurePoses).pulselib$getDepth() == depth &&
				featurePoses.last().pose().equals(entry.pose(), 0.00001f) && featurePoses.last().normal().equals(entry.normal(), 0.00001f), "Deferred failure restores feature pose/normal/depth");
		draw(feature, submit, consumer(() -> matches(expected, model, "Deferred recovery")));
		matches(caller, model, "Deferred recovery restores caller");
		draw(feature, nested, consumer(() -> matches(expectedInner, model, "Sequential different owner")));
		matches(caller, model, "Sequential owner restores caller");
		check(((PoseStackAccessor)featurePoses).pulselib$getDepth() == depth &&
				featurePoses.last().pose().equals(entry.pose(), 0.00001f), "Normal deferred draw restores feature stack");
	}

	private static void draw(ModelFeatureRenderer feature, SubmitNodeStorage.ModelSubmit<?> submit, VertexConsumer consumer)
	{
		try
		{
			var method = java.util.Arrays.stream(ModelFeatureRenderer.class.getDeclaredMethods()).filter(candidate -> candidate.getName().equals("renderModel")).findFirst().orElseThrow();
			method.setAccessible(true);
			method.invoke(feature, submit, RenderTypes.entitySolid(PLibDatabase.rl("textures/stage42a_white.png")), consumer, null, null);
		}
		catch (InvocationTargetException failure)
		{
			if (failure.getCause() instanceof RuntimeException runtime)
				throw runtime;
			if (failure.getCause() instanceof Error error)
				throw error;
			throw new AssertionError(failure.getCause());
		}
		catch (ReflectiveOperationException failure)
		{
			throw new AssertionError(failure);
		}
	}

	private static VertexConsumer consumer(Runnable vertex)
	{
		return (VertexConsumer)Proxy.newProxyInstance(VertexConsumer.class.getClassLoader(), new Class<?>[] {VertexConsumer.class}, (proxy, method, args) ->
		{
			if (method.getName().equals("addVertex"))
			{
				vertices++;
				vertex.run();
			}
			return method.getReturnType() == VertexConsumer.class ? proxy : null;
		});
	}

	private static SubmitNodeCollector collector()
	{
		return (SubmitNodeCollector)Proxy.newProxyInstance(SubmitNodeCollector.class.getClassLoader(), new Class<?>[] {SubmitNodeCollector.class}, (proxy, method, args) -> method.getName().equals("order") ? proxy : null);
	}

	private static void expect(String message, Runnable operation)
	{
		try
		{
			operation.run();
			throw new AssertionError("Missing expected failure " + message);
		}
		catch (IllegalStateException failure)
		{
			check(message.equals(failure.getMessage()), "Expected failure propagated: " + message);
			failures++;
		}
	}

	private static List<SavedPart> save(PlayerModel model)
	{
		return model.allParts().stream().map(SavedPart::new).toList();
	}

	private static void restore(List<SavedPart> parts)
	{
		parts.forEach(SavedPart::restore);
	}

	private static void matches(List<SavedPart> expected, PlayerModel model, String message)
	{
		check(expected.equals(save(model)), message);
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
			throw new AssertionError("Stage 5.2b.2a rig: " + message);
	}

	private record SavedPart(ModelPart part, PartPose pose, float xScale, float yScale, float zScale, boolean visible, boolean skipDraw)
	{
		private SavedPart(ModelPart part)
		{
			this(part, part.storePose(), part.xScale, part.yScale, part.zScale, part.visible, part.skipDraw);
		}

		private void restore()
		{
			this.part.loadPose(this.pose);
			this.part.xScale = this.xScale;
			this.part.yScale = this.yScale;
			this.part.zScale = this.zScale;
			this.part.visible = this.visible;
			this.part.skipDraw = this.skipDraw;
		}
	}
}
