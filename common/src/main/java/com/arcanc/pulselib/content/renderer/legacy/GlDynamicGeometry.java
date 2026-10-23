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
import com.arcanc.pulselib.util.PRenderTypes;

import java.nio.ByteBuffer;

/** Canonical dynamic geometry facade over the single Minecraft 26.1 GPU backend. */
public final class GlDynamicGeometry implements PDynamicGeometry
{
	private final PGlDynamicGeometry source = new PGlDynamicGeometry(new PGeometryData(
			ByteBuffer.allocate(0), ByteBuffer.allocate(0),
			PRenderTypes.VertexFormatProvider.POSITION_TEX_NORMAL.getVertexSize(), 0, PGeometryData.IndexType.UNSIGNED_INT));

	/** Minecraft 26.1 replaced VertexBuffer; the source accepts immutable geometry uploads. */
	public PGlDynamicGeometry vertexBuffer()
	{
		return this.source;
	}

	public int indexCount()
	{
		return this.source.geometry().indexCount();
	}

	public void close()
	{
		this.source.close();
	}
}
