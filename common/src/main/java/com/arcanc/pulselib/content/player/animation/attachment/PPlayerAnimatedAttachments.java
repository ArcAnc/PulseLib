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
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationSpace;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import com.arcanc.pulselib.content.player.animation.firstPerson.PFirstPersonRenderPresentation;
import com.arcanc.pulselib.content.player.animation.firstPerson.PPlayerFirstPersonAnchorPose;
import com.arcanc.pulselib.content.renderer.base.PRenderCallbackScope;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

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
		List<PPlayerAnimatedAttachmentRenderer> renderers = RENDERERS.computeIfAbsent(Objects.requireNonNull(renderer.anchor()), ignored -> new ArrayList<>());
		if (!renderers.contains(renderer))
			renderers.add(renderer);
	}

	static boolean hasRenderer(PPlayerAnimationAnchor anchor)
	{
		List<PPlayerAnimatedAttachmentRenderer> renderers = RENDERERS.get(anchor);
		return renderers != null && !renderers.isEmpty();
	}

	/**
	 * Renders the third person.
	 * @param player the player to use.
	 * @param poseStack the pose stack to use.
	 * @param buffers the submit node collector to use.
	 * @param packedLight the packed light to use.
	 * @param partialTick the partial tick to use.
	 */
	public static void renderThirdPerson(AbstractClientPlayer player,
	                                     PoseStack poseStack,
	                                     SubmitNodeCollector buffers,
	                                     int packedLight,
	                                     float partialTick)
	{
		try (var invocation = PRenderCallbackScope.player(poseStack, player))
		{
			for (PPlayerAnimationAnchorPose pose : PPlayerAnimations.animationAnchorPoses(player, partialTick))
			{
				Matrix4f matrix = new Matrix4f();
				if (PPlayerAnimations.attachmentMatrix(player, pose.animation(), pose.anchor(), partialTick, matrix))
					render(player, pose.animation(), pose.anchor(), pose.transform(), matrix, pose.weight(), poseStack, buffers, packedLight, partialTick);
			}
		}
	}

	/**
	 * Renders the first person.
	 * @param player the player to use.
	 * @param presentation the first person presentation to use.
	 * @param poseStack the pose stack to use.
	 * @param buffers the submit node collector to use.
	 * @param packedLight the packed light to use.
	 * @param partialTick the partial tick to use.
	 */
	public static void renderFirstPerson(AbstractClientPlayer player, PFirstPersonRenderPresentation presentation,
	                                     PoseStack poseStack, SubmitNodeCollector buffers, int packedLight, float partialTick)
	{
		try (var invocation = PRenderCallbackScope.player(poseStack, player))
		{
			for (PPlayerFirstPersonAnchorPose pose : presentation.animationAnchors())
			{
				List<PPlayerAnimatedAttachmentRenderer> renderers = RENDERERS.get(pose.anchor());
				if (renderers == null)
					continue;
				for (PPlayerAnimatedAttachmentRenderer renderer : renderers.stream().
						sorted(Comparator.comparing(value -> value.getClass().getName())).toList())
					try (var callback = PRenderCallbackScope.player(poseStack, player))
					{
						poseStack.pushPose();
						Matrix4f matrix = new Matrix4f();
						if (PPlayerAnimations.firstPersonAttachmentMatrix(player, pose.animation(), pose.anchor(), partialTick, matrix))
							PPlayerAnimationSpace.applyChannelOverride(matrix, pose.transform(), PTransform.fromMatrix(matrix));
						else
							pose.transform().matrix(matrix);
						poseStack.mulPose(matrix);
						renderer.render(new PPlayerAnimatedAttachmentContext(player, pose.animation(), pose.anchor(), pose.transform(),
							pose.weight(), true, poseStack, buffers, packedLight, partialTick));
					}
			}
		}
	}

	/**
	 * Performs the render operation.
	 * @param player the player to use.
	 * @param animation the animation to use.
	 * @param anchor the anchor to use.
	 * @param transform the transform to use.
	 * @param matrix the exact attachment matrix, including inherited affine transforms.
	 * @param weight the weight to use.
	 * @param poseStack the pose stack to use.
	 * @param buffers the submit node collector to use.
	 * @param packedLight the packed light to use.
	 * @param partialTick the partial tick to use.
	 */
	private static void render(AbstractClientPlayer player,
	                           Identifier animation,
	                           PPlayerAnimationAnchor anchor,
	                           PTransform transform,
	                           Matrix4f matrix,
	                           float weight,
	                           PoseStack poseStack,
	                           SubmitNodeCollector buffers,
	                           int packedLight,
	                           float partialTick)
	{
		List<PPlayerAnimatedAttachmentRenderer> renderers = RENDERERS.get(anchor);
		if (renderers == null)
			return;
		for (PPlayerAnimatedAttachmentRenderer renderer : renderers.stream().sorted(Comparator.comparing(renderer -> renderer.getClass().getName())).toList())
			try (var callback = PRenderCallbackScope.player(poseStack, player))
			{
				poseStack.pushPose();
				poseStack.mulPose(matrix);
				renderer.render(new PPlayerAnimatedAttachmentContext(player, animation, anchor, transform, weight, false, poseStack, buffers, packedLight, partialTick));
			}
	}
}
