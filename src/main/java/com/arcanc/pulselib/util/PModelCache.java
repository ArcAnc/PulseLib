/**
 * @author ArcAnc
 * Created at: 27.01.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.util;


import com.arcanc.pulselib.content.model.PBone;
import com.arcanc.pulselib.content.model.PMaterial;
import com.arcanc.pulselib.content.model.PMesh;
import com.arcanc.pulselib.content.model.PMeshPrimitive;
import com.arcanc.pulselib.content.model.PModel;
import com.arcanc.pulselib.content.model.resource.PModelResource;
import com.arcanc.pulselib.content.model.baked.AtlasBufferBuilder;
import com.arcanc.pulselib.content.model.baked.PBakedBone;
import com.arcanc.pulselib.content.model.baked.PDeformedMeshBuffers;
import com.arcanc.pulselib.content.model.baked.PMeshTextureVariants;
import com.arcanc.pulselib.content.model.baked.PBakedMesh;
import com.arcanc.pulselib.content.model.baked.PSubdividedMeshCache;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.model.textures.PTextureAlphaClassifier;
import com.arcanc.pulselib.content.model.textures.atlas.PLibSpriteMetadata;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.data.gltf.PGltfModelLoader;
import com.arcanc.pulselib.data.PModelLoader;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.datafixers.util.Pair;
import de.javagl.jgltf.model.GltfConstants;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PModelCache
{
	private static @Nullable Map<Identifier, PBakedModel> MODELS;
	private static final Map<Identifier, PModelLoader> MODEL_LOADERS = Stream.of(PGltfModelLoader.INSTANCE).
			collect(Collectors.toMap(
					PModelLoader :: id,
					Function.identity(),
					(oldV, newV) ->
					{
						throw new IllegalStateException("Duplicate model loader id: " + oldV.id());
					},
					Object2ObjectOpenHashMap :: new));
	
	public static @Nullable Map<Identifier, PBakedModel> getModels()
	{
		return MODELS;
	}
	
	public static void registerModelLoader(PModelLoader modelLoader)
	{
		if (MODEL_LOADERS.containsKey(modelLoader.id()))
			return;
		
		MODEL_LOADERS.put(modelLoader.id(), modelLoader);
	}
	
	public static void unregisterModelLoader(Identifier loaderId)
	{
		MODEL_LOADERS.remove(loaderId);
	}
	
	public static List<PModelLoader> getModelLoaders()
	{
		return List.copyOf(MODEL_LOADERS.values());
	}
	
	public static Optional<PModelLoader> getModelLoader(Identifier loaderId)
	{
		return Optional.ofNullable(MODEL_LOADERS.get(loaderId));
	}
	
	@ApiStatus.Internal
	public static CompletableFuture<Void> reload(PreparableReloadListener.SharedState sharedState,
	                                             Executor backgroundExecutor,
	                                             PreparableReloadListener.PreparationBarrier preparationBarrier,
	                                             Executor gameExecutor)
	{
		Map<Identifier, PModel> models = new Object2ObjectOpenHashMap<>();
		return CompletableFuture.allOf(loadModels(backgroundExecutor, sharedState.resourceManager(), models :: put)).
				thenRun(() -> verifyModelsLoaded(models)).
				thenCompose(preparationBarrier :: wait).
				thenAcceptAsync(empty ->
				{
					if (PModelCache.MODELS != null)
						clearCaches();
					PModelCache.MODELS = bakeModels(models);
				},
				gameExecutor);
	}
	
	private static void clearCaches()
	{
		PRenderQueue.cleanUp();
		if (MODELS != null)
		{
			PMeshTextureVariants.clear();
			MODELS.forEach((_, model) ->
				model.bones().forEach(PModelCache :: clearBoneCache));
			MODELS = null;
		}
	}
	
	private static void clearBoneCache(PBakedBone bone)
	{
		bone.meshes().forEach(mesh ->
		{
			PDeformedMeshBuffers.close(mesh);
			PSubdividedMeshCache.close(mesh);
			mesh.vbo().close();
			mesh.indices().close();
		});
		bone.children().forEach(PModelCache :: clearBoneCache);
	}
	
	private static Map<Identifier, PBakedModel> bakeModels(Map<Identifier, PModel> rawModels)
	{
		Map<Identifier, PBakedModel> bakedModelMap = new Object2ObjectOpenHashMap<>();
		for (Map.Entry<Identifier, PModel> rawModel : rawModels.entrySet())
		{
			try
			{
				PModel model = rawModel.getValue();
				Identifier modelPath = rawModel.getKey();
				Map<UUID, PBakedBone.PBakedBoneBuilder> bakedBoneBuilder = new HashMap<>();
				for (PBone bone : model.bones.values())
				{
					bakedBoneBuilder.put(bone.uuid(), new PBakedBone.PBakedBoneBuilder(bone.uuid(), bone.name(),
							new Vector3f(bone.pivot()), new Quaternionf(bone.baseRotation()).normalize()));
				}

				for (Map.Entry<UUID, Pair<UUID, List<UUID>>> entry : model.boneMeshes.entrySet())
				{
					PBakedBone.PBakedBoneBuilder builder = bakedBoneBuilder.get(entry.getKey());
					Map<PMaterial, List<PMeshPrimitive>> primitivesByMaterial = new LinkedHashMap<>();
					for (UUID meshUUID : entry.getValue().getSecond())
					{
						PMesh mesh = model.meshes.get(meshUUID);
						for (PMeshPrimitive primitive : mesh.primitives())
							primitivesByMaterial.computeIfAbsent(primitive.material(), ignored -> new ArrayList<>()).add(primitive);
					}
					for (List<PMeshPrimitive> primitives : primitivesByMaterial.values())
						bakePrimitive(modelPath, PMeshPrimitive.merge(primitives), builder);
				}

				for (PBone bone : model.bones.values())
				{
					PBone parentBone = bone.parent();
					if (parentBone == null)
						continue;
					PBakedBone.PBakedBoneBuilder child = bakedBoneBuilder.get(bone.uuid());
					PBakedBone.PBakedBoneBuilder parent = bakedBoneBuilder.get(parentBone.uuid());
					child.parent = parent;
					parent.children.add(child);
				}

				List<PBakedBone> rootBones = new ArrayList<>();
				for (PBakedBone.PBakedBoneBuilder builder : bakedBoneBuilder.values())
					if (builder.parent == null)
						rootBones.add(bakeBone(builder, null));
				bakedModelMap.put(rawModel.getKey(), new PBakedModel(ImmutableList.copyOf(rootBones), ImmutableMap.copyOf(model.animations)));
			}
			catch (RuntimeException exception)
			{
				throw new IllegalStateException("Can't bake model " + rawModel.getKey(), exception);
			}
		}
		
		return bakedModelMap;
	}

	private static void verifyModelsLoaded(Map<Identifier, PModel> models)
	{
		for (PModelResource resource : PResourceCache.getResourceCache().values())
		{
			PModelLoader loader = MODEL_LOADERS.get(resource.modelLoaderId());
			List<Identifier> candidates = loader.modelResourceCandidates(resource.model());
			if (candidates.stream().noneMatch(models :: containsKey))
				throw new IllegalStateException("Registered model was not loaded; tried: " +
						candidates.stream().map(Identifier :: toString).collect(Collectors.joining(", ")));
		}
	}

	private static void bakePrimitive(Identifier modelPath, PMeshPrimitive primitive, PBakedBone.PBakedBoneBuilder builder)
	{
		String textureReference = primitive == null || primitive.material() == null ? "<missing>" : primitive.material().textureReference();
		if (textureReference.isEmpty())
			throw new IllegalStateException("Primitive has no texture reference");
		try
		{
			if (primitive == null || primitive.material() == null)
				throw new IllegalStateException("Primitive has no material");
			Identifier texture = PResourceCache.resolve(modelPath, textureReference);
			TextureAtlasSprite sprite = PResourceCache.getTextureAtlas().getSprite(texture);
			if (sprite.contents().name().getPath().equals("missingno"))
				throw new IllegalStateException("Texture is missing from atlas: " + texture);
			boolean emissive = sprite.contents().getAdditionalMetadata(PLibSpriteMetadata.TYPE).
					map(PLibSpriteMetadata :: emissive).orElse(false);
			ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(
					primitive.vertexCount() * PRenderTypes.VertexFormatProvider.POSITION_TEX_NORMAL.getVertexSize());
			BufferBuilder buffer = new AtlasBufferBuilder(bytes, PrimitiveTopology.TRIANGLES,
					PRenderTypes.VertexFormatProvider.POSITION_TEX_NORMAL, sprite);
			for (int vertex = 0; vertex < primitive.vertexCount(); vertex++)
				buffer.addVertex(primitive.positions().get(vertex * 3), primitive.positions().get(vertex * 3 + 1), primitive.positions().get(vertex * 3 + 2)).
						setUv(primitive.uvs().get(vertex * 2), primitive.uvs().get(vertex * 2 + 1)).
						setNormal(primitive.normals().get(vertex * 3), primitive.normals().get(vertex * 3 + 1), primitive.normals().get(vertex * 3 + 2));
			UUID primitiveUUID = UUID.randomUUID();
			try (MeshData data = buffer.buildOrThrow())
			{
				GpuBuffer vertices = RenderSystem.getDevice().createBuffer(primitiveUUID :: toString, GpuBuffer.USAGE_VERTEX, data.vertexBuffer());
				ByteBuffer indices = primitive.indices().duplicate();
				indices.clear();
				GpuBuffer indexBuffer = RenderSystem.getDevice().createBuffer(() -> primitiveUUID + "_indexes", GpuBuffer.USAGE_INDEX, indices);
				IndexType indexType = primitive.glIndexType() == GltfConstants.GL_UNSIGNED_SHORT ? IndexType.SHORT : IndexType.INT;
				builder.meshes.add(new PBakedMesh(primitiveUUID, vertices, primitive.vertexCount(), indexBuffer,
						primitive.indicesCount(), indexType, textureReference, emissive,
						PTextureAlphaClassifier.resolve(sprite.contents()), primitive, texture));
			}
		}
		catch (RuntimeException exception)
		{
			throw new IllegalStateException("Can't bake primitive for model " + modelPath + " with texture reference " + textureReference, exception);
		}
	}
	
	private static PBakedBone bakeBone(
			PBakedBone.PBakedBoneBuilder builder,
			@Nullable PBakedBone bakedParent)
	{
		List<PBakedBone> bakedChildren = new ArrayList<>();
		
		PBakedBone bakedBone = new PBakedBone(
				builder.name,
				builder.basePosition,
				builder.baseRotation,
				List.of(),
				bakedParent,
				ImmutableList.copyOf(builder.meshes)
		);
		
		for (PBakedBone.PBakedBoneBuilder child : builder.children)
			bakedChildren.add(bakeBone(child, bakedBone));
		
		return new PBakedBone(
				builder.name,
				builder.basePosition,
				builder.baseRotation,
				ImmutableList.copyOf(bakedChildren),
				bakedParent,
				ImmutableList.copyOf(builder.meshes)
		);
	}
	
	private static CompletableFuture<?> loadModels(Executor backgroundExecutor,
	                                               ResourceManager resourceManager,
	                                               BiConsumer<Identifier, PModel> elementConsumer)
	{
		CompletableFuture<?> chain = CompletableFuture.completedFuture(null);
		for (PModelResource resource : PResourceCache.getResourceCache().values())
			if (!MODEL_LOADERS.containsKey(resource.modelLoaderId()))
				throw new IllegalStateException("No model loader registered for " + resource.model() + ": " + resource.modelLoaderId());
		for (PModelLoader modelLoader : getModelLoaders())
			chain = chain.thenCompose(empty -> modelLoader.loadModels(backgroundExecutor, resourceManager, (model, parsed) ->
			{
				if (PResourceCache.getModelResource(model, resourceManager).
						filter(resource -> resource.modelLoaderId().equals(modelLoader.id())).isPresent())
					elementConsumer.accept(model, parsed);
			}));
		
		return chain;
	}
}
