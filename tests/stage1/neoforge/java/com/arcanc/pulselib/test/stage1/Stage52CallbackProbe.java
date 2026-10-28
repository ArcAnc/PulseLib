/**
 * @author ArcAnc
 * Created at: 28.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1;

import com.arcanc.pulselib.content.mixin.PoseStackAccessor;
import com.arcanc.pulselib.content.model.baked.PBakedBone;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.content.model.animation.PPoseEasing;
import com.arcanc.pulselib.content.model.animation.PRawAnimation;
import com.arcanc.pulselib.content.model.animation.PTransform;
import com.arcanc.pulselib.content.model.animation.PTransitionInterruptionPolicy;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationAnchor;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationDefinition;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import com.arcanc.pulselib.content.player.animation.PPlayerPart;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAnimatedAttachmentContext;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAnimatedAttachmentRenderer;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAnimatedAttachments;
import com.arcanc.pulselib.content.player.animation.firstPerson.PFirstPersonRenderPresentation;
import com.arcanc.pulselib.content.player.animation.firstPerson.PFirstPersonArmPose;
import com.arcanc.pulselib.content.player.animation.firstPerson.PFirstPersonItemPose;
import com.arcanc.pulselib.content.player.animation.firstPerson.PPlayerFirstPersonAnchorPose;
import com.arcanc.pulselib.content.renderer.PRenderContexts;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.plan.PFrameCompiler;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.content.renderer.PWorldRenderPass;
import com.arcanc.pulselib.content.renderer.base.PModelPartSnapshot;
import com.arcanc.pulselib.content.renderer.base.PWorldRenderState;
import com.arcanc.pulselib.platform.PClientRenderHooks;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PResourceCache;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.attachments.PAttachmentAnchor;
import com.arcanc.pulselib.util.attachments.PAttachmentAnchorResolvers;
import com.arcanc.pulselib.util.attachments.PLivingAttachmentDefinition;
import com.arcanc.pulselib.util.attachments.PLivingAttachmentLayer;
import com.arcanc.pulselib.util.attachments.PLivingAttachments;
import com.arcanc.pulselib.util.attachments.humanoid.PHumanoidBindings;
import com.arcanc.pulselib.util.attachments.humanoid.PHumanoidAnchors;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Native arbitrary callback scopes and actual getTransparencyChain selection, without pixel substitutes. */
public final class Stage52CallbackProbe
{
	private static final PPlayerAnimationAnchor ANCHOR = new PPlayerAnimationAnchor(PLibDatabase.rl("stage52b3_callback"));
	private static final PAttachmentAnchor LEGACY = PAttachmentAnchor.minecraft("stage52b3_callback");
	private static final List<String> ERRORS = new ArrayList<>();
	private static @Nullable Identifier activeDefinition;
	private static Consumer<PPlayerAnimatedAttachmentContext> animated = context ->
	{
	};
	private static Runnable legacy = () ->
	{
	};
	private static boolean registered;
	private static int suites;
	private static int checks;
	private static int targets;
	private static int calls;
	private static int expectedFailures;
	private static int queueCases;
	private static final PRenderQueue.RenderStage CALLBACK_STAGE = new PRenderQueue.RenderStage("stage52b3_callback");

	private Stage52CallbackProbe()
	{
	}

