/**
 * @author ArcAnc
 * Created at: 04.07.2026
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
import com.arcanc.pulselib.util.PRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.List;

public class PLivingAttachmentLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M>
{
	protected static final List<EquipmentSlot> EQUIPMENT_SLOTS = List.of(
			EquipmentSlot.HEAD,
			EquipmentSlot.CHEST,
			EquipmentSlot.LEGS,
			EquipmentSlot.FEET,
			EquipmentSlot.BODY,
			EquipmentSlot.MAINHAND,
			EquipmentSlot.OFFHAND);
	
	public PLivingAttachmentLayer(RenderLayerParent<T, M> parent)
	{
		super(parent);
	}
	
	@Override
	public void render(PoseStack poseStack,
	                   MultiBufferSource buffer,
	                   int light,
	                   T entity,
	                   float limbSwing,
	                   float limbSwingAmount,
	                   float partialTick,
	                   float ageInTicks,
	                   float netHeadYaw,
	                   float headPitch)
	{
		for (EquipmentSlot slot : EQUIPMENT_SLOTS)
		{
			ItemStack stack = entity.getItemBySlot(slot);
			for (PLivingAttachmentDefinition definition : PLivingAttachments.get(stack, slot, entity))
				renderDefinition(poseStack, light, partialTick, entity, stack, definition,
						binding -> PAttachmentAnchorResolvers.resolve(entity, this.getParentModel(), binding.anchor()));
		}
		
		for (PLivingAttachmentDefinition definition : PLivingAttachments.getGlobal(entity))
			renderDefinition(poseStack, light, partialTick, entity, ItemStack.EMPTY, definition,
					binding -> PAttachmentAnchorResolvers.resolve(entity, this.getParentModel(), binding.anchor()));
	}
	
	protected static void renderFirstPersonAnchor(PoseStack poseStack,
	                                              int light,
	                                              LivingEntity entity,
	                                              PAttachmentAnchor targetAnchor,
	                                              ModelPart anchorPart,
	                                              float partialTick)
	{
		for (EquipmentSlot slot : EQUIPMENT_SLOTS)
		{
			ItemStack stack = entity.getItemBySlot(slot);
			for (PLivingAttachmentDefinition definition : PLivingAttachments.get(stack, slot, entity))
				renderDefinition(poseStack, light, partialTick, entity, stack, definition,
						binding -> binding.anchor().equals(targetAnchor) ? anchorPart : null);
		}

		for (PLivingAttachmentDefinition definition : PLivingAttachments.getGlobal(entity))
			renderDefinition(poseStack, light, partialTick, entity, ItemStack.EMPTY, definition,
					binding -> binding.anchor().equals(targetAnchor) ? anchorPart : null);
	}
	
	private static void renderDefinition(
			PoseStack poseStack,
			int light,
			float partialTick,
			LivingEntity entity,
			ItemStack stack,
			PLivingAttachmentDefinition definition,
			AttachmentPartResolver partResolver)
	{
		PBakedModel model = definition.model().getModel();
		if (model == null)
			return;
		
		Collection<PAnimationController<?>> controllers = definition.animationControllers(entity, stack, model, partialTick);
		
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
			
			drawBone(poseStack, definition, bone, entity, stack, controllers, light, partialTick);
			
			poseStack.popPose();
		}
	}
	
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
				PRenderTypes.RenderTypeProvider :: trianglesImmediate,
				-1,
				light,
				OverlayTexture.NO_OVERLAY);
		
		bone.instantDraw(
				poseStack,
				definition.model(),
				(Collection) controllers,
				(bakedBone, mesh, inherited) ->
						definition.renderResolver().resolve(entity, stack, bakedBone, mesh, inherited, partialTick),
				context,
				partialTick);
	}
	
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
		@Nullable ModelPart resolve(PAttachmentBinding binding);
	}
}
