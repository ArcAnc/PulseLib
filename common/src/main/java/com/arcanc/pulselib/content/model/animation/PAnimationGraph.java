/**
 * @author ArcAnc
 * Created at: 05.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;

import java.util.List;
import java.util.Objects;

public record PAnimationGraph(List<PAnimationState> states, List<PAnimationTransition> transitions)
{
	public PAnimationGraph
	{
		states = List.copyOf(states);
		transitions = List.copyOf(transitions);
		if (states.isEmpty())
			throw new IllegalArgumentException("An animation graph needs at least one state");
		for (PAnimationState state : states)
			Objects.requireNonNull(state);
		for (PAnimationTransition transition : transitions)
		{
			Objects.requireNonNull(transition);
			if (transition.source() >= states.size() || transition.target() >= states.size())
				throw new IllegalArgumentException("Animation transition references a missing state");
			if (states.get(transition.source()).isOverlay() || states.get(transition.target()).isOverlay())
				throw new IllegalArgumentException("One-shot overlays cannot be transition endpoints");
		}
		if (states.stream().allMatch(PAnimationState::isOverlay))
			throw new IllegalArgumentException("An animation graph needs one non-overlay state");
	}

	public int initialState()
	{
		for (int index = 0; index < this.states.size(); index++)
			if (!this.states.get(index).isOverlay())
				return index;
		throw new IllegalStateException("Graph has no state-machine state");
	}
}
