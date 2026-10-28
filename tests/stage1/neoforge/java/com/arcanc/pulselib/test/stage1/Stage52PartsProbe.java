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
import com.arcanc.pulselib.content.model.deformer.PChannelReference;
import com.arcanc.pulselib.content.model.deformer.PDeformerInstance;
import com.arcanc.pulselib.content.model.deformer.PDeformerStack;
import com.arcanc.pulselib.content.model.deformer.PDeformerPrepareContext;
import com.arcanc.pulselib.content.model.deformer.PMeshDeformer;
import com.arcanc.pulselib.content.model.deformer.PStretchDeformer;
import com.arcanc.pulselib.content.model.deformer.PStretchDefinition;
import com.arcanc.pulselib.content.model.animation.PRawAnimation;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import com.arcanc.pulselib.content.player.animation.PPlayerPart;
import com.arcanc.pulselib.content.player.deformer.PPlayerMeshDeformers;
import com.arcanc.pulselib.content.renderer.base.PModelPartSnapshot;
import com.arcanc.pulselib.platform.PClientRenderHooks;
import com.arcanc.pulselib.util.PLibDatabase;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelPartFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.joml.Vector3f;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;

/** Actual hand submissions, deferred ModelPart passes and item state reuse/nested recovery. */
public final class Stage52PartsProbe
{
	private static @Nullable PartDraw partDraw;
	private static @Nullable ItemDraw itemDraw;
	private static int suites;
	private static int failures;
	private static int partVertices;
	private static int itemQuads;
	private static int deformerSuites;
	private static int deformerFailures;
	private static int valueSuites;
	private static int valueFailures;
	private static int reloadReplays;
	private static final List<FrozenPart> BEFORE_RELOAD = new ArrayList<>();
	private static final Set<String> PART_PASSES = new HashSet<>();
	private static final Set<String> ITEM_PASSES = new HashSet<>();

	private Stage52PartsProbe()
	{
	}

	public static void completed()
	{
		check(suites == 4 && failures == 32 && partVertices > 0 && itemQuads > 0, "Both formats/phases and all nested failure/recovery suites");
		check(PART_PASSES.equals(Set.of("main", "outline", "crumbling")) && ITEM_PASSES.equals(Set.of("main", "outline", "foil")), "All native feature branches observed");
		check(deformerSuites == 4 && deformerFailures == 8, "Cube binding replay/recovery in both formats before/after reload");
		check(valueSuites == 4 && valueFailures == 8 && reloadReplays == 2 && BEFORE_RELOAD.isEmpty(), "Frozen values, two owners, controller stop, custom branches and old nodes across actual reload");
		PLibDatabase.LOGGER.info("STAGE 5.2b.2b PART/ITEM PASS: suites={}, expectedFailures={}, partVertices={}, itemQuads={}, partPasses={}, itemPasses={}, hand/modelPart/item snapshot/nested failure/recovery/reload PASS", suites, failures, partVertices, itemQuads, PART_PASSES, ITEM_PASSES);
		PLibDatabase.LOGGER.info("STAGE 5.2b.2c.1 CUBE BINDINGS PASS: suites={}, expectedFailures={}, native main/outline/crumbling geometry, model reuse/nested recovery/reload PASS; channel sampling/armor owner extraction pending", deformerSuites, deformerFailures);
		PLibDatabase.LOGGER.info("STAGE 5.2b.2c.2a FROZEN VALUES PASS: suites={}, expectedFailures={}, oldNodesAfterReload={}, two owners/controller stop/custom position-dependent channels/operation mutation/main/outline/crumbling/nested recovery PASS; automatic main/armor extraction pending", valueSuites, valueFailures, reloadReplays);
	}

