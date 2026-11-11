/**
 * @author ArcAnc
 * Created at: 28.02.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer;


import com.arcanc.pulselib.content.animatable.PAnimatable;
import com.arcanc.pulselib.content.animatable.PAnimationController;
import com.arcanc.pulselib.content.animatable.PAnimationManager;
import com.arcanc.pulselib.content.mixin.ItemStackRenderStateAccessor;
import com.arcanc.pulselib.content.model.animation.PPose;
import com.arcanc.pulselib.content.model.animation.PAnimationEvaluationContext;
import com.arcanc.pulselib.content.model.baked.PBakedBone;
import com.arcanc.pulselib.content.model.baked.PBakedMesh;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.model.baked.PMeshRenderContext;
import com.arcanc.pulselib.content.model.baked.PMeshRenderMaterial;
import com.arcanc.pulselib.content.renderer.base.PItemRenderState;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.data.PModelLoader;
import com.arcanc.pulselib.data.gecko.MolangParser;
import com.arcanc.pulselib.data.gltf.PGltfModelLoader;
import com.arcanc.pulselib.util.PModelCache;
import com.arcanc.pulselib.util.PResourceCache;
import com.arcanc.pulselib.util.helpers.PLibHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Renders item.
 */
public abstract class PItemRenderer<T extends Item & PAnimatable<T>, RS extends PItemRenderState<T>> implements SpecialModelRenderer<RS>, PRenderer<T, RS>
{
	private final PModelData modelData;
	private final PRenderStateScope<RS> submission = new PRenderStateScope<>();
	private final Function<Identifier, RenderType> renderType;
	
	/**
	 * Creates an instance of the enclosing type.
	 * @param modelData the model data to use.
	 * @param renderType the render type to use.
	 */
	public PItemRenderer(PModelData modelData, Function<Identifier, RenderType> renderType)
	{
		this.modelData = modelData;
		this.renderType = renderType;
	}

	/**
	 * Canonical constructor adapter. Minecraft's special renderer no longer owns a BEWLR dispatcher/model set.
	 * The model selection and render pipeline initialize the same renderer as the native constructor.
	 */
	public PItemRenderer(PModelData data, Function<Identifier, RenderType> renderType,
	                     BlockEntityRenderDispatcher blockEntityRenderDispatcher, EntityModelSet entityModelSet)
	{
		this(data, renderType);
	}
	
	/**
	 * Creates the render state.
	 * @return the value produced by this operation.
	 */
	protected abstract RS createRenderState();
	
	/**
	 * Preserves the canonical reload listener name without restoring BEWLR ownership.
	 * @return the simple name of this renderer's runtime class.
	 */
	public String getName()
	{
		return this.getClass().getSimpleName();
	}

	/**
	 * Performs the submit operation.
	 * @param renderState the render state to use.
	 * @param poseStack the pose stack to use.
	 * @param submitNodeCollector the submit node collector to use.
	 * @param lightCoords the light coords to use.
	 * @param overlayCoords the overlay coords to use.
	 * @param hasFoil the has foil to use.
	 * @param outlineColor the outline color to use.
	 */
	@Override
	public void submit(@Nullable RS renderState,
	                   PoseStack poseStack,
	                   SubmitNodeCollector submitNodeCollector,
	                   int lightCoords,
	                   int overlayCoords,
	                   boolean hasFoil,
	                   int outlineColor)
	{
		if (renderState == null)
			return;
		boolean gui = ((ItemStackRenderStateAccessor)renderState.itemRenderState()).pulselib$getDisplayContext() == ItemDisplayContext.GUI;
		try (var scope = gui && PRenderContexts.localScope() == null ? PRenderContexts.enter(PRenderContexts.Context.GUI) : PRenderContexts.enterEntity())
		{
			try
			{
				renderState.extractAdditionalData(lightCoords,  overlayCoords, hasFoil, outlineColor);
				poseStack.pushPose();
				try
				{
					PModelData modelData = getModelData(renderState);
					PModelLoader modelLoader = PModelCache.getModelLoader(modelData.getModelLoaderId()).orElse(PGltfModelLoader.INSTANCE);
					modelLoader.applyItemTransform(poseStack);
		
					CameraRenderState cameraRenderState = new CameraRenderState();
					preSubmit(poseStack, renderState, cameraRenderState, submitNodeCollector);
					trueSubmit(poseStack, renderState, cameraRenderState, submitNodeCollector);
					postSubmit(poseStack, renderState, cameraRenderState, submitNodeCollector);
				}
				finally
				{
					poseStack.popPose();
				}
			}
			catch (RuntimeException | Error failure)
			{
				if (scope != null)
					scope.discard();
				throw failure;
			}
		}
	}
	