	public static void verify(Identifier scene, String clip)
	{
		register();
		var mc = Minecraft.getInstance();
		var renderer = mc.getEntityRenderDispatcher().getPlayerRenderer(mc.player);
		var model = renderer.getModel();
		var owner = owner(0x53b0 + suites * 2);
		var other = owner(0x53b1 + suites * 2);
		var anchorId = PLibDatabase.rl("stage52b3/anchor/" + suites);
		var rigId = PLibDatabase.rl("stage52b3/rig/" + suites);
		boolean[] enabled = {true};
		Runnable[] rig = {() -> {}};
		var data = PPlayerAnimations.get(scene).modelData();
		PPlayerAnimations.register(anchorId, PPlayerAnimationDefinition.builder(data).
				when(player -> enabled[0] && (player == owner || player == other)).
				bind(PPlayerPart.ROOT, "root").mask((player, part, tick) -> false).
				meshAttachment("stage52b3_missing").anchor(ANCHOR, "arm").
				crossfade(0, PPoseEasing.LINEAR, PTransitionInterruptionPolicy.FROM_CURRENT).
				controllers(registrar -> registrar.add("motion", () -> state -> com.arcanc.pulselib.content.animatable.ControllerState.PLAY)).build());
		PPlayerAnimations.register(rigId, PPlayerAnimationDefinition.builder(data).
				when(player -> enabled[0] && player == owner).
				meshAttachment("stage52b3_missing").bind(PPlayerPart.RIGHT_ARM, "arm").
				weight((player, tick) ->
				{
					rig[0].run();
					return 1;
				}).
				crossfade(0, PPoseEasing.LINEAR, PTransitionInterruptionPolicy.FROM_CURRENT).
				controllers(registrar -> registrar.add("motion", () -> state -> com.arcanc.pulselib.content.animatable.ControllerState.PLAY)).build());
		var poses = new PoseStack();
		poses.translate(0.125f, -0.25f, 0.5f);
		try (var caller = PModelPartSnapshot.capture(model.root()))
		{
			var bare = renderer.createRenderState(owner, 0.65f);
			activate(owner, anchorId, clip);
			activate(other, anchorId, clip);
			activeDefinition = anchorId;
			check(PPlayerAnimations.automaticMeshAttachmentPoses(owner, 0.65f).isEmpty(), "Anchor-only definition has no automatic meshes");
			int before = calls;
			var anchor = renderer.createRenderState(owner, 0.65f);
			check(calls == before, "Extraction must classify callbacks without executing renderers");
			target(bare, false, "bare");
			target(anchor, true, "custom anchor only");
			var commands = new PFirstPersonRenderPresentation(PFirstPersonArmPose.vanilla(), PFirstPersonArmPose.vanilla(), PFirstPersonItemPose.vanilla(), PFirstPersonItemPose.vanilla(),
					List.of(new PPlayerFirstPersonAnchorPose(anchorId, ANCHOR, PTransform.IDENTITY, 1)), List.of());
			for (boolean first : new boolean[] {false, true})
				for (boolean fail : new boolean[] {false, true})
				{
					var entry = poses.last().copy();
					int depth = depth(poses);
					var original = model.body.storePose();
					animated = context ->
					{
						context.poseStack().pushPose();
						context.poseStack().pushPose();
						context.poseStack().scale(2, 3, 4);
						model.body.x = 73;
						if (fail)
							throw new IllegalStateException("animated callback");
					};
					call(fail, "animated callback", () -> dispatch(owner, first, commands, poses));
					observe(depth(poses) == depth && poses.last().pose().equals(entry.pose()) && poses.last().normal().equals(entry.normal()), "animated stack " + first + "/" + fail + " depth=" + depth(poses) + "/" + depth);
					observe(model.body.storePose().equals(original), "animated model " + first + "/" + fail);
					while (depth(poses) > depth)
						poses.popPose();
					poses.last().set(entry);
					caller.restore();
				}
			animated = context ->
			{
			};
			nested(owner, other, commands, poses, model);
			queues(owner, commands, poses, model);
			arm(poses, model);
			enabled[0] = false;
			invoke(PPlayerAnimations.getInstance(owner, anchorId), "tick", false);
			invoke(PPlayerAnimations.getInstance(owner, anchorId), "tick", false);
			invoke(PPlayerAnimations.getInstance(other, anchorId), "tick", false);
			invoke(PPlayerAnimations.getInstance(other, anchorId), "tick", false);
			enabled[0] = true;
			activate(owner, rigId, clip);
			check(PPlayerAnimations.automaticMeshAttachmentPoses(owner, 0.65f).isEmpty(), "Rig-only definition has no automatic meshes");
			target(renderer.createRenderState(owner, 0.65f), true, "rig only");
			var original = model.body.storePose();
			rig[0] = () -> model.body.x = 91;
			renderer.createRenderState(owner, 0.65f);
			observe(model.body.storePose().equals(original), "Rig provider extraction restores caller model");
			caller.restore();
			rig[0] = () ->
			{
				model.body.x = 92;
				throw new IllegalStateException("rig provider");
			};
			call(true, "rig provider", () -> renderer.createRenderState(owner, 0.65f));
			observe(model.body.storePose().equals(original), "Failed rig provider restores caller model");
			boolean[] nestingRig = {true};
			rig[0] = () ->
			{
				model.body.x = 93;
				if (nestingRig[0])
				{
					nestingRig[0] = false;
					renderer.createRenderState(other, 0.65f);
					observe(model.body.x == 93, "Nested rig extraction restores enclosing model");
				}
			};
			renderer.createRenderState(owner, 0.65f);
			observe(!nestingRig[0] && model.body.storePose().equals(original), "Nested rig extraction restores caller model");
			rig[0] = () ->
			{
			};
			enabled[0] = false;
			invoke(PPlayerAnimations.getInstance(owner, rigId), "tick", false);
			invoke(PPlayerAnimations.getInstance(owner, rigId), "tick", false);
			owner.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
			target(renderer.createRenderState(owner, 0.65f), true, "native armor");
			var resolver = (PClientRenderHooks.ArmorModelResolver)field(PClientRenderHooks.class, "armorModels");
			PClientRenderHooks.installArmorModels((stack, type, selected) ->
			{
				model.body.x = 101;
				return selected;
			});
			try
			{
				renderer.createRenderState(owner, 0.65f);
				observe(model.body.storePose().equals(original), "Armor selector extraction restores caller model");
				PClientRenderHooks.installArmorModels((stack, type, selected) ->
				{
					model.body.x = 102;
					throw new IllegalStateException("armor selector");
				});
				call(true, "armor selector", () -> renderer.createRenderState(owner, 0.65f));
				observe(model.body.storePose().equals(original), "Failed armor selector restores caller model");
				PClientRenderHooks.installArmorModels((stack, type, selected) ->
				{
					model.body.x = 103;
					renderer.createRenderState(other, 0.65f);
					observe(model.body.x == 103, "Nested armor extraction restores enclosing model");
					return selected;
				});
				renderer.createRenderState(owner, 0.65f);
				observe(model.body.storePose().equals(original), "Nested armor selector restores caller model");
			}
			finally
			{
				PClientRenderHooks.installArmorModels(resolver);
				caller.restore();
			}
			owner.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
			boolean[] living = {true};
			PLivingAttachments.registerGlobal(new PLivingAttachmentDefinition(hierarchy(data),
					(entity, slot, stack) -> living[0] && (entity == owner || entity == mc.player), List.of(PHumanoidBindings.bind(LEGACY, "root"), PHumanoidBindings.bind(PHumanoidAnchors.RIGHT_ARM, "arm")),
					(entity, stack, bone, mesh, inherited, tick) ->
					{
						legacy.run();
						return inherited;
					}, false));
			try
			{
				var state = renderer.createRenderState(owner, 0.65f);
				target(state, true, "living armor callback");
				var layer = new PLivingAttachmentLayer<AvatarRenderState, PlayerModel>(renderer);
				legacyMatrices(layer, state, poses, model);
				for (boolean fail : new boolean[] {false, true})
				{
					var entry = poses.last().copy();
					int depth = depth(poses);
					legacy = () ->
					{
						poses.pushPose();
						poses.pushPose();
						poses.translate(9, 8, 7);
						model.body.x = 113;
						if (fail)
							throw new IllegalStateException("legacy armor callback");
					};
					try (var scope = PRenderContexts.enter(PRenderContexts.Context.GUI))
					{
						try
						{
							call(fail, "legacy armor callback", () -> layer.submit(poses, new SubmitNodeStorage(), 15728880, state, 0, 0));
						}
						finally
						{
							scope.discard();
						}
					}
					observe(depth(poses) == depth && poses.last().pose().equals(entry.pose()), "Legacy armor stack " + fail);
					observe(model.body.storePose().equals(original), "Legacy anchor/resolver model " + fail);
					while (depth(poses) > depth)
						poses.popPose();
					poses.last().set(entry);
					caller.restore();
				}
				var persistent = new PFirstPersonRenderPresentation(PFirstPersonArmPose.animated(PTransform.IDENTITY), PFirstPersonArmPose.vanilla(),
						PFirstPersonItemPose.vanilla(), PFirstPersonItemPose.vanilla(), List.of(), List.of());
				for (boolean fail : new boolean[] {false, true})
				{
					var entry = poses.last().copy();
					int depth = depth(poses);
					legacy = () ->
					{
						poses.pushPose();
						poses.pushPose();
						model.body.x = 114;
						if (fail)
							throw new IllegalStateException("first-person armor callback");
					};
					try (var scope = PRenderContexts.enter(PRenderContexts.Context.GUI))
					{
						try
						{
							call(fail, "first-person armor callback", () -> com.arcanc.pulselib.content.player.animation.firstPerson.PPlayerFirstPersonRenderer.renderExtras(
									mc.player, persistent, poses, new SubmitNodeStorage(), 15728880, 0.65f));
						}
						finally
						{
							scope.discard();
						}
					}
					observe(depth(poses) == depth && poses.last().pose().equals(entry.pose()) && model.body.storePose().equals(original), "Persistent first-person armor restoration " + fail);
				}
				boolean[] nestingLegacy = {true};
				legacy = () ->
				{
					poses.pushPose();
					model.body.x = 115;
					if (nestingLegacy[0])
					{
						nestingLegacy[0] = false;
						int depth = depth(poses);
						var entry = poses.last().copy();
						call(true, "nested legacy callback", () -> layer.submit(poses, new SubmitNodeStorage(), 15728880, state, 0, 0));
						observe(model.body.x == 115 && depth(poses) == depth && poses.last().pose().equals(entry.pose()), "Nested legacy armor restores enclosing model/stack");
					}
					throw new IllegalStateException("nested legacy callback");
				};
				var entry = poses.last().copy();
				int depth = depth(poses);
				call(true, "nested legacy callback", () -> layer.submit(poses, new SubmitNodeStorage(), 15728880, state, 0, 0));
				observe(!nestingLegacy[0] && depth(poses) == depth && poses.last().pose().equals(entry.pose()) && model.body.storePose().equals(original), "Nested legacy armor restores caller");
			}
			finally
			{
				living[0] = false;
				legacy = () ->
				{
				};
			}
			observe(!((PWorldRenderState)renderer.createRenderState(other, 0.65f)).pulselib$requiresSeparateTargets(), "Unrelated owner does not inherit armor/rig eligibility");
		}
		finally
		{
			enabled[0] = false;
			activeDefinition = null;
			animated = context ->
			{
			};
		}
		PLibDatabase.LOGGER.info("STAGE 5.2b.3 CALLBACK RESULT: suite={}, checks={}, targetChecks={}, callbackCalls={}, expectedFailures={}, errors={}", suites, checks, targets, calls, expectedFailures, ERRORS);
		check(ERRORS.isEmpty(), "Callback isolation/target eligibility failures: " + ERRORS);
		suites++;
	}

