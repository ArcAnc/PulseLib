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

/**
 * Named float and boolean graph parameters together with consumable one-shot triggers.
 */
public final class PAnimationParameters
{
	private final Map<String, Float> numbers = new HashMap<>();
	private final Set<String> triggers = new HashSet<>();

	/**
	 * Stores a named graph parameter, encoding boolean values as zero or one.
	 *
	 * @param name the graph parameter name
	 * @param value the parameter value to store
	 */
	public void set(String name, float value)
	{
		this.numbers.put(requireName(name), value);
	}

	/**
	 * Stores a named graph parameter, encoding boolean values as zero or one.
	 *
	 * @param name the graph parameter name
	 * @param value the parameter value to store
	 */
	public void set(String name, boolean value)
	{
		set(name, value ? 1.0f : 0.0f);
	}

	/**
	 * Returns the named numeric parameter or the requested fallback when it is absent.
	 *
	 * @param name the graph parameter name
	 * @return the named numeric parameter or the requested fallback when it is absent
	 */
	public float get(String name)
	{
		return get(name, 0.0f);
	}

	/**
	 * Returns the named numeric parameter or the requested fallback when it is absent.
	 *
	 * @param name the graph parameter name
	 * @param defaultValue the default value
	 * @return the named numeric parameter or the requested fallback when it is absent
	 */
	public float get(String name, float defaultValue)
	{
		return this.numbers.getOrDefault(requireName(name), defaultValue);
	}

	/**
	 * Returns whether the named parameter represents a true value.
	 *
	 * @param name the graph parameter name
	 * @return whether the named parameter represents a true value
	 */
	public boolean getBoolean(String name)
	{
		return get(name) != 0.0f;
	}

	/**
	 * Returns the currently stored numeric graph parameters.
	 *
	 * @return the currently stored numeric graph parameters
	 */
	public Map<String, Float> values()
	{
		return Collections.unmodifiableMap(this.numbers);
	}

	/**
	 * Marks a trigger as pending until it is consumed.
	 *
	 * @param name the graph parameter name
	 */
	public void trigger(String name)
	{
		this.triggers.add(requireName(name));
	}

	/**
	 * Returns whether the named trigger is currently pending.
	 *
	 * @param name the graph parameter name
	 * @return whether the named trigger is currently pending
	 */
	public boolean isTriggered(String name)
	{
		return this.triggers.contains(requireName(name));
	}

	/**
	 * Clears a pending trigger and reports whether it was present.
	 *
	 * @param name the graph parameter name
	 * @return whether it was present
	 */
	public boolean consumeTrigger(String name)
	{
		return this.triggers.remove(requireName(name));
	}

	private static String requireName(String name)
	{
		if (name == null || name.isBlank())
			throw new IllegalArgumentException("Animation parameter name must not be blank");
		return name;
	}
}
