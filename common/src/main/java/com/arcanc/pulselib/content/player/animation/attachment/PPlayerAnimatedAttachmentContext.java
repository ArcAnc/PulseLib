/**
 * @author ArcAnc
 * Created at: 09.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation.attachment;

import com.arcanc.pulselib.content.model.animation.PTransform;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationAnchor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.resources.Identifier;

/**
 * Native 26.1.2 attachment invocation context.
 *
 * @param player the player being rendered
 * @param animation the animation identifier
 * @param anchor the semantic attachment anchor
 * @param transform the editable sampled transform
 * @param weight the contribution weight
 * @param firstPerson whether this is a first-person invocation
 * @param poseStack the current attachment pose stack
 * @param buffers the native deferred geometry collector
 * @param packedLight the packed light coordinates
 * @param partialTick the interpolation fraction
 */
public record PPlayerAnimatedAttachmentContext(
		AbstractClientPlayer player,
		Identifier animation,
		PPlayerAnimationAnchor anchor,
		PTransform transform,
		float weight,
		boolean firstPerson,
		PoseStack poseStack,
		SubmitNodeCollector buffers,
		int packedLight,
		float partialTick)
{
	/**
	 * Retains the original 26.1 collector accessor alongside the canonical buffers accessor.
	 *
	 * @return the native deferred geometry collector
	 */
	public SubmitNodeCollector submitNodeCollector()
	{
		return this.buffers;
	}
}
