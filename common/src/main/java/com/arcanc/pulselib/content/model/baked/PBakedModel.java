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
import com.arcanc.pulselib.content.model.animation.PAnimation;
import com.arcanc.pulselib.content.model.animation.PAnimationPoseResolver;
import com.arcanc.pulselib.content.model.animation.PAnimationRuntime;
import com.arcanc.pulselib.content.model.animation.PBoneAnimation;
import com.arcanc.pulselib.content.model.animation.PCompiledAnimation;
import com.arcanc.pulselib.content.model.animation.PPose;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.data.gecko.MolangParser;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.Function;

/**
 * Provides support for baked model.
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
	 * Creates an instance of the enclosing type.
	 * @param bones the bones to use.
	 * @param animations the animations to use.
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
	 * Performs the bones operation.
	 * @return the value produced by this operation.
	 */
	public List<PBakedBone> bones() { return this.bones; }
	/**
	 * Performs the animations operation.
	 * @return the value produced by this operation.
	 */
	public Map<String, PAnimation> animations() { return this.animations; }
	/**
	 * Performs the bone count operation.
	 * @return the value produced by this operation.
	 */
	public int boneCount() { return this.indexedBones.length; }
	/**
	 * Performs the bone index operation.
	 * @param name the name to use.
	 * @return the value produced by this operation.
	 */
	public int boneIndex(String name) { return this.boneIndices.getOrDefault(name, -1); }
	/**
	 * Performs the bone index operation.
	 * @param bone the bone to use.
	 * @return the value produced by this operation.
	 */
	public int boneIndex(PBakedBone bone) { return boneIndex(bone.name()); }
	/**
	 * Performs the bone operation.
	 * @param index the index to use.
	 * @return the value produced by this operation.
	 */
	public PBakedBone bone(int index) { return this.indexedBones[index]; }
	/**
	 * Performs the parent index operation.
	 * @param index the index to use.
	 * @return the value produced by this operation.
	 */
	public int parentIndex(int index) { return this.parents[index]; }
	/**
	 * Performs the parent indices operation.
	 * @return the value produced by this operation.
	 */
	public int[] parentIndices() { return this.parents.clone(); }
	/**
	 * Performs the compiled animation operation.
	 * @param name the name to use.
	 * @return the value produced by this operation.
	 */
	public PCompiledAnimation compiledAnimation(String name) { return this.compiledAnimations.get(name); }

	/**
	 * Binds the pose.
	 * @return the value produced by this operation.
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
	 * Performs the instant draw operation.
	 * @param poseStack the pose stack to use.
	 * @param modelData the model data to use.
	 * @param controllers the controllers to use.
	 * @param renderType the render type to use.
	 * @param color the color to use.
	 * @param packedOverlay the packed overlay to use.
	 * @param partialTick the partial tick to use.
	 */
	public <T extends PAnimatable<T>>void instantDraw(PoseStack poseStack,
	                                                  PModelData modelData,
	                                                  Collection<PAnimationController<T>> controllers,
	                                                  Function<Identifier, RenderType> renderType,
	                                                  int color,
	                                                  int packedOverlay,
	                                                  float partialTick)
	{
		this.bones.forEach(bone -> bone.instantDraw(
				poseStack,
				modelData,
				controllers,
				renderType,
				color,
				packedOverlay,
				partialTick));
	}

	/**
	 * Performs the instant draw operation.
	 * @param poseStack the pose stack to use.
	 * @param modelData the model data to use.
	 * @param controllers the controllers to use.
	 * @param renderType the render type to use.
	 * @param color the color to use.
	 * @param packedLight the packed light to use.
	 * @param packedOverlay the packed overlay to use.
	 * @param partialTick the partial tick to use.
	 */
	public <T extends PAnimatable<T>>void instantDraw(PoseStack poseStack,
	                                                  PModelData modelData,
	                                                  Collection<PAnimationController<T>> controllers,
	                                                  Function<Identifier, RenderType> renderType,
	                                                  int color,
	                                                  int packedLight,
	                                                  int packedOverlay,
	                                                  float partialTick)
	{
		this.bones.forEach(bone -> bone.instantDraw(
				poseStack,
				modelData,
				controllers,
				renderType,
				color,
				packedLight,
				packedOverlay,
				partialTick));
	}

	/**
	 * Performs the evaluate operation.
	 * @param controllers the controllers to use.
	 * @param contexts the contexts to use.
	 * @param partialTick the partial tick to use.
	 * @return the value produced by this operation.
	 */
	public <T extends PAnimatable<T>> PPose evaluate(Collection<PAnimationController<T>> controllers,
	                                                 Map<PAnimationController<T>, MolangParser.Context> contexts,
	                                                 float partialTick)
	{
		return PAnimationRuntime.evaluate(this, controllers,
				(controller, tick) -> contexts.getOrDefault(controller,
						PAnimationPoseResolver.<T>defaultContexts().context(controller, tick)), partialTick);
	}

	/**
	 * Performs the index operation.
	 * @param bone the bone to use.
	 * @param target the target to use.
	 * @param indices the indices to use.
	 */
	private static void index(PBakedBone bone, List<PBakedBone> target, Map<String, Integer> indices)
	{
		if (indices.putIfAbsent(bone.name(), target.size()) != null)
			throw new IllegalArgumentException("Model contains duplicate bone name: " + bone.name());
		target.add(bone);
		bone.children().forEach(child -> index(child, target, indices));
	}

	/**
	 * Compiles the animations.
	 * @return the value produced by this operation.
	 */
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