	/**
	 * Returns the extents.
	 * @param output the output to use.
	 */
	@Override
	public void getExtents(Consumer<Vector3fc> output)
	{
		output.accept(new Vector3f());
	}
	
	/**
	 * Extracts the argument.
	 * @param stack the stack to use.
	 * @return the value produced by this operation.
	 */
	@Override
	public @Nullable RS extractArgument(ItemStack stack)
	{
		RS state = createRenderState();
		state.extractStackData(stack, this);
		return state;
	}
	
	/**
	 * Returns the model data.
	 * @param renderState the render state to use.
	 * @return the value produced by this operation.
	 */
	@Override
	public PModelData getModelData(RS renderState)
	{
		return getModelData(renderState.getAnimatable());
	}
	
	/** Selects the canonical model data for an owner during extraction and submission. */
	@Override
	/**
	 * {@inheritDoc}
	 */
	public PModelData getModelData(T animatable)
	{
		return this.modelData;
	}

	/** Returns the baked model selected for the supplied owner, or null before resource load. */
	@Override
	/**
	 * {@inheritDoc}
	 */
	public @Nullable PBakedModel getModel(T animatable)
	{
		return getModelData(animatable).getModel();
	}

	/**
	 * Returns the model.
	 * @param renderState the render state to use.
	 * @return the value produced by this operation.
	 */
	@Override
	public @Nullable PBakedModel getModel(RS renderState)
	{
		return getModel(renderState.getAnimatable());
	}
	
	/**
	 * Returns the render type.
	 * @param texture the texture to use.
	 * @return the value produced by this operation.
	 */
	@Override
	public RenderType getRenderType(Identifier texture)
	{
		return this.renderType.apply(texture);
	}
	
	/**
	 * Performs the pre submit operation.
	 * @param poseStack the pose stack to use.
	 * @param renderState the render state to use.
	 * @param cameraRenderState the camera render state to use.
	 * @param submitNodeCollector the submit node collector to use.
	 */
	@Override
	public void preSubmit(PoseStack poseStack, RS renderState, CameraRenderState cameraRenderState, SubmitNodeCollector submitNodeCollector)
	{
	}
	
	/**
	 * Performs the true submit operation.
	 * @param poseStack the pose stack to use.
	 * @param renderState the render state to use.
	 * @param cameraRenderState the camera render state to use.
	 * @param submitNodeCollector the submit node collector to use.
	 */
	@Override
	public void trueSubmit(PoseStack poseStack, RS renderState, CameraRenderState cameraRenderState, SubmitNodeCollector submitNodeCollector)
	{
		this.submission.run(renderState, () ->
		{
			PBakedModel model = renderState.getBakedModel();
			if (model == null)
				return;
		
			PAnimationManager<T> manager = PLibHelper.getManager(renderState.getAnimatable(), renderState.getAnimKey());
			manager.bindModel(model);
		
			Collection<PAnimationController<T>> controllers = manager.getControllers().values();
			Map<PAnimationController<T>, MolangParser.Context> molangContexts = prepareMolangContexts(
					renderState.getAnimatable(), manager, controllers, renderState.partialTick());
		
			PPose pose = model.evaluate(controllers, molangContexts, renderState.partialTick());
			ItemDisplayContext context = ((ItemStackRenderStateAccessor)renderState.itemRenderState()).pulselib$getDisplayContext();
			model.bones().forEach(bone -> perBoneSubmit(renderState.getAnimatable(), renderState.stack(), poseStack, bone, pose, controllers, molangContexts, this::getRenderType, -1, renderState.lightCoords(), renderState.overlayCoords(), renderState.partialTick(), context));
		});
	}
	
