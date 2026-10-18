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
 * Supplies animation controller factories and resolves animation state for a concrete owner identity.
 * <p>
 * Part of this code copied from Geckolib: <a href="https://github.com/bernie-g/geckolib/blob/main/common/src/main/java/com/geckolib/animatable/GeoAnimatable.java">GeoAnimatable</a>
 * <p>Stop crying, Tslat!</p>
 * <p>Modified by ArcAnc</p>
 *
 * @param <T> the animatable owner type
 */
public interface PAnimatable<T extends PAnimatable<T>>
{
	/**
	 * Returns the manager for a context after owner construction.
	 * An owner may store PLibHelper.createManager(this) in a field and return field.getManager(key).
	 * Custom storage strategies and direct access to instance-owned managers remain supported.
	 *
	 * @param key the key
	 * @return the manager for a context after owner construction.
 An owner may store PLibHelper.createManager(this) in a field and return field.getManager(key).
 Custom storage strategies and direct access to instance-owned managers remain supported
	 */
	PAnimationManager<T> getAnimationManager(AnimManagerKey key);
	
	/**
	 * Declares the controllers and animation graphs to create for each animation manager.
	 *
	 * @param registrar the active registration scope for this callback
	 */
	void registerAnimationControllers(PAnimationManager.PAnimationRegistrar<T> registrar);
}