	public static @Nullable VertexConsumer partBuffer(String pass)
	{
		VertexConsumer geometry = Stage52ArmorProbe.buffer(pass);
		if (geometry != null)
			return geometry;
		PartDraw draw = partDraw;
		if (draw == null)
			return null;
		return consumer((name, args) ->
		{
			if (name.equals("addVertex"))
			{
				partVertices++;
				PART_PASSES.add(pass);
				draw.vertices.computeIfAbsent(pass, ignored -> new ArrayList<>()).add(new Vector3f((float)args[0], (float)args[1], (float)args[2]));
				matches(draw.expected, "Deferred " + pass + " uses captured ModelPart pose/scale/visibility");
				if (!draw.fired && draw.firstVertex != null)
				{
					draw.fired = true;
					draw.firstVertex.run();
				}
			}
			return null;
		});
	}

	public static @Nullable VertexConsumer itemBuffer(String pass)
	{
		ItemDraw draw = itemDraw;
		if (draw == null)
			return null;
		return consumer((name, args) ->
		{
			if (name.equals("putBakedQuad"))
			{
				itemQuads++;
				ITEM_PASSES.add(pass);
				QuadInstance values = (QuadInstance)args[2];
				itemValues(draw.submit, values);
				if (!draw.fired && draw.firstQuad != null)
				{
					draw.fired = true;
					draw.firstQuad.run();
					itemValues(draw.submit, values);
				}
			}
			return null;
		});
	}

	public static void verify(Identifier definition, String clip)
	{
		var mc = Minecraft.getInstance();
		var renderer = mc.getEntityRenderDispatcher().getPlayerRenderer(mc.player);
		var model = renderer.getModel();
		List<SavedPart> caller = save(model.root());
		var poses = new PoseStack();
		poses.pushPose();
		poses.translate(0.2f, -0.3f, 0.4f);
		poses.scale(2, 0.5f, -1);
		var entry = poses.last().copy();
		int depth = depth(poses);
		try
		{
			var nodes = new SubmitNodeStorage();
			var skin = mc.player.getSkin().body().texturePath();
			// Independent oracle for the exact native hand setup, including opposite-arm edits.
			model.rightArm.resetPose();
			model.rightArm.visible = true;
			model.leftArm.zRot = -0.1f;
			model.rightArm.zRot = 0.1f;
			model.leftSleeve.visible = false;
			model.rightSleeve.visible = false;
			List<SavedPart> right = save(model.rightArm);
			List<SavedPart> rightModel = save(model.root());
			restore(caller);
			PClientRenderHooks.renderArm(renderer, poses, nodes, 15728880, skin, false, mc.player, true);
			matches(caller, "Right hand returns caller model");
			stack(poses, entry, depth);
			model.leftArm.resetPose();
			model.leftArm.visible = true;
			model.leftArm.zRot = -0.1f;
			model.rightArm.zRot = 0.1f;
			model.leftSleeve.visible = true;
			model.rightSleeve.visible = true;
			List<SavedPart> left = save(model.leftArm);
			restore(caller);
			PClientRenderHooks.renderArm(renderer, poses, nodes, 15728880, skin, true, mc.player, false);
			matches(caller, "Left hand returns caller model");
			List<SubmitNodeStorage.ModelPartSubmit> queued = partNodes(nodes);
			check(queued.size() == 2, "Two actual native arm nodes");
			var rightNode = queued.stream().filter(node -> node.modelPart() == model.rightArm).findFirst().orElseThrow();
			var leftNode = queued.stream().filter(node -> node.modelPart() == model.leftArm).findFirst().orElseThrow();
			restore(right);
			var fullRight = fullNode(rightNode);
			restore(left);
			var fullLeft = fullNode(leftNode);
			restore(caller);
			SubmitNodeCollector innerFailure = collector((name, args) ->
			{
				if (name.equals("submitModelPart"))
				{
					poses.pushPose();
					poses.pushPose();
					model.leftSleeve.skipDraw = true;
					throw new IllegalStateException("inner hand");
				}
				return null;
			});
			SubmitNodeCollector outerFailure = collector((name, args) ->
			{
				if (name.equals("submitModelPart"))
				{
					matches(rightModel, "Outer native hand setup");
					expect("inner hand", () -> PClientRenderHooks.renderArm(renderer, poses, innerFailure, 15728880, skin, true, mc.player, false));
					matches(rightModel, "Inner hand failure returns outer model");
					poses.pushPose();
					model.rightSleeve.xScale = 9;
					throw new IllegalStateException("outer hand");
				}
				return null;
			});
			expect("outer hand", () -> PClientRenderHooks.renderArm(renderer, poses, outerFailure, 15728880, skin, false, mc.player, true));
			matches(caller, "Hand exception restores all affected model parts");
			stack(poses, entry, depth);
			// Reuse the same model before executing the old nodes; sleeves now deliberately disagree.
			model.rightArm.x += 7;
			model.rightArm.xScale = 3;
			model.rightSleeve.visible = true;
			model.leftArm.zRot += 0.7f;
			model.leftSleeve.visible = false;
			List<SavedPart> reused = save(model.root());
			var feature = new ModelPartFeatureRenderer();
			PoseStack featurePoses = field(feature, "poseStack");
			featurePoses.pushPose();
			featurePoses.translate(1, 2, 3);
			featurePoses.scale(2, -1, 0.5f);
			var featureEntry = featurePoses.last().copy();
			int featureDepth = depth(featurePoses);
			part(feature, rightNode, right, null);
			part(feature, leftNode, left, null);
			part(feature, fullRight, right, null);
			part(feature, fullLeft, left, null);
			matches(reused, "Normal deferred passes restore reused caller model");
			expect("outer part", () -> part(feature, fullRight, right, () ->
			{
				var outerPose = featurePoses.last().copy();
				int outerDepth = depth(featurePoses);
				expect("inner part", () -> part(feature, fullLeft, left, () ->
				{
					featurePoses.pushPose();
					model.leftSleeve.skipDraw = true;
					throw new IllegalStateException("inner part");
				}));
				matches(right, "Nested deferred part returns outer captured pose");
				stack(featurePoses, outerPose, outerDepth);
				featurePoses.pushPose();
				model.rightArm.visible = false;
				throw new IllegalStateException("outer part");
			}));
			matches(reused, "Deferred exception restores reused caller model");
			stack(featurePoses, featureEntry, featureDepth);
			part(feature, fullRight, right, null);
			matches(reused, "Deferred part recovery");
			stack(featurePoses, featureEntry, featureDepth);
			verifyItems(poses);
			verifyDeformerBindings(poses);
			verifyFrozenValues(poses, definition, clip);
			stack(poses, entry, depth);
			suites++;
		}
		finally
		{
			restore(caller);
			partDraw = null;
			itemDraw = null;
		}
	}

