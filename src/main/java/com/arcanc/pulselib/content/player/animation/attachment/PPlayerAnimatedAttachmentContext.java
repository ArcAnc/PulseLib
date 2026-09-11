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

public record PPlayerAnimatedAttachmentContext(
		AbstractClientPlayer player,
		Identifier animation,
		PPlayerAnimationAnchor anchor,
		PTransform transform,
		float weight,
		boolean firstPerson,
		PoseStack poseStack,
		SubmitNodeCollector submitNodeCollector,
		int packedLight,
		float partialTick)
{
}
