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
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

/**
 * Provides support for player animations.
 */
public final class PPlayerAnimations
{
	private static final Map<Identifier, PPlayerAnimationDefinition> DEFINITIONS = new HashMap<>();
	private static final Map<UUID, Map<Identifier, PPlayerAnimationInstance>> INSTANCES = new HashMap<>();
	private static final Set<Identifier> INVALID_FIRST_PERSON_DEFINITIONS = new HashSet<>();

	/**
	 * Creates an instance of the enclosing type.
	 */
	private PPlayerAnimations()
	{
	}

	/**
	 * Performs the register operation.
	 * @param id the id to use.
	 * @param definition the definition to use.
	 */
	public static void register(Identifier id, PPlayerAnimationDefinition definition)
	{
		if (DEFINITIONS.putIfAbsent(id, definition) != null)
			throw new IllegalArgumentException("Duplicate player animation definition: " + id);
		if (definition.firstPersonSettings().enabled() &&
				!definition.anchors().containsKey(PPlayerAnimationAnchors.FIRST_PERSON_CAMERA))
		{
			INVALID_FIRST_PERSON_DEFINITIONS.add(id);
			PLibDatabase.LOGGER.error("Player animation definition {} enables first-person rendering but has no {} anchor; vanilla first-person rendering will be used", id, PPlayerAnimationAnchors.FIRST_PERSON_CAMERA.id());
		}
	}

	/**
	 * Performs the get operation.
	 * @param id the id to use.
	 * @return the value produced by this operation.
	 */
	public static @Nullable PPlayerAnimationDefinition get(Identifier id)
	{
		return DEFINITIONS.get(id);
	}

	/**
	 * Returns the instance.
	 * @param player the player to use.
	 * @param id the id to use.
	 * @return the value produced by this operation.
	 */
	public static @Nullable PPlayerAnimationInstance getInstance(Player player, Identifier id)
	{
		PPlayerAnimationDefinition definition = DEFINITIONS.get(id);
		return definition == null ? null : instance(player, id, definition);
	}

	/**
	 * Returns the handle.
	 * @param player the player to use.
	 * @param id the id to use.
	 * @return the value produced by this operation.
	 */
	public static @Nullable PPlayerAnimationHandle getHandle(Player player, Identifier id)
	{
		return DEFINITIONS.containsKey(id) ? new PPlayerAnimationHandle(player, id) : null;
	}

	/**
	 * Performs the tick operation.
	 * @param level the level to use.
	 */
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

	/**
	 * Performs the clean up operation.
	 */
	@ApiStatus.Internal
	public static void cleanUp()
	{
		INSTANCES.clear();
	}

	/**
	 * Performs the apply operation.
	 * @param player the player to use.
	 * @param model the model to use.
	 * @param partialTick the partial tick to use.
	 * @param allowedParts the allowed parts to use.
	 * @return the value produced by this operation.
	 */
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

	/**
	 * Performs the first person presentation operation.
	 * @param player the player to use.
	 * @param partialTick the partial tick to use.
	 * @return the value produced by this operation.
	 */
	@ApiStatus.Internal
	public static @Nullable PFirstPersonRenderPresentation firstPersonPresentation(Player player, float partialTick)
	{
		FirstPersonPresentationBuilder pose = new FirstPersonPresentationBuilder();
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

			PFirstPersonPresentation presentation = PFirstPersonPresentation.create(frame);
			if (presentation == null)
				continue;

			pose.addArm(PPlayerPart.RIGHT_ARM, presentation, definition, player, partialTick, activationWeight);
			pose.addArm(PPlayerPart.LEFT_ARM, presentation, definition, player, partialTick, activationWeight);
			pose.addItem(PPlayerAnimationAnchors.RIGHT_ITEM, presentation, definition, instance, player, partialTick, activationWeight);
			pose.addItem(PPlayerAnimationAnchors.LEFT_ITEM, presentation, definition, instance, player, partialTick, activationWeight);
			pose.resolveItemVisibility(true, definition, instance, player, partialTick);
			pose.resolveItemVisibility(false, definition, instance, player, partialTick);
			pose.addAnimationAnchors(entry.getKey(), frame, presentation, definition, activationWeight);
			pose.addMeshAttachments(entry.getKey(), frame, presentation, definition, activationWeight);
			pose.hasContributingAnimation = true;
		}

