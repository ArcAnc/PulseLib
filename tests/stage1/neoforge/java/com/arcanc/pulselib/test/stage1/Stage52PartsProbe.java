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
import com.arcanc.pulselib.platform.PClientRenderHooks;
import com.arcanc.pulselib.util.PLibDatabase;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
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
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
	private static final Set<String> PART_PASSES = new HashSet<>();
	private static final Set<String> ITEM_PASSES = new HashSet<>();

	private Stage52PartsProbe()
	{
	}

	public static void completed()
	{
		check(suites == 4 && failures == 32 && partVertices > 0 && itemQuads > 0, "Both formats/phases and all nested failure/recovery suites");
		check(PART_PASSES.equals(Set.of("main", "outline", "crumbling")) && ITEM_PASSES.equals(Set.of("main", "outline", "foil")), "All native feature branches observed");
		PLibDatabase.LOGGER.info("STAGE 5.2b.2b PART/ITEM PASS: suites={}, expectedFailures={}, partVertices={}, itemQuads={}, partPasses={}, itemPasses={}, hand/modelPart/item snapshot/nested failure/recovery/reload PASS", suites, failures, partVertices, itemQuads, PART_PASSES, ITEM_PASSES);
	}

	public static @Nullable VertexConsumer partBuffer(String pass)
	{
		PartDraw draw = partDraw;
		if (draw == null)
			return null;
		return consumer((name, args) ->
		{
			if (name.equals("addVertex"))
			{
				partVertices++;
				PART_PASSES.add(pass);
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

	public static void verify()
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

	private static void part(ModelPartFeatureRenderer feature, SubmitNodeStorage.ModelPartSubmit node, List<SavedPart> expected, @Nullable Runnable first)
	{
		PartDraw previous = partDraw;
		partDraw = new PartDraw(expected, first);
		try
		{
			var buffers = Minecraft.getInstance().renderBuffers();
			invoke(feature, "render", Map.of(RenderTypes.outline(PLibDatabase.rl("textures/stage42a_white.png")), List.of(node)), buffers.bufferSource(), buffers.outlineBufferSource(), buffers.crumblingBufferSource());
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
