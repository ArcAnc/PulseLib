/**
 * @author ArcAnc
 * Created at: 14.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.arcanc.pulselib.content.animatable.PItemAnimatable;
import com.arcanc.pulselib.content.animatable.PItemStackAnimationData;
import com.arcanc.pulselib.content.animatable.PItemStackAnimationIds;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stores transient item animation identities and preserves or separates them during copy and split
 * operations.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements PItemStackAnimationData
{
	@Unique
	private transient volatile long pulselib$animationId;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long pulselib$getAnimationId()
	{
		return this.pulselib$animationId;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void pulselib$setAnimationId(long id)
	{
		if (id < 0)
			throw new IllegalArgumentException("Item animation IDs must be nonnegative");
		if ((Object)this != ItemStack.EMPTY)
			this.pulselib$animationId = id;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long pulselib$getOrCreateAnimationId()
	{
		long id = this.pulselib$animationId;
		if (id != 0)
			return id;
		ItemStack stack = (ItemStack)(Object)this;
		if (stack.isEmpty() || !(stack.getItem() instanceof PItemAnimatable<?>))
			return 0;
		synchronized (this)
		{
			if (this.pulselib$animationId == 0)
				this.pulselib$animationId = PItemStackAnimationIds.nextId();
			return this.pulselib$animationId;
		}
	}

	/**
	 * Copies a live animation identity to a nonempty copy without creating animation controllers.
	 *
	 * @param callback the Mixin callback used to inspect or replace the intercepted result
	 */
	@Inject(method = "copy", at = @At("RETURN"))
	private void pulselib$copyAnimationId(CallbackInfoReturnable<ItemStack> callback)
	{
		ItemStack result = callback.getReturnValue();
		if (!result.isEmpty())
		{
			long id = pulselib$getOrCreateAnimationId();
			if (id != 0)
				((PItemStackAnimationData)(Object)result).pulselib$setAnimationId(id);
		}
	}

	/**
	 * Separates live animation identities after a split unless the source stack was fully transferred.
	 *
	 * @param callback the Mixin callback used to inspect or replace the intercepted result
	 */
	@Inject(method = "split", at = @At("RETURN"))
	private void pulselib$splitAnimationId(CallbackInfoReturnable<ItemStack> callback)
	{
		ItemStack result = callback.getReturnValue();
		if (result.isEmpty())
			return;
		PItemStackAnimationData data = (PItemStackAnimationData)(Object)result;
		if (data.pulselib$getAnimationId() == 0)
			return;
		if (((ItemStack)(Object)this).isEmpty())
			pulselib$setAnimationId(0);
		else
			data.pulselib$setAnimationId(PItemStackAnimationIds.nextId());
	}
}
