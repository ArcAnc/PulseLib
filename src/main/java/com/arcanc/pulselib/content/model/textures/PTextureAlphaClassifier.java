/**
 * @author ArcAnc
 * Created at: 21.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.textures;

import com.arcanc.pulselib.content.model.textures.atlas.PLibSpriteMetadata;
import com.mojang.blaze3d.platform.Transparency;
import net.minecraft.client.renderer.texture.SpriteContents;

import java.util.Map;
import java.util.WeakHashMap;

public final class PTextureAlphaClassifier
{
	private static final Map<SpriteContents, PAlphaMode> CACHE = new WeakHashMap<>();

	/**
	 * Creates an instance of the enclosing type.
	 */
	private PTextureAlphaClassifier()
	{
	}

	/**
	 * Performs the resolve operation.
	 * @param contents the contents to use.
	 * @return the value produced by this operation.
	 */
	public static synchronized PAlphaMode resolve(SpriteContents contents)
	{
		return CACHE.computeIfAbsent(contents, PTextureAlphaClassifier :: classify);
	}

	/**
	 * Performs the clear operation.
	 */
	public static synchronized void clear()
	{
		CACHE.clear();
	}

	/**
	 * Performs the classify operation.
	 * @param contents the contents to use.
	 * @return the value produced by this operation.
	 */
	private static PAlphaMode classify(SpriteContents contents)
	{
		PAlphaMode configured = contents.getAdditionalMetadata(PLibSpriteMetadata.TYPE).
				map(PLibSpriteMetadata :: alphaMode).
				orElse(PAlphaMode.AUTO);
		if (configured != PAlphaMode.AUTO)
			return configured;

		Transparency transparency = contents.getOriginalImage().computeTransparency();
		if (transparency.hasTranslucent())
			return PAlphaMode.TRANSLUCENT;
		return transparency.hasTransparent() ? PAlphaMode.CUTOUT : PAlphaMode.OPAQUE;
	}
}
