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
import com.arcanc.pulselib.content.model.baked.PBakedBone;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationFrame;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import net.minecraft.resources.ResourceLocation;

/** A selected animated mesh subtree and its canonical player frame. */
public record PPlayerAnimationMeshAttachmentPose(ResourceLocation animation, PModelData modelData,
                                                 PBakedBone root, PPlayerAnimationFrame frame,
                                                 PTransform transform, float weight)
{
}
