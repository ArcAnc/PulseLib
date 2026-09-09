/**
 * @author ArcAnc
 * Created at: 04.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation;


import org.joml.Quaternionf;
import org.joml.Vector3f;

public record PPlayerBonePose(Vector3f translation,
                              Quaternionf rotation,
                              Vector3f scale,
                              boolean hasTranslation,
                              boolean hasRotation,
                              boolean hasScale)
{
	public PPlayerBonePose(Vector3f translation,
	                       Quaternionf rotation,
	                       Vector3f scale,
	                       boolean hasTranslation,
	                       boolean hasRotation,
	                       boolean hasScale)
	{
		this.translation = new Vector3f(translation);
		this.rotation = new Quaternionf(rotation);
		this.scale = new Vector3f(scale);
		this.hasTranslation = hasTranslation;
		this.hasRotation = hasRotation;
		this.hasScale = hasScale;
	}
	
	@Override
	public Vector3f translation()
	{
		return new Vector3f(this.translation);
	}
	
	@Override
	public Quaternionf rotation()
	{
		return new Quaternionf(this.rotation);
	}
	
	@Override
	public Vector3f scale()
	{
		return new Vector3f(this.scale);
	}
	
	public boolean isAnimated()
	{
		return this.hasTranslation ||
				this.hasRotation ||
				this.hasScale;
	}
}
