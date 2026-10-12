/**
 * @author ArcAnc
 * Created at: 07.07.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.util.attachments;


import com.arcanc.pulselib.content.animatable.PAnimationController;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public record PLivingAttachmentDefinition(
		PModelData model,
		PLivingAttachmentSource source,
		List<PAttachmentBinding> bindings,
		PLivingMeshRenderResolver renderResolver,
		boolean hideVanilla,
		ControllerProvider controllerProvider)
{
	public PLivingAttachmentDefinition(PModelData model,
	                                   PLivingAttachmentSource source,
	                                   List<PAttachmentBinding> bindings,
	                                   PLivingMeshRenderResolver renderResolver,
	                                   boolean hideVanilla)
	{
		this(model, source, bindings, renderResolver, hideVanilla, ControllerProvider.EMPTY);
	}
	
	public PLivingAttachmentDefinition
	{
		Objects.requireNonNull(model);
		Objects.requireNonNull(source);
		bindings = List.copyOf(Objects.requireNonNull(bindings));
		Objects.requireNonNull(renderResolver);
		Objects.requireNonNull(controllerProvider);
	}
	
	public PModelData modelData()
	{
		return this.model;
	}
	
	public Collection<PAnimationController<?>> animationControllers(LivingEntity entity, ItemStack stack, PBakedModel model, float partialTick)
	{
		return this.controllerProvider.controllers(entity, stack, model, partialTick);
	}
	
	@FunctionalInterface
	public interface ControllerProvider
	{
		ControllerProvider EMPTY = (entity, stack, model, partialTick) -> List.of();
		
		Collection<PAnimationController<?>> controllers(LivingEntity entity, ItemStack stack, PBakedModel model, float partialTick);
	}
}
