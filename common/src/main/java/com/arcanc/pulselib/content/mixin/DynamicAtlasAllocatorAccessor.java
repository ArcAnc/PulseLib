/**
 * @author ArcAnc
 * Created at: 20.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import net.minecraft.client.gui.render.DynamicAtlasAllocator;
import org.jetbrains.annotations.ApiStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.BiPredicate;

@ApiStatus.Internal
@Mixin(DynamicAtlasAllocator.class)
public interface DynamicAtlasAllocatorAccessor
{
	@Invoker("freeSlotIf")
	void pulselib$freeSlotIf(BiPredicate<Object, DynamicAtlasAllocator.Slot> predicate);
}
