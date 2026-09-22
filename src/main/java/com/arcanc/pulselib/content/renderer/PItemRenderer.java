/**
 * @author ArcAnc
 * Created at: 28.02.2026
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
import com.arcanc.pulselib.content.model.animation.PPose;
import com.arcanc.pulselib.content.model.baked.*;
import com.arcanc.pulselib.content.model.deformer.gpu.PGpuDeformerBuffers;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.data.PModelLoader;
import com.arcanc.pulselib.data.gecko.MolangParser;
import com.arcanc.pulselib.data.gltf.PGltfModelLoader;
import com.arcanc.pulselib.util.PModelCache;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.PResourceCache;
import com.arcanc.pulselib.util.helpers.PLibRenderHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Map;
import java.util.function.Function;

public abstract class PItemRenderer<T extends Item & PAnimatable<T>> extends BlockEntityWithoutLevelRenderer implements PRenderer<T>
{
	private final PModelData modelData;
	private final Function<ResourceLocation, RenderType> renderType;
	public PItemRenderer(PModelData data, Function<ResourceLocation, RenderType> renderType, BlockEntityRenderDispatcher blockEntityRenderDispatcher, EntityModelSet entityModelSet)
	{
		super(blockEntityRenderDispatcher, entityModelSet);
		this.modelData = data;
		this.renderType = renderType;
	}
	
	@Override
	public PModelData getModelData(T animatable)
	{
		return this.modelData;
	}
	
	@Override
	public @Nullable PBakedModel getModel(T animatable)
	{
		return getModelData(animatable).getModel();
	}
	
	@Override
	public RenderType getRenderType(ResourceLocation texture)
	{
		return this.renderType.apply(texture);
	}
	
	@Override
	public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay)
	{
		@SuppressWarnings("unchecked")
		T animatable = stack.getItem() instanceof PAnimatable ? (T) stack.getItem() : null;
		if (animatable == null)
			return;
		float partialTick = PLibRenderHelper.mc().isPaused() ? 0f : PLibRenderHelper.mc().getTimer().getGameTimeDeltaPartialTick(false);
		
		poseStack.pushPose();
		PModelLoader modelLoader = PModelCache.getModelLoader(this.modelData.getModelFormat()).orElse(PGltfModelLoader.INSTANCE);
		modelLoader.applyItemTransform(poseStack);
		preSubmit(poseStack, animatable, this :: getRenderType, buffer, packedLight, packedOverlay, partialTick, displayContext);
		trueSubmit(poseStack, animatable, this :: getRenderType, buffer, packedLight, packedOverlay, partialTick, displayContext, stack);
		postSubmit(poseStack, animatable, this :: getRenderType, buffer, packedLight, packedOverlay, partialTick, displayContext);
		poseStack.popPose();
	}
	
	@Override
	public void preSubmit(PoseStack poseStack, T animatable, Function<ResourceLocation, RenderType> renderType, MultiBufferSource bufferSource, int packedLight, int packedOverlay, float partialTick, @Nullable Object... additionalData)
	{
	}
	
	@Override
	public void trueSubmit(PoseStack poseStack, T animatable, Function<ResourceLocation, RenderType> renderType, MultiBufferSource bufferSource, int packedLight, int packedOverlay, float partialTick, @Nullable Object... additionalData)
	{
		PBakedModel model = this.getModelData(animatable).getModel();
		if (model == null)
			return;
		
		ItemStack stack = (ItemStack) additionalData[1];
		
		PAnimationManager<T> manager = animatable.getAnimationManager(AnimManagerKey.of(stack));
		manager.bindModel(model);
		
		Collection<PAnimationController<T>> controllers = manager.getControllers().values();
		Map<PAnimationController<T>, MolangParser.Context> molangContexts = prepareMolangContexts(animatable, manager.key(), controllers, partialTick);
		ItemDisplayContext context = (ItemDisplayContext)additionalData[0];
		if (context == ItemDisplayContext.GUI)
		{
			PMeshRenderContext inherited = new PMeshRenderContext(PRenderTypes.RenderTypeProvider :: trianglesGui, -1, packedLight, packedOverlay);
			model.instantDraw(
					poseStack,
					getModelData(animatable),
					controllers,
					molangContexts,
					(bone, mesh, inheritedContext) -> resolveGuiMeshRender(animatable, stack, bone, mesh, inheritedContext, partialTick),
					inherited,
					partialTick);
			return;
		}
		PPose pose = model.evaluate(controllers, molangContexts, partialTick);
		model.bones().forEach(bone -> perBoneSubmit(animatable, stack, poseStack, bone, pose, controllers, molangContexts, renderType, -1, packedLight, packedOverlay, partialTick, context));
	}
	
	@Override
	public void postSubmit(PoseStack poseStack, T animatable, Function<ResourceLocation, RenderType> renderType, MultiBufferSource bufferSource, int packedLight, int packedOverlay, float partialTick, @Nullable Object... additionalData)
	{
	
	}
	
	protected void perBoneSubmit(T animatable, ItemStack stack, PoseStack poseStack, PBakedBone bone, PPose pose, Collection<PAnimationController<T>> controllers, Map<PAnimationController<T>, MolangParser.Context> molangContexts, Function<ResourceLocation, RenderType> renderType, int packedColor, int packedLight, int packedOverlay, float partialTick, ItemDisplayContext context)
	{
		PMeshRenderContext inherited = new PMeshRenderContext(
				renderType,
				packedColor,
				packedLight,
				packedOverlay);
		perBoneSubmit(animatable, stack, poseStack, bone, pose, controllers, molangContexts, inherited, partialTick, context);
	}

	protected void perBoneSubmit(T animatable, ItemStack stack, PoseStack poseStack, PBakedBone bone, PPose pose, Collection<PAnimationController<T>> controllers, Map<PAnimationController<T>, MolangParser.Context> molangContexts, PMeshRenderContext inherited, float partialTick, ItemDisplayContext context)
	{
		PModelData data = this.getModelData(animatable);
		int boneIndex = data.getModel().boneIndex(bone);
		poseStack.pushPose();
		poseStack.translate(pose.translation(boneIndex).x(), pose.translation(boneIndex).y(), pose.translation(boneIndex).z());
		poseStack.mulPose(pose.rotation(boneIndex));
		poseStack.scale(pose.scale(boneIndex).x(), pose.scale(boneIndex).y(), pose.scale(boneIndex).z());
		
		PMeshRenderContext boneContext = resolveBoneRender(animatable, stack, context, bone, inherited, partialTick);
		this.submitBone(animatable, stack, bone, poseStack, data, controllers, boneContext, partialTick, context);
		
		if (!bone.children().isEmpty())
			bone.children().forEach(child -> perBoneSubmit(animatable, stack, poseStack, child, pose, controllers, molangContexts, boneContext, partialTick, context));
		
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
					query("anim_time", controller.getInterpolatedTime(partialTick)).
					randomSeed(key.key());
			populateMolangContext(animatable, controller, context, partialTick);
			contexts.put(controller, context);
		}
		return contexts;
	}

	protected void populateMolangContext(T animatable,
	                                    PAnimationController<T> controller,
	                                    MolangParser.Context context,
	                                    float partialTick)
	{
	}
	
	protected void submitBone(T animatable,
	                          ItemStack stack,
	                          PBakedBone bone,
	                          PoseStack poseStack,
	                          PModelData modelData,
	                          Collection<PAnimationController<T>> controllers,
	                          Function<ResourceLocation, RenderType> renderType,
	                          int color,
	                          int packedLight,
	                          int packedOverlay,
	                          float partialTick,
	                          ItemDisplayContext context)
	{
		submitBone(animatable, stack, bone, poseStack, modelData, controllers,
				new PMeshRenderContext(renderType, color, packedLight, packedOverlay), partialTick, context);
	}

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
		Matrix4f matrix4fstack = new Matrix4f(poseStack.last().pose());
		
		bone.meshes().forEach(mesh ->
		{
			if (mesh.textureReference().isEmpty())
				return;
			
			PMeshRenderContext meshContext = resolveMeshRender(animatable, stack, context, bone, mesh, inherited, partialTick);
			PMeshRenderMaterial material = PMeshRenderMaterial.resolve(mesh, meshContext);
			
			RenderType type = material.resolveRenderType(meshContext, PResourceCache.ATLAS_LOCATION);
			
			PGpuDeformerBuffers.Submission deformation = PGpuDeformerBuffers.submit(meshContext.deformation());
			PInstanceHeader instance = new PInstanceHeader(matrix4fstack, meshContext.color(), material.packedLight(), meshContext.packedOverlay(),
					deformation.operationOffset(), deformation.valueOffset(), deformation.operationCount());
			if (meshContext.deformation() == null || meshContext.deformation().stack().isEmpty())
				PRenderQueue.submitItem(context, type, material.mesh().geometry(), instance);
			else if (deformation.enabled())
				PRenderQueue.submitItem(context, type,
						PGpuDeformedMeshBuffers.resolve(material.mesh(), meshContext.deformation().subdivisionLevel()), instance);
			else
				PRenderQueue.submitItem(context, type, PDeformedMeshBuffers.resolve(material.mesh(), meshContext.deformation()), instance);
		});
	}

	private PMeshRenderContext resolveGuiMeshRender(T animatable,
	                                                ItemStack stack,
	                                                PBakedBone bone,
	                                                PBakedMesh mesh,
	                                                PMeshRenderContext inherited,
	                                                float partialTick)
	{
		Deque<PBakedBone> hierarchy = new ArrayDeque<>();
		for (PBakedBone current = bone; current != null; current = current.parent())
			hierarchy.push(current);
		PMeshRenderContext boneContext = inherited;
		while (!hierarchy.isEmpty())
			boneContext = resolveBoneRender(animatable, stack, ItemDisplayContext.GUI, hierarchy.pop(), boneContext, partialTick);
		return resolveMeshRender(animatable, stack, ItemDisplayContext.GUI, bone, mesh, boneContext, partialTick);
	}

	protected PMeshRenderContext resolveBoneRender(T animatable,
	                                               ItemStack stack,
	                                               ItemDisplayContext context,
	                                               PBakedBone bone,
	                                               PMeshRenderContext inherited,
	                                               float partialTick)
	{
		return inherited;
	}
	
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
}
