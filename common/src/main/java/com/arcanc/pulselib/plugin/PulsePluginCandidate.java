package com.arcanc.pulselib.plugin;

import com.arcanc.pulselib.api.plugin.PulsePlugin;

import java.util.Objects;

/** Discovery metadata, obtained without loading the plugin class. */
public record PulsePluginCandidate(String className, String modId, PulsePlugin.Side side)
{
	public PulsePluginCandidate
	{
		Objects.requireNonNull(className);
		Objects.requireNonNull(modId);
		Objects.requireNonNull(side);
	}
}
