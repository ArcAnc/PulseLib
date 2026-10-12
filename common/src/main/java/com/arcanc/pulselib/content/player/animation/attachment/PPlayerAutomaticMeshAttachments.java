/**
 * @author ArcAnc
 * Created at: 23.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation.attachment;

import com.arcanc.pulselib.content.model.animation.BoneFrame;
import com.arcanc.pulselib.content.model.animation.PAnimationPoseResolver;
import com.arcanc.pulselib.content.model.animation.PTransform;
import com.arcanc.pulselib.content.model.baked.PBakedBone;
import com.arcanc.pulselib.content.model.baked.PBakedMesh;
import com.arcanc.pulselib.content.model.baked.PMeshRenderContext;
import com.arcanc.pulselib.content.model.baked.PMeshRenderMaterial;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationDefinition;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationFrame;
import com.arcanc.pulselib.content.player.animation.firstPerson.PPlayerFirstPersonMeshAttachmentPose;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.util.PResourceCache;
import com.arcanc.pulselib.util.PRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Selects and draws mesh subtrees that are not represented by vanilla player parts. */
public final class PPlayerAutomaticMeshAttachments
{
	private PPlayerAutomaticMeshAttachments() {}

	public static List<PBakedBone> roots(PPlayerAnimationFrame frame)
	{
		PPlayerAnimationDefinition definition = frame.definition();
		if (definition.modelData().getModel() == null || frame.resolver().activeAnimationBones().isEmpty()) return List.of();
		Set<String> configured = definition.meshAttachmentRoots();
		List<PBakedBone> roots = new ArrayList<>();
		if (!configured.isEmpty())
		{
			for (int index = 0; index < definition.modelData().getModel().boneCount(); index++)
			{
				PBakedBone bone = definition.modelData().getModel().bone(index);
				if (configured.contains(bone.name()) && !hasConfiguredAncestor(bone, configured)) roots.add(bone);
			}
			return List.copyOf(roots);
		}
		Set<String> skeleton = new HashSet<>(definition.bindings().values());
		for (PBakedBone bone : definition.modelData().getModel().bones()) findRoots(bone, skeleton, frame.resolver().activeAnimationBones(), roots);
		return List.copyOf(roots);
	}

	public static void renderThirdPerson(List<PPlayerAnimationMeshAttachmentPose> poses, PoseStack poseStack, int packedLight)
	{
		for (PPlayerAnimationMeshAttachmentPose pose : poses)
			if (pose.weight() > 1.0e-4f) submit(pose.root(), pose.frame(), pose.transform(), poseStack, packedLight);
	}

	/** Draws model mesh attachments after their first-person presentation has been resolved. */
	public static void renderFirstPerson(List<PPlayerFirstPersonMeshAttachmentPose> poses, PoseStack poseStack, int packedLight)
	{
		for (PPlayerFirstPersonMeshAttachmentPose pose : poses)
			if (pose.weight() > 1.0e-4f) submitFirstPerson(pose.root(), pose.frame(), pose.transform(), poseStack, packedLight);
	}

	/**
	 * First-person meshes are drawn after Minecraft has written hand depth. An
	 * immediate draw here would still test against the world depth buffer.
	 */
	private static void submitFirstPerson(PBakedBone root, PPlayerAnimationFrame frame,
	                                      PTransform transform,
	                                      PoseStack poseStack, int packedLight)
	{
		BoneFrame local = frame.localTransform(root.name());
		if (local == null) return;
		poseStack.pushPose();
		try
		{
			poseStack.mulPose(transform.matrix());
			poseStack.scale(inverse(local.scale().x), inverse(local.scale().y), inverse(local.scale().z));
			poseStack.mulPose(new Quaternionf(local.rotation()).invert());
			poseStack.translate(-local.translation().x, -local.translation().y, -local.translation().z);
			submitFirstPersonBone(root, frame.resolver(), poseStack,
					new PMeshRenderContext(PRenderTypes.RenderTypeProvider::trianglesTranslucent, -1, packedLight, OverlayTexture.NO_OVERLAY));
		}
		finally { poseStack.popPose(); }
	}

