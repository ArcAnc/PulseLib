/**
 * @author ArcAnc
 * Created at: 27.01.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.baked;

import com.arcanc.pulselib.content.model.PMeshPrimitive;
import com.arcanc.pulselib.content.model.deformer.PMeshTessellator;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.PResourceCache;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public final class PGpuDeformedMeshBuffers
{

	private static final Map<PBakedMesh, Map<Integer, PGeometryData>> GEOMETRIES = new IdentityHashMap<>();

	private PGpuDeformedMeshBuffers()
	{
	}

	public static PGeometryData resolve(PBakedMesh mesh, int subdivisionLevel)
	{
		if (subdivisionLevel < 0)
			throw new IllegalArgumentException("Subdivision level cannot be negative");
		if (subdivisionLevel == 0)
			return mesh.geometry();
		return GEOMETRIES.computeIfAbsent(mesh, ignored -> new HashMap<>()).computeIfAbsent(
				subdivisionLevel, level -> bake(mesh, PMeshTessellator.subdivide(mesh.source(), level)));
	}

	public static void close(PBakedMesh mesh)
	{
		GEOMETRIES.remove(mesh);
	}

	public static void cleanup()
	{
		GEOMETRIES.clear();
	}

	private static PGeometryData bake(PBakedMesh baked, PMeshPrimitive mesh)
	{
		TextureAtlasSprite sprite = PResourceCache.getTextureAtlas().getSprite(baked.textureLocation());
		ByteBufferBuilder bytes = new ByteBufferBuilder(mesh.vertexCount() * PRenderTypes.VertexFormatProvider.POSITION_TEX_NORMAL.getVertexSize());
		BufferBuilder builder = sprite.contents().name().getPath().equals("missingno") ?
				new BufferBuilder(bytes, VertexFormat.Mode.TRIANGLES, PRenderTypes.VertexFormatProvider.POSITION_TEX_NORMAL) :
				new AtlasBufferBuilder(bytes, VertexFormat.Mode.TRIANGLES, PRenderTypes.VertexFormatProvider.POSITION_TEX_NORMAL, sprite);
		for (int vertex = 0; vertex < mesh.vertexCount(); vertex++)
			builder.addVertex(mesh.positions().get(vertex * 3), mesh.positions().get(vertex * 3 + 1), mesh.positions().get(vertex * 3 + 2)).
					setUv(mesh.uvs().get(vertex * 2), mesh.uvs().get(vertex * 2 + 1)).
					setNormal(mesh.normals().get(vertex * 3), mesh.normals().get(vertex * 3 + 1), mesh.normals().get(vertex * 3 + 2));
		try (MeshData data = builder.buildOrThrow())
		{
			ByteBuffer indices = mesh.indices().duplicate();
			indices.clear();
			return new PGeometryData(data.vertexBuffer(), indices,
					PRenderTypes.VertexFormatProvider.POSITION_TEX_NORMAL.getVertexSize(), mesh.indicesCount(),
					mesh.glIndexType() == VertexFormat.IndexType.SHORT.asGLType ?
							PGeometryData.IndexType.UNSIGNED_SHORT : PGeometryData.IndexType.UNSIGNED_INT);
		}
	}
}
