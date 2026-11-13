/**
 * @author ArcAnc
 * Created at: 13.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.rhi;

import com.arcanc.pulselib.content.renderer.plan.PDynamicGeometry;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.arcanc.pulselib.content.renderer.plan.PGeometrySource;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

/** Mutable CPU source; GPU allocation belongs exclusively to its registering RHI registry. */
@ApiStatus.Internal
public final class PRhiDynamicGeometry implements PDynamicGeometry, PGeometrySource, AutoCloseable
{
	private final Thread owner = Thread.currentThread();
	private final Map<Object, Runnable> listeners = new IdentityHashMap<>();
	private PGeometryData geometry;
	private boolean closed;

	public PRhiDynamicGeometry(PGeometryData geometry)
	{
		this.geometry = Objects.requireNonNull(geometry);
	}

	@Override
	public PGeometryData geometry()
	{
		ensureOpen();
		return this.geometry;
	}

	/** Pending plans use the latest source; flush before changing layout/index counts. */
	public void update(PGeometryData geometry)
	{
		ensureOpen();
		this.geometry = Objects.requireNonNull(geometry);
	}

	@Override
	public void onClose(Object owner, Runnable listener)
	{
		ensureOpen();
		this.listeners.put(Objects.requireNonNull(owner), Objects.requireNonNull(listener));
	}

	@Override
	public void removeCloseListener(Object owner)
	{
		this.listeners.remove(owner);
	}

	@Override
	public void close()
	{
		checkThread();
		if (this.closed)
			return;
		this.closed = true;
		var callbacks = new ArrayList<>(this.listeners.values());
		this.listeners.clear();
		RuntimeException failure = null;
		for (Runnable callback : callbacks)
			try
			{
				callback.run();
			}
			catch (RuntimeException exception)
			{
				if (failure == null)
					failure = exception;
				else
					failure.addSuppressed(exception);
			}
		if (failure != null)
			throw failure;
	}

	private void ensureOpen()
	{
		checkThread();
		if (this.closed)
			throw new IllegalStateException("RHI dynamic geometry is closed");
	}

	private void checkThread()
	{
		if (Thread.currentThread() != this.owner)
			throw new IllegalStateException("RHI dynamic geometry must be accessed on its owning render thread");
	}
}
