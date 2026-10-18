/**
 * @author ArcAnc
 * Created at: 23.05.2026
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
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.data.gecko.MolangParser;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Additional model layer whose bones can follow the animated bones of a PulseLib entity renderer.
 *
 * @param <T> the animatable owner type
 */
public abstract class PEntityRenderLayer<T extends Entity & PAnimatable<T>>
{
	private final PModelData modelData;
	private final Function<ResourceLocation, RenderType> renderType;
	private final Map<String, String> boneBindings = new HashMap<>();
	private final Vector3f offset = new Vector3f();
	private final Quaternionf rotation = new Quaternionf();
	private final Vector3f scale = new Vector3f(1, 1, 1);
	
	/**
	 * Creates an additional entity model layer with its own model and pipeline resolver.
	 *
	 * @param modelData the canonical model and loader selection
	 * @param renderType the render type
	 */
	public PEntityRenderLayer(PModelData modelData, Function<ResourceLocation, RenderType> renderType)
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
	
	/**
	 * Selects the layer render pipeline for the supplied texture.
	 *
	 * @param texture the texture resource location
	 * @return the vanilla render pipeline
	 */
	public RenderType getRenderType(ResourceLocation texture)
	{
		return this.renderType.apply(texture);
	}
	
	/**
	 * Binds a layer bone to the named entity-model bone.
	 *
	 * @param layerBoneName the layer bone name
	 * @param entityBoneName the entity bone name
	 * @return this instance
	 */
	public PEntityRenderLayer<T> bindBone(String layerBoneName, String entityBoneName)
	{
		this.boneBindings.put(layerBoneName, entityBoneName);
		return this;
	}
	
	/**
	 * Binds a layer bone to the entity bone with the same name.
	 *
	 * @param boneName the animation bone name
	 * @return this render layer
	 */
	public PEntityRenderLayer<T> bindMatchingBone(String boneName)
	{
		return bindBone(boneName, boneName);
	}
	
	/**
	 * Binds each supplied layer bone to its same-named entity bone.
	 *
	 * @param boneNames the bone names
	 * @return this instance
	 */
	public PEntityRenderLayer<T> bindMatchingBones(String... boneNames)
	{
		for (String boneName : boneNames)
			bindMatchingBone(boneName);
		return this;
	}
	
	/**
	 * Returns the entity bone bound to the layer bone, or {@code null} when no binding exists.
	 *
	 * @param layerBoneName the layer bone name
	 * @return the entity bone bound to the layer bone, or {@code null} when no binding exists
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
		return inherited.
				withColor(getColor(animatable, bone, mesh, inherited.color())).
				withPackedLight(getPackedLight(animatable, inherited.packedLight())).
				withPackedOverlay(getPackedOverlay(animatable, inherited.packedOverlay()));
	}
	
	/**
	 * Submits the layer model using the captured entity bone transforms and current animation inputs.
	 *
	 * @param renderer the renderer
	 * @param animatable the owner whose animation state is being processed
	 * @param poseStack the current model-to-render pose stack
	 * @param controllers the animation controllers in pose composition order
	 * @param molangContexts the frame-specific Molang context of each controller
	 * @param packedColor the packed ARGB vertex color
	 * @param packedLight the packed block and sky light coordinates
	 * @param packedOverlay the packed overlay texture coordinates
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @param headRotation the interpolated head rotation used by bone-specific rendering; may be {@code null}
	 * @param entityBonePoses the exact entity bone matrices captured for attached render layers; may be {@code null}
	 * @param layerTransform the additional local transform of an entity render layer; may be {@code null}
	 */
	public void submit(PEntityRenderer<T> renderer,
	                   T animatable,
	                   PoseStack poseStack,
	                   Collection<PAnimationController<T>> controllers,
	                   Map<PAnimationController<T>, MolangParser.Context> molangContexts,
	                   int packedColor,
	                   int packedLight,
	                   int packedOverlay,
	                   float partialTick,
					   @Nullable PEntityRenderer.HeadRotation headRotation,
	                   @Nullable Map<String, Matrix4f> entityBonePoses,
	                   @Nullable Matrix4f layerTransform)
	{
		PBakedModel model = getModel(animatable);
		if (model == null)
			return;
		
		PModelData data = getModelData(animatable);
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
	}
	
	/**
	 * Sets the layer-local translation offset.
	 *
	 * @param offset the local translation offset
	 */
	public void setOffset(Vector3f offset)
	{
		this.offset.set(offset);
	}
	
	/**
	 * Returns the local translation offset.
	 *
	 * @return the local translation offset
	 */
	public Vector3f offset()
	{
		return this.offset;
	}
	
	/**
	 * Sets the layer-local rotation.
	 *
	 * @param rotation the local quaternion rotation
	 */
	public void setRotation(Quaternionf rotation)
	{
		this.rotation.set(rotation);
	}
	
	/**
	 * Returns the local quaternion rotation.
	 *
	 * @return the local quaternion rotation
	 */
	public Quaternionf rotation()
	{
		return this.rotation;
	}
	
	/**
	 * Sets the layer-local scale.
	 *
	 * @param scale the local scale vector
	 */
	public void setScale(Vector3f scale)
	{
		this.scale.set(scale);
	}
	
	/**
	 * Returns the local scale vector.
	 *
	 * @return the local scale vector
	 */
	public Vector3f scale()
	{
		return this.scale;
	}
}
