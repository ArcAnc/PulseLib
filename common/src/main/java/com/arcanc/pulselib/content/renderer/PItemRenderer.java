/**
 * @author ArcAnc
 * Created at: 28.02.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer;


import com.arcanc.pulselib.content.animatable.*;
import com.arcanc.pulselib.content.model.animation.PAnimationEvaluationContext;
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
import com.arcanc.pulselib.util.helpers.PLibHelper;
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

/**
 * Base item renderer that selects stack-specific animation state and submits geometry for the current
 * display context.
 *
 * @param <T> the animatable owner type
 */
public abstract class PItemRenderer<T extends Item & PAnimatable<T>> extends BlockEntityWithoutLevelRenderer implements PRenderer<T>
{
	private final PModelData modelData;
	private final Function<ResourceLocation, RenderType> renderType;
	/**
	 * Creates an item renderer using the model selection and vanilla client renderer dependencies.
	 *
	 * @param data the canonical model and loader selection
	 * @param renderType the render type
	 * @param blockEntityRenderDispatcher the block entity render dispatcher
	 * @param entityModelSet the entity model set
	 */
	public PItemRenderer(PModelData data, Function<ResourceLocation, RenderType> renderType, BlockEntityRenderDispatcher blockEntityRenderDispatcher, EntityModelSet entityModelSet)
	{
		super(blockEntityRenderDispatcher, entityModelSet);
		this.modelData = data;
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
	 * Selects stack-specific animation state and submits the item model for the supplied display context.
	 *
	 * @param stack the item stack supplying render or animation state
	 * @param displayContext the display context
	 * @param poseStack the current model-to-render pose stack
	 * @param buffer the vertex or buffer output
	 * @param packedLight the packed block and sky light coordinates
	 * @param packedOverlay the packed overlay texture coordinates
	 */
	@Override
	public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay)
	{
		if (stack.isEmpty())
			return;
		@SuppressWarnings("unchecked")
		T animatable = stack.getItem() instanceof PItemAnimatable<?> ? (T) stack.getItem() : null;
		if (animatable == null)
			return;
		float partialTick = PLibRenderHelper.mc().isPaused() ? 0f : PLibRenderHelper.mc().getTimer().getGameTimeDeltaPartialTick(false);

		poseStack.pushPose();
		PModelLoader modelLoader = PModelCache.getModelLoader(this.modelData.getModelLoaderId()).orElse(PGltfModelLoader.INSTANCE);
		modelLoader.applyItemTransform(poseStack);
		preSubmit(poseStack, animatable, this :: getRenderType, buffer, packedLight, packedOverlay, partialTick, displayContext);
		trueSubmit(poseStack, animatable, this :: getRenderType, buffer, packedLight, packedOverlay, partialTick, displayContext, stack);
		postSubmit(poseStack, animatable, this :: getRenderType, buffer, packedLight, packedOverlay, partialTick, displayContext);
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

		ItemStack stack = (ItemStack) additionalData[1];

		PAnimationManager<T> manager = PLibHelper.getManager(animatable, AnimManagerKey.of(stack));
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
	protected void perBoneSubmit(T animatable, ItemStack stack, PoseStack poseStack, PBakedBone bone, PPose pose, Collection<PAnimationController<T>> controllers, Map<PAnimationController<T>, MolangParser.Context> molangContexts, Function<ResourceLocation, RenderType> renderType, int packedColor, int packedLight, int packedOverlay, float partialTick, ItemDisplayContext context)
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
}
