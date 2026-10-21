/**
 * @author ArcAnc
 * Created at: 12.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.api.plugin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A public plugin class with a public no-argument constructor. No loader entrypoint is needed.
 */
@Retention (RetentionPolicy.RUNTIME)
@Target (ElementType.TYPE)
public @interface PulsePlugin
{
	/**
	 * Returns the physical sides on which the annotated plugin may be instantiated.
	 *
	 * @return the physical sides on which the annotated plugin may be instantiated
	 */
	Side value() default Side.CLIENT;

	/**
	 * Physical-side restriction used when discovering plugin entry points.
	 */
	enum Side
	{
		/**
		 * Loads the plugin only in a physical client process.
		 */
		CLIENT,
		/**
		 * Loads the plugin only in a dedicated server process.
		 */
		SERVER,
		/**
		 * Loads the plugin on either physical side.
		 */
		BOTH;

		/**
		 * Tests whether this restriction permits loading on the supplied physical side.
		 *
		 * @param client whether the current process is a physical client
		 * @return whether this restriction permits loading on the supplied physical side
		 */
		public boolean matches(boolean client)
		{
			return this == BOTH || (client ? this == CLIENT : this == SERVER);
		}
	}
}
