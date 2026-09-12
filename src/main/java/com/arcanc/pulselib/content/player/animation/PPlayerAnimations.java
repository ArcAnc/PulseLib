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
import com.arcanc.pulselib.content.model.animation.PTransform;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAnimationMeshAttachmentPose;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAutomaticMeshAttachments;
import com.arcanc.pulselib.content.player.animation.firstPerson.*;
import com.arcanc.pulselib.util.PLibDatabase;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

public final class PPlayerAnimations
{
	private static final Map<Identifier, PPlayerAnimationDefinition> DEFINITIONS = new HashMap<>();
	private static final Map<UUID, Map<Identifier, PPlayerAnimationInstance>> INSTANCES = new HashMap<>();
	private static final Set<Identifier> INVALID_FIRST_PERSON_DEFINITIONS = new HashSet<>();

	private PPlayerAnimations()
	{
	}

	public static void register(Identifier id, PPlayerAnimationDefinition definition)
	{
		if (DEFINITIONS.putIfAbsent(id, definition) != null)
			throw new IllegalArgumentException("Duplicate player animation definition: " + id);
		if (definition.firstPersonSettings().enable() &&
				!definition.anchors().containsKey(PPlayerAnimationAnchors.FIRST_PERSON_CAMERA))
		{
			INVALID_FIRST_PERSON_DEFINITIONS.add(id);
			PLibDatabase.LOGGER.error("Player animation definition {} enables first-person rendering but has no {} anchor; vanilla first-person rendering will be used", id, PPlayerAnimationAnchors.FIRST_PERSON_CAMERA.id());
		}
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
			if (!definition.firstPersonSettings().enabled() || INVALID_FIRST_PERSON_DEFINITIONS.contains(entry.getKey()))
				continue;

			float definitionWeight = definition.weight(player, partialTick);
			if (definitionWeight <= 0.0f)
				continue;

			PPlayerAnimationInstance instance = instance(player, entry.getKey(), definition);
			if (!instance.isFirstPersonContributing())
				continue;

			PPlayerAnimationFrame frame = instance.sampleFrame(partialTick);
			if (frame == null)
				continue;

			float activationWeight = definitionWeight * instance.firstPersonActivationWeight(partialTick);
			if (activationWeight <= 0.0f)
				continue;

			pose.addArm(PPlayerPart.RIGHT_ARM, frame, definition, player, partialTick, activationWeight);
			pose.addArm(PPlayerPart.LEFT_ARM, frame, definition, player, partialTick, activationWeight);
			pose.addItem(PPlayerAnimationAnchors.RIGHT_ITEM, frame, definition, player, partialTick, activationWeight);
			pose.addItem(PPlayerAnimationAnchors.LEFT_ITEM, frame, definition, player, partialTick, activationWeight);
			pose.addAnimationAnchors(entry.getKey(), frame, definition, activationWeight);
			pose.addMeshAttachments(entry.getKey(), frame, definition, activationWeight);
			pose.hasContributingAnimation = true;
		}

