package com.arcanc.pulselib.api.registration;

/** Registrars are valid only during their plugin callback. */
public abstract class PulseRegistrar implements AutoCloseable
{
	private boolean open = true;

	protected final void checkOpen()
	{
		if (!this.open)
			throw new IllegalStateException("PulseLib registration phase has already ended");
	}

	@Override
	public final void close()
	{
		this.open = false;
	}
}
