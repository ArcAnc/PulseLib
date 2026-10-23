/**
 * @author ArcAnc
 * Created at: 23.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1;

import com.arcanc.pulselib.content.animatable.ControllerState;
import com.arcanc.pulselib.content.animatable.PAnimatable;
import com.arcanc.pulselib.content.animatable.PAnimationController;
import com.arcanc.pulselib.content.animatable.instance.InstanceAnimationManager;
import com.arcanc.pulselib.content.model.animation.PAnimation;
import com.arcanc.pulselib.content.model.animation.PAnimationTrack;
import com.arcanc.pulselib.content.model.animation.PBoneAnimation;
import com.arcanc.pulselib.content.model.animation.PInterpolationType;
import com.arcanc.pulselib.content.model.animation.PKeyframe;
import com.arcanc.pulselib.content.model.animation.PPose;
import com.arcanc.pulselib.content.model.animation.PRawAnimation;
import com.arcanc.pulselib.content.model.animation.PVectorConversion;
import com.arcanc.pulselib.content.model.baked.PBakedBone;
import com.arcanc.pulselib.content.model.baked.PBakedMesh;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.model.baked.PMeshRenderContext;
import com.arcanc.pulselib.content.model.deformer.PChannelReference;
import com.arcanc.pulselib.content.model.deformer.PDeformerInstance;
import com.arcanc.pulselib.content.model.deformer.PDeformerStack;
import com.arcanc.pulselib.content.model.deformer.PDeformerValueSource;
import com.arcanc.pulselib.content.model.deformer.PHingeDeformer;
import com.arcanc.pulselib.content.model.deformer.PHingeDefinition;
import com.arcanc.pulselib.content.model.deformer.PMeshDeformation;
import com.arcanc.pulselib.content.model.textures.PAlphaMode;
import com.arcanc.pulselib.content.registration.PLibRegistration;
import com.arcanc.pulselib.content.registration.block.block_entity.TestBlockEntity;
import com.arcanc.pulselib.content.registration.block.block_entity.ber.TestBlockEntityRenderer;
import com.arcanc.pulselib.content.registration.block.block_entity.ber.renderState.TestBlockEntityRenderState;
import com.arcanc.pulselib.content.registration.entity.TestEntity;
import com.arcanc.pulselib.content.registration.entity.renderer.TestEntityRender;
import com.arcanc.pulselib.content.registration.item.TestBlockItem;
import com.arcanc.pulselib.content.registration.item.renderer.TestBlockItemRenderer;
import com.arcanc.pulselib.content.registration.item.renderer.renderState.TestBlockItemRenderState;
import com.arcanc.pulselib.content.renderer.PBlockRenderer;
import com.arcanc.pulselib.content.renderer.PEntityRenderer;
import com.arcanc.pulselib.content.renderer.PEntityRenderLayer;
import com.arcanc.pulselib.content.renderer.PItemRenderer;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.base.PEntityRenderState;
import com.arcanc.pulselib.content.renderer.gl.PGlRenderHost;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.content.renderer.plan.PFrameCompiler;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.content.renderer.plan.PRenderExecutor;
import com.arcanc.pulselib.content.renderer.plan.PRenderPlan;
import com.arcanc.pulselib.data.gecko.MolangParser;
import com.arcanc.pulselib.data.gecko.PMolangVectorValue;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.PResourceCache;
import com.arcanc.pulselib.util.helpers.PLibHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.arcanc.pulselib.test.stage1.Stage41bGeometryProbe.field;

/** Uses actual owners, extracted states, inherited materials, compiled plans and native GPU queries. */
public final class Stage41b3RendererProbe
{
	public static EntityRendererProvider.@Nullable Context entityContext;
	private static final List<Expected> EXPECTED = new ArrayList<>();
	private static int pending;
	private static int passes;

	private Stage41b3RendererProbe()
	{
	}