		return pose.hasContributingAnimation ? pose.build() : null;
	}

	/**
	 * Performs the animation anchor poses operation.
	 * @param player the player to use.
	 * @param partialTick the partial tick to use.
	 * @return the value produced by this operation.
	 */
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

	/**
	 * Performs the automatic mesh attachment poses operation.
	 * @param player the player to use.
	 * @param partialTick the partial tick to use.
	 * @return the value produced by this operation.
	 */
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
							PPlayerAnimationSpace.toPlayerGeometrySpace(transform, definition),
							weight));
				}
			}
		});
		return List.copyOf(result);
	}

	/**
	 * Performs the ordered anchors operation.
	 * @param definition the definition to use.
	 * @return the value produced by this operation.
	 */
	private static List<PPlayerAnimationAnchor> orderedAnchors(PPlayerAnimationDefinition definition)
	{
		return definition.anchors().keySet().stream().
				sorted(Comparator.comparing(anchor -> anchor.id().toString())).
				toList();
	}

	/**
	 * Applies the root.
	 * @param player the player to use.
	 * @param poseStack the pose stack to use.
	 * @param partialTick the partial tick to use.
	 */
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

	/** Compatibility query retained until the first-person mixin replacement is ported. */
	@ApiStatus.Internal
	public static boolean isPartAnimating(Player player, PPlayerPart playerPart, float partialTick)
	{
		boolean[] animating = {false};
		applyDefinitions(player, partialTick, Set.of(playerPart), (part, pose, definition, weight) -> animating[0] = true);
		return animating[0];
	}


	/**
	 * Performs the camera pose operation.
	 * @param player the player to use.
	 * @param partialTick the partial tick to use.
	 * @return the value produced by this operation.
	 */
	@ApiStatus.Internal
	public static @Nullable PPlayerCameraPose cameraPose(Player player, float partialTick)
	{
		PPlayerCameraPose cameraPose = new PPlayerCameraPose();
		List<Map.Entry<Identifier, PPlayerAnimationDefinition>> definitions = new ArrayList<>(DEFINITIONS.entrySet());
		definitions.sort(Comparator.
				comparingInt((Map.Entry<Identifier, PPlayerAnimationDefinition> entry) -> entry.getValue().priority()).
				thenComparing(Map.Entry :: getKey));
		for (Map.Entry<Identifier, PPlayerAnimationDefinition> entry : definitions)
		{
			PPlayerAnimationDefinition definition = entry.getValue();
			if (!definition.firstPersonSettings().enabled() ||
					definition.firstPersonSettings().cameraMode() != PFirstPersonCameraMode.ANIMATED ||
					INVALID_FIRST_PERSON_DEFINITIONS.contains(entry.getKey()))
				continue;
			float weight = definition.weight(player, partialTick);
			if (weight <= 0.0f)
				continue;
			PPlayerAnimationInstance instance = instance(player, entry.getKey(), definition);
			if (!instance.isFirstPersonContributing())
				continue;
			PPlayerAnimationFrame frame = instance.sampleFrame(partialTick);
			PTransform delta = frame == null ? null : frame.cameraAnchorModelDelta();
			if (delta != null)
				cameraPose.addCamera(delta,
						definition.blendMode(), weight * instance.firstPersonActivationWeight(partialTick));
		}
		return cameraPose.isEmpty() ? null : cameraPose;
	}

	/**
	 * Performs the active deformers operation.
	 * @param player the player to use.
	 * @param part the part to use.
	 * @param partialTick the partial tick to use.
	 * @return the value produced by this operation.
	 */
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

	/**
	 * Applies the definitions.
	 * @param player the player to use.
	 * @param partialTick the partial tick to use.
	 * @param allowedParts the allowed parts to use.
	 * @param consumer the consumer to use.
	 */
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

				PPlayerBonePose pose = frame.canonicalModelDelta(binding.getValue());
				if (pose == null)
					continue;

				consumer.apply(part, pose, definition, weight);
			}
		}
	}

	/**
	 * Performs the for each active frame operation.
	 * @param player the player to use.
	 * @param partialTick the partial tick to use.
	 * @param consumer the consumer to use.
	 */
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

	/**
	 * Performs the instance operation.
	 * @param player the player to use.
	 * @param id the id to use.
	 * @param definition the definition to use.
	 * @return the value produced by this operation.
	 */
	private static PPlayerAnimationInstance instance(Player player, Identifier id, PPlayerAnimationDefinition definition)
	{
		Map<Identifier, PPlayerAnimationInstance> playerInstances = INSTANCES.computeIfAbsent(player.getUUID(), $ -> new HashMap<>());
		PPlayerAnimationInstance instance = playerInstances.computeIfAbsent(id, $ -> new PPlayerAnimationInstance(player, id, definition));
		instance.updatePlayer(player);
		return instance;
	}

	/**
	 * Performs the synchronize groups operation.
	 * @param player the player to use.
	 */
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

	/**
	 * Performs the apply operation.
	 * @param part the part to use.
	 * @param original the original to use.
	 * @param pose the pose to use.
	 * @param blendMode the blend mode to use.
	 * @param weight the weight to use.
	 */
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
/**
 * Defines the contract for pose consumer.
 */
	private interface PoseConsumer
	{
		/**
		 * Performs the apply operation.
		 * @param part the part to use.
		 * @param pose the pose to use.
		 * @param definition the definition to use.
		 * @param weight the weight to use.
		 */
		void apply(PPlayerPart part,
		           PPlayerBonePose pose,
		           PPlayerAnimationDefinition definition,
		           float weight);
	}

	@FunctionalInterface
