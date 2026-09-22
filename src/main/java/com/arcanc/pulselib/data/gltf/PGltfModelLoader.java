/**
 * @author ArcAnc
 * Created at: 27.05.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.data.gltf;


import com.arcanc.pulselib.content.model.PModel;
import com.arcanc.pulselib.data.PModelLoader;
import com.arcanc.pulselib.util.PLibDatabase;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

public class PGltfModelLoader implements PModelLoader
{
	public static final PGltfModelLoader INSTANCE = new PGltfModelLoader();
	
	private static final ResourceLocation ID = PLibDatabase.rl("gltf");
	
	private static final String ROOT = "glmodels";
	private static final String GLB_EXTENSION = ".glb";
	private static final String GLTF_EXTENSION = ".gltf";
	private static final String EVENTS_EXTENSION = ".events.json";
	private static final String ANIMATION_EVENTS_EXTENSION = ".animation_events.json";
	private PGltfModelLoader()
	{
	}
	
	@Override
	public ResourceLocation id()
	{
		return ID;
	}
	
	@Override
	public boolean supports(ResourceLocation modelPath)
	{
		String path = modelPath.getPath();
		return path.startsWith(ROOT + "/") && (path.endsWith(GLB_EXTENSION) || path.endsWith(GLTF_EXTENSION));
	}
	
	@Override
	public ResourceLocation defaultModelLocation(ResourceLocation modelLocation, String modelType)
	{
		return modelLocation.withPrefix(ROOT + "/" + modelType + "/").withSuffix(GLB_EXTENSION);
	}
	
	@Override
	public ResourceLocation modelResourceLocation(ResourceLocation modelLocation)
	{
		return modelLocation.getPath().startsWith(ROOT + "/") ? modelLocation : modelLocation.withPrefix(ROOT + "/");
	}

	@Override
	public ResourceLocation normalizeModelResourceLocation(ResourceLocation modelLocation)
	{
		ResourceLocation resource = modelResourceLocation(modelLocation);
		String path = resource.getPath();
		return path.endsWith(GLB_EXTENSION) || path.endsWith(GLTF_EXTENSION) ? resource : resource.withSuffix(GLB_EXTENSION);
	}

	@Override
	public List<ResourceLocation> modelResourceCandidates(ResourceLocation modelLocation)
	{
		ResourceLocation resource = modelResourceLocation(modelLocation);
		String path = resource.getPath();
		if (path.endsWith(GLTF_EXTENSION))
			return List.of(resource, resource.withPath(path.substring(0, path.length() - GLTF_EXTENSION.length()) + GLB_EXTENSION));
		if (path.endsWith(GLB_EXTENSION))
			return List.of(resource, resource.withPath(path.substring(0, path.length() - GLB_EXTENSION.length()) + GLTF_EXTENSION));
		return List.of(resource.withSuffix(GLB_EXTENSION), resource.withSuffix(GLTF_EXTENSION));
	}
	
	@Override
	public CompletableFuture<?> loadModels(Executor backgroundExecutor,
	                                       ResourceManager resourceManager,
	                                       BiConsumer<ResourceLocation, PModel> elementConsumer)
	{
		return CompletableFuture.supplyAsync(
				() -> resourceManager.listResources(
						ROOT,
						fileName -> fileName.toString().endsWith(GLB_EXTENSION) || fileName.toString().endsWith(GLTF_EXTENSION)),
				backgroundExecutor).
				thenApplyAsync(resources ->
		{
			Map<ResourceLocation, CompletableFuture<PModel>> tasks = new Object2ObjectOpenHashMap<>();
			
			for (ResourceLocation resource : resources.keySet())
			{
				tasks.put(resource, CompletableFuture.supplyAsync(() ->
				{
					try
					{
						PModel model = PGltfModelParser.parse(resources.get(resource).open());
						loadAnimationEvents(resourceManager, resource, model);
						return model;
					}
					catch (IOException e)
					{
						throw new RuntimeException("Can't load GLTF model " + resource, e);
					}
				}, backgroundExecutor));
			}
			return tasks;
		}, backgroundExecutor).
				thenAcceptAsync(modelsMap ->
		{
			for (Map.Entry<ResourceLocation, CompletableFuture<PModel>> entry : modelsMap.entrySet())
				elementConsumer.accept(entry.getKey(), entry.getValue().join());
		}, backgroundExecutor);
	}
	
	private void loadAnimationEvents(ResourceManager resourceManager, ResourceLocation modelResource, PModel model) throws IOException
	{
		Optional<ResourceLocation> eventsResource = eventCandidates(modelResource).stream().
				filter(resource -> resourceManager.getResource(resource).isPresent()).
				findFirst();
		
		if (eventsResource.isEmpty())
			return;
		
		PGltfAnimationEventSidecarParser.mergeSidecar(
				PGltfAnimationEventSidecarParser.parseJson(resourceManager.getResourceOrThrow(eventsResource.get()).open()),
				model.animations);
	}
	
	private List<ResourceLocation> eventCandidates(ResourceLocation modelResource)
	{
		String modelName = stripModelExtension(modelResource.getPath()).substring(ROOT.length() + 1);
		String fileName = modelName.substring(modelName.lastIndexOf('/') + 1);
		
		List<ResourceLocation> candidates = new ArrayList<>();
		candidates.add(modelResource.withPath(ROOT + "/" + modelName + EVENTS_EXTENSION));
		candidates.add(modelResource.withPath(ROOT + "/" + modelName + ANIMATION_EVENTS_EXTENSION));
		candidates.add(modelResource.withPath(ROOT + "/events/" + modelName + EVENTS_EXTENSION));
		candidates.add(modelResource.withPath(ROOT + "/events/" + fileName + EVENTS_EXTENSION));
		return candidates;
	}
	
	private static String stripModelExtension(String path)
	{
		if (path.endsWith(GLB_EXTENSION))
			return path.substring(0, path.length() - GLB_EXTENSION.length());
		if (path.endsWith(GLTF_EXTENSION))
			return path.substring(0, path.length() - GLTF_EXTENSION.length());
		return path;
	}
}
