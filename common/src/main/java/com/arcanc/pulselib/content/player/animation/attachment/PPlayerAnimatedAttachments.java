/**
 * @author ArcAnc
 * Created at: 23.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation.attachment;

import com.arcanc.pulselib.content.model.animation.PTransform;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationSpace;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationAnchor;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationAnchorPose;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import com.arcanc.pulselib.content.player.animation.firstPerson.PFirstPersonRenderPresentation;
import com.arcanc.pulselib.content.player.animation.firstPerson.PPlayerFirstPersonAnchorPose;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;

import java.util.*;

/**
 * Registry and third-person dispatcher for semantic player-animation anchors.
 */
public final class PPlayerAnimatedAttachments
{
	private static final Map<PPlayerAnimationAnchor, List<PPlayerAnimatedAttachmentRenderer>> RENDERERS = new HashMap<>();

	private PPlayerAnimatedAttachments()
	{
	}

	public static void register(PPlayerAnimatedAttachmentRenderer renderer)
	{
		List<PPlayerAnimatedAttachmentRenderer> renderers = RENDERERS.computeIfAbsent(renderer.anchor(), ignored -> new ArrayList<>());
		if (!renderers.contains(renderer))
			renderers.add(renderer);
	}

	public static void renderThirdPerson(AbstractClientPlayer player, PoseStack poseStack,
	                                     MultiBufferSource buffers, int packedLight, float partialTick)
	{
		for (PPlayerAnimationAnchorPose pose : PPlayerAnimations.animationAnchorPoses(player, partialTick))
		{
			List<PPlayerAnimatedAttachmentRenderer> renderers = RENDERERS.get(pose.anchor());
			if (renderers == null)
				continue;
			Matrix4f matrix = new Matrix4f();
			if (!PPlayerAnimations.attachmentMatrix(player, pose.animation(), pose.anchor(), partialTick, matrix))
			{
				continue;
			}
			for (PPlayerAnimatedAttachmentRenderer renderer : renderers.stream().
					sorted(Comparator.comparing(value -> value.getClass().getName())).toList())
			{
				poseStack.pushPose();
				try
				{
					poseStack.mulPose(matrix);
					renderer.render(new PPlayerAnimatedAttachmentContext(player, pose.animation(), pose.anchor(), pose.transform(),
						pose.weight(), false, poseStack, buffers, packedLight, partialTick));
				}
				finally
				{
					poseStack.popPose();
				}
			}
		}
	}

	/**
	 * Renders animation-owned anchors from an already resolved first-person presentation.
	 */
	public static void renderFirstPerson(AbstractClientPlayer player, PFirstPersonRenderPresentation presentation,
	                                     PoseStack poseStack, MultiBufferSource buffers, int packedLight, float partialTick)
	{
		for (PPlayerFirstPersonAnchorPose pose : presentation.animationAnchors())
		{
			List<PPlayerAnimatedAttachmentRenderer> renderers = RENDERERS.get(pose.anchor());
			if (renderers == null)
				continue;
			for (PPlayerAnimatedAttachmentRenderer renderer : renderers.stream().
					sorted(Comparator.comparing(value -> value.getClass().getName())).toList())
			{
				poseStack.pushPose();
				try
				{
					Matrix4f matrix = new Matrix4f();
					if (PPlayerAnimations.firstPersonAttachmentMatrix(player, pose.animation(), pose.anchor(), partialTick, matrix))
					{
						PPlayerAnimationSpace.applyChannelOverride(matrix, pose.transform(), PTransform.fromMatrix(matrix));
					}
					else
					{
						pose.transform().matrix(matrix);
					}
					poseStack.mulPose(matrix);
					renderer.render(new PPlayerAnimatedAttachmentContext(player, pose.animation(), pose.anchor(), pose.transform(),
						pose.weight(), true, poseStack, buffers, packedLight, partialTick));
				}
				finally
				{
					poseStack.popPose();
				}
			}
		}
	}
}
