/**
 * @author ArcAnc
 * Created at: 04.07.2026
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

/**
 * Built-in attachment anchors for vanilla humanoid model parts.
 */
public class PHumanoidAnchors
{
	/**
	 * Attachment anchor for the humanoid head model part.
	 */
	public static final PAttachmentAnchor HEAD = PAttachmentAnchor.minecraft("head");
	/**
	 * Attachment anchor for the humanoid hat model part.
	 */
	public static final PAttachmentAnchor HAT = PAttachmentAnchor.minecraft("hat");
	/**
	 * Attachment anchor for the humanoid body model part.
	 */
	public static final PAttachmentAnchor BODY = PAttachmentAnchor.minecraft("body");
	/**
	 * Attachment anchor for the humanoid right arm model part.
	 */
	public static final PAttachmentAnchor RIGHT_ARM = PAttachmentAnchor.minecraft("right_arm");
	/**
	 * Attachment anchor for the humanoid left arm model part.
	 */
	public static final PAttachmentAnchor LEFT_ARM = PAttachmentAnchor.minecraft("left_arm");
	/**
	 * Attachment anchor for the humanoid right leg model part.
	 */
	public static final PAttachmentAnchor RIGHT_LEG = PAttachmentAnchor.minecraft("right_leg");
	/**
	 * Attachment anchor for the humanoid left leg model part.
	 */
	public static final PAttachmentAnchor LEFT_LEG = PAttachmentAnchor.minecraft("left_leg");
	
	/**
	 * Registers resolvers for the built-in humanoid attachment anchors.
	 */
	@ApiStatus.Internal
	public static void registerDefaults()
	{
		PAttachmentAnchorResolvers.register(HumanoidModel.class, HEAD, (entity, model) -> ((HumanoidModel<?>)model).head);
		PAttachmentAnchorResolvers.register(HumanoidModel.class, HAT, (entity, model) -> ((HumanoidModel<?>)model).hat);
		PAttachmentAnchorResolvers.register(HumanoidModel.class, BODY, (entity, model) -> ((HumanoidModel<?>)model).body);
		PAttachmentAnchorResolvers.register(HumanoidModel.class, RIGHT_ARM, (entity, model) -> ((HumanoidModel<?>)model).rightArm);
		PAttachmentAnchorResolvers.register(HumanoidModel.class, LEFT_ARM, (entity, model) -> ((HumanoidModel<?>)model).leftArm);
		PAttachmentAnchorResolvers.register(HumanoidModel.class, RIGHT_LEG, (entity, model) -> ((HumanoidModel<?>)model).rightLeg);
		PAttachmentAnchorResolvers.register(HumanoidModel.class, LEFT_LEG, (entity, model) -> ((HumanoidModel<?>)model).leftLeg);
	}
}
