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
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Scopes world rendering and guarantees end-of-frame cleanup of deferred submissions.
 */
@Mixin (LevelRenderer.class)
public abstract class LevelRendererContextMixin
{
	/**
	 * Runs level rendering in world context and guarantees deferred frame cleanup.
	 *
	 * @param deltaTracker the delta tracker
	 * @param outline the outline
	 * @param camera the camera
	 * @param renderer the renderer
	 * @param light the packed block and sky light coordinates
	 * @param view the view
	 * @param projection the captured projection matrix
	 * @param original the original implementation or unmodified value supplied by the render hook
	 */
	@WrapMethod (method = "renderLevel")
	private void pulselib$worldContext(DeltaTracker deltaTracker, boolean outline, Camera camera,
	                                 GameRenderer renderer, LightTexture light, Matrix4f view, Matrix4f projection,
	                                 Operation<Void> original)
	{
		try (var scope = PRenderContexts.enter(PRenderContexts.Context.WORLD))
		{
			original.call(deltaTracker, outline, camera, renderer, light, view, projection);
		}
		finally
		{
			PRenderQueue.endWorldFrame();
		}
	}
}
