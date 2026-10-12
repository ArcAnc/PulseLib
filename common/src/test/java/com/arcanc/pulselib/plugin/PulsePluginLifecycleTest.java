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

	@Test
	void failedOneTimePhaseClearsActiveAndBlocksRetryAndLaterPhases()
	{
		var lifecycle = new PulsePluginLifecycle();
		var cause = new IllegalArgumentException("Failed channel registration");
		lifecycle.begin(PulsePluginLifecycle.Phase.CHANNELS);
		lifecycle.fail(PulsePluginLifecycle.Phase.CHANNELS, cause);
		assertEquals(PulsePluginLifecycle.State.FAILED, lifecycle.state(PulsePluginLifecycle.Phase.CHANNELS));
		var repeat = assertThrows(IllegalStateException.class, () -> lifecycle.begin(PulsePluginLifecycle.Phase.CHANNELS));
		assertSame(cause, repeat.getCause());
		assertFalse(repeat.getMessage().contains("already active"));
		assertThrows(IllegalStateException.class, () -> lifecycle.begin(PulsePluginLifecycle.Phase.EVENTS));
		assertThrows(IllegalStateException.class, () -> lifecycle.complete(PulsePluginLifecycle.Phase.CHANNELS));
	}

	@Test
	void modelsCanRecoverFromFailureWithoutRepeatingCompletedPhases()
	{
		var lifecycle = new PulsePluginLifecycle();
		for (var phase : PulsePluginLifecycle.Phase.values())
		{
			lifecycle.begin(phase);
			if (phase == PulsePluginLifecycle.Phase.MODELS)
				lifecycle.fail(phase, new IllegalStateException("Reload failed"));
			else
				lifecycle.complete(phase);
		}
		assertFalse(lifecycle.begin(PulsePluginLifecycle.Phase.ATTACHMENTS));
		assertEquals(PulsePluginLifecycle.State.FAILED, lifecycle.state(PulsePluginLifecycle.Phase.MODELS));
		assertTrue(lifecycle.begin(PulsePluginLifecycle.Phase.MODELS));
		lifecycle.complete(PulsePluginLifecycle.Phase.MODELS);
		assertEquals(PulsePluginLifecycle.State.COMPLETE, lifecycle.state(PulsePluginLifecycle.Phase.MODELS));
	}

}