	private static void verifyFrozenValues(PoseStack poses, Identifier definition, String clip)
	{
		if (valueSuites == 2)
		{
			for (FrozenPart previous : BEFORE_RELOAD)
			{
				geometryMatches(previous.vertices, part(new ModelPartFeatureRenderer(), previous.node, previous.transforms, null));
				reloadReplays++;
			}
			BEFORE_RELOAD.clear();
		}
		var mc = Minecraft.getInstance();
		var model = mc.getEntityRenderDispatcher().getPlayerRenderer(mc.player).getModel();
		var arm = model.rightArm;
		var other = new RemotePlayer(mc.level, new GameProfile(new UUID(0, 0x525c20 + valueSuites), "FrozenOther"));
		var id = PLibDatabase.rl("stage52c2a_values");
		float[] operation = {1};
		int[] reads = {0};
		Set<String> queries = new HashSet<>();
		PMeshDeformer<Integer> custom = new PMeshDeformer<>()
		{
			@Override
			public Identifier id()
			{
				return PLibDatabase.rl("stage52c2a_custom");
			}

			@Override
			public MapCodec<Integer> codec()
			{
				return MapCodec.unit(0);
			}

			@Override
			public void prepare(PDeformerPrepareContext context, Integer ignored)
			{
				context.add((position, values) ->
				{
					String name = (position.x < -1 ? "negative/" : "positive/") + (int)Math.floor(position.y);
					float value = values.resolve(new PChannelReference<>(name, Math.abs(position.z) * 0.125f));
					position.x += value * operation[0];
				});
			}
		};
		var stack = PDeformerStack.compile(List.of(new PDeformerInstance<>(custom, 0)));
		try (var caller = PModelPartSnapshot.capture(model.root()))
		{
			arm.visible = true;
			arm.skipDraw = false;
			check(PPlayerAnimations.getHandle(other, definition).play("motion", PRawAnimation.begin().thenLoop(clip).build()), "Second owner controller starts");
			PPlayerMeshDeformers.register(id, PPlayerPart.RIGHT_ARM, player -> player == mc.player || player == other, stack, (player, reference) ->
			{
				reads[0]++;
				queries.add(reference.name());
				float playing = PPlayerAnimations.getHandle(player, definition).isPlaying("motion") ? 1 : 9;
				return (player == other ? 24 : 8) * playing + reference.defaultValue();
			});
			PPlayerMeshDeformers.apply(mc.player, model);
			int hiddenReads = reads[0];
			arm.visible = false;
			frozenNode(arm, poses);
			arm.visible = true;
			arm.skipDraw = true;
			boolean sleeveVisible = model.rightSleeve.visible;
			model.rightSleeve.visible = false;
			frozenNode(arm, poses);
			check(reads[0] == hiddenReads, "Hidden arm and skipped own cubes never evaluate callbacks");
			model.rightSleeve.visible = true;
			frozenNode(arm, poses);
			check(reads[0] > hiddenReads, "skipDraw preserves the visible deformed child overlay");
			model.rightSleeve.visible = sleeveVisible;
			arm.skipDraw = false;
			hiddenReads = reads[0];
			model.root().visible = false;
			frozenNode(model.root(), poses);
			model.root().visible = true;
			check(reads[0] == hiddenReads, "Hidden root/descendants and skipDraw cubes do not evaluate custom callbacks");
			List<Vector3f> firstVertices = geometry(arm, poses);
			var first = frozenNode(arm, poses);
			PPlayerMeshDeformers.apply(other, model);
			List<Vector3f> secondVertices = geometry(arm, poses);
			var second = frozenNode(arm, poses);
			check(queries.size() > 3 && !firstVertices.equals(secondVertices), "Custom operation samples many vertex-dependent channel references and distinguishes owners");
			PPlayerAnimations.getHandle(mc.player, definition).stopAll();
			PPlayerAnimations.getHandle(other, definition).stopAll();
			operation[0] = 100;
			PPlayerMeshDeformers.unregister(id);
			PPlayerMeshDeformers.apply(mc.player, model);
			List<Vector3f> reused = geometry(arm, poses);
			List<SavedPart> transforms = save(arm);
			int readsBeforeReplay = reads[0];
			var feature = new ModelPartFeatureRenderer();
			geometryMatches(firstVertices, part(feature, first, transforms, null));
			geometryMatches(secondVertices, part(feature, second, transforms, null));
			expectValueFailure("outer values", () -> part(feature, first, transforms, () ->
			{
				expectValueFailure("inner values", () -> part(feature, second, transforms, () ->
				{
					throw new IllegalStateException("inner values");
				}));
				check(firstVertices.equals(geometry(arm, poses)), "Nested failure restores enclosing frozen geometry");
				throw new IllegalStateException("outer values");
			}));
			geometryMatches(firstVertices, part(feature, first, transforms, null));
			check(reused.equals(geometry(arm, poses)) && reads[0] == readsBeforeReplay, "Replay never calls live values or prepared operations, and restores caller bindings");
			if (valueSuites < 2)
				BEFORE_RELOAD.add(new FrozenPart(first, firstVertices, transforms));
			PLibDatabase.LOGGER.info("STAGE 5.2b.2c.2a VALUES CASE PASS: suite={}, queries={}, readsBeforeReplay={}, readsAfterReplay={}, vertices={}, oldNodesAfterReload={}", valueSuites, queries.size(), readsBeforeReplay, reads[0], firstVertices.size(), reloadReplays);
			valueSuites++;
		}
		finally
		{
			PPlayerMeshDeformers.unregister(id);
			PPlayerAnimations.getHandle(other, definition).stopAll();
			check(PPlayerAnimations.getHandle(mc.player, definition).play("motion", PRawAnimation.begin().thenLoop(clip).build()), "Restore fixture controller after value suite");
		}
	}

