/**
 * @author ArcAnc
 * Created at: 09.09.2026
 * Copyright (c) 2026
 */

package com.arcanc.pulselib.content.player.animation.firstPerson;

import com.arcanc.pulselib.content.model.animation.PTransform;
import com.arcanc.pulselib.content.model.baked.PBakedBone;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationFrame;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import net.minecraft.resources.Identifier;

public record PPlayerFirstPersonMeshAttachmentPose(
		Identifier animation,
		PModelData modelData,
		PBakedBone root,
		PPlayerAnimationFrame frame,
		PTransform transform)
{
}