	private static void register()
	{
		if (registered)
			return;
		registered = true;
		PAttachmentAnchorResolvers.register(PlayerModel.class, LEGACY, (state, model) ->
		{
			((PlayerModel)model).body.x = 17;
			return ((PlayerModel)model).body;
		});
		PPlayerAnimatedAttachments.register(new PPlayerAnimatedAttachmentRenderer()
		{
			@Override
			public PPlayerAnimationAnchor anchor()
			{
				return ANCHOR;
			}

			@Override
			public void render(PPlayerAnimatedAttachmentContext context)
			{
				if (!context.animation().equals(activeDefinition))
					return;
				calls++;
				animated.accept(context);
			}
		});
	}

	private static void nested(RemotePlayer owner, RemotePlayer other, PFirstPersonRenderPresentation commands, PoseStack poses, PlayerModel model)
	{
		var entry = poses.last().copy();
		int depth = depth(poses);
		var original = model.body.storePose();
		animated = context ->
		{
			context.poseStack().pushPose();
			model.body.x = context.player() == owner ? 41 : 42;
			if (context.player() == owner)
			{
				var enclosing = poses.last().copy();
				int enclosingDepth = depth(poses);
				call(true, "nested callback", () -> dispatch(other, true, commands, poses));
				observe(model.body.x == 41 && depth(poses) == enclosingDepth && poses.last().pose().equals(enclosing.pose()), "Nested callback restores outer model/stack");
			}
			throw new IllegalStateException("nested callback");
		};
		call(true, "nested callback", () -> dispatch(owner, true, commands, poses));
		observe(depth(poses) == depth && poses.last().pose().equals(entry.pose()) && model.body.storePose().equals(original), "Nested outer callback restores caller");
		while (depth(poses) > depth)
			poses.popPose();
		poses.last().set(entry);
		model.body.loadPose(original);
	}

