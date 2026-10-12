package com.arcanc.pulselib.api.plugin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** A public plugin class with a public no-argument constructor. No loader entrypoint is needed. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PulsePlugin
{
	Side value() default Side.CLIENT;

	enum Side
	{
		CLIENT, SERVER, BOTH;

		public boolean matches(boolean client)
		{
			return this == BOTH || (client ? this == CLIENT : this == SERVER);
		}
	}
}
