/**
 * @author ArcAnc
 * Created at: 24.05.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.data;


import com.arcanc.pulselib.content.model.PModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

public interface PModelLoader
{
	/**
	 * Performs the id operation.
	 * @return the value produced by this operation.
	 */
	Identifier id();

	/**
	 * Applies the item transform.
	 * @param poseStack the pose stack to use.
	 */
	default void applyItemTransform(PoseStack poseStack)
	{
		poseStack.translate(0.5f, 0, 0.5f);
		poseStack.mulPose(Axis.YP.rotationDegrees(180));
	}
	
	/**
	 * Performs the supports operation.
	 * @param modelPath the model path to use.
	 * @return the value produced by this operation.
	 */
	boolean supports(Identifier modelPath);
	
	/**
	 * Performs the default model location operation.
	 * @param modelLocation the model location to use.
	 * @param modelType the model type to use.
	 * @return the value produced by this operation.
	 */
	Identifier defaultModelLocation(Identifier modelLocation, String modelType);
	
	/**
	 * Performs the default texture location operation.
	 * @param textureLocation the texture location to use.
	 * @param modelLocation the model location to use.
	 * @param modelType the model type to use.
	 * @return the value produced by this operation.
	 */
	default Identifier defaultTextureLocation(Identifier textureLocation, Identifier modelLocation, String modelType)
	{
		return textureLocation.withPrefix(modelType + "/" + modelLocation.getPath() + "/");
	}
	
	/**
	 * Performs the texture location operation.
	 * @param modelPath the model path to use.
	 * @param textureName the texture name to use.
	 * @return the value produced by this operation.
	 */
	Identifier textureLocation(Identifier modelPath, String textureName);
	
	/**
	 * Loads the models.
	 * @param backgroundExecutor the background executor to use.
	 * @param resourceManager the resource manager to use.
	 * @param elementConsumer the element consumer to use.
	 * @return the value produced by this operation.
	 */
	CompletableFuture<?> loadModels(Executor backgroundExecutor,
	                                ResourceManager resourceManager,
	                                BiConsumer<Identifier, PModel> elementConsumer);
}
