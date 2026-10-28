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
import com.arcanc.pulselib.content.model.animation.PRawAnimation;
import com.arcanc.pulselib.content.model.deformer.PChannelReference;
import com.arcanc.pulselib.content.model.deformer.PDeformerInstance;
import com.arcanc.pulselib.content.model.deformer.PDeformerStack;
import com.arcanc.pulselib.content.model.deformer.PStretchDeformer;
import com.arcanc.pulselib.content.model.deformer.PStretchDefinition;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationDefinition;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import com.arcanc.pulselib.content.player.animation.PPlayerPart;
import com.arcanc.pulselib.content.player.animation.firstPerson.PFirstPersonRenderContexts;
import com.arcanc.pulselib.content.player.deformer.PPlayerGeometryRenderState;
import com.arcanc.pulselib.content.player.deformer.PPlayerMeshDeformers;
import com.arcanc.pulselib.content.player.deformer.PPlayerModelGeometry;
import com.arcanc.pulselib.content.player.deformer.PPlayerModelSubmitState;
import com.arcanc.pulselib.content.renderer.base.PModelPartSnapshot;
import com.arcanc.pulselib.content.renderer.base.PPlayerRigRenderState;
import com.arcanc.pulselib.platform.PClientRenderHooks;
import com.arcanc.pulselib.util.PLibDatabase;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelPartFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Native armor selection, per-node geometry, first-person arms and actual world armor GPU draws. */
public final class Stage52ArmorProbe
{
	private static final List<EquipmentSlot> SLOTS = List.of(EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.HEAD);
	private static final List<Item> ITEMS = List.of(Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS, Items.DIAMOND_HELMET);
	private static final List<Pending> PENDING = new ArrayList<>();
	private static final Set<RenderType> WORLD_TYPES = Collections.newSetFromMap(new IdentityHashMap<>());
	private static final int WITNESS = 0x525b01;
	private static final Map<Integer, String> WORLD_MODELS = new java.util.HashMap<>();
	private static final int[] GPU_DRAWS = new int[2];
	private static final int[] GPU_PRIMITIVES = new int[2];
	private static boolean selecting;
	private static boolean choiceAlive;
	private static float slotBias;
	private static int choices;
	private static int suites;
	private static int failures;
	private static int reloadSubmissions;
	private static int armorNodes;
	private static int handNodes;
	private static int phase;
	private static @Nullable Model<?> shared;
	private static @Nullable Draw current;

	private Stage52ArmorProbe()
	{
	}

	public static void register(IEventBus bus)
	{
		bus.addListener((RegisterClientExtensionsEvent event) -> event.registerItem(new IClientItemExtensions()
		{
			@Override
			public Model<?> getGenericArmorModel(ItemStack stack, EquipmentClientInfo.LayerType type, Model original)
			{
				if (!selecting)
					return original;
				check(choiceAlive, "Armor extension must only run during extraction/oracle");
				choices++;
				slotBias = stack.is(Items.DIAMOND_LEGGINGS) ? 0.25f : 0;
				if (stack.is(Items.DIAMOND_CHESTPLATE) || stack.is(Items.DIAMOND_LEGGINGS))
				{
					if (shared == null)
						shared = new HumanoidModel<AvatarRenderState>(original.root());
					return shared;
				}
				return original;
			}
		}, ITEMS.toArray(Item[]::new)));
	}

	public static void afterReload()
	{
		phase = 1;
	}

	public static void world(SubmitNodeStorage.ModelSubmit<?> node, RenderType type)
	{
		if (current == null && node.state() instanceof AvatarRenderState avatar)
			WORLD_MODELS.put(avatar.id, node.model().getClass().getName());
		if (current == null && node.state() instanceof AvatarRenderState state && state.id == WITNESS &&
				((PPlayerModelSubmitState)(Object)node).pulselib$getModelGeometry() != null && PPlayerModelGeometry.main(state, node.model()) == null)
			WORLD_TYPES.add(type);
	}

