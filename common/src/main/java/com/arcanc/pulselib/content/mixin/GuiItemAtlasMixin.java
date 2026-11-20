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
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.render.DynamicAtlasAllocator;
import net.minecraft.client.gui.render.GuiItemAtlas;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Owns one atlas slot's Pulse submissions through deferred features and native buffer flushing. */
@Mixin(GuiItemAtlas.class)
public abstract class GuiItemAtlasMixin
{
	@Shadow
	@Final
	private PoseStack poseStack;

	@Shadow
	@Final
	private SubmitNodeStorage submitNodeStorage;

	@Shadow
	@Final
	private DynamicAtlasAllocator<Object> allocator;

	@Unique
	private PRenderContexts.@Nullable Scope pulselib$scope;

	@WrapMethod(method = "drawToSlot")
	private void pulselib$restoreSlot(int x, int y, boolean clear, ItemStackRenderState item, Operation<Void> original)
	{
		PRenderContexts.@Nullable Scope previous = this.pulselib$scope;
		int depth = ((PoseStackAccessor)this.poseStack).pulselib$getDepth();
		PoseStack.Pose pose = this.poseStack.last().copy();
		this.pulselib$scope = null;
		try (var caller = new PRhiRenderState())
		{
			try
			{
				original.call(x, y, clear, item);
				if (this.pulselib$scope != null)
					this.pulselib$scope.close();
			}
			catch (RuntimeException | Error failure)
			{
				this.submitNodeStorage.getSubmitsPerOrder().clear();
				try (var scope = this.pulselib$scope)
				{
					if (scope != null)
						scope.discard();
					while (((PoseStackAccessor)this.poseStack).pulselib$getDepth() > depth)
						this.poseStack.popPose();
					this.poseStack.last().set(pose);
					throw failure;
				}
			}
		}
		finally
		{
			this.pulselib$scope = previous;
		}
	}

	/** An aborted draw must be retried as a stale slot, including its color/depth clear. */
	@WrapMethod(method = "getOrUpdate")
	private GuiItemAtlas.@Nullable SlotView pulselib$releaseFailedSlot(TrackingItemStackRenderState item,
			Operation<GuiItemAtlas.@Nullable SlotView> original)
	{
		try
		{
			return original.call(item);
		}
		catch (RuntimeException | Error failure)
		{
			Object key = item.getModelIdentity();
			((DynamicAtlasAllocatorAccessor)(Object)this.allocator).pulselib$freeSlotIf((candidate, slot) -> key.equals(candidate));
			throw failure;
		}
	}

	@Inject(method = "drawToSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
	private void pulselib$enterSlot(int x, int y, boolean clear, ItemStackRenderState item, CallbackInfo callback)
	{
		this.pulselib$scope = PRenderContexts.enter(PRenderContexts.Context.GUI);
	}
}
