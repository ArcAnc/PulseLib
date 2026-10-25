/**
 * @author ArcAnc
 * Created at: 25.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Narrow access to the extractor's native state and copied pose/scissor parameters. */
@Mixin(GuiGraphicsExtractor.class)
public interface GuiGraphicsExtractorAccessor
{
	@Accessor("guiRenderState")
	GuiRenderState pulselib$getRenderState();

	@Invoker("createDefaultTextParameters")
	ActiveTextCollector.Parameters pulselib$parameters(float opacity);
}
