/**
 * @author ArcAnc
 * Created at: 23.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation.attachment;

import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

/** Native 1.21.1 third-person player layer for animated anchor and mesh attachments. */
public final class PPlayerAnimatedAttachmentLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>
{
	/**
	 * Attaches animated anchor and mesh rendering to the supplied player renderer.
	 *
	 * @param parent the parent
	 */
	public PPlayerAnimatedAttachmentLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) { super(parent); }

	/**
	 * Renders animated action-anchor attachments and automatic animation-model mesh attachments for the
	 * player.
	 *
	 * @param poseStack the current model-to-render pose stack
	 * @param buffers the vanilla vertex buffer source
	 * @param light the packed block and sky light coordinates
	 * @param player the player whose animation or presentation is being processed
	 * @param limbSwing the entity limb animation position
	 * @param limbSwingAmount the entity limb animation amplitude
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @param ageInTicks the entity age used by vanilla render animation
	 * @param netHeadYaw the head yaw relative to the body in degrees
	 * @param headPitch the head pitch in degrees
	 */
	@Override
	public void render(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player,
	                   float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
	                   float netHeadYaw, float headPitch)
	{
		PPlayerAnimatedAttachments.renderThirdPerson(player, poseStack, buffers, light, partialTick);
		PPlayerAutomaticMeshAttachments.renderThirdPerson(PPlayerAnimations.meshAttachmentPoses(player, partialTick), poseStack, light);
	}
}
