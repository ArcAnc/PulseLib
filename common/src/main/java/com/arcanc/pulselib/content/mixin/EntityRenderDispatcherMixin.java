/**
 * @author ArcAnc
 * Created at: 17.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.arcanc.pulselib.content.renderer.PRenderContexts;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Isolates entity render submissions in GUI and offscreen contexts and discards failed local draws.
 */
@Mixin (EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin
{
	/**
	 * Isolates non-world entity submissions and discards them if the original render fails.
	 *
	 * @param <E> the entity type rendered or animated by this contract
	 * @param entity the entity being animated or rendered
	 * @param x the x component or coordinate
	 * @param y the y component or coordinate
	 * @param z the z component or coordinate
	 * @param yaw the yaw angle in degrees
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @param pose the current model-to-render pose stack
	 * @param buffer the vertex or buffer output
	 * @param light the packed block and sky light coordinates
	 * @param original the original implementation or unmodified value supplied by the render hook
	 */
	@WrapMethod (method = "render")
	private <E extends Entity> void pulselib$entityContext(E entity, double x, double y, double z, float yaw,
	                                                   float partialTick, PoseStack pose, MultiBufferSource buffer,
	                                                   int light, Operation<Void> original)
	{
		var context = PRenderContexts.current();
		if (context == PRenderContexts.Context.WORLD || context == PRenderContexts.Context.FIRST_PERSON)
		{
			original.call(entity, x, y, z, yaw, partialTick, pose, buffer, light);
			return;
		}
		try (var scope = PRenderContexts.enter(context))
		{
			try
			{
				original.call(entity, x, y, z, yaw, partialTick, pose, buffer, light);
			}
			catch (Throwable exception)
			{
				scope.discard();
				throw exception;
			}
		}
	}
}
