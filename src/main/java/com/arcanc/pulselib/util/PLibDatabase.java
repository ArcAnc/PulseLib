/**
 * @author ArcAnc
 * Created at: 26.01.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.util;


import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides support for lib database.
 */
public class PLibDatabase
{
	public static final String MOD_ID = "pulselib";
	public static final String MOD_NAME = "Pulse Lib";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);
	
	public static final Identifier RELOAD_LISTENER_ID = rl("models_reload_listener");
	
	/**
	 * Performs the rl operation.
	 * @param name the name to use.
	 * @return the value produced by this operation.
	 */
	public static Identifier rl(String name)
	{
		return Identifier.fromNamespaceAndPath(MOD_ID, name);
	}
}
