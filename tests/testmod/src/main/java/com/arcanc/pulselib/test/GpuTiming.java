/**
 * @author ArcAnc
 * Created at: 13.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.ARBTimerQuery;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Test-only GPU timestamps. Nested passes use timestamp pairs, not nested
 * GL_TIME_ELAPSED queries. Results are read only after availability is reported.
 */
public final class GpuTiming
{
	private static final Deque<Span> active = new ArrayDeque<>();
	private static final List<Span> spans = new ArrayList<>();
	private static final Deque<Sample> pending = new ArrayDeque<>();
	private static boolean recording;
	private static int fullscreen;
	private static int allocations;
	private static int bufferReallocations;

	private GpuTiming()
	{
	}

	public static void start()
	{
		SmokeHarness.check(!recording, "GPU sample already active");
		recording = true;
		fullscreen = 0;
		allocations = 0;
		bufferReallocations = 0;
		begin("frame");
	}

	public static void begin(String name)
	{
		if (!recording)
		{
			return;
		}
		Span span = new Span(name, GL15.glGenQueries(), GL15.glGenQueries());
		ARBTimerQuery.glQueryCounter(span.start(), ARBTimerQuery.GL_TIMESTAMP);
		active.push(span);
	}

	public static void end()
	{
		if (!recording)
		{
			return;
		}
		Span span = active.pop();
		ARBTimerQuery.glQueryCounter(span.end(), ARBTimerQuery.GL_TIMESTAMP);
		spans.add(span);
	}

	public static void fullscreen()
	{
		if (recording)
		{
			fullscreen++;
		}
	}

	public static void allocation()
	{
		if (recording)
		{
			allocations++;
		}
	}

	public static void bufferReallocation()
	{
		if (recording)
		{
			bufferReallocations++;
		}
	}

	public static void finish(boolean measured)
	{
		end();
		SmokeHarness.check(active.isEmpty(), "Unbalanced GPU timer spans");
		pending.add(new Sample(List.copyOf(spans), fullscreen, allocations, bufferReallocations, measured));
		spans.clear();
		recording = false;
	}

	static boolean drain(List<Result> results)
	{
		while (!pending.isEmpty())
		{
			Sample sample = pending.peek();
			int lastQuery = sample.spans().getLast().end();
			if (GL15.glGetQueryObjecti(lastQuery, GL15.GL_QUERY_RESULT_AVAILABLE) == 0)
			{
				return false;
			}
			Map<String, Double> timings = new LinkedHashMap<>();
			for (Span span : sample.spans())
			{
				long start = ARBTimerQuery.glGetQueryObjectui64(span.start(), GL15.GL_QUERY_RESULT);
				long end = ARBTimerQuery.glGetQueryObjectui64(span.end(), GL15.GL_QUERY_RESULT);
				SmokeHarness.check(end >= start, "GPU timestamps must be monotonic");
				timings.merge(span.name(), (end - start) / 1_000_000.0, Double::sum);
				GL15.glDeleteQueries(span.start());
				GL15.glDeleteQueries(span.end());
			}
			if (sample.measured())
			{
				results.add(new Result(Map.copyOf(timings), sample.fullscreen(), sample.allocations(), sample.bufferReallocations()));
			}
			pending.remove();
		}
		return true;
	}

	private record Span(String name, int start, int end)
	{
	}

	private record Sample(List<Span> spans, int fullscreen, int allocations, int bufferReallocations, boolean measured)
	{
	}

	record Result(Map<String, Double> milliseconds, int fullscreen, int allocations, int bufferReallocations)
	{
	}
}