	@SuppressWarnings("unchecked")
	private static void arm(PoseStack poses, PlayerModel model)
	{
		var mc = Minecraft.getInstance();
		var stencil = (Predicate<TextureFormat>)field(PClientRenderHooks.class, "stencilAspect");
		var arms = (PClientRenderHooks.ArmRenderer)field(PClientRenderHooks.class, "armRenderer");
		var entry = poses.last().copy();
		int depth = depth(poses);
		PClientRenderHooks.install(stencil, (renderer, pose, collector, light, skin, sleeve, player, right) ->
		{
			pose.pushPose();
			pose.pushPose();
			pose.scale(2, 3, 4);
			model.body.x = 88;
			throw new IllegalStateException("platform arm callback");
		});
		try
		{
			var original = model.body.storePose();
			call(true, "platform arm callback", () -> PClientRenderHooks.renderArm(mc.getEntityRenderDispatcher().getPlayerRenderer(mc.player), poses,
					new SubmitNodeStorage(), 15728880, mc.player.getSkin().body().texturePath(), true, mc.player, true));
			observe(depth(poses) == depth && poses.last().pose().equals(entry.pose()) && model.body.storePose().equals(original), "Platform arm callback restores caller");
		}
		finally
		{
			PClientRenderHooks.install(stencil, arms);
			while (depth(poses) > depth)
				poses.popPose();
			poses.last().set(entry);
		}
	}