		return pose.hasContributingAnimation ? pose.build() : null;
	}
	
	@ApiStatus.Internal
	public static List<PPlayerAnimationAnchorPose> animationAnchorPoses(Player player, float partialTick)
	{
		List<PPlayerAnimationAnchorPose> result = new ArrayList<>();
		forEachActiveFrame(player, partialTick, (id, frame, definition, weight) ->
		{
			for (PPlayerAnimationAnchor anchor : orderedAnchors(definition))
			{
				PTransform transform = frame.actionTransform(anchor);
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
				PTransform transform = frame.rootRelativeTransform(root.name());
				if (transform != null)
				{
					result.add(
							new PPlayerAnimationMeshAttachmentPose(
									id,
									definition.modelData(),
									root,
									frame,
							PTransform.IDENTITY.interpolate(
									PPlayerAnimationSpace.toPlayerGeometrySpace(transform, definition),
									weight)));
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
		applyFirstPersonDefinitions(player, partialTick, Set.of(PPlayerPart.ROOT), (part, pose, definition, weight) ->
				cameraPose.addRoot(
						PPlayerAnimationSpace.toPlayerSpace(pose, definition),
						PPlayerAnimationSpace.toPlayerSpace(definition.rootPivot(), definition),
						weight));
		applyFirstPersonDefinitions(player, partialTick, Set.of(PPlayerPart.HEAD), (part, pose, definition, weight) ->
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

	private static void applyFirstPersonDefinitions(Player player,
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
			if (!definition.firstPersonSettings().enabled() ||
					definition.firstPersonSettings().cameraMode() != PFirstPersonCameraMode.ANIMATED)
				continue;
			float definitionWeight = definition.weight(player, partialTick);
			if (definitionWeight <= 0.0f)
				continue;

			PPlayerAnimationInstance instance = instance(player, entry.getKey(), definition);
			if (!instance.isFirstPersonContributing())
				continue;
			PPlayerAnimationFrame frame = instance.sampleFrame(partialTick);
			if (frame == null)
				continue;

			for (Map.Entry<PPlayerPart, String> binding : definition.bindings().entrySet())
			{
				PPlayerPart part = binding.getKey();
				if (!allowedParts.contains(part) || !definition.appliesTo(player, part, partialTick))
					continue;
				float weight = definitionWeight * instance.firstPersonActivationWeight(partialTick) *
						definition.partWeight(player, part, partialTick) * definition.boneWeight(player, binding.getValue(), partialTick);
				if (weight <= 0.0f)
					continue;
				PPlayerBonePose pose = frame.animationDelta(binding.getValue());
				if (pose != null)
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
		private PFirstPersonArmPose rightArm = PFirstPersonArmPose.vanilla();
		private PFirstPersonArmPose leftArm = PFirstPersonArmPose.vanilla();
		private boolean rightArmContributed;
		private boolean leftArmContributed;
		private PTransform rightItem;
		private PTransform leftItem;
		private boolean rightItemContributed;
		private boolean leftItemContributed;
		private PPlayerAnimationDefinition.ItemRenderPolicy rightItemRenderPolicy = PPlayerAnimationDefinition.ItemRenderPolicy.RENDER;
		private PPlayerAnimationDefinition.ItemRenderPolicy leftItemRenderPolicy = PPlayerAnimationDefinition.ItemRenderPolicy.RENDER;
		private final List<PPlayerFirstPersonAnchorPose> animationAnchors = new ArrayList<>();
		private final List<PPlayerFirstPersonMeshAttachmentPose> meshAttachments = new ArrayList<>();
		private boolean hasContributingAnimation;

		private void addArm(PPlayerPart part,
		                    PPlayerAnimationFrame frame,
		                    PPlayerAnimationDefinition definition,
		                    Player player,
		                    float partialTick,
		                    float activationWeight)
		{
			if (!definition.appliesTo(player, part, partialTick))
				return;
			String boneName = definition.bindings().get(part);
			if (boneName == null)
				return;
			float weight = activationWeight * definition.partWeight(player, part, partialTick) *
					definition.boneWeight(player, boneName, partialTick);
			if (weight <= 0.0f)
				return;
			PTransform transform = frame.firstPersonTransform(part);
			if (transform != null)
				setArm(part == PPlayerPart.RIGHT_ARM, PPlayerAnimationSpace.toFirstPersonSpace(transform, definition), definition.blendMode(), weight);
		}

		private void addItem(PPlayerAnimationAnchor anchor,
		                     PPlayerAnimationFrame frame,
		                     PPlayerAnimationDefinition definition,
		                     Player player,
		                     float partialTick,
		                     float activationWeight)
		{
			String boneName = definition.anchors().get(anchor);
			if (boneName == null)
				return;
			float weight = activationWeight * definition.boneWeight(player, boneName, partialTick);
			if (weight <= 0.0f)
				return;
			boolean rightHand = anchor.equals(PPlayerAnimationAnchors.RIGHT_ITEM);
			PTransform transform = frame.firstPersonTransform(anchor);
			if (transform != null)
				setItem(rightHand, PPlayerAnimationSpace.toFirstPersonItemAnchorSpace(transform, definition),
						definition.itemRenderPolicy(), definition.blendMode(), weight);
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
				PTransform transform = frame.firstPersonTransform(anchor);
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
				PTransform transform = frame.firstPersonTransform(root.name());
				if (transform != null)
					this.meshAttachments.add(
							new PPlayerFirstPersonMeshAttachmentPose(
									id,
									definition.modelData(),
									root,
									frame,
									PTransform.IDENTITY.interpolate(
											PPlayerAnimationSpace.toFirstPersonGeometrySpace(transform, definition),
											weight)));
			}
		}

		private void setArm(boolean right, PTransform transform, PPlayerAnimationBlendMode blendMode, float weight)
		{
			PFirstPersonArmPose current = right ? this.rightArm : this.leftArm;
			boolean contributed = right ? this.rightArmContributed : this.leftArmContributed;
			PTransform rest = PFirstPersonRestPose.arm(right ? HumanoidArm.RIGHT : HumanoidArm.LEFT);
			PTransform blended = contributed ?
					blend(current.transform(), transform, blendMode, weight) :
					interpolate(rest, transform, weight);
			if (right)
			{
				this.rightArm = PFirstPersonArmPose.animated(blended);
				this.rightArmContributed = true;
			}
			else
			{
				this.leftArm = PFirstPersonArmPose.animated(blended);
				this.leftArmContributed = true;
			}
		}

		private void setItem(boolean right,
		                     PTransform transform,
		                     PPlayerAnimationDefinition.ItemRenderPolicy renderPolicy,
		                     PPlayerAnimationBlendMode blendMode,
		                     float weight)
		{
			boolean contributed = right ? this.rightItemContributed : this.leftItemContributed;
			PTransform current = contributed ?
					(right ? this.rightItem : this.leftItem) :
					PFirstPersonRestPose.item(right ? HumanoidArm.RIGHT : HumanoidArm.LEFT);
			PTransform blended = blend(current, transform, blendMode, weight);
			if (right)
			{
				this.rightItem = blended;
				this.rightItemRenderPolicy = renderPolicy;
				this.rightItemContributed = true;
			}
			else
			{
				this.leftItem = blended;
				this.leftItemRenderPolicy = renderPolicy;
				this.leftItemContributed = true;
			}
		}

		private static PTransform blend(@Nullable PTransform current, PTransform target, PPlayerAnimationBlendMode blendMode, float weight)
		{
			if (current == null)
				return interpolate(PTransform.IDENTITY, target, weight);

			PTransform weighted = interpolate(PTransform.IDENTITY, target, weight);
			return switch (blendMode.poseBlendMode())
			{
				case ADDITIVE_LOCAL, ADDITIVE_MESH_SPACE -> current.compose(weighted);
				case DIFFERENCE -> new PTransform(
						new Vector3f(current.translation()).sub(weighted.translation()),
						new Quaternionf(current.rotation()).mul(new Quaternionf(weighted.rotation()).invert()),
						divide(current.scale(), weighted.scale()));
				case MULTIPLY_SCALE -> new PTransform(
						current.translation(), current.rotation(),
						new Vector3f(current.scale()).mul(weighted.scale()));
				case OVERRIDE -> interpolate(current, target, weight);
			};
		}

		private static PTransform interpolate(PTransform from, PTransform to, float weight)
		{
			return from.interpolate(to, weight);
		}

		private static Vector3f divide(Vector3f dividend, Vector3f divisor)
		{
			return new Vector3f(
					divide(dividend.x, divisor.x),
					divide(dividend.y, divisor.y),
					divide(dividend.z, divisor.z));
		}

		private static float divide(float dividend, float divisor)
		{
			return Math.abs(divisor) < 1.0e-6f ? 0.0f : dividend / divisor;
		}

		private PPlayerFirstPersonPose build()
		{
			return new PPlayerFirstPersonPose(this.rightArm, this.leftArm,
					this.rightItemContributed ? PFirstPersonItemPose.animated(this.rightItem, this.rightItemRenderPolicy) : PFirstPersonItemPose.vanilla(),
					this.leftItemContributed ? PFirstPersonItemPose.animated(this.leftItem, this.leftItemRenderPolicy) : PFirstPersonItemPose.vanilla(),
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
