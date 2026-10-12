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
import com.arcanc.pulselib.data.PLoadedModel;
import com.arcanc.pulselib.data.PModelLoader;
import com.arcanc.pulselib.util.PLibDatabase;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Loads Gecko models and their animation sidecars.
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

	private PGeckoModelLoader()
	{
	}

	@Override
	public ResourceLocation id()
	{
		return ID;
	}

	@Override
	public void applyItemTransform(PoseStack poseStack)
	{
		poseStack.translate(0.5f, 0.51f, 0.5f);
	}

	@Override
	public List<ResourceLocation> modelResourceCandidates(ResourceLocation modelId)
	{
		return List.of(modelId.withPrefix(MODEL_ROOT + "/").withSuffix(MODEL_EXTENSION));
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
				PModel model = PGeckoModelParser.parseModel(resourceManager.getResourceOrThrow(source).open());
				loadAnimations(resourceManager, source, model);
				return new PLoadedModel(modelId, source, model);
			}
			catch (IOException exception)
			{
				throw new RuntimeException("Can't load Gecko model " + source + " for " + modelId, exception);
			}
		}, backgroundExecutor);
	}

	private void loadAnimations(ResourceManager resourceManager, ResourceLocation modelResource, PModel model) throws IOException
	{
		Optional<ResourceLocation> animationResource = animationCandidates(modelResource).stream()
				.filter(resource -> resourceManager.getResource(resource).isPresent())
				.findFirst();

		if (animationResource.isEmpty())
			return;

		model.animations.putAll(PGeckoModelParser.parseAnimations(
				resourceManager.getResourceOrThrow(animationResource.get()).open(), model));
		loadAnimationSidecar(resourceManager, animationResource.get(), model);
	}

	private void loadAnimationSidecar(ResourceManager resourceManager,
	                                  ResourceLocation animationResource,
	                                  PModel model) throws IOException
	{
		Optional<ResourceLocation> sidecarResource = sidecarCandidates(animationResource).stream()
				.filter(resource -> resourceManager.getResource(resource).isPresent())
				.findFirst();
		if (sidecarResource.isEmpty())
			return;

		PAnimationSidecarParser.mergeSidecar(
				PAnimationSidecarParser.parseJson(resourceManager.getResourceOrThrow(sidecarResource.get()).open()),
				model.animations);
	}

	private List<ResourceLocation> animationCandidates(ResourceLocation modelResource)
	{
		String modelName = modelName(modelResource);
		String fileName = modelName.substring(modelName.lastIndexOf('/') + 1);

		List<ResourceLocation> candidates = new ArrayList<>();
		candidates.add(modelResource.withPath(ANIMATION_ROOT + "/" + fileName + ANIMATION_EXTENSION));
		candidates.add(modelResource.withPath(ANIMATION_ROOT + "/" + fileName + JSON_EXTENSION));
		candidates.add(modelResource.withPath(ANIMATION_ROOT + "/" + modelName + ANIMATION_EXTENSION));
		candidates.add(modelResource.withPath(ANIMATION_ROOT + "/" + modelName + JSON_EXTENSION));
		return candidates;
	}

	private List<ResourceLocation> sidecarCandidates(ResourceLocation animationResource)
	{
		String path = animationResource.getPath();
		String base = path.endsWith(ANIMATION_EXTENSION)
				? path.substring(0, path.length() - ANIMATION_EXTENSION.length())
				: path.substring(0, path.length() - JSON_EXTENSION.length());
		String fileName = base.substring(base.lastIndexOf('/') + 1);
		String root = base.substring(0, base.lastIndexOf('/'));

		List<ResourceLocation> candidates = new ArrayList<>();
		candidates.add(animationResource.withPath(base + EVENTS_EXTENSION));
		candidates.add(animationResource.withPath(base + ANIMATION_EVENTS_EXTENSION));
		candidates.add(animationResource.withPath(root + "/events/" + fileName + EVENTS_EXTENSION));
		return candidates;
	}

	private static String modelName(ResourceLocation modelResource)
	{
		String path = modelResource.getPath();
		String modelName = path.substring(MODEL_ROOT.length() + 1);
		return modelName.substring(0, modelName.length() - MODEL_EXTENSION.length());
	}
}
