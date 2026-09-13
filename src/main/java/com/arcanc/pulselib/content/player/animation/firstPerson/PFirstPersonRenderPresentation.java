/**
 * @author ArcAnc
 * Created at: 05.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation.firstPerson;

import java.util.List;
import java.util.Objects;

/**
 * Render commands for the first-person bridge.  These are VIEW-space outputs
 * of {@link PFirstPersonPresentation}; this type deliberately owns no
 * alternative skeletal pose or animation channels.
 */
public record PFirstPersonRenderPresentation(
		PFirstPersonArmPose rightArm,
		PFirstPersonArmPose leftArm,
		PFirstPersonItemPose rightItem,
		PFirstPersonItemPose leftItem,
		List<PPlayerFirstPersonAnchorPose> animationAnchors,
		List<PPlayerFirstPersonMeshAttachmentPose> meshAttachments)
{
	public PFirstPersonRenderPresentation
	{
		rightArm = Objects.requireNonNull(rightArm);
		leftArm = Objects.requireNonNull(leftArm);
		rightItem = Objects.requireNonNull(rightItem);
		leftItem = Objects.requireNonNull(leftItem);
		animationAnchors = List.copyOf(animationAnchors);
		meshAttachments = List.copyOf(meshAttachments);
	}

}
