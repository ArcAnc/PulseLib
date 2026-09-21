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
	
	public PModelData(Builder builder)
	{
		this.modelLocation = builder.modelLocation;
		this.modelType = builder.modelType;
		this.modelFormat = builder.modelFormat;
	}
	
	protected static Identifier generateDefaultModelLocation(Identifier modelLocation, String type)
	{
		return generateDefaultModelLocation(modelLocation, type, DEFAULT_MODEL_FORMAT);
	}
	
	protected static Identifier generateDefaultModelLocation(Identifier modelLocation, String type, Identifier modelFormat)
	{
		return PModelCache.getModelLoader(modelFormat).
				map(loader -> loader.defaultModelLocation(modelLocation, type)).
				orElseGet(() -> PGltfModelLoader.INSTANCE.defaultModelLocation(modelLocation, type));
	}
	
	public Identifier getModelLocation()
	{
		return this.modelLocation;
	}
	
	public String getModelType()
	{
		return this.modelType;
	}
	
	public Identifier getModelFormat()
	{
		return this.modelFormat;
	}
	
	public @Nullable PBakedModel getModel()
	{
		if (PModelCache.getModels() == null)
			return null;
		return PModelCache.getModels().get(this.modelLocation);
	}
	
	public static class Builder
	{
		protected Identifier modelLocation;
		protected String modelType;
		protected Identifier modelFormat;
		
		public Builder(Identifier modelLocation, String modelType)
		{
			this(modelLocation, modelType, DEFAULT_MODEL_FORMAT);
		}
		
		public Builder(Identifier modelLocation, String modelType, Identifier modelFormat)
		{
			this.modelType = modelType;
			this.modelFormat = modelFormat;
			this.modelLocation = normalizeModelLocation(modelLocation, modelType, modelFormat);
		}
		
		public PModelData build()
		{
			return new PModelData(this);
		}
		
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
