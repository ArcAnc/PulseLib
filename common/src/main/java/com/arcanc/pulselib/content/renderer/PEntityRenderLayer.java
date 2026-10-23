/**
 * @author ArcAnc
 * Created at: 20.05.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer;


import com.arcanc.pulselib.content.animatable.PAnimatable;
import com.arcanc.pulselib.content.animatable.PAnimationController;
import com.arcanc.pulselib.content.model.animation.PPose;
import com.arcanc.pulselib.content.model.baked.PBakedBone;
import com.arcanc.pulselib.content.model.baked.PBakedMesh;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.model.baked.PMeshRenderContext;
import com.arcanc.pulselib.content.renderer.base.PEntityRenderState;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.data.gecko.MolangParser;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Provides support for entity render layer.
 */
public abstract class PEntityRenderLayer<T extends Entity & PAnimatable<T>, RS extends EntityRenderState & PEntityRenderState<T>>
{
	private final PModelData modelData;
	private final PRenderStateScope<RS> submission = new PRenderStateScope<>();
	private final PRenderStateScope<ModelSample<T>> models = new PRenderStateScope<>();
	private final Function<Identifier, RenderType> renderType;
	private final Map<String, String> boneBindings = new HashMap<>();
	private final Vector3f offset = new Vector3f();
	private final Quaternionf rotation = new Quaternionf();
	private final Vector3f scale = new Vector3f(1, 1, 1);

	/**
	 * Creates an instance of the enclosing type.
	 * @param modelData the model data to use.
	 * @param renderType the render type to use.
	 */
	public PEntityRenderLayer(PModelData modelData, Function<Identifier, RenderType> renderType)
	{
		this.modelData = modelData;
		this.renderType = renderType;
	}

	/**
	 * Returns the model selection used for this entity layer.
	 *
	 * @param animatable the owner whose animation state is being processed
	 * @return the model selection used for this entity layer
	 */
	public PModelData getModelData(T animatable)
	{
		return this.modelData;
	}

	public PModelData getModelData(RS renderState)
	{
		return getModelData(renderState.getAnimatable());
	}

	/**
	 * Returns the baked model selected for this entity layer.
	 *
	 * @param animatable the owner whose animation state is being processed
	 * @return the baked model selected for this entity layer, or {@code null} when the requested model has not been loaded
	 */
	public @Nullable PBakedModel getModel(T animatable)
	{
		return getModelData(animatable).getModel();
	}

	public @Nullable PBakedModel getModel(RS renderState)
	{
		return getModel(renderState.getAnimatable());
	}

	/**
	 * Returns the render type.
	 * @param texture the texture to use.
	 * @return the value produced by this operation.
	 */
	public RenderType getRenderType(Identifier texture)
	{
		return this.renderType.apply(texture);
	}

	/**
	 * Binds the bone.
	 * @param layerBoneName the layer bone name to use.
	 * @param entityBoneName the entity bone name to use.
	 * @return the value produced by this operation.
	 */
	public PEntityRenderLayer<T, RS> bindBone(String layerBoneName, String entityBoneName)
	{
		this.boneBindings.put(layerBoneName, entityBoneName);
		return this;
	}

	/**
	 * Binds the matching bone.
	 * @param boneName the bone name to use.
	 * @return the value produced by this operation.
	 */
	public PEntityRenderLayer<T, RS> bindMatchingBone(String boneName)
	{
		return bindBone(boneName, boneName);
	}

	/**
	 * Binds the matching bones.
	 * @param boneNames the bone names to use.
	 * @return the value produced by this operation.
	 */
	public PEntityRenderLayer<T, RS> bindMatchingBones(String... boneNames)
	{
		for (String boneName : boneNames)
			bindMatchingBone(boneName);
		return this;
	}

	/**
	 * Returns the bound entity bone.
	 * @param layerBoneName the layer bone name to use.
	 * @return the value produced by this operation.
	 */
	public @Nullable String getBoundEntityBone(String layerBoneName)
	{
		return this.boneBindings.get(layerBoneName);
	}

	/**
	 * Returns whether this layer should be submitted for the current animatable.
	 *
	 * @param animatable the owner whose animation state is being processed
	 * @return whether this layer should be submitted for the current animatable
	 */
	public boolean shouldRender(T animatable)
	{
		return true;
	}

	public boolean shouldRender(RS renderState)
	{
		return shouldRender(renderState.getAnimatable());
	}

	/**
	 * Resolves the packed color used to render one layer mesh.
	 *
	 * @param animatable the owner whose animation state is being processed
	 * @param bone the baked bone being sampled or submitted
	 * @param mesh the baked mesh geometry and material
	 * @param packedColor the packed ARGB vertex color
	 * @return the packed mesh color after layer overrides
	 */
	public int getColor(T animatable, PBakedBone bone, PBakedMesh mesh, int packedColor)
	{
		return packedColor;
	}

