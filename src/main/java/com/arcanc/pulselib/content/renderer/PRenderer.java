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
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.renderer.base.PRenderState;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Defines the contract for renderer.
 */
public interface PRenderer<T extends PAnimatable<T>, RS extends PRenderState<T>>
{
	/**
	 * Returns the model data.
	 * @param renderState the render state to use.
	 * @return the value produced by this operation.
	 */
	PModelData getModelData(RS renderState);
	
	/**
	 * Returns the model.
	 * @param renderState the render state to use.
	 * @return the value produced by this operation.
	 */
	@Nullable PBakedModel getModel(RS renderState);
	
	/**
	 * Returns the render type.
	 * @param texture the texture to use.
	 * @return the value produced by this operation.
	 */
	RenderType getRenderType(Identifier texture);
	
	/**
	 * Performs the pre submit operation.
	 * @param poseStack the pose stack to use.
	 * @param renderState the render state to use.
	 * @param cameraRenderState the camera render state to use.
	 * @param submitNodeCollector the submit node collector to use.
	 */
	void preSubmit(PoseStack poseStack, RS renderState, CameraRenderState cameraRenderState, SubmitNodeCollector submitNodeCollector);
	/**
	 * Performs the true submit operation.
	 * @param poseStack the pose stack to use.
	 * @param renderState the render state to use.
	 * @param cameraRenderState the camera render state to use.
	 * @param submitNodeCollector the submit node collector to use.
	 */
	void trueSubmit(PoseStack poseStack, RS renderState, CameraRenderState cameraRenderState, SubmitNodeCollector submitNodeCollector);
	/**
	 * Performs the post submit operation.
	 * @param poseStack the pose stack to use.
	 * @param renderState the render state to use.
	 * @param cameraRenderState the camera render state to use.
	 * @param submitNodeCollector the submit node collector to use.
	 */
	void postSubmit(PoseStack poseStack, RS renderState, CameraRenderState cameraRenderState, SubmitNodeCollector submitNodeCollector);
}
