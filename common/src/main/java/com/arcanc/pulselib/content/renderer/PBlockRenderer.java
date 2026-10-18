/**
 * @author ArcAnc
 * Created at: 27.01.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer;


import com.arcanc.pulselib.content.animatable.AnimManagerKey;
import com.arcanc.pulselib.content.animatable.PAnimatable;
import com.arcanc.pulselib.content.animatable.PAnimationController;
import com.arcanc.pulselib.content.animatable.PAnimationManager;
import com.arcanc.pulselib.content.animatable.instance.InstanceAnimationManager;
import com.arcanc.pulselib.content.model.animation.PAnimationEvaluationContext;
import com.arcanc.pulselib.content.model.animation.PPose;
import com.arcanc.pulselib.content.model.baked.*;
import com.arcanc.pulselib.content.model.deformer.gpu.PGpuDeformerBuffers;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.content.renderer.plan.PDynamicGeometry;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.data.gecko.MolangParser;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.PResourceCache;
import com.arcanc.pulselib.util.helpers.PLibHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

/**
 * Base renderer for animatable block entities with per-bone submission and Molang query hooks.
 *
 * @param <T> the animatable owner type
 */
public abstract class PBlockRenderer<T extends BlockEntity & PAnimatable<T>>
		implements PRenderer<T>, BlockEntityRenderer<T>
{
	private final PModelData modelData;
	private final Function<ResourceLocation, RenderType> renderType;
	
	/**
	 * Creates a block-entity renderer using the supplied model and texture-to-pipeline resolver.
	 *
	 * @param modelData the canonical model and loader selection
	 * @param renderType the render type
	 */
	public PBlockRenderer(PModelData modelData, Function<ResourceLocation, RenderType> renderType)
	{
		this.modelData = modelData;
		this.renderType = renderType;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public PModelData getModelData(T animatable)
	{
		return this.modelData;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public @Nullable PBakedModel getModel(T animatable)
	{
		return getModelData(animatable).getModel();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public RenderType getRenderType(ResourceLocation texture)
	{
		return this.renderType.apply(texture);
	}
	
	/**
	 * Samples block-entity animation and submits the model with the block-facing orientation and render
	 * inputs.
	 *
	 * @param animatable the owner whose animation state is being processed
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @param poseStack the current model-to-render pose stack
	 * @param bufferSource the vanilla vertex buffer source
	 * @param packedLight the packed block and sky light coordinates
	 * @param packedOverlay the packed overlay texture coordinates
	 */
	@Override
	public void render(T animatable, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay)
	{
		poseStack.pushPose();
		poseStack.translate(0.5f, 0, 0.5f);
		tryRotateToRealRotation(poseStack, getAnimatableFacing(animatable));
		preSubmit(poseStack, animatable, this :: getRenderType, bufferSource, packedLight, packedOverlay, partialTick);
		trueSubmit(poseStack, animatable, this :: getRenderType, bufferSource, packedLight, packedOverlay, partialTick);
		postSubmit(poseStack, animatable, this :: getRenderType, bufferSource, packedLight, packedOverlay, partialTick);
		poseStack.popPose();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void preSubmit(PoseStack poseStack, T animatable, Function<ResourceLocation, RenderType> renderType, MultiBufferSource bufferSource, int packedLight, int packedOverlay, float partialTick, @Nullable Object... additionalData)
	{
	
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void trueSubmit(PoseStack poseStack, T animatable, Function<ResourceLocation, RenderType> renderType, MultiBufferSource bufferSource, int packedLight, int packedOverlay, float partialTick, @Nullable Object... additionalData)
	{
		PBakedModel model = this.getModelData(animatable).getModel();
		if (model == null)
			return;
		PAnimationManager<T> manager = PLibHelper.getManager(animatable);
		manager.bindModel(model);
		Collection<PAnimationController<T>> controllers = manager.getControllers().values();
		Map<PAnimationController<T>, MolangParser.Context> molangContexts = prepareMolangContexts(animatable, manager.key(), controllers, partialTick);
		InstanceAnimationManager.addManager(manager);
		
		PPose pose = model.evaluate(controllers, molangContexts, partialTick);
		model.bones().forEach(bone -> perBoneSubmit(animatable, poseStack, bone, pose, controllers, molangContexts, renderType, -1, packedLight, packedOverlay, partialTick));
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void postSubmit(PoseStack poseStack, T animatable, Function<ResourceLocation, RenderType> renderType, MultiBufferSource bufferSource, int packedLight, int packedOverlay, float partialTick, @Nullable Object... additionalData)
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
	protected void perBoneSubmit(T animatable, PoseStack poseStack, PBakedBone bone, PPose pose, Collection<PAnimationController<T>> controllers, Map<PAnimationController<T>, MolangParser.Context> molangContexts, Function<ResourceLocation, RenderType> renderType, int packedColor, int packedLight, int packedOverlay, float partialTick)
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
		int boneIndex = data.getModel().boneIndex(bone);
		poseStack.pushPose();
		poseStack.translate(pose.translation(boneIndex).x(), pose.translation(boneIndex).y(), pose.translation(boneIndex).z());
		poseStack.mulPose(pose.rotation(boneIndex));
		poseStack.scale(pose.scale(boneIndex).x(), pose.scale(boneIndex).y(), pose.scale(boneIndex).z());
		
		PMeshRenderContext boneContext = resolveBoneRender(animatable, bone, inherited, partialTick);
		this.submitBone(animatable, bone, poseStack, data, controllers, boneContext, partialTick);
		
		if (!bone.children().isEmpty())
			bone.children().forEach(child -> perBoneSubmit(animatable, poseStack, child, pose, controllers, molangContexts, boneContext, partialTick));

		poseStack.popPose();
	}

	private Map<PAnimationController<T>, MolangParser.Context> prepareMolangContexts(T animatable,
	                                                                                   AnimManagerKey key,
	                                                                                   Collection<PAnimationController<T>> controllers,
	                                                                                   float partialTick)
	{
		Map<PAnimationController<T>, MolangParser.Context> contexts = new Object2ObjectOpenHashMap<>();
		for (PAnimationController<T> controller : controllers)
		{
			MolangParser.Context context = new MolangParser.Context().
					query("anim_time", PAnimationEvaluationContext.ticksToSeconds(controller.getInterpolatedTime(partialTick))).
					randomSeed(key.key());
			populateMolangContext(animatable, controller, context, partialTick);
			contexts.put(controller, context);
		}
		return contexts;
	}

	/**
	 * Populates owner-specific Molang queries for this controller and render sample.
	 *
	 * @param animatable the owner whose animation state is being processed
	 * @param controller the controller supplying playback and persistent expression state
	 * @param context the Molang context containing current frame and instance values
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 */
	protected void populateMolangContext(T animatable,
	                                    PAnimationController<T> controller,
	                                    MolangParser.Context context,
	                                    float partialTick)
	{
	}
	
	/**
	 * Applies the block-facing orientation to the current render pose.
	 *
	 * @param poseStack the current model-to-render pose stack
	 * @param facing the facing
	 */
	protected void tryRotateToRealRotation(PoseStack poseStack, Direction facing)
	{
		if (facing.getAxis().isHorizontal())
			poseStack.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));
		else
			poseStack.mulPose(Axis.XP.rotationDegrees(90 * facing.getNormal().getY()));
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
	                          Function<ResourceLocation, RenderType> renderType,
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
		Matrix4f matrix4fstack = new Matrix4f(poseStack.last().pose());

		bone.meshes().forEach(mesh ->
		{
			if (mesh.textureReference().isEmpty())
				return;
			
			PMeshRenderContext meshContext = resolveMeshRender(animatable, bone, mesh, inherited, partialTick);
			PMeshRenderMaterial material = PMeshRenderMaterial.resolve(mesh, meshContext);
			
			RenderType type = material.resolveRenderType(meshContext, PResourceCache.ATLAS_LOCATION);
			
			PGpuDeformerBuffers.Submission deformation = PGpuDeformerBuffers.submit(meshContext.deformation());
			PRenderTypes.getTransparencyState(type).ifPresent(transparency ->
			{
				PInstanceHeader instanceData = new PInstanceHeader(matrix4fstack, meshContext.color(), material.packedLight(), meshContext.packedOverlay(),
						deformation.operationOffset(), deformation.valueOffset(), deformation.operationCount());
				boolean staticGeometry = meshContext.deformation() == null || meshContext.deformation().stack().isEmpty();
				if (staticGeometry)
				{
					if (transparency == RenderStateShard.TransparencyStateShard.NO_TRANSPARENCY)
						PRenderQueue.submitBlockEntityMesh(type, material.mesh().geometry(), instanceData);
					else
						PRenderQueue.submitBlockEntityTranslucentMesh(type, material.mesh().geometry(), instanceData);
				}
				else if (deformation.enabled())
				{
					if (transparency == RenderStateShard.TransparencyStateShard.NO_TRANSPARENCY)
						PRenderQueue.submitBlockEntityMesh(type,
								PGpuDeformedMeshBuffers.resolve(material.mesh(), meshContext.deformation().subdivisionLevel()), instanceData);
					else
						PRenderQueue.submitBlockEntityTranslucentMesh(type,
								PGpuDeformedMeshBuffers.resolve(material.mesh(), meshContext.deformation().subdivisionLevel()), instanceData);
				}
				else
				{
					PDynamicGeometry buffer = PDeformedMeshBuffers.resolve(material.mesh(), meshContext.deformation());
					if (transparency == RenderStateShard.TransparencyStateShard.NO_TRANSPARENCY)
						PRenderQueue.submitBlockEntityMesh(type, buffer, instanceData);
					else
						PRenderQueue.submitBlockEntityTranslucentMesh(type, buffer, instanceData);
				}
			});
		});
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
}
