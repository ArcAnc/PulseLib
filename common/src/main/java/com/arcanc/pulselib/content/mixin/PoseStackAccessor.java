/**
 * @author ArcAnc
 * Created at: 25.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Native item layers can throw after more than one pose push; rollback uses invocation entry depth. */
@Mixin(PoseStack.class)
public interface PoseStackAccessor
{
	@Accessor("lastIndex")
	int pulselib$getDepth();
}
