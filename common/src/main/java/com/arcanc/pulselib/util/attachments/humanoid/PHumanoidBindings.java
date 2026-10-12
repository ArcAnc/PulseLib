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
	private PHumanoidBindings()
	{
	}
	
	public static PAttachmentBinding bind(PAttachmentAnchor anchor, String bone)
	{
		return new Binding(anchor, bone, PTransform.IDENTITY);
	}
	
	public static PAttachmentBinding bind(PAttachmentAnchor anchor, String bone, PTransform transform)
	{
		return new Binding(anchor, bone, transform);
	}
	
	public static PAttachmentBinding head(String bone)
	{
		return bind(PHumanoidAnchors.HEAD, bone);
	}
	
	public static PAttachmentBinding body(String bone)
	{
		return bind(PHumanoidAnchors.BODY, bone);
	}
	
	public static PAttachmentBinding rightArm(String bone)
	{
		return bind(PHumanoidAnchors.RIGHT_ARM, bone);
	}
	
	public static PAttachmentBinding leftArm(String bone)
	{
		return bind(PHumanoidAnchors.LEFT_ARM, bone);
	}
	
	public static PAttachmentBinding rightLeg(String bone)
	{
		return bind(PHumanoidAnchors.RIGHT_LEG, bone);
	}
	
	public static PAttachmentBinding leftLeg(String bone)
	{
		return bind(PHumanoidAnchors.LEFT_LEG, bone);
	}
	
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