	/**
	 * Performs the post submit operation.
	 * @param poseStack the pose stack to use.
	 * @param renderState the render state to use.
	 * @param cameraRenderState the camera render state to use.
	 * @param submitNodeCollector the submit node collector to use.
	 */
	@Override
	public void postSubmit(PoseStack poseStack, RS renderState, CameraRenderState cameraRenderState, SubmitNodeCollector submitNodeCollector)
	{
	}
	
	/**
	 * Submits the selected bone and its descendants using the sampled pose and inherited mesh render context.
	 *
	 * @param animatable the owner whose animation state is being processed
	 * @param stack the item stack supplying render or animation state
	 * @param poseStack the current model-to-render pose stack
	 * @param bone the baked bone being sampled or submitted
	 * @param pose the indexed bone pose
	 * @param controllers the animation controllers in pose composition order
	 * @param molangContexts the frame-specific Molang context of each controller
	 * @param renderType the render type
	 * @param packedColor the packed ARGB vertex color
	 * @param packedLight the packed block and sky light coordinates
	 * @param packedOverlay the packed overlay texture coordinates
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @param context the context
	 */
	protected void perBoneSubmit(T animatable, ItemStack stack, PoseStack poseStack, PBakedBone bone, PPose pose, Collection<PAnimationController<T>> controllers, Map<PAnimationController<T>, MolangParser.Context> molangContexts, Function<Identifier, RenderType> renderType, int packedColor, int packedLight, int packedOverlay, float partialTick, ItemDisplayContext context)
	{
		PMeshRenderContext inherited = new PMeshRenderContext(
				renderType,
				packedColor,
				packedLight,
				packedOverlay);
		perBoneSubmit(animatable, stack, poseStack, bone, pose, controllers, molangContexts, inherited, partialTick, context);
	}

	/**
	 * Submits the selected bone and its descendants using the sampled pose and inherited mesh render context.
	 *
	 * @param animatable the owner whose animation state is being processed
	 * @param stack the item stack supplying render or animation state
	 * @param poseStack the current model-to-render pose stack
	 * @param bone the baked bone being sampled or submitted
	 * @param pose the indexed bone pose
	 * @param controllers the animation controllers in pose composition order
	 * @param molangContexts the frame-specific Molang context of each controller
	 * @param inherited the render inputs inherited from the parent bone or renderer
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @param context the context
	 */
	protected void perBoneSubmit(T animatable, ItemStack stack, PoseStack poseStack, PBakedBone bone, PPose pose, Collection<PAnimationController<T>> controllers, Map<PAnimationController<T>, MolangParser.Context> molangContexts, PMeshRenderContext inherited, float partialTick, ItemDisplayContext context)
	{
		PModelData data = this.getModelData(animatable);
		PBakedModel model = selectedModel(animatable);
		if (model == null)
			return;
		int boneIndex = model.boneIndex(bone);
		if (!pose.isVisible(boneIndex))
			return;
		poseStack.pushPose();
		try
		{
			poseStack.translate(pose.translation(boneIndex).x(), pose.translation(boneIndex).y(), pose.translation(boneIndex).z());
			poseStack.mulPose(pose.rotation(boneIndex));
			poseStack.scale(pose.scale(boneIndex).x(), pose.scale(boneIndex).y(), pose.scale(boneIndex).z());

			PMeshRenderContext boneContext = resolveBoneForSubmission(animatable, stack, context, bone, inherited, partialTick);
			this.submitBone(animatable, stack, bone, poseStack, data, controllers, boneContext, partialTick, context);

			if (!bone.children().isEmpty())
				bone.children().forEach(child -> perBoneSubmit(animatable, stack, poseStack, child, pose, controllers, molangContexts, boneContext, partialTick, context));
		}
		finally
		{
			poseStack.popPose();
		}
	}

	protected void perBoneSubmit(RS renderState, PoseStack poseStack, PBakedBone bone, Collection<PAnimationController<T>> controllers, Map<PAnimationController<T>, MolangParser.Context> molangContexts, Function<Identifier, RenderType> renderType, int packedColor, int packedLight, int packedOverlay, ItemDisplayContext context)
	{
		PBakedModel model = renderState.getBakedModel();
		if (model == null)
			return;
		PPose pose = model.evaluate(controllers, molangContexts, renderState.partialTick());
		this.submission.run(renderState, () -> perBoneSubmit(renderState.getAnimatable(), renderState.stack(), poseStack,
				bone, pose, controllers, molangContexts, renderType, packedColor, packedLight, packedOverlay, renderState.partialTick(), context));
	}