/**
 * Defines the contract for active frame consumer.
 */
	private interface ActiveFrameConsumer
	{
		/**
		 * Performs the accept operation.
		 * @param id the id to use.
		 * @param frame the frame to use.
		 * @param definition the definition to use.
		 * @param weight the weight to use.
		 */
		void accept(Identifier id, PPlayerAnimationFrame frame, PPlayerAnimationDefinition definition, float weight);
	}

/**
 * Builds first person presentation.
 */
	private static final class FirstPersonPresentationBuilder
	{
		private PFirstPersonArmPose rightArm = PFirstPersonArmPose.vanilla();
		private PFirstPersonArmPose leftArm = PFirstPersonArmPose.vanilla();
		private boolean rightArmContributed;
		private boolean leftArmContributed;
		private PTransform rightItem;
		private PTransform leftItem;
		private boolean rightItemContributed;
		private boolean leftItemContributed;
		private boolean rightItemHidden;
		private boolean leftItemHidden;
		private final List<PPlayerFirstPersonAnchorPose> animationAnchors = new ArrayList<>();
		private final List<PPlayerFirstPersonMeshAttachmentPose> meshAttachments = new ArrayList<>();
		/** Reused while converting canonical matrices into immutable render commands. */
		private final Matrix4f presentationMatrix = new Matrix4f();
		private boolean hasContributingAnimation;

		/**
		 * Adds the arm.
		 * @param part the part to use.
		 * @param presentation the presentation to use.
		 * @param definition the definition to use.
		 * @param player the player to use.
		 * @param partialTick the partial tick to use.
		 * @param activationWeight the activation weight to use.
		 */
		private void addArm(PPlayerPart part,
		                    PFirstPersonPresentation presentation,
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
			PTransform armOrigin = presentation.armPreModelPartMatrix(part, this.presentationMatrix) ?
					PTransform.fromMatrix(this.presentationMatrix) : null;
			if (armOrigin != null)
			{
				boolean right = part == PPlayerPart.RIGHT_ARM;
				HumanoidArm arm = right ? HumanoidArm.RIGHT : HumanoidArm.LEFT;
				/*
				 * PFirstPersonArmPose is a hand target for the vanilla resolver. The
				 * geometry itself is driven by the arm origin; the hand is its local
				 * child socket and can therefore never detach from this rigid arm.
				 */
				PTransform handTarget = armOrigin.compose(PFirstPersonRestPose.armRig(arm).armToHand());
				setArm(right, handTarget, definition.blendMode(), weight);
			}
		}

		/**
		 * Adds the item.
		 * @param anchor the anchor to use.
		 * @param presentation the presentation to use.
		 * @param definition the definition to use.
		 * @param instance the instance to use.
		 * @param player the player to use.
		 * @param partialTick the partial tick to use.
		 * @param activationWeight the activation weight to use.
		 */
		private void addItem(PPlayerAnimationAnchor anchor,
		                     PFirstPersonPresentation presentation,
		                     PPlayerAnimationDefinition definition,
		                     PPlayerAnimationInstance instance,
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
			if (presentation.itemMatrix(anchor, this.presentationMatrix))
				setItem(rightHand, PTransform.fromMatrix(this.presentationMatrix), definition.blendMode(), weight);
		}

		/**
		 * Resolves the item visibility.
		 * @param right the right to use.
		 * @param definition the definition to use.
		 * @param instance the instance to use.
		 * @param player the player to use.
		 * @param partialTick the partial tick to use.
		 */
		private void resolveItemVisibility(boolean right,
		                                   PPlayerAnimationDefinition definition,
		                                   PPlayerAnimationInstance instance,
		                                   Player player,
		                                   float partialTick)
		{
			if (!(player instanceof LocalPlayer localPlayer))
				return;
			InteractionHand hand = right == (localPlayer.getMainArm() == HumanoidArm.RIGHT) ?
					InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
			PPlayerAnimationDefinition.ItemVisibilityPolicy visibilityPolicy = definition.itemVisibilityPolicy();
			String controllerName = definition.itemVisibilityController();
			boolean hidden = definition.itemRenderPolicy().hide(localPlayer, hand, localPlayer.getItemInHand(hand));
			if (visibilityPolicy != null && controllerName != null)
				hidden |= visibilityPolicy.visibility(localPlayer, hand,
						instance.controllerAnimationTime(controllerName, partialTick), localPlayer.getItemInHand(hand)) ==
						PPlayerAnimationDefinition.ItemVisibility.HIDDEN;
			if (right)
				this.rightItemHidden = hidden;
			else
				this.leftItemHidden = hidden;
		}

		/**
		 * Adds the animation anchors.
		 * @param id the id to use.
		 * @param frame the frame to use.
		 * @param presentation the presentation to use.
		 * @param definition the definition to use.
		 * @param weight the weight to use.
		 */
		private void addAnimationAnchors(Identifier id,
		                                 PPlayerAnimationFrame frame,
		                                 PFirstPersonPresentation presentation,
		                                 PPlayerAnimationDefinition definition,
		                                 float weight)
		{
			for (PPlayerAnimationAnchor anchor : orderedAnchors(definition))
			{
				if (anchor.equals(PPlayerAnimationAnchors.FIRST_PERSON_CAMERA) ||
						anchor.equals(PPlayerAnimationAnchors.RIGHT_ITEM) ||
						anchor.equals(PPlayerAnimationAnchors.LEFT_ITEM))
					continue;
				if (presentation.anchorMatrix(anchor, this.presentationMatrix))
					this.animationAnchors.add(
							new PPlayerFirstPersonAnchorPose(
									id,
									anchor,
							PTransform.fromMatrix(this.presentationMatrix),
									weight));
			}
		}

		/**
		 * Adds the mesh attachments.
		 * @param id the id to use.
		 * @param frame the frame to use.
		 * @param presentation the presentation to use.
		 * @param definition the definition to use.
		 * @param weight the weight to use.
		 */
		private void addMeshAttachments(Identifier id,
		                                PPlayerAnimationFrame frame,
		                                PFirstPersonPresentation presentation,
		                                PPlayerAnimationDefinition definition,
		                                float weight)
		{
			for (var root : PPlayerAutomaticMeshAttachments.roots(frame))
			{
				if (presentation.boneMatrix(root.name(), this.presentationMatrix))
					this.meshAttachments.add(
							new PPlayerFirstPersonMeshAttachmentPose(
									id,
									definition.modelData(),
									root,
									frame,
							PPlayerAnimationSpace.toFirstPersonGeometrySpace(PTransform.fromMatrix(this.presentationMatrix), definition),
									weight));
			}
		}

		/**
		 * Sets the arm.
		 * @param right the right to use.
		 * @param transform the transform to use.
		 * @param blendMode the blend mode to use.
		 * @param weight the weight to use.
		 */
		private void setArm(boolean right, PTransform transform, PPlayerAnimationBlendMode blendMode, float weight)
		{
			PFirstPersonArmPose current = right ? this.rightArm : this.leftArm;
			boolean contributed = right ? this.rightArmContributed : this.leftArmContributed;
			PTransform rest = right ? PFirstPersonRestPose.VANILLA.rightHand() : PFirstPersonRestPose.VANILLA.leftHand();
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

		/**
		 * Sets the item.
		 * @param right the right to use.
		 * @param transform the transform to use.
		 * @param blendMode the blend mode to use.
		 * @param weight the weight to use.
		 */
		private void setItem(boolean right,
		                     PTransform transform,
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
				this.rightItemContributed = true;
			}
			else
			{
				this.leftItem = blended;
				this.leftItemContributed = true;
			}
		}

		/**
		 * Performs the blend operation.
		 * @param current the current to use.
		 * @param target the target to use.
		 * @param blendMode the blend mode to use.
		 * @param weight the weight to use.
		 * @return the value produced by this operation.
		 */
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

		/**
		 * Performs the interpolate operation.
		 * @param from the from to use.
		 * @param to the to to use.
		 * @param weight the weight to use.
		 * @return the value produced by this operation.
		 */
		private static PTransform interpolate(PTransform from, PTransform to, float weight)
		{
			return from.interpolate(to, weight);
		}

		/**
		 * Performs the divide operation.
		 * @param dividend the dividend to use.
		 * @param divisor the divisor to use.
		 * @return the value produced by this operation.
		 */
		private static Vector3f divide(Vector3f dividend, Vector3f divisor)
		{
			return new Vector3f(
					divide(dividend.x, divisor.x),
					divide(dividend.y, divisor.y),
					divide(dividend.z, divisor.z));
		}

		/**
		 * Performs the divide operation.
		 * @param dividend the dividend to use.
		 * @param divisor the divisor to use.
		 * @return the value produced by this operation.
		 */
		private static float divide(float dividend, float divisor)
		{
			return Math.abs(divisor) < 1.0e-6f ? 0.0f : dividend / divisor;
		}

		/**
		 * Performs the build operation.
		 * @return the value produced by this operation.
		 */
		private PFirstPersonRenderPresentation build()
		{
			return new PFirstPersonRenderPresentation(this.rightArm, this.leftArm,
					this.rightItemHidden ? PFirstPersonItemPose.hidden() :
							(this.rightItemContributed ? PFirstPersonItemPose.animated(this.rightItem) : PFirstPersonItemPose.vanilla()),
					this.leftItemHidden ? PFirstPersonItemPose.hidden() :
							(this.leftItemContributed ? PFirstPersonItemPose.animated(this.leftItem) : PFirstPersonItemPose.vanilla()),
					List.copyOf(this.animationAnchors), List.copyOf(this.meshAttachments));
		}
	}

	@ApiStatus.Internal
