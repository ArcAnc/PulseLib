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
import com.arcanc.pulselib.data.PAnimationSidecarParser;
import com.arcanc.pulselib.data.PModelLoader;
import com.arcanc.pulselib.util.PLibDatabase;
import com.mojang.blaze3d.vertex.PoseStack;
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

/**
 * Loads gecko model.
 */
public class PGeckoModelLoader implements PModelLoader
{
	public static final PGeckoModelLoader INSTANCE = new PGeckoModelLoader();
	
	private static final ResourceLocation ID = PLibDatabase.rl("gecko");
	
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
	public ResourceLocation id()
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
	
	/**
	 * Performs the supports operation.
	 * @param modelPath the model path to use.
	 * @return the value produced by this operation.
	 */
	@Override
	public boolean supports(ResourceLocation modelPath)
	{
		String path = modelPath.getPath();
		return path.startsWith(MODEL_ROOT + "/") && path.endsWith(JSON_EXTENSION);
	}
	
	/**
	 * Performs the default model location operation.
	 * @param modelLocation the model location to use.
	 * @param modelType the model type to use.
	 * @return the value produced by this operation.
	 */
	@Override
	public ResourceLocation defaultModelLocation(ResourceLocation modelLocation, String modelType)
	{
		return modelLocation.withPrefix(MODEL_ROOT + "/" + modelType + "/").withSuffix(MODEL_EXTENSION);
	}

	/**
	 * Performs the model resource location operation.
	 * @param modelLocation the loader-relative model id.
	 * @return the resource-pack model location.
	 */
	@Override
	public ResourceLocation modelResourceLocation(ResourceLocation modelLocation)
	{
		return modelLocation.getPath().startsWith(MODEL_ROOT + "/") ? modelLocation : modelLocation.withPrefix(MODEL_ROOT + "/");
	}

	/**
	 * Normalizes a GeckoLib resource id, using {@code .geo.json} when no JSON extension was supplied.
	 *
	 * @param modelLocation the loader-relative model id.
	 * @return the normalized GeckoLib resource location.
	 */
	@Override
	public ResourceLocation normalizeModelResourceLocation(ResourceLocation modelLocation)
	{
		ResourceLocation resourceLocation = modelResourceLocation(modelLocation);
		return resourceLocation.getPath().endsWith(JSON_EXTENSION) ?
				resourceLocation : resourceLocation.withSuffix(MODEL_EXTENSION);
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
	                                       BiConsumer<ResourceLocation, PModel> elementConsumer)
	{
		return CompletableFuture.supplyAsync(
				() -> resourceManager.listResources(
						MODEL_ROOT,
						fileName -> fileName.toString().endsWith(JSON_EXTENSION)),
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
								PModel model = PGeckoModelParser.parseModel(resources.get(resource).open());
								loadAnimations(resourceManager, resource, model);
								return model;
							}
							catch (IOException e)
							{
								throw new RuntimeException("Can't load GeckoLib model " + resource, e);
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
	
	/**
	 * Loads the animations.
	 * @param resourceManager the resource manager to use.
	 * @param modelResource the model resource to use.
	 * @param model the model to use.
	 */
	private void loadAnimations(ResourceManager resourceManager, ResourceLocation modelResource, PModel model) throws IOException
	{
		Optional<ResourceLocation> animationResource = animationCandidates(modelResource).stream().
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
	                                 ResourceLocation animationResource,
	                                 PModel model) throws IOException
	{
		Optional<ResourceLocation> sidecarResource = sidecarCandidates(animationResource).stream().
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
	private List<ResourceLocation> animationCandidates(ResourceLocation modelResource)
	{
		String modelName = modelName(modelResource);
		String[] divided = modelName.split("/");
		String fileName = divided[divided.length - 1];
		
		List<ResourceLocation> candidates = new ArrayList<>();
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
	private List<ResourceLocation> sidecarCandidates(ResourceLocation animationResource)
	{
		String path = animationResource.getPath();
		String base = path.endsWith(ANIMATION_EXTENSION) ?
				path.substring(0, path.length() - ANIMATION_EXTENSION.length()) :
				path.substring(0, path.length() - JSON_EXTENSION.length());
		String fileName = base.substring(base.lastIndexOf('/') + 1);
		String root = base.substring(0, base.lastIndexOf('/'));

		List<ResourceLocation> candidates = new ArrayList<>();
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
	private static String modelName(ResourceLocation modelResource)
	{
		String path = modelResource.getPath();
		String modelName = path.substring(MODEL_ROOT.length() + 1);
		if (modelName.endsWith(MODEL_EXTENSION))
			return modelName.substring(0, modelName.length() - MODEL_EXTENSION.length());
		
		return modelName.substring(0, modelName.length() - JSON_EXTENSION.length());
	}
}
