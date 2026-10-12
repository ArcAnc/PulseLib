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
import com.arcanc.pulselib.data.PLoadedModel;
import com.arcanc.pulselib.data.PModelLoader;
import com.arcanc.pulselib.util.PLibDatabase;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

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
	public List<ResourceLocation> modelResourceCandidates(ResourceLocation modelId)
	{
		ResourceLocation source = modelId.withPrefix(ROOT + "/");
		return List.of(source.withSuffix(GLB_EXTENSION), source.withSuffix(GLTF_EXTENSION));
	}

	@Override
	public CompletableFuture<PLoadedModel> loadModel(Executor backgroundExecutor,
	                                                 ResourceManager resourceManager,
	                                                 ResourceLocation modelId)
	{
		return CompletableFuture.supplyAsync(() ->
		{
			ResourceLocation source = modelResourceCandidates(modelId).stream()
					.filter(candidate -> resourceManager.getResource(candidate).isPresent())
					.findFirst()
					.orElseThrow(() -> new IllegalStateException(
							"Registered model was not loaded: " + modelId + "; checked resources: "
									+ modelResourceCandidates(modelId)));
			try
			{
				PModel model = PGltfModelParser.parse(resourceManager.getResourceOrThrow(source).open());
				loadAnimationEvents(resourceManager, source, model);
				return new PLoadedModel(modelId, source, model);
			}
			catch (IOException exception)
			{
				throw new RuntimeException("Can't load GLTF model " + source + " for " + modelId, exception);
			}
		}, backgroundExecutor);
	}

	private void loadAnimationEvents(ResourceManager resourceManager, ResourceLocation modelResource, PModel model) throws IOException
	{
		Optional<ResourceLocation> eventsResource = eventCandidates(modelResource).stream()
				.filter(resource -> resourceManager.getResource(resource).isPresent())
				.findFirst();

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
