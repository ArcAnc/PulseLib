package com.arcanc.pulselib.content.player.animation.attachment;

import com.arcanc.pulselib.content.player.animation.PPlayerAnimationAnchorPose;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import com.arcanc.pulselib.content.player.animation.firstPerson.PFirstPersonRenderPresentation;
import com.arcanc.pulselib.content.player.animation.firstPerson.PPlayerFirstPersonAnchorPose;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Registry and third-person dispatcher for semantic player-animation anchors. */
public final class PPlayerAnimatedAttachments
{
	private static final Map<com.arcanc.pulselib.content.player.animation.PPlayerAnimationAnchor, List<PPlayerAnimatedAttachmentRenderer>> RENDERERS = new HashMap<>();

	private PPlayerAnimatedAttachments() {}

	public static void register(PPlayerAnimatedAttachmentRenderer renderer)
	{
		RENDERERS.computeIfAbsent(renderer.anchor(), ignored -> new ArrayList<>()).add(renderer);
	}

	public static void renderThirdPerson(AbstractClientPlayer player, PoseStack poseStack,
	                                     MultiBufferSource buffers, int packedLight, float partialTick)
	{
		for (PPlayerAnimationAnchorPose pose : PPlayerAnimations.animationAnchorPoses(player, partialTick))
		{
			List<PPlayerAnimatedAttachmentRenderer> renderers = RENDERERS.get(pose.anchor());
			if (renderers == null) continue;
			for (PPlayerAnimatedAttachmentRenderer renderer : renderers.stream().sorted(Comparator.comparing(value -> value.getClass().getName())).toList())
			{
				poseStack.pushPose();
				try
				{
					poseStack.mulPose(pose.transform().matrix());
					renderer.render(new PPlayerAnimatedAttachmentContext(player, pose.animation(), pose.anchor(), pose.transform(),
							pose.weight(), false, poseStack, buffers, packedLight, partialTick));
				}
				finally { poseStack.popPose(); }
			}
		}
	}

	/** Renders animation-owned anchors from an already resolved first-person presentation. */
	public static void renderFirstPerson(AbstractClientPlayer player, PFirstPersonRenderPresentation presentation,
	                                     PoseStack poseStack, MultiBufferSource buffers, int packedLight, float partialTick)
	{
		for (PPlayerFirstPersonAnchorPose pose : presentation.animationAnchors())
		{
			List<PPlayerAnimatedAttachmentRenderer> renderers = RENDERERS.get(pose.anchor());
			if (renderers == null) continue;
			for (PPlayerAnimatedAttachmentRenderer renderer : renderers.stream().sorted(Comparator.comparing(value -> value.getClass().getName())).toList())
			{
				poseStack.pushPose();
				try
				{
					poseStack.mulPose(pose.transform().matrix());
					renderer.render(new PPlayerAnimatedAttachmentContext(player, pose.animation(), pose.anchor(), pose.transform(),
							pose.weight(), true, poseStack, buffers, packedLight, partialTick));
				}
				finally { poseStack.popPose(); }
			}
		}
	}
}
