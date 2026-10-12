package com.arcanc.pulselib.plugin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PulsePluginLifecycleTest
{
	@Test
	void enforcesOrderAndDoesNotRepeatOneTimePhases()
	{
		var lifecycle = new PulsePluginLifecycle();
		assertThrows(IllegalStateException.class, () -> lifecycle.begin(PulsePluginLifecycle.Phase.ATTACHMENTS));
		for (var phase : PulsePluginLifecycle.Phase.values())
		{
			assertTrue(lifecycle.begin(phase));
			assertThrows(IllegalStateException.class, () -> lifecycle.begin(phase));
			lifecycle.complete(phase);
		}
		assertFalse(lifecycle.begin(PulsePluginLifecycle.Phase.ATTACHMENTS));
		assertTrue(lifecycle.begin(PulsePluginLifecycle.Phase.MODELS));
		lifecycle.complete(PulsePluginLifecycle.Phase.MODELS);
	}
}