	private static SubmitNodeStorage.ModelPartSubmit frozenNode(ModelPart arm, PoseStack poses)
	{
		return new SubmitNodeStorage.ModelPartSubmit(poses.last().copy(), arm, 15728880, 0, null, false, false, -1,
				new ModelFeatureRenderer.CrumblingOverlay(0, poses.last().copy()), 0xff123456);
	}

	private static void expectValueFailure(String message, Runnable action)
	{
		try
		{
			action.run();
			throw new AssertionError("Missing expected value failure " + message);
		}
		catch (IllegalStateException failure)
		{
			check(message.equals(failure.getMessage()), "Value failure propagated");
			valueFailures++;
		}
	}

	private record FrozenPart(SubmitNodeStorage.ModelPartSubmit node, List<Vector3f> vertices, List<SavedPart> transforms)
	{
	}

	private static void verifyDeformerBindings(PoseStack poses)
	{
		var mc = Minecraft.getInstance();
		var model = mc.getEntityRenderDispatcher().getPlayerRenderer(mc.player).getModel();
		var part = model.rightArm;
		var id = PLibDatabase.rl("stage52c1_binding");
		var stack = PDeformerStack.compile(List.of(new PDeformerInstance<>(PStretchDeformer.INSTANCE,
				new PStretchDefinition(new Vector3f(), new Vector3f(1, 0, 0), PChannelReference.constant(1)))));
		try (var caller = PModelPartSnapshot.capture(model.root()))
		{
			part.visible = true;
			part.skipDraw = false;
			PPlayerMeshDeformers.register(id, PPlayerPart.RIGHT_ARM, player -> player == mc.player, stack, (player, reference) -> 2);
			PPlayerMeshDeformers.apply(mc.player, model);
			List<Vector3f> firstVertices = geometry(part, poses);
			var first = new SubmitNodeStorage.ModelPartSubmit(poses.last().copy(), part, 15728880, 0, null, false, false, -1,
					new ModelFeatureRenderer.CrumblingOverlay(0, poses.last().copy()), 0xff123456);
			PPlayerMeshDeformers.unregister(id);
			PPlayerMeshDeformers.register(id, PPlayerPart.RIGHT_ARM, player -> player == mc.player, stack, (player, reference) -> 3);
			PPlayerMeshDeformers.apply(mc.player, model);
			List<Vector3f> secondVertices = geometry(part, poses);
			var second = new SubmitNodeStorage.ModelPartSubmit(poses.last().copy(), part, 15728880, 0, null, false, false, -1,
					new ModelFeatureRenderer.CrumblingOverlay(0, poses.last().copy()), 0xff123456);
			PPlayerMeshDeformers.unregister(id);
			PPlayerMeshDeformers.apply(mc.player, model);
			List<Vector3f> reusedVertices = geometry(part, poses);
			check(!firstVertices.equals(secondVertices) && !firstVertices.equals(reusedVertices), "Real deformed geometry differs across bindings and identity");
			List<SavedPart> transforms = save(part);
			var feature = new ModelPartFeatureRenderer();
			geometryMatches(firstVertices, part(feature, first, transforms, null));
			geometryMatches(secondVertices, part(feature, second, transforms, null));
			check(reusedVertices.equals(geometry(part, poses)), "Deferred draws restore caller cube bindings");
			expectDeformerFailure("outer cube", () -> part(feature, first, transforms, () ->
			{
				expectDeformerFailure("inner cube", () -> part(feature, second, transforms, () ->
				{
					throw new IllegalStateException("inner cube");
				}));
				check(firstVertices.equals(geometry(part, poses)), "Nested failed draw restores enclosing cube geometry");
				throw new IllegalStateException("outer cube");
			}));
			check(reusedVertices.equals(geometry(part, poses)), "Failed outer draw restores caller cube bindings");
			geometryMatches(firstVertices, part(feature, first, transforms, null));
			check(reusedVertices.equals(geometry(part, poses)), "Repeated node replay restores caller cube bindings");
			deformerSuites++;
		}
		finally
		{
			PPlayerMeshDeformers.unregister(id);
		}
	}

