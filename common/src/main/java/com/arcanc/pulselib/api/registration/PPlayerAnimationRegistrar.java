/**
 * @author ArcAnc
 * Created at: 12.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.api.registration;

import com.arcanc.pulselib.content.player.animation.PPlayerAnimationDefinition;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Collects named player animation definitions during a client plugin callback.
 */
public class PPlayerAnimationRegistrar extends PulseRegistrar
{
	private final Map<ResourceLocation, PPlayerAnimationDefinition> definitions = new LinkedHashMap<>();

	/**
	 * Queues a player animation definition under its namespaced identifier.
	 *
	 * @param id the namespaced resource or registration identifier
	 * @param definition the immutable configuration to register or evaluate
	 */
	public void register(ResourceLocation id, PPlayerAnimationDefinition definition)
	{
		checkOpen();
		Objects.requireNonNull(id);
		Objects.requireNonNull(definition);
		PPlayerAnimationDefinition previous = this.definitions.putIfAbsent(id, definition);
		if (previous != null && previous != definition)
			throw new IllegalArgumentException("Conflicting player animation definition: " + id);
	}

	/**
	 * Installs the collected animation definitions and closes this registration scope.
	 */
	public void apply()
	{
		checkOpen();
		this.definitions.forEach((id, definition) ->
		{
			PPlayerAnimationDefinition previous = PPlayerAnimations.get(id);
			if (previous != null && previous != definition)
				throw new IllegalArgumentException("Conflicting player animation definition: " + id);
		});
		this.definitions.forEach((id, definition) ->
		{
			if (PPlayerAnimations.get(id) == null)
				PPlayerAnimations.register(id, definition);
		});
		close();
	}
}
