/**
 * @author ArcAnc
 * Created at: 13.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Visits cycle windows using integer indices rather than rounded float cursors.
 */
final class PCyclicAnimationEvents
{
	// A pathological tick must fail before executing a million client callbacks.
	// This is a diagnostic, never a truncation of the event stream.
	private static final int MAX_EVENTS_PER_DISPATCH = 1 << 20;
	private static final double MAX_EXACT_CYCLE = 0x1.0p53;

	private PCyclicAnimationEvents()
	{
	}

	static void visit(PAnimation animation, double from, double to, boolean includeReverseStart, Consumer<PAnimationEvent<?>> consumer)
	{
		if (!Double.isFinite(from) || !Double.isFinite(to) || !Float.isFinite(animation.length()))
		{
			throw new IllegalArgumentException("Cyclic animation times and length must be finite: " + animation.name());
		}
		double length = animation.length();
		if (length <= 0.0 || from == to || animation.events().isEmpty())
		{
			return;
		}
		boolean forward = to > from;
		Comparator<PAnimationEvent<?>> order = Comparator.comparingDouble(PAnimationEvent::time);
		List<PAnimationEvent<?>> events = animation.events().stream().
				filter(event -> event.time() <= length).
				sorted(forward ? order : order.reversed()).
				toList();
		if (events.isEmpty())
		{
			return;
		}
		Position start = position(animation, from, length);
		Position end = position(animation, to, length);
		long first = !forward && start.local() == 0.0 ? start.cycle() - 1L : start.cycle();
		long last = forward && end.local() == 0.0 ? end.cycle() - 1L : end.cycle();
		long step = forward ? 1L : -1L;
		long windows = (last - first) * step + 1L;
		double firstFrom = !forward && start.local() == 0.0 ? length : start.local();
		double lastTo = forward && end.local() == 0.0 ? length : end.local();
		if (windows < 1L)
		{
			throw invalidRange(animation, from, to, "cycle windows do not advance");
		}
		long count = count(events, window(firstFrom, windows == 1L ? lastTo : forward ? length : 0.0,
				length, forward, includeReverseStart));
		if (windows > 1L)
		{
			count += count(events, window(forward ? 0.0 : length, lastTo, length, forward, true));
		}

		double total = count + Math.max(0L, windows - 2L) * (double)events.size();
		if (total > MAX_EVENTS_PER_DISPATCH)
		{
			throw invalidRange(animation, from, to, "would dispatch " + total +
					" events; maximum per dispatch is " + MAX_EVENTS_PER_DISPATCH);
		}
		for (long cycle = first; ; cycle += step)
		{
			Window window = window(cycle == first ? firstFrom : forward ? 0.0 : length,
					cycle == last ? lastTo : forward ? length : 0.0, length, forward, cycle != first || includeReverseStart);
			for (PAnimationEvent<?> event : events)
			{
				if (window.contains(event.time()))
				{
					consumer.accept(event);
				}
			}
			if (cycle == last)
			{
				break;
			}
		}
	}

	/**
	 * FMA retains the local residual without first rounding a huge absolute
	 * boundary. Division can round onto an adjacent integer; correct that once
	 * using the residual, rather than stepping a floating-point cursor.
	 */
	private static Position position(PAnimation animation, double time, double length)
	{
		double candidate = Math.floor(time / length);
		if (!Double.isFinite(candidate) || Math.abs(candidate) > MAX_EXACT_CYCLE)
		{
			throw invalidRange(animation, time, time, "cycle index cannot be represented exactly");
		}
		long cycle = (long)candidate;
		double local = Math.fma(-cycle, length, time);
		if (local < 0.0)
		{
			cycle--;
			local = Math.fma(-cycle, length, time);
		}
		else if (local >= length)
		{
			cycle++;
			local = Math.fma(-cycle, length, time);
		}
		if (Math.abs(cycle) >= MAX_EXACT_CYCLE || !(local >= 0.0 && local < length))
		{
			throw invalidRange(animation, time, time, "cycle phase cannot be represented precisely");
		}
		return new Position(cycle, local);
	}

	private static Window window(double from, double to, double length, boolean forward, boolean includeReverseStart)
	{
		if (forward ? to <= from : to >= from)
		{
			throw new IllegalArgumentException("Cyclic animation window does not advance: from=" + from + ", to=" + to);
		}
		return new Window(from, to, length, forward, includeReverseStart);
	}

	private record Position(long cycle, double local)
	{
	}

	private static long count(List<PAnimationEvent<?>> events, Window window)
	{
		return events.stream().filter(event -> window.contains(event.time())).count();
	}

	private static IllegalArgumentException invalidRange(PAnimation animation, double from, double to, String reason)
	{
		return new IllegalArgumentException("Cannot dispatch cyclic events for " + animation.name() +
				" (length=" + animation.length() + ", from=" + from + ", to=" + to + "): " + reason);
	}

	/**
	 * Forward includes zero on departure and length on arrival; reverse includes
	 * length on departure and zero on arrival. A seam's two distinct markers
	 * therefore fire once each, in playback order, even across adjacent ticks.
	 */
	private record Window(double from, double to, double length, boolean forward, boolean includeReverseStart)
	{
		private boolean contains(double time)
		{
			return this.forward ? (time > this.from || this.from == 0.0 && time == 0.0) && time <= this.to :
					time >= this.to && (time < this.from || this.includeReverseStart && this.from == this.length && time == this.length);
		}
	}
}
