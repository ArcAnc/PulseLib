package com.arcanc.pulselib.plugin;

import java.util.EnumSet;

public final class PulsePluginLifecycle
{
	public enum Phase { CHANNELS, EVENTS, DEFORMERS, ATTACHMENTS, PLAYER_ANIMATIONS, ANIMATED_ATTACHMENTS, MODELS }

	private final EnumSet<Phase> completed = EnumSet.noneOf(Phase.class);
	private Phase active;

	public boolean begin(Phase phase)
	{
		if (this.active != null)
			throw new IllegalStateException("PulseLib phase " + this.active + " is already active");
		if (phase != Phase.MODELS && this.completed.contains(phase))
			return false;
		if (phase.ordinal() > 0 && !this.completed.contains(Phase.values()[phase.ordinal() - 1]))
			throw new IllegalStateException("PulseLib phase out of order: " + phase);
		this.active = phase;
		return true;
	}

	public void complete(Phase phase)
	{
		if (this.active != phase)
			throw new IllegalStateException("PulseLib phase is not active: " + phase);
		this.completed.add(phase);
		this.active = null;
	}
}
