/**
 * @author ArcAnc
 * Created at: 09.09.2026
 * Copyright (c) 2026
 */

package com.arcanc.pulselib.content.player.animation.attachment;

import com.arcanc.pulselib.content.model.animation.BoneFrame;
import com.arcanc.pulselib.content.model.animation.PTransform;
import com.arcanc.pulselib.content.model.baked.PBakedBone;
import com.arcanc.pulselib.content.model.baked.PMeshRenderContext;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationDefinition;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationFrame;
import com.arcanc.pulselib.util.PRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Provides support for player mesh attachments selected automatically or explicitly.
 */
public final class PPlayerAutomaticMeshAttachments
{
	/**
	 * Creates an instance of the enclosing type.
	 */
	private PPlayerAutomaticMeshAttachments()
	{
	}

	/**
	 * Resolves mesh attachment roots for an active animation. Explicit roots take
	 * precedence over automatic discovery.
	 * @param frame the frame to use.
	 * @return the value produced by this operation.
	 */
	public static List<PBakedBone> roots(PPlayerAnimationFrame frame)
	{
		PPlayerAnimationDefinition definition = frame.definition();
		if (definition.modelData().getModel() == null)
			return List.of();
		Set<String> animatedBones = frame.resolver().activeAnimationBones();
		if (animatedBones.isEmpty())
			return List.of();
		if (!definition.meshAttachmentRoots().isEmpty())
			return explicitRoots(definition);

		Set<String> skeletonBones = new HashSet<>(definition.bindings().values());
		List<PBakedBone> roots = new ArrayList<>();
		for (PBakedBone bone : definition.modelData().getModel().bones())
			findRoots(bone, skeletonBones, animatedBones, roots);
		return List.copyOf(roots);
	}

	/**
	 * Resolves explicitly configured attachment roots. A descendant of another
	 * configured root is omitted because rendering its ancestor already renders it.
	 * @param definition the animation definition to use.
	 * @return explicitly configured attachment roots.
	 */
	private static List<PBakedBone> explicitRoots(PPlayerAnimationDefinition definition)
	{
		Set<String> configuredRoots = definition.meshAttachmentRoots();
		List<PBakedBone> roots = new ArrayList<>();
		for (int index = 0; index < definition.modelData().getModel().boneCount(); index++)
		{
			PBakedBone bone = definition.modelData().getModel().bone(index);
			if (!configuredRoots.contains(bone.name()) || hasConfiguredAncestor(bone, configuredRoots))
				continue;
			roots.add(bone);
		}
		return List.copyOf(roots);
	}

	/**
	 * Determines whether this bone is already rendered by a configured ancestor.
	 * @param bone the bone to use.
	 * @param configuredRoots explicitly configured root names.
	 * @return whether a configured ancestor exists.
	 */
	private static boolean hasConfiguredAncestor(PBakedBone bone, Set<String> configuredRoots)
	{
		for (PBakedBone parent = bone.parent(); parent != null; parent = parent.parent())
			if (configuredRoots.contains(parent.name()))
				return true;
		return false;
	}

	/**
	 * Finds the roots.
	 * @param bone the bone to use.
	 * @param skeletonBones the skeleton bones to use.
	 * @param animatedBones the animated bones to use.
	 * @param roots the roots to use.
	 */
	private static void findRoots(PBakedBone bone,
	                              Set<String> skeletonBones,
	                              Set<String> animatedBones,
	                              List<PBakedBone> roots)
	{
		if (skeletonBones.contains(bone.name()))
			return;
		if (!containsAnimatedBone(bone, animatedBones))
			return;
		if (containsMesh(bone))
		{
			roots.add(bone);
			return;
		}
		for (PBakedBone child : bone.children())
			findRoots(child, skeletonBones, animatedBones, roots);
	}

	/**
	 * Performs the contains animated bone operation.
	 * @param bone the bone to use.
	 * @param animatedBones the animated bones to use.
	 * @return the value produced by this operation.
	 */
	private static boolean containsAnimatedBone(PBakedBone bone, Set<String> animatedBones)
	{
		if (animatedBones.contains(bone.name()))
			return true;
		return bone.children().stream().anyMatch(child -> containsAnimatedBone(child, animatedBones));
	}

	/**
	 * Performs the contains mesh operation.
	 * @param bone the bone to use.
	 * @return the value produced by this operation.
	 */
	private static boolean containsMesh(PBakedBone bone)
	{
		if (!bone.meshes().isEmpty())
			return true;
		return bone.children().stream().anyMatch(PPlayerAutomaticMeshAttachments::containsMesh);
	}

	/**
	 * Renders the third person.
	 * @param poses the poses to use.
	 * @param poseStack the pose stack to use.
	 * @param packedLight the packed light to use.
	 */
	public static void renderThirdPerson(List<PPlayerAnimationMeshAttachmentPose> poses,
	                                     PoseStack poseStack,
	                                     int packedLight)
	{
		for (PPlayerAnimationMeshAttachmentPose pose : poses)
			if (pose.weight() > 1.0e-4f)
				render(pose.root(), pose.frame(), pose.transform(), poseStack, packedLight);
	}

	/**
	 * Performs the render operation.
	 * @param root the root to use.
	 * @param frame the frame to use.
	 * @param transform the transform to use.
	 * @param poseStack the pose stack to use.
	 * @param packedLight the packed light to use.
	 */
	private static void render(PBakedBone root,
	                           PPlayerAnimationFrame frame,
	                           PTransform transform,
	                           PoseStack poseStack,
	                           int packedLight)
	{
		BoneFrame local = frame.localTransform(root.name());
		if (local == null)
			return;

		poseStack.pushPose();
		try
		{
			poseStack.mulPose(transform.matrix());
			poseStack.scale(safeInverse(local.scale().x), safeInverse(local.scale().y), safeInverse(local.scale().z));
			poseStack.mulPose(new Quaternionf(local.rotation()).invert());
			poseStack.translate(-local.translation().x, -local.translation().y, -local.translation().z);
			root.instantDraw(
					poseStack,
					frame.resolver(),
					(bone, mesh, inherited) -> inherited,
					new PMeshRenderContext(
							PRenderTypes.RenderTypeProvider::trianglesTranslucent,
							-1,
							packedLight,
							OverlayTexture.NO_OVERLAY));
		}
		finally
		{
			poseStack.popPose();
		}
	}

	/**
	 * Performs the safe inverse operation.
	 * @param value the value to use.
	 * @return the value produced by this operation.
	 */
	private static float safeInverse(float value)
	{
		return Math.abs(value) < 1.0e-6f ? 0.0f : 1.0f / value;
	}
}
