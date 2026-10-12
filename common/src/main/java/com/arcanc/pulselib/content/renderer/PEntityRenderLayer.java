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

public abstract class PEntityRenderLayer<T extends Entity & PAnimatable<T>>
{
	private final PModelData modelData;
	private final Function<ResourceLocation, RenderType> renderType;
	private final Map<String, String> boneBindings = new HashMap<>();
	private final Vector3f offset = new Vector3f();
	private final Quaternionf rotation = new Quaternionf();
	private final Vector3f scale = new Vector3f(1, 1, 1);
	
	public PEntityRenderLayer(PModelData modelData, Function<ResourceLocation, RenderType> renderType)
	{
		this.modelData = modelData;
		this.renderType = renderType;
	}
	
	public PModelData getModelData(T animatable)
	{
		return this.modelData;
	}
	
	public @Nullable PBakedModel getModel(T animatable)
	{
		return getModelData(animatable).getModel();
	}
	
	public RenderType getRenderType(ResourceLocation texture)
	{
		return this.renderType.apply(texture);
	}
	
	public PEntityRenderLayer<T> bindBone(String layerBoneName, String entityBoneName)
	{
		this.boneBindings.put(layerBoneName, entityBoneName);
		return this;
	}
	
	public PEntityRenderLayer<T> bindMatchingBone(String boneName)
	{
		return bindBone(boneName, boneName);
	}
	
	public PEntityRenderLayer<T> bindMatchingBones(String... boneNames)
	{
		for (String boneName : boneNames)
			bindMatchingBone(boneName);
		return this;
	}
	
	public @Nullable String getBoundEntityBone(String layerBoneName)
	{
		return this.boneBindings.get(layerBoneName);
	}
	
	public boolean shouldRender(T animatable)
	{
		return true;
	}
	
	public int getColor(T animatable, PBakedBone bone, PBakedMesh mesh, int packedColor)
	{
		return packedColor;
	}
	
	public int getPackedLight(T animatable, int packedLight)
	{
		return packedLight;
	}
	
	public int getPackedOverlay(T animatable, int packedOverlay)
	{
		return packedOverlay;
	}

	public PMeshRenderContext resolveBoneRender(T animatable,
	                                            PBakedBone bone,
	                                            PMeshRenderContext inherited,
	                                            float partialTick)
	{
		return inherited;
	}
	
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
	
	public void setOffset(Vector3f offset)
	{
		this.offset.set(offset);
	}
	
	public Vector3f offset()
	{
		return this.offset;
	}
	
	public void setRotation(Quaternionf rotation)
	{
		this.rotation.set(rotation);
	}
	
	public Quaternionf rotation()
	{
		return this.rotation;
	}
	
	public void setScale(Vector3f scale)
	{
		this.scale.set(scale);
	}
	
	public Vector3f scale()
	{
		return this.scale;
	}
}