	/**
	 * Prepares the molang contexts.
	 * @param animatable the animatable to use.
	 * @param manager the manager to use.
	 * @param controllers the controllers to use.
	 * @param partialTick the partial tick to use.
	 * @return the value produced by this operation.
	 */
	private Map<PAnimationController<T>, MolangParser.Context> prepareMolangContexts(T animatable,
	                                                                                   PAnimationManager<T> manager,
	                                                                                   Collection<PAnimationController<T>> controllers,
	                                                                                   float partialTick)
	{
		Map<PAnimationController<T>, MolangParser.Context> contexts = new PRenderPoseContexts<>();
		for (PAnimationController<T> controller : controllers)
		{
			MolangParser.Context context = new MolangParser.Context().
					query("anim_time", PAnimationEvaluationContext.ticksToSeconds(controller.getInterpolatedTime(partialTick))).
					randomSeed(manager.key().key());
			populateMolangContext(animatable, controller, context, partialTick);
			contexts.put(controller, context);
		}
		return contexts;
	}

	/**
	 * Performs the populate molang context operation.
	 * @param animatable the animatable to use.
	 * @param controller the controller to use.
	 * @param context the context to use.
	 * @param partialTick the partial tick to use.
	 */
	protected void populateMolangContext(T animatable,
	                                    PAnimationController<T> controller,
	                                    MolangParser.Context context,
	                                    float partialTick)
	{
	}
	
	/**
	 * Submits one bone hierarchy using the supplied model and controller inputs.
	 *
	 * @param animatable the owner whose animation state is being processed
	 * @param stack the item stack supplying render or animation state
	 * @param bone the baked bone being sampled or submitted
	 * @param poseStack the current model-to-render pose stack
	 * @param modelData the canonical model and loader selection
	 * @param controllers the animation controllers in pose composition order
	 * @param renderType the render type
	 * @param color the packed ARGB vertex color
	 * @param packedLight the packed block and sky light coordinates
	 * @param packedOverlay the packed overlay texture coordinates
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @param context the context
	 */
	protected void submitBone(T animatable,
	                          ItemStack stack,
	                          PBakedBone bone,
	                          PoseStack poseStack,
	                          PModelData modelData,
	                          Collection<PAnimationController<T>> controllers,
	                          Function<Identifier, RenderType> renderType,
	                          int color,
	                          int packedLight,
	                          int packedOverlay,
	                          float partialTick,
	                          ItemDisplayContext context)
	{
		submitBone(animatable, stack, bone, poseStack, modelData, controllers,
				new PMeshRenderContext(renderType, color, packedLight, packedOverlay), partialTick, context);
	}

	/**
	 * Submits one bone hierarchy using the supplied model and controller inputs.
	 *
	 * @param animatable the owner whose animation state is being processed
	 * @param stack the item stack supplying render or animation state
	 * @param bone the baked bone being sampled or submitted
	 * @param poseStack the current model-to-render pose stack
	 * @param modelData the canonical model and loader selection
	 * @param controllers the animation controllers in pose composition order
	 * @param inherited the render inputs inherited from the parent bone or renderer
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @param context the context
	 */
	protected void submitBone(T animatable,
	                          ItemStack stack,
	                          PBakedBone bone,
	                          PoseStack poseStack,
	                          PModelData modelData,
	                          Collection<PAnimationController<T>> controllers,
	                          PMeshRenderContext inherited,
	                          float partialTick,
	                          ItemDisplayContext context)
	{
		Matrix4f transform = new Matrix4f(poseStack.last().pose());
		for (PBakedMesh mesh : bone.meshes())
		{
			if (mesh.textureReference().isEmpty())
				continue;
			PMeshRenderContext meshContext = resolveMeshForSubmission(animatable, stack, context, bone, mesh, inherited, partialTick);
			PMeshRenderMaterial material = PMeshRenderMaterial.resolve(mesh, meshContext);
			RenderType type = material.resolveRenderType(meshContext, PResourceCache.ATLAS_LOCATION);
			var instance = new PRenderQueue.InstanceData(transform, meshContext.color(), material.packedLight(), meshContext.packedOverlay());
			PRenderQueue.submitItem(context, type, material.mesh(), meshContext.deformation(), instance);
		}
	}

