/**
 * @author ArcAnc
 * Created at: 09.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation.attachment;

import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/**
 * Provides support for player animated attachment layer.
 */
public final class PPlayerAnimatedAttachmentLayer extends RenderLayer<AvatarRenderState, PlayerModel>
{
	/**
	 * Creates an instance of the enclosing type.
	 * @param parent the parent to use.
	 */
	public PPlayerAnimatedAttachmentLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent)
	{
		super(parent);
	}

	/**
	 * Performs the submit operation.
	 * @param poseStack the pose stack to use.
	 * @param submitNodeCollector the submit node collector to use.
	 * @param packedLight the packed light to use.
	 * @param state the state to use.
	 * @param yRot the y rot to use.
	 * @param xRot the x rot to use.
	 */
	@Override
	public void submit(PoseStack poseStack,
	                   SubmitNodeCollector submitNodeCollector,
	                   int packedLight,
	                   AvatarRenderState state,
	                   float yRot,
	                   float xRot)
	{
		if (Minecraft.getInstance().level == null ||
				!(Minecraft.getInstance().level.getEntity(state.id) instanceof AbstractClientPlayer player))
			return;
		PPlayerAutomaticMeshAttachments.renderThirdPerson(
				PPlayerAnimations.automaticMeshAttachmentPoses(player, state.partialTick),
				poseStack,
				packedLight);
		PPlayerAnimatedAttachments.renderThirdPerson(player, poseStack, submitNodeCollector, packedLight, state.partialTick);
	}
}
