/**
 * @author ArcAnc
 * Created at: 28.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage62deformation;

import com.arcanc.pulselib.content.mixin.PoseStackAccessor;
import com.arcanc.pulselib.content.model.deformer.PChannelReference;
import com.arcanc.pulselib.content.model.deformer.PDeformerInstance;
import com.arcanc.pulselib.content.model.deformer.PDeformerStack;
import com.arcanc.pulselib.content.model.deformer.PStretchDefinition;
import com.arcanc.pulselib.content.model.deformer.PStretchDeformer;
import com.arcanc.pulselib.content.player.animation.PPlayerPart;
import com.arcanc.pulselib.content.player.deformer.PPlayerGeometryRenderState;
import com.arcanc.pulselib.content.player.deformer.PPlayerMeshDeformers;
import com.arcanc.pulselib.content.player.deformer.PPlayerModelGeometry;
import com.arcanc.pulselib.content.player.deformer.PPlayerModelSubmitState;
import com.arcanc.pulselib.content.renderer.base.PModelPartSnapshot;
import com.arcanc.pulselib.platform.PClientRenderHooks;
import com.arcanc.pulselib.util.PLibDatabase;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelPartFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Native geometry oracle adapted from the verified 5.2b.2c.2b fixture; no production buffers are changed outside isolated draws. */
public final class NativeParityProbe
{
	private static final List<EquipmentSlot> SLOTS = List.of(EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.HEAD);
	private static final List<Item> ITEMS = List.of(Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS, Items.DIAMOND_HELMET);
	public static int suites;
	public static int failures;
	public static int armorNodes;
	public static int handNodes;
	public static int vertices;
	public static int nativeArmCalls;
	public static int reads;
	public static List<Vector3f> portable = List.of();
	private static @Nullable Draw current;
	private static boolean alive;

	private NativeParityProbe()
	{
	}

	public static @Nullable VertexConsumer buffer(String pass)
	{
		return current == null ? null : consumer(current.passes.computeIfAbsent(pass, ignored -> new ArrayList<>()), current.first);
	}

