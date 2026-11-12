/**
 * @author ArcAnc
 * Created at: 05.07.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.util.attachments;


import com.arcanc.pulselib.content.animatable.PAnimationController;
import com.arcanc.pulselib.content.model.animation.PAnimationPoseResolver;
import com.arcanc.pulselib.content.model.baked.*;
import com.arcanc.pulselib.content.renderer.PRenderContexts;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.base.PLivingAttachmentRenderState;
import com.arcanc.pulselib.content.renderer.base.PRenderCallbackScope;
import com.arcanc.pulselib.content.renderer.base.PRenderState;
import com.arcanc.pulselib.content.renderer.plan.PTransparencyMode;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.PResourceCache;
import com.arcanc.pulselib.util.helpers.PLibRenderHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Provides support for living attachment layer.
 */
public class PLivingAttachmentLayer<S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M>
{
	protected static final List<EquipmentSlot> EQUIPMENT_SLOTS = List.of(
			EquipmentSlot.HEAD,
			EquipmentSlot.CHEST,
			EquipmentSlot.LEGS,
			EquipmentSlot.FEET,
			EquipmentSlot.BODY,
			EquipmentSlot.MAINHAND,
			EquipmentSlot.OFFHAND);

	/**
	 * Creates an instance of the enclosing type.
	 * @param parent the parent to use.
	 */
	public PLivingAttachmentLayer(RenderLayerParent<S, M> parent)
	{
		super(parent);
	}

