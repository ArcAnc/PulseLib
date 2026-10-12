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
	Side value() default Side.CLIENT;

	enum Side
	{
		CLIENT,
		SERVER,
		BOTH;

		public boolean matches(boolean client)
		{
			return this == BOTH || (client ? this == CLIENT : this == SERVER);
		}
	}
}
