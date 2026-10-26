/**
 * @author ArcAnc
 * Created at: 09.09.2026
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
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationSpace;
import com.arcanc.pulselib.content.player.animation.firstPerson.PFirstPersonPresentation;
import com.arcanc.pulselib.content.player.animation.firstPerson.PPlayerFirstPersonMeshAttachmentPose;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.PResourceCache;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.texture.OverlayTexture;

import org.joml.Matrix4f;
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
			findRoots(bone, skeletonBones, animatedBones, false, roots);
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
	 * @param animatedParent whether the parent inherits animation.
	 * @param roots the roots to use.
	 */
	private static void findRoots(PBakedBone bone,
								  Set<String> skeletonBones,
								  Set<String> animatedBones,
								  boolean animatedParent,
								  List<PBakedBone> roots)
	{
		boolean inheritsAnimation = animatedParent || skeletonBones.contains(bone.name()) || animatedBones.contains(bone.name());
		if (!skeletonBones.contains(bone.name()) && (inheritsAnimation || containsAnimatedBone(bone, animatedBones)) && containsMesh(bone))
		{
			roots.add(bone);
			return;
		}
		for (PBakedBone child : bone.children())
			findRoots(child, skeletonBones, animatedBones, inheritsAnimation, roots);
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
	 * Submits sampled animation-model mesh attachments in third-person player space.
	 *
	 * @param poses the poses
	 * @param poseStack the current model-to-render pose stack
	 * @param packedLight the packed block and sky light coordinates
	 */
	public static void renderThirdPerson(List<PPlayerAnimationMeshAttachmentPose> poses, PoseStack poseStack, int packedLight)
	{
		for (PPlayerAnimationMeshAttachmentPose pose : poses)
			if (pose.weight() > 1.0e-4f)
			{
				Matrix4f matrix = new Matrix4f();
				if (pose.frame().currentRootRelativeMatrix(pose.root().name(), matrix))
				{
					PPlayerAnimationSpace.toPlayerGeometrySpace(matrix, pose.frame().definition());
					PTransform sampled = PPlayerAnimationSpace.toPlayerGeometrySpace(
						pose.frame().currentRootRelativeTransform(pose.root().name()), pose.frame().definition());
					PPlayerAnimationSpace.applyChannelOverride(matrix, pose.transform(), sampled);
					submit(pose.root(), pose.frame(), matrix, poseStack, packedLight);
				}
			}
	}

	/**
	 * Draws model mesh attachments after their first-person presentation has been resolved.
	 *
	 * @param poses the poses
	 * @param poseStack the current model-to-render pose stack
	 * @param packedLight the packed block and sky light coordinates
	 */
	public static void renderFirstPerson(List<PPlayerFirstPersonMeshAttachmentPose> poses, PoseStack poseStack, int packedLight)
	{
		for (PPlayerFirstPersonMeshAttachmentPose pose : poses)
			if (pose.weight() > 1.0e-4f)
			{
				PFirstPersonPresentation presentation = PFirstPersonPresentation.create(pose.frame());
				Matrix4f matrix = new Matrix4f();
				if (presentation != null && presentation.boneMatrix(pose.root().name(), matrix))
				{
					PTransform sampled = PPlayerAnimationSpace.toFirstPersonGeometrySpace(PTransform.fromMatrix(matrix), pose.frame().definition());
					PPlayerAnimationSpace.applyChannelOverride(matrix, pose.transform(), sampled);
					submitFirstPerson(pose.root(), pose.frame(), matrix, poseStack, packedLight);
				}
			}
	}

	/**
	 * First-person meshes are drawn after Minecraft has written hand depth. An
	 * immediate draw here would still test against the world depth buffer.
	 *
	 * @param root the root
	 * @param frame the frame matrices or sampled animation values used by this operation
	 * @param transform the transform
	 * @param poseStack the current model-to-render pose stack
	 * @param packedLight the packed block and sky light coordinates
	 */
	private static void submitFirstPerson(PBakedBone root, PPlayerAnimationFrame frame,
										  Matrix4f transform,
										  PoseStack poseStack, int packedLight)
	{
		BoneFrame local = frame.localTransform(root.name());
		if (local == null)
			return;
		poseStack.pushPose();
		try
		{
			poseStack.mulPose(transform);
			poseStack.scale(safeInverse(local.scale().x), safeInverse(local.scale().y), safeInverse(local.scale().z));
			poseStack.mulPose(new Quaternionf(local.rotation()).invert());
			poseStack.translate(-local.translation().x, -local.translation().y, -local.translation().z);
			submitFirstPersonBone(root, frame.resolver(), poseStack,
								  new PMeshRenderContext(PRenderTypes.RenderTypeProvider::trianglesTranslucent, -1, packedLight, OverlayTexture.NO_OVERLAY));
		}
		finally
		{
			poseStack.popPose();
		}
	}

	private static void submit(PBakedBone root, PPlayerAnimationFrame frame,
							   Matrix4f transform,
							   PoseStack poseStack, int packedLight)
	{
		BoneFrame local = frame.localTransform(root.name());
		if (local == null)
			return;
		poseStack.pushPose();
		try
		{
			poseStack.mulPose(transform);
			poseStack.scale(safeInverse(local.scale().x), safeInverse(local.scale().y), safeInverse(local.scale().z));
			poseStack.mulPose(new Quaternionf(local.rotation()).invert());
			poseStack.translate(-local.translation().x, -local.translation().y, -local.translation().z);
			submitBone(root, frame.resolver(), poseStack,
					   new PMeshRenderContext(PRenderTypes.RenderTypeProvider::trianglesTranslucent, -1, packedLight, OverlayTexture.NO_OVERLAY));
		}
		finally
		{
			poseStack.popPose();
		}
	}

	private static void submitBone(PBakedBone bone, PAnimationPoseResolver<?> resolver,
								   PoseStack poseStack, PMeshRenderContext inherited)
	{
		if (!resolver.isVisible(bone))
			return;
		BoneFrame frame = resolver.resolve(bone).localTransform();
		poseStack.pushPose();
		try
		{
			poseStack.translate(frame.translation().x(), frame.translation().y(), frame.translation().z());
			poseStack.mulPose(frame.rotation());
			poseStack.scale(frame.scale().x(), frame.scale().y(), frame.scale().z());
			for (PBakedMesh mesh : bone.meshes())
			{
				if (mesh.textureReference().isEmpty())
					continue;
				PMeshRenderMaterial material = PMeshRenderMaterial.resolve(mesh, inherited);
				PRenderQueue.submitEntityMesh(
					material.resolveRenderType(inherited, PResourceCache.ATLAS_LOCATION),
					material.mesh().geometry(),
					new PInstanceHeader(new Matrix4f(poseStack.last().pose()), inherited.color(),
										material.packedLight(), inherited.packedOverlay()));
			}
			for (PBakedBone child : bone.children())
				submitBone(child, resolver, poseStack, inherited);
		}
		finally
		{
			poseStack.popPose();
		}
	}

	private static void submitFirstPersonBone(PBakedBone bone, PAnimationPoseResolver<?> resolver,
											  PoseStack poseStack, PMeshRenderContext inherited)
	{
		if (!resolver.isVisible(bone))
			return;
		BoneFrame frame = resolver.resolve(bone).localTransform();
		poseStack.pushPose();
		try
		{
			poseStack.translate(frame.translation().x(), frame.translation().y(), frame.translation().z());
			poseStack.mulPose(frame.rotation());
			poseStack.scale(frame.scale().x(), frame.scale().y(), frame.scale().z());
			for (PBakedMesh mesh : bone.meshes())
			{
				if (mesh.textureReference().isEmpty())
					continue;
				PMeshRenderMaterial material = PMeshRenderMaterial.resolve(mesh, inherited);
				PRenderQueue.submitFirstPersonMesh(
					material.resolveRenderType(inherited, PResourceCache.ATLAS_LOCATION),
					material.mesh().geometry(),
					new PInstanceHeader(new Matrix4f(poseStack.last().pose()), inherited.color(),
										material.packedLight(), inherited.packedOverlay()));
			}
			for (PBakedBone child : bone.children())
				submitFirstPersonBone(child, resolver, poseStack, inherited);
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
