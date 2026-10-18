/**
 * @author ArcAnc
 * Created at: 21.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.textures;

import com.arcanc.pulselib.content.mixin.SpriteContentsAccessor;
import com.arcanc.pulselib.content.model.textures.atlas.PLibMetadata;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Caches texture-pixel analysis used to choose opaque, cutout or translucent rendering.
 */
public final class PTextureAlphaClassifier
{
	private static final Map<SpriteContents, PAlphaMode> CACHE = new WeakHashMap<>();

	private PTextureAlphaClassifier()
	{
	}

	/**
	 * Classifies the alpha values of the supplied sprite contents.
	 *
	 * @param contents the contents
	 * @return the alpha policy inferred from the sprite pixels
	 */
	public static synchronized PAlphaMode resolve(SpriteContents contents)
	{
		return CACHE.computeIfAbsent(contents, PTextureAlphaClassifier :: classify);
	}

	/**
	 * Discards cached texture alpha classifications.
	 */
	public static synchronized void clear()
	{
		CACHE.clear();
	}

	private static PAlphaMode classify(SpriteContents contents)
	{
		PAlphaMode configured = contents.metadata().getSection(PLibMetadata.TYPE).
				map(PLibMetadata :: alphaMode).
				orElse(PAlphaMode.AUTO);
		if (configured != PAlphaMode.AUTO)
			return configured;

		NativeImage image = ((SpriteContentsAccessor) contents).pulselib$getOriginalImage();
		boolean transparent = false;
		for (int y = 0; y < image.getHeight(); y++)
			for (int x = 0; x < image.getWidth(); x++)
			{
				int alpha = image.getPixelRGBA(x, y) >>> 24;
				if (alpha == 0)
					transparent = true;
				else if (alpha < 255)
					return PAlphaMode.TRANSLUCENT;
			}
		return transparent ? PAlphaMode.CUTOUT : PAlphaMode.OPAQUE;
	}
}
