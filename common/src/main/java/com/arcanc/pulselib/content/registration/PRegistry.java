/**
 * @author ArcAnc
 * Created at: 01.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.registration;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Small platform-independent registry for PulseLib extension types.
 *
 * @param <T> the registered value type
 */
public final class PRegistry<T>
{
	private final Map<ResourceLocation, T> values = new Object2ObjectOpenHashMap<>();

	/**
	 * Adds a value under the supplied identifier and returns the registered value.
	 *
	 * @param <V> the registered value subtype
	 * @param id the namespaced resource or registration identifier
	 * @param value the value written by this operation
	 * @return the registered value
	 */
	public <V extends T> V register(ResourceLocation id, V value)
	{
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(value, "value");
		if (this.values.putIfAbsent(id, value) != null)
			throw new IllegalArgumentException("Duplicate PulseLib registry entry " + id);
		return value;
	}

	/**
	 * Returns the value registered under the supplied identifier when present.
	 *
	 * @param id the namespaced resource or registration identifier
	 * @return the value registered under the supplied identifier when present
	 */
	public Optional<T> get(ResourceLocation id)
	{
		return Optional.ofNullable(this.values.get(id));
	}

	/**
	 * Returns the registered identifier-to-value mappings.
	 *
	 * @return the registered identifier-to-value mappings
	 */
	public Map<ResourceLocation, T> entries()
	{
		return Map.copyOf(this.values);
	}

	/**
	 * Checks a batch of additions for invalid or duplicate registrations without applying it.
	 *
	 * @param additions the additions
	 */
	public void validateAll(Map<ResourceLocation, ? extends T> additions)
	{
		additions.forEach((id, value) ->
		{
			Objects.requireNonNull(id, "id");
			Objects.requireNonNull(value, "value");
			T previous = this.values.get(id);
			if (previous != null && previous != value)
				throw new IllegalArgumentException("Duplicate PulseLib registry entry " + id);
		});
	}

	/**
	 * Validates and installs a batch of namespaced values.
	 *
	 * @param additions the additions
	 */
	public void registerAll(Map<ResourceLocation, ? extends T> additions)
	{
		validateAll(additions);
		this.values.putAll(additions);
	}
}