	/** Exercise parent/child traversal with real meshes decoded from each unchanged Gecko/glTF asset. */
	private static PModelData hierarchy(PModelData source)
	{
		var decoded = source.getModel();
		var meshes = decoded.bone(decoded.boneIndex("ball")).meshes();
		check(!meshes.isEmpty(), "The real decoded motion asset supplies mesh geometry");
		List<PBakedBone> children = new ArrayList<>();
		var root = new PBakedBone("root", new Vector3f(), new Quaternionf(), children, null, meshes);
		children.add(new PBakedBone("arm", new Vector3f(0.2f, 0.3f, 0.4f), new Quaternionf().rotationY(0.2f), List.of(), root, meshes));
		children.add(new PBakedBone("sibling", new Vector3f(-0.3f, 0.1f, 0.5f), new Quaternionf().rotationX(0.3f), List.of(), root, meshes));
		var model = new PBakedModel(List.of(root), Map.of());
		return new PModelData(PLibDatabase.rl("stage52b3/hierarchy/" + suites), source.getModelLoaderId())
		{
			@Override
			public PBakedModel getModel()
			{
				return model;
			}
		};
	}

	private static void legacyMatrices(PLivingAttachmentLayer<AvatarRenderState, PlayerModel> layer, AvatarRenderState state, PoseStack poses, PlayerModel model)
	{
		List<Matrix4f> matrices = new ArrayList<>();
		List<Float> modelPositions = new ArrayList<>();
		legacy = () ->
		{
			matrices.add(new Matrix4f(poses.last().pose()));
			modelPositions.add(model.body.x);
		};
		try (var scope = PRenderContexts.enter(PRenderContexts.Context.GUI))
		{
			try
			{
				layer.submit(poses, new SubmitNodeStorage(), 15728880, state, 0, 0);
			}
			finally
			{
				scope.discard();
			}
		}
		check(matrices.size() == 4, "Decoded meshes appear on root, child, sibling and separate arm binding");
		int[] index = {0};
		legacy = () ->
		{
			int current = index[0]++;
			observe(current < matrices.size() && poses.last().pose().equals(matrices.get(current)) && model.body.x == modelPositions.get(current), "Each legacy mesh receives its own unmodified ancestor transform: mesh=" + current);
			poses.pushPose();
			poses.pushPose();
			poses.scale(2, 3, 4);
			model.body.x = 116;
		};
		try (var scope = PRenderContexts.enter(PRenderContexts.Context.GUI))
		{
			try
			{
				layer.submit(poses, new SubmitNodeStorage(), 15728880, state, 0, 0);
			}
			finally
			{
				scope.discard();
			}
		}
		observe(index[0] == matrices.size(), "Legacy resolver preserves all original mesh callbacks");
	}

