/**
 * @author ArcAnc
 * Created at: 13.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.plan;

/**
 * Optional CPU-data and lifetime contract for dynamic geometry shared across render backends.
 * The source owns immutable data; registries own their GPU allocations. Update, submit and close
 * on the render thread, and flush pending submissions before changing index count or layout.
 * Existing implementations of the marker PDynamicGeometry remain valid with an explicit adapter.
 */
public interface PGeometrySource extends PDynamicGeometry
{
	/**
	 * Returns the stable, non-delegating source behind a facade. The returned source must return itself.
	 * Registries use this identity to share one allocation and lifetime across aliases.
	 */
	default PGeometrySource geometrySource()
	{
		return this;
	}

	/** Returns the latest immutable data, rejecting access after close. */
	PGeometryData geometry();

	/** Registers/replaces one listener by owner identity, rejecting registration after close. */
	void onClose(Object owner, Runnable listener);

	/** Removes the owner's callback without closing either the source or its data. */
	void removeCloseListener(Object owner);

	/** Closes once and invokes every registered listener, aggregating runtime exceptions. */
	void close();
}
