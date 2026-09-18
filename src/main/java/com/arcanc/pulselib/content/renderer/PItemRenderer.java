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
import com.arcanc.pulselib.content.model.animation.BoneFrame;
import com.arcanc.pulselib.content.model.baked.*;
import com.arcanc.pulselib.content.renderer.base.PItemRenderState;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.data.PModelLoader;
import com.arcanc.pulselib.data.gecko.MolangParser;
import com.arcanc.pulselib.data.gltf.PGltfModelLoader;
import com.arcanc.pulselib.util.PModelCache;
import com.arcanc.pulselib.util.PResourceCache;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
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
	 * Creates the render state.
	 * @return the value produced by this operation.
	 */
	protected abstract RS createRenderState();
	
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
		renderState.extractAdditionalData(lightCoords,  overlayCoords, hasFoil, outlineColor);
		poseStack.pushPose();
		PModelData modelData = getModelData(renderState);
		PModelLoader modelLoader = PModelCache.getModelLoader(modelData.getModelFormat()).orElse(PGltfModelLoader.INSTANCE);
		modelLoader.applyItemTransform(poseStack);
		
		CameraRenderState cameraRenderState = new CameraRenderState();
		preSubmit(poseStack, renderState, cameraRenderState, submitNodeCollector);
		trueSubmit(poseStack, renderState, cameraRenderState, submitNodeCollector);
		postSubmit(poseStack, renderState, cameraRenderState, submitNodeCollector);
		poseStack.popPose();
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
		PBakedModel model = renderState.getBakedModel();
		if (model == null)
			return;
		
		PAnimationManager<T> manager = renderState.getAnimatable().getAnimationManager(renderState.getAnimKey());
		manager.bindModel(model);
		
		Collection<PAnimationController<T>> controllers = manager.getControllers().values();
		Map<PAnimationController<T>, MolangParser.Context> molangContexts = prepareMolangContexts(
				renderState.getAnimatable(), manager, controllers, renderState.partialTick());
		
		ItemDisplayContext context = ((ItemStackRenderStateAccessor)renderState.itemRenderState()).pulselib$getDisplayContext();
		if (context == ItemDisplayContext.GUI)
		{
			model.bones().forEach(bone -> perBoneSubmit(renderState, poseStack, bone, controllers, molangContexts, renderType, -1, renderState.lightCoords(), renderState.overlayCoords(), context));
			submitNodeCollector.submitCustomGeometry(
					poseStack,
					this.renderType.apply(PResourceCache.ATLAS_LOCATION),
					(_, _) -> PRenderQueue.flush(PRenderQueue.RenderStage.GUI));
			return;
		}
		model.bones().forEach(bone -> perBoneSubmit(renderState, poseStack, bone, controllers, molangContexts, renderType, -1, renderState.lightCoords(), renderState.overlayCoords(), context));
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
	 * @param context the context to use.
	 */
	protected void perBoneSubmit(RS renderState, PoseStack poseStack, PBakedBone bone, Collection<PAnimationController<T>> controllers, Map<PAnimationController<T>, MolangParser.Context> molangContexts, Function<Identifier, RenderType> renderType, int packedColor, int packedLight, int packedOverlay, ItemDisplayContext context)
	{
		PModelData data = this.getModelData(renderState);
		PBakedModel model = data.getModel();
		if (model == null)
			return;
		BoneFrame frame = bone.mixBone(model, controllers, molangContexts, renderState.partialTick());
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
		
		this.submitBone(renderState, bone, poseStack, data, controllers, renderType, packedColor, packedLight, packedOverlay, context);
		
		if (!bone.children().isEmpty())
			bone.children().forEach(child -> perBoneSubmit(renderState, poseStack, child, controllers, molangContexts, renderType, packedColor, packedLight, packedOverlay, context));
		
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
	 * @param context the context to use.
	 */
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
			PMeshRenderContext meshContext = resolveMeshRender(renderState, context, bone, mesh, inherited);
			PMeshRenderMaterial material = PMeshRenderMaterial.resolve(mesh, meshContext);
			
			RenderType type = material.resolveRenderType(meshContext, PResourceCache.ATLAS_LOCATION);
			
			PRenderQueue.submitItem(context, type, material.mesh(), meshContext.deformation(), new PRenderQueue.InstanceData(matrix4fstack, meshContext.color(), material.packedLight(), meshContext.packedOverlay()));
		});
	}
	
	/**
	 * Resolves the mesh render.
	 * @param renderState the render state to use.
	 * @param context the context to use.
	 * @param bone the bone to use.
	 * @param mesh the mesh to use.
	 * @param inherited the inherited to use.
	 * @return the value produced by this operation.
	 */
	protected PMeshRenderContext resolveMeshRender(RS renderState,
	                                               ItemDisplayContext context,
	                                               PBakedBone bone,
	                                               PBakedMesh mesh,
	                                               PMeshRenderContext inherited)
	{
		return inherited;
	}
}
