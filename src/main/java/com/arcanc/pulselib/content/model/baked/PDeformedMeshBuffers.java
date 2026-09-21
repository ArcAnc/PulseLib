/**
 * @author ArcAnc
 * Created at: 08.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.baked;

import com.arcanc.pulselib.content.model.PMeshPrimitive;
import com.arcanc.pulselib.content.model.deformer.PMeshDeformation;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.PResourceCache;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.joml.Vector3f;

import java.util.IdentityHashMap;
import java.util.Map;

public final class PDeformedMeshBuffers
{
	private PDeformedMeshBuffers()
	{
	}

	private static final Map<PBakedMesh, IdentityHashMap<Object, GpuBuffer>> BUFFERS = new IdentityHashMap<>();

	public static GpuBufferSlice resolve(PBakedMesh mesh, PMeshDeformation deformation)
	{
		if (deformation == null || deformation.stack().isEmpty())
			return mesh.vbo().slice();
		IdentityHashMap<Object, GpuBuffer> buffers = BUFFERS.computeIfAbsent(mesh, ignored -> new IdentityHashMap<>());
		GpuBuffer previous = buffers.remove(deformation.cacheKey());
		if (previous != null)
			previous.close();
		GpuBuffer buffer = upload(mesh, deformation);
		buffers.put(deformation.cacheKey(), buffer);
		return buffer.slice();
	}

	public static PBakedMesh resolveMesh(PBakedMesh mesh, PMeshDeformation deformation)
	{
		if (deformation == null || deformation.stack().isEmpty())
			return mesh;
		return new PBakedMesh(mesh.uuid(), resolve(mesh, deformation).buffer(), mesh.vertexesAmount(),
				mesh.indices(), mesh.indicesCount(), mesh.indexType(), mesh.textureReference(), mesh.isEmissive(),
				mesh.alphaMode(), mesh.source(), mesh.textureLocation());
	}

	public static void close(PBakedMesh mesh)
	{
		IdentityHashMap<Object, GpuBuffer> buffers = BUFFERS.remove(mesh);
		if (buffers != null)
			buffers.values().forEach(GpuBuffer::close);
	}

	public static void cleanup()
	{
		BUFFERS.values().forEach(buffers -> buffers.values().forEach(GpuBuffer::close));
		BUFFERS.clear();
	}

	private static GpuBuffer upload(PBakedMesh baked, PMeshDeformation deformation)
	{
		PMeshPrimitive mesh = baked.source();
		TextureAtlasSprite sprite = PResourceCache.getTextureAtlas().getSprite(baked.textureLocation());
		ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(
				mesh.vertexCount() * PRenderTypes.VertexFormatProvider.POSITION_TEX_NORMAL.getVertexSize());
		BufferBuilder builder = sprite.contents().name().getPath().equals("missingno")
				? new BufferBuilder(bytes, PrimitiveTopology.TRIANGLES, PRenderTypes.VertexFormatProvider.POSITION_TEX_NORMAL)
				: new AtlasBufferBuilder(bytes, PrimitiveTopology.TRIANGLES, PRenderTypes.VertexFormatProvider.POSITION_TEX_NORMAL, sprite);
		for (int vertex = 0; vertex < mesh.vertexCount(); vertex++)
		{
			Vector3f position = new Vector3f(mesh.positions().get(vertex * 3), mesh.positions().get(vertex * 3 + 1), mesh.positions().get(vertex * 3 + 2));
			Vector3f normal = deformation.stack().deformNormal(position,
					new Vector3f(mesh.normals().get(vertex * 3), mesh.normals().get(vertex * 3 + 1), mesh.normals().get(vertex * 3 + 2)), deformation.values());
			deformation.stack().deformInPlace(position, deformation.values());
			builder.addVertex(position.x, position.y, position.z)
					.setUv(mesh.uvs().get(vertex * 2), mesh.uvs().get(vertex * 2 + 1))
					.setNormal(normal.x, normal.y, normal.z);
		}
		try (MeshData data = builder.buildOrThrow())
		{
			return RenderSystem.getDevice().createBuffer(
					() -> baked.uuid() + "_deformed",
					GpuBuffer.USAGE_VERTEX,
					data.vertexBuffer());
		}
	}
}
