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
import com.arcanc.pulselib.content.player.animation.firstPerson.PPlayerFirstPersonMeshAttachmentPose;
import com.arcanc.pulselib.util.PRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PPlayerAutomaticMeshAttachments
{
	private PPlayerAutomaticMeshAttachments()
	{
	}

	public static List<PBakedBone> roots(PPlayerAnimationFrame frame)
	{
		PPlayerAnimationDefinition definition = frame.definition();
		if (definition.modelData().getModel() == null)
			return List.of();

		Set<String> skeletonBones = new HashSet<>(definition.bindings().values());
		Set<String> animatedBones = frame.resolver().activeAnimationBones();
		List<PBakedBone> roots = new ArrayList<>();
		for (PBakedBone bone : definition.modelData().getModel().bones())
			findRoots(bone, skeletonBones, animatedBones, roots);
		return List.copyOf(roots);
	}

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

	private static boolean containsAnimatedBone(PBakedBone bone, Set<String> animatedBones)
	{
		if (animatedBones.contains(bone.name()))
			return true;
		return bone.children().stream().anyMatch(child -> containsAnimatedBone(child, animatedBones));
	}

	private static boolean containsMesh(PBakedBone bone)
	{
		if (!bone.meshes().isEmpty())
			return true;
		return bone.children().stream().anyMatch(PPlayerAutomaticMeshAttachments::containsMesh);
	}

	public static void renderThirdPerson(List<PPlayerAnimationMeshAttachmentPose> poses,
	                                     PoseStack poseStack,
	                                     SubmitNodeCollector submitNodeCollector,
	                                     int packedLight)
	{
		for (PPlayerAnimationMeshAttachmentPose pose : poses)
			if (pose.weight() > 1.0e-4f)
				render(pose.root(), pose.frame(), pose.transform(), poseStack, packedLight);
	}

	public static void renderFirstPerson(List<PPlayerFirstPersonMeshAttachmentPose> poses,
	                                    PoseStack poseStack,
	                                    SubmitNodeCollector submitNodeCollector,
	                                    int packedLight)
	{
		for (PPlayerFirstPersonMeshAttachmentPose pose : poses)
			if (pose.weight() > 1.0e-4f)
				render(pose.root(), pose.frame(), pose.transform(), poseStack, packedLight);
	}

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
			poseStack.mulPose(new org.joml.Quaternionf(local.rotation()).invert());
			poseStack.translate(-local.translation().x, -local.translation().y, -local.translation().z);
			root.instantDraw(
					poseStack,
					frame.resolver(),
					(bone, mesh, inherited) -> inherited,
					new PMeshRenderContext(
							PRenderTypes.RenderTypeProvider::trianglesCutout,
							-1,
							packedLight,
							OverlayTexture.NO_OVERLAY));
		}
		finally
		{
			poseStack.popPose();
		}
	}

	private static float safeInverse(float value)
	{
		return Math.abs(value) < 1.0e-6f ? 0.0f : 1.0f / value;
	}
}
