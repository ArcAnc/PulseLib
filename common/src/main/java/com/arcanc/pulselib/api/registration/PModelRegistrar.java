/**
 * @author ArcAnc
 * Created at: 12.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.api.registration;

import com.arcanc.pulselib.content.model.PTextureReference;
import com.arcanc.pulselib.content.model.resource.PModelResource;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.data.gltf.PGltfModelLoader;
import com.arcanc.pulselib.util.PModelCache;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class PModelRegistrar extends PulseRegistrar
{
	private final Map<ResourceLocation, ModelRegistration> models = new LinkedHashMap<>();

	public ModelRegistration model(PModelData data)
	{
		return model(data.getModelId(), data.getModelLoaderId());
	}

	public ModelRegistration model(ResourceLocation id)
	{
		return model(id, PGltfModelLoader.INSTANCE.id());
	}

	public ModelRegistration model(ResourceLocation id, ResourceLocation loader)
	{
		checkOpen();
		Objects.requireNonNull(id, "model id");
		Objects.requireNonNull(loader, "loader id");
		PModelCache.getModelLoader(loader).
				orElseThrow(() -> new IllegalStateException("No model loader registered for " + loader));
		ModelRegistration registration = this.models.computeIfAbsent(id, key -> new ModelRegistration(key, loader));
		if (!registration.loader.equals(loader))
			throw new IllegalStateException("Conflicting model loaders registered for " + id);
		return registration;
	}

	/**
	 * Validates all changes before applying a plugin's declarations.
	 */
	public void apply(Map<ResourceLocation, PModelResource> target)
	{
		checkOpen();
		Map<ResourceLocation, PModelResource> staged = new LinkedHashMap<>();
		this.models.forEach((id, registration) ->
		{
			Map<String, ResourceLocation> textures = new LinkedHashMap<>();
			PModelResource previous = target.get(id);
			if (previous != null)
			{
				if (!previous.modelLoaderId().equals(registration.loader))
					throw new IllegalStateException("Conflicting model loaders registered for " + id);
				textures.putAll(previous.textures());
			}
			registration.textures.forEach((reference, texture) ->
			{
				ResourceLocation old = textures.putIfAbsent(reference, texture);
				if (old != null && !old.equals(texture))
					throw new IllegalStateException("Conflicting texture for model " + id + ": " + reference);
			});
			staged.put(id, new PModelResource(id, registration.loader, textures));
		});
		target.putAll(staged);
		close();
	}

	public final class ModelRegistration
	{
		private final ResourceLocation id;
		private final ResourceLocation loader;
		private final Map<String, ResourceLocation> textures = new LinkedHashMap<>();

		private ModelRegistration(ResourceLocation id, ResourceLocation loader)
		{
			this.id = id;
			this.loader = loader;
		}

		public ModelRegistration texture(String reference, ResourceLocation texture)
		{
			checkOpen();
			String normalized = PTextureReference.normalize(reference);
			ResourceLocation old = this.textures.putIfAbsent(normalized, Objects.requireNonNull(texture));
			if (old != null && !old.equals(texture))
				throw new IllegalStateException("Conflicting texture for model " + this.id + ": " + normalized);
			return this;
		}
	}
}