	protected void submitBone(RS renderState,
	                          PBakedBone bone,
	                          PoseStack poseStack,
	                          PModelData modelData,
	                          Collection<PAnimationController<T>> controllers,
	                          Function<Identifier, RenderType> renderType,
	                          int color,
	                          int packedLight,
	                          int packedOverlay,
	                          ItemDisplayContext context)
	{
		this.submission.run(renderState, () -> submitBone(renderState.getAnimatable(), renderState.stack(), bone, poseStack, modelData, controllers, renderType, color, packedLight, packedOverlay, renderState.partialTick(), context));
	}
	
	/**
	 * Resolves render overrides inherited by the selected bone and its meshes.
	 *
	 * @param animatable the owner whose animation state is being processed
	 * @param stack the item stack supplying render or animation state
	 * @param context the context
	 * @param bone the baked bone being sampled or submitted
	 * @param inherited the render inputs inherited from the parent bone or renderer
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @return the inherited or overridden mesh render inputs
	 */
	protected PMeshRenderContext resolveBoneRender(T animatable,
	                                               ItemStack stack,
	                                               ItemDisplayContext context,
	                                               PBakedBone bone,
	                                               PMeshRenderContext inherited,
	                                               float partialTick)
	{
		return inherited;
	}

	/**
	 * Resolves final render overrides for one mesh of the selected bone.
	 *
	 * @param animatable the owner whose animation state is being processed
	 * @param stack the item stack supplying render or animation state
	 * @param context the context
	 * @param bone the baked bone being sampled or submitted
	 * @param mesh the baked mesh geometry and material
	 * @param inherited the render inputs inherited from the parent bone or renderer
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @return the inherited or overridden mesh render inputs
	 */
	protected PMeshRenderContext resolveMeshRender(T animatable,
	                                               ItemStack stack,
	                                               ItemDisplayContext context,
	                                               PBakedBone bone,
	                                               PBakedMesh mesh,
	                                               PMeshRenderContext inherited,
	                                               float partialTick)
	{
		return inherited;
	}

	protected PMeshRenderContext resolveMeshRender(RS renderState,
	                                               ItemDisplayContext context,
	                                               PBakedBone bone,
	                                               PBakedMesh mesh,
	                                               PMeshRenderContext inherited)
	{
		return resolveMeshRender(renderState.getAnimatable(), renderState.stack(), context, bone, mesh, inherited, renderState.partialTick());
	}

	private @Nullable PBakedModel selectedModel(T animatable)
	{
		RS state = stateFor(animatable);
		return state == null ? getModel(animatable) : state.getBakedModel();
	}

	/** Native state hook delegates to the canonical owner hook by default. */
	protected PMeshRenderContext resolveBoneRender(RS state, ItemDisplayContext context, PBakedBone bone, PMeshRenderContext inherited)
	{
		return resolveBoneRender(state.getAnimatable(), state.stack(), context, bone, inherited, state.partialTick());
	}

	private PMeshRenderContext resolveBoneForSubmission(T animatable, ItemStack stack, ItemDisplayContext context, PBakedBone bone, PMeshRenderContext inherited, float partialTick)
	{
		RS state = stateFor(animatable);
		return state == null ? resolveBoneRender(animatable, stack, context, bone, inherited, partialTick) :
				resolveBoneRender(state, context, bone, inherited);
	}

	private PMeshRenderContext resolveMeshForSubmission(T animatable, ItemStack stack, ItemDisplayContext context, PBakedBone bone, PBakedMesh mesh, PMeshRenderContext inherited, float partialTick)
	{
		RS state = stateFor(animatable);
		return state == null ? resolveMeshRender(animatable, stack, context, bone, mesh, inherited, partialTick) :
				resolveMeshRender(state, context, bone, mesh, inherited);
	}

	private @Nullable RS stateFor(T owner)
	{
		RS state = this.submission.current();
		return state != null && state.getAnimatable() == owner ? state : null;
	}
}
