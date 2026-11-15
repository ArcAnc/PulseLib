/**
 * @author ArcAnc
 * Created at: 15.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.porting.scene.mixin;

import com.arcanc.pulselib.porting.scene.WorldChecks;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;

/** Only the fixture's explicitly marked exception is handled, after the production framegraph finally. */
@Mixin(GameRenderer.class)
public abstract class WorldRenderObserver
{
	@WrapMethod(method = "renderLevel")
	private void scene$expectedFailure(DeltaTracker delta, Operation<Void> original)
	{
		var matrix = RenderSystem.getModelViewMatrixCopy();
		try
		{
			original.call(delta);
		}
		catch (WorldChecks.ExpectedWorldAbort expected)
		{
			WorldChecks.recoveredAbort(matrix);
		}
	}
}
