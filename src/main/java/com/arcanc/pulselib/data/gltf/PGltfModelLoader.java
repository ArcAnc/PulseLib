/**
 * @author ArcAnc
 * Created at: 24.05.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.data.gltf;


import com.arcanc.pulselib.content.model.PModel;
import com.arcanc.pulselib.content.model.resource.PModelResource;
import com.arcanc.pulselib.data.PLoadedModel;
import com.arcanc.pulselib.data.PModelLoader;
import com.arcanc.pulselib.util.PLibDatabase;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Loads gltf model.
 */
public class PGltfModelLoader implements PModelLoader
{
	public static final PGltfModelLoader INSTANCE = new PGltfModelLoader();
	
	private static final Identifier ID = PLibDatabase.rl("gltf");
	
	private static final String ROOT = "glmodels";
	private static final String GLB_EXTENSION = ".glb";
	private static final String GLTF_EXTENSION = ".gltf";
	private static final String EVENTS_EXTENSION = ".events.json";
	private static final String ANIMATION_EVENTS_EXTENSION = ".animation_events.json";
	/**
	 * Creates an instance of the enclosing type.
	 */
	private PGltfModelLoader()
	{
	}
	
	/**
	 * Performs the id operation.
	 * @return the value produced by this operation.
	 */
	@Override
	public Identifier id()
	{
		return ID;
	}
	
	/**
	 * Returns both physical glTF representations for a canonical model. GLB is
	 * preferred for an extension-less logical id.
	 *
	 * @param modelId the canonical model id.
	 * @return the GLB and glTF resource candidates.
	 */
	@Override
	public List<Identifier> physicalResourceCandidates(Identifier modelId)
	{
		Identifier base = modelId.withPrefix(ROOT + "/");
		return List.of(base.withSuffix(GLB_EXTENSION), base.withSuffix(GLTF_EXTENSION));
	}
	
	/**
	 * Loads the models.
	 * @param backgroundExecutor the background executor to use.
	 * @param resourceManager the resource manager to use.
	 * @param elementConsumer the element consumer to use.
	 * @return the value produced by this operation.
	 */
	@Override
	public CompletableFuture<?> loadModels(Executor backgroundExecutor,
	                                       ResourceManager resourceManager,
	                                       Collection<PModelResource> models,
	                                       Consumer<PLoadedModel> elementConsumer)
	{
		List<CompletableFuture<PLoadedModel>> tasks = models.stream().map(resource ->
				CompletableFuture.supplyAsync(() -> loadModel(resourceManager, resource), backgroundExecutor)).toList();
		return CompletableFuture.allOf(tasks.toArray(CompletableFuture[] :: new)).thenRunAsync(
				() -> tasks.forEach(task -> elementConsumer.accept(task.join())), backgroundExecutor);
	}

	private PLoadedModel loadModel(ResourceManager resourceManager, PModelResource resource)
	{
		Identifier source = physicalResourceCandidates(resource.modelId()).stream().
				filter(candidate -> resourceManager.getResource(candidate).isPresent()).findFirst().
				orElseThrow(() -> new IllegalStateException("Registered glTF model " + resource.modelId() +
						" has no resource; tried: " + physicalResourceCandidates(resource.modelId())));
		try
		{
			PModel model = PGltfModelParser.parse(resourceManager.getResourceOrThrow(source).open());
			loadAnimationEvents(resourceManager, source, model);
			return new PLoadedModel(resource.modelId(), source, model);
		}
		catch (IOException exception)
		{
			throw new RuntimeException("Can't load GLTF model " + resource.modelId() + " from " + source, exception);
		}
	}
	
	/**
	 * Loads the animation events.
	 * @param resourceManager the resource manager to use.
	 * @param modelResource the model resource to use.
	 * @param model the model to use.
	 */
	private void loadAnimationEvents(ResourceManager resourceManager, Identifier modelResource, PModel model) throws IOException
	{
		Optional<Identifier> eventsResource = eventCandidates(modelResource).stream().
				filter(resource -> resourceManager.getResource(resource).isPresent()).
				findFirst();
		
		if (eventsResource.isEmpty())
			return;
		
		PGltfAnimationEventSidecarParser.mergeSidecar(
				PGltfAnimationEventSidecarParser.parseJson(resourceManager.getResourceOrThrow(eventsResource.get()).open()),
				model.animations);
	}
	
	/**
	 * Performs the event candidates operation.
	 * @param modelResource the model resource to use.
	 * @return the value produced by this operation.
	 */
	private List<Identifier> eventCandidates(Identifier modelResource)
	{
		String modelName = stripModelExtension(modelResource.getPath()).substring(ROOT.length() + 1);
		String fileName = modelName.substring(modelName.lastIndexOf('/') + 1);
		
		List<Identifier> candidates = new ArrayList<>();
		candidates.add(modelResource.withPath(ROOT + "/" + modelName + EVENTS_EXTENSION));
		candidates.add(modelResource.withPath(ROOT + "/" + modelName + ANIMATION_EVENTS_EXTENSION));
		candidates.add(modelResource.withPath(ROOT + "/events/" + modelName + EVENTS_EXTENSION));
		candidates.add(modelResource.withPath(ROOT + "/events/" + fileName + EVENTS_EXTENSION));
		return candidates;
	}
	
	/**
	 * Performs the strip model extension operation.
	 * @param path the path to use.
	 * @return the value produced by this operation.
	 */
	private static String stripModelExtension(String path)
	{
		if (path.endsWith(GLB_EXTENSION))
			return path.substring(0, path.length() - GLB_EXTENSION.length());
		if (path.endsWith(GLTF_EXTENSION))
			return path.substring(0, path.length() - GLTF_EXTENSION.length());
		return path;
	}

}
