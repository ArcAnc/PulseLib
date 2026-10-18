/**
 * @author ArcAnc
 * Created at: 07.07.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.util.attachments.humanoid;


import com.arcanc.pulselib.util.attachments.PAttachmentAnchor;
import com.arcanc.pulselib.util.attachments.PAttachmentBinding;
import com.arcanc.pulselib.util.attachments.PTransform;

import java.util.Objects;

/**
 * Factory methods binding attachment bones to standard humanoid model anchors.
 */
public final class PHumanoidBindings
{
	private PHumanoidBindings()
	{
	}
	
	/**
	 * Creates an attachment binding to the supplied humanoid anchor.
	 *
	 * @param anchor the anchor
	 * @param bone the bone or bone index being processed
	 * @return the attachment binding to the selected humanoid anchor
	 */
	public static PAttachmentBinding bind(PAttachmentAnchor anchor, String bone)
	{
		return new Binding(anchor, bone, PTransform.IDENTITY);
	}
	
	/**
	 * Creates an attachment binding to the supplied humanoid anchor.
	 *
	 * @param anchor the anchor
	 * @param bone the bone or bone index being processed
	 * @param transform the transform
	 * @return the attachment binding to the selected humanoid anchor
	 */
	public static PAttachmentBinding bind(PAttachmentAnchor anchor, String bone, PTransform transform)
	{
		return new Binding(anchor, bone, transform);
	}
	
	/**
	 * Binds an attachment bone to the humanoid head.
	 *
	 * @param bone the bone or bone index being processed
	 * @return the attachment binding to the selected humanoid anchor
	 */
	public static PAttachmentBinding head(String bone)
	{
		return bind(PHumanoidAnchors.HEAD, bone);
	}
	
	/**
	 * Binds an attachment bone to the humanoid torso.
	 *
	 * @param bone the bone or bone index being processed
	 * @return the attachment binding to the selected humanoid anchor
	 */
	public static PAttachmentBinding body(String bone)
	{
		return bind(PHumanoidAnchors.BODY, bone);
	}
	
	/**
	 * Binds an attachment bone to the humanoid right arm.
	 *
	 * @param bone the bone or bone index being processed
	 * @return the attachment binding to the selected humanoid anchor
	 */
	public static PAttachmentBinding rightArm(String bone)
	{
		return bind(PHumanoidAnchors.RIGHT_ARM, bone);
	}
	
	/**
	 * Binds an attachment bone to the humanoid left arm.
	 *
	 * @param bone the bone or bone index being processed
	 * @return the attachment binding to the selected humanoid anchor
	 */
	public static PAttachmentBinding leftArm(String bone)
	{
		return bind(PHumanoidAnchors.LEFT_ARM, bone);
	}
	
	/**
	 * Binds an attachment bone to the humanoid right leg.
	 *
	 * @param bone the bone or bone index being processed
	 * @return the attachment binding to the selected humanoid anchor
	 */
	public static PAttachmentBinding rightLeg(String bone)
	{
		return bind(PHumanoidAnchors.RIGHT_LEG, bone);
	}
	
	/**
	 * Binds an attachment bone to the humanoid left leg.
	 *
	 * @param bone the bone or bone index being processed
	 * @return the attachment binding to the selected humanoid anchor
	 */
	public static PAttachmentBinding leftLeg(String bone)
	{
		return bind(PHumanoidAnchors.LEFT_LEG, bone);
	}
	
	/**
	 * Concrete humanoid attachment anchor, bone name and local transform.
	 *
	 * @param anchor the anchor
	 * @param bone the bone or bone index being processed
	 * @param transform the transform
	 */
	private record Binding(PAttachmentAnchor anchor, String bone, PTransform transform) implements PAttachmentBinding
	{
		private Binding
		{
			Objects.requireNonNull(anchor);
			Objects.requireNonNull(bone);
			Objects.requireNonNull(transform);
		}
	}
}