	public int getColor(RS renderState, PBakedBone bone, PBakedMesh mesh, int packedColor)
	{
		return getColor(renderState.getAnimatable(), bone, mesh, packedColor);
	}

	/**
	 * Resolves the packed light used by this render layer.
	 *
	 * @param animatable the owner whose animation state is being processed
	 * @param packedLight the packed block and sky light coordinates
	 * @return the packed light after layer overrides
	 */
	public int getPackedLight(T animatable, int packedLight)
	{
		return packedLight;
	}

	public int getPackedLight(RS renderState, int packedLight)
	{
		return getPackedLight(renderState.getAnimatable(), packedLight);
	}

	/**
	 * Resolves the packed overlay used by this render layer.
	 *
	 * @param animatable the owner whose animation state is being processed
	 * @param packedOverlay the packed overlay texture coordinates
	 * @return the packed overlay after layer overrides
	 */
	public int getPackedOverlay(T animatable, int packedOverlay)
	{
		return packedOverlay;
	}

	public int getPackedOverlay(RS renderState, int packedOverlay)
	{
		return getPackedOverlay(renderState.getAnimatable(), packedOverlay);
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
	public PMeshRenderContext resolveMeshRender(T animatable,
	                                            PBakedBone bone,
	                                            PBakedMesh mesh,
	                                            PMeshRenderContext inherited,
	                                            float partialTick)
	{
		RS state = this.submission.current();
		return inherited.
				withColor(state == null ? getColor(animatable, bone, mesh, inherited.color()) : getColor(state, bone, mesh, inherited.color())).
				withPackedLight(state == null ? getPackedLight(animatable, inherited.packedLight()) : getPackedLight(state, inherited.packedLight())).
				withPackedOverlay(state == null ? getPackedOverlay(animatable, inherited.packedOverlay()) : getPackedOverlay(state, inherited.packedOverlay()));
	}

	public PMeshRenderContext resolveMeshRender(RS renderState,
	                                            PBakedBone bone,
	                                            PBakedMesh mesh,
	                                            PMeshRenderContext inherited)
	{
		return resolveMeshRender(renderState.getAnimatable(), bone, mesh, inherited, renderState.partialTick());
	}

	/**
	 * Performs the submit operation.
	 * @param renderer the renderer to use.
	 * @param renderState the render state to use.
	 * @param poseStack the pose stack to use.
	 * @param submitNodeCollector the submit node collector to use.
	 * @param cameraRenderState the camera render state to use.
	 * @param controllers the controllers to use.
	 * @param molangContexts the molang contexts to use.
	 * @param packedColor the packed color to use.
	 * @param packedLight the packed light to use.
	 * @param packedOverlay the packed overlay to use.
	 */
	public void submit(PEntityRenderer<T, RS> renderer,
	                   RS renderState,
	                   PoseStack poseStack,
	                   SubmitNodeCollector submitNodeCollector,
	                   CameraRenderState cameraRenderState,
	                   Collection<PAnimationController<T>> controllers,
	                   Map<PAnimationController<T>, MolangParser.Context> molangContexts,
	                   int packedColor,
	                   int packedLight,
	                   int packedOverlay)
	{
		submit(renderer, renderState, poseStack, submitNodeCollector, cameraRenderState, controllers, molangContexts, packedColor, packedLight, packedOverlay, null, null);
	}

	/**
	 * Performs the submit operation.
	 * @param renderer the renderer to use.
	 * @param renderState the render state to use.
	 * @param poseStack the pose stack to use.
	 * @param submitNodeCollector the submit node collector to use.
	 * @param cameraRenderState the camera render state to use.
	 * @param controllers the controllers to use.
	 * @param molangContexts the molang contexts to use.
	 * @param packedColor the packed color to use.
	 * @param packedLight the packed light to use.
	 * @param packedOverlay the packed overlay to use.
	 * @param entityBonePoses the entity bone poses to use.
	 * @param layerTransform the layer transform to use.
	 */
	public void submit(PEntityRenderer<T, RS> renderer,
	                   RS renderState,
	                   PoseStack poseStack,
	                   SubmitNodeCollector submitNodeCollector,
	                   CameraRenderState cameraRenderState,
	                   Collection<PAnimationController<T>> controllers,
	                   Map<PAnimationController<T>, MolangParser.Context> molangContexts,
	                   int packedColor,
	                   int packedLight,
	                   int packedOverlay,
	                   @Nullable Map<String, Matrix4f> entityBonePoses,
	                   @Nullable Matrix4f layerTransform)
	{
		this.submission.run(renderState, () -> submit(renderer, renderState.getAnimatable(), poseStack, controllers,
				molangContexts, packedColor, packedLight, packedOverlay, renderState.partialTick(),
				renderState instanceof PEntityRenderState.LivingImpl<?> living ?
						new PEntityRenderer.HeadRotation(living.getHeadYRot(), living.getHeadXRot()) : null,
				entityBonePoses, layerTransform));
	}

	/** Canonical geometry-only layer submission bound to an extracted Minecraft state. */
	public void submit(PEntityRenderer<T, RS> renderer, RS state, PoseStack poseStack,
	                   Collection<PAnimationController<T>> controllers, Map<PAnimationController<T>, MolangParser.Context> contexts,
	                   int color, int light, int overlay, @Nullable Map<String, Matrix4f> entityBonePoses,
	                   @Nullable Matrix4f layerTransform)
	{
		this.submission.run(state, () -> submit(renderer, state.getAnimatable(), poseStack, controllers, contexts,
				color, light, overlay, state.partialTick(), state instanceof PEntityRenderState.LivingImpl<?> living ?
						new PEntityRenderer.HeadRotation(living.getHeadYRot(), living.getHeadXRot()) : null,
				entityBonePoses, layerTransform));
	}

	public void submit(PEntityRenderer<T, RS> renderer,
	                   T animatable,
	                   PoseStack poseStack,
	                   Collection<PAnimationController<T>> controllers,
	                   Map<PAnimationController<T>, MolangParser.Context> molangContexts,
	                   int packedColor,
	                   int packedLight,
	                   int packedOverlay,
	                   float partialTick,
					   PEntityRenderer.@Nullable HeadRotation headRotation,
	                   @Nullable Map<String, Matrix4f> entityBonePoses,
	                   @Nullable Matrix4f layerTransform)
	{
		RS state = this.submission.current();
		PBakedModel model = state == null ? getModel(animatable) : getModel(state);
		if (model == null)
			return;

		this.models.run(new ModelSample<>(animatable, model), () ->
		{
			PModelData data = state == null ? getModelData(animatable) : getModelData(state);
			PPose pose = model.evaluate(controllers, molangContexts, partialTick);
			for (PBakedBone bone : model.bones())
			{
				renderer.perBoneSubmit(
						animatable,
						poseStack,
						bone,
						pose,
						controllers,
						data,
						this :: getRenderType,
						packedColor,
						packedLight,
						packedOverlay,
						partialTick,
						headRotation,
						this,
						entityBonePoses,
						layerTransform,
						null,
						molangContexts);
			}
		});
	}

	/**
	 * Sets the offset.
	 * @param offset the offset to use.
	 */
	public void setOffset(Vector3f offset)
	{
		this.offset.set(offset);
	}

	/**
	 * Performs the offset operation.
	 * @return the value produced by this operation.
	 */
	public Vector3f offset()
	{
		return this.offset;
	}

	/**
	 * Sets the rotation.
	 * @param rotation the rotation to use.
	 */
	public void setRotation(Quaternionf rotation)
	{
		this.rotation.set(rotation);
	}

	/**
	 * Performs the rotation operation.
	 * @return the value produced by this operation.
	 */
	public Quaternionf rotation()
	{
		return this.rotation;
	}

	/**
	 * Sets the scale.
	 * @param scale the scale to use.
	 */
	public void setScale(Vector3f scale)
	{
		this.scale.set(scale);
	}

	/**
	 * Performs the scale operation.
	 * @return the value produced by this operation.
	 */
	public Vector3f scale()
	{
		return this.scale;
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
	public PMeshRenderContext resolveBoneRender(T animatable,
	                                            PBakedBone bone,
	                                            PMeshRenderContext inherited,
	                                            float partialTick)
	{
		return inherited;
	}

	/** Native hook forwards to the canonical owner customization point. */
	public PMeshRenderContext resolveBoneRender(RS state, PBakedBone bone, PMeshRenderContext inherited)
	{
		return resolveBoneRender(state.getAnimatable(), bone, inherited, state.partialTick());
	}

	/** Keeps one selected layer model paired with the PPose evaluated for this invocation. */
	@Nullable PBakedModel currentModel(T owner)
	{
		ModelSample<T> sample = this.models.current();
		if (sample != null && sample.owner() == owner)
			return sample.model();
		RS state = this.submission.current();
		return state == null || state.getAnimatable() != owner ? getModel(owner) : getModel(state);
	}

	private record ModelSample<T>(T owner, PBakedModel model)
	{
	}
}
