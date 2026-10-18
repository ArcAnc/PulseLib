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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Runs inventory entity previews inside an isolated GUI render context.
 */
@Mixin (InventoryScreen.class)
public abstract class InventoryScreenMixin
{
	/**
	 * Renders the inventory entity preview in an isolated GUI submission scope.
	 *
	 * @param graphics the graphics
	 * @param x the x component or coordinate
	 * @param y the y component or coordinate
	 * @param scale the model scale factor
	 * @param translation the local translation vector
	 * @param pose the quaternion rotation
	 * @param camera the camera; may be {@code null}
	 * @param entity the entity being animated or rendered
	 * @param original the original implementation or unmodified value supplied by the render hook
	 */
	@WrapMethod (method = "renderEntityInInventory")
	private static void pulselib$guiContext(GuiGraphics graphics, float x, float y, float scale,
	                                      Vector3f translation, Quaternionf pose, @Nullable Quaternionf camera,
	                                      LivingEntity entity, Operation<Void> original)
	{
		try (var scope = PRenderContexts.enter(PRenderContexts.Context.GUI))
		{
			original.call(graphics, x, y, scale, translation, pose, camera, entity);
		}
	}
}
