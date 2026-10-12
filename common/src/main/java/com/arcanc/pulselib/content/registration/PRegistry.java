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
 * Small client-side registry for PulseLib extension types.
 */
public final class PRegistry<T>
{
	private final Map<ResourceLocation, T> values = new Object2ObjectOpenHashMap<>();

	public <V extends T> V register(ResourceLocation id, V value)
	{
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(value, "value");
		if (this.values.putIfAbsent(id, value) != null)
			throw new IllegalArgumentException("Duplicate PulseLib registry entry " + id);
		return value;
	}

	public Optional<T> get(ResourceLocation id)
	{
		return Optional.ofNullable(this.values.get(id));
	}

	public Map<ResourceLocation, T> entries()
	{
		return Map.copyOf(this.values);
	}

	public void validateAll(Map<ResourceLocation, ? extends T> additions)
	{
		additions.forEach((id, value) -> {
			Objects.requireNonNull(id, "id");
			Objects.requireNonNull(value, "value");
			T previous = this.values.get(id);
			if (previous != null && previous != value)
				throw new IllegalArgumentException("Duplicate PulseLib registry entry " + id);
		});
	}

	public void registerAll(Map<ResourceLocation, ? extends T> additions)
	{
		validateAll(additions);
		this.values.putAll(additions);
	}
}
