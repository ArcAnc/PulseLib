package com.arcanc.pulselib.plugin;

import java.util.EnumMap;
import java.util.Objects;

/** Ordered registration state. Failed one-time phases require a restart; models can retry. */
public final class PulsePluginLifecycle
{
	public enum Phase { CHANNELS, EVENTS, DEFORMERS, ATTACHMENTS, PLAYER_ANIMATIONS, ANIMATED_ATTACHMENTS, MODELS }
	public enum State { PENDING, ACTIVE, COMPLETE, FAILED }

	private final EnumMap<Phase, State> states = new EnumMap<>(Phase.class);
	private final EnumMap<Phase, Throwable> failures = new EnumMap<>(Phase.class);
	private Phase active;

	public State state(Phase phase)
	{
		return this.states.getOrDefault(Objects.requireNonNull(phase), State.PENDING);
	}

	public boolean begin(Phase phase)
	{
		Objects.requireNonNull(phase);
		if (this.active != null)
			throw new IllegalStateException("PulseLib phase " + this.active + " is already active");
		if (phase != Phase.MODELS && state(phase) == State.COMPLETE)
			return false;
		for (Phase previous : Phase.values())
			if (previous != Phase.MODELS && state(previous) == State.FAILED)
				throw new IllegalStateException("PulseLib phase " + previous + " failed; restart is required", this.failures.get(previous));
		if (phase.ordinal() > 0 && state(Phase.values()[phase.ordinal() - 1]) != State.COMPLETE)
			throw new IllegalStateException("PulseLib phase out of order: " + phase);
		this.active = phase;
		this.states.put(phase, State.ACTIVE);
		return true;
	}

	public void complete(Phase phase)
	{
		requireActive(phase);
		this.states.put(phase, State.COMPLETE);
		this.failures.remove(phase);
		this.active = null;
	}

	public void fail(Phase phase, Throwable failure)
	{
		requireActive(phase);
		this.failures.put(phase, Objects.requireNonNull(failure));
		this.states.put(phase, State.FAILED);
		this.active = null;
	}

	private void requireActive(Phase phase)
	{
		if (this.active != phase)
			throw new IllegalStateException("PulseLib phase is not active: " + phase);
	}
}