	public static void register()
	{
		NeoForge.EVENT_BUS.addListener(Stage41b3RendererProbe::render);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Stage41b3RendererProbe::shutdown);
	}

	public static void tick(int tick)
	{
		if (tick == 100 || tick == 230)
			pending = tick;
	}

	private static void render(RenderLevelStageEvent.AfterOpaqueBlocks event)
	{
		if (pending == 0)
			return;
		var level = Minecraft.getInstance().level;
		check(level != null && entityContext != null, "Real client level and renderer context required");
		PBakedMesh mesh = TestBlockEntityRenderer.MODEL_DATA.getModel().bones().stream().flatMap(bone -> bone.meshes().stream()).findFirst().orElseThrow();
		var block = new TestBlockEntity(new BlockPos(-1, 80, 8), Stage1Content.BlockReg.TEST_BLOCK.get().defaultBlockState());
		block.setLevel(level);
		var entity = new TestEntity(Stage1Content.EntityTypeReg.TEST_ENTITY.get(), level);
		var stack = new ItemStack(Stage1Content.ItemReg.TEST_ITEM.get());
		var blockTrace = new Trace("block", 1, mesh);
		var entityTrace = new Trace("entity", 2, mesh);
		var itemTrace = new Trace("item", 3, mesh);
		var layerTrace = new Trace("layer", 4, mesh);
		var blockRenderer = new BlockProbe(blockTrace);
		var layerRenderer = new LayerProbe(layerTrace);
		var entityRenderer = new EntityProbe(entityTrace, layerRenderer);
		var itemRenderer = new ItemProbe(itemTrace);
		var blockController = controller(block, blockTrace.model);
		var entityController = controller(entity, entityTrace.model);
		var item = (TestBlockItem)stack.getItem();
		var itemController = controller(item, stack, itemTrace.model);
		var blockState = blockRenderer.createRenderState();
		var camera = event.getLevelRenderState().cameraRenderState;
		blockRenderer.extractRenderState(block, blockState, 0.5f, camera.pos, null);
		check(blockState.facing() == Direction.EAST && blockRenderer.facings == 1, "Canonical facing hook must execute during extraction");
		var entityState = entityRenderer.createRenderState();
		entityRenderer.extractRenderState(entity, entityState, 0.5f);
		var sleeping = entityRenderer.createRenderState();
		sleeping.pose = net.minecraft.world.entity.Pose.SLEEPING;
		sleeping.bedOrientation = Direction.EAST;
		sleeping.eyeHeight = 1.7f;
		sleeping.scale = 1;
		sleeping.extractHeadData(19, -8);
		PoseStack sleepingPose = new PoseStack();
		var head = entityRenderer.position(sleeping, sleepingPose);
		check(head.headYaw() == 19 && head.headPitch() == -8 && Math.abs(sleepingPose.last().pose().m30() + 1.6f) < 0.00001f,
				"Captured sleeping translation and canonical HeadRotation must match the state");
		var upside = new TestEntity(Stage1Content.EntityTypeReg.TEST_ENTITY.get(), level);
		upside.setCustomName(net.minecraft.network.chat.Component.literal("Dinnerbone"));
		upside.yHeadRotO = upside.yHeadRot = 30;
		upside.yBodyRotO = upside.yBodyRot = 0;
		var upsideState = entityRenderer.createRenderState();
		entityRenderer.extractRenderState(upside, upsideState, 0.5f);
		check(Math.abs(upsideState.getHeadYRot() + 30) < 0.00001f, "Upside-down yaw must be captured after its canonical sign correction");
		var itemState = itemRenderer.extractArgument(stack);
		itemState.extractItemRenderState(new ItemStackRenderState());
		var collector = new SubmitNodeStorage();
		PoseStack poses = new PoseStack();
		poses.translate(-camera.pos.x, 85 - camera.pos.y, 15 - camera.pos.z);
		Matrix4f original = new Matrix4f(poses.last().pose());

		blockTrace.fail = true;
		try
		{
			blockRenderer.submit(blockState, poses, collector, camera);
			throw new AssertionError("Owner hook failure must propagate");
		}
		catch (IllegalArgumentException expected)
		{
			check(expected.getMessage().equals("stage41b3 leaf failure"), "Unexpected renderer error");
		}
		check(poses.last().pose().equals(original), "Native submit must restore caller pose after a nested owner hook failure");
		Object scope = field(PBlockRenderer.class, blockRenderer, "submission");
		check(field(scope.getClass(), scope, "current") == null, "Failed canonical callback must release the captured state");
		executeOpaque();
		EXPECTED.clear();
		blockTrace.reset();
		resetCounter(blockController);
		resetCounter(entityController);
		resetCounter(itemController);
		blockRenderer.submit(blockState, poses, collector, camera);
		entityRenderer.submit(entityState, poses, collector, camera);
		itemRenderer.submit(itemState, poses, collector, 0xF000F0, 0, false, 0);
		check(poses.last().pose().equals(original), "All native renderer entries must restore caller pose");
		for (Trace trace : List.of(blockTrace, entityTrace, itemTrace, layerTrace))
		{
			check(trace.contexts.size() == 4 && trace.meshes == 4, trace.name + " must traverse all four bones exactly once");
			if (!trace.name.equals("layer"))
				check(trace.nativeMeshes == 4 && trace.submits == 4 && trace.traversals == 4, trace.name + " state and canonical owner overrides must both remain active");
		}
		check(layerRenderer.modelSelections == 1, "Layer getModel override must select one snapshot for all its bones");
		check(layerTrace.rootPose.equals(entityTrace.leafPose, 0.0001f), "Forward-bound layer root must use the final entity leaf pose");
		check(counter(blockController) == 4 && counter(itemController) == 4 && counter(entityController) == 8,
				"Stateful Molang must evaluate once per model (four bones), including a separate layer model");
		int primitives = executeOpaque();
		check(EXPECTED.size() == 16, "All model/layer mesh overrides must reach the compiler");
		PLibDatabase.LOGGER.info("STAGE 4.1b.3 RENDERER PASS: tick={}, contexts=16, owner/state/submit/traversal hooks, sibling isolation, deferred forward-bound layer, Molang=4/4/8, GPU primitives={}, exception pose/state recovery, sleeping/head snapshots, layer modelSelections=1", pending, primitives);
		InstanceAnimationManager.removeManager(block);
		InstanceAnimationManager.removeManager(entity);
		EXPECTED.clear();
		passes++;
		pending = 0;
	}

	private static <T extends PAnimatable<T>> PAnimationController<T> controller(T owner, PBakedModel model)
	{
		var manager = PLibHelper.getManager(owner);
		return configure(owner, model, manager.getControllers());
	}

	private static PAnimationController<TestBlockItem> controller(TestBlockItem item, ItemStack stack, PBakedModel model)
	{
		var manager = PLibHelper.getManager(item, com.arcanc.pulselib.content.animatable.AnimManagerKey.of(stack));
		return configure(item, model, manager.getControllers());
	}

	private static <T extends PAnimatable<T>> PAnimationController<T> configure(T owner, PBakedModel model, Map<String, PAnimationController<T>> controllers)
	{
		controllers.clear();
		var controller = new PAnimationController<T>(state -> ControllerState.PLAY);
		controller.play(PRawAnimation.begin().thenLoop("probe").build());
		controller.tick(owner, 1, model);
		controllers.put("probe", controller);
		resetCounter(controller);
		return controller;
	}

	private static void resetCounter(PAnimationController<?> controller)
	{
		MolangParser.evaluate("v.stage_count = 0", controller.persistentMolangContext());
	}

	private static float counter(PAnimationController<?> controller)
	{
		return MolangParser.evaluate("v.stage_count", controller.persistentMolangContext());
	}

	@SuppressWarnings("unchecked")
	private static int executeOpaque()
	{
		var compiler = (PFrameCompiler<PRenderQueue.RenderStage>)field(PRenderQueue.class, null, "COMPILER");
		var executor = (PRenderExecutor)field(PRenderQueue.class, null, "EXECUTOR");
		List<PRenderPlan> plans = List.of(compiler.compileOpaque(PRenderQueue.RenderStage.SOLID_BLOCKS),
				compiler.compileOpaque(PRenderQueue.RenderStage.ENTITIES), compiler.compileOpaque(PRenderQueue.RenderStage.TRANSLUCENT_BLOCKS));
		List<PInstanceHeader> headers = plans.stream().flatMap(plan -> plan.instances().stream()).toList();
		for (Expected expected : EXPECTED)
			check(headers.stream().filter(header -> header.packedColor() == expected.context.color() && header.transform().equals(expected.pose, 0.0001f) &&
					header.packedLight() == (expected.context.emissive() ? 0xF000F0 : expected.context.packedLight()) &&
					header.packedOverlay() == expected.context.packedOverlay() && header.auxiliaryOperationCount() == 1).count() == 1,
					"Inherited material, matrix and GPU deformer values must reach the actual packed plan exactly once");
		int expected = plans.stream().flatMap(plan -> plan.groups().stream()).mapToInt(group -> group.command().indexCount() / 3 * group.command().instanceCount()).sum();
		int query = GL15.glGenQueries();
		try
		{
			GL15.glBeginQuery(GL30.GL_PRIMITIVES_GENERATED, query);
			try
			{
				for (PRenderPlan plan : plans)
					executor.execute(plan, new PGlRenderHost().captureFrame());
			}
			finally
			{
				GL15.glEndQuery(GL30.GL_PRIMITIVES_GENERATED);
			}
			int actual = GL15.glGetQueryObjecti(query, GL15.GL_QUERY_RESULT);
			check(actual == expected && actual > 0, "Native instanced GPU draws must match all compiled opaque geometry");
			return actual;
		}
		finally
		{
			GL15.glDeleteQueries(query);
		}
	}

	private static PBakedModel tree(PBakedMesh mesh)
	{
		List<PBakedBone> roots = new ArrayList<>();
		PBakedBone root = new PBakedBone("root", new Vector3f(), new Quaternionf(), new ArrayList<>(), null, List.of(mesh));
		PBakedBone branch = new PBakedBone("branch", new Vector3f(1, 0, 0), new Quaternionf(), new ArrayList<>(), root, List.of(mesh));
		PBakedBone leaf = new PBakedBone("leaf", new Vector3f(0, 2, 0), new Quaternionf(), List.of(), branch, List.of(mesh));
		branch.children().add(leaf);
		root.children().add(branch);
		root.children().add(new PBakedBone("sibling", new Vector3f(-1, 0, 0), new Quaternionf(), List.of(), root, List.of(mesh)));
		roots.add(root);
		var value = new PMolangVectorValue(MolangParser.parse("v.stage_count = (v.stage_count ?? 0) + 1; return 1;"), MolangParser.parse("1"), MolangParser.parse("1"), PVectorConversion.IDENTITY);
		var track = new PAnimationTrack<>(PLibRegistration.AnimationChannelReg.SCALE, List.of(new PKeyframe<>(0, value, value, PInterpolationType.LINEAR)));
		Map<String, PBoneAnimation> tracks = new HashMap<>();
		for (String name : List.of("root", "branch", "leaf", "sibling"))
			tracks.put(name, new PBoneAnimation(UUID.randomUUID(), Map.of("scale", track)));
		return new PBakedModel(roots, Map.of("probe", new PAnimation("probe", 20, tracks)));
	}

	private static final class Trace
	{
		private final String name;
		private final int id;
		private final PBakedModel model;
		private final PMeshDeformation deformation;
		private final Map<String, PMeshRenderContext> contexts = new HashMap<>();
		private Matrix4f transform = new Matrix4f();
		private Matrix4f rootPose = new Matrix4f();
		private Matrix4f leafPose = new Matrix4f();
		private int meshes;
		private int nativeMeshes;
		private int submits;
		private int traversals;
		private boolean fail;

		private Trace(String name, int id, PBakedMesh mesh)
		{
			this.name = name;
			this.id = id;
			this.model = tree(mesh);
			this.deformation = new PMeshDeformation(PDeformerStack.compile(List.of(new PDeformerInstance<>(PHingeDeformer.INSTANCE,
					new PHingeDefinition(new Vector3f(), new Vector3f(0, 1, 0), new Vector3f(1, 0, 0), new PChannelReference<>("", 0.2f))))), PDeformerValueSource.DEFAULTS, this, 1);
		}

		private void reset()
		{
			this.contexts.clear();
			this.meshes = this.nativeMeshes = this.submits = this.traversals = 0;
			this.fail = false;
		}

		private PMeshRenderContext bone(PBakedBone bone, PMeshRenderContext inherited)
		{
			if (this.fail && bone.name().equals("leaf"))
				throw new IllegalArgumentException("stage41b3 leaf failure");
			if (bone.parent() != null)
				check(inherited == this.contexts.get(bone.parent().name()), this.name + " child must inherit its parent's full immutable context, without mesh/sibling pollution");
			PMeshRenderContext context = inherited;
			if (bone.name().equals("root"))
				context = inherited.withColor(0xFF123456).withPackedLight(0x00700070).withPackedOverlay(3 << 16 | 2).
						withTexture(TestBlockEntityRenderer.TORUS).withEmissive(true).withAlphaMode(PAlphaMode.OPAQUE).withDeformation(this.deformation);
			else if (bone.name().equals("branch"))
				context = inherited.withColor(0xFF654321).withTexture(TestBlockEntityRenderer.CUBE).withEmissive(false).withAlphaMode(PAlphaMode.CUTOUT);
			this.contexts.put(bone.name(), context);
			return context;
		}

		private void before(PBakedBone bone, PoseStack poses)
		{
			this.submits++;
			this.transform = new Matrix4f(poses.last().pose());
			if (bone.name().equals("root"))
				this.rootPose = new Matrix4f(this.transform);
			if (bone.name().equals("leaf"))
				this.leafPose = new Matrix4f(this.transform);
		}

		private PMeshRenderContext mesh(PBakedBone bone, PMeshRenderContext inherited)
		{
			check(inherited == this.contexts.get(bone.name()), this.name + " mesh must receive the selected bone context");
			check(inherited.deformation() == this.deformation, "Per-owner deformation key must propagate unchanged");
			int index = List.of("root", "branch", "leaf", "sibling").indexOf(bone.name());
			var result = inherited.withColor(0xFF000000 | this.id << 16 | index + 1);
			EXPECTED.add(new Expected(new Matrix4f(this.transform), result));
			this.meshes++;
			return result;
		}
	}

	private record Expected(Matrix4f pose, PMeshRenderContext context)
	{
	}

	private static final class BlockProbe extends PBlockRenderer<TestBlockEntity, TestBlockEntityRenderState>
	{
		private final Trace trace;
		private int facings;

		private BlockProbe(Trace trace)
		{
			super(TestBlockEntityRenderer.MODEL_DATA, PRenderTypes.RenderTypeProvider::trianglesSolid);
			this.trace = trace;
		}

		@Override
		public TestBlockEntityRenderState createRenderState()
		{
			return new TestBlockEntityRenderState();
		}

		@Override
		public PBakedModel getModel(TestBlockEntity owner)
		{
			return this.trace.model;
		}

		@Override
		protected Direction getAnimatableFacing(TestBlockEntity owner)
		{
			this.facings++;
			return Direction.EAST;
		}

		@Override
		protected PMeshRenderContext resolveBoneRender(TestBlockEntity owner, PBakedBone bone, PMeshRenderContext inherited, float partial)
		{
			return this.trace.bone(bone, inherited);
		}

		@Override
		protected PMeshRenderContext resolveMeshRender(TestBlockEntity owner, PBakedBone bone, PBakedMesh mesh, PMeshRenderContext inherited, float partial)
		{
			return this.trace.mesh(bone, inherited);
		}

		@Override
		protected PMeshRenderContext resolveMeshRender(TestBlockEntityRenderState state, PBakedBone bone, PBakedMesh mesh, PMeshRenderContext inherited)
		{
			this.trace.nativeMeshes++;
			return super.resolveMeshRender(state, bone, mesh, inherited);
		}

		@Override
		protected void submitBone(TestBlockEntity owner, PBakedBone bone, PoseStack poses, PModelData data,
		                          Collection<PAnimationController<TestBlockEntity>> controllers, PMeshRenderContext inherited, float partial)
		{
			this.trace.before(bone, poses);
			super.submitBone(owner, bone, poses, data, controllers, inherited, partial);
		}

		@Override
		protected void perBoneSubmit(TestBlockEntity owner, PoseStack poses, PBakedBone bone, PPose pose,
		                             Collection<PAnimationController<TestBlockEntity>> controllers, Map<PAnimationController<TestBlockEntity>, MolangParser.Context> contexts, PMeshRenderContext inherited, float partial)
		{
			this.trace.traversals++;
			super.perBoneSubmit(owner, poses, bone, pose, controllers, contexts, inherited, partial);
		}
	}

	private static final class ItemProbe extends PItemRenderer<TestBlockItem, TestBlockItemRenderState>
	{
		private final Trace trace;

		private ItemProbe(Trace trace)
		{
			super(TestBlockItemRenderer.MODEL_DATA, PRenderTypes.RenderTypeProvider::trianglesSolid,
					Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
			this.trace = trace;
		}

		@Override
		public TestBlockItemRenderState createRenderState()
		{
			return new TestBlockItemRenderState();
		}

		@Override
		public PBakedModel getModel(TestBlockItem owner)
		{
			return this.trace.model;
		}

		@Override
		protected PMeshRenderContext resolveBoneRender(TestBlockItem owner, ItemStack stack, ItemDisplayContext display, PBakedBone bone, PMeshRenderContext inherited, float partial)
		{
			return this.trace.bone(bone, inherited);
		}

		@Override
		protected PMeshRenderContext resolveMeshRender(TestBlockItem owner, ItemStack stack, ItemDisplayContext display, PBakedBone bone, PBakedMesh mesh, PMeshRenderContext inherited, float partial)
		{
			return this.trace.mesh(bone, inherited);
		}

		@Override
		protected PMeshRenderContext resolveMeshRender(TestBlockItemRenderState state, ItemDisplayContext display, PBakedBone bone, PBakedMesh mesh, PMeshRenderContext inherited)
		{
			this.trace.nativeMeshes++;
			return super.resolveMeshRender(state, display, bone, mesh, inherited);
		}

		@Override
		protected void submitBone(TestBlockItem owner, ItemStack stack, PBakedBone bone, PoseStack poses, PModelData data,
		                          Collection<PAnimationController<TestBlockItem>> controllers, PMeshRenderContext inherited, float partial, ItemDisplayContext display)
		{
			this.trace.before(bone, poses);
			super.submitBone(owner, stack, bone, poses, data, controllers, inherited, partial, display);
		}

		@Override
		protected void perBoneSubmit(TestBlockItem owner, ItemStack stack, PoseStack poses, PBakedBone bone, PPose pose,
		                             Collection<PAnimationController<TestBlockItem>> controllers, Map<PAnimationController<TestBlockItem>, MolangParser.Context> contexts, PMeshRenderContext inherited, float partial, ItemDisplayContext display)
		{
			this.trace.traversals++;
			super.perBoneSubmit(owner, stack, poses, bone, pose, controllers, contexts, inherited, partial, display);
		}
	}

	private static final class EntityProbe extends PEntityRenderer<TestEntity, PEntityRenderState.LivingImpl<TestEntity>>
	{
		private final Trace trace;

		private EntityProbe(Trace trace, LayerProbe layer)
		{
			super(entityContext, TestEntityRender.MODEL_DATA, PRenderTypes.RenderTypeProvider::trianglesSolid);
			this.trace = trace;
			addRenderLayer("root", layer.bindBone("root", "leaf"));
		}

		private HeadRotation position(PEntityRenderState.LivingImpl<TestEntity> state, PoseStack poses)
		{
			return applyPositioning(state, poses);
		}

		@Override
		public PEntityRenderState.LivingImpl<TestEntity> createRenderState()
		{
			return PLibHelper.livingRenderState();
		}

		@Override
		public PBakedModel getModel(TestEntity owner)
		{
			return this.trace.model;
		}

		@Override
		protected PMeshRenderContext resolveBoneRender(TestEntity owner, PBakedBone bone, PMeshRenderContext inherited, float partial)
		{
			return this.trace.bone(bone, inherited);
		}

		@Override
		protected PMeshRenderContext resolveMeshRender(TestEntity owner, PBakedBone bone, PBakedMesh mesh, PMeshRenderContext inherited, float partial)
		{
			return this.trace.mesh(bone, inherited);
		}

		@Override
		protected PMeshRenderContext resolveMeshRender(PEntityRenderState.LivingImpl<TestEntity> state, PBakedBone bone, PBakedMesh mesh, PMeshRenderContext inherited)
		{
			this.trace.nativeMeshes++;
			return super.resolveMeshRender(state, bone, mesh, inherited);
		}

		@Override
		protected void submitBone(TestEntity owner, PBakedBone bone, PoseStack poses, PModelData data,
		                          Collection<PAnimationController<TestEntity>> controllers, PMeshRenderContext inherited, float partial)
		{
			this.trace.before(bone, poses);
			super.submitBone(owner, bone, poses, data, controllers, inherited, partial);
		}

		@Override
		protected void submitBone(TestEntity owner, PBakedBone bone, PoseStack poses, PModelData data,
		                          Collection<PAnimationController<TestEntity>> controllers, PMeshRenderContext inherited, float partial,
		                          @Nullable PEntityRenderLayer<TestEntity, PEntityRenderState.LivingImpl<TestEntity>> layer)
		{
			if (layer instanceof LayerProbe probe)
				probe.trace.before(bone, poses);
			super.submitBone(owner, bone, poses, data, controllers, inherited, partial, layer);
		}

		@Override
		protected void perBoneSubmit(TestEntity owner, PoseStack poses, PBakedBone bone, PPose pose, Collection<PAnimationController<TestEntity>> controllers,
		                             PModelData data, PMeshRenderContext inherited, float partial, @Nullable HeadRotation head,
		                             @Nullable PEntityRenderLayer<TestEntity, PEntityRenderState.LivingImpl<TestEntity>> layer,
		                             @Nullable Map<String, Matrix4f> entityPoses, @Nullable Matrix4f transform, @Nullable List<DeferredLayerSubmit> deferred,
		                             Map<PAnimationController<TestEntity>, MolangParser.Context> contexts)
		{
			if (layer == null)
				this.trace.traversals++;
			super.perBoneSubmit(owner, poses, bone, pose, controllers, data, inherited, partial, head, layer, entityPoses, transform, deferred, contexts);
		}
	}

	private static final class LayerProbe extends PEntityRenderLayer<TestEntity, PEntityRenderState.LivingImpl<TestEntity>>
	{
		private final Trace trace;
		private int modelSelections;

		private LayerProbe(Trace trace)
		{
			super(TestEntityRender.MODEL_DATA, PRenderTypes.RenderTypeProvider::trianglesSolid);
			this.trace = trace;
		}

		@Override
		public PBakedModel getModel(TestEntity owner)
		{
			this.modelSelections++;
			return this.trace.model;
		}

		@Override
		public PMeshRenderContext resolveBoneRender(TestEntity owner, PBakedBone bone, PMeshRenderContext inherited, float partial)
		{
			return this.trace.bone(bone, inherited);
		}

		@Override
		public PMeshRenderContext resolveMeshRender(TestEntity owner, PBakedBone bone, PBakedMesh mesh, PMeshRenderContext inherited, float partial)
		{
			// The entity's canonical layer submit overload uses the pose already bound to the forward reference.
			return this.trace.mesh(bone, inherited);
		}
	}

	private static void shutdown(GameShuttingDownEvent event)
	{
		check(passes == 2, "Both real renderer inheritance probes must complete before shutdown");
		PLibDatabase.LOGGER.info("STAGE 4.1b.3 RENDERER SHUTDOWN PASS: two complete probe passes, existing GPU/manager cleanup checks retained");
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
			throw new AssertionError(message);
	}
}
