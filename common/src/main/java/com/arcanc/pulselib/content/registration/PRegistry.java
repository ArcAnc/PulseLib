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
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Registers registry.
 */
public final class PRegistry<T>
{
	private final Map<Identifier, T> values = new Object2ObjectOpenHashMap<>();

	/**
	 * Performs the register operation.
	 * @param id the id to use.
	 * @param value the value to use.
	 * @return the value produced by this operation.
	 */
	public <V extends T> V register(Identifier id, V value)
	{
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(value, "value");
		if (this.values.putIfAbsent(id, value) != null)
			throw new IllegalArgumentException("Duplicate PulseLib registry entry " + id);
		return value;
	}

	/**
	 * Performs the get operation.
	 * @param id the id to use.
	 * @return the value produced by this operation.
	 */
	public Optional<T> get(Identifier id)
	{
		return Optional.ofNullable(this.values.get(id));
	}
}
