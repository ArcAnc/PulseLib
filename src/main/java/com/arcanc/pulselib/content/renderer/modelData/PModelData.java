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


public class PModelData
{
	public static final ResourceLocation DEFAULT_MODEL_FORMAT = PGltfModelLoader.INSTANCE.id();
	
	private final ResourceLocation modelLocation;
	private final String modelType;
	private final ResourceLocation modelFormat;
	
	public PModelData(Builder builder)
	{
		this.modelLocation = builder.modelLocation;
		this.modelType = builder.modelType;
		this.modelFormat = builder.modelFormat;
	}
	
	protected static ResourceLocation generateDefaultModelLocation(ResourceLocation modelLocation, String type)
	{
		return generateDefaultModelLocation(modelLocation, type, DEFAULT_MODEL_FORMAT);
	}
	
	protected static ResourceLocation generateDefaultModelLocation(ResourceLocation modelLocation, String type, ResourceLocation modelFormat)
	{
		return PModelCache.getModelLoader(modelFormat).
				map(loader -> loader.defaultModelLocation(modelLocation, type)).
				orElseGet(() -> PGltfModelLoader.INSTANCE.defaultModelLocation(modelLocation, type));
	}
	
	public ResourceLocation getModelLocation()
	{
		return this.modelLocation;
	}
	
	public String getModelType()
	{
		return this.modelType;
	}
	
	public ResourceLocation getModelFormat()
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
		protected ResourceLocation modelLocation;
		protected String modelType;
		protected ResourceLocation modelFormat;
		
		public Builder(ResourceLocation modelLocation, String modelType)
		{
			this(modelLocation, modelType, DEFAULT_MODEL_FORMAT);
		}
		
		public Builder(ResourceLocation modelLocation, String modelType, ResourceLocation modelFormat)
		{
			this.modelType = modelType;
			this.modelFormat = modelFormat;
			this.modelLocation = normalizeModelLocation(modelLocation, modelType, modelFormat);
		}
		
		public PModelData build()
		{
			return new PModelData(this);
		}
		
		private static ResourceLocation normalizeModelLocation(ResourceLocation modelLocation, String modelType, ResourceLocation modelFormat)
		{
			if (modelType.isEmpty())
				return modelLocation;
			
			if (PModelCache.getModelLoaders().stream().anyMatch(loader -> loader.supports(modelLocation)))
				return modelLocation;
			
			return PModelData.generateDefaultModelLocation(modelLocation, modelType, modelFormat);
		}
	}
}
