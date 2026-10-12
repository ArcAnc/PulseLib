/**
 * @author ArcAnc
 * Created at: 13.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.legacy;

import com.arcanc.pulselib.content.mixin.VertexBufferAccessor;
import com.arcanc.pulselib.content.renderer.plan.PDynamicGeometry;
import com.mojang.blaze3d.vertex.VertexBuffer;

public final class GlDynamicGeometry implements PDynamicGeometry
{
	private final VertexBuffer vertexBuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);

	public VertexBuffer vertexBuffer()
	{
		return this.vertexBuffer;
	}

	public int indexCount()
	{
		return ((VertexBufferAccessor)this.vertexBuffer).pulselib$getIndexCount();
	}

	public void close()
	{
		this.vertexBuffer.close();
	}
}
