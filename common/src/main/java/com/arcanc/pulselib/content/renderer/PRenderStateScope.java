/**
 * @author ArcAnc
 * Created at: 23.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer;

import org.jspecify.annotations.Nullable;

/**
 * Binds canonical owner callbacks to the current Minecraft render-state invocation.
 * Render-thread confined, per renderer, reentrant and exception-safe. This does not own a render target or queue.
 */
final class PRenderStateScope<S>
{
	private @Nullable S current;

	@Nullable S current()
	{
		return this.current;
	}

	void run(S state, Runnable action)
	{
		S previous = this.current;
		this.current = state;
		try
		{
			action.run();
		}
		finally
		{
			this.current = previous;
		}
	}
}
