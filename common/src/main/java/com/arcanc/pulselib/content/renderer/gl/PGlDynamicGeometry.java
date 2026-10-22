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

import java.util.Objects;

/**
 * Minecraft 26.1 dynamic geometry source. Reuse the source and replace its immutable data.
 * The queue owns GPU allocations and releases them on reload/disconnect/shutdown.
 * Update and submit on the render thread; pending submissions use the latest uploaded data.
 * Flush pending submissions before changing the index count or layout.
 */
public final class PGlDynamicGeometry implements PDynamicGeometry
{
	private PGeometryData geometry;

	public PGlDynamicGeometry(PGeometryData geometry)
	{
		this.geometry = Objects.requireNonNull(geometry);
	}

	public PGeometryData geometry()
	{
		return this.geometry;
	}

	/** Changes CPU source data; the executor uploads into its reclaimed frame slot. */
	public void update(PGeometryData geometry)
	{
		this.geometry = Objects.requireNonNull(geometry);
	}
}
