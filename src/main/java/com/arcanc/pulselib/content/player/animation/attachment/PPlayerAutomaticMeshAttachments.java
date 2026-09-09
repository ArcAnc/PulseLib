/**
 * @author ArcAnc
 * Created at: 09.09.2026
 * Copyright (c) 2026
 */

package com.arcanc.pulselib.content.player.animation.attachment;

import com.arcanc.pulselib.content.model.animation.BoneFrame;
import com.arcanc.pulselib.content.model.baked.PBakedBone;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationDefinition;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationFrame;
import com.arcanc.pulselib.content.player.animation.firstPerson.PPlayerFirstPersonMeshAttachmentPose;
import com.arcanc.pulselib.util.PRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;

import java.util.*;

public final class PPlayerAutomaticMeshAttachments
{
	private static final java.util.Map<PPlayerAnimationDefinition, List<PBakedBone>> ROOTS =
			Collections.synchronizedMap(new IdentityHashMap<>());

	private PPlayerAutomaticMeshAttachments()
	{
	}

	public static List<PBakedBone> roots(PPlayerAnimationDefinition definition)
	{
		if (definition.modelData().getModel() == null)
			return List.of();
		List<PBakedBone> cached = ROOTS.get(definition);
		if (cached != null)
			return cached;

		Set<String> skeletonBones = new HashSet<>(definition.bindings().values());
		List<PBakedBone> roots = new ArrayList<>();
		for (PBakedBone bone : definition.modelData().getModel().bones())
			findRoots(bone, skeletonBones, false, roots);
		List<PBakedBone> resolved = List.copyOf(roots);
		ROOTS.put(definition, resolved);
		return resolved;
	}

	private static void findRoots(PBakedBone bone,
	                              Set<String> skeletonBones,
	                              boolean belongsToSkeleton,
	                              List<PBakedBone> roots)
	{
		boolean inSkeleton = belongsToSkeleton || skeletonBones.contains(bone.name());
		if (inSkeleton)
			return;
		if (containsMesh(bone))
		{
			roots.add(bone);
			return;
		}
		for (PBakedBone child : bone.children())
			findRoots(child, skeletonBones, false, roots);
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
			render(pose.root(), pose.frame(), pose.transform(), poseStack, packedLight);
	}

	public static void renderFirstPerson(List<PPlayerFirstPersonMeshAttachmentPose> poses,
	                                    PoseStack poseStack,
	                                    SubmitNodeCollector submitNodeCollector,
	                                    int packedLight)
	{
		for (PPlayerFirstPersonMeshAttachmentPose pose : poses)
			render(pose.root(), pose.frame(), pose.transform(), poseStack, packedLight);
	}

	private static void render(PBakedBone root,
	                           PPlayerAnimationFrame frame,
	                           Matrix4f transform,
	                           PoseStack poseStack,
	                           int packedLight)
	{
		BoneFrame local = frame.localTransform(root.name());
		if (local == null)
			return;

		Matrix4f localMatrix = new Matrix4f().translationRotateScale(
				local.translation(), local.rotation(), local.scale());
		poseStack.pushPose();
		try
		{
			// transform is the full root transform.  The bone renderer applies
			// its local transform once, so remove it here to avoid applying it twice.
			poseStack.mulPose(transform);
			poseStack.mulPose(localMatrix.invert());
			root.instantDraw(
					poseStack,
					frame.resolver(),
					(bone, mesh, inherited) -> inherited,
					new com.arcanc.pulselib.content.model.baked.PMeshRenderContext(
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
}
