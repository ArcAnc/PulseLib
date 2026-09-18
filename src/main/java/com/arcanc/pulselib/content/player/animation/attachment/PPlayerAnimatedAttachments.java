/**
 * @author ArcAnc
 * Created at: 09.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation.attachment;

import com.arcanc.pulselib.content.model.animation.PTransform;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationAnchor;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationAnchorPose;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import com.arcanc.pulselib.content.player.animation.firstPerson.PPlayerFirstPersonAnchorPose;
import com.arcanc.pulselib.content.player.animation.firstPerson.PFirstPersonRenderPresentation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.resources.Identifier;

import java.util.*;

/**
 * Provides support for player animated attachments.
 */
public final class PPlayerAnimatedAttachments
{
	private static final Map<PPlayerAnimationAnchor, List<PPlayerAnimatedAttachmentRenderer>> RENDERERS = new HashMap<>();

	/**
	 * Creates an instance of the enclosing type.
	 */
	private PPlayerAnimatedAttachments()
	{
	}

	/**
	 * Performs the register operation.
	 * @param renderer the renderer to use.
	 */
	public static void register(PPlayerAnimatedAttachmentRenderer renderer)
	{
		Objects.requireNonNull(renderer);
		RENDERERS.computeIfAbsent(Objects.requireNonNull(renderer.anchor()), ignored -> new ArrayList<>()).add(renderer);
	}

	/**
	 * Renders the third person.
	 * @param player the player to use.
	 * @param poseStack the pose stack to use.
	 * @param submitNodeCollector the submit node collector to use.
	 * @param packedLight the packed light to use.
	 * @param partialTick the partial tick to use.
	 */
	public static void renderThirdPerson(AbstractClientPlayer player,
	                                     PoseStack poseStack,
	                                     SubmitNodeCollector submitNodeCollector,
	                                     int packedLight,
	                                     float partialTick)
	{
		for (PPlayerAnimationAnchorPose pose : PPlayerAnimations.animationAnchorPoses(player, partialTick))
			render(player, pose.animation(), pose.anchor(), pose.transform(), pose.weight(), false, poseStack, submitNodeCollector, packedLight, partialTick);
	}

	/**
	 * Renders the first person.
	 * @param player the player to use.
	 * @param firstPersonPresentation the first person presentation to use.
	 * @param poseStack the pose stack to use.
	 * @param submitNodeCollector the submit node collector to use.
	 * @param packedLight the packed light to use.
	 * @param partialTick the partial tick to use.
	 */
	public static void renderFirstPerson(AbstractClientPlayer player,
	                                    PFirstPersonRenderPresentation firstPersonPresentation,
	                                    PoseStack poseStack,
	                                    SubmitNodeCollector submitNodeCollector,
	                                    int packedLight,
	                                    float partialTick)
	{
		for (PPlayerFirstPersonAnchorPose pose : firstPersonPresentation.animationAnchors())
			render(player, pose.animation(), pose.anchor(), pose.transform(), pose.weight(), true, poseStack, submitNodeCollector, packedLight, partialTick);
	}

	/**
	 * Performs the render operation.
	 * @param player the player to use.
	 * @param animation the animation to use.
	 * @param anchor the anchor to use.
	 * @param transform the transform to use.
	 * @param weight the weight to use.
	 * @param firstPerson the first person to use.
	 * @param poseStack the pose stack to use.
	 * @param submitNodeCollector the submit node collector to use.
	 * @param packedLight the packed light to use.
	 * @param partialTick the partial tick to use.
	 */
	private static void render(AbstractClientPlayer player,
	                           Identifier animation,
	                           PPlayerAnimationAnchor anchor,
	                           PTransform transform,
	                           float weight,
	                           boolean firstPerson,
	                           PoseStack poseStack,
	                           SubmitNodeCollector submitNodeCollector,
	                           int packedLight,
	                           float partialTick)
	{
		List<PPlayerAnimatedAttachmentRenderer> renderers = RENDERERS.get(anchor);
		if (renderers == null)
			return;
		for (PPlayerAnimatedAttachmentRenderer renderer : renderers.stream().sorted(Comparator.comparing(renderer -> renderer.getClass().getName())).toList())
		{
			poseStack.pushPose();
			try
			{
				poseStack.mulPose(transform.matrix());
				renderer.render(new PPlayerAnimatedAttachmentContext(player, animation, anchor, transform, weight, firstPerson, poseStack, submitNodeCollector, packedLight, partialTick));
			}
			finally
			{
				poseStack.popPose();
			}
		}
	}
}
