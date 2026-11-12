/**
 * @author ArcAnc
 * Created at: 27.02.2026
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
import com.arcanc.pulselib.content.model.animation.PAnimationEvaluationContext;
import com.arcanc.pulselib.content.model.animation.PPose;
import com.arcanc.pulselib.content.model.baked.*;
import com.arcanc.pulselib.content.renderer.base.PEntityRenderState;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.data.gecko.MolangParser;
import com.arcanc.pulselib.platform.PulseServices;
import com.arcanc.pulselib.util.PResourceCache;
import com.arcanc.pulselib.util.helpers.PLibHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.SkullBlock;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Renders entity.
 */
public abstract class PEntityRenderer<T extends Entity & PAnimatable<T>, RS extends EntityRenderState & PEntityRenderState<T>> extends EntityRenderer<T, RS>
	implements PRenderer<T, RS>
{
	/** Canonical EntityRenderer constants, no longer exposed by the native superclass. */
	public static final int LEASH_RENDER_STEPS = 24;
	protected static final float NAMETAG_SCALE = 0.025F;

	private final PModelData modelData;
	private final PRenderStateScope<RS> submission = new PRenderStateScope<>();
	private final Function<Identifier, RenderType> renderType;
	private final Map<String, List<PEntityRenderLayer<T, RS>>> renderLayers = new Object2ObjectOpenHashMap<>();

	/**
	 * Creates an instance of the enclosing type.
	 * @param context the context to use.
	 * @param modelData the model data to use.
	 * @param renderType the render type to use.
	 */
	public PEntityRenderer(EntityRendererProvider.Context context, PModelData modelData, Function<Identifier, RenderType> renderType)
	{
		super(context);
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
	 * Adds the render layer.
	 * @param boneName the bone name to use.
	 * @param renderLayer the render layer to use.
	 */
	public void addRenderLayer(String boneName, PEntityRenderLayer<T, RS> renderLayer)
	{
		this.renderLayers.compute(boneName, (bone, listLayers) ->
		{
			if (listLayers == null)
			{
				listLayers = new ArrayList<>();
			}
			listLayers.add(renderLayer);
			return listLayers;
		});
	}

	/**
	 * Extracts the render state.
	 * @param entity the entity to use.
	 * @param renderState the render state to use.
	 * @param partialTick the partial tick to use.
	 */
	@Override
	public void extractRenderState(T entity, RS renderState, float partialTick)
	{
		super.extractRenderState(entity, renderState, partialTick);
		renderState.extractEntityData(entity, this);
		if (renderState.getAnimatable() instanceof LivingEntity living && renderState instanceof PEntityRenderState.LivingImpl<?> livingEntityRenderState)
		{
			extractLivingRenderData(living, livingEntityRenderState, partialTick);
		}
	}

	/**
	 * Performs the submit operation.
	 * @param renderState the render state to use.
	 * @param poseStack the pose stack to use.
	 * @param submitNodeCollector the submit node collector to use.
	 * @param cameraRenderState the camera render state to use.
	 */
	@Override
	public void submit(RS renderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState)
	{
		super.submit(renderState, poseStack, submitNodeCollector, cameraRenderState);

		preSubmit(poseStack, renderState, cameraRenderState, submitNodeCollector);
		poseStack.pushPose();
		try
		{
			poseStack.mulPose(Axis.YP.rotationDegrees(180));
			trueSubmit(poseStack, renderState, cameraRenderState, submitNodeCollector);
		}
		finally
		{
			poseStack.popPose();
		}
		postSubmit(poseStack, renderState, cameraRenderState, submitNodeCollector);
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
			InstanceAnimationManager.addManager(manager);

			Collection<PAnimationController<T>> controllers = manager.getControllers().values();
			Map<PAnimationController<T>, MolangParser.Context> molangContexts = prepareMolangContexts(
					renderState.getAnimatable(), manager, controllers, renderState.partialTick());
			PPose pose = model.evaluate(controllers, molangContexts, renderState.partialTick());
			HeadRotation headRotation = headRotation(renderState);
			int packedOverlay;

			if (renderState instanceof LivingEntityRenderState livingEntityRenderState)
			{
				packedOverlay = LivingEntityRenderer.getOverlayCoords(livingEntityRenderState, 0.0f);
				headRotation = applyPositioning(livingEntityRenderState, poseStack);
			}
			else
			{
				packedOverlay = OverlayTexture.NO_OVERLAY;
			}

			if (this.renderLayers.isEmpty())
			{
				for (PBakedBone bone : model.bones())
				{
					perBoneSubmit(renderState.getAnimatable(), poseStack, bone, pose, controllers, this.getModelData(renderState), this::getRenderType, -1, renderState.lightCoords, packedOverlay, renderState.partialTick(), headRotation, null, null, null, null, molangContexts);
				}
			}
			else
			{
				Map<String, Matrix4f> entityBonePoses = new Object2ObjectOpenHashMap<>();
				List<DeferredLayerSubmit> deferredLayers = new ArrayList<>();

				for (PBakedBone bone : model.bones())
				{
					perBoneSubmit(renderState.getAnimatable(), poseStack, bone, pose, controllers, this.getModelData(renderState), this::getRenderType, -1, renderState.lightCoords, packedOverlay, renderState.partialTick(), headRotation, null, entityBonePoses, null, deferredLayers, molangContexts);
				}

				for (DeferredLayerSubmit deferredLayer : deferredLayers)
				{
					submitDeferredLayer(renderState, poseStack, submitNodeCollector, cameraRenderState, controllers, molangContexts, entityBonePoses, deferredLayer);
				}
			}
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
	 * @param data the canonical model and loader selection
	 * @param renderType the render type
	 * @param packedColor the packed ARGB vertex color
	 * @param packedLight the packed block and sky light coordinates
	 * @param packedOverlay the packed overlay texture coordinates
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @param headRotation the interpolated head rotation used by bone-specific rendering; may be {@code null}
	 * @param renderLayer the render layer; may be {@code null}
	 * @param entityBonePoses the exact entity bone matrices captured for attached render layers; may be {@code null}
	 * @param layerTransform the additional local transform of an entity render layer; may be {@code null}
	 * @param deferredLayers the render layers waiting for their bound entity bone transforms; may be {@code null}
	 * @param molangContexts the frame-specific Molang context of each controller
	 */
	protected void perBoneSubmit(T animatable,
	                             PoseStack poseStack,
	                             PBakedBone bone,
	                             PPose pose,
	                             Collection<PAnimationController<T>> controllers,
								 PModelData data,
	                             Function<Identifier, RenderType> renderType,
	                             int packedColor,
	                             int packedLight,
	                             int packedOverlay,
	                             float partialTick,
	                             @Nullable HeadRotation headRotation,
	                             @Nullable PEntityRenderLayer<T, RS> renderLayer,
	                             @Nullable Map<String, Matrix4f> entityBonePoses,
	                             @Nullable Matrix4f layerTransform,
	                             @Nullable List<DeferredLayerSubmit> deferredLayers,
	                             Map<PAnimationController<T>, MolangParser.Context> molangContexts)
	{
		PMeshRenderContext inherited = new PMeshRenderContext(
				renderType,
				packedColor,
				packedLight,
				packedOverlay);
		perBoneSubmit(animatable, poseStack, bone, pose, controllers, data, inherited, partialTick,
				headRotation, renderLayer, entityBonePoses, layerTransform, deferredLayers, molangContexts);
	}

	/**
	 * Submits the selected bone and its descendants using the sampled pose and inherited mesh render context.
	 *
	 * @param animatable the owner whose animation state is being processed
	 * @param poseStack the current model-to-render pose stack
	 * @param bone the baked bone being sampled or submitted
	 * @param pose the indexed bone pose
	 * @param controllers the animation controllers in pose composition order
	 * @param data the canonical model and loader selection
	 * @param inherited the render inputs inherited from the parent bone or renderer
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @param headRotation the interpolated head rotation used by bone-specific rendering; may be {@code null}
	 * @param renderLayer the render layer; may be {@code null}
	 * @param entityBonePoses the exact entity bone matrices captured for attached render layers; may be {@code null}
	 * @param layerTransform the additional local transform of an entity render layer; may be {@code null}
	 * @param deferredLayers the render layers waiting for their bound entity bone transforms; may be {@code null}
	 * @param molangContexts the frame-specific Molang context of each controller
	 */
	protected void perBoneSubmit(T animatable,
	                             PoseStack poseStack,
	                             PBakedBone bone,
	                             PPose pose,
	                             Collection<PAnimationController<T>> controllers,
	                             PModelData data,
	                             PMeshRenderContext inherited,
	                             float partialTick,
	                             @Nullable HeadRotation headRotation,
	                             @Nullable PEntityRenderLayer<T, RS> renderLayer,
	                             @Nullable Map<String, Matrix4f> entityBonePoses,
	                             @Nullable Matrix4f layerTransform,
	                             @Nullable List<DeferredLayerSubmit> deferredLayers,
	                             Map<PAnimationController<T>, MolangParser.Context> molangContexts)
	{
		PBakedModel model = renderLayer == null ? selectedModel(animatable) :
				renderLayer.currentModel(animatable);
		if (model == null)
			return;
		int boneIndex = model.boneIndex(bone);
		if (!pose.isVisible(boneIndex))
			return;
		poseStack.pushPose();
		try
		{
			if (renderLayer != null && entityBonePoses != null)
				applyBoundLayerBonePose(poseStack, bone, renderLayer, entityBonePoses, layerTransform);
			poseStack.translate(pose.translation(boneIndex).x(), pose.translation(boneIndex).y(), pose.translation(boneIndex).z());
			poseStack.mulPose(pose.rotation(boneIndex));
			poseStack.scale(pose.scale(boneIndex).x(), pose.scale(boneIndex).y(), pose.scale(boneIndex).z());
			if (bone.name().equals("head") && headRotation != null)
			{
				poseStack.mulPose(Axis.YN.rotationDegrees(headRotation.headYaw()));
				poseStack.mulPose(Axis.XN.rotationDegrees(headRotation.headPitch()));
			}
			if (renderLayer == null && entityBonePoses != null)
				entityBonePoses.put(bone.name(), new Matrix4f(poseStack.last().pose()));

			PMeshRenderContext boneContext = renderLayer == null ?
					resolveBoneForSubmission(animatable, bone, inherited, partialTick) :
					(stateFor(animatable) == null ? renderLayer.resolveBoneRender(animatable, bone, inherited, partialTick) : renderLayer.resolveBoneRender(stateFor(animatable), bone, inherited));

			if (renderLayer == null)
			{
				this.submitBone(animatable, bone, poseStack, data, controllers, boneContext, partialTick);
				List<PEntityRenderLayer<T, RS>> layers = this.renderLayers.get(bone.name());
				if (layers != null)
					for (PEntityRenderLayer<T, RS> layer : layers)
						if ((stateFor(animatable) == null ? layer.shouldRender(animatable) : layer.shouldRender(stateFor(animatable))))
						{
							poseStack.pushPose();
							try
							{
								Vector3f offset = layer.offset();
								poseStack.translate(offset.x(), offset.y(), offset.z());
								poseStack.mulPose(layer.rotation());
								Vector3f scale = layer.scale();
								poseStack.scale(scale.x(), scale.y(), scale.z());
								Matrix4f attachmentTransform = getLayerTransform(bone.name(), poseStack, entityBonePoses);
								if (deferredLayers != null)
									deferredLayers.add(new DeferredLayerSubmit(layer, new Matrix4f(poseStack.last().pose()), attachmentTransform,
											boneContext.color(), boneContext.packedLight(), boneContext.packedOverlay()));
								else
									submitLayer(layer, animatable, poseStack, controllers, molangContexts, boneContext, partialTick, headRotation, entityBonePoses, attachmentTransform);
							}
							finally
							{
								poseStack.popPose();
							}
						}
			}
			else
				this.submitBone(animatable, bone, poseStack, data, controllers, boneContext, partialTick, renderLayer);

			if (!bone.children().isEmpty())
				for (PBakedBone child : bone.children())
					perBoneSubmit(animatable, poseStack, child, pose, controllers, data, boneContext, partialTick,
							headRotation, renderLayer, entityBonePoses, layerTransform, deferredLayers, molangContexts);
		}
		finally
		{
			poseStack.popPose();
		}
	}

	protected void perBoneSubmit(RS renderState,
	                             PoseStack poseStack,
	                             PBakedBone bone,
	                             Collection<PAnimationController<T>> controllers,
	                             PModelData data,
	                             Function<Identifier, RenderType> renderType,
	                             int packedColor,
	                             int packedLight,
	                             int packedOverlay,
	                             SubmitNodeCollector submitNodeCollector,
	                             CameraRenderState cameraRenderState,
	                             @Nullable PEntityRenderLayer<T, RS> renderLayer,
	                             Map<PAnimationController<T>, MolangParser.Context> molangContexts)
	{
		PBakedModel model = renderLayer == null ? renderState.getBakedModel() : renderLayer.getModel(renderState);
		if (model == null)
			return;
		PPose pose = model.evaluate(controllers, molangContexts, renderState.partialTick());
		this.submission.run(renderState, () -> perBoneSubmit(renderState.getAnimatable(), poseStack, bone, pose, controllers,
				data, renderType, packedColor, packedLight, packedOverlay, renderState.partialTick(), headRotation(renderState),
				renderLayer, null, null, null, molangContexts));
	}

	protected void perBoneSubmit(RS renderState,
	                             PoseStack poseStack,
	                             PBakedBone bone,
	                             Collection<PAnimationController<T>> controllers,
	                             PModelData data,
	                             Function<Identifier, RenderType> renderType,
	                             int packedColor,
	                             int packedLight,
	                             int packedOverlay,
	                             SubmitNodeCollector submitNodeCollector,
	                             CameraRenderState cameraRenderState,
	                             @Nullable PEntityRenderLayer<T, RS> renderLayer,
	                             @Nullable Map<String, Matrix4f> entityBonePoses,
	                             @Nullable Matrix4f layerTransform,
	                             Map<PAnimationController<T>, MolangParser.Context> molangContexts)
	{
		PBakedModel model = renderLayer == null ? renderState.getBakedModel() : renderLayer.getModel(renderState);
		if (model == null)
			return;
		PPose pose = model.evaluate(controllers, molangContexts, renderState.partialTick());
		this.submission.run(renderState, () -> perBoneSubmit(renderState.getAnimatable(), poseStack, bone, pose, controllers,
				data, renderType, packedColor, packedLight, packedOverlay, renderState.partialTick(), headRotation(renderState),
				renderLayer, entityBonePoses, layerTransform, null, molangContexts));
	}

	/**
	 * Performs the submit deferred layer operation.
	 * @param renderState the render state to use.
	 * @param poseStack the pose stack to use.
	 * @param submitNodeCollector the submit node collector to use.
	 * @param cameraRenderState the camera render state to use.
	 * @param controllers the controllers to use.
	 * @param molangContexts the molang contexts to use.
	 * @param entityBonePoses the entity bone poses to use.
	 * @param deferredLayer the deferred layer to use.
	 */
	private void submitDeferredLayer(RS renderState,
	                                 PoseStack poseStack,
	                                 SubmitNodeCollector submitNodeCollector,
	                                 CameraRenderState cameraRenderState,
	                                 Collection<PAnimationController<T>> controllers,
	                                 Map<PAnimationController<T>, MolangParser.Context> molangContexts,
	                                 Map<String, Matrix4f> entityBonePoses,
	                                 DeferredLayerSubmit deferredLayer)
	{
		poseStack.pushPose();
		try
		{
			poseStack.last().pose().set(deferredLayer.attachmentPose);
			deferredLayer.layer.submit(
					this,
					renderState,
					poseStack,
					submitNodeCollector,
					cameraRenderState,
					controllers,
					molangContexts,
					deferredLayer.packedColor,
					deferredLayer.packedLight,
					deferredLayer.packedOverlay,
					entityBonePoses,
					deferredLayer.layerTransform);
		}
		finally
		{
			poseStack.popPose();
		}
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
	 * Returns the layer transform.
	 * @param anchorBoneName the anchor bone name to use.
	 * @param poseStack the pose stack to use.
	 * @param entityBonePoses the entity bone poses to use.
	 * @return the value produced by this operation.
	 */
	private Matrix4f getLayerTransform(String anchorBoneName, PoseStack poseStack, @Nullable Map<String, Matrix4f> entityBonePoses)
	{
		if (entityBonePoses == null)
		{
			return new Matrix4f();
		}
		Matrix4f anchorPose = entityBonePoses.get(anchorBoneName);
		if (anchorPose == null)
		{
			return new Matrix4f();
		}
		return new Matrix4f(anchorPose).invert().mul(poseStack.last().pose());
	}

	/**
	 * Applies the bound layer bone pose.
	 * @param poseStack the pose stack to use.
	 * @param bone the bone to use.
	 * @param renderLayer the render layer to use.
	 * @param entityBonePoses the entity bone poses to use.
	 * @param layerTransform the layer transform to use.
	 */
	private void applyBoundLayerBonePose(PoseStack poseStack,
	                                     PBakedBone bone,
	                                     PEntityRenderLayer<T, RS> renderLayer,
	                                     Map<String, Matrix4f> entityBonePoses,
	                                     @Nullable Matrix4f layerTransform)
	{
		String entityBoneName = renderLayer.getBoundEntityBone(bone.name());
		if (entityBoneName == null)
		{
			return;
		}
		Matrix4f entityBonePose = entityBonePoses.get(entityBoneName);
		if (entityBonePose == null)
		{
			return;
		}
		Matrix4f pose = poseStack.last().pose();
		pose.set(entityBonePose);
		pose.translate(-bone.basePosition().x(), -bone.basePosition().y(), -bone.basePosition().z());
		if (layerTransform != null)
		{
			pose.mul(layerTransform);
		}
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
		submitBone(animatable, bone, poseStack, modelData, controllers, renderType, color, packedLight, packedOverlay, partialTick, null);
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
	 * @param renderLayer the render layer; may be {@code null}
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
	                          float partialTick,
	                          @Nullable PEntityRenderLayer<T, RS> renderLayer)
	{
		submitBone(animatable, bone, poseStack, modelData, controllers,
				new PMeshRenderContext(renderType, color, packedLight, packedOverlay), partialTick, renderLayer);
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
		submitBone(animatable, bone, poseStack, modelData, controllers, inherited, partialTick, null);
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
	 * @param renderLayer the render layer; may be {@code null}
	 */
	protected void submitBone(T animatable,
	                          PBakedBone bone,
	                          PoseStack poseStack,
	                          PModelData modelData,
	                          Collection<PAnimationController<T>> controllers,
	                          PMeshRenderContext inherited,
	                          float partialTick,
	                          @Nullable PEntityRenderLayer<T, RS> renderLayer)
	{
		Matrix4f transform = new Matrix4f(poseStack.last().pose());
		for (PBakedMesh mesh : bone.meshes())
		{
			if (mesh.textureReference().isEmpty())
				continue;
			PMeshRenderContext meshContext = renderLayer == null ? resolveMeshForSubmission(animatable, bone, mesh, inherited, partialTick) :
					(stateFor(animatable) == null ? renderLayer.resolveMeshRender(animatable, bone, mesh, inherited, partialTick) : renderLayer.resolveMeshRender(stateFor(animatable), bone, mesh, inherited));
			PMeshRenderMaterial material = PMeshRenderMaterial.resolve(mesh, meshContext);
			RenderType type = material.resolveRenderType(meshContext, PResourceCache.ATLAS_LOCATION);
			var instance = new PRenderQueue.InstanceData(transform, meshContext.color(), material.packedLight(), meshContext.packedOverlay());
			PRenderQueue.submitEntityMesh(type, material.mesh(), meshContext.deformation(), instance);
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

	protected void submitBone(RS renderState,
	                          PBakedBone bone,
	                          PoseStack poseStack,
	                          PModelData modelData,
	                          Collection<PAnimationController<T>> controllers,
	                          Function<Identifier, RenderType> renderType,
	                          int color,
	                          int packedLight,
	                          int packedOverlay,
	                          @Nullable PEntityRenderLayer<T, RS> renderLayer)
	{
		this.submission.run(renderState, () -> submitBone(renderState.getAnimatable(), bone, poseStack, modelData, controllers, renderType, color, packedLight, packedOverlay, renderState.partialTick(), renderLayer));
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
	 * Performs the setup rotations operation.
	 * @param renderState the render state to use.
	 * @param poseStack the pose stack to use.
	 * @param yBodyRot the y body rot to use.
	 * @param scale the scale to use.
	 */
	protected void setupRotations(LivingEntityRenderState renderState, PoseStack poseStack, float yBodyRot, float scale)
	{
		if (renderState.isFullyFrozen)
		{
			yBodyRot += (float)(Math.cos(Mth.floor(renderState.ageInTicks) * 3.25F) * Math.PI * 0.4F);
		}

		if (!renderState.hasPose(Pose.SLEEPING))
		{
			poseStack.mulPose(Axis.YN.rotationDegrees(yBodyRot));
		}

		float deathTime = renderState.deathTime;
		if (deathTime > 0)
		{
			float f = (deathTime - 1.0F) / 20.0F * 1.6F;
			f = Mth.sqrt(f);
			if (f > 1.0F)
			{
				f = 1.0F;
			}

			poseStack.mulPose(Axis.ZP.rotationDegrees(f * 90));
		}
		else if (renderState.isAutoSpinAttack)
		{
			poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F - renderState.xRot));
			poseStack.mulPose(Axis.YP.rotationDegrees(renderState.ageInTicks * -75.0F));
		}
		else if (renderState.hasPose(Pose.SLEEPING))
		{
			Direction direction = renderState.bedOrientation;
			float f1 = direction != null ? sleepDirectionToRotation(direction) : yBodyRot;
			poseStack.mulPose(Axis.YP.rotationDegrees(f1));
			poseStack.mulPose(Axis.ZP.rotationDegrees(90));
			poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
		}
		else if (renderState.isUpsideDown)
		{
			poseStack.translate(0.0F, (renderState.boundingBoxHeight + 0.1F) / scale, 0.0F);
			poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
		}
	}

	/**
	 * Performs the sleep direction to rotation operation.
	 * @param facing the facing to use.
	 * @return the value produced by this operation.
	 */
	private float sleepDirectionToRotation(Direction facing)
	{
		return switch (facing)
		{
			case SOUTH -> 90.0F;
			case NORTH -> 270.0F;
			case EAST -> 180.0F;
			default -> 0.0F;
		};
	}

	/**
	 * Extracts the living render data.
	 * @param entity the entity to use.
	 * @param state the state to use.
	 * @param partialTick the partial tick to use.
	 */
	protected void extractLivingRenderData(LivingEntity entity, PEntityRenderState.LivingImpl<?> state, float partialTick)
	{
		// Vanilla md copy/paste
		float headYRot = Mth.rotLerp(partialTick, entity.yHeadRotO, entity.yHeadRot);
		float headXRot = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
		state.bodyRot = solveBodyRot(entity, headYRot, partialTick);
		state.yRot = Mth.wrapDegrees(headYRot - state.bodyRot);
		state.xRot = entity.getXRot(partialTick);
		state.isUpsideDown = this.isEntityUpsideDown(entity);

		if (state.isUpsideDown)
		{
			state.yRot *= -1f;
			state.xRot *= -1f;
		}

		state.extractHeadData(state.yRot, headXRot);

		float walkAnimationPos = 0.0f;
		float walkAnimationSpeed = 0.0f;

		if (!entity.isPassenger() && entity.isAlive())
		{
			walkAnimationPos = entity.walkAnimation.position(partialTick);
			walkAnimationSpeed = entity.walkAnimation.speed(partialTick);
		}

		state.walkAnimationPos = walkAnimationPos;
		state.walkAnimationSpeed = walkAnimationSpeed;

		float wornHeadAnimationPos = walkAnimationPos;

		if (entity.getVehicle() instanceof LivingEntity vehicle)
		{
			wornHeadAnimationPos = vehicle.walkAnimation.position(partialTick);
		}

		state.wornHeadAnimationPos = wornHeadAnimationPos;

		state.scale = entity.getScale();
		state.ageScale = entity.getAgeScale();
		state.pose = entity.getPose();

		state.bedOrientation = entity.getBedOrientation();

		if (state.bedOrientation != null)
		{
			state.eyeHeight = entity.getEyeHeight(Pose.STANDING);
		}

		state.isFullyFrozen = entity.isFullyFrozen();
		state.isBaby = entity.isBaby();
		state.isInWater = entity.isInWater();
		state.isAutoSpinAttack = entity.isAutoSpinAttack();
		state.ticksSinceKineticHitFeedback = entity.getTicksSinceLastKineticHitFeedback(partialTick);
		state.hasRedOverlay = entity.hurtTime > 0 || entity.deathTime > 0;
		ItemStack headItem = entity.getItemBySlot(EquipmentSlot.HEAD);

		SkullBlock.Type wornHeadType = null;
		ResolvableProfile wornHeadProfile = null;

		if (headItem.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof AbstractSkullBlock skullBlock)
		{
			wornHeadType = skullBlock.getType();
			wornHeadProfile = headItem.get(DataComponents.PROFILE);
			state.headItem.clear();
		}
		else
		{
			// TODO: add hat rendering
			/*if (!HumanoidArmorLayer.shouldRender(headItem, EquipmentSlot.HEAD))
				this.itemModelResolver.updateForLiving(state.headItem, headItem, ItemDisplayContext.HEAD, entity);
			else*/
			state.headItem.clear();
		}

		state.wornHeadType = wornHeadType;
		state.wornHeadProfile = wornHeadProfile;

		state.deathTime = entity.deathTime > 0 ? entity.deathTime + partialTick : 0.0f;
		Minecraft minecraft = Minecraft.getInstance();
		state.isInvisibleToPlayer = state.isInvisible && entity.isInvisibleTo(minecraft.player);
	}

	/**
	 * Performs the solve body rot operation.
	 * @param entity the entity to use.
	 * @param headRot the head rot to use.
	 * @param partialTick the partial tick to use.
	 * @return the value produced by this operation.
	 */
	private float solveBodyRot(LivingEntity entity, float headRot, float partialTick)
	{
		if (entity.isPassenger() && entity.getVehicle() instanceof LivingEntity riding && PulseServices.PLATFORM.shouldRiderSit(riding))
		{
			float bodyRot = Mth.rotLerp(partialTick, riding.yBodyRotO, riding.yBodyRot);
			float maxHeadDiff = 85.0F;
			float headDiff = Mth.clamp(Mth.wrapDegrees(headRot - bodyRot), -maxHeadDiff, maxHeadDiff);
			bodyRot = headRot - headDiff;
			if (Math.abs(headDiff) > 50.0F)
			{
				bodyRot += headDiff * 0.2F;
			}

			return bodyRot;
		}
		else
		{
			return Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
		}
	}

	/**
	 * Determines whether entity upside down.
	 * @param mob the mob to use.
	 * @return the value produced by this operation.
	 */
	protected boolean isEntityUpsideDown(LivingEntity mob)
	{
		Component customName = mob.getCustomName();
		return customName != null && isUpsideDownName(customName.getString());
	}

	/**
	 * Determines whether upside down name.
	 * @param name the name to use.
	 * @return the value produced by this operation.
	 */
	protected static boolean isUpsideDownName(String name)
	{
		return "Dinnerbone".equals(name) || "Grumm".equals(name);
	}

	/**
	 * Provides support for deferred layer submit.
    */
	protected final class DeferredLayerSubmit
	{
		private final PEntityRenderLayer<T, RS> layer;
		private final Matrix4f attachmentPose;
		private final Matrix4f layerTransform;
		private final int packedColor;
		private final int packedLight;
		private final int packedOverlay;

		/**
		 * Creates an instance of the enclosing type.
		 * @param layer the layer to use.
		 * @param attachmentPose the attachment pose to use.
		 * @param layerTransform the layer transform to use.
		 * @param packedColor the packed color to use.
		 * @param packedLight the packed light to use.
		 * @param packedOverlay the packed overlay to use.
		 */
		private DeferredLayerSubmit(PEntityRenderLayer<T, RS> layer,
		                            Matrix4f attachmentPose,
		                            Matrix4f layerTransform,
		                            int packedColor,
		                            int packedLight,
		                            int packedOverlay)
		{
			this.layer = layer;
			this.attachmentPose = attachmentPose;
			this.layerTransform = layerTransform;
			this.packedColor = packedColor;
			this.packedLight = packedLight;
			this.packedOverlay = packedOverlay;
		}
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

	/** Captured living-entity body positioning; no live entity state is read at submit time. */
	protected HeadRotation applyPositioning(LivingEntityRenderState state, PoseStack poseStack)
	{
		if (state.hasPose(Pose.SLEEPING) && state.bedOrientation != null)
		{
			float offset = state.eyeHeight - 0.1f;
			poseStack.translate(-state.bedOrientation.getStepX() * offset, 0, -state.bedOrientation.getStepZ() * offset);
		}
		float scale = state.scale;
		poseStack.scale(scale, scale, scale);
		setupRotations(state, poseStack, state.bodyRot, scale);
		return state instanceof PEntityRenderState.LivingImpl<?> living ?
				new HeadRotation(living.getHeadYRot(), living.getHeadXRot()) : new HeadRotation(state.yRot, state.xRot);
	}

	private @Nullable HeadRotation headRotation(RS state)
	{
		return state instanceof PEntityRenderState.LivingImpl<?> living ?
				new HeadRotation(living.getHeadYRot(), living.getHeadXRot()) : null;
	}

	/** Canonical interpolated head orientation passed through the whole bone hierarchy. */
	public record HeadRotation(float headYaw, float headPitch)
	{
	}

	/** Entity textures are atlas-backed; per-mesh overrides are selected by the material hooks. */
	public @Nullable Identifier getTextureLocation(T entity)
	{
		return null;
	}

	private void submitLayer(PEntityRenderLayer<T, RS> layer, T owner, PoseStack poses,
	                         Collection<PAnimationController<T>> controllers, Map<PAnimationController<T>, MolangParser.Context> contexts,
	                         PMeshRenderContext inherited, float partialTick, @Nullable HeadRotation head,
	                         @Nullable Map<String, Matrix4f> entityPoses, @Nullable Matrix4f transform)
	{
		RS state = stateFor(owner);
		if (state == null)
			layer.submit(this, owner, poses, controllers, contexts, inherited.color(), inherited.packedLight(), inherited.packedOverlay(), partialTick, head, entityPoses, transform);
		else
			layer.submit(this, state, poses, controllers, contexts, inherited.color(), inherited.packedLight(), inherited.packedOverlay(), entityPoses, transform);
	}

	private @Nullable RS stateFor(T owner)
	{
		RS state = this.submission.current();
		return state != null && state.getAnimatable() == owner ? state : null;
	}
}
