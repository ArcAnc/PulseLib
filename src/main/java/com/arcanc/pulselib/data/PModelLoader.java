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
	List<Identifier> physicalResourceCandidates(Identifier modelId);
	
	/**
	 * Loads the models.
	 * @param backgroundExecutor the background executor to use.
	 * @param resourceManager the resource manager to use.
	 * @param models registered logical models owned by this loader.
	 * @param elementConsumer receives a canonical id, selected physical source,
	 *                        and parsed model.
	 * @return the value produced by this operation.
	 */
	CompletableFuture<?> loadModels(Executor backgroundExecutor,
	                                ResourceManager resourceManager,
	                                Collection<PModelResource> models,
	                                Consumer<PLoadedModel> elementConsumer);
}