	/**
	 * Performs the submit operation.
	 * @param poseStack the pose stack to use.
	 * @param submitNodeCollector the submit node collector to use.
	 * @param light the light to use.
	 * @param state the state to use.
	 * @param yRot the y rot to use.
	 * @param xRot the x rot to use.
	 */
	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, S state, float yRot, float xRot)
	{
		List<RenderEntry> entries = ((PLivingAttachmentRenderState)state).pulselib$getAttachments();
		if (entries == null)
			return;

		for (RenderEntry entry : entries)
			renderDefinition(poseStack, light, PRenderState.partialTick(state), entry,
					binding -> PAttachmentAnchorResolvers.resolve(state, this.getParentModel(), binding.anchor()), this.getParentModel().root(), true);
	}

	/**
	 * Extracts the render entries.
	 * @param entity the entity to use.
	 * @param partialTick the partial tick to use.
	 * @return the value produced by this operation.
	 */
	public static List<RenderEntry> extractRenderEntries(LivingEntity entity, float partialTick)
	{
		List<RenderEntry> entries = new ArrayList<>();
		for (EquipmentSlot slot : EQUIPMENT_SLOTS)
		{
			ItemStack stack = entity.getItemBySlot(slot);
			for (PLivingAttachmentDefinition definition : PLivingAttachments.get(stack, slot, entity))
				addRenderEntry(entries, entity, stack, definition, partialTick);
		}

		for (PLivingAttachmentDefinition definition : PLivingAttachments.getGlobal(entity))
			addRenderEntry(entries, entity, ItemStack.EMPTY, definition, partialTick);

		return entries;
	}

	/**
	 * Adds the render entry.
	 * @param entries the entries to use.
	 * @param entity the entity to use.
	 * @param stack the stack to use.
	 * @param definition the definition to use.
	 * @param partialTick the partial tick to use.
	 */
	private static void addRenderEntry(List<RenderEntry> entries,
	                                   LivingEntity entity,
	                                   ItemStack stack,
	                                   PLivingAttachmentDefinition definition,
	                                   float partialTick)
	{
		PBakedModel model = definition.modelData().getModel();
		if (model == null)
			return;

		Collection<PAnimationController<?>> controllers = definition.animationControllers(entity, stack, model, partialTick);
		entries.add(new RenderEntry(definition, entity, stack.copy(), controllers));
	}

	/**
	 * Renders the first person anchor.
	 * @param poseStack the pose stack to use.
	 * @param light the light to use.
	 * @param entity the entity to use.
	 * @param targetAnchor the target anchor to use.
	 * @param anchorPart the anchor part to use.
	 * @param partialTick the partial tick to use.
	 */
	protected static void renderFirstPersonAnchor(PoseStack poseStack,
	                                              int light,
	                                              LivingEntity entity,
	                                              PAttachmentAnchor targetAnchor,
	                                              ModelPart anchorPart,
	                                              float partialTick)
	{
		ModelPart modelRoot = entity instanceof AbstractClientPlayer player ?
				PLibRenderHelper.mc().getEntityRenderDispatcher().getPlayerRenderer(player).getModel().root() : anchorPart;
		try (var invocation = PRenderCallbackScope.capture(poseStack, modelRoot))
		{
			anchorPart.xRot = 0;
			anchorPart.yRot = 0;
			anchorPart.zRot = 0;

			for (RenderEntry entry : extractRenderEntries(entity, partialTick))
				renderDefinition(poseStack, light, partialTick, entry,
						binding -> binding.anchor().equals(targetAnchor) ? anchorPart : null, modelRoot,
							PRenderContexts.current() == PRenderContexts.Context.FIRST_PERSON);
		}
	}

	/**
	 * Renders the definition.
	 * @param poseStack the pose stack to use.
	 * @param light the light to use.
	 * @param partialTick the partial tick to use.
	 * @param entry the entry to use.
	 * @param partResolver the part resolver to use.
	 */
	private static void renderDefinition(
			PoseStack poseStack,
			int light,
			float partialTick,
			RenderEntry entry,
			AttachmentPartResolver partResolver, ModelPart modelRoot, boolean queued)
	{
		PLivingAttachmentDefinition definition = entry.definition();
		PBakedModel model = definition.modelData().getModel();
		if (model == null)
			return;

		for (PAttachmentBinding binding : definition.bindings())
			try (var callback = PRenderCallbackScope.capture(poseStack, modelRoot))
			{
				ModelPart vanillaPart = partResolver.resolve(binding);
				if (vanillaPart == null)
					continue;

				PBakedBone bone = findBone(model.bones(), binding.bone());
				if (bone == null)
					continue;

				poseStack.pushPose();
				vanillaPart.translateAndRotate(poseStack);
				poseStack.mulPose(Axis.ZP.rotationDegrees(180));
				PTransform transform = binding.transform();
				Vector3f offset = transform.offset();
				Quaternionf rotation = transform.rotation();
				Vector3f scale = transform.scale();
				poseStack.translate(offset.x(), offset.y(), offset.z());
				poseStack.mulPose(rotation);
				poseStack.scale(scale.x(), scale.y(), scale.z());
				poseStack.translate(-bone.basePosition().x(), -bone.basePosition().y(), -bone.basePosition().z());

				drawBone(poseStack, definition, bone, entry.entity(), entry.stack(), entry.controllers(), light, partialTick, modelRoot, queued);

				poseStack.popPose();
			}
	}

	/**
	 * Draws the bone.
	 * @param poseStack the pose stack to use.
	 * @param definition the definition to use.
	 * @param bone the bone to use.
	 * @param entity the entity to use.
	 * @param stack the stack to use.
	 * @param controllers the controllers to use.
	 * @param light the light to use.
	 * @param partialTick the partial tick to use.
	 */
	private static void drawBone(PoseStack poseStack,
	                             PLivingAttachmentDefinition definition,
	                             PBakedBone bone,
	                             LivingEntity entity,
	                             ItemStack stack,
	                             Collection<PAnimationController<?>> controllers,
	                             int light,
	                             float partialTick, ModelPart modelRoot, boolean queued)
	{
		drawBoneUnchecked(poseStack, definition, bone, entity, stack, controllers, light, partialTick, modelRoot, queued);
	}

	/**
	 * Draws the bone unchecked.
	 * @param poseStack the pose stack to use.
	 * @param definition the definition to use.
	 * @param bone the bone to use.
	 * @param entity the entity to use.
	 * @param stack the stack to use.
	 * @param controllers the controllers to use.
	 * @param light the light to use.
	 * @param partialTick the partial tick to use.
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void drawBoneUnchecked(PoseStack poseStack,
	                                      PLivingAttachmentDefinition definition,
	                                      PBakedBone bone,
	                                      LivingEntity entity,
	                                      ItemStack stack,
	                                      Collection<PAnimationController<?>> controllers,
	                                      int light,
	                                      float partialTick, ModelPart modelRoot, boolean queued)
	{
		PMeshRenderContext context = new PMeshRenderContext(
				PRenderTypes.RenderTypeProvider :: trianglesCutout,
				-1,
				light,
				OverlayTexture.NO_OVERLAY);

		PMeshRenderResolver resolver = (bakedBone, mesh, inherited) ->
		{
			try (var callback = PRenderCallbackScope.capture(poseStack, modelRoot))
			{
				return definition.renderResolver().resolve(entity, stack, bakedBone, mesh, inherited, partialTick);
			}
		};
		if (queued)
			submitBone(poseStack, bone, modelPose(definition, controllers, partialTick), resolver, context);
		else
			bone.instantDraw(poseStack, definition.modelData(), (Collection)controllers, resolver, context, partialTick);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static PAnimationPoseResolver<?> modelPose(PLivingAttachmentDefinition definition,
			Collection<PAnimationController<?>> controllers, float partialTick)
	{
		return new PAnimationPoseResolver(definition.modelData().getModel(), (Collection)controllers,
				PAnimationPoseResolver.defaultContexts(), partialTick);
	}

	/** Native layers submit before deferred player geometry; world meshes must join the existing entity pass. */
	private static void submitBone(PoseStack poses, PBakedBone bone, PAnimationPoseResolver<?> animation,
			PMeshRenderResolver resolver, PMeshRenderContext inherited)
	{
		if (!animation.isVisible(bone))
			return;
		var frame = animation.resolve(bone).localTransform();
		poses.pushPose();
		try
		{
			poses.translate(frame.translation().x(), frame.translation().y(), frame.translation().z());
			poses.mulPose(frame.rotation());
			poses.scale(frame.scale().x(), frame.scale().y(), frame.scale().z());
			for (PBakedMesh mesh : bone.meshes())
			{
				if (mesh.textureReference().isEmpty())
					continue;
				PMeshRenderContext context = resolver.resolve(bone, mesh, inherited);
				PMeshRenderMaterial material = PMeshRenderMaterial.resolve(mesh, context);
				// Legacy direct drawing uses ordinary alpha, even when the world has pending OIT.
				PRenderQueue.submitEntityMesh(material.resolveRenderType(context, PResourceCache.ATLAS_LOCATION),
						material.mesh(), context.deformation(), new PRenderQueue.InstanceData(new Matrix4f(poses.last().pose()),
								context.color(), material.packedLight(), context.packedOverlay()), PTransparencyMode.ORDINARY);
			}
			for (PBakedBone child : bone.children())
				submitBone(poses, child, animation, resolver, inherited);
		}
		finally
		{
			poses.popPose();
		}
	}

	/**
	 * Finds the bone.
	 * @param bones the bones to use.
	 * @param name the name to use.
	 * @return the value produced by this operation.
	 */
	private static @Nullable PBakedBone findBone(List<PBakedBone> bones, String name)
	{
		for (PBakedBone bone : bones)
		{
			if (bone.name().equals(name))
				return bone;

			PBakedBone child = findBone(bone.children(), name);
			if (child != null)
				return child;
		}

		return null;
	}

	/**
	 * Defines the contract for attachment part resolver.
    */
	@FunctionalInterface
	private interface AttachmentPartResolver
	{
		/**
		 * Performs the resolve operation.
		 * @param binding the binding to use.
		 * @return the value produced by this operation.
		 */
		@Nullable ModelPart resolve(PAttachmentBinding binding);
	}

	/**
	 * Immutable value object representing render entry.
    */
	public record RenderEntry(
			PLivingAttachmentDefinition definition,
			LivingEntity entity,
			ItemStack stack,
			Collection<PAnimationController<?>> controllers)
	{
	}
}
