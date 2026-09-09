/**
 * @author ArcAnc
 * Created at: 09.09.2026
 * Copyright (c) 2026
 */

package com.arcanc.pulselib.content.player.animation.attachment;

import com.arcanc.pulselib.content.model.baked.PBakedBone;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationFrame;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

public record PPlayerAnimationMeshAttachmentPose(
		Identifier animation,
		PModelData modelData,
		PBakedBone root,
		PPlayerAnimationFrame frame,
		Matrix4f transform,
		float weight)
{
	public PPlayerAnimationMeshAttachmentPose
	{
		transform = new Matrix4f(transform);
	}
}
