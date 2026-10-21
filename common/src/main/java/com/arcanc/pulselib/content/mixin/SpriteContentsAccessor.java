/**
 * @author ArcAnc
 * Created at: 12.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes original sprite pixels for texture alpha classification.
 */
@Mixin (SpriteContents.class)
public interface SpriteContentsAccessor
{
	/**
	 * Returns the original sprite image used for alpha classification.
	 *
	 * @return the original sprite image used for alpha classification
	 */
	@Accessor ("originalImage")
	NativeImage pulselib$getOriginalImage();
}