	private static List<Vector3f> geometry(ModelPart part, PoseStack poses)
	{
		List<Vector3f> vertices = new ArrayList<>();
		part.render(poses, consumer((name, args) ->
		{
			if (name.equals("addVertex"))
				vertices.add(new Vector3f((float)args[0], (float)args[1], (float)args[2]));
			return null;
		}), 15728880, 0, -1);
		check(!vertices.isEmpty(), "Native cube emits geometry");
		return vertices;
	}

	private static void geometryMatches(List<Vector3f> expected, Map<String, List<Vector3f>> passes)
	{
		check(passes.keySet().equals(Set.of("main", "outline", "crumbling")), "Frozen cube geometry reaches all native part passes");
		for (var vertices : passes.values())
		{
			check(vertices.size() == expected.size(), "Native pass preserves cube vertex count");
			for (int index = 0; index < vertices.size(); index++)
				check(vertices.get(index).distanceSquared(expected.get(index)) < 1.0e-10f, "Native pass uses the queued binding's geometry");
		}
	}

	private static void expectDeformerFailure(String message, Runnable action)
	{
		try
		{
			action.run();
			throw new AssertionError("Missing expected cube failure " + message);
		}
		catch (IllegalStateException failure)
		{
			check(message.equals(failure.getMessage()), "Cube failure propagated");
			deformerFailures++;
		}
	}

