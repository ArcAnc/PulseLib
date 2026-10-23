/**
 * @author ArcAnc
 * Created at: 27.01.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer;


import com.arcanc.pulselib.content.animatable.PAnimatable;
import com.arcanc.pulselib.content.animatable.PAnimationController;
import com.arcanc.pulselib.content.animatable.PAnimationManager;
import com.arcanc.pulselib.content.animatable.instance.InstanceAnimationManager;
import com.arcanc.pulselib.content.model.animation.PPose;
import com.arcanc.pulselib.content.model.animation.PAnimationEvaluationContext;
import com.arcanc.pulselib.content.model.baked.PBakedBone;
import com.arcanc.pulselib.content.model.baked.PBakedMesh;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.model.baked.PMeshRenderContext;
import com.arcanc.pulselib.content.model.baked.PMeshRenderMaterial;
import com.arcanc.pulselib.content.renderer.base.PBlockRenderState;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.data.gecko.MolangParser;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.PResourceCache;
import com.arcanc.pulselib.util.helpers.PLibHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Renders block.
 */
public abstract class PBlockRenderer<T extends BlockEntity & PAnimatable<T>, RS extends BlockEntityRenderState & PBlockRenderState<T>>
		implements PRenderer<T, RS>, BlockEntityRenderer<T, RS>
{
	private final PModelData modelData;
	private final PRenderStateScope<RS> submission = new PRenderStateScope<>();
	private final Function<Identifier, RenderType> renderType;
	
	/**
	 * Creates an instance of the enclosing type.
	 * @param modelData the model data to use.
	 * @param renderType the render type to use.
	 */
	public PBlockRenderer(PModelData modelData, Function<Identifier, RenderType> renderType)
	{
		this.modelData = modelData;
		this.renderType = renderType;
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
	 * Extracts the render state.
	 * @param blockEntity the block entity to use.
	 * @param renderState the render state to use.
	 * @param partialTick the partial tick to use.
	 * @param cameraPosition the camera position to use.
	 * @param breakProgress the break progress to use.
	 */
	@Override
	public void extractRenderState(T blockEntity,
	                               RS renderState,
	                               float partialTick,
	                               Vec3 cameraPosition,
	                               ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress)
	{
		renderState.extractBlockData(blockEntity, this, breakProgress);
		renderState.extractFacing(getAnimatableFacing(blockEntity));
	}
	
	/**
	 * Performs the submit operation.
	 * @param renderState the render state to use.
	 * @param poseStack the pose stack to use.
	 * @param submitNodeCollector the submit node collector to use.
	 * @param cameraRenderState the camera render state to use.
	 */
	@Override
	public void submit(RS renderState,
	                   PoseStack poseStack,
	                   SubmitNodeCollector submitNodeCollector,
	                   CameraRenderState cameraRenderState)
	{
		poseStack.pushPose();
		try
		{
			poseStack.translate(0.5f, 0, 0.5f);
			tryRotateToRealRotation(poseStack, getAnimatableFacing(renderState));
			preSubmit(poseStack, renderState, cameraRenderState, submitNodeCollector);
			trueSubmit(poseStack, renderState, cameraRenderState, submitNodeCollector);
			postSubmit(poseStack, renderState, cameraRenderState, submitNodeCollector);
		}
		finally
		{
			poseStack.popPose();
		}
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
			InstanceAnimationManager.addManager(manager);
		
			PPose pose = model.evaluate(controllers, molangContexts, renderState.partialTick());
			model.bones().forEach(bone -> perBoneSubmit(renderState.getAnimatable(), poseStack, bone, pose, controllers,
					molangContexts, this::getRenderType, -1, renderState.lightCoords, OverlayTexture.NO_OVERLAY, renderState.partialTick()));
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
	 */
	protected void perBoneSubmit(T animatable, PoseStack poseStack, PBakedBone bone, PPose pose, Collection<PAnimationController<T>> controllers, Map<PAnimationController<T>, MolangParser.Context> molangContexts, Function<Identifier, RenderType> renderType, int packedColor, int packedLight, int packedOverlay, float partialTick)
	{
		PMeshRenderContext inherited = new PMeshRenderContext(
				renderType,
				packedColor,
				packedLight,
				packedOverlay);
		perBoneSubmit(animatable, poseStack, bone, pose, controllers, molangContexts, inherited, partialTick);
	}

	/**
	 * Submits the selected bone and its descendants using the sampled pose and inherited mesh render context.
	 *
	 * @param animatable the owner whose animation state is being processed
	 * @param poseStack the current model-to-render pose stack
	 * @param bone the baked bone being sampled or submitted
	 * @param pose the indexed bone pose
	 * @param controllers the animation controllers in pose composition order
	 * @param molangContexts the frame-specific Molang context of each controller
	 * @param inherited the render inputs inherited from the parent bone or renderer
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 */
	protected void perBoneSubmit(T animatable, PoseStack poseStack, PBakedBone bone, PPose pose, Collection<PAnimationController<T>> controllers, Map<PAnimationController<T>, MolangParser.Context> molangContexts, PMeshRenderContext inherited, float partialTick)
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

			PMeshRenderContext boneContext = resolveBoneForSubmission(animatable, bone, inherited, partialTick);
			this.submitBone(animatable, bone, poseStack, data, controllers, boneContext, partialTick);

			if (!bone.children().isEmpty())
				bone.children().forEach(child -> perBoneSubmit(animatable, poseStack, child, pose, controllers, molangContexts, boneContext, partialTick));
		}
		finally
		{
			poseStack.popPose();
		}
	}

	protected void perBoneSubmit(RS renderState, PoseStack poseStack, PBakedBone bone, Collection<PAnimationController<T>> controllers, Map<PAnimationController<T>, MolangParser.Context> molangContexts, Function<Identifier, RenderType> renderType, int packedColor, int packedLight, int packedOverlay)
	{
		PBakedModel model = renderState.getBakedModel();
		if (model == null)
			return;
		PPose pose = model.evaluate(controllers, molangContexts, renderState.partialTick());
		this.submission.run(renderState, () -> perBoneSubmit(renderState.getAnimatable(), poseStack, bone, pose,
				controllers, molangContexts, renderType, packedColor, packedLight, packedOverlay, renderState.partialTick()));
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
	 * @param bone the baked bone being sampled or submitted
	 * @param poseStack the current model-to-render pose stack
	 * @param modelData the canonical model and loader selection
	 * @param controllers the animation controllers in pose composition order
	 * @param renderType the render type
	 * @param color the packed ARGB vertex color
	 * @param packedLight the packed block and sky light coordinates
	 * @param packedOverlay the packed overlay texture coordinates
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 */
	protected void submitBone(T animatable,
	                          PBakedBone bone,
	                          PoseStack poseStack,
	                          PModelData modelData,
	                          Collection<PAnimationController<T>> controllers,
	                          Function<Identifier, RenderType> renderType,
	                          int color,
	                          int packedLight,
	                          int packedOverlay,
	                          float partialTick)
	{
		submitBone(animatable, bone, poseStack, modelData, controllers,
				new PMeshRenderContext(renderType, color, packedLight, packedOverlay), partialTick);
	}

	/**
	 * Submits one bone hierarchy using the supplied model and controller inputs.
	 *
	 * @param animatable the owner whose animation state is being processed
	 * @param bone the baked bone being sampled or submitted
	 * @param poseStack the current model-to-render pose stack
	 * @param modelData the canonical model and loader selection
	 * @param controllers the animation controllers in pose composition order
	 * @param inherited the render inputs inherited from the parent bone or renderer
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 */
	protected void submitBone(T animatable,
	                          PBakedBone bone,
	                          PoseStack poseStack,
	                          PModelData modelData,
	                          Collection<PAnimationController<T>> controllers,
	                          PMeshRenderContext inherited,
	                          float partialTick)
	{
		Matrix4f transform = new Matrix4f(poseStack.last().pose());
		for (PBakedMesh mesh : bone.meshes())
		{
			if (mesh.textureReference().isEmpty())
				continue;
			PMeshRenderContext meshContext = resolveMeshForSubmission(animatable, bone, mesh, inherited, partialTick);
			PMeshRenderMaterial material = PMeshRenderMaterial.resolve(mesh, meshContext);
			RenderType type = material.resolveRenderType(meshContext, PResourceCache.ATLAS_LOCATION);
			var instance = new PRenderQueue.InstanceData(transform, meshContext.color(), material.packedLight(), meshContext.packedOverlay());
			if (PRenderTypes.isTransparent(type))
				PRenderQueue.submitBlockEntityTranslucentMesh(type, material.mesh(), meshContext.deformation(), instance);
			else
				PRenderQueue.submitBlockEntityMesh(type, material.mesh(), meshContext.deformation(), instance);
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
	                          int packedOverlay)
	{
		this.submission.run(renderState, () -> submitBone(renderState.getAnimatable(), bone, poseStack, modelData, controllers, renderType, color, packedLight, packedOverlay, renderState.partialTick()));
	}
	
	/**
	 * Resolves render overrides inherited by the selected bone and its meshes.
	 *
	 * @param animatable the owner whose animation state is being processed
	 * @param bone the baked bone being sampled or submitted
	 * @param inherited the render inputs inherited from the parent bone or renderer
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @return the inherited or overridden mesh render inputs
	 */
	protected PMeshRenderContext resolveBoneRender(T animatable,
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
	 * @param bone the baked bone being sampled or submitted
	 * @param mesh the baked mesh geometry and material
	 * @param inherited the render inputs inherited from the parent bone or renderer
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @return the inherited or overridden mesh render inputs
	 */
	protected PMeshRenderContext resolveMeshRender(T animatable,
	                                               PBakedBone bone,
	                                               PBakedMesh mesh,
	                                               PMeshRenderContext inherited,
	                                               float partialTick)
	{
		return inherited;
	}

	protected PMeshRenderContext resolveMeshRender(RS renderState,
	                                               PBakedBone bone,
	                                               PBakedMesh mesh,
	                                               PMeshRenderContext inherited)
	{
		return resolveMeshRender(renderState.getAnimatable(), bone, mesh, inherited, renderState.partialTick());
	}
	
	/**
	 * Performs the try rotate to real rotation operation.
	 * @param poseStack the pose stack to use.
	 * @param facing the facing to use.
	 */
	protected void tryRotateToRealRotation(PoseStack poseStack, Direction facing)
	{
		if (facing.getAxis().isHorizontal())
		{
			poseStack.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));
		}
		else
		{
			poseStack.mulPose(Axis.XP.rotationDegrees(90 * facing.getUnitVec3i().getY()));
		}
	}
	
	/**
	 * Returns the animatable facing.
	 * @param renderState the render state to use.
	 * @return the value produced by this operation.
	 */
	protected Direction getAnimatableFacing(RS renderState)
	{
		return renderState.facing();
	}

	/**
	 * Returns the block-facing direction used to orient the animatable model.
	 *
	 * @param animatable the owner whose animation state is being processed
	 * @return the block-facing direction used to orient the animatable model
	 */
	protected Direction getAnimatableFacing(T animatable)
	{
		BlockState blockState = animatable.getBlockState();
		Direction dir = Direction.NORTH;
		
		if (blockState.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
			dir = blockState.getValue(BlockStateProperties.HORIZONTAL_FACING);
		
		if (blockState.hasProperty(BlockStateProperties.FACING))
			dir = blockState.getValue(BlockStateProperties.FACING);
		
		if (dir.getAxis() ==  Direction.Axis.Z)
			dir = dir.getOpposite();
		return dir;
	}

	private @Nullable PBakedModel selectedModel(T animatable)
	{
		RS state = stateFor(animatable);
		return state == null ? getModel(animatable) : state.getBakedModel();
	}

	/** Native state hook delegates to the canonical owner hook by default. */
	protected PMeshRenderContext resolveBoneRender(RS state, PBakedBone bone, PMeshRenderContext inherited)
	{
		return resolveBoneRender(state.getAnimatable(), bone, inherited, state.partialTick());
	}

	private PMeshRenderContext resolveBoneForSubmission(T animatable, PBakedBone bone, PMeshRenderContext inherited, float partialTick)
	{
		RS state = stateFor(animatable);
		return state == null ? resolveBoneRender(animatable, bone, inherited, partialTick) :
				resolveBoneRender(state, bone, inherited);
	}

	private PMeshRenderContext resolveMeshForSubmission(T animatable, PBakedBone bone, PBakedMesh mesh, PMeshRenderContext inherited, float partialTick)
	{
		RS state = stateFor(animatable);
		return state == null ? resolveMeshRender(animatable, bone, mesh, inherited, partialTick) :
				resolveMeshRender(state, bone, mesh, inherited);
	}

	private @Nullable RS stateFor(T owner)
	{
		RS state = this.submission.current();
		return state != null && state.getAnimatable() == owner ? state : null;
	}
}
