/**
 * @author ArcAnc
 * Created at: 27.01.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.modelData;

import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.data.gltf.PGltfModelLoader;
import com.arcanc.pulselib.util.PModelCache;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Renderer-facing description of a logical model.
 *
 * <p>The model id is canonical: it never includes a loader root or a physical
 * file extension. A {@link com.arcanc.pulselib.data.PModelLoader} resolves it
 * to the resource-pack file it loads.</p>
 */
public class PModelData
{
	public static final ResourceLocation DEFAULT_MODEL_LOADER_ID = PGltfModelLoader.INSTANCE.id();

	private final ResourceLocation modelId;
	private final ResourceLocation modelLoaderId;

	/**
	 * Creates model data for a canonical model id. Subclasses can override
	 * lookup behavior, or expose several model ids, without exposing mutable
	 * state from this class.
	 */
	protected PModelData(ResourceLocation modelId, ResourceLocation modelLoaderId)
	{
		this.modelId = Objects.requireNonNull(modelId);
		this.modelLoaderId = Objects.requireNonNull(modelLoaderId);
	}

	public static PModelData direct(ResourceLocation modelId)
	{
		return direct(modelId, DEFAULT_MODEL_LOADER_ID);
	}

	public static PModelData direct(ResourceLocation modelId, ResourceLocation modelLoaderId)
	{
		return new PModelData(modelId, modelLoaderId);
	}

	public static PModelData block(ResourceLocation modelId)
	{
		return block(modelId, DEFAULT_MODEL_LOADER_ID);
	}

	public static PModelData block(ResourceLocation modelId, ResourceLocation modelLoaderId)
	{
		return typed(modelId, "block", modelLoaderId);
	}

	public static PModelData item(ResourceLocation modelId)
	{
		return item(modelId, DEFAULT_MODEL_LOADER_ID);
	}

	public static PModelData item(ResourceLocation modelId, ResourceLocation modelLoaderId)
	{
		return typed(modelId, "item", modelLoaderId);
	}

	public static PModelData entity(ResourceLocation modelId)
	{
		return entity(modelId, DEFAULT_MODEL_LOADER_ID);
	}

	public static PModelData entity(ResourceLocation modelId, ResourceLocation modelLoaderId)
	{
		return typed(modelId, "entity", modelLoaderId);
	}

	public static PModelData entityLayer(ResourceLocation entityType, ResourceLocation modelId)
	{
		return entityLayer(entityType, modelId, DEFAULT_MODEL_LOADER_ID);
	}

	public static PModelData entityLayer(ResourceLocation entityType,
	                                     ResourceLocation modelId,
	                                     ResourceLocation modelLoaderId)
	{
		return entity(modelId.withPrefix(Objects.requireNonNull(entityType).getPath() + "/"), modelLoaderId);
	}

	private static PModelData typed(ResourceLocation modelId, String type, ResourceLocation modelLoaderId)
	{
		return new PModelData(Objects.requireNonNull(modelId).withPrefix(type + "/"), modelLoaderId);
	}

	/**
	 * Returns the canonical renderer-facing model id.
	 */
	public ResourceLocation getModelId()
	{
		return this.modelId;
	}

	/**
	 * Returns the loader that resolves this model id to a physical resource.
	 */
	public ResourceLocation getModelLoaderId()
	{
		return this.modelLoaderId;
	}

	/**
	 * Looks up this data's primary baked model. Subclasses may override this to
	 * select dynamically or to expose a different model arrangement.
	 */
	public @Nullable PBakedModel getModel()
	{
		if (PModelCache.getModels() == null)
			return null;

		return PModelCache.getModels().get(this.modelId);
	}
}