	private static void verifyItems(PoseStack poses)
	{
		var mc = Minecraft.getInstance();
		var state = new ItemStackRenderState();
		mc.getItemModelResolver().updateForTopItem(state, new ItemStack(Items.STICK), ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, mc.level, mc.player, 0);
		var nodes = new SubmitNodeStorage();
		state.submit(poses, nodes, 15728880, 0, 0);
		var actual = nodes.order(0).getItemSubmits().getFirst();
		int quads = actual.quads().size();
		check(quads > 0, "Actual vanilla stick resolves native item quads");
		state.clear();
		check(actual.quads().size() == quads, "Native queued quads survive real ItemStackRenderState.clear");
		BakedQuad template = actual.quads().getFirst();
		var material = template.materialInfo();
		var tinted = new BakedQuad(template.position0(), template.position1(), template.position2(), template.position3(), template.packedUV0(), template.packedUV1(), template.packedUV2(), template.packedUV3(), template.direction(),
				new BakedQuad.MaterialInfo(material.sprite(), material.layer(), material.itemRenderType(), 0, material.shade(), material.lightEmission()));
		int[] tints = {0xffabcdef};
		List<BakedQuad> mutable = new ArrayList<>(List.of(tinted));
		var outer = new SubmitNodeStorage.ItemSubmit(poses.last().copy(), ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, 0x123456, 0x234567, 0xff123456, tints, mutable, ItemStackRenderState.FoilType.STANDARD);
		tints[0] = 0;
		mutable.clear();
		check(outer.tintLayers()[0] == 0xffabcdef && outer.quads().size() == 1, "Node owns tint values and quad membership");
		var inner = new SubmitNodeStorage.ItemSubmit(poses.last().copy(), ItemDisplayContext.FIRST_PERSON_LEFT_HAND, 0x654321, 0x765432, 0xff654321, new int[] {0xff654321}, List.of(tinted), ItemStackRenderState.FoilType.STANDARD);
		var feature = new ItemFeatureRenderer();
		QuadInstance values = field(feature, "quadInstance");
		for (int vertex = 0; vertex < 4; vertex++)
		{
			values.setColor(vertex, 0xff000001 + vertex);
			values.setLightCoords(vertex, 17 + vertex);
		}
		values.setOverlayCoords(23);
		List<Integer> callerValues = values(values);
		item(feature, outer, null);
		check(callerValues.equals(values(values)), "Normal item restores caller scratch values");
		expect("outer item", () -> item(feature, outer, () ->
		{
			expect("inner item", () -> item(feature, inner, () ->
			{
				throw new IllegalStateException("inner item");
			}));
			itemValues(outer, values);
			throw new IllegalStateException("outer item");
		}));
		check(callerValues.equals(values(values)), "Nested item failure restores caller scratch values");
		item(feature, inner, null);
		item(feature, outer, null);
		check(callerValues.equals(values(values)), "Item draw recovery");
		var layerState = new ItemStackRenderState();
		layerState.newLayer().prepareQuadList().add(template);
		var innerState = new ItemStackRenderState();
		innerState.newLayer().prepareQuadList().add(template);
		var entry = poses.last().copy();
		int depth = depth(poses);
		SubmitNodeCollector innerFailure = collector((name, args) ->
		{
			if (name.equals("submitItem"))
			{
				poses.pushPose();
				poses.pushPose();
				throw new IllegalStateException("inner layer");
			}
			return null;
		});
		SubmitNodeCollector outerFailure = collector((name, args) ->
		{
			if (name.equals("submitItem"))
			{
				var outerPose = poses.last().copy();
				int outerDepth = depth(poses);
				expect("inner layer", () -> innerState.submit(poses, innerFailure, 1, 2, 0));
				stack(poses, outerPose, outerDepth);
				poses.pushPose();
				throw new IllegalStateException("outer layer");
			}
			return null;
		});
		expect("outer layer", () -> layerState.submit(poses, outerFailure, 1, 2, 0));
		stack(poses, entry, depth);
		layerState.submit(poses, nodes, 1, 2, 0);
		stack(poses, entry, depth);
	}