/**
 * Provides support for player camera pose.
 */
	public static final class PPlayerCameraPose
	{
		private static final float VANILLA_MODEL_ORIGIN_HEIGHT = 1.501f;

		private final List<RootTransform> rootTransforms = new ArrayList<>();
		private final Vector3f headTranslation = new Vector3f();
		private final Quaternionf headRotation = new Quaternionf();
		private PTransform cameraTransform = PTransform.IDENTITY;
		private boolean hasCameraTransform;
		private boolean hasCameraRotation;
		private boolean changed;

		/**
		 * Adds the root.
		 * @param pose the pose to use.
		 * @param pivot the pivot to use.
		 * @param weight the weight to use.
		 */
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

		/**
		 * Adds the head.
		 * @param pose the pose to use.
		 * @param blendMode the blend mode to use.
		 * @param weight the weight to use.
		 */
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

		/**
		 * Adds the camera.
		 * @param delta the delta to use.
		 * @param blendMode the blend mode to use.
		 * @param weight the weight to use.
		 */
		private void addCamera(PTransform delta, PPlayerAnimationBlendMode blendMode, float weight)
		{
			if (weight <= 0.0f || isIdentity(delta))
				return;
			if (!this.hasCameraTransform || blendMode.poseBlendMode() == PPoseBlendMode.OVERRIDE)
				this.cameraTransform = this.cameraTransform.interpolate(delta, weight);
			else
			this.cameraTransform = this.cameraTransform.compose(PTransform.IDENTITY.interpolate(delta, weight));
			this.hasCameraTransform = true;
			this.hasCameraRotation |= hasRotation(delta);
			this.changed = true;
		}

		/**
		 * Determines whether the object has rotation.
		 * @return the value produced by this operation.
		 */
		public boolean hasRotation()
		{
			return this.hasCameraTransform ? this.hasCameraRotation : !this.headRotation.equals(new Quaternionf());
		}

		/**
		 * Determines whether identity.
		 * @param transform the transform to use.
		 * @return the value produced by this operation.
		 */
		private static boolean isIdentity(PTransform transform)
		{
			return transform.translation().lengthSquared() < 1.0e-10f &&
					Math.abs(Math.abs(transform.rotation().w) - 1.0f) < 1.0e-5f &&
					Math.abs(transform.rotation().x) < 1.0e-5f &&
					Math.abs(transform.rotation().y) < 1.0e-5f &&
					Math.abs(transform.rotation().z) < 1.0e-5f &&
					new Vector3f(transform.scale()).sub(1.0f, 1.0f, 1.0f).lengthSquared() < 1.0e-10f;
		}

		/**
		 * Determines whether the object has rotation.
		 * @param transform the transform to use.
		 * @return the value produced by this operation.
		 */
		private static boolean hasRotation(PTransform transform)
		{
			Quaternionf rotation = transform.rotation();
			return Math.abs(Math.abs(rotation.w) - 1.0f) >= 1.0e-5f ||
					Math.abs(rotation.x) >= 1.0e-5f ||
					Math.abs(rotation.y) >= 1.0e-5f ||
					Math.abs(rotation.z) >= 1.0e-5f;
		}

		/**
		 * Determines whether empty.
		 * @return the value produced by this operation.
		 */
		private boolean isEmpty()
		{
			return !this.changed;
		}

		/**
		 * Performs the rotation operation.
		 * @return the value produced by this operation.
		 */
		public Quaternionf rotation()
		{
			if (this.hasCameraTransform)
				return this.cameraTransform.rotation();
			Quaternionf rotation = new Quaternionf();
			for (RootTransform transform : this.rootTransforms)
				rotation.mul(transform.rotation());
			return rotation.mul(this.headRotation);
		}

		/**
		 * Performs the position offset operation.
		 * @param eyeHeight the eye height to use.
		 * @return the value produced by this operation.
		 */
		public Vector3f positionOffset(float eyeHeight)
		{
			if (this.hasCameraTransform)
				return this.cameraTransform.translation();
			Vector3f initialEyePosition = new Vector3f(0.0f, VANILLA_MODEL_ORIGIN_HEIGHT - eyeHeight, 0.0f);
			Vector3f eyePosition = new Vector3f(initialEyePosition).add(this.headTranslation);
			for (int index = this.rootTransforms.size() - 1; index >= 0; index--)
			{
				RootTransform transform = this.rootTransforms.get(index);
				eyePosition.sub(transform.pivot()).mul(transform.scale()).rotate(transform.rotation()).add(transform.pivot()).add(transform.translation());
			}
			return eyePosition.sub(initialEyePosition);
		}

/**
 * Immutable value object representing root transform.
 */
		private record RootTransform(Vector3f pivot, Vector3f translation, Quaternionf rotation, Vector3f scale)
		{
		}
	}

	@ApiStatus.Internal
