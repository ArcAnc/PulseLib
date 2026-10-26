/**
 * @author ArcAnc
 * Created at: 26.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1;

import com.arcanc.pulselib.content.mixin.PoseStackAccessor;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import com.arcanc.pulselib.content.renderer.PRenderContexts;
import com.arcanc.pulselib.content.renderer.base.PPlayerRootRenderState;
import com.arcanc.pulselib.content.renderer.base.PWorldRenderState;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/**
 * Actual transformed AvatarRenderer submissions, including nested failures on the same renderer/stack.
 */
public final class Stage52RootProbe
{
	private static @Nullable List<Matrix4f> trace;
	private static int suites;
	private static int worldSamples;

	private Stage52RootProbe()
	{
	}

	public static int suites()
	{
		return suites;
	}

	public static int worldSamples()
	{
		return worldSamples;
	}

	public static void submitted(LivingEntityRenderState state, PoseStack poses)
	{
		if (trace != null)
			trace.add(new Matrix4f(poses.last().pose()));
		else if (state instanceof PPlayerRootRenderState root && root.pulselib$getPlayerRoot() != null)
			worldSamples++;
	}

	public static void verify()
	{
		var mc = Minecraft.getInstance();
		var renderer = mc.getEntityRenderDispatcher().getPlayerRenderer(mc.player);
		var state = renderer.createRenderState(mc.player, 0.65f);
		var snapshot = (PPlayerRootRenderState)state;
		var expected = new PoseStack();
		PPlayerAnimations.applyRoot(mc.player, expected, 0.65f);
		Matrix4f frozen = snapshot.pulselib$getPlayerRoot();
		check(frozen != null && frozen.equals(expected.last().pose(), 0.00001f) && !frozen.equals(new Matrix4f(), 0.00001f), "Visual ROOT sampled at extraction through the canonical evaluator");
		check(((PWorldRenderState)state).pulselib$requiresSeparateTargets(), "Active automatic player meshes retain native separate targets during extraction");
		// Native layers deliberately cannot resolve this owner during the following submission.
		state.id = Integer.MAX_VALUE;
		var inner = renderer.createRenderState(mc.player, 0.65f);
		inner.id = Integer.MAX_VALUE;
		Matrix4f innerRoot = new Matrix4f().translate(0.3f, -0.2f, 0.1f).rotateZ(0.4f);
		((PPlayerRootRenderState)inner).pulselib$setPlayerRoot(innerRoot);
		List<SavedPart> parts = renderer.getModel().allParts().stream().map(SavedPart::new).toList();
		var camera = new CameraRenderState();
		var poses = new PoseStack();
		poses.pushPose();
		poses.pushPose();
		poses.translate(0.2f, -0.3f, 0.4f);
		poses.scale(2, 0.5f, -1);
		poses.mulPose(new Quaternionf().rotationX(0.2f));
		int depth = ((PoseStackAccessor)poses).pulselib$getDepth();
		PoseStack.Pose entry = poses.last().copy();
		List<Matrix4f> observed = new ArrayList<>();
		trace = observed;
		try (var scope = PRenderContexts.enter(PRenderContexts.Context.GUI))
		{
			try
			{
				SubmitNodeCollector discard = collector((method, proxy) -> null);
				snapshot.pulselib$setPlayerRoot(null);
				renderer.submit(state, poses, discard, camera);
				check(observed.size() == 1, "Native ROOT boundary observed");
				Matrix4f vanilla = new Matrix4f(entry.pose()).invert().mul(observed.getFirst());
				snapshot.pulselib$setPlayerRoot(frozen);
				renderer.submit(state, poses, discard, camera);
				Matrix4f outer = new Matrix4f(entry.pose()).mul(vanilla).mul(frozen);
				matrix(outer, observed.get(1), "Captured ROOT survives absent late entity lookup and is inherited once");
				caller(poses, entry, depth);
				SubmitNodeCollector innerFailure = collector((method, proxy) ->
				{
					if (method.equals("submitModel"))
					{
						poses.pushPose();
						poses.pushPose();
						throw new IllegalStateException("stage52b inner");
					}
					return null;
				});
				SubmitNodeCollector outerFailure = collector((method, proxy) ->
				{
					if (method.equals("submitModel"))
					{
						PoseStack.Pose beforeInner = poses.last().copy();
						int innerDepth = ((PoseStackAccessor)poses).pulselib$getDepth();
						try
						{
							renderer.submit(inner, poses, innerFailure, camera);
							throw new AssertionError("Native nested submit must fail");
						}
						catch (IllegalStateException failure)
						{
							check(failure.getMessage().equals("stage52b inner"), "Nested failure propagates");
						}
						caller(poses, beforeInner, innerDepth);
						poses.pushPose();
						throw new IllegalStateException("stage52b outer");
					}
					return null;
				});
				try
				{
					renderer.submit(state, poses, outerFailure, camera);
					throw new AssertionError("Native outer submit must fail");
				}
				catch (IllegalStateException failure)
				{
					check(failure.getMessage().equals("stage52b outer"), "Outer failure propagates");
				}
				matrix(outer, observed.get(2), "Outer invocation ROOT");
				matrix(new Matrix4f(outer).mul(vanilla).mul(innerRoot), observed.get(3), "Nested invocation uses its own ROOT snapshot");
				caller(poses, entry, depth);
				renderer.submit(state, poses, discard, camera);
				matrix(outer, observed.get(4), "Native submission after failure recovers");
				caller(poses, entry, depth);
				suites++;
			}
			finally
			{
				scope.discard();
			}
		}
		finally
		{
			trace = null;
			parts.forEach(SavedPart::restore);
		}
	}

	private static SubmitNodeCollector collector(Handler handler)
	{
		return (SubmitNodeCollector)Proxy.newProxyInstance(SubmitNodeCollector.class.getClassLoader(), new Class<?>[] {SubmitNodeCollector.class}, (proxy, method, args) ->
		{
			if (method.getName().equals("order"))
				return proxy;
			if (method.getName().equals("hashCode"))
				return System.identityHashCode(proxy);
			if (method.getName().equals("equals"))
				return proxy == args[0];
			if (method.getName().equals("toString"))
				return "Stage52bDiscardCollector";
			return handler.call(method.getName(), proxy);
		});
	}

	private static void caller(PoseStack poses, PoseStack.Pose expected, int depth)
	{
		matrix(expected.pose(), poses.last().pose(), "Caller position matrix restored");
		check(expected.normal().equals(poses.last().normal(), 0.00001f), "Caller normal matrix restored");
		check(((PoseStackAccessor)poses).pulselib$getDepth() == depth, "Caller depth restored");
	}

	private static void matrix(Matrix4f expected, Matrix4f actual, String message)
	{
		check(expected.equals(actual, 0.0001f), message + " expected=" + expected + " actual=" + actual);
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
			throw new AssertionError("Stage 5.2b.1 ROOT: " + message);
	}

	@FunctionalInterface
	private interface Handler
	{
		@Nullable
		Object call(String method, Object proxy);
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
