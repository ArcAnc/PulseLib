/**
 * @author ArcAnc
 * Created at: 03.07.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.util.attachments.humanoid;


import com.arcanc.pulselib.util.attachments.PAttachmentAnchor;
import com.arcanc.pulselib.util.attachments.PLivingAttachmentLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;

public class PHumanoidAttachmentLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends PLivingAttachmentLayer<T, M>
{
	public PHumanoidAttachmentLayer(RenderLayerParent<T, M> parent)
	{
		super(parent);
	}
	
	public static void renderFirstPersonArm(PoseStack poseStack,
	                                        int light,
	                                        LivingEntity entity,
	                                        HumanoidArm arm,
	                                        ModelPart armPart,
	                                        float partialTick)
	{
		PAttachmentAnchor targetAnchor = arm == HumanoidArm.RIGHT ? PHumanoidAnchors.RIGHT_ARM : PHumanoidAnchors.LEFT_ARM;
		renderFirstPersonAnchor(poseStack, light, entity, targetAnchor, armPart, partialTick);
	}
}
