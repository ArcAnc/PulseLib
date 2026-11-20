/**
 * @author ArcAnc
 * Created at: 25.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.arcanc.pulselib.content.renderer.PRenderContexts;
import com.arcanc.pulselib.content.renderer.rhi.PRhiRenderState;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.joml.Matrix4fStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Owns the local queue from native PIP submission through deferred feature execution. */
@Mixin(PictureInPictureRenderer.class)
public abstract class PictureInPictureRendererMixin
{
	@Shadow
	@Final
	private SubmitNodeStorage submitNodeStorage;

	@Unique
	private PRenderContexts.@Nullable Scope pulselib$scope;

	@Unique
	private boolean pulselib$modelViewPushed;

	@WrapMethod(method = "prepare(Lnet/minecraft/client/renderer/state/gui/pip/PictureInPictureRenderState;Lnet/minecraft/client/renderer/state/gui/GuiRenderState;Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;I)V")
	private void pulselib$restoreCaller(PictureInPictureRenderState state, GuiRenderState gui, FeatureRenderDispatcher dispatcher, int scale, Operation<Void> original)
	{
		try (var caller = new PRhiRenderState())
		{
			try
			{
				original.call(state, gui, dispatcher, scale);
			}
			catch (RuntimeException | Error failure)
			{
				this.submitNodeStorage.getSubmitsPerOrder().clear();
				var scope = this.pulselib$scope;
				this.pulselib$scope = null;
				try (scope)
				{
					if (scope != null)
						scope.discard();
					throw failure;
				}
			}
			finally
			{
				if (this.pulselib$modelViewPushed)
				{
					this.pulselib$modelViewPushed = false;
					RenderSystem.getModelViewStack().popMatrix();
				}
			}
		}
	}

	@WrapOperation(method = "prepare", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4fStack;pushMatrix()Lorg/joml/Matrix4fStack;"))
	private Matrix4fStack pulselib$pushModelView(Matrix4fStack stack, Operation<Matrix4fStack> original)
	{
		var result = original.call(stack);
		this.pulselib$modelViewPushed = true;
		return result;
	}

	@Inject(method = "prepare", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/pip/PictureInPictureRenderer;renderToTexture(Lnet/minecraft/client/renderer/state/gui/pip/PictureInPictureRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V"))
	private void pulselib$enterLocal(PictureInPictureRenderState state, GuiRenderState gui, FeatureRenderDispatcher dispatcher, int scale, CallbackInfo callback)
	{
		// The inherited scissor is in the caller's target coordinates. GUI clipping is applied at blit.
		RenderSystem.disableScissorForRenderTypeDraws();
		this.pulselib$scope = PRenderContexts.enter(PRenderContexts.Context.GUI);
	}

	@WrapOperation(method = "prepare", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4fStack;popMatrix()Lorg/joml/Matrix4fStack;"))
	private Matrix4fStack pulselib$flushLocal(Matrix4fStack stack, Operation<Matrix4fStack> original)
	{
		var scope = this.pulselib$scope;
		this.pulselib$scope = null;
		if (scope != null)
			scope.close();
		var result = original.call(stack);
		this.pulselib$modelViewPushed = false;
		return result;
	}
}
