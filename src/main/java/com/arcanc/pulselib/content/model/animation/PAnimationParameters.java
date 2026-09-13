/**
 * @author ArcAnc
 * Created at: 05.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class PAnimationParameters
{
	private final Map<String, Float> numbers = new HashMap<>();
	private final Set<String> triggers = new HashSet<>();

	/**
	 * Performs the set operation.
	 * @param name the name to use.
	 * @param value the value to use.
	 */
	public void set(String name, float value)
	{
		this.numbers.put(requireName(name), value);
	}

	/**
	 * Performs the set operation.
	 * @param name the name to use.
	 * @param value the value to use.
	 */
	public void set(String name, boolean value)
	{
		set(name, value ? 1.0f : 0.0f);
	}

	/**
	 * Performs the get operation.
	 * @param name the name to use.
	 * @return the value produced by this operation.
	 */
	public float get(String name)
	{
		return get(name, 0.0f);
	}

	/**
	 * Performs the get operation.
	 * @param name the name to use.
	 * @param defaultValue the default value to use.
	 * @return the value produced by this operation.
	 */
	public float get(String name, float defaultValue)
	{
		return this.numbers.getOrDefault(requireName(name), defaultValue);
	}

	/**
	 * Returns the boolean.
	 * @param name the name to use.
	 * @return the value produced by this operation.
	 */
	public boolean getBoolean(String name)
	{
		return get(name) != 0.0f;
	}

	/**
	 * Performs the values operation.
	 * @return the value produced by this operation.
	 */
	public Map<String, Float> values()
	{
		return Collections.unmodifiableMap(this.numbers);
	}

	/**
	 * Performs the trigger operation.
	 * @param name the name to use.
	 */
	public void trigger(String name)
	{
		this.triggers.add(requireName(name));
	}

	/**
	 * Determines whether triggered.
	 * @param name the name to use.
	 * @return the value produced by this operation.
	 */
	public boolean isTriggered(String name)
	{
		return this.triggers.contains(requireName(name));
	}

	/**
	 * Performs the consume trigger operation.
	 * @param name the name to use.
	 * @return the value produced by this operation.
	 */
	public boolean consumeTrigger(String name)
	{
		return this.triggers.remove(requireName(name));
	}

	/**
	 * Performs the require name operation.
	 * @param name the name to use.
	 * @return the value produced by this operation.
	 */
	private static String requireName(String name)
	{
		if (name == null || name.isBlank())
			throw new IllegalArgumentException("Animation parameter name must not be blank");
		return name;
	}
}
