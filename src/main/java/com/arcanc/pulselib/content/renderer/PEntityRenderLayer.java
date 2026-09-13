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

public abstract class PEntityRenderLayer<T extends Entity & PAnimatable<T>, RS extends EntityRenderState & PEntityRenderState<T>>
{
	private final PModelData modelData;
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
	 * Returns the model data.
	 * @param renderState the render state to use.
	 * @return the value produced by this operation.
	 */
	public PModelData getModelData(RS renderState)
	{
		return this.modelData;
	}
	
	/**
	 * Returns the model.
	 * @param renderState the render state to use.
	 * @return the value produced by this operation.
	 */
	public @Nullable PBakedModel getModel(RS renderState)
	{
		return getModelData(renderState).getModel();
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
	 * Performs the should render operation.
	 * @param renderState the render state to use.
	 * @return the value produced by this operation.
	 */
	public boolean shouldRender(RS renderState)
	{
		return true;
	}
	
	/**
	 * Returns the color.
	 * @param renderState the render state to use.
	 * @param bone the bone to use.
	 * @param mesh the mesh to use.
	 * @param packedColor the packed color to use.
	 * @return the value produced by this operation.
	 */
	public int getColor(RS renderState, PBakedBone bone, PBakedMesh mesh, int packedColor)
	{
		return packedColor;
	}
	
	/**
	 * Returns the packed light.
	 * @param renderState the render state to use.
	 * @param packedLight the packed light to use.
	 * @return the value produced by this operation.
	 */
	public int getPackedLight(RS renderState, int packedLight)
	{
		return packedLight;
	}
	
	/**
	 * Returns the packed overlay.
	 * @param renderState the render state to use.
	 * @param packedOverlay the packed overlay to use.
	 * @return the value produced by this operation.
	 */
	public int getPackedOverlay(RS renderState, int packedOverlay)
	{
		return packedOverlay;
	}
	
	/**
	 * Resolves the mesh render.
	 * @param renderState the render state to use.
	 * @param bone the bone to use.
	 * @param mesh the mesh to use.
	 * @param inherited the inherited to use.
	 * @return the value produced by this operation.
	 */
	public PMeshRenderContext resolveMeshRender(RS renderState,
	                                            PBakedBone bone,
	                                            PBakedMesh mesh,
	                                            PMeshRenderContext inherited)
	{
		return inherited.
				withColor(getColor(renderState, bone, mesh, inherited.color())).
				withPackedLight(getPackedLight(renderState, inherited.packedLight())).
				withPackedOverlay(getPackedOverlay(renderState, inherited.packedOverlay()));
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
		PBakedModel model = getModel(renderState);
		if (model == null)
			return;
		
		PModelData data = getModelData(renderState);
		for (PBakedBone bone : model.bones())
		{
			renderer.perBoneSubmit(
					renderState,
					poseStack,
					bone,
					controllers,
					data,
					this :: getRenderType,
					packedColor,
					packedLight,
					packedOverlay,
					submitNodeCollector,
					cameraRenderState,
					this,
					entityBonePoses,
					layerTransform,
					molangContexts);
		}
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
}
