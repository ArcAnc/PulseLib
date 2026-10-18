/**
 * @author ArcAnc
 * Created at: 26.01.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.util;


import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared mod identifiers, logger and resource-location helpers.
 */
public class PLibDatabase
{
	/**
	 * Namespace used by built-in PulseLib resources and registrations.
	 */
	public static final String MOD_ID = "pulselib";
	/**
	 * Human-readable library name.
	 */
	public static final String MOD_NAME = "Pulse Lib";
	/**
	 * Logger used for PulseLib lifecycle and resource diagnostics.
	 */
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);
	
	/**
	 * Creates a resource location in the PulseLib namespace.
	 *
	 * @param name the resource path within the PulseLib namespace
	 * @return the namespaced resource or registry identifier
	 */
	public static ResourceLocation rl(String name)
	{
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, name);
	}
}
