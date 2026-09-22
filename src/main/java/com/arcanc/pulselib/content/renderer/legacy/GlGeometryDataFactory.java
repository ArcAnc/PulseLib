/**
 * @author ArcAnc
 * Created at: 13.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.legacy;

import com.arcanc.pulselib.content.model.PMeshPrimitive;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;

import java.nio.ByteBuffer;

public final class GlGeometryDataFactory
{
	private GlGeometryDataFactory()
	{
	}

	public static PGeometryData capture(MeshData meshData, PMeshPrimitive mesh, int vertexStride)
	{
		ByteBuffer vertices = meshData.vertexBuffer().duplicate();
		ByteBuffer indices = mesh.indices().duplicate();
		indices.clear();
		return new PGeometryData(vertices, indices, vertexStride, mesh.indicesCount(),
				mesh.glIndexType() == VertexFormat.IndexType.SHORT.asGLType ?
						PGeometryData.IndexType.UNSIGNED_SHORT : PGeometryData.IndexType.UNSIGNED_INT);
	}
}
