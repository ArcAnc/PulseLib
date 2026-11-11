/**
 * @author ArcAnc
 * Created at: 30.07.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes vanilla first-person arm rendering for animated held-arm presentation.
 */
@Mixin(ItemInHandRenderer.class)
public interface ItemInHandRendererAccessor
{
	/**
	 * Invokes vanilla first-person arm rendering with the supplied hand-pass inputs.
	 *
	 * @param poseStack the current model-to-render pose stack
	 * @param collector the native deferred submission collector
	 * @param packedLight the packed block and sky light coordinates
	 * @param equippedProgress the vanilla hand equip animation progress
	 * @param swingProgress the vanilla hand swing animation progress
	 * @param side the arm to render
	 */
	@Invoker("renderPlayerArm")
	void pulselib$renderPlayerArm(PoseStack poseStack,
	                              SubmitNodeCollector collector,
	                              int packedLight,
	                              float equippedProgress,
	                              float swingProgress,
	                              HumanoidArm side);
}
