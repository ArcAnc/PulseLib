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

@Mixin (InventoryScreen.class)
public abstract class InventoryScreenMixin
{
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
