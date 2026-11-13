/**
 * @author ArcAnc
 * Created at: 13.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.legacy;

import com.arcanc.pulselib.content.renderer.gl.PGlDynamicGeometry;
import com.arcanc.pulselib.content.renderer.plan.PDynamicGeometry;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.arcanc.pulselib.content.renderer.plan.PGeometrySource;

import java.nio.ByteBuffer;

/** Canonical CPU geometry facade; GPU allocation belongs to the selected render backend. */
public final class GlDynamicGeometry implements PDynamicGeometry, PGeometrySource
{
	// Preserve the original empty layout: xyz (12), uv (8), packed normal (3), padding (1).
	private static final int EMPTY_VERTEX_STRIDE = 24;
	private final PGlDynamicGeometry source = new PGlDynamicGeometry(new PGeometryData(
			ByteBuffer.allocate(0), ByteBuffer.allocate(0),
			EMPTY_VERTEX_STRIDE, 0, PGeometryData.IndexType.UNSIGNED_INT));

	/** Compatibility accessor retaining the canonical CPU source and return type on every GPU backend. */
	public PGlDynamicGeometry vertexBuffer()
	{
		return this.source;
	}

	public int indexCount()
	{
		return this.source.geometry().indexCount();
	}

	@Override
	public PGeometrySource geometrySource()
	{
		return this.source;
	}

	@Override
	public PGeometryData geometry()
	{
		return this.source.geometry();
	}

	@Override
	public void onClose(Object owner, Runnable listener)
	{
		this.source.onClose(owner, listener);
	}

	@Override
	public void removeCloseListener(Object owner)
	{
		this.source.removeCloseListener(owner);
	}

	@Override
	public void close()
	{
		this.source.close();
	}
}
