/**
 * @author ArcAnc
 * Created at: 30.07.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation;

import com.arcanc.pulselib.content.model.animation.PPoseBlendMode;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAnimationMeshAttachmentPose;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAutomaticMeshAttachments;
import com.arcanc.pulselib.content.player.animation.firstPerson.PPlayerFirstPersonAnchorPose;
import com.arcanc.pulselib.content.player.animation.firstPerson.PPlayerFirstPersonMeshAttachmentPose;
import com.arcanc.pulselib.content.player.animation.firstPerson.PPlayerFirstPersonPose;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

public final class PPlayerAnimations
{
	private static final Map<Identifier, PPlayerAnimationDefinition> DEFINITIONS = new HashMap<>();
	private static final Map<UUID, Map<Identifier, PPlayerAnimationInstance>> INSTANCES = new HashMap<>();

	private PPlayerAnimations()
	{
	}

	public static void register(Identifier id, PPlayerAnimationDefinition definition)
	{
		if (DEFINITIONS.putIfAbsent(id, definition) != null)
			throw new IllegalArgumentException("Duplicate player animation definition: " + id);
	}

	public static @Nullable PPlayerAnimationDefinition get(Identifier id)
	{
		return DEFINITIONS.get(id);
	}
	
	public static @Nullable PPlayerAnimationInstance getInstance(Player player, Identifier id)
	{
		PPlayerAnimationDefinition definition = DEFINITIONS.get(id);
		return definition == null ? null : instance(player, id, definition);
	}
	
	public static @Nullable PPlayerAnimationHandle getHandle(Player player, Identifier id)
	{
		return DEFINITIONS.containsKey(id) ? new PPlayerAnimationHandle(player, id) : null;
	}

	@ApiStatus.Internal
	public static void tick(ClientLevel level)
	{
		Set<UUID> livePlayers = new HashSet<>();
		for (Player player : level.players())
		{
			livePlayers.add(player.getUUID());
			for (Map.Entry<Identifier, PPlayerAnimationDefinition> entry : DEFINITIONS.entrySet())
			{
				PPlayerAnimationInstance instance = instance(player, entry.getKey(), entry.getValue());
				instance.tick(entry.getValue().shouldApply(player));
			}
			synchronizeGroups(player);
		}
		INSTANCES.keySet().removeIf(uuid -> !livePlayers.contains(uuid));
	}

	@ApiStatus.Internal
	public static void cleanUp()
	{
		INSTANCES.clear();
	}
	
	@ApiStatus.Internal
	public static PPlayerModelPose apply(Player player, PlayerModel model, float partialTick, Set<PPlayerPart> allowedParts)
	{
		PPlayerModelPose originalPose = PPlayerModelPose.capture(model, allowedParts);
		applyDefinitions(player, partialTick, allowedParts, (part, pose, definition, weight) ->
		{
			pose = PPlayerAnimationSpace.toPlayerSpace(pose, definition);
			for (ModelPart modelPart : part.resolve(model))
				apply(modelPart, originalPose.part(modelPart), pose, definition.blendMode(), weight);
		});
		return originalPose;
	}
	
	@ApiStatus.Internal
	public static @Nullable PPlayerFirstPersonPose firstPersonPose(Player player, float partialTick)
	{
		FirstPersonPoseBuilder pose = new FirstPersonPoseBuilder();
		List<Map.Entry<Identifier, PPlayerAnimationDefinition>> definitions = new ArrayList<>(DEFINITIONS.entrySet());
		definitions.sort(Comparator.
				comparingInt((Map.Entry<Identifier, PPlayerAnimationDefinition> entry) -> entry.getValue().priority()).
				thenComparing(Map.Entry :: getKey));

		for (Map.Entry<Identifier, PPlayerAnimationDefinition> entry : definitions)
		{
			PPlayerAnimationDefinition definition = entry.getValue();
			if (!definition.firstPersonSettings().enable())
				continue;

			float definitionWeight = definition.weight(player, partialTick);
			if (definitionWeight <= 0.0f)
				continue;

			PPlayerAnimationInstance instance = instance(player, entry.getKey(), definition);
			if (!instance.isContributing() || !instance.hasActiveController())
				continue;

			PPlayerAnimationFrame frame = instance.sampleFrame(partialTick);
			if (frame == null)
				continue;

			float activationWeight = definitionWeight * instance.activationWeight(partialTick);
			if (activationWeight <= 0.0f)
				continue;

			pose.addArm(PPlayerPart.RIGHT_ARM, frame, definition, player, partialTick, activationWeight);
			pose.addArm(PPlayerPart.LEFT_ARM, frame, definition, player, partialTick, activationWeight);
			pose.addItem(PPlayerAnimationAnchors.RIGHT_ITEM, frame, definition, activationWeight);
			pose.addItem(PPlayerAnimationAnchors.LEFT_ITEM, frame, definition, activationWeight);
			pose.addAnimationAnchors(entry.getKey(), frame, definition, activationWeight);
			pose.addMeshAttachments(entry.getKey(), frame, definition, activationWeight);
			pose.enabled = true;
		}

		return pose.enabled ? pose.build() : null;
	}
	
	@ApiStatus.Internal
	public static List<PPlayerAnimationAnchorPose> animationAnchorPoses(Player player, float partialTick)
	{
		List<PPlayerAnimationAnchorPose> result = new ArrayList<>();
		forEachActiveFrame(player, partialTick, (id, frame, definition, weight) ->
		{
			for (PPlayerAnimationAnchor anchor : orderedAnchors(definition))
			{
				Matrix4f transform = frame.actionTransform(anchor);
				if (transform != null)
					result.add(new PPlayerAnimationAnchorPose(
							id,
							anchor,
							PPlayerAnimationSpace.toPlayerSpace(
									transform,
									definition),
							weight));
			}
		});
		return List.copyOf(result);
	}

	@ApiStatus.Internal
	public static List<PPlayerAnimationMeshAttachmentPose> automaticMeshAttachmentPoses(Player player, float partialTick)
	{
		List<PPlayerAnimationMeshAttachmentPose> result = new ArrayList<>();
		forEachActiveFrame(player, partialTick, (id, frame, definition, weight) ->
		{
			for (var root : PPlayerAutomaticMeshAttachments.roots(frame))
			{
				Matrix4f transform = frame.rootRelativeTransform(root.name());
				if (transform != null)
				{
					result.add(
							new PPlayerAnimationMeshAttachmentPose(
									id,
									definition.modelData(),
									root,
									frame,
									PPlayerAnimationSpace.toPlayerGeometrySpace(
											transform,
											definition),
									weight));
				}
			}
		});
		return List.copyOf(result);
	}

	private static List<PPlayerAnimationAnchor> orderedAnchors(PPlayerAnimationDefinition definition)
	{
		return definition.anchors().keySet().stream().
				sorted(Comparator.comparing(anchor -> anchor.id().toString())).
				toList();
	}
	
	@ApiStatus.Internal
	public static void applyRoot(Player player, PoseStack poseStack, float partialTick)
	{
		applyDefinitions(player, partialTick, Set.of(PPlayerPart.ROOT), (part, pose, definition, weight) ->
		{
			PPlayerBonePose modelPose = PPlayerAnimationSpace.toPlayerSpace(pose, definition);
			Vector3f translation = new Vector3f(modelPose.translation()).mul(weight);
			Vector3f pivot = PPlayerAnimationSpace.toPlayerSpace(definition.rootPivot(), definition);
			Quaternionf rotation = new Quaternionf().slerp(modelPose.rotation(), weight);
			Vector3f scale = new Vector3f(1.0f).lerp(pose.scale(), weight);

			poseStack.translate(translation.x, translation.y, translation.z);
			poseStack.translate(pivot.x, pivot.y, pivot.z);
			poseStack.mulPose(rotation);
			poseStack.scale(scale.x, scale.y, scale.z);
			poseStack.translate(-pivot.x, -pivot.y, -pivot.z);
		});
	}
	
	
	@ApiStatus.Internal
	public static @Nullable PPlayerCameraPose cameraPose(Player player, float partialTick)
	{
		PPlayerCameraPose cameraPose = new PPlayerCameraPose();
		applyDefinitions(player, partialTick, Set.of(PPlayerPart.ROOT), (part, pose, definition, weight) ->
				cameraPose.addRoot(
						PPlayerAnimationSpace.toPlayerSpace(pose, definition),
						PPlayerAnimationSpace.toPlayerSpace(definition.rootPivot(), definition),
						weight));
		applyDefinitions(player, partialTick, Set.of(PPlayerPart.HEAD), (part, pose, definition, weight) ->
				cameraPose.addHead(PPlayerAnimationSpace.toPlayerSpace(pose, definition), definition.blendMode(), weight));
		return cameraPose.isEmpty() ? null : cameraPose;
	}

	@ApiStatus.Internal
	public static List<PPlayerAnimationDeformerApplication> activeDeformers(Player player,
	                                                                        PPlayerPart part,
	                                                                        float partialTick)
	{
		List<PPlayerAnimationDeformerApplication> applications = new ArrayList<>();
		List<Map.Entry<Identifier, PPlayerAnimationDefinition>> definitions = new ArrayList<>(DEFINITIONS.entrySet());
		definitions.sort(Comparator.
				comparingInt((Map.Entry<Identifier, PPlayerAnimationDefinition> entry) -> entry.getValue().priority()).
				thenComparing(Map.Entry :: getKey));

		for (Map.Entry<Identifier, PPlayerAnimationDefinition> entry : definitions)
		{
			PPlayerAnimationDefinition definition = entry.getValue();
			if (!definition.appliesTo(player, part, partialTick))
				continue;
			float definitionWeight = definition.weight(player, partialTick);
			if (definitionWeight <= 0.0f)
				continue;

			PPlayerAnimationInstance instance = instance(player, entry.getKey(), definition);
			if (!instance.isContributing())
				continue;
			float weight = definitionWeight * instance.activationWeight(partialTick) * definition.partWeight(player, part, partialTick);
			if (weight <= 0.0f)
				continue;

			PPlayerAnimationDeformerContext context = new PPlayerAnimationDeformerContext(player, instance, partialTick, weight);
			for (PPlayerAnimationDeformer deformer : definition.deformers())
			{
				if (deformer.part() != part)
					continue;
				applications.add(new PPlayerAnimationDeformerApplication(deformer.stack(), (ignored, reference) ->
				{
					float value = deformer.values().resolve(context, reference);
					return Float.isFinite(value) ? Mth.lerp(weight, reference.defaultValue(), value) : reference.defaultValue();
				}));
			}
		}
		return applications;
	}

	private static void applyDefinitions(Player player,
	                                     float partialTick,
	                                     Set<PPlayerPart> allowedParts,
	                                     PoseConsumer consumer)
	{
		List<Map.Entry<Identifier, PPlayerAnimationDefinition>> definitions = new ArrayList<>(DEFINITIONS.entrySet());
		definitions.sort(Comparator.
				comparingInt((Map.Entry<Identifier, PPlayerAnimationDefinition> entry) -> entry.getValue().priority()).
				thenComparing(Map.Entry :: getKey));

		for (Map.Entry<Identifier, PPlayerAnimationDefinition> entry : definitions)
		{
			PPlayerAnimationDefinition definition = entry.getValue();
			float definitionWeight = definition.weight(player, partialTick);
			if (definitionWeight <= 0.0f)
				continue;

			PPlayerAnimationInstance instance = instance(player, entry.getKey(), definition);
			if (!instance.isContributing())
				continue;
			
			PPlayerAnimationFrame frame = instance.sampleFrame(partialTick);
			if (frame == null)
				continue;
			
			for (Map.Entry<PPlayerPart, String> binding : definition.bindings().entrySet())
			{
				PPlayerPart part = binding.getKey();
				if (!allowedParts.contains(part) || !definition.appliesTo(player, part, partialTick))
					continue;

				float weight = definitionWeight * instance.activationWeight(partialTick) *
						definition.partWeight(player, part, partialTick) * definition.boneWeight(player, binding.getValue(), partialTick);
				if (weight <= 0.0f)
					continue;
					
				PPlayerBonePose pose = frame.animationDelta(binding.getValue());
				if (pose == null)
					continue;

				consumer.apply(part, pose, definition, weight);
			}
		}
	}

	private static void forEachActiveFrame(Player player,
	                                       float partialTick,
	                                       ActiveFrameConsumer consumer)
	{
		List<Map.Entry<Identifier, PPlayerAnimationDefinition>> definitions = new ArrayList<>(DEFINITIONS.entrySet());
		definitions.sort(Comparator.
				comparingInt((Map.Entry<Identifier, PPlayerAnimationDefinition> entry) -> entry.getValue().priority()).
				thenComparing(Map.Entry :: getKey));
		for (Map.Entry<Identifier, PPlayerAnimationDefinition> entry : definitions)
		{
			PPlayerAnimationDefinition definition = entry.getValue();
			float definitionWeight = definition.weight(player, partialTick);
			if (definitionWeight <= 0.0f)
				continue;
			PPlayerAnimationInstance instance = instance(player, entry.getKey(), definition);
			if (!instance.isContributing())
				continue;
			PPlayerAnimationFrame frame = instance.sampleFrame(partialTick);
			if (frame == null)
				continue;
			float weight = definitionWeight * instance.activationWeight(partialTick);
			if (weight > 0.0f)
				consumer.accept(entry.getKey(), frame, definition, weight);
		}
	}

	private static PPlayerAnimationInstance instance(Player player, Identifier id, PPlayerAnimationDefinition definition)
	{
		Map<Identifier, PPlayerAnimationInstance> playerInstances = INSTANCES.computeIfAbsent(player.getUUID(), $ -> new HashMap<>());
		PPlayerAnimationInstance instance = playerInstances.computeIfAbsent(id, $ -> new PPlayerAnimationInstance(player, id, definition));
		instance.updatePlayer(player);
		return instance;
	}

	private static void synchronizeGroups(Player player)
	{
		Map<String, List<PPlayerAnimationInstance>> groups = new HashMap<>();
		for (Map.Entry<Identifier, PPlayerAnimationDefinition> entry : DEFINITIONS.entrySet())
		{
			String syncGroup = entry.getValue().syncGroup();
			if (syncGroup.isBlank())
				continue;
			PPlayerAnimationInstance instance = instance(player, entry.getKey(), entry.getValue());
			if (instance.isContributing())
				groups.computeIfAbsent(syncGroup, ignored -> new ArrayList<>()).add(instance);
		}
		for (List<PPlayerAnimationInstance> group : groups.values())
			if (group.size() > 1)
				PPlayerAnimationInstance.synchronize(group);
	}

	private static void apply(ModelPart part,
	                          PPlayerModelPose.PartPose original,
	                          PPlayerBonePose pose,
	                          PPlayerAnimationBlendMode blendMode,
	                          float weight)
	{
		PPoseBlendMode mode = blendMode.poseBlendMode();
		if (pose.hasTranslation())
		{
			float x = pose.translation().x() * 16.0f;
			float y = pose.translation().y() * 16.0f;
			float z = pose.translation().z() * 16.0f;
			switch (mode)
			{
				case ADDITIVE_LOCAL, ADDITIVE_MESH_SPACE ->
				{
					part.x += x * weight;
					part.y += y * weight;
					part.z += z * weight;
				}
				case DIFFERENCE ->
				{
					part.x -= x * weight;
					part.y -= y * weight;
					part.z -= z * weight;
				}
				case OVERRIDE ->
				{
					part.x = Mth.lerp(weight, part.x, original.x + x);
					part.y = Mth.lerp(weight, part.y, original.y + y);
					part.z = Mth.lerp(weight, part.z, original.z + z);
				}
				case MULTIPLY_SCALE -> { }
			}
		}

		if (pose.hasRotation())
		{
			Quaternionf current = new Quaternionf().rotationXYZ(part.xRot, part.yRot, part.zRot);
			Quaternionf target = current;
			switch (mode)
			{
				case ADDITIVE_LOCAL, ADDITIVE_MESH_SPACE -> target = current.premul(new Quaternionf().slerp(pose.rotation(), weight));
				case DIFFERENCE -> target = current.premul(new Quaternionf().slerp(pose.rotation(), weight).invert());
				case OVERRIDE -> target = current.slerp(pose.rotation(), weight);
				case MULTIPLY_SCALE -> target = current;
			}

			Vector3f euler = target.getEulerAnglesXYZ(new Vector3f());
			part.xRot = euler.x;
			part.yRot = euler.y;
			part.zRot = euler.z;
		}

		if (pose.hasScale())
		{
			switch (mode)
			{
				case ADDITIVE_LOCAL, ADDITIVE_MESH_SPACE, MULTIPLY_SCALE ->
				{
					part.xScale *= Mth.lerp(weight, 1.0f, pose.scale().x());
					part.yScale *= Mth.lerp(weight, 1.0f, pose.scale().y());
					part.zScale *= Mth.lerp(weight, 1.0f, pose.scale().z());
				}
				case DIFFERENCE ->
				{
					part.xScale /= Mth.lerp(weight, 1.0f, pose.scale().x());
					part.yScale /= Mth.lerp(weight, 1.0f, pose.scale().y());
					part.zScale /= Mth.lerp(weight, 1.0f, pose.scale().z());
				}
				case OVERRIDE ->
				{
					part.xScale = Mth.lerp(weight, part.xScale, original.xScale * pose.scale().x());
					part.yScale = Mth.lerp(weight, part.yScale, original.yScale * pose.scale().y());
					part.zScale = Mth.lerp(weight, part.zScale, original.zScale * pose.scale().z());
				}
			}
		}
	}

	@FunctionalInterface
	private interface PoseConsumer
	{
		void apply(PPlayerPart part,
		           PPlayerBonePose pose,
		           PPlayerAnimationDefinition definition,
		           float weight);
	}

	@FunctionalInterface
	private interface ActiveFrameConsumer
	{
		void accept(Identifier id, PPlayerAnimationFrame frame, PPlayerAnimationDefinition definition, float weight);
	}

	private static final class FirstPersonPoseBuilder
	{
		private Matrix4f rightArm;
		private Matrix4f leftArm;
		private Matrix4f rightItem;
		private Matrix4f leftItem;
		private final List<PPlayerFirstPersonAnchorPose> animationAnchors = new ArrayList<>();
		private final List<PPlayerFirstPersonMeshAttachmentPose> meshAttachments = new ArrayList<>();
		private boolean enabled;

		private void addArm(PPlayerPart part,
		                    PPlayerAnimationFrame frame,
		                    PPlayerAnimationDefinition definition,
		                    Player player,
		                    float partialTick,
		                    float activationWeight)
		{
			if (!definition.appliesTo(player, part, partialTick))
				return;
			float weight = activationWeight * definition.partWeight(player, part, partialTick);
			if (weight <= 0.0f)
				return;
			Matrix4f transform = frame.firstPersonTransform(part);
			if (transform != null)
				set(part == PPlayerPart.RIGHT_ARM, PPlayerAnimationSpace.toFirstPersonSpace(transform, definition), definition.blendMode(), weight, false);
		}

		private void addItem(PPlayerAnimationAnchor anchor,
		                     PPlayerAnimationFrame frame,
		                     PPlayerAnimationDefinition definition,
		                     float weight)
		{
			Matrix4f transform = frame.firstPersonTransform(anchor);
			if (transform != null)
				set(anchor.equals(PPlayerAnimationAnchors.RIGHT_ITEM), PPlayerAnimationSpace.toFirstPersonSpace(transform, definition), definition.blendMode(), weight, true);
		}

		private void addAnimationAnchors(Identifier id,
		                                 PPlayerAnimationFrame frame,
		                                 PPlayerAnimationDefinition definition,
		                                 float weight)
		{
			for (PPlayerAnimationAnchor anchor : orderedAnchors(definition))
			{
				if (anchor.equals(PPlayerAnimationAnchors.FIRST_PERSON_CAMERA) ||
						anchor.equals(PPlayerAnimationAnchors.RIGHT_ITEM) ||
						anchor.equals(PPlayerAnimationAnchors.LEFT_ITEM))
					continue;
				Matrix4f transform = frame.firstPersonTransform(anchor);
				if (transform != null)
					this.animationAnchors.add(
							new PPlayerFirstPersonAnchorPose(
									id,
									anchor,
									PPlayerAnimationSpace.toFirstPersonSpace(
											transform,
											definition),
									weight));
			}
		}

		private void addMeshAttachments(Identifier id,
		                                PPlayerAnimationFrame frame,
		                                PPlayerAnimationDefinition definition,
		                                float weight)
		{
			for (var root : PPlayerAutomaticMeshAttachments.roots(frame))
			{
				Matrix4f transform = frame.firstPersonTransform(root.name());
				if (transform != null)
					this.meshAttachments.add(
							new PPlayerFirstPersonMeshAttachmentPose(
									id,
									definition.modelData(),
									root,
									frame,
									PPlayerAnimationSpace.toFirstPersonGeometrySpace(
											transform,
											definition),
									weight));
			}
		}

		private void set(boolean right, Matrix4f transform, PPlayerAnimationBlendMode blendMode, float weight, boolean item)
		{
			Matrix4f current = item ? (right ? this.rightItem : this.leftItem) : (right ? this.rightArm : this.leftArm);
			Matrix4f blended = blend(current, transform, blendMode, weight);
			if (item)
			{
				if (right) this.rightItem = blended; else this.leftItem = blended;
			}
			else if (right) this.rightArm = blended; else this.leftArm = blended;
		}

		private static Matrix4f blend(@Nullable Matrix4f current, Matrix4f target, PPlayerAnimationBlendMode blendMode, float weight)
		{
			if (current == null)
				return interpolate(new Matrix4f(), target, weight);
			if (blendMode.poseBlendMode() == PPoseBlendMode.ADDITIVE_LOCAL || blendMode.poseBlendMode() == PPoseBlendMode.ADDITIVE_MESH_SPACE)
				return new Matrix4f(current).mul(interpolate(new Matrix4f(), target, weight));
			if (blendMode.poseBlendMode() == PPoseBlendMode.DIFFERENCE)
				return new Matrix4f(current).mul(interpolate(new Matrix4f(), target, weight).invert());
			return interpolate(current, target, weight);
		}

		private static Matrix4f interpolate(Matrix4f from, Matrix4f to, float weight)
		{
			Vector3f translation = from.getTranslation(new Vector3f()).lerp(to.getTranslation(new Vector3f()), weight);
			Quaternionf rotation = from.getUnnormalizedRotation(new Quaternionf()).slerp(to.getUnnormalizedRotation(new Quaternionf()), weight);
			Vector3f scale = from.getScale(new Vector3f()).lerp(to.getScale(new Vector3f()), weight);
			return new Matrix4f().translationRotateScale(translation, rotation, scale);
		}

		private PPlayerFirstPersonPose build()
		{
			return new PPlayerFirstPersonPose(this.rightArm, this.leftArm, this.rightItem, this.leftItem,
					List.copyOf(this.animationAnchors), List.copyOf(this.meshAttachments));
		}
	}

	@ApiStatus.Internal
	public static final class PPlayerCameraPose
	{
		private static final float VANILLA_MODEL_ORIGIN_HEIGHT = 1.501f;

		private final List<RootTransform> rootTransforms = new ArrayList<>();
		private final Vector3f headTranslation = new Vector3f();
		private final Quaternionf headRotation = new Quaternionf();
		private boolean changed;

		private void addRoot(PPlayerBonePose pose,
		                     Vector3f pivot,
		                     float weight)
		{
			if (!pose.hasTranslation() && !pose.hasRotation() && !pose.hasScale())
				return;

			this.rootTransforms.add(new RootTransform(
					new Vector3f(pivot),
					new Vector3f(pose.translation()).mul(weight),
					new Quaternionf().slerp(pose.rotation(), weight),
					new Vector3f(1.0f).lerp(pose.scale(), weight)));
			this.changed = true;
		}

		private void addHead(PPlayerBonePose pose,
		                     PPlayerAnimationBlendMode blendMode,
		                     float weight)
		{
			boolean isAdditive = blendMode.poseBlendMode() == PPoseBlendMode.ADDITIVE_LOCAL ||
					blendMode.poseBlendMode() == PPoseBlendMode.ADDITIVE_MESH_SPACE;
			if (pose.hasTranslation())
			{
				Vector3f translation = new Vector3f(pose.translation());
				if (isAdditive)
					this.headTranslation.add(translation.mul(weight));
				else
					this.headTranslation.lerp(translation, weight);
			}

			if (pose.hasRotation())
			{
				if (isAdditive)
					this.headRotation.premul(new Quaternionf().slerp(pose.rotation(), weight));
				else
					this.headRotation.slerp(pose.rotation(), weight);
			}

			this.changed |= pose.hasTranslation() || pose.hasRotation() || pose.hasScale();
		}

		private boolean isEmpty()
		{
			return !this.changed;
		}
		
		public Quaternionf rotation()
		{
			Quaternionf rotation = new Quaternionf();
			for (RootTransform transform : this.rootTransforms)
				rotation.mul(transform.rotation());
			return rotation.mul(this.headRotation);
		}
		
		public Vector3f positionOffset(float eyeHeight)
		{
			Vector3f initialEyePosition = new Vector3f(0.0f, VANILLA_MODEL_ORIGIN_HEIGHT - eyeHeight, 0.0f);
			Vector3f eyePosition = new Vector3f(initialEyePosition).add(this.headTranslation);
			for (int index = this.rootTransforms.size() - 1; index >= 0; index--)
			{
				RootTransform transform = this.rootTransforms.get(index);
				eyePosition.sub(transform.pivot()).mul(transform.scale()).rotate(transform.rotation()).add(transform.pivot()).add(transform.translation());
			}
			return eyePosition.sub(initialEyePosition);
		}

		private record RootTransform(Vector3f pivot, Vector3f translation, Quaternionf rotation, Vector3f scale)
		{
		}
	}

	@ApiStatus.Internal
	public static final class PPlayerModelPose
	{
		private final Map<ModelPart, PartPose> parts;

		private PPlayerModelPose(Map<ModelPart, PartPose> parts)
		{
			this.parts = parts;
		}

		private static PPlayerModelPose capture(PlayerModel model, Set<PPlayerPart> allowedParts)
		{
			Map<ModelPart, PartPose> parts = new IdentityHashMap<>();
			for (PPlayerPart part : allowedParts)
				for (ModelPart modelPart : part.resolve(model))
					parts.put(modelPart, new PartPose(modelPart));
			return new PPlayerModelPose(parts);
		}

		private PartPose part(ModelPart modelPart)
		{
			return this.parts.get(modelPart);
		}

		public void restore()
		{
			this.parts.forEach((part, pose) -> pose.restore(part));
		}

		private static final class PartPose
		{
			private final float x;
			private final float y;
			private final float z;
			private final float xRot;
			private final float yRot;
			private final float zRot;
			private final float xScale;
			private final float yScale;
			private final float zScale;

			private PartPose(ModelPart part)
			{
				this.x = part.x;
				this.y = part.y;
				this.z = part.z;
				this.xRot = part.xRot;
				this.yRot = part.yRot;
				this.zRot = part.zRot;
				this.xScale = part.xScale;
				this.yScale = part.yScale;
				this.zScale = part.zScale;
			}

			private void restore(ModelPart part)
			{
				part.x = this.x;
				part.y = this.y;
				part.z = this.z;
				part.xRot = this.xRot;
				part.yRot = this.yRot;
				part.zRot = this.zRot;
				part.xScale = this.xScale;
				part.yScale = this.yScale;
				part.zScale = this.zScale;
			}
		}
	}

	@ApiStatus.Internal
	public static Set<PPlayerPart> allParts()
	{
		return EnumSet.allOf(PPlayerPart.class);
	}

}
