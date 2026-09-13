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

public final class PHumanoidBindings
{
	/**
	 * Creates an instance of the enclosing type.
	 */
	private PHumanoidBindings()
	{
	}
	
	/**
	 * Performs the bind operation.
	 * @param anchor the anchor to use.
	 * @param bone the bone to use.
	 * @return the value produced by this operation.
	 */
	public static PAttachmentBinding bind(PAttachmentAnchor anchor, String bone)
	{
		return new Binding(anchor, bone, PTransform.IDENTITY);
	}
	
	/**
	 * Performs the bind operation.
	 * @param anchor the anchor to use.
	 * @param bone the bone to use.
	 * @param transform the transform to use.
	 * @return the value produced by this operation.
	 */
	public static PAttachmentBinding bind(PAttachmentAnchor anchor, String bone, PTransform transform)
	{
		return new Binding(anchor, bone, transform);
	}
	
	/**
	 * Performs the head operation.
	 * @param bone the bone to use.
	 * @return the value produced by this operation.
	 */
	public static PAttachmentBinding head(String bone)
	{
		return bind(PHumanoidAnchors.HEAD, bone);
	}
	
	/**
	 * Performs the body operation.
	 * @param bone the bone to use.
	 * @return the value produced by this operation.
	 */
	public static PAttachmentBinding body(String bone)
	{
		return bind(PHumanoidAnchors.BODY, bone);
	}
	
	/**
	 * Performs the right arm operation.
	 * @param bone the bone to use.
	 * @return the value produced by this operation.
	 */
	public static PAttachmentBinding rightArm(String bone)
	{
		return bind(PHumanoidAnchors.RIGHT_ARM, bone);
	}
	
	/**
	 * Performs the left arm operation.
	 * @param bone the bone to use.
	 * @return the value produced by this operation.
	 */
	public static PAttachmentBinding leftArm(String bone)
	{
		return bind(PHumanoidAnchors.LEFT_ARM, bone);
	}
	
	/**
	 * Performs the right leg operation.
	 * @param bone the bone to use.
	 * @return the value produced by this operation.
	 */
	public static PAttachmentBinding rightLeg(String bone)
	{
		return bind(PHumanoidAnchors.RIGHT_LEG, bone);
	}
	
	/**
	 * Performs the left leg operation.
	 * @param bone the bone to use.
	 * @return the value produced by this operation.
	 */
	public static PAttachmentBinding leftLeg(String bone)
	{
		return bind(PHumanoidAnchors.LEFT_LEG, bone);
	}
	
	private record Binding(PAttachmentAnchor anchor, String bone, PTransform transform) implements PAttachmentBinding
	{
		/**
		 * Creates an instance of the enclosing type.
		 * @param anchor the anchor to use.
		 * @param bone the bone to use.
		 * @param transform the transform to use.
		 */
		private Binding
		{
			Objects.requireNonNull(anchor);
			Objects.requireNonNull(bone);
			Objects.requireNonNull(transform);
		}
	}
}
