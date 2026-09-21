/**
 * @author ArcAnc
 * Created at: 14.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.baked;

import com.arcanc.pulselib.content.model.PMeshPrimitive;
import com.arcanc.pulselib.content.model.deformer.PMeshTessellator;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.PResourceCache;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import de.javagl.jgltf.model.GltfConstants;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public final class PSubdividedMeshCache
{
	private static final Map<PBakedMesh, Map<Integer, PBakedMesh>> MESHES = new IdentityHashMap<>();

	private PSubdividedMeshCache()
	{
	}

	public static PBakedMesh resolve(PBakedMesh mesh, int subdivisionLevel)
	{
		if (subdivisionLevel == 0)
			return mesh;
		return MESHES.computeIfAbsent(mesh, ignored -> new HashMap<>()).computeIfAbsent(subdivisionLevel,
				level -> bake(mesh, level));
	}

	public static void close(PBakedMesh mesh)
	{
		Map<Integer, PBakedMesh> variants = MESHES.remove(mesh);
		if (variants != null)
			variants.values().forEach(PSubdividedMeshCache :: closeBuffers);
	}

	public static void cleanup()
	{
		MESHES.values().forEach(variants -> variants.values().forEach(PSubdividedMeshCache :: closeBuffers));
		MESHES.clear();
	}

	private static PBakedMesh bake(PBakedMesh base, int subdivisionLevel)
	{
		PMeshPrimitive source = PMeshTessellator.subdivide(base.source(), subdivisionLevel);
		TextureAtlasSprite sprite = PResourceCache.getTextureAtlas().getSprite(base.textureLocation());
		ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(
				source.vertexCount() * PRenderTypes.VertexFormatProvider.POSITION_TEX_NORMAL.getVertexSize());
		BufferBuilder builder = sprite.contents().name().getPath().equals("missingno")
				? new BufferBuilder(bytes, PrimitiveTopology.TRIANGLES, PRenderTypes.VertexFormatProvider.POSITION_TEX_NORMAL)
				: new AtlasBufferBuilder(bytes, PrimitiveTopology.TRIANGLES, PRenderTypes.VertexFormatProvider.POSITION_TEX_NORMAL, sprite);
		for (int vertex = 0; vertex < source.vertexCount(); vertex++)
			builder.addVertex(source.positions().get(vertex * 3), source.positions().get(vertex * 3 + 1), source.positions().get(vertex * 3 + 2)).
					setUv(source.uvs().get(vertex * 2), source.uvs().get(vertex * 2 + 1)).
					setNormal(source.normals().get(vertex * 3), source.normals().get(vertex * 3 + 1), source.normals().get(vertex * 3 + 2));
		try (MeshData data = builder.buildOrThrow())
		{
			GpuBuffer vertices = RenderSystem.getDevice().createBuffer(
					() -> base.uuid() + "_subdivided_" + subdivisionLevel,
					GpuBuffer.USAGE_VERTEX,
					data.vertexBuffer());
			ByteBuffer indices = source.indices().duplicate();
			indices.clear();
			GpuBuffer indexBuffer = RenderSystem.getDevice().createBuffer(
					() -> base.uuid() + "_subdivided_" + subdivisionLevel + "_indices",
					GpuBuffer.USAGE_INDEX,
					indices);
			return new PBakedMesh(base.uuid(), vertices, source.vertexCount(), indexBuffer, source.indicesCount(),
					indexType(source), base.textureReference(), base.isEmissive(), base.alphaMode(), source, base.textureLocation());
		}
	}

	private static IndexType indexType(PMeshPrimitive mesh)
	{
		return switch (mesh.glIndexType())
		{
			case GltfConstants.GL_UNSIGNED_SHORT -> IndexType.SHORT;
			case GltfConstants.GL_UNSIGNED_INT -> IndexType.INT;
			default -> throw new IllegalArgumentException("Unsupported GLTF index type: " + mesh.glIndexType());
		};
	}

	private static void closeBuffers(PBakedMesh mesh)
	{
		PDeformedMeshBuffers.close(mesh);
		mesh.vbo().close();
		mesh.indices().close();
	}
}
