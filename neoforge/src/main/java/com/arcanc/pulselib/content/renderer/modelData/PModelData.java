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
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Renderer-facing description of a model.
 *
 * <p>The model id is canonical: it never contains a loader root or a physical
 * file extension. Loaders resolve that id to their own resource-pack paths.
 * The class remains extensible so specialised data may choose or expose models
 * differently.</p>
 */
public class PModelData
{
	public static final Identifier DEFAULT_MODEL_LOADER = PGltfModelLoader.INSTANCE.id();

	public static final MapCodec<PModelData> CODEC = RecordCodecBuilder.mapCodec(instance ->
			instance.group(
					Identifier.CODEC.fieldOf("model_id").forGetter(PModelData :: getModelId),
					Identifier.CODEC.optionalFieldOf("model_loader", DEFAULT_MODEL_LOADER).forGetter(PModelData :: getModelLoaderId)
			).apply(instance, PModelData :: direct));

	private final Identifier modelId;
	private final Identifier modelLoaderId;

	/**
	 * Creates model data for one canonical model. Subclasses may override lookup
	 * methods to support multiple or dynamically selected models.
	 *
	 * @param modelId the canonical model id.
	 * @param modelLoaderId the loader responsible for the model.
	 */
	protected PModelData(Identifier modelId, Identifier modelLoaderId)
	{
		this.modelId = Objects.requireNonNull(modelId);
		this.modelLoaderId = Objects.requireNonNull(modelLoaderId);
	}

	/** Creates data for a canonical block model. */
	public static PModelData block(Identifier modelId)
	{
		return block(modelId, DEFAULT_MODEL_LOADER);
	}

	/** Creates data for a canonical block model using a custom loader. */
	public static PModelData block(Identifier modelId, Identifier modelLoaderId)
	{
		return direct(withType(modelId, "block"), modelLoaderId);
	}

	/** Creates data for a canonical item model. */
	public static PModelData item(Identifier modelId)
	{
		return item(modelId, DEFAULT_MODEL_LOADER);
	}

	/** Creates data for a canonical item model using a custom loader. */
	public static PModelData item(Identifier modelId, Identifier modelLoaderId)
	{
		return direct(withType(modelId, "item"), modelLoaderId);
	}

	/** Creates data for a canonical entity model. */
	public static PModelData entity(Identifier modelId)
	{
		return entity(modelId, DEFAULT_MODEL_LOADER);
	}

	/** Creates data for a canonical entity model using a custom loader. */
	public static PModelData entity(Identifier modelId, Identifier modelLoaderId)
	{
		return direct(withType(modelId, "entity"), modelLoaderId);
	}

	/**
	 * Creates data for an entity layer model. The layer id is scoped beneath the
	 * entity type, for example {@code entity/robot/armor}.
	 *
	 * @param entityType the entity type whose layer is being rendered.
	 * @param modelId the layer id relative to the entity type.
	 * @return model data for the glTF loader.
	 */
	public static PModelData entityLayer(Identifier entityType, Identifier modelId)
	{
		return entityLayer(entityType, modelId, DEFAULT_MODEL_LOADER);
	}

	/**
	 * Creates data for an entity layer model using a custom loader.
	 *
	 * @param entityType the entity type whose layer is being rendered.
	 * @param modelId the layer id relative to the entity type.
	 * @param modelLoaderId the loader responsible for the model.
	 * @return model data.
	 */
	public static PModelData entityLayer(Identifier entityType, Identifier modelId, Identifier modelLoaderId)
	{
		Objects.requireNonNull(entityType);
		Objects.requireNonNull(modelId);
		return entity(modelId.withPrefix(entityType.getPath() + "/"), modelLoaderId);
	}

	/** Creates data for an already explicit canonical model id. */
	public static PModelData direct(Identifier modelId)
	{
		return direct(modelId, DEFAULT_MODEL_LOADER);
	}

	/** Creates data for an already explicit canonical model id using a custom loader. */
	public static PModelData direct(Identifier modelId, Identifier modelLoaderId)
	{
		return new PModelData(modelId, modelLoaderId);
	}

	private static Identifier withType(Identifier modelId, String type)
	{
		Objects.requireNonNull(modelId);
		return modelId.getPath().startsWith(type + "/") ? modelId : modelId.withPrefix(type + "/");
	}

	/** Returns the canonical model id. */
	public Identifier getModelId()
	{
		return this.modelId;
	}

	/** Returns the loader responsible for this model. */
	public Identifier getModelLoaderId()
	{
		return this.modelLoaderId;
	}

	/**
	 * Returns the baked model selected by this data. Subclasses can override
	 * this method for multi-model or dynamic lookup behaviour.
	 */
	public @Nullable PBakedModel getModel()
	{
		if (PModelCache.getModels() == null)
			return null;
		return PModelCache.getModels().get(this.modelId);
	}
}
