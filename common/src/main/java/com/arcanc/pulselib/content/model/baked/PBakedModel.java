/**
 * @author ArcAnc
 * Created at: 28.01.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.baked;

import com.arcanc.pulselib.content.animatable.PAnimatable;
import com.arcanc.pulselib.content.animatable.PAnimationController;
import com.arcanc.pulselib.content.model.animation.*;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.data.gecko.MolangParser;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.Function;

/**
 * Render-ready model with indexed bones, parent indices, bind pose and compiled animation lookup.
 */
public final class PBakedModel
{
	private final List<PBakedBone> bones;
	private final Map<String, PAnimation> animations;
	private final PBakedBone[] indexedBones;
	private final Map<String, Integer> boneIndices;
	private final int[] parents;
	private final Map<String, PCompiledAnimation> compiledAnimations;

	/**
	 * Copies the bone hierarchy and animations, then builds indexed bone and animation lookup.
	 *
	 * @param bones the bones
	 * @param animations the animation clips indexed by source name
	 */
	public PBakedModel(List<PBakedBone> bones, Map<String, PAnimation> animations)
	{
		this.bones = List.copyOf(bones);
		this.animations = Map.copyOf(animations);
		List<PBakedBone> flatBones = new ArrayList<>();
		Map<String, Integer> indices = new HashMap<>();
		for (PBakedBone root : this.bones)
			index(root, flatBones, indices);
		this.indexedBones = flatBones.toArray(PBakedBone[] :: new);
		this.boneIndices = Map.copyOf(indices);
		this.parents = new int[this.indexedBones.length];
		for (int index = 0; index < this.indexedBones.length; index++)
		{
			PBakedBone parent = this.indexedBones[index].parent();
			this.parents[index] = parent == null ? -1 : this.boneIndices.get(parent.name());
		}
		this.compiledAnimations = compileAnimations();
	}

	/**
	 * Returns the bones.
	 *
	 * @return the bones
	 */
	public List<PBakedBone> bones()
	{
		return this.bones;
	}

	/**
	 * Returns the animation clips indexed by source name.
	 *
	 * @return the animation clips indexed by source name
	 */
	public Map<String, PAnimation> animations()
	{
		return this.animations;
	}

	/**
	 * Returns the number of bones in the indexed baked hierarchy.
	 *
	 * @return the number of bones in the indexed baked hierarchy
	 */
	public int boneCount()
	{
		return this.indexedBones.length;
	}

	/**
	 * Finds the index of a baked bone, returning the implementation's missing-index sentinel when absent.
	 *
	 * @param name the bone name
	 * @return the bone index, or {@code -1} when no matching bone exists
	 */
	public int boneIndex(String name)
	{
		return this.boneIndices.getOrDefault(name, -1);
	}

	/**
	 * Finds the index of a baked bone, returning the implementation's missing-index sentinel when absent.
	 *
	 * @param bone the baked bone being sampled or submitted
	 * @return the bone index, or {@code -1} when no matching bone exists
	 */
	public int boneIndex(PBakedBone bone)
	{
		return boneIndex(bone.name());
	}

	/**
	 * Returns the baked bone at the supplied model index.
	 *
	 * @param index the index
	 * @return the baked bone at the supplied model index
	 */
	public PBakedBone bone(int index)
	{
		return this.indexedBones[index];
	}

	/**
	 * Returns the parent index of a baked bone, or the root sentinel for a root bone.
	 *
	 * @param index the index
	 * @return the parent index of a baked bone, or the root sentinel for a root bone
	 */
	public int parentIndex(int index)
	{
		return this.parents[index];
	}

	/**
	 * Returns a defensive copy of the indexed parent hierarchy.
	 *
	 * @return a defensive copy of the indexed parent hierarchy
	 */
	public int[] parentIndices()
	{
		return this.parents.clone();
	}

	/**
	 * Returns the named animation indexed for this model, or {@code null} if absent.
	 *
	 * @param name the bone name
	 * @return the named animation indexed for this model, or {@code null} if absent
	 */
	public PCompiledAnimation compiledAnimation(String name)
	{
		return this.compiledAnimations.get(name);
	}

	/**
	 * Returns the model's unanimated local bone pose.
	 *
	 * @return the model's unanimated local bone pose
	 */
	public PPose bindPose()
	{
		PPose pose = new PPose(this.indexedBones.length);
		for (int index = 0; index < this.indexedBones.length; index++)
		{
			PBakedBone bone = this.indexedBones[index];
			pose.set(index, bone.basePosition(), bone.baseRotation(), new Vector3f(1f));
		}
		return pose;
	}

	/**
	 * Evaluates the supplied controllers and submits the model meshes for rendering.
	 *
	 * @param <T> the animatable owner type
	 * @param poseStack the current model-to-render pose stack
	 * @param modelData the canonical model and loader selection
	 * @param controllers the animation controllers in pose composition order
	 * @param renderType the render type
	 * @param color the packed ARGB vertex color
	 * @param packedLight the packed block and sky light coordinates
	 * @param packedOverlay the packed overlay texture coordinates
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 */
	public <T extends PAnimatable<T>> void instantDraw(PoseStack poseStack, PModelData modelData,
	                                                   Collection<PAnimationController<T>> controllers,
	                                                   Function<ResourceLocation, RenderType> renderType, int color,
	                                                   int packedLight, int packedOverlay, float partialTick)
	{
		instantDraw(poseStack, modelData, controllers, Map.of(), renderType, color, packedLight, packedOverlay, partialTick);
	}

