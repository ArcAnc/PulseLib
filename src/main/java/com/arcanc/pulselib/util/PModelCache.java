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
import com.arcanc.pulselib.content.model.PMesh;
import com.arcanc.pulselib.content.model.PModel;
import com.arcanc.pulselib.content.model.baked.AtlasBufferBuilder;
import com.arcanc.pulselib.content.model.baked.PBakedBone;
import com.arcanc.pulselib.content.model.baked.PSubdividedMeshCache;
import com.arcanc.pulselib.content.model.baked.PMeshTextureVariants;
import com.arcanc.pulselib.content.model.baked.PBakedMesh;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.model.deformer.gpu.PGpuDeformerBuffers;
import com.arcanc.pulselib.content.model.textures.PTextureAlphaClassifier;
import com.arcanc.pulselib.content.model.textures.atlas.PLibSpriteMetadata;
import com.arcanc.pulselib.data.gltf.PGltfModelLoader;
import com.arcanc.pulselib.data.PModelLoader;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
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
	
	/**
	 * Returns the models.
	 * @return the value produced by this operation.
	 */
	public static @Nullable Map<Identifier, PBakedModel> getModels()
	{
		return MODELS;
	}
	
	/**
	 * Registers the model loader.
	 * @param modelLoader the model loader to use.
	 */
	public static void registerModelLoader(PModelLoader modelLoader)
	{
		if (MODEL_LOADERS.containsKey(modelLoader.id()))
			return;
		
		MODEL_LOADERS.put(modelLoader.id(), modelLoader);
	}
	
	/**
	 * Unregisters the model loader.
	 * @param loaderId the loader id to use.
	 */
	public static void unregisterModelLoader(Identifier loaderId)
	{
		MODEL_LOADERS.remove(loaderId);
	}
	
	/**
	 * Returns the model loaders.
	 * @return the value produced by this operation.
	 */
	public static List<PModelLoader> getModelLoaders()
	{
		return List.copyOf(MODEL_LOADERS.values());
	}
	
	/**
	 * Returns the model loader.
	 * @param loaderId the loader id to use.
	 * @return the value produced by this operation.
	 */
	public static Optional<PModelLoader> getModelLoader(Identifier loaderId)
	{
		return Optional.ofNullable(MODEL_LOADERS.get(loaderId));
	}
	
	/**
	 * Performs the reload operation.
	 * @param sharedState the shared state to use.
	 * @param backgroundExecutor the background executor to use.
	 * @param preparationBarrier the preparation barrier to use.
	 * @param gameExecutor the game executor to use.
	 * @return the value produced by this operation.
	 */
	@ApiStatus.Internal
	public static CompletableFuture<Void> reload(PreparableReloadListener.SharedState sharedState,
	                                             Executor backgroundExecutor,
	                                             PreparableReloadListener.PreparationBarrier preparationBarrier,
	                                             Executor gameExecutor)
	{
		Map<Identifier, PModel> models = new Object2ObjectOpenHashMap<>();
		return CompletableFuture.allOf(loadModels(backgroundExecutor, sharedState.resourceManager(), models :: put)).
				thenCompose(preparationBarrier :: wait).
				thenAcceptAsync(empty ->
				{
					if (PModelCache.MODELS != null)
						clearCaches();
					PModelCache.MODELS = bakeModels(models);
				},
				gameExecutor);
	}
	
	/**
	 * Clears the caches.
	 */
	private static void clearCaches()
	{
		PTextureAlphaClassifier.clear();
		if (MODELS != null)
		{
			MODELS.forEach((_, model) ->
				model.bones().forEach(PModelCache :: clearBoneCache));
			PMeshTextureVariants.clear();
			PGpuDeformerBuffers.cleanup();
			MODELS = null;
		}
	}
	
	/**
	 * Clears the bone cache.
	 * @param bone the bone to use.
	 */
	private static void clearBoneCache(PBakedBone bone)
	{
		bone.meshes().forEach(mesh ->
		{
			PSubdividedMeshCache.close(mesh);
			mesh.vbo().close();
			mesh.indices().close();
		});
		bone.children().forEach(PModelCache :: clearBoneCache);
	}
	
	/**
	 * Performs the bake models operation.
	 * @param rawModels the raw models to use.
	 * @return the value produced by this operation.
	 */
	private static Map<Identifier, PBakedModel> bakeModels(Map<Identifier, PModel> rawModels)
	{
		Map<Identifier, PBakedModel> bakedModelMap = new Object2ObjectOpenHashMap<>();
		for (Map.Entry<Identifier, PModel> rawModel : rawModels.entrySet())
		{
			PModel model = rawModel.getValue();
			Identifier modelPath = rawModel.getKey();
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
				
				for (UUID meshUUID : bone2MeshesEntry.getValue().getSecond())
				{
					PMesh mesh = model.meshes.get(meshUUID);
					Identifier loc = textureLocation(modelPath, mesh.texture());
					
					TextureAtlasSprite sprite = PTextureCache.getTextureAtlas().getSprite(loc);
					
					boolean emissive = sprite.contents().getAdditionalMetadata(PLibSpriteMetadata.TYPE).
							map(PLibSpriteMetadata :: emissive).
							orElse(false);
					
					ByteBufferBuilder byteBufferBuilder = ByteBufferBuilder.
							exactlySized(mesh.vertexCount() * PRenderTypes.VertexFormatProvider.POSITION_TEX_NORMAL.getVertexSize());
					BufferBuilder bufferBuilder;
					
					if (sprite.contents().name().getPath().equals("missingno"))
						bufferBuilder = new BufferBuilder(byteBufferBuilder, VertexFormat.Mode.TRIANGLES, PRenderTypes.VertexFormatProvider.POSITION_TEX_NORMAL);
					else
						bufferBuilder = new AtlasBufferBuilder(byteBufferBuilder, VertexFormat.Mode.TRIANGLES, PRenderTypes.VertexFormatProvider.POSITION_TEX_NORMAL, sprite);
					
					for (int q = 0; q < mesh.vertexCount(); q++)
					{
						float x = mesh.positions().get(q * 3);
						float y = mesh.positions().get(q * 3 + 1);
						float z = mesh.positions().get(q * 3 + 2);
						
						float u = mesh.uvs().get(q * 2);
						float v = mesh.uvs().get(q * 2 + 1);
						
						float nx = mesh.normals().get(q * 3);
						float ny = mesh.normals().get(q * 3 + 1);
						float nz = mesh.normals().get(q * 3 + 2);
						
						bufferBuilder.
								addVertex(
										x,
										y,
										z).
								setUv(
										u,
										v).
								setNormal(
										nx,
										ny,
										nz);
					}
					ByteBuffer indexBuffer = mesh.indices();
					
					try (MeshData meshData = bufferBuilder.buildOrThrow())
					{
						GpuBuffer buffer = RenderSystem.getDevice().createBuffer(
								meshUUID :: toString,
								GpuBuffer.USAGE_VERTEX,
								meshData.vertexBuffer()
						);
						
						GpuBuffer gpuIndexBuffer = RenderSystem.getDevice().createBuffer(
								() -> meshUUID.toString() + "_indexes",
								GpuBuffer.USAGE_INDEX,
								indexBuffer);
						VertexFormat.IndexType type = mesh.glIndexType() == GltfConstants.GL_UNSIGNED_SHORT ? VertexFormat.IndexType.SHORT : VertexFormat.IndexType.INT;
						builder.meshes.add(new PBakedMesh(
								meshUUID,
								buffer,
								mesh.vertexCount(),
								gpuIndexBuffer,
								mesh.indicesCount(),
								type,
								mesh.texture(),
								emissive,
								PTextureAlphaClassifier.resolve(sprite.contents()),
								mesh,
								loc));
					}
				}
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
	
	/**
	 * Performs the bake bone operation.
	 * @param builder the builder to use.
	 * @param bakedParent the baked parent to use.
	 * @return the value produced by this operation.
	 */
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
	
	/**
	 * Resolves the texture location.
	 * @param modelPath the model path to use.
	 * @param textureName the texture name to use.
	 * @return the value produced by this operation.
	 */
	public static Identifier resolveTextureLocation(Identifier modelPath, String textureName)
	{
		for (PModelLoader modelLoader : PModelCache.getModelLoaders())
			if (modelLoader.supports(modelPath))
				return modelLoader.textureLocation(modelPath, textureName);
		
		return modelPath.withPath(textureName);
	}
	
	/**
	 * Performs the texture location operation.
	 * @param modelPath the model path to use.
	 * @param textureName the texture name to use.
	 * @return the value produced by this operation.
	 */
	private static Identifier textureLocation(Identifier modelPath, String textureName)
	{
		return resolveTextureLocation(modelPath, textureName);
	}
	
	/**
	 * Loads the models.
	 * @param backgroundExecutor the background executor to use.
	 * @param resourceManager the resource manager to use.
	 * @param elementConsumer the element consumer to use.
	 * @return the value produced by this operation.
	 */
	private static CompletableFuture<?> loadModels(Executor backgroundExecutor,
	                                               ResourceManager resourceManager,
	                                               BiConsumer<Identifier, PModel> elementConsumer)
	{
		CompletableFuture<?> chain = CompletableFuture.completedFuture(null);
		
		for (PModelLoader modelLoader : getModelLoaders())
			chain = chain.thenCompose(empty -> modelLoader.loadModels(backgroundExecutor, resourceManager, elementConsumer));
		
		return chain;
	}
}