	public static void gpu(RenderType type, MeshData mesh, Runnable draw)
	{
		if (!WORLD_TYPES.contains(type) || GL15.glGetQueryi(GL30.GL_PRIMITIVES_GENERATED, GL15.GL_CURRENT_QUERY) != 0)
		{
			draw.run();
			return;
		}
		int query = GL15.glGenQueries();
		GL15.glBeginQuery(GL30.GL_PRIMITIVES_GENERATED, query);
		try
		{
			draw.run();
		}
		finally
		{
			GL15.glEndQuery(GL30.GL_PRIMITIVES_GENERATED);
			GPU_PRIMITIVES[phase] += GL15.glGetQueryObjecti(query, GL15.GL_QUERY_RESULT);
			GPU_DRAWS[phase]++;
			GL15.glDeleteQueries(query);
		}
	}

	public static @Nullable VertexConsumer buffer(String pass)
	{
		Draw draw = current;
		return draw == null ? null : consumer(draw.passes.computeIfAbsent(pass, ignored -> new ArrayList<>()), draw.first);
	}

	public static void verify(Identifier definition, String clip)
	{
		var mc = Minecraft.getInstance();
		var renderer = mc.getEntityRenderDispatcher().getPlayerRenderer(mc.player);
		var model = renderer.getModel();
		if (suites == 2)
		{
			for (Pending pending : PENDING)
			{
				execute(pending);
				reloadSubmissions++;
			}
			PENDING.clear();
		}
		var other = new RemotePlayer(mc.level, new GameProfile(new UUID(0, 0x525cb0 + suites), "ArmorOther"));
		other.setId(0x525cb0 + suites);
		mc.level.addEntity(other);
		var savedEquipment = new EnumMap<EquipmentSlot, ItemStack>(EquipmentSlot.class);
		SLOTS.forEach(slot -> savedEquipment.put(slot, mc.player.getItemBySlot(slot)));
		for (int i = 0; i < SLOTS.size(); i++)
		{
			var stack = new ItemStack(ITEMS.get(i));
			stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
			mc.player.setItemSlot(SLOTS.get(i), stack);
			other.setItemSlot(SLOTS.get(i), stack.copy());
		}
		var binding = PLibDatabase.rl("stage52c2b/registered");
		var animation = PLibDatabase.rl("stage52c2b/animation/" + suites);
		var deformation = PDeformerStack.compile(List.of(new PDeformerInstance<>(PStretchDeformer.INSTANCE,
				new PStretchDefinition(new Vector3f(), new Vector3f(0, 1, 0), PChannelReference.constant(1)))));
		boolean[] enabled = {true}, alive = {true}, failing = {false}, nesting = {false};
		int[] reads = {0}, modernReads = {0};
		AvatarRenderState[] nested = {null};
		PPlayerAnimations.register(animation, PPlayerAnimationDefinition.builder(PPlayerAnimations.get(definition).modelData()).
				when(player -> enabled[0] && (player == mc.player || player == other)).
				bind(PPlayerPart.RIGHT_ARM, "arm").
				crossfade(0, com.arcanc.pulselib.content.model.animation.PPoseEasing.LINEAR, com.arcanc.pulselib.content.model.animation.PTransitionInterruptionPolicy.FROM_CURRENT).
				controllers(registrar -> registrar.add("shape", () -> state -> com.arcanc.pulselib.content.animatable.ControllerState.PLAY)).
				deform(PPlayerPart.RIGHT_ARM, deformation, (context, reference) ->
				{
					check(alive[0], "Modern deformer context must not be queried after extraction");
					modernReads[0]++;
					return 1.5f + slotBias;
				}).build());
		PPlayerMeshDeformers.register(binding, PPlayerPart.BODY, player -> player == mc.player || player == other, deformation, (player, reference) ->
		{
			check(alive[0], "Registered values must not be queried after extraction");
			if (failing[0])
				throw new IllegalStateException("geometry extraction");
			reads[0]++;
			if (nesting[0] && player == mc.player)
			{
				nesting[0] = false;
				float previous = slotBias;
				nested[0] = renderer.createRenderState(other, 0.65f);
				slotBias = previous;
			}
			return (player == other ? 3 : 2) + slotBias;
		});
		selecting = choiceAlive = true;
		shared = null;
		try (var caller = PModelPartSnapshot.capture(model.root()))
		{
			for (Player player : List.of(mc.player, other))
			{
				check(PPlayerAnimations.getHandle(player, animation).play("shape", PRawAnimation.begin().thenLoop(clip).build()), "Modern shape controller starts");
				check(PPlayerAnimations.getHandle(player, definition).play("motion", PRawAnimation.begin().thenLoop(clip).build()), "Owner rig controller starts");
			}
			PPlayerAnimations.tick(mc.level);
			PPlayerAnimations.tick(mc.level);
			var beforeFailure = poses(model);
			failing[0] = true;
			expect("geometry extraction", () -> renderer.createRenderState(mc.player, 0.65f));
			failing[0] = false;
			check(beforeFailure.equals(poses(model)), "Failed extraction restores shared model poses");
			slotBias = 0;
			nesting[0] = true;
			var state = renderer.createRenderState(mc.player, 0.65f);
			check(nested[0] != null, "A value callback performs nested extraction of another owner");
			var layers = armorLayers(renderer);
			var first = oracle(mc.player, state, model, layers, reads, modernReads);
			var second = oracle(other, nested[0], model, layers, reads, modernReads);
			verifyHands(definition, animation, model, alive);
			alive[0] = false;
			choiceAlive = false;
			for (Player player : List.of(mc.player, other))
			{
				PPlayerAnimations.getHandle(player, definition).stopAll();
				PPlayerAnimations.getHandle(player, animation).stopAll();
			}
			int beforeReplay = reads[0] + modernReads[0], beforeChoices = choices;
			execute(first);
			execute(second);
			verifySubmitFailure(first, second);
			check(reads[0] + modernReads[0] == beforeReplay && choices == beforeChoices, "Submit and replay never query live values or model extensions");
			if (suites < 2)
				PENDING.add(first);
			PLibDatabase.LOGGER.info("STAGE 5.2b.2c.2b ARMOR CASE PASS: suite={}, registeredReads={}, modernReads={}, choices={}, armorNodes={}, handNodes={}, reloadSubmissions={}", suites, reads[0], modernReads[0], choices, armorNodes, handNodes, reloadSubmissions);
			suites++;
		}
		finally
		{
			selecting = false;
			enabled[0] = false;
			PPlayerMeshDeformers.unregister(binding);
			savedEquipment.forEach(mc.player::setItemSlot);
			check(PPlayerAnimations.getHandle(mc.player, definition).play("motion", PRawAnimation.begin().thenLoop(clip).build()), "Restore main fixture controller");
			PPlayerAnimations.getHandle(other, definition).stopAll();
			PPlayerAnimations.getHandle(other, animation).stopAll();
			PPlayerAnimations.getHandle(mc.player, animation).stopAll();
			mc.level.removeEntity(other.getId(), Entity.RemovalReason.DISCARDED);
			PPlayerAnimations.tick(mc.level);
			PPlayerAnimations.tick(mc.level);
		}
		witness();
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static Pending oracle(Player player, AvatarRenderState state, PlayerModel model, List<HumanoidArmorLayer> layers, int[] reads, int[] modern)
	{
		Map<PPlayerModelGeometry.Entry, List<Vector3f>> expected = new IdentityHashMap<>();
		var data = ((PPlayerGeometryRenderState)state).pulselib$getPlayerGeometry();
		check(data != null, "Per-owner geometry is captured in the render state");
		slotBias = 0;
		expected.put(data.main(model), immediate(player, state, model, model));
		for (HumanoidArmorLayer layer : layers)
			for (EquipmentSlot slot : SLOTS)
			{
				var original = (Model<?>)invoke(layer, "getArmorModel", state, slot);
				ItemStack stack = player.getItemBySlot(slot);
				var type = slot == EquipmentSlot.LEGS ? EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS : EquipmentClientInfo.LayerType.HUMANOID;
				var selected = IClientItemExtensions.of(stack).getGenericArmorModel(stack, type, original);
				Object key = field(layer, "pulselib$geometryKey");
				var entry = data.armor(key, slot);
				check(entry != null && entry.model() == selected, "Native extension selection captured for each armor slot");
				expected.put(entry, immediate(player, state, selected, model));
			}
		check(expected.size() == 5, "Main and four armor entries: " + expected.size());
		check(modern[0] > 0 && reads[0] > 0, "Modern/registered deformer reads: " + modern[0] + "/" + reads[0]);
		state.id = Integer.MAX_VALUE;
		state.outlineColor = 0xff123456;
		return new Pending(state, model, layers, expected);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static List<Vector3f> immediate(Player owner, AvatarRenderState state, Model<?> model, PlayerModel playerModel)
	{
		var access = (PPlayerGeometryRenderState)state;
		var data = access.pulselib$getPlayerGeometry();
		var rig = ((PPlayerRigRenderState)state).pulselib$getPlayerRig();
		access.pulselib$setPlayerGeometry(null);
		((PPlayerRigRenderState)state).pulselib$setPlayerRig(null);
		try (var first = PModelPartSnapshot.capture(playerModel.root()); var second = PModelPartSnapshot.capture(model.root()))
		{
			((Model)model).setupAnim(state);
			if (model instanceof HumanoidModel humanoid)
			{
				copyPose(humanoid, playerModel);
				PPlayerAnimations.apply(owner, playerModel, 0.65f, PPlayerAnimations.allParts());
				copyPose(playerModel, humanoid);
				PPlayerMeshDeformers.apply(owner, humanoid, 0.65f);
			}
			return geometry(model, new PoseStack());
		}
		finally
		{
			access.pulselib$setPlayerGeometry(data);
			((PPlayerRigRenderState)state).pulselib$setPlayerRig(rig);
		}
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
		verifyNode(main, RenderTypes.entityCutout(PLibDatabase.rl("textures/stage42a_white.png")), pending.expected);
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
		var breaking = render(feature, crumbling, RenderTypes.entitySolid(PLibDatabase.rl("textures/stage42a_white.png")), null);
		match(expected.get(entry), breaking.passes.get("crumbling"));
		var caller = poses(node.model());
		expect("outer geometry", () -> render(feature, node, type, () ->
		{
			expect("inner geometry", () -> render(feature, crumbling, RenderTypes.entitySolid(PLibDatabase.rl("textures/stage42a_white.png")), () ->
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

	private static void verifyHands(Identifier definition, Identifier animation, PlayerModel model, boolean[] alive)
	{
		var mc = Minecraft.getInstance();
		var poses = new PoseStack();
		try (var caller = PModelPartSnapshot.capture(model.root()))
		{
			model.rightArm.resetPose();
			model.rightArm.visible = true;
			model.rightSleeve.visible = true;
			model.rightArm.zRot = 0.1f;
			PPlayerMeshDeformers.apply(mc.player, model, 0.65f);
			List<Vector3f> expected = new ArrayList<>();
			model.rightArm.render(poses, consumer(expected, null), 15728880, 0, -1);
			caller.restore();
			var nodes = new SubmitNodeStorage();
			PFirstPersonRenderContexts.beginPass(mc.player, 0.65f);
			try
			{
				PClientRenderHooks.renderArm(mc.getEntityRenderDispatcher().getPlayerRenderer(mc.player), poses, nodes, 15728880,
						mc.player.getSkin().body().texturePath(), true, mc.player, true);
			}
			finally
			{
				PFirstPersonRenderContexts.endPass();
			}
			alive[0] = false;
			PPlayerAnimations.getHandle(mc.player, definition).stopAll();
			PPlayerAnimations.getHandle(mc.player, animation).stopAll();
			Draw previous = current;
			current = new Draw(null);
			try
			{
				new ModelPartFeatureRenderer().renderTranslucent(nodes.order(0),
						mc.renderBuffers().bufferSource(), mc.renderBuffers().outlineBufferSource(), mc.renderBuffers().crumblingBufferSource());
				match(expected, current.passes.get("main"));
				handNodes++;
			}
			finally
			{
				current = previous;
			}
		}
	}

	public static void completed()
	{
		check(suites == 4 && reloadSubmissions == 2 && armorNodes >= 64 && handNodes == 4 && PENDING.isEmpty(), "Complete owner/armor/first-person/reload matrix");
		check(GPU_DRAWS[0] > 0 && GPU_DRAWS[1] > 0 && GPU_PRIMITIVES[0] > 0 && GPU_PRIMITIVES[1] > 0, "Actual native armor GPU draws before and after reload: types=" + WORLD_TYPES.size() + ", draws=" + GPU_DRAWS[0] + "/" + GPU_DRAWS[1] + ", primitives=" + GPU_PRIMITIVES[0] + "/" + GPU_PRIMITIVES[1] + ", worldModels=" + WORLD_MODELS);
		PPlayerMeshDeformers.unregister(PLibDatabase.rl("stage52c2b/world"));
		PLibDatabase.LOGGER.info("STAGE 5.2b.2c.2b ARMOR PASS: suites={}, armorNodes={}, handNodes={}, failures={}, reloadSubmissions={}, GPUdraws={}/{}, GPUprimitives={}/{}", suites, armorNodes, handNodes, failures, reloadSubmissions, GPU_DRAWS[0], GPU_DRAWS[1], GPU_PRIMITIVES[0], GPU_PRIMITIVES[1]);
	}

	private static void witness()
	{
		var mc = Minecraft.getInstance();
		if (mc.level.getEntity(WITNESS) instanceof Player equipped && !equipped.getItemBySlot(EquipmentSlot.CHEST).isEmpty())
			return;
		var player = (Player)mc.level.getEntity(WITNESS);
		check(player != null, "The native camera observer is available for armor GPU coverage");
		for (int i = 0; i < SLOTS.size(); i++)
			player.setItemSlot(SLOTS.get(i), new ItemStack(ITEMS.get(i)));
		var stack = PDeformerStack.compile(List.of(new PDeformerInstance<>(PStretchDeformer.INSTANCE,
				new PStretchDefinition(new Vector3f(), new Vector3f(0, 1, 0), PChannelReference.constant(1)))));
		PPlayerMeshDeformers.register(PLibDatabase.rl("stage52c2b/world"), PPlayerPart.BODY, owner -> owner == player, stack, (owner, reference) -> 1.2f);
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

	private static void copyPose(HumanoidModel<?> from, HumanoidModel<?> to)
	{
		var source = List.of(from.head, from.body, from.rightArm, from.leftArm, from.rightLeg, from.leftLeg);
		var target = List.of(to.head, to.body, to.rightArm, to.leftArm, to.rightLeg, to.leftLeg);
		for (int i = 0; i < source.size(); i++)
			target.get(i).loadPose(source.get(i).storePose());
	}

	private static SubmitNodeCollector collector(Runnable action)
	{
		return (SubmitNodeCollector)Proxy.newProxyInstance(SubmitNodeCollector.class.getClassLoader(), new Class<?>[] {SubmitNodeCollector.class}, (proxy, method, args) ->
		{
			action.run();
			return null;
		});
	}

	private static Object field(Object object, String name)
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
			throw new AssertionError("Stage 5.2b.2c.2b armor: " + message);
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
