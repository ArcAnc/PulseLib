/**
 * @author ArcAnc
 * Created at: 12.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.platform;

import net.minecraft.world.entity.Entity;

/**
 * Loader-specific physical-side and entity riding behavior used by common code.
 */
public interface IPulsePlatform
{
	/**
	 * Returns whether the current process is a physical client.
	 *
	 * @return whether the current process is a physical client
	 */
	boolean isClient();

	/**
	 * Returns whether a passenger should use the sitting pose on the supplied mount.
	 *
	 * @param mount the entity carrying the passenger
	 * @return whether a passenger should use the sitting pose on the supplied mount
	 */
	boolean shouldRiderSit(Entity mount);
}
