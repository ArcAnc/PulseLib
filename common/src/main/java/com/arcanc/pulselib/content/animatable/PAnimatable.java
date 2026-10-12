/**
 * @author ArcAnc
 * Created at: 27.01.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.animatable;

/**
 * Part of this code copied from Geckolib: <a href="https://github.com/bernie-g/geckolib/blob/main/common/src/main/java/com/geckolib/animatable/GeoAnimatable.java">GeoAnimatable</a>
 * <p>Stop crying, Tslat!</p>
 * <p>Modified by ArcAnc</p>
 */
public interface PAnimatable<T extends PAnimatable<T>>
{
	PAnimationManager<T> getAnimationManager(AnimManagerKey key);
	
	void registerAnimationControllers(PAnimationManager.PAnimationRegistrar<T> registrar);
}
