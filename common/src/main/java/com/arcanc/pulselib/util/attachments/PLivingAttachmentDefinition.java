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

/**
 * Model, source predicate, bone bindings, material resolver and controller provider for a living
 * attachment.
 *
 * @param model the canonical model and loader selection
 * @param source the predicate selecting eligible attachment owners and equipment
 * @param bindings the attachment bone-to-anchor bindings
 * @param renderResolver the callback resolving attachment mesh render inputs
 * @param hideVanilla whether this attachment replaces matching vanilla armor
 * @param controllerProvider the callback supplying attachment animation controllers
 */
public record PLivingAttachmentDefinition(
		PModelData model,
		PLivingAttachmentSource source,
		List<PAttachmentBinding> bindings,
		PLivingMeshRenderResolver renderResolver,
		boolean hideVanilla,
		ControllerProvider controllerProvider)
{
	/**
	 * Copies the model bindings and rendering policy of a living attachment.
	 *
	 * @param model the canonical model and loader selection
	 * @param source the predicate selecting eligible attachment owners and equipment
	 * @param bindings the attachment bone-to-anchor bindings
	 * @param renderResolver the callback resolving attachment mesh render inputs
	 * @param hideVanilla whether this attachment replaces matching vanilla armor
	 */
	public PLivingAttachmentDefinition(PModelData model,
	                                   PLivingAttachmentSource source,
	                                   List<PAttachmentBinding> bindings,
	                                   PLivingMeshRenderResolver renderResolver,
	                                   boolean hideVanilla)
	{
		this(model, source, bindings, renderResolver, hideVanilla, ControllerProvider.EMPTY);
	}
	
	/**
	 * Copies the model bindings and rendering policy of a living attachment.
	 *
	 * @param model the canonical model and loader selection
	 * @param source the predicate selecting eligible attachment owners and equipment
	 * @param bindings the attachment bone-to-anchor bindings
	 * @param renderResolver the callback resolving attachment mesh render inputs
	 * @param hideVanilla whether this attachment replaces matching vanilla armor
	 * @param controllerProvider the callback supplying attachment animation controllers
	 */
	public PLivingAttachmentDefinition
	{
		Objects.requireNonNull(model);
		Objects.requireNonNull(source);
		bindings = List.copyOf(Objects.requireNonNull(bindings));
		Objects.requireNonNull(renderResolver);
		Objects.requireNonNull(controllerProvider);
	}
	
	/**
	 * Returns the model selection used to render this attachment.
	 *
	 * @return the model selection used to render this attachment
	 */
	public PModelData modelData()
	{
		return this.model;
	}
	
	/**
	 * Returns the controllers used to animate this attachment for the current owner and stack.
	 *
	 * @param entity the entity being animated or rendered
	 * @param stack the item stack supplying render or animation state
	 * @param model the baked model supplying indexed bones and animations
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @return the controllers used to animate this attachment for the current owner and stack
	 */
	public Collection<PAnimationController<?>> animationControllers(LivingEntity entity, ItemStack stack, PBakedModel model, float partialTick)
	{
		return this.controllerProvider.controllers(entity, stack, model, partialTick);
	}
	
	/**
	 * Supplies optional animation controllers for a living attachment render.
	 */
	@FunctionalInterface
	public interface ControllerProvider
	{
		/**
		 * Provider that returns no attachment animation controllers.
		 */
		ControllerProvider EMPTY = (entity, stack, model, partialTick) -> List.of();
		
		/**
		 * Returns the controller collection used for this attachment render sample.
		 *
		 * @param entity the entity being animated or rendered
		 * @param stack the item stack supplying render or animation state
		 * @param model the baked model supplying indexed bones and animations
		 * @param partialTick the interpolation fraction between the previous and current game ticks
		 * @return the controller collection used for this attachment render sample
		 */
		Collection<PAnimationController<?>> controllers(LivingEntity entity, ItemStack stack, PBakedModel model, float partialTick);
	}
}