	private static void submit(PBakedBone root, PPlayerAnimationFrame frame,
	                           PTransform transform,
	                           PoseStack poseStack, int packedLight)
	{
		BoneFrame local = frame.localTransform(root.name());
		if (local == null) return;
		poseStack.pushPose();
		try
		{
			poseStack.mulPose(transform.matrix());
			poseStack.scale(inverse(local.scale().x), inverse(local.scale().y), inverse(local.scale().z));
			poseStack.mulPose(new Quaternionf(local.rotation()).invert());
			poseStack.translate(-local.translation().x, -local.translation().y, -local.translation().z);
			submitBone(root, frame.resolver(), poseStack,
					new PMeshRenderContext(PRenderTypes.RenderTypeProvider::trianglesTranslucent, -1, packedLight, OverlayTexture.NO_OVERLAY));
		}
		finally { poseStack.popPose(); }
	}

	private static void submitBone(PBakedBone bone, PAnimationPoseResolver<?> resolver,
	                               PoseStack poseStack, PMeshRenderContext inherited)
	{
		if (!resolver.isVisible(bone)) return;
		BoneFrame frame = resolver.resolve(bone).localTransform();
		poseStack.pushPose();
		try
		{
			poseStack.translate(frame.translation().x(), frame.translation().y(), frame.translation().z());
			poseStack.mulPose(frame.rotation());
			poseStack.scale(frame.scale().x(), frame.scale().y(), frame.scale().z());
			for (PBakedMesh mesh : bone.meshes())
			{
				if (mesh.textureReference().isEmpty()) continue;
				PMeshRenderMaterial material = PMeshRenderMaterial.resolve(mesh, inherited);
				PRenderQueue.submitEntityMesh(
						material.resolveRenderType(inherited, PResourceCache.ATLAS_LOCATION),
						material.mesh().geometry(),
						new PInstanceHeader(new Matrix4f(poseStack.last().pose()), inherited.color(),
								material.packedLight(), inherited.packedOverlay()));
			}
			for (PBakedBone child : bone.children()) submitBone(child, resolver, poseStack, inherited);
		}
		finally { poseStack.popPose(); }
	}

	private static void submitFirstPersonBone(PBakedBone bone, PAnimationPoseResolver<?> resolver,
	                                           PoseStack poseStack, PMeshRenderContext inherited)
	{
		if (!resolver.isVisible(bone)) return;
		BoneFrame frame = resolver.resolve(bone).localTransform();
		poseStack.pushPose();
		try
		{
			poseStack.translate(frame.translation().x(), frame.translation().y(), frame.translation().z());
			poseStack.mulPose(frame.rotation());
			poseStack.scale(frame.scale().x(), frame.scale().y(), frame.scale().z());
			for (PBakedMesh mesh : bone.meshes())
			{
				if (mesh.textureReference().isEmpty()) continue;
				PMeshRenderMaterial material = PMeshRenderMaterial.resolve(mesh, inherited);
				PRenderQueue.submitFirstPersonMesh(
						material.resolveRenderType(inherited, PResourceCache.ATLAS_LOCATION),
						material.mesh().geometry(),
						new PInstanceHeader(new Matrix4f(poseStack.last().pose()), inherited.color(),
								material.packedLight(), inherited.packedOverlay()));
			}
			for (PBakedBone child : bone.children()) submitFirstPersonBone(child, resolver, poseStack, inherited);
		}
		finally { poseStack.popPose(); }
	}

	private static boolean hasConfiguredAncestor(PBakedBone bone, Set<String> configured)
	{
		for (PBakedBone parent = bone.parent(); parent != null; parent = parent.parent()) if (configured.contains(parent.name())) return true;
		return false;
	}
	private static void findRoots(PBakedBone bone, Set<String> skeleton, Set<String> animated, List<PBakedBone> roots)
	{
		if (skeleton.contains(bone.name()) || !containsAnimated(bone, animated)) return;
		if (containsMesh(bone)) { roots.add(bone); return; }
		for (PBakedBone child : bone.children()) findRoots(child, skeleton, animated, roots);
	}
	private static boolean containsAnimated(PBakedBone bone, Set<String> animated)
	{
		return animated.contains(bone.name()) || bone.children().stream().anyMatch(child -> containsAnimated(child, animated));
	}
	private static boolean containsMesh(PBakedBone bone)
	{
		return !bone.meshes().isEmpty() || bone.children().stream().anyMatch(PPlayerAutomaticMeshAttachments::containsMesh);
	}
	private static float inverse(float value) { return Math.abs(value) < 1.0e-6f ? 0f : 1f / value; }
}
