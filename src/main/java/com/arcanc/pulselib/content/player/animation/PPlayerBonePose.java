package com.arcanc.pulselib.content.player.animation;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Immutable canonical player-bone delta. */
public record PPlayerBonePose(Vector3f translation,
                              Quaternionf rotation,
                              Vector3f scale,
                              boolean hasTranslation,
                              boolean hasRotation,
                              boolean hasScale)
{
	public PPlayerBonePose
	{
		translation = new Vector3f(translation);
		rotation = new Quaternionf(rotation);
		scale = new Vector3f(scale);
	}

	@Override public Vector3f translation() { return new Vector3f(this.translation); }
	@Override public Quaternionf rotation() { return new Quaternionf(this.rotation); }
	@Override public Vector3f scale() { return new Vector3f(this.scale); }

	public boolean isAnimated()
	{
		return this.hasTranslation || this.hasRotation || this.hasScale;
	}
}