	@SuppressWarnings("unchecked")
	private static void queues(RemotePlayer owner, PFirstPersonRenderPresentation commands, PoseStack poses, PlayerModel model)
	{
		var world = (PFrameCompiler<PRenderQueue.RenderStage>)field(PRenderQueue.class, "COMPILER");
		int before = queued(world);
		var color = RenderSystem.outputColorTextureOverride;
		var depth = RenderSystem.outputDepthTextureOverride;
		var target = new TextureTarget("stage52b3 callback offscreen", 32, 24, true);
		var type = PRenderTypes.RenderTypeProvider.trianglesEmissiveTranslucent(PResourceCache.ATLAS_LOCATION);
		var quad = Stage42aWorldStageProbe.quad();
		try
		{
			for (var context : List.of(PRenderContexts.Context.WORLD, PRenderContexts.Context.GUI, PRenderContexts.Context.OFFSCREEN))
				for (boolean first : new boolean[] {false, true})
					for (boolean fail : new boolean[] {false, true})
					{
						RenderSystem.outputColorTextureOverride = context == PRenderContexts.Context.OFFSCREEN ? target.getColorTextureView() : color;
						RenderSystem.outputDepthTextureOverride = context == PRenderContexts.Context.OFFSCREEN ? target.getDepthTextureView() : depth;
						try (var scope = PRenderContexts.enter(context))
						{
							var local = context == PRenderContexts.Context.WORLD ? world : (PFrameCompiler<PRenderQueue.RenderStage>)field(scope, "compiler");
							@Nullable Matrix4f[] expected = {null};
							var entry = poses.last().copy();
							int stackDepth = depth(poses);
							var original = model.body.storePose();
							animated = callback ->
							{
								check(PRenderContexts.current() == context, "Callback retains its execution context");
								callback.poseStack().pushPose();
								expected[0] = new Matrix4f(callback.poseStack().last().pose());
								var header = new PInstanceHeader(new Matrix4f(expected[0]), -1, 15728880, 0);
								PRenderQueue.submit(CALLBACK_STAGE, type, quad, header);
								callback.poseStack().scale(7, 8, 9);
								model.body.x = 79;
								if (fail)
									throw new IllegalStateException("queued callback");
							};
							call(fail, "queued callback", () -> dispatch(owner, first, commands, poses));
							var plan = local.compile(CALLBACK_STAGE);
							observe(plan.instances().size() == 1 && plan.instances().getFirst().transform().equals(expected[0]), "Callback submissions retain their own matrices " + context + "/" + first + "/" + fail);
							observe(queued(world) == before && depth(poses) == stackDepth && poses.last().pose().equals(entry.pose()) && model.body.storePose().equals(original), "Callback queue/model/stack ownership " + context + "/" + first + "/" + fail);
							queueCases++;
						}
					}
		}
		finally
		{
			RenderSystem.outputColorTextureOverride = color;
			RenderSystem.outputDepthTextureOverride = depth;
			target.destroyBuffers();
			animated = callback ->
			{
			};
		}
	}

	private static int queued(Object compiler)
	{
		return count(field(compiler, "opaque")) + count(field(compiler, "translucent"));
	}

	private static int count(Object values)
	{
		if (values instanceof Map<?, ?> map)
			return map.values().stream().mapToInt(Stage52CallbackProbe::count).sum();
		return values instanceof List<?> list ? list.size() : 0;
	}

	private static void dispatch(RemotePlayer owner, boolean first, PFirstPersonRenderPresentation presentation, PoseStack poses)
	{
		if (first)
			PPlayerAnimatedAttachments.renderFirstPerson(owner, presentation, poses, new SubmitNodeStorage(), 15728880, 0.65f);
		else
			PPlayerAnimatedAttachments.renderThirdPerson(owner, poses, new SubmitNodeStorage(), 15728880, 0.65f);
	}

