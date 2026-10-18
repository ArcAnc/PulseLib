/**
 * @author ArcAnc
 * Created at: 27.02.2026
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
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.data.gecko.MolangParser;
import com.arcanc.pulselib.platform.PulseServices;
import com.arcanc.pulselib.util.PResourceCache;
import com.arcanc.pulselib.util.helpers.PLibHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Base entity renderer that samples PulseLib animation, submits bone meshes and renders bound model layers.
 *
 * @param <T> the animatable owner type
 */
public abstract class PEntityRenderer<T extends Entity & PAnimatable<T>> extends EntityRenderer<T>
	implements PRenderer<T>
{
	private final PModelData modelData;
	private final Function<ResourceLocation, RenderType> renderType;
	private final Map<String, List<PEntityRenderLayer<T>>> renderLayers = new Object2ObjectOpenHashMap<>();

	/**
	 * Creates an entity renderer using the loader context, model selection and pipeline resolver.
	 *
	 * @param context the vanilla entity renderer construction context
	 * @param modelData the canonical model and loader selection
	 * @param renderType the render type
	 */
	public PEntityRenderer(EntityRendererProvider.Context context, PModelData modelData, Function<ResourceLocation, RenderType> renderType)
	{
		super(context);
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
	 * Attaches an additional model render layer to the named entity bone.
	 *
	 * @param boneName the animation bone name
	 * @param renderLayer the render layer
	 */
	public void addRenderLayer(String boneName, PEntityRenderLayer<T> renderLayer)
	{
		this.renderLayers.compute(boneName, (bone, listLayers) ->
		{
			if (listLayers == null)
				listLayers = new ArrayList<>();
			listLayers.add(renderLayer);
			return listLayers;
		});
	}

	/**
	 * Samples entity animation, submits model and bound layer geometry, and preserves vanilla entity-render
	 * integration.
	 *
	 * @param entity the entity being animated or rendered
	 * @param entityYaw the entity yaw in degrees
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @param poseStack the current model-to-render pose stack
	 * @param bufferSource the vanilla vertex buffer source
	 * @param packedLight the packed block and sky light coordinates
	 */
	@Override
	public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight)
	{
		try (var scope = PRenderContexts.enterEntity())
		{
			try
			{
				super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

				int packedOverlay = entity instanceof LivingEntity living ? LivingEntityRenderer.getOverlayCoords(living, 0.0f) : OverlayTexture.NO_OVERLAY;
				preSubmit(poseStack, entity, this :: getRenderType, bufferSource, packedLight, packedOverlay, partialTick);
				poseStack.pushPose();
				try
				{
					poseStack.mulPose(Axis.YP.rotationDegrees(180));
					trueSubmit(poseStack, entity, this :: getRenderType, bufferSource, packedLight, packedOverlay, partialTick);
				}
				finally
				{
					poseStack.popPose();
				}
				postSubmit(poseStack, entity, this :: getRenderType, bufferSource, packedLight, packedOverlay, partialTick);
			}
			catch (Throwable exception)
			{
				if (scope != null)
					scope.discard();
				throw exception;
			}
		}
	}

	/**
	 * Returns {@code null} because textures are resolved separately for each baked mesh.
	 *
	 * @param entity the entity being animated or rendered
	 * @return {@code null}; each baked mesh supplies its own texture selection
	 */
	@Override
	public @Nullable ResourceLocation getTextureLocation(T entity)
	{
		return null;
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
		InstanceAnimationManager.addManager(manager);

		Collection<PAnimationController<T>> controllers = manager.getControllers().values();
		Map<PAnimationController<T>, MolangParser.Context> molangContexts = prepareMolangContexts(animatable, manager.key(), controllers, partialTick);
		PPose pose = model.evaluate(controllers, molangContexts, partialTick);
		HeadRotation headRotation = null;
		if (animatable instanceof LivingEntity livingEntity)
			headRotation = applyPositioning(livingEntity, poseStack, partialTick);

		if (this.renderLayers.isEmpty())
		{
			for (PBakedBone bone : model.bones())
				perBoneSubmit(animatable, poseStack, bone, pose, controllers, this.getModelData(animatable), renderType, - 1, packedLight, packedOverlay, partialTick, headRotation, null, null, null, null, molangContexts);
		}
		else
		{
			Map<String, Matrix4f> entityBonePoses = new Object2ObjectOpenHashMap<>();
			List<DeferredLayerSubmit> deferredLayers = new ArrayList<>();

			for (PBakedBone bone : model.bones())
				perBoneSubmit(animatable, poseStack, bone, pose, controllers, this.getModelData(animatable), renderType, -1, packedLight, packedOverlay, partialTick, headRotation, null, entityBonePoses, null, deferredLayers, molangContexts);

			for (DeferredLayerSubmit deferredLayer : deferredLayers)
				submitDeferredLayer(animatable, poseStack, controllers, molangContexts, partialTick, headRotation, entityBonePoses, deferredLayer);
		}
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
	                             Function<ResourceLocation, RenderType> renderType,
	                             int packedColor,
	                             int packedLight,
	                             int packedOverlay,
	                             float partialTick,
	                             @Nullable HeadRotation headRotation,
	                             @Nullable PEntityRenderLayer<T> renderLayer,
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
	                             @Nullable PEntityRenderLayer<T> renderLayer,
	                             @Nullable Map<String, Matrix4f> entityBonePoses,
	                             @Nullable Matrix4f layerTransform,
	                             @Nullable List<DeferredLayerSubmit> deferredLayers,
	                             Map<PAnimationController<T>, MolangParser.Context> molangContexts)
	{
		int boneIndex = data.getModel().boneIndex(bone);
		poseStack.pushPose();
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
				resolveBoneRender(animatable, bone, inherited, partialTick) :
				renderLayer.resolveBoneRender(animatable, bone, inherited, partialTick);

		if (renderLayer == null)
		{
			this.submitBone(animatable, bone, poseStack, data, controllers, boneContext, partialTick);
			List<PEntityRenderLayer<T>> layers = this.renderLayers.get(bone.name());
			if (layers != null)
				for (PEntityRenderLayer<T> layer : layers)
					if (layer.shouldRender(animatable))
					{
						poseStack.pushPose();
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
							layer.submit(this, animatable, poseStack, controllers, molangContexts,
									boneContext.color(), boneContext.packedLight(), boneContext.packedOverlay(), partialTick,
									headRotation, entityBonePoses, attachmentTransform);

						poseStack.popPose();
					}
		}
		else
			this.submitBone(animatable, bone, poseStack, data, controllers, boneContext, partialTick, renderLayer);

		if (!bone.children().isEmpty())
			for (PBakedBone child : bone.children())
				perBoneSubmit(animatable, poseStack, child, pose, controllers, data, boneContext, partialTick,
						headRotation, renderLayer, entityBonePoses, layerTransform, deferredLayers, molangContexts);

		poseStack.popPose();
	}

	private void submitDeferredLayer(T animatable,
	                                 PoseStack poseStack,
	                                 Collection<PAnimationController<T>> controllers,
	                                 Map<PAnimationController<T>, MolangParser.Context> molangContexts,
	                                 float partialTick,
									 @Nullable HeadRotation headRotation,
	                                 Map<String, Matrix4f> entityBonePoses,
	                                 DeferredLayerSubmit deferredLayer)
	{
		poseStack.pushPose();
		poseStack.last().pose().set(deferredLayer.attachmentPose);
		deferredLayer.layer.submit(
				this,
				animatable,
				poseStack,
				controllers,
				molangContexts,
				deferredLayer.packedColor,
				deferredLayer.packedLight,
				deferredLayer.packedOverlay,
				partialTick,
				headRotation,
				entityBonePoses,
				deferredLayer.layerTransform);
		poseStack.popPose();
	}

	private Matrix4f getLayerTransform(String anchorBoneName, PoseStack poseStack, @Nullable Map<String, Matrix4f> entityBonePoses)
	{
		if (entityBonePoses == null)
			return new Matrix4f();
		Matrix4f anchorPose = entityBonePoses.get(anchorBoneName);
		if (anchorPose == null)
			return new Matrix4f();
		return new Matrix4f(anchorPose).invert().mul(poseStack.last().pose());
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

	private void applyBoundLayerBonePose(PoseStack poseStack,
	                                     PBakedBone bone,
	                                     PEntityRenderLayer<T> renderLayer,
	                                     Map<String, Matrix4f> entityBonePoses,
	                                     @Nullable Matrix4f layerTransform)
	{
		String entityBoneName = renderLayer.getBoundEntityBone(bone.name());
		if (entityBoneName == null)
			return;
		Matrix4f entityBonePose = entityBonePoses.get(entityBoneName);
		if (entityBonePose == null)
			return;
		Matrix4f pose = poseStack.last().pose();
		pose.set(entityBonePose);
		pose.translate(-bone.basePosition().x(), -bone.basePosition().y(), -bone.basePosition().z());
		if (layerTransform != null)
			pose.mul(layerTransform);
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
	                          Function<ResourceLocation, RenderType> renderType,
	                          int color,
	                          int packedLight,
	                          int packedOverlay,
	                          float partialTick,
	                          @Nullable PEntityRenderLayer<T> renderLayer)
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
	                          @Nullable PEntityRenderLayer<T> renderLayer)
	{
		Matrix4f matrix4fstack = new Matrix4f(poseStack.last().pose());

		for (PBakedMesh mesh : bone.meshes())
		{
			if (mesh.textureReference().isEmpty())
				continue;

			PMeshRenderContext meshContext = renderLayer == null ?
					resolveMeshRender(animatable, bone, mesh, inherited, partialTick) :
					renderLayer.resolveMeshRender(animatable, bone, mesh, inherited, partialTick);
			PMeshRenderMaterial material = PMeshRenderMaterial.resolve(mesh, meshContext);

			RenderType type = material.resolveRenderType(meshContext, PResourceCache.ATLAS_LOCATION);

			PGpuDeformerBuffers.Submission deformation = PGpuDeformerBuffers.submit(meshContext.deformation());
			PInstanceHeader instance = new PInstanceHeader(matrix4fstack, meshContext.color(), material.packedLight(), meshContext.packedOverlay(),
					deformation.operationOffset(), deformation.valueOffset(), deformation.operationCount());
			if (meshContext.deformation() == null || meshContext.deformation().stack().isEmpty())
				PRenderQueue.submitEntityMesh(type, material.mesh().geometry(), instance);
			else if (deformation.enabled())
				PRenderQueue.submitEntityMesh(type,
						PGpuDeformedMeshBuffers.resolve(material.mesh(), meshContext.deformation().subdivisionLevel()), instance);
			else
				PRenderQueue.submitEntityMesh(type, PDeformedMeshBuffers.resolve(material.mesh(), meshContext.deformation()), instance);
		}
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

	/**
	 * Applies entity positioning and returns the interpolated head rotation inputs.
	 *
	 * @param entity the entity being animated or rendered
	 * @param poseStack the current model-to-render pose stack
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @return the interpolated relative head yaw and pitch
	 */
	protected HeadRotation applyPositioning(LivingEntity entity, PoseStack poseStack, float partialTick)
	{
		/// Vanilla copy paste
		boolean shouldSit = entity.isPassenger() && (entity.getVehicle() != null && PulseServices.PLATFORM.shouldRiderSit(entity.getVehicle()));
		float bodyRot = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
		float headRot = Mth.rotLerp(partialTick, entity.yHeadRotO, entity.yHeadRot);
		float diffAngle = headRot - bodyRot;
		if (shouldSit && entity.getVehicle() instanceof LivingEntity livingentity)
		{
			bodyRot = Mth.rotLerp(partialTick, livingentity.yBodyRotO, livingentity.yBodyRot);
			diffAngle = headRot - bodyRot;
			float clampedAngle = Mth.clamp(Mth.wrapDegrees(diffAngle), -85f, 85f);

			bodyRot = headRot - clampedAngle;
			if (clampedAngle * clampedAngle > 2500f)
				bodyRot += clampedAngle * 0.2f;

			diffAngle = headRot - bodyRot;
		}

		if (LivingEntityRenderer.isEntityUpsideDown(entity))
			diffAngle *= -1.0F;

		diffAngle = Mth.wrapDegrees(diffAngle);
		if (entity.hasPose(Pose.SLEEPING))
		{
			Direction direction = entity.getBedOrientation();
			if (direction != null)
			{
				float f3 = entity.getEyeHeight(Pose.STANDING) - 0.1F;
				poseStack.translate(-direction.getStepX() * f3, 0.0F, -direction.getStepZ() * f3);
			}
		}

		float entityScale = entity.getScale();
		poseStack.scale(entityScale, entityScale, entityScale);
		float age = entity.tickCount + partialTick;
		this.setupRotations(entity, poseStack, age, bodyRot, partialTick, entityScale);
		float headPitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
		return new HeadRotation(diffAngle, headPitch);
	}

	/**
	 * Applies living-entity body, death, sleeping and movement rotations to the render pose.
	 *
	 * @param entity the entity being animated or rendered
	 * @param poseStack the current model-to-render pose stack
	 * @param bob the bob
	 * @param yBodyRot the interpolated body yaw in degrees
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @param scale the model scale factor
	 */
	protected void setupRotations(LivingEntity entity, PoseStack poseStack, float bob, float yBodyRot, float partialTick, float scale)
	{
		if (entity.isFullyFrozen())
			yBodyRot += (float) (Math.cos(entity.tickCount * 3.25f) * Math.PI * 0.4F);

		if (!entity.hasPose(Pose.SLEEPING))
			poseStack.mulPose(Axis.YN.rotationDegrees(yBodyRot));

		if (entity.deathTime > 0)
		{
			float f = ((float)entity.deathTime + partialTick - 1.0F) / 20.0F * 1.6F;
			f = Mth.sqrt(f);
			if (f > 1.0F)
				f = 1.0F;

			poseStack.mulPose(Axis.ZP.rotationDegrees(f * 90));
		}
		else if (entity.isAutoSpinAttack())
		{
			poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F - entity.getXRot()));
			poseStack.mulPose(Axis.YP.rotationDegrees((entity.tickCount + partialTick) * -75.0F));
		}
		else if (entity.hasPose(Pose.SLEEPING))
		{
			Direction direction = entity.getBedOrientation();
			float f1 = direction != null ? sleepDirectionToRotation(direction) : yBodyRot;
			poseStack.mulPose(Axis.YP.rotationDegrees(f1));
			poseStack.mulPose(Axis.ZP.rotationDegrees(90));
			poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
		}
		else if (LivingEntityRenderer.isEntityUpsideDown(entity))
		{
			poseStack.translate(0.0F, (entity.getBbHeight() + 0.1F) / scale, 0.0F);
			poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
		}
	}

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
	 * Interpolated head yaw and pitch used while submitting entity bones.
	 *
	 * @param headYaw the head yaw
	 * @param headPitch the head pitch in degrees
	 */
	public record HeadRotation(float headYaw, float headPitch)
	{
	}

	/**
	 * Captured entity layer submission deferred until the required parent bone pose is known.
	 */
	protected final class DeferredLayerSubmit
	{
		private final PEntityRenderLayer<T> layer;
		private final Matrix4f attachmentPose;
		private final Matrix4f layerTransform;
		private final int packedColor;
		private final int packedLight;
		private final int packedOverlay;

		private DeferredLayerSubmit(PEntityRenderLayer<T> layer,
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
}
