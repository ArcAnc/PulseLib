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
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Provides support for model data.
 */
public class PModelData
{
	public static final Identifier DEFAULT_MODEL_FORMAT = PGltfModelLoader.INSTANCE.id();
	
	public static final MapCodec<PModelData> CODEC = RecordCodecBuilder.mapCodec(instance ->
			instance.group(
					Identifier.CODEC.fieldOf("model_location").forGetter(PModelData :: getModelLocation),
					Codec.STRING.optionalFieldOf("model_type", "").forGetter(PModelData :: getModelType),
					Identifier.CODEC.optionalFieldOf("model_format", DEFAULT_MODEL_FORMAT).forGetter(PModelData :: getModelFormat)
			).apply(instance, (identifier, type, format) -> new Builder(identifier, type, format).build()));
	
	private final Identifier modelLocation;
	private final String modelType;
	private final Identifier modelFormat;
	
	/**
	 * Creates an instance of the enclosing type.
	 * @param builder the builder to use.
	 */
	public PModelData(Builder builder)
	{
		this.modelLocation = builder.modelLocation;
		this.modelType = builder.modelType;
		this.modelFormat = builder.modelFormat;
	}
	
	/**
	 * Performs the generate default model location operation.
	 * @param modelLocation the model location to use.
	 * @param type the type to use.
	 * @return the value produced by this operation.
	 */
	protected static Identifier generateDefaultModelLocation(Identifier modelLocation, String type)
	{
		return generateDefaultModelLocation(modelLocation, type, DEFAULT_MODEL_FORMAT);
	}
	
	/**
	 * Performs the generate default model location operation.
	 * @param modelLocation the model location to use.
	 * @param type the type to use.
	 * @param modelFormat the model format to use.
	 * @return the value produced by this operation.
	 */
	protected static Identifier generateDefaultModelLocation(Identifier modelLocation, String type, Identifier modelFormat)
	{
		return PModelCache.getModelLoader(modelFormat).
				map(loader -> loader.defaultModelLocation(modelLocation, type)).
				orElseGet(() -> PGltfModelLoader.INSTANCE.defaultModelLocation(modelLocation, type));
	}
	
	/**
	 * Returns the model location.
	 * @return the value produced by this operation.
	 */
	public Identifier getModelLocation()
	{
		return this.modelLocation;
	}
	
	/**
	 * Returns the model type.
	 * @return the value produced by this operation.
	 */
	public String getModelType()
	{
		return this.modelType;
	}
	
	/**
	 * Returns the model format.
	 * @return the value produced by this operation.
	 */
	public Identifier getModelFormat()
	{
		return this.modelFormat;
	}
	
	/**
	 * Returns the model.
	 * @return the value produced by this operation.
	 */
	public @Nullable PBakedModel getModel()
	{
		if (PModelCache.getModels() == null)
			return null;
		return PModelCache.getModels().get(this.modelLocation);
	}
	
/**
 * Builds builder.
 */
	public static class Builder
	{
		protected Identifier modelLocation;
		protected String modelType;
		protected Identifier modelFormat;
		
		/**
		 * Creates an instance of the enclosing type.
		 * @param modelLocation the model location to use.
		 * @param modelType the model type to use.
		 */
		public Builder(Identifier modelLocation, String modelType)
		{
			this(modelLocation, modelType, DEFAULT_MODEL_FORMAT);
		}
		
		/**
		 * Creates an instance of the enclosing type.
		 * @param modelLocation the model location to use.
		 * @param modelType the model type to use.
		 * @param modelFormat the model format to use.
		 */
		public Builder(Identifier modelLocation, String modelType, Identifier modelFormat)
		{
			this.modelType = modelType;
			this.modelFormat = modelFormat;
			this.modelLocation = normalizeModelLocation(modelLocation, modelType, modelFormat);
		}
		
		/**
		 * Performs the build operation.
		 * @return the value produced by this operation.
		 */
		public PModelData build()
		{
			return new PModelData(this);
		}
		
		/**
		 * Performs the normalize model location operation.
		 * @param modelLocation the model location to use.
		 * @param modelType the model type to use.
		 * @param modelFormat the model format to use.
		 * @return the value produced by this operation.
		 */
		private static Identifier normalizeModelLocation(Identifier modelLocation, String modelType, Identifier modelFormat)
		{
			if (modelType.isEmpty())
				return modelLocation;
			
			if (PModelCache.getModelLoaders().stream().anyMatch(loader -> loader.supports(modelLocation)))
				return modelLocation;
			
			return PModelData.generateDefaultModelLocation(modelLocation, modelType, modelFormat);
		}
	}
}
