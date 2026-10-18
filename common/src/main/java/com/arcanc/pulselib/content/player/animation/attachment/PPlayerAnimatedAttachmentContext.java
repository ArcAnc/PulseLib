/**
 * @author ArcAnc
 * Created at: 23.09.2026
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
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;

/**
 * Native 1.21.1 attachment invocation context.
 *
 * @param player the player whose animation or presentation is being processed
 * @param animation the namespaced resource or registry identifier
 * @param anchor the anchor
 * @param transform the transform
 * @param weight the contribution weight
 * @param firstPerson the first person
 * @param poseStack the current model-to-render pose stack
 * @param buffers the vanilla vertex buffer source
 * @param packedLight the packed block and sky light coordinates
 * @param partialTick the interpolation fraction between the previous and current game ticks
 */
public record PPlayerAnimatedAttachmentContext(AbstractClientPlayer player, ResourceLocation animation,
                                               PPlayerAnimationAnchor anchor, PTransform transform, float weight,
                                               boolean firstPerson, PoseStack poseStack, MultiBufferSource buffers,
                                               int packedLight, float partialTick)
{
}
