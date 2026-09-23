/**
 * @author ArcAnc
 * Created at: 27.01.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.util;


import com.arcanc.pulselib.content.model.*;
import com.arcanc.pulselib.content.model.baked.*;
import com.arcanc.pulselib.content.model.deformer.gpu.PGpuDeformerBuffers;
import com.arcanc.pulselib.content.model.resource.PModelResource;
import com.arcanc.pulselib.content.model.textures.PTextureAlphaClassifier;
import com.arcanc.pulselib.content.model.textures.atlas.PLibMetadata;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.legacy.GlGeometryDataFactory;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.arcanc.pulselib.data.PModelLoader;
import com.arcanc.pulselib.data.gltf.PGltfModelLoader;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PModelCache
{
	private static @Nullable Map<ResourceLocation, PBakedModel> MODELS;
	private static final Map<ResourceLocation, PModelLoader> MODEL_LOADERS = Stream.of(PGltfModelLoader.INSTANCE).
			collect(Collectors.toMap(
			PModelLoader :: id,
			Function.identity(),
			(oldV, newV) ->
			{
				throw new IllegalStateException("Duplicate model loader id: " + oldV.id());
			},
			Object2ObjectOpenHashMap :: new));
	
	public static @Nullable Map<ResourceLocation, PBakedModel> getModels()
	{
		return MODELS;
	}
	
	public static void registerModelLoader(PModelLoader modelLoader)
	{
		if (MODEL_LOADERS.containsKey(modelLoader.id()))
			return;
		
		MODEL_LOADERS.put(modelLoader.id(), modelLoader);
	}
	
	public static void unregisterModelLoader(ResourceLocation loaderId)
	{
		MODEL_LOADERS.remove(loaderId);
	}
	
	public static List<PModelLoader> getModelLoaders()
	{
		return List.copyOf(MODEL_LOADERS.values());
	}
	
	public static Optional<PModelLoader> getModelLoader(ResourceLocation loaderId)
	{
		return Optional.ofNullable(MODEL_LOADERS.get(loaderId));
	}
	
	@ApiStatus.Internal
	public static CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier stage,
	                                             ResourceManager resourceManager,
	                                             ProfilerFiller preparationsProfiler,
	                                             ProfilerFiller reloadProfiler,
	                                             Executor backgroundExecutor,
	                                             Executor gameExecutor)
	{
		Map<ResourceLocation, PModel> models = new Object2ObjectOpenHashMap<>();
		return CompletableFuture.allOf(loadModels(backgroundExecutor, resourceManager, models :: put)).
				thenRun(() -> verifyModelsLoaded(models)).
				thenCompose(stage :: wait).
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
		PRenderQueue.cleanup();
		PMeshTextureVariants.clear();
		PTextureAlphaClassifier.clear();
		if (MODELS != null)
		{
			MODELS.forEach(($, model) ->
					model.bones().forEach(PModelCache :: clearBoneCache));
			MODELS = null;
		}
		PGpuDeformerBuffers.clearDefinitions();
	}

	private static void verifyModelsLoaded(Map<ResourceLocation, PModel> models)
	{
		for (PModelResource resource : PResourceCache.getResourceCache().values())
		{
			PModelLoader loader = MODEL_LOADERS.get(resource.modelLoaderId());
			List<ResourceLocation> candidates = loader.modelResourceCandidates(resource.model());
			if (!models.containsKey(resource.model()))
				throw new IllegalStateException("Registered model was not loaded: " + resource.model()
						+ "; checked resources: " + candidates);
		}
	}
	
	private static void clearBoneCache(PBakedBone bone)
	{
		bone.meshes().forEach(mesh ->
		{
			PDeformedMeshBuffers.close(mesh);
			PGpuDeformedMeshBuffers.close(mesh);
		});
		bone.children().forEach(PModelCache :: clearBoneCache);
	}
	
	private static Map<ResourceLocation, PBakedModel> bakeModels(Map<ResourceLocation, PModel> rawModels)
	{
		Map<ResourceLocation, PBakedModel> bakedModelMap = new Object2ObjectOpenHashMap<>();
		for (Map.Entry<ResourceLocation, PModel> rawModel : rawModels.entrySet())
		{
			PModel model = rawModel.getValue();
			ResourceLocation modelPath = rawModel.getKey();
			Map<UUID, PBakedBone.PBakedBoneBuilder> bakedBoneBuilder = new HashMap<>();
			for (PBone bone : model.bones.values())
			{
				bakedBoneBuilder.put(
						bone.uuid(),
						new PBakedBone.PBakedBoneBuilder(
								bone.uuid(),
								bone.name(),
								new Vector3f(bone.pivot()),
								new Quaternionf(bone.baseRotation()).normalize()
						)
				);
			}
			
			for (Map.Entry<UUID, Pair<UUID, List<UUID>>> bone2MeshesEntry : model.boneMeshes.entrySet())
			{
				PBakedBone.PBakedBoneBuilder builder = bakedBoneBuilder.get(bone2MeshesEntry.getKey());
				
				Map<PMaterial, List<PMeshPrimitive>> byMaterial = new LinkedHashMap<>();
				for (UUID meshUUID : bone2MeshesEntry.getValue().getSecond())
				{
					PMesh mesh = model.meshes.get(meshUUID);
					for (PMeshPrimitive primitive : mesh.primitives())
						byMaterial.computeIfAbsent(primitive.material(), ignored -> new ArrayList<>()).add(primitive);
				}
				for (List<PMeshPrimitive> primitives : byMaterial.values())
					bakePrimitive(modelPath, UUID.randomUUID(), PMeshPrimitive.merge(primitives), builder);
			}
			
			for (PBone bone : model.bones.values())
			{
				PBone parentBone = bone.parent();
				if (parentBone == null)
					continue;
				
				PBakedBone.PBakedBoneBuilder child =
						bakedBoneBuilder.get(bone.uuid());
				
				PBakedBone.PBakedBoneBuilder parent =
						bakedBoneBuilder.get(parentBone.uuid());
				
				child.parent = parent;
				parent.children.add(child);
			}
			
			List<PBakedBone> rootBones = new ArrayList<>();
			
			for (PBakedBone.PBakedBoneBuilder builder : bakedBoneBuilder.values())
				if (builder.parent == null)
					rootBones.add(bakeBone(builder, null));
			
			bakedModelMap.put(
					rawModel.getKey(),
					new PBakedModel(ImmutableList.copyOf(rootBones),
							ImmutableMap.copyOf(model.animations))
			);
		}
		
		return bakedModelMap;
	}

	private static void bakePrimitive(ResourceLocation modelPath, UUID meshId, PMeshPrimitive primitive,
	                                  PBakedBone.PBakedBoneBuilder builder)
	{
		String reference = primitive.material() == null ? "<missing>" : primitive.material().textureReference();
		if (reference.isEmpty())
			return;
		try
		{
			if (primitive.material() == null)
				throw new IllegalStateException("Primitive has no material");
			ResourceLocation texture = PResourceCache.resolve(modelPath, reference);
			TextureAtlasSprite sprite = PResourceCache.getTextureAtlas().getSprite(texture);
			if (sprite.contents().name().getPath().equals("missingno"))
				throw new IllegalStateException("Texture is missing from atlas: " + texture);
			boolean emissive = sprite.contents().metadata().getSection(PLibMetadata.TYPE)
					.map(PLibMetadata::isEmissive).orElse(false);
			ByteBufferBuilder bytes = new ByteBufferBuilder(
					primitive.vertexCount() * PRenderTypes.VertexFormatProvider.POSITION_TEX_NORMAL.getVertexSize());
			BufferBuilder buffer = new AtlasBufferBuilder(bytes, VertexFormat.Mode.TRIANGLES,
					PRenderTypes.VertexFormatProvider.POSITION_TEX_NORMAL, sprite);
			for (int vertex = 0; vertex < primitive.vertexCount(); vertex++)
				buffer.addVertex(primitive.positions().get(vertex * 3), primitive.positions().get(vertex * 3 + 1), primitive.positions().get(vertex * 3 + 2))
						.setUv(primitive.uvs().get(vertex * 2), primitive.uvs().get(vertex * 2 + 1))
						.setNormal(primitive.normals().get(vertex * 3), primitive.normals().get(vertex * 3 + 1), primitive.normals().get(vertex * 3 + 2));
			try (MeshData meshData = buffer.buildOrThrow())
			{
				PGeometryData geometry = GlGeometryDataFactory.capture(meshData, primitive,
						PRenderTypes.VertexFormatProvider.POSITION_TEX_NORMAL.getVertexSize());
				builder.meshes.add(new PBakedMesh(meshId, geometry, reference, emissive,
						PTextureAlphaClassifier.resolve(sprite.contents()), primitive, texture));
			}
		}
		catch (RuntimeException exception)
		{
			throw new IllegalStateException("Can't bake model " + modelPath + " texture " + reference, exception);
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
	                                               BiConsumer<ResourceLocation, PModel> elementConsumer)
	{
		CompletableFuture<?> chain = CompletableFuture.completedFuture(null);
		for (PModelResource resource : PResourceCache.getResourceCache().values())
			if (!MODEL_LOADERS.containsKey(resource.modelLoaderId()))
				throw new IllegalStateException("No model loader registered for " + resource.model() + ": " + resource.modelLoaderId());
		
		for (PModelLoader modelLoader : getModelLoaders())
			chain = chain.thenCompose(empty -> modelLoader.loadModels(backgroundExecutor, resourceManager, (model, parsed) ->
					PResourceCache.getModelResource(model, resourceManager)
							.filter(resource -> resource.modelLoaderId().equals(modelLoader.id()))
							.ifPresent(resource -> elementConsumer.accept(resource.model(), parsed))));
		
		return chain;
	}
}