	private static void itemValues(SubmitNodeStorage.ItemSubmit submit, QuadInstance actual)
	{
		for (int vertex = 0; vertex < 4; vertex++)
			check(actual.getColor(vertex) == submit.tintLayers()[0] && actual.getLightCoords(vertex) == submit.lightCoords() && actual.overlayCoords() == submit.overlayCoords(), "Native item captured color/light/overlay, including after nested draw");
	}

	private static List<Integer> values(QuadInstance instance)
	{
		List<Integer> result = new ArrayList<>();
		for (int vertex = 0; vertex < 4; vertex++)
		{
			result.add(instance.getColor(vertex));
			result.add(instance.getLightCoords(vertex));
		}
		result.add(instance.overlayCoords());
		return result;
	}

	private static SubmitNodeStorage.ModelPartSubmit fullNode(SubmitNodeStorage.ModelPartSubmit node)
	{
		return new SubmitNodeStorage.ModelPartSubmit(node.pose().copy(), node.modelPart(), node.lightCoords(), node.overlayCoords(), null, false, false, node.tintedColor(), new ModelFeatureRenderer.CrumblingOverlay(0, node.pose().copy()), 0xff123456);
	}

	private static List<SubmitNodeStorage.ModelPartSubmit> partNodes(SubmitNodeStorage nodes)
	{
		List<SubmitNodeStorage.ModelPartSubmit> result = new ArrayList<>();
		Object storage = nodes.order(0).getModelPartSubmits();
		for (var field : storage.getClass().getDeclaredFields())
			if (Map.class.isAssignableFrom(field.getType()))
				try
				{
					field.setAccessible(true);
					for (Object list : ((Map<?, ?>)field.get(storage)).values())
						for (Object node : (List<?>)list)
							result.add((SubmitNodeStorage.ModelPartSubmit)node);
				}
				catch (ReflectiveOperationException failure)
				{
					throw new AssertionError(failure);
				}
		return result;
	}

	private static Map<String, List<Vector3f>> part(ModelPartFeatureRenderer feature, SubmitNodeStorage.ModelPartSubmit node, List<SavedPart> expected, @Nullable Runnable first)
	{
		PartDraw previous = partDraw;
		PartDraw draw = new PartDraw(expected, first);
		partDraw = draw;
		try
		{
			var buffers = Minecraft.getInstance().renderBuffers();
			invoke(feature, "render", Map.of(RenderTypes.outline(PLibDatabase.rl("textures/stage42a_white.png")), List.of(node)), buffers.bufferSource(), buffers.outlineBufferSource(), buffers.crumblingBufferSource());
			return draw.vertices;
		}
		finally
		{
			partDraw = previous;
		}
	}