	private static void target(AvatarRenderState owner, boolean expected, String label)
	{
		var mc = Minecraft.getInstance();
		var world = (LevelRenderState)field(mc.levelRenderer, "levelRenderState");
		var entities = new ArrayList<>(world.entityRenderStates);
		var blocks = new ArrayList<>(world.blockEntityRenderStates);
		boolean panorama = world.cameraRenderState.isPanoramicMode;
		boolean improved = mc.options.improvedTransparency().get();
		var options = mc.gameRenderer.getGameRenderState().optionsRenderState;
		boolean extractedImproved = options.improvedTransparency;
		world.entityRenderStates.clear();
		world.blockEntityRenderStates.clear();
		world.entityRenderStates.add(owner);
		try
		{
			for (boolean graphics : new boolean[] {false, true})
				for (boolean panoramic : new boolean[] {false, true})
				{
					mc.options.improvedTransparency().set(graphics);
					options.improvedTransparency = graphics;
					world.cameraRenderState.isPanoramicMode = panoramic;
					observe(PWorldRenderPass.requiresSeparateTargets(world) == (expected && !panoramic), "World target " + label + "/" + graphics + "/" + panoramic);
					targets++;
					if (!panoramic)
					{
						observe((invoke(mc.levelRenderer, "getTransparencyChain") != null) == (Minecraft.useShaderTransparency() || expected), "Native transparency chain " + label + "/" + graphics);
						targets++;
					}
				}
		}
		finally
		{
			world.entityRenderStates.clear();
			world.entityRenderStates.addAll(entities);
			world.blockEntityRenderStates.addAll(blocks);
			world.cameraRenderState.isPanoramicMode = panorama;
			mc.options.improvedTransparency().set(improved);
			options.improvedTransparency = extractedImproved;
		}
	}

	private static RemotePlayer owner(int id)
	{
		var mc = Minecraft.getInstance();
		return new RemotePlayer(mc.level, new GameProfile(new UUID(0, id), "CallbackOwner"))
		{
			@Override
			public PlayerSkin getSkin()
			{
				return mc.player.getSkin();
			}
		};
	}

	private static void activate(RemotePlayer player, Identifier definition, String clip)
	{
		check(PPlayerAnimations.getHandle(player, definition).play("motion", PRawAnimation.begin().thenLoop(clip).build()), "Fixture controller starts");
		invoke(PPlayerAnimations.getInstance(player, definition), "tick", true);
		invoke(PPlayerAnimations.getInstance(player, definition), "tick", true);
	}

	private static int depth(PoseStack poses)
	{
		return ((PoseStackAccessor)poses).pulselib$getDepth();
	}

	private static void call(boolean fail, String message, Runnable action)
	{
		try
		{
			action.run();
			check(!fail, "Expected callback exception " + message);
		}
		catch (IllegalStateException exception)
		{
			check(fail && message.equals(exception.getMessage()), "Expected callback failure propagated: " + exception);
			expectedFailures++;
		}
	}

	private static void observe(boolean condition, String message)
	{
		checks++;
		if (!condition)
			ERRORS.add(message);
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
			throw new AssertionError("Stage 5.2b.3: " + message);
	}

	public static void completed()
	{
		check(suites == 4 && queueCases == 48 && ERRORS.isEmpty(), "All Gecko/glTF callback suites before/after reload");
		PLibDatabase.LOGGER.info("STAGE 5.2b.3 CALLBACK PASS: suites={}, checks={}, targetChecks={}, callbackCalls={}, expectedFailures={}, queueCases={}", suites, checks, targets, calls, expectedFailures, queueCases);
	}

	private static Object field(Object object, String name)
	{
		for (Class<?> type = object instanceof Class<?> target ? target : object.getClass(); type != null; type = type.getSuperclass())
			try
			{
				var field = type.getDeclaredField(name);
				field.setAccessible(true);
				return field.get(object instanceof Class<?> ? null : object);
			}
			catch (NoSuchFieldException ignored)
			{
			}
			catch (ReflectiveOperationException exception)
			{
				throw new AssertionError(exception);
			}
		throw new AssertionError(name);
	}

	private static Object invoke(Object object, String name, Object... args)
	{
		for (Class<?> type = object.getClass(); type != null; type = type.getSuperclass())
			for (var method : type.getDeclaredMethods())
				if (method.getName().equals(name) && method.getParameterCount() == args.length && !method.isBridge())
					try
					{
						method.setAccessible(true);
						return method.invoke(object, args);
					}
					catch (InvocationTargetException exception)
					{
						if (exception.getCause() instanceof Error error)
							throw error;
						if (exception.getCause() instanceof RuntimeException runtime)
							throw runtime;
						throw new AssertionError(exception.getCause());
					}
					catch (ReflectiveOperationException exception)
					{
						throw new AssertionError(exception);
					}
		throw new AssertionError(name);
	}
}
