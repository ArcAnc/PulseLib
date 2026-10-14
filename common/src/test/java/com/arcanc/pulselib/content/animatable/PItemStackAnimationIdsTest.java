/**
 * @author ArcAnc
 * Created at: 14.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.animatable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Timeout(10)
class PItemStackAnimationIdsTest
{
	@Test
	void exhaustionDoesNotWrapOrReuseIds() throws ReflectiveOperationException
	{
		var counterField = PItemStackAnimationIds.class.getDeclaredField("NEXT_ID");
		counterField.setAccessible(true);
		AtomicLong counter = (AtomicLong)counterField.get(null);
		long saved = counter.get();
		try
		{
			counter.set(Long.MAX_VALUE - 1);
			assertEquals(Long.MAX_VALUE, PItemStackAnimationIds.nextId());
			assertThrows(IllegalStateException.class, PItemStackAnimationIds::nextId);
			assertThrows(IllegalStateException.class, PItemStackAnimationIds::nextId);
			assertEquals(Long.MAX_VALUE, counter.get());
		}
		finally
		{
			counter.set(saved);
		}
	}

	@Test
	void idsArePositiveMonotonicAndNeverZero()
	{
		long previous = PItemStackAnimationIds.nextId();
		for (int index = 0; index < 100_000; index++)
		{
			long next = PItemStackAnimationIds.nextId();
			assertTrue(next > previous);
			assertTrue((Long.MIN_VALUE | next) < 0);
			previous = next;
		}
	}

	@Test
	void simultaneousAllocationsRemainUnique() throws Exception
	{
		try (var executor = Executors.newFixedThreadPool(8))
		{
			var tasks = IntStream.range(0, 8).mapToObj(thread -> (Callable<Set<Long>>)() ->
			{
				Set<Long> ids = new HashSet<>();
				for (int index = 0; index < 12_500; index++)
				{
					assertTrue(ids.add(PItemStackAnimationIds.nextId()));
				}
				return ids;
			}).toList();
			Set<Long> all = new HashSet<>();
			for (var result : executor.invokeAll(tasks))
			{
				for (long id : result.get())
				{
					assertTrue(all.add(id));
					assertTrue(id > 0);
				}
			}
			assertEquals(100_000, all.size());
		}
	}
}
