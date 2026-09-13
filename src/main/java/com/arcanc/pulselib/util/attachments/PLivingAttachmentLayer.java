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
import com.arcanc.pulselib.content.model.baked.PBakedBone;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.model.baked.PMeshRenderContext;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PLivingAttachmentLayer<S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M>
{
	public static final ContextKey<List<RenderEntry>> RENDER_DATA = new ContextKey<>(PLibDatabase.rl("living_attachments"));
	
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
		List<RenderEntry> entries = state.getRenderData(RENDER_DATA);
		if (entries == null)
			return;
		
		for (RenderEntry entry : entries)
			renderDefinition(poseStack, light, state.partialTick, entry,
					binding -> PAttachmentAnchorResolvers.resolve(state, this.getParentModel(), binding.anchor()));
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
		float oldXRot = anchorPart.xRot;
		float oldYRot = anchorPart.yRot;
		float oldZRot = anchorPart.zRot;
		try
		{
			anchorPart.xRot = 0;
			anchorPart.yRot = 0;
			anchorPart.zRot = 0;
			
			for (RenderEntry entry : extractRenderEntries(entity, partialTick))
				renderDefinition(poseStack, light, partialTick, entry,
						binding -> binding.anchor().equals(targetAnchor) ? anchorPart : null);
		}
		finally
		{
			anchorPart.xRot = oldXRot;
			anchorPart.yRot = oldYRot;
			anchorPart.zRot = oldZRot;
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
			AttachmentPartResolver partResolver)
	{
		PLivingAttachmentDefinition definition = entry.definition();
		PBakedModel model = definition.modelData().getModel();
		if (model == null)
			return;
		
		for (PAttachmentBinding binding : definition.bindings())
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
			
			drawBone(poseStack, definition, bone, entry.entity(), entry.stack(), entry.controllers(), light, partialTick);
			
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
	                             float partialTick)
	{
		drawBoneUnchecked(poseStack, definition, bone, entity, stack, controllers, light, partialTick);
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
	                                      float partialTick)
	{
		PMeshRenderContext context = new PMeshRenderContext(
				PRenderTypes.RenderTypeProvider :: trianglesCutout,
				-1,
				light,
				OverlayTexture.NO_OVERLAY);
		
		bone.instantDraw(
				poseStack,
				definition.modelData(),
				(Collection) controllers,
				(bakedBone, mesh, inherited) ->
						definition.renderResolver().resolve(entity, stack, bakedBone, mesh, inherited, partialTick),
				context,
				partialTick);
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
	
	public record RenderEntry(
			PLivingAttachmentDefinition definition,
			LivingEntity entity,
			ItemStack stack,
			Collection<PAnimationController<?>> controllers)
	{
	}
}
