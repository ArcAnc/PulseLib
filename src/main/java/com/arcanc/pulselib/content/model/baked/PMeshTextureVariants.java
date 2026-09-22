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
import com.arcanc.pulselib.content.renderer.legacy.GlGeometryDataFactory;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.arcanc.pulselib.content.model.textures.atlas.PLibMetadata;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.PResourceCache;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public final class PMeshTextureVariants
{
	private static final Map<PBakedMesh, Map<ResourceLocation, PBakedMesh>> VARIANTS = new IdentityHashMap<>();

	private PMeshTextureVariants()
	{
	}

	public static PBakedMesh resolve(PBakedMesh mesh, @Nullable ResourceLocation texture)
	{
		if (texture == null)
			return mesh;
		ResourceLocation sprite = PResourceCache.spriteId(texture);
		if (sprite.equals(mesh.textureLocation()))
			return mesh;
		return VARIANTS.computeIfAbsent(mesh, ignored -> new HashMap<>()).computeIfAbsent(sprite,
				location -> bake(mesh, location));
	}

	@ApiStatus.Internal
	public static void clear()
	{
		for (Map<ResourceLocation, PBakedMesh> variants : VARIANTS.values())
			for (PBakedMesh variant : variants.values())
			{
				PDeformedMeshBuffers.close(variant);
				PGpuDeformedMeshBuffers.close(variant);
			}
		VARIANTS.clear();
	}

	private static PBakedMesh bake(PBakedMesh base, ResourceLocation texture)
	{
		PMeshPrimitive source = base.source();
		TextureAtlasSprite sprite = PResourceCache.getTextureAtlas().getSprite(texture);
		boolean emissive = sprite.contents().metadata().getSection(PLibMetadata.TYPE).
				map(PLibMetadata :: isEmissive).orElse(false);
		ByteBufferBuilder bytes = new ByteBufferBuilder(source.vertexCount() * PRenderTypes.VertexFormatProvider.POSITION_TEX_NORMAL.getVertexSize());
		BufferBuilder builder = sprite.contents().name().getPath().equals("missingno") ?
				new BufferBuilder(bytes, VertexFormat.Mode.TRIANGLES, PRenderTypes.VertexFormatProvider.POSITION_TEX_NORMAL) :
				new AtlasBufferBuilder(bytes, VertexFormat.Mode.TRIANGLES, PRenderTypes.VertexFormatProvider.POSITION_TEX_NORMAL, sprite);
		for (int vertex = 0; vertex < source.vertexCount(); vertex++)
			builder.addVertex(source.positions().get(vertex * 3), source.positions().get(vertex * 3 + 1), source.positions().get(vertex * 3 + 2)).
					setUv(source.uvs().get(vertex * 2), source.uvs().get(vertex * 2 + 1)).
					setNormal(source.normals().get(vertex * 3), source.normals().get(vertex * 3 + 1), source.normals().get(vertex * 3 + 2));
		PGeometryData geometry;
		try (MeshData data = builder.buildOrThrow())
		{
			geometry = GlGeometryDataFactory.capture(data, source,
					PRenderTypes.VertexFormatProvider.POSITION_TEX_NORMAL.getVertexSize());
		}
		return new PBakedMesh(base.uuid(), geometry, base.textureReference(), emissive,
				PTextureAlphaClassifier.resolve(sprite.contents()), source, texture);
	}
}
