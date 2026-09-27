/**
 * @author ArcAnc
 * Created at: 27.05.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.data;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Resolves canonical model ids to physical resources and parses them.
 */
public interface PModelLoader
{
	ResourceLocation id();

	default void applyItemTransform(PoseStack poseStack)
	{
		poseStack.translate(0.5f, 0, 0.5f);
		poseStack.mulPose(Axis.YP.rotationDegrees(180));
	}

	/**
	 * Returns physical resource candidates for a canonical model id, in
	 * precedence order. This is intentionally the only place resource-path
	 * conventions belong.
	 */
	List<ResourceLocation> modelResourceCandidates(ResourceLocation modelId);

	/**
	 * Loads one registered canonical model. The returned source is the physical
	 * candidate selected by this loader and must not be used as a cache key.
	 */
	CompletableFuture<PLoadedModel> loadModel(Executor backgroundExecutor,
	                                         ResourceManager resourceManager,
	                                         ResourceLocation modelId);
}
