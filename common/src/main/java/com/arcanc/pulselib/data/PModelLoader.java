/**
 * @author ArcAnc
 * Created at: 24.05.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.data;


import com.arcanc.pulselib.content.model.resource.PModelResource;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

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
	 * Resolves a canonical model id to physical resource candidates. The first
	 * candidate has the highest precedence.
	 *
	 * @param modelId the registered canonical model id.
	 * @return physical resource candidates.
	 */
	List<Identifier> modelResourceCandidates(Identifier modelId);
	
	/**
	 * Loads one canonical model and records its selected physical source.
	 *
	 * @param backgroundExecutor the executor used to prepare model resources.
	 * @param resourceManager the source resource manager.
	 * @param modelId the canonical model identifier.
	 * @return the canonical id, selected physical source and parsed model.
	 */
	CompletableFuture<PLoadedModel> loadModel(Executor backgroundExecutor,
	                                         ResourceManager resourceManager,
	                                         Identifier modelId);

	/** Native reload alias for the canonical resource candidate contract. */
	default List<Identifier> physicalResourceCandidates(Identifier modelId)
	{
		return modelResourceCandidates(modelId);
	}

	/**
	 * Loads a registered batch through the canonical single-model contract.
	 * Results are delivered in registration order after the whole batch succeeds.
	 *
	 * @param backgroundExecutor the executor used to prepare model resources.
	 * @param resourceManager the source resource manager.
	 * @param models registered logical models owned by this loader.
	 * @param elementConsumer receives each loaded model in registration order.
	 * @return completion of the batch and its result delivery.
	 */
	default CompletableFuture<?> loadModels(Executor backgroundExecutor,
	                                        ResourceManager resourceManager,
	                                        Collection<PModelResource> models,
	                                        Consumer<PLoadedModel> elementConsumer)
	{
		List<CompletableFuture<PLoadedModel>> tasks = models.stream().
				map(resource -> loadModel(backgroundExecutor, resourceManager, resource.modelId())).toList();
		return CompletableFuture.allOf(tasks.toArray(CompletableFuture[] :: new)).
				thenRunAsync(() -> tasks.forEach(task -> elementConsumer.accept(task.join())), backgroundExecutor);
	}
}
