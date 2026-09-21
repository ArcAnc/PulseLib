/**
 * @author ArcAnc
 * Created at: 13.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.baked;

import com.arcanc.pulselib.content.model.PMeshPrimitive;
import com.arcanc.pulselib.content.model.textures.PTextureAlphaClassifier;
import com.arcanc.pulselib.content.model.textures.atlas.PLibSpriteMetadata;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.PResourceCache;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.PrimitiveTopology;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public final class PMeshTextureVariants
{
	private static final Map<PBakedMesh, Map<Identifier, PBakedMesh>> VARIANTS = new IdentityHashMap<>();

	private PMeshTextureVariants()
	{
	}

	public static PBakedMesh resolve(PBakedMesh mesh, @Nullable Identifier texture)
	{
		if (texture == null)
			return mesh;
		Identifier resolvedTexture = PResourceCache.spriteId(texture);
		if (resolvedTexture.equals(mesh.textureLocation()))
			return mesh;
		return VARIANTS.computeIfAbsent(mesh, ignored -> new HashMap<>()).computeIfAbsent(resolvedTexture,
				location -> bake(mesh, location));
	}

	public static void clear()
	{
		for (Map<Identifier, PBakedMesh> variants : VARIANTS.values())
			for (PBakedMesh variant : variants.values())
			{
				PDeformedMeshBuffers.close(variant);
				PSubdividedMeshCache.close(variant);
				variant.vbo().close();
				variant.indices().close();
			}
		VARIANTS.clear();
	}

	private static PBakedMesh bake(PBakedMesh base, Identifier texture)
	{
		PMeshPrimitive source = base.source();
		TextureAtlasSprite sprite = PResourceCache.getTextureAtlas().getSprite(texture);
		boolean emissive = sprite.contents().getAdditionalMetadata(PLibSpriteMetadata.TYPE).
				map(PLibSpriteMetadata :: emissive).orElse(false);
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
					() -> base.uuid() + "_" + texture + "_vertices", GpuBuffer.USAGE_VERTEX, data.vertexBuffer());
			ByteBuffer indices = source.indices().duplicate();
			indices.clear();
			GpuBuffer indexBuffer = RenderSystem.getDevice().createBuffer(
					() -> base.uuid() + "_" + texture + "_indices", GpuBuffer.USAGE_INDEX, indices);
			return new PBakedMesh(base.uuid(), vertices, source.vertexCount(), indexBuffer, source.indicesCount(),
					base.indexType(), base.textureReference(), emissive, PTextureAlphaClassifier.resolve(sprite.contents()), source, texture);
		}
	}
}
