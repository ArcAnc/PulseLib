package com.arcanc.pulselib.content.player.animation.attachment;

import com.arcanc.pulselib.content.model.animation.BoneFrame;
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
			if (pose.weight() > 1.0e-4f) render(pose.root(), pose.frame(), pose.transform(), poseStack, packedLight);
	}

	private static void render(PBakedBone root, PPlayerAnimationFrame frame,
	                           com.arcanc.pulselib.content.model.animation.PTransform transform,
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
			root.instantDraw(poseStack, frame.resolver(), (bone, mesh, inherited) -> inherited,
					new PMeshRenderContext(PRenderTypes.RenderTypeProvider::trianglesTranslucent, -1, packedLight, OverlayTexture.NO_OVERLAY));
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
