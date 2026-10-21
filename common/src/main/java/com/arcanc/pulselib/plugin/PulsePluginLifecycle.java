/**
 * @author ArcAnc
 * Created at: 12.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.plugin;

import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Objects;

/**
 * Ordered registration state. Failed one-time phases require a restart; models can retry.
 */
public final class PulsePluginLifecycle
{
	/**
	 * Ordered common and client plugin registration phases.
	 */
	public enum Phase
	{
		/**
		 * Common animation channel registration phase.
		 */
		CHANNELS,
		/**
		 * Common animation event registration phase.
		 */
		EVENTS,
		/**
		 * Common mesh deformer registration phase.
		 */
		DEFORMERS,
		/**
		 * Client living attachment registration phase.
		 */
		ATTACHMENTS,
		/**
		 * Client player animation definition registration phase.
		 */
		PLAYER_ANIMATIONS,
		/**
		 * Client animated attachment renderer registration phase.
		 */
		ANIMATED_ATTACHMENTS,
		/**
		 * Reloadable client model resource registration phase.
		 */
		MODELS
	}

	/**
	 * Execution state of one plugin registration phase.
	 */
	public enum State
	{
		/**
		 * The registration phase has not started.
		 */
		PENDING,
		/**
		 * Registration callbacks for the phase are running.
		 */
		ACTIVE,
		/**
		 * The phase completed successfully.
		 */
		COMPLETE,
		/**
		 * The phase failed and subsequent lifecycle checks retain its failure.
		 */
		FAILED
	}

	private final EnumMap<Phase, State> states = new EnumMap<>(Phase.class);
	private final EnumMap<Phase, Throwable> failures = new EnumMap<>(Phase.class);
	private @Nullable Phase active;

	/**
	 * Returns the current state of the requested registration phase.
	 *
	 * @param phase the normalized animation cycle phase
	 * @return the current state of the requested registration phase
	 */
	public State state(Phase phase)
	{
		return this.states.getOrDefault(Objects.requireNonNull(phase), State.PENDING);
	}

	/**
	 * Starts a registration phase when permitted and reports whether its callbacks should run.
	 *
	 * @param phase the normalized animation cycle phase
	 * @return whether its callbacks should run
	 */
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

	/**
	 * Marks the active registration phase as successfully completed.
	 *
	 * @param phase the normalized animation cycle phase
	 */
	public void complete(Phase phase)
	{
		requireActive(phase);
		this.states.put(phase, State.COMPLETE);
		this.failures.remove(phase);
		this.active = null;
	}

	/**
	 * Marks the active phase as failed and retains the failure for subsequent lifecycle checks.
	 *
	 * @param phase the normalized animation cycle phase
	 * @param failure the failure that prevents the registration phase from completing
	 */
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
