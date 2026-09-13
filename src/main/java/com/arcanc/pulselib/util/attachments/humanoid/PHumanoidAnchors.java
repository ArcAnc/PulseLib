/**
 * @author ArcAnc
 * Created at: 05.07.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.util.attachments.humanoid;


import com.arcanc.pulselib.util.attachments.PAttachmentAnchor;
import com.arcanc.pulselib.util.attachments.PAttachmentAnchorResolvers;
import net.minecraft.client.model.HumanoidModel;
import org.jetbrains.annotations.ApiStatus;

public class PHumanoidAnchors
{
	public static final PAttachmentAnchor HEAD = PAttachmentAnchor.minecraft("head");
	public static final PAttachmentAnchor HAT = PAttachmentAnchor.minecraft("hat");
	public static final PAttachmentAnchor BODY = PAttachmentAnchor.minecraft("body");
	public static final PAttachmentAnchor RIGHT_ARM = PAttachmentAnchor.minecraft("right_arm");
	public static final PAttachmentAnchor LEFT_ARM = PAttachmentAnchor.minecraft("left_arm");
	public static final PAttachmentAnchor RIGHT_LEG = PAttachmentAnchor.minecraft("right_leg");
	public static final PAttachmentAnchor LEFT_LEG = PAttachmentAnchor.minecraft("left_leg");
	
	/**
	 * Registers the defaults.
	 */
	@ApiStatus.Internal
	public static void registerDefaults()
	{
		PAttachmentAnchorResolvers.register(HumanoidModel.class, HEAD, (_, model) -> ((HumanoidModel<?>)model).head);
		PAttachmentAnchorResolvers.register(HumanoidModel.class, HAT, (_, model) -> ((HumanoidModel<?>)model).hat);
		PAttachmentAnchorResolvers.register(HumanoidModel.class, BODY, (_, model) -> ((HumanoidModel<?>)model).body);
		PAttachmentAnchorResolvers.register(HumanoidModel.class, RIGHT_ARM, (_, model) -> ((HumanoidModel<?>)model).rightArm);
		PAttachmentAnchorResolvers.register(HumanoidModel.class, LEFT_ARM, (_, model) -> ((HumanoidModel<?>)model).leftArm);
		PAttachmentAnchorResolvers.register(HumanoidModel.class, RIGHT_LEG, (_, model) -> ((HumanoidModel<?>)model).rightLeg);
		PAttachmentAnchorResolvers.register(HumanoidModel.class, LEFT_LEG, (_, model) -> ((HumanoidModel<?>)model).leftLeg);
	}
}
