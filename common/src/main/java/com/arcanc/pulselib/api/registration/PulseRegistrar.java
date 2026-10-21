/**
 * @author ArcAnc
 * Created at: 12.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.api.registration;

/**
 * Registrars are valid only during their plugin callback.
 */
public abstract class PulseRegistrar implements AutoCloseable
{
	private boolean open = true;

	/**
	 * Rejects registration after this callback scope has been closed.
	 */
	protected final void checkOpen()
	{
		if (!this.open)
			throw new IllegalStateException("PulseLib registration phase has already ended");
	}

	/**
	 * Closes this registration scope so it can no longer accept declarations.
	 */
	@Override
	public final void close()
	{
		this.open = false;
	}
}