	/**
	 * Evaluates the supplied controllers and submits the model meshes for rendering.
	 *
	 * @param <T> the animatable owner type
	 * @param poseStack the current model-to-render pose stack
	 * @param modelData the canonical model and loader selection
	 * @param controllers the animation controllers in pose composition order
	 * @param molangContexts the frame-specific Molang context of each controller
	 * @param renderType the render type
	 * @param color the packed ARGB vertex color
	 * @param packedLight the packed block and sky light coordinates
	 * @param packedOverlay the packed overlay texture coordinates
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 */
	public <T extends PAnimatable<T>> void instantDraw(PoseStack poseStack, PModelData modelData,
	                                                   Collection<PAnimationController<T>> controllers,
	                                                   Map<PAnimationController<T>, MolangParser.Context> molangContexts,
	                                                   Function<ResourceLocation, RenderType> renderType, int color,
	                                                   int packedLight, int packedOverlay, float partialTick)
	{
		PMeshRenderContext inherited = new PMeshRenderContext(renderType, color, packedLight, packedOverlay);
		PAnimationPoseResolver<T> poseResolver = poseResolver(controllers, molangContexts, partialTick);
		this.bones.forEach(bone -> bone.instantDraw(poseStack, poseResolver, (ignoredBone, mesh, context) -> context, inherited));
	}

	/**
	 * Evaluates the supplied controllers and submits the model meshes for rendering.
	 *
	 * @param <T> the animatable owner type
	 * @param poseStack the current model-to-render pose stack
	 * @param modelData the canonical model and loader selection
	 * @param controllers the animation controllers in pose composition order
	 * @param resolver the resolver
	 * @param inherited the render inputs inherited from the parent bone or renderer
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 */
	public <T extends PAnimatable<T>> void instantDraw(PoseStack poseStack, PModelData modelData,
	                                                   Collection<PAnimationController<T>> controllers,
	                                                   PMeshRenderResolver resolver, PMeshRenderContext inherited, float partialTick)
	{
		instantDraw(poseStack, modelData, controllers, Map.of(), resolver, inherited, partialTick);
	}

	/**
	 * Evaluates the supplied controllers and submits the model meshes for rendering.
	 *
	 * @param <T> the animatable owner type
	 * @param poseStack the current model-to-render pose stack
	 * @param modelData the canonical model and loader selection
	 * @param controllers the animation controllers in pose composition order
	 * @param molangContexts the frame-specific Molang context of each controller
	 * @param resolver the resolver
	 * @param inherited the render inputs inherited from the parent bone or renderer
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 */
	public <T extends PAnimatable<T>> void instantDraw(PoseStack poseStack, PModelData modelData,
	                                                   Collection<PAnimationController<T>> controllers,
	                                                   Map<PAnimationController<T>, MolangParser.Context> molangContexts,
	                                                   PMeshRenderResolver resolver, PMeshRenderContext inherited, float partialTick)
	{
		PAnimationPoseResolver<T> poseResolver = poseResolver(controllers, molangContexts, partialTick);
		this.bones.forEach(bone -> bone.instantDraw(poseStack, poseResolver, resolver, inherited));
	}

	/**
	 * Samples the supplied controller collection into an indexed bone pose.
	 *
	 * @param <T> the animatable owner type
	 * @param controllers the animation controllers in pose composition order
	 * @param contexts the contexts
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @return the indexed bone pose
	 */
	public <T extends PAnimatable<T>> PPose evaluate(Collection<PAnimationController<T>> controllers,
	                                                 Map<PAnimationController<T>, MolangParser.Context> contexts,
	                                                 float partialTick)
	{
		return PAnimationRuntime.evaluate(this, controllers,
				(controller, tick) -> contexts.getOrDefault(controller,
						PAnimationPoseResolver.<T>defaultContexts().context(controller, tick)), partialTick);
	}

	private <T extends PAnimatable<T>> PAnimationPoseResolver<T> poseResolver(
			Collection<PAnimationController<T>> controllers,
			Map<PAnimationController<T>, MolangParser.Context> contexts,
			float partialTick)
	{
		return new PAnimationPoseResolver<>(this, controllers,
				(controller, tick) -> contexts.getOrDefault(controller,
						PAnimationPoseResolver.<T>defaultContexts().context(controller, tick)), partialTick);
	}

	private static void index(PBakedBone bone, List<PBakedBone> target, Map<String, Integer> indices)
	{
		if (indices.putIfAbsent(bone.name(), target.size()) != null)
			throw new IllegalArgumentException("Model contains duplicate bone name: " + bone.name());
		target.add(bone);
		bone.children().forEach(child -> index(child, target, indices));
	}

	private Map<String, PCompiledAnimation> compileAnimations()
	{
		Map<String, PCompiledAnimation> compiled = new HashMap<>();
		for (Map.Entry<String, PAnimation> entry : this.animations.entrySet())
		{
			PAnimation animation = entry.getValue();
			PBoneAnimation[] byIndex = new PBoneAnimation[this.indexedBones.length];
			BitSet mask = new BitSet(this.indexedBones.length);
			animation.boneAnimations().forEach((name, boneAnimation) ->
			{
				int index = boneIndex(name);
				if (index >= 0)
				{
					byIndex[index] = boneAnimation;
					mask.set(index);
				}
			});
			PCompiledAnimation compiledAnimation = new PCompiledAnimation(animation, byIndex, mask);
			compiled.put(entry.getKey(), compiledAnimation);
			compiled.putIfAbsent(animation.name(), compiledAnimation);
		}
		return Map.copyOf(compiled);
	}
}