	private static void item(ItemFeatureRenderer feature, SubmitNodeStorage.ItemSubmit node, @Nullable Runnable first)
	{
		ItemDraw previous = itemDraw;
		itemDraw = new ItemDraw(node, first);
		try
		{
			var buffers = Minecraft.getInstance().renderBuffers();
			invoke(feature, "renderItem", buffers.bufferSource(), buffers.outlineBufferSource(), node);
		}
		finally
		{
			itemDraw = previous;
		}
	}

	private static void invoke(Object target, String name, Object... args)
	{
		try
		{
			var method = java.util.Arrays.stream(target.getClass().getDeclaredMethods()).filter(candidate -> candidate.getName().equals(name)).findFirst().orElseThrow();
			method.setAccessible(true);
			method.invoke(target, args);
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

	@SuppressWarnings("unchecked")
	private static <T> T field(Object target, String name)
	{
		try
		{
			var field = target.getClass().getDeclaredField(name);
			field.setAccessible(true);
			return (T)field.get(target);
		}
		catch (ReflectiveOperationException failure)
		{
			throw new AssertionError(failure);
		}
	}

	private static VertexConsumer consumer(BiFunction<String, Object[], @Nullable Object> action)
	{
		return (VertexConsumer)Proxy.newProxyInstance(VertexConsumer.class.getClassLoader(), new Class<?>[] {VertexConsumer.class}, (proxy, method, args) ->
		{
			action.apply(method.getName(), args);
			return method.getReturnType() == VertexConsumer.class ? proxy : null;
		});
	}

	private static SubmitNodeCollector collector(BiFunction<String, Object[], @Nullable Object> action)
	{
		return (SubmitNodeCollector)Proxy.newProxyInstance(SubmitNodeCollector.class.getClassLoader(), new Class<?>[] {SubmitNodeCollector.class}, (proxy, method, args) -> method.getName().equals("order") ? proxy : action.apply(method.getName(), args));
	}

	private static int depth(PoseStack poses)
	{
		return ((PoseStackAccessor)poses).pulselib$getDepth();
	}

	private static void stack(PoseStack poses, PoseStack.Pose expected, int depth)
	{
		check(depth(poses) == depth && poses.last().pose().equals(expected.pose(), 0.00001f) && poses.last().normal().equals(expected.normal(), 0.00001f), "Caller pose/normal/depth restored");
	}

	private static List<SavedPart> save(ModelPart root)
	{
		return root.getAllParts().stream().map(SavedPart::new).toList();
	}

	private static void restore(List<SavedPart> parts)
	{
		parts.forEach(SavedPart::restore);
	}

	private static void matches(List<SavedPart> expected, String message)
	{
		check(expected.stream().allMatch(part -> part.equals(new SavedPart(part.part))), message);
	}

	private static void expect(String message, Runnable action)
	{
		try
		{
			action.run();
			throw new AssertionError("Missing expected failure " + message);
		}
		catch (IllegalStateException failure)
		{
			check(message.equals(failure.getMessage()), "Expected failure propagated: " + message);
			failures++;
		}
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
			throw new AssertionError("Stage 5.2b.2b: " + message);
	}

	private static final class PartDraw
	{
		private final List<SavedPart> expected;
		private final @Nullable Runnable firstVertex;
		private final Map<String, List<Vector3f>> vertices = new HashMap<>();
		private boolean fired;

		private PartDraw(List<SavedPart> expected, @Nullable Runnable firstVertex)
		{
			this.expected = expected;
			this.firstVertex = firstVertex;
		}
	}

	private static final class ItemDraw
	{
		private final SubmitNodeStorage.ItemSubmit submit;
		private final @Nullable Runnable firstQuad;
		private boolean fired;

		private ItemDraw(SubmitNodeStorage.ItemSubmit submit, @Nullable Runnable firstQuad)
		{
			this.submit = submit;
			this.firstQuad = firstQuad;
		}
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
