/**
 * @author ArcAnc
 * Created at: 14.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.animatable;

import java.util.concurrent.atomic.AtomicLong;

/** Session-wide counter only: it retains no stacks, managers or controllers. */
public final class PItemStackAnimationIds
{
	private static final AtomicLong NEXT_ID = new AtomicLong();

	private PItemStackAnimationIds()
	{
	}

	public static long nextId()
	{
		return NEXT_ID.updateAndGet(previous ->
		{
			if (previous == Long.MAX_VALUE)
			{
				throw new IllegalStateException("Item animation runtime IDs exhausted");
			}
			return previous + 1;
		});
	}
}
