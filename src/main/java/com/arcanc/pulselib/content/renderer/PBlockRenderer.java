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
import com.arcanc.pulselib.content.mixin.BlockEntityRenderStateAccessor;
import com.arcanc.pulselib.content.model.animation.BoneFrame;
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
		return this.modelData;
	}
	
	/**
	 * Returns the model.
	 * @param renderState the render state to use.
	 * @return the value produced by this operation.
	 */
	@Override
	public @Nullable PBakedModel getModel(RS renderState)
	{
		return getModelData(renderState).getModel();
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
		poseStack.translate(0.5f, 0, 0.5f);
		tryRotateToRealRotation(poseStack, getAnimatableFacing(renderState));
		preSubmit(poseStack, renderState, cameraRenderState, submitNodeCollector);
		trueSubmit(poseStack, renderState, cameraRenderState, submitNodeCollector);
		postSubmit(poseStack, renderState, cameraRenderState, submitNodeCollector);
		poseStack.popPose();
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
		PBakedModel model = this.getModelData(renderState).getModel();
		if (model == null)
			return;
		PAnimationManager<T> manager = renderState.getAnimatable().getAnimationManager(renderState.getAnimKey());
		manager.bindModel(model);
		Collection<PAnimationController<T>> controllers = manager.getControllers().values();
		Map<PAnimationController<T>, MolangParser.Context> molangContexts = prepareMolangContexts(
				renderState.getAnimatable(), manager, controllers, renderState.partialTick());
		InstanceAnimationManager.addManager(manager);
		
		model.bones().forEach(bone -> perBoneSubmit(renderState, poseStack, bone, controllers, molangContexts, renderType, -1, renderState.lightCoords, OverlayTexture.NO_OVERLAY));
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
	 * Performs the per bone submit operation.
	 * @param renderState the render state to use.
	 * @param poseStack the pose stack to use.
	 * @param bone the bone to use.
	 * @param controllers the controllers to use.
	 * @param molangContexts the molang contexts to use.
	 * @param renderType the render type to use.
	 * @param packedColor the packed color to use.
	 * @param packedLight the packed light to use.
	 * @param packedOverlay the packed overlay to use.
	 */
	protected void perBoneSubmit(RS renderState, PoseStack poseStack, PBakedBone bone, Collection<PAnimationController<T>> controllers, Map<PAnimationController<T>, MolangParser.Context> molangContexts, Function<Identifier, RenderType> renderType, int packedColor, int packedLight, int packedOverlay)
	{
		PModelData data = this.getModelData(renderState);
		BoneFrame frame = bone.mixBone(data.getModel(), controllers, molangContexts, renderState.partialTick());
		poseStack.pushPose();
		if (frame != null)
		{
			poseStack.translate(frame.translation().x(), frame.translation().y(), frame.translation().z());
			poseStack.mulPose(frame.rotation());
			poseStack.scale(frame.scale().x(), frame.scale().y(), frame.scale().z());
		}
		else
		{
			poseStack.translate(bone.basePosition().x(), bone.basePosition().y(), bone.basePosition().z());
			poseStack.mulPose(bone.baseRotation());
		}
		
		this.submitBone(renderState, bone, poseStack, data, controllers, renderType, packedColor, packedLight, packedOverlay);
		
		if (!bone.children().isEmpty())
			bone.children().forEach(child -> perBoneSubmit(renderState, poseStack, child, controllers, molangContexts, renderType, packedColor, packedLight, packedOverlay));
		
		poseStack.popPose();
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
		Map<PAnimationController<T>, MolangParser.Context> contexts = new IdentityHashMap<>();
		for (PAnimationController<T> controller : controllers)
		{
			MolangParser.Context context = new MolangParser.Context().
					query("anim_time", controller.getInterpolatedTime(partialTick) / 20.0f).
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
	 * Performs the submit bone operation.
	 * @param renderState the render state to use.
	 * @param bone the bone to use.
	 * @param poseStack the pose stack to use.
	 * @param modelData the model data to use.
	 * @param controllers the controllers to use.
	 * @param renderType the render type to use.
	 * @param color the color to use.
	 * @param packedLight the packed light to use.
	 * @param packedOverlay the packed overlay to use.
	 */
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
		Matrix4f matrix4fstack = new Matrix4f(poseStack.last().pose());
		
		bone.meshes().forEach(mesh ->
		{
			if (mesh.textureReference().isEmpty())
				return;
			
			PMeshRenderContext inherited = new PMeshRenderContext(
					renderType,
					color,
					packedLight,
					packedOverlay);
			PMeshRenderContext meshContext = resolveMeshRender(renderState, bone, mesh, inherited);
			PMeshRenderMaterial material = PMeshRenderMaterial.resolve(mesh, meshContext);
			
			RenderType type = material.resolveRenderType(meshContext, PResourceCache.ATLAS_LOCATION);
			
			if (PRenderTypes.isTransparent(type))
				PRenderQueue.submitBlockEntityTranslucentMesh(type, material.mesh(), meshContext.deformation(), new PRenderQueue.InstanceData(matrix4fstack, meshContext.color(), material.packedLight(), meshContext.packedOverlay()));
			else
				PRenderQueue.submitBlockEntityMesh(type, material.mesh(), meshContext.deformation(), new PRenderQueue.InstanceData(matrix4fstack, meshContext.color(), material.packedLight(), meshContext.packedOverlay()));
		});
	}
	
	/**
	 * Resolves the mesh render.
	 * @param renderState the render state to use.
	 * @param bone the bone to use.
	 * @param mesh the mesh to use.
	 * @param inherited the inherited to use.
	 * @return the value produced by this operation.
	 */
	protected PMeshRenderContext resolveMeshRender(RS renderState,
	                                               PBakedBone bone,
	                                               PBakedMesh mesh,
	                                               PMeshRenderContext inherited)
	{
		return inherited;
	}
	
	/**
	 * Performs the try rotate to real rotation operation.
	 * @param poseStack the pose stack to use.
	 * @param facing the facing to use.
	 */
	protected void tryRotateToRealRotation(PoseStack poseStack, Direction facing)
	{
		if (facing.getAxis().isHorizontal())
			poseStack.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));
		else
			poseStack.mulPose(Axis.XP.rotationDegrees(90 * facing.getUnitVec3i().getY()));
	}
	
	/**
	 * Returns the animatable facing.
	 * @param renderState the render state to use.
	 * @return the value produced by this operation.
	 */
	protected Direction getAnimatableFacing(RS renderState)
	{
		BlockState blockState = ((BlockEntityRenderStateAccessor)renderState).pulselib$getBlockState();
		Direction dir = Direction.NORTH;
		
		if (blockState.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
			dir = blockState.getValue(BlockStateProperties.HORIZONTAL_FACING);
		
		if (blockState.hasProperty(BlockStateProperties.FACING))
			dir = blockState.getValue(BlockStateProperties.FACING);
		
		if (dir.getAxis() ==  Direction.Axis.Z)
			dir = dir.getOpposite();
		return dir;
	}
}
