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
import com.arcanc.pulselib.util.attachments.PLivingAttachmentLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;

/**
 * Provides support for humanoid attachment layer.
 */
public class PHumanoidAttachmentLayer<S extends HumanoidRenderState, M extends HumanoidModel<S>> extends PLivingAttachmentLayer<S, M>
{
	/**
	 * Creates an instance of the enclosing type.
	 * @param parent the parent to use.
	 */
	public PHumanoidAttachmentLayer(RenderLayerParent<S, M> parent)
	{
		super(parent);
	}
	
	/**
	 * Renders the first person arm.
	 * @param poseStack the pose stack to use.
	 * @param light the light to use.
	 * @param entity the entity to use.
	 * @param arm the arm to use.
	 * @param armPart the arm part to use.
	 * @param partialTick the partial tick to use.
	 */
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
