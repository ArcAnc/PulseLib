/**
 * @author ArcAnc
 * Created at: 09.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation.firstPerson;

import com.arcanc.pulselib.content.model.animation.PTransform;
import com.arcanc.pulselib.content.model.baked.PBakedBone;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationFrame;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import net.minecraft.resources.Identifier;

/**
 * Immutable value object representing player first person mesh attachment pose.
 */
public record PPlayerFirstPersonMeshAttachmentPose(
		Identifier animation,
		PModelData modelData,
		PBakedBone root,
		PPlayerAnimationFrame frame,
		PTransform transform,
		float weight)
{
}