	public static void verify()
	{
		var mc = Minecraft.getInstance();
		var renderer = mc.getEntityRenderDispatcher().getPlayerRenderer(mc.player);
		var model = renderer.getModel();
		var other = new RemotePlayer(mc.level, new GameProfile(new UUID(0, 0x62d2b0 + suites), "ParityOther"));
		other.setId(0x62d2b0 + suites);
		other.setPos(30000, 100, 30000);
		mc.level.addEntity(other);
		var saved = new EnumMap<EquipmentSlot, ItemStack>(EquipmentSlot.class);
		SLOTS.forEach(slot -> saved.put(slot, mc.player.getItemBySlot(slot)));
		for (int i = 0; i < SLOTS.size(); i++)
		{
			var stack = new ItemStack(ITEMS.get(i));
			stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
			mc.player.setItemSlot(SLOTS.get(i), stack);
			other.setItemSlot(SLOTS.get(i), stack.copy());
		}
		var deformation = PDeformerStack.compile(List.of(new PDeformerInstance<>(PStretchDeformer.INSTANCE,
				new PStretchDefinition(new Vector3f(), new Vector3f(0, 1, 0), PChannelReference.constant(1)))));
		alive = true;
		List<Identifier> bindings = new ArrayList<>();
		for (var part : List.of(PPlayerPart.BODY, PPlayerPart.RIGHT_ARM, PPlayerPart.LEFT_ARM))
		{
			var binding = PLibDatabase.rl("stage62d2b/" + part.name().toLowerCase(java.util.Locale.ROOT));
			bindings.add(binding);
			PPlayerMeshDeformers.register(binding, part, owner -> owner == mc.player || owner == other, deformation, (owner, channel) ->
			{
				check(alive, "No value query after extraction/hand submission");
				reads++;
				return owner == other ? 2.25f : 1.5f;
			});
		}
		try (var caller = PModelPartSnapshot.capture(model.root()))
		{
			var first = oracle(mc.player, renderer.createRenderState(mc.player, 0.65f), model, armorLayers(renderer));
			var second = oracle(other, renderer.createRenderState(other, 0.65f), model, armorLayers(renderer));
			verifyHands(model);
			alive = false;
			bindings.forEach(PPlayerMeshDeformers::unregister);
			execute(first);
			execute(second);
			verifySubmitFailure(first, second);
			check(reads > 0 && !portable.isEmpty(), "Real deformer values and captured arm geometry");
			suites++;
			PLibDatabase.LOGGER.info("STAGE62D2B NATIVE PASS: suites={}, armorNodes={}, handNodes={}, nativeArmCalls={}, vertexComparisons={}, valueReads={}, failures={}", suites, armorNodes, handNodes, nativeArmCalls, vertices, reads, failures);
		}
		finally
		{
			alive = true;
			bindings.forEach(PPlayerMeshDeformers::unregister);
			saved.forEach(mc.player::setItemSlot);
			mc.level.removeEntity(other.getId(), Entity.RemovalReason.DISCARDED);
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static Pending oracle(Player player, AvatarRenderState state, PlayerModel model, List<HumanoidArmorLayer> layers)
	{
		Map<PPlayerModelGeometry.Entry, List<Vector3f>> expected = new IdentityHashMap<>();
		var data = ((PPlayerGeometryRenderState)state).pulselib$getPlayerGeometry();
		check(data != null, "Native owner geometry captured");
		expected.put(data.main(model), immediate(player, state, model));
		for (HumanoidArmorLayer layer : layers)
			for (EquipmentSlot slot : SLOTS)
			{
				var original = (Model<?>)invoke(layer, "getArmorModel", state, slot);
				Object key = field(layer, "pulselib$geometryKey");
				var entry = data.armor(key, slot);
				check(entry != null && entry.original() == original, "Native four-slot armor geometry entries");
				expected.put(entry, immediate(player, state, entry.model()));
			}
		check(expected.size() == 5, "Main and four armor entries");
		state.id = Integer.MAX_VALUE;
		state.outlineColor = 0xff123456;
		return new Pending(state, model, layers, expected);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static List<Vector3f> immediate(Player owner, AvatarRenderState state, Model<?> model)
	{
		var access = (PPlayerGeometryRenderState)state;
		var data = access.pulselib$getPlayerGeometry();
		boolean main = data != null && data.main(model) != null;
		access.pulselib$setPlayerGeometry(null);
		try (var caller = PModelPartSnapshot.capture(model.root()))
		{
			((Model)model).setupAnim(state);
			List<Vector3f> plain = geometry(model, new PoseStack());
			if (model instanceof PlayerModel player)
				PPlayerMeshDeformers.apply(owner, player, 0.65f);
			else if (model instanceof HumanoidModel<?> humanoid)
				PPlayerMeshDeformers.apply(owner, humanoid, 0.65f);
			List<Vector3f> deformed = geometry(model, new PoseStack());
			if (deformed.size() == plain.size() && !plain.isEmpty() && main)
				check(java.util.stream.IntStream.range(0, plain.size()).anyMatch(i -> plain.get(i).distanceSquared(deformed.get(i)) > 0.0001f), "Deformer changes actual model vertices");
			return deformed;
		}
		finally
		{
			access.pulselib$setPlayerGeometry(data);
		}
	}

	private record Hand(SubmitNodeStorage.ModelPartSubmit node, RenderType type, List<Vector3f> expected)
	{
	}

	@SuppressWarnings("unchecked")
	private static void verifyHands(PlayerModel model)
	{
		var mc = Minecraft.getInstance();
		var renderer = mc.getEntityRenderDispatcher().getPlayerRenderer(mc.player);
		List<Hand> hands = new ArrayList<>();
		for (boolean right : List.of(true, false))
			for (boolean sleeve : List.of(true, false))
				try (var caller = PModelPartSnapshot.capture(model.root()))
				{
					ModelPart part = right ? model.rightArm : model.leftArm;
					part.resetPose();
					part.visible = true;
					model.leftArm.zRot = -0.1f;
					model.rightArm.zRot = 0.1f;
					model.rightSleeve.visible = model.leftSleeve.visible = sleeve;
					PPlayerMeshDeformers.apply(mc.player, model, 0.65f);
					List<Vector3f> expected = new ArrayList<>();
					part.render(new PoseStack(), consumer(expected, null), 15728880, 0, -1);
					caller.restore();
					var nodes = new SubmitNodeStorage();
					PClientRenderHooks.renderArm(renderer, new PoseStack(), nodes, 15728880, mc.player.getSkin().body().texturePath(), sleeve, mc.player, right);
					Map<RenderType, List<SubmitNodeStorage.ModelPartSubmit>> queued = (Map<RenderType, List<SubmitNodeStorage.ModelPartSubmit>>)field(nodes.order(0).getModelPartSubmits(), "translucentModelPartSubmits");
					check(queued.values().stream().mapToInt(List::size).sum() == 1, "Exactly one actual native hand node");
					queued.forEach((type, list) -> hands.add(new Hand(list.getFirst(), type, expected)));
				}
		// Execute the real ItemInHandRenderer call sites too: Fabric has 5 arguments, NeoForge 6.
		try (var caller = PModelPartSnapshot.capture(model.root()))
		{
			var nativeNodes = new SubmitNodeStorage();
			for (var arm : List.of(HumanoidArm.RIGHT, HumanoidArm.LEFT))
			{
				invoke(mc.gameRenderer.itemInHandRenderer, "renderPlayerArm", new PoseStack(), nativeNodes, 15728880, 0f, 0f, arm);
				nativeArmCalls++;
			}
			int count = 0;
			for (var submits : nativeNodes.getSubmitsPerOrder().values())
				count += ((Map<?, List<?>>)field(submits.getModelPartSubmits(), "translucentModelPartSubmits")).values().stream().mapToInt(List::size).sum();
			check(count == 2, "Both actual ItemInHandRenderer arm bridge call sites reach native nodes");
		}
		alive = false;
		var buffers = mc.renderBuffers();
		for (var hand : hands)
		{
			model.rightArm.x = model.leftArm.x = 300;
			model.rightSleeve.visible = model.leftSleeve.visible = !model.rightSleeve.visible;
			var before = poses(model);
			Draw previous = current;
			current = new Draw(null);
			try
			{
				invoke(new ModelPartFeatureRenderer(), "render", Map.of(hand.type, List.of(hand.node)), buffers.bufferSource(), buffers.outlineBufferSource(), buffers.crumblingBufferSource());
				match(hand.expected, current.passes.get("main"));
				if (hand == hands.getFirst())
					portable = current.passes.get("main").stream().map(Vector3f::new).toList();
				check(before.equals(poses(model)), "Hand feature restores shared model after frozen replay");
				handNodes++;
			}
			finally
			{
				current = previous;
			}
		}
		alive = true;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void execute(Pending pending)
	{
		var nodes = new SubmitNodeStorage();
		var poses = new PoseStack();
		for (HumanoidArmorLayer layer : pending.layers)
			layer.submit(poses, nodes, 15728880, pending.state, 0, 0);
		var main = new SubmitNodeStorage.ModelSubmit(poses.last().copy(), pending.main, pending.state, 15728880, 0, -1, null, pending.state.outlineColor, null);
		check(((PPlayerGeometryRenderState)pending.state).pulselib$getSubmittingGeometry() == null, "Armor submission context is restored");
		verifyNode(main, RenderTypes.entityCutout(PLibDatabase.rl("textures/stage62d2b_white.png")), pending.expected);
		var queued = nodes(nodes);
		check(queued.size() == 8, "Four actual vanilla armor textures plus four foil submissions");
		for (Queued node : queued)
		{
			verifyNode(node.node, node.type, pending.expected);
			armorNodes++;
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void verifyNode(SubmitNodeStorage.ModelSubmit<?> node, RenderType type, Map<PPlayerModelGeometry.Entry, List<Vector3f>> expected)
	{
		var entry = ((PPlayerModelSubmitState)(Object)node).pulselib$getModelGeometry();
		check(entry != null && expected.containsKey(entry), "Every native node owns the correct owner/slot entry");
		var feature = new ModelFeatureRenderer();
		var draw = render(feature, node, type, null);
		match(expected.get(entry), draw.passes.get("main"));
		if (type.outline().isPresent() || type.isOutline())
			match(expected.get(entry), draw.passes.get("outline"));
		// A native armor RenderType normally disables crumbling. Exercise that feature branch separately.
		var access = (PPlayerGeometryRenderState)node.state();
		var previous = access.pulselib$getSubmittingGeometry();
		access.pulselib$setSubmittingGeometry(entry);
		SubmitNodeStorage.ModelSubmit<?> crumbling;
		try
		{
			crumbling = new SubmitNodeStorage.ModelSubmit(node.pose(), node.model(), node.state(), node.lightCoords(), node.overlayCoords(), node.tintedColor(), null, 0,
					new ModelFeatureRenderer.CrumblingOverlay(0, node.pose().copy()));
		}
		finally
		{
			access.pulselib$setSubmittingGeometry(previous);
		}
		var breaking = render(feature, crumbling, RenderTypes.entitySolid(PLibDatabase.rl("textures/stage62d2b_white.png")), null);
		match(expected.get(entry), breaking.passes.get("crumbling"));
		var caller = poses(node.model());
		expect("outer geometry", () -> render(feature, node, type, () ->
		{
			expect("inner geometry", () -> render(feature, crumbling, RenderTypes.entitySolid(PLibDatabase.rl("textures/stage62d2b_white.png")), () ->
			{
				throw new IllegalStateException("inner geometry");
			}));
			throw new IllegalStateException("outer geometry");
		}));
		check(caller.equals(poses(node.model())), "Failed deferred armor restores caller poses");
		match(expected.get(entry), render(feature, node, type, null).passes.get("main"));
	}

	private static void verifySubmitFailure(Pending first, Pending second)
	{
		var poses = new PoseStack();
		var entryPose = poses.last().copy();
		int depth = ((PoseStackAccessor)poses).pulselib$getDepth();
		SubmitNodeCollector inner = collector(() ->
		{
			poses.pushPose();
			throw new IllegalStateException("inner armor submit");
		});
		SubmitNodeCollector outer = collector(() ->
		{
			expect("inner armor submit", () -> invoke(second.layers.getFirst(), "submit", poses, inner, 15728880, second.state, 0f, 0f));
			check(((PPlayerGeometryRenderState)first.state).pulselib$getSubmittingGeometry() != null, "Nested owner submit restores outer entry");
			poses.pushPose();
			throw new IllegalStateException("outer armor submit");
		});
		expect("outer armor submit", () -> invoke(first.layers.getFirst(), "submit", poses, outer, 15728880, first.state, 0f, 0f));
		check(((PPlayerGeometryRenderState)first.state).pulselib$getSubmittingGeometry() == null &&
				((PPlayerGeometryRenderState)second.state).pulselib$getSubmittingGeometry() == null &&
				((PoseStackAccessor)poses).pulselib$getDepth() == depth && poses.last().pose().equals(entryPose.pose()), "Failed nested armor submit restores context and stack");
	}

	private static Draw render(ModelFeatureRenderer feature, SubmitNodeStorage.ModelSubmit<?> node, RenderType type, @Nullable Runnable first)
	{
		Draw previous = current;
		var draw = new Draw(first);
		current = draw;
		try
		{
			var buffers = Minecraft.getInstance().renderBuffers();
			invoke(feature, "renderModel", node, type, buffer("main"), buffers.outlineBufferSource(), buffers.crumblingBufferSource());
			return draw;
		}
		finally
		{
			current = previous;
		}
	}

	private static VertexConsumer consumer(List<Vector3f> vertices, @Nullable Runnable first)
	{
		boolean[] fired = {false};
		return (VertexConsumer)Proxy.newProxyInstance(VertexConsumer.class.getClassLoader(), new Class<?>[] {VertexConsumer.class}, (proxy, method, args) ->
		{
			if (method.getName().equals("addVertex"))
			{
				vertices.add(new Vector3f((float)args[0], (float)args[1], (float)args[2]));
				if (first != null && !fired[0])
				{
					fired[0] = true;
					first.run();
				}
			}
			return method.getReturnType() == VertexConsumer.class ? proxy : null;
		});
	}

	private static List<Vector3f> geometry(Model<?> model, PoseStack poses)
	{
		List<Vector3f> vertices = new ArrayList<>();
		model.renderToBuffer(poses, consumer(vertices, null), 15728880, 0, -1);
		return vertices;
	}

	private static void match(List<Vector3f> expected, @Nullable List<Vector3f> actual)
	{
		check(actual != null && !actual.isEmpty() && expected.size() == actual.size(), "Native geometry count matches immediate oracle: " + expected.size() + "/" + (actual == null ? "missing" : actual.size()));
		vertices += expected.size();
		for (int i = 0; i < expected.size(); i++)
			check(expected.get(i).distanceSquared(actual.get(i)) < 1.0e-10f, "Native geometry matches immediate canonical pose/deformation at " + i + ": " + expected.get(i) + "/" + actual.get(i));
	}

	@SuppressWarnings("unchecked")
	private static List<HumanoidArmorLayer> armorLayers(Object renderer)
	{
		return ((List<?>)field(renderer, "layers")).stream().filter(HumanoidArmorLayer.class::isInstance).map(HumanoidArmorLayer.class::cast).toList();
	}

	@SuppressWarnings("unchecked")
	private static List<Queued> nodes(SubmitNodeStorage storage)
	{
		List<Queued> result = new ArrayList<>();
		for (var collection : storage.getSubmitsPerOrder().values())
		{
			Object models = collection.getModelSubmits();
			Map<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>> solid = (Map<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>>)field(models, "solidModelSubmits");
			solid.forEach((type, list) -> list.forEach(node -> result.add(new Queued(node, type))));
			List<SubmitNodeStorage.TranslucentModelSubmit<?>> translucent = (List<SubmitNodeStorage.TranslucentModelSubmit<?>>)field(models, "translucentModelSubmits");
			translucent.forEach(node -> result.add(new Queued(node.modelSubmit(), node.renderType())));
		}
		return result;
	}

	private static List<String> poses(Model<?> model)
	{
		return model.root().getAllParts().stream().map(part -> part.storePose() + ":" + part.xScale + ":" + part.yScale + ":" + part.zScale + ":" + part.visible + ":" + part.skipDraw).toList();
	}

	private static SubmitNodeCollector collector(Runnable action)
	{
		return (SubmitNodeCollector)Proxy.newProxyInstance(SubmitNodeCollector.class.getClassLoader(), new Class<?>[] {SubmitNodeCollector.class}, (proxy, method, args) ->
		{
			action.run();
			return null;
		});
	}

	static Object field(Object object, String name)
	{
		for (Class<?> type = object.getClass(); type != null; type = type.getSuperclass())
			try
			{
				var field = type.getDeclaredField(name);
				field.setAccessible(true);
				return field.get(object);
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

	private static void expect(String message, Runnable action)
	{
		try
		{
			action.run();
			throw new AssertionError("Missing expected " + message);
		}
		catch (IllegalStateException exception)
		{
			check(message.equals(exception.getMessage()), "Expected failure propagated");
			failures++;
		}
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
			throw new AssertionError("STAGE62D2B native: " + message);
	}

	private record Pending(AvatarRenderState state, PlayerModel main, List<HumanoidArmorLayer> layers,
			Map<PPlayerModelGeometry.Entry, List<Vector3f>> expected)
	{
	}

	private record Queued(SubmitNodeStorage.ModelSubmit<?> node, RenderType type)
	{
	}

	private static final class Draw
	{
		private final Map<String, List<Vector3f>> passes = new java.util.HashMap<>();
		private final @Nullable Runnable first;

		private Draw(@Nullable Runnable first)
		{
			this.first = first;
		}
	}
}
