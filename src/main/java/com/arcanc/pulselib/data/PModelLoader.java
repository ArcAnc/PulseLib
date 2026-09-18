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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

/**
 * Defines the contract for model loader.
 */
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
	 * Resolves a loader-relative model id to its resource-pack location.
	 * @param modelLocation the loader-relative model id.
	 * @return the resource-pack model location.
	 */
	default Identifier modelResourceLocation(Identifier modelLocation)
	{
		return modelLocation;
	}

	/**
	 * Normalizes a loader-relative model id to the exact resource-pack location.
	 * Implementations add their default model extension when the id has none while
	 * preserving extensions they support explicitly.
	 *
	 * @param modelLocation the loader-relative model id.
	 * @return the normalized resource-pack model location.
	 */
	default Identifier normalizeModelResourceLocation(Identifier modelLocation)
	{
		return modelResourceLocation(modelLocation);
	}

	/**
	 * Returns the resource-pack locations that may satisfy a registered model.
	 * The first location is the preferred one.
	 *
	 * @param modelLocation the loader-relative model id.
	 * @return the candidate resource locations.
	 */
	default List<Identifier> modelResourceCandidates(Identifier modelLocation)
	{
		return List.of(normalizeModelResourceLocation(modelLocation));
	}
	
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
