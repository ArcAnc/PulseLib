/**
 * @author ArcAnc
 * Created at: 22.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.gl;

import com.arcanc.pulselib.content.renderer.plan.PDynamicGeometry;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Minecraft 26.1 dynamic geometry source. Reuse the source and replace its immutable data.
 * The queue owns GPU allocations and releases them on reload/disconnect/shutdown.
 * Update and submit on the render thread; pending submissions use the latest uploaded data.
 * Flush pending submissions before changing the index count or layout.
 */
public final class PGlDynamicGeometry implements PDynamicGeometry, AutoCloseable
{
	private PGeometryData geometry;
	private final Map<Object, Runnable> closeListeners = new IdentityHashMap<>();
	private boolean closed;

	public PGlDynamicGeometry(PGeometryData geometry)
	{
		this.geometry = Objects.requireNonNull(geometry);
	}

	public PGeometryData geometry()
	{
		ensureOpen();
		return this.geometry;
	}

	/** Changes CPU source data; the executor uploads into its reclaimed frame slot. */
	public void update(PGeometryData geometry)
	{
		ensureOpen();
		this.geometry = Objects.requireNonNull(geometry);
	}

	void onClose(Object owner, Runnable listener)
	{
		ensureOpen();
		this.closeListeners.put(owner, listener);
	}

	void removeCloseListener(Object owner)
	{
		this.closeListeners.remove(owner);
	}

	/** Releases queue registrations and GPU pages owned by this source on the render thread. */
	@Override
	public void close()
	{
		if (this.closed)
			return;
		this.closed = true;
		var listeners = new ArrayList<>(this.closeListeners.values());
		this.closeListeners.clear();
		listeners.forEach(Runnable::run);
	}

	private void ensureOpen()
	{
		if (this.closed)
			throw new IllegalStateException("Dynamic geometry is closed");
	}
}