/**
 * Provides support for player model pose.
 */
	public static final class PPlayerModelPose
	{
		private final Map<ModelPart, PartPose> parts;

		/**
		 * Creates an instance of the enclosing type.
		 * @param parts the parts to use.
		 */
		private PPlayerModelPose(Map<ModelPart, PartPose> parts)
		{
			this.parts = parts;
		}

		/**
		 * Performs the capture operation.
		 * @param model the model to use.
		 * @param allowedParts the allowed parts to use.
		 * @return the value produced by this operation.
		 */
		private static PPlayerModelPose capture(PlayerModel model, Set<PPlayerPart> allowedParts)
		{
			Map<ModelPart, PartPose> parts = new IdentityHashMap<>();
			for (PPlayerPart part : allowedParts)
				for (ModelPart modelPart : part.resolve(model))
					parts.put(modelPart, new PartPose(modelPart));
			return new PPlayerModelPose(parts);
		}

		/**
		 * Performs the part operation.
		 * @param modelPart the model part to use.
		 * @return the value produced by this operation.
		 */
		private PartPose part(ModelPart modelPart)
		{
			return this.parts.get(modelPart);
		}

		/**
		 * Performs the restore operation.
		 */
		public void restore()
		{
			this.parts.forEach((part, pose) -> pose.restore(part));
		}

/**
 * Provides support for part pose.
 */
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

			/**
			 * Creates an instance of the enclosing type.
			 * @param part the part to use.
			 */
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

			/**
			 * Performs the restore operation.
			 * @param part the part to use.
			 */
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

	/**
	 * Performs the all parts operation.
	 * @return the value produced by this operation.
	 */
	@ApiStatus.Internal
	public static Set<PPlayerPart> allParts()
	{
		return EnumSet.allOf(PPlayerPart.class);
	}

}
