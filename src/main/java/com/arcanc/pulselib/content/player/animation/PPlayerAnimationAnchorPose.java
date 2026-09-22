package com.arcanc.pulselib.content.player.animation;

import com.arcanc.pulselib.content.model.animation.PTransform;
import net.minecraft.resources.ResourceLocation;

/** The resolved transform of one player-animation anchor. */
public record PPlayerAnimationAnchorPose(ResourceLocation animation,
                                         PPlayerAnimationAnchor anchor,
                                         PTransform transform,
                                         float weight)
{
}
