/**
 * @author ArcAnc
 * Created at: 23.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Immutable canonical player-bone delta.
 *
 * @param translation the local translation vector
 * @param rotation the local quaternion rotation
 * @param scale the local scale vector
 * @param hasTranslation whether the translation channel contributes an animated value
 * @param hasRotation whether the rotation channel contributes an animated value
 * @param hasScale whether the scale channel contributes an animated value
 */
public record PPlayerBonePose(Vector3f translation,
                              Quaternionf rotation,
                              Vector3f scale,
                              boolean hasTranslation,
                              boolean hasRotation,
                              boolean hasScale)
{
	/**
	 * Copies player bone transforms and records which channels contribute animation.
	 *
	 * @param translation the local translation vector
	 * @param rotation the local quaternion rotation
	 * @param scale the local scale vector
	 * @param hasTranslation whether the translation channel contributes an animated value
	 * @param hasRotation whether the rotation channel contributes an animated value
	 * @param hasScale whether the scale channel contributes an animated value
	 */
	public PPlayerBonePose
	{
		translation = new Vector3f(translation);
		rotation = new Quaternionf(rotation);
		scale = new Vector3f(scale);
	}

	/**
	 * Returns a defensive copy of the translation.
	 *
	 * @return a defensive copy of the translation
	 */
	@Override public Vector3f translation() { return new Vector3f(this.translation); }
	/**
	 * Returns a defensive copy of the rotation.
	 *
	 * @return a defensive copy of the rotation
	 */
	@Override public Quaternionf rotation() { return new Quaternionf(this.rotation); }
	/**
	 * Returns a defensive copy of the scale.
	 *
	 * @return a defensive copy of the scale
	 */
	@Override public Vector3f scale() { return new Vector3f(this.scale); }

	/**
	 * Returns whether translation, rotation or scale contributes an animated value.
	 *
	 * @return whether translation, rotation or scale contributes an animated value
	 */
	public boolean isAnimated()
	{
		return this.hasTranslation || this.hasRotation || this.hasScale;
	}
}
