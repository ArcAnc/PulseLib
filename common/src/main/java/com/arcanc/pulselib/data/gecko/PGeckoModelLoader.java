/**
 * @author ArcAnc
 * Created at: 24.05.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.data.gecko;


import com.arcanc.pulselib.content.model.PModel;
import com.arcanc.pulselib.content.model.resource.PModelResource;
import com.arcanc.pulselib.data.PAnimationSidecarParser;
import com.arcanc.pulselib.data.PLoadedModel;
import com.arcanc.pulselib.data.PModelLoader;
import com.arcanc.pulselib.util.PLibDatabase;
import com.mojang.blaze3d.vertex.PoseStack;
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
 * Loads gecko model.
 */
public class PGeckoModelLoader implements PModelLoader
{
	public static final PGeckoModelLoader INSTANCE = new PGeckoModelLoader();
	
	private static final Identifier ID = PLibDatabase.rl("gecko");
	
	private static final String MODEL_ROOT = "geckolib/models";
	private static final String ANIMATION_ROOT = "geckolib/animations";
	private static final String MODEL_EXTENSION = ".geo.json";
	private static final String ANIMATION_EXTENSION = ".animation.json";
	private static final String EVENTS_EXTENSION = ".events.json";
	private static final String ANIMATION_EVENTS_EXTENSION = ".animation_events.json";
	private static final String JSON_EXTENSION = ".json";
	
	/**
	 * Creates an instance of the enclosing type.
	 */
	private PGeckoModelLoader()
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
	 * Applies the item transform.
	 * @param poseStack the pose stack to use.
	 */
	@Override
	public void applyItemTransform(PoseStack poseStack)
	{
		poseStack.translate(0.5f, 0.51f, 0.5f);
	}
	
	/** Resolves a canonical model id to a GeckoLib model resource. */
	@Override
	public List<Identifier> physicalResourceCandidates(Identifier modelId)
	{
		return List.of(modelId.withPrefix(MODEL_ROOT + "/").withSuffix(MODEL_EXTENSION));
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
				orElseThrow(() -> new IllegalStateException("Registered GeckoLib model " + resource.modelId() +
						" has no resource; tried: " + physicalResourceCandidates(resource.modelId())));
		try
		{
			PModel model = PGeckoModelParser.parseModel(resourceManager.getResourceOrThrow(source).open());
			loadAnimations(resourceManager, source, model);
			return new PLoadedModel(resource.modelId(), source, model);
		}
		catch (IOException exception)
		{
			throw new RuntimeException("Can't load GeckoLib model " + resource.modelId() + " from " + source, exception);
		}
	}
	
	/**
	 * Loads the animations.
	 * @param resourceManager the resource manager to use.
	 * @param modelResource the model resource to use.
	 * @param model the model to use.
	 */
	private void loadAnimations(ResourceManager resourceManager, Identifier modelResource, PModel model) throws IOException
	{
		Optional<Identifier> animationResource = animationCandidates(modelResource).stream().
				filter(resource -> resourceManager.getResource(resource).isPresent()).
				findFirst();
		
		if (animationResource.isEmpty())
			return;
		
		model.animations.putAll(PGeckoModelParser.parseAnimations(
				resourceManager.getResourceOrThrow(animationResource.get()).open(),
				model));
		loadAnimationSidecar(resourceManager, animationResource.get(), model);
	}

	/**
	 * Loads the animation sidecar.
	 * @param resourceManager the resource manager to use.
	 * @param animationResource the animation resource to use.
	 * @param model the model to use.
	 */
	private void loadAnimationSidecar(ResourceManager resourceManager,
	                                 Identifier animationResource,
	                                 PModel model) throws IOException
	{
		Optional<Identifier> sidecarResource = sidecarCandidates(animationResource).stream().
				filter(resource -> resourceManager.getResource(resource).isPresent()).
				findFirst();
		if (sidecarResource.isEmpty())
			return;

		PAnimationSidecarParser.mergeSidecar(
				PAnimationSidecarParser.parseJson(resourceManager.getResourceOrThrow(sidecarResource.get()).open()),
				model.animations);
	}
	
	/**
	 * Performs the animation candidates operation.
	 * @param modelResource the model resource to use.
	 * @return the value produced by this operation.
	 */
	private List<Identifier> animationCandidates(Identifier modelResource)
	{
		String modelName = modelName(modelResource);
		String[] divided = modelName.split("/");
		String fileName = divided[divided.length - 1];
		
		List<Identifier> candidates = new ArrayList<>();
		candidates.add(modelResource.withPath(ANIMATION_ROOT + "/" + fileName + ANIMATION_EXTENSION));
		candidates.add(modelResource.withPath(ANIMATION_ROOT + "/" + fileName + JSON_EXTENSION));
		candidates.add(modelResource.withPath(ANIMATION_ROOT + "/" + modelName + ANIMATION_EXTENSION));
		candidates.add(modelResource.withPath(ANIMATION_ROOT + "/" + modelName + JSON_EXTENSION));
		return candidates;
	}

	/**
	 * Performs the sidecar candidates operation.
	 * @param animationResource the animation resource to use.
	 * @return the value produced by this operation.
	 */
	private List<Identifier> sidecarCandidates(Identifier animationResource)
	{
		String path = animationResource.getPath();
		String base = path.endsWith(ANIMATION_EXTENSION) ?
				path.substring(0, path.length() - ANIMATION_EXTENSION.length()) :
				path.substring(0, path.length() - JSON_EXTENSION.length());
		String fileName = base.substring(base.lastIndexOf('/') + 1);
		String root = base.substring(0, base.lastIndexOf('/'));

		List<Identifier> candidates = new ArrayList<>();
		candidates.add(animationResource.withPath(base + EVENTS_EXTENSION));
		candidates.add(animationResource.withPath(base + ANIMATION_EVENTS_EXTENSION));
		candidates.add(animationResource.withPath(root + "/events/" + fileName + EVENTS_EXTENSION));
		return candidates;
	}
	
	/**
	 * Performs the model name operation.
	 * @param modelResource the model resource to use.
	 * @return the value produced by this operation.
	 */
	private static String modelName(Identifier modelResource)
	{
		String path = modelResource.getPath();
		String modelName = path.substring(MODEL_ROOT.length() + 1);
		if (modelName.endsWith(MODEL_EXTENSION))
			return modelName.substring(0, modelName.length() - MODEL_EXTENSION.length());
		
		return modelName.substring(0, modelName.length() - JSON_EXTENSION.length());
	}
}
