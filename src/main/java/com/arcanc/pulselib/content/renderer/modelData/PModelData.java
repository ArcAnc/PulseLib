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
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PModelData
{
	public static final Identifier DEFAULT_MODEL_FORMAT = PGltfModelLoader.INSTANCE.id();
	
	public static final MapCodec<PModelData> CODEC = RecordCodecBuilder.mapCodec(instance ->
			instance.group(
					Identifier.CODEC.fieldOf("model_location").forGetter(PModelData :: getModelLocation),
					Codec.STRING.optionalFieldOf("model_type", "").forGetter(PModelData :: getModelType),
					Identifier.CODEC.optionalFieldOf("model_format", DEFAULT_MODEL_FORMAT).forGetter(PModelData :: getModelFormat),
					Identifier.CODEC.listOf().fieldOf("textures").forGetter(arcModelData -> new ArrayList<>(arcModelData.textures.values()))
			).apply(instance, (identifier, type, format, identifiers) ->
			{
				Builder builder = new Builder(identifier, type, format);
				for (Identifier texture : identifiers)
				{
					builder.addTexture(texture);
				}
				return builder.build();
			}));
	
	private final Identifier modelLocation;
	private final String modelType;
	private final Identifier modelFormat;
	private final Map<String, Identifier> textures;
	
	/**
	 * Creates an instance of the enclosing type.
	 * @param builder the builder to use.
	 */
	public PModelData(Builder builder)
	{
		this.modelLocation = builder.modelLocation;
		this.modelType = builder.modelType;
		this.modelFormat = builder.modelFormat;
		this.textures = new Object2ObjectOpenHashMap<>();
		for (Pair<String, Identifier> texture : builder.textures)
			this.textures.put(texture.getFirst(), texture.getSecond());
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
	 * Performs the generate default texture location operation.
	 * @param textureLocation the texture location to use.
	 * @param modelLocation the model location to use.
	 * @param type the type to use.
	 * @return the value produced by this operation.
	 */
	protected static Identifier generateDefaultTextureLocation(Identifier textureLocation, String modelLocation, String type)
	{
		return generateDefaultTextureLocation(textureLocation, Identifier.withDefaultNamespace(modelLocation), type, DEFAULT_MODEL_FORMAT);
	}
	
	/**
	 * Performs the generate default texture location operation.
	 * @param textureLocation the texture location to use.
	 * @param modelLocation the model location to use.
	 * @param type the type to use.
	 * @param modelFormat the model format to use.
	 * @return the value produced by this operation.
	 */
	protected static Identifier generateDefaultTextureLocation(Identifier textureLocation, Identifier modelLocation, String type, Identifier modelFormat)
	{
		return PModelCache.getModelLoader(modelFormat).
				map(loader -> loader.defaultTextureLocation(textureLocation, modelLocation, type)).
				orElseGet(() -> PGltfModelLoader.INSTANCE.defaultTextureLocation(textureLocation, modelLocation, type));
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
	 * Returns the texture by name.
	 * @param name the name to use.
	 * @return the value produced by this operation.
	 */
	public Identifier getTextureByName(String name)
	{
		if (this.textures.get(name) == null)
		{
			Identifier texturePath = PModelCache.resolveTextureLocation(this.modelLocation, name);
			this.textures.put(name, texturePath);
		}
		return this.textures.getOrDefault(name, TextureManager.INTENTIONAL_MISSING_TEXTURE);
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
	
	public static class Builder
	{
		protected Identifier modelLocation;
		protected String modelType;
		protected Identifier modelFormat;
		protected List<Pair<String, Identifier>> textures;
		
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
			this.textures = new ArrayList<>();
		}
		
		/**
		 * Adds the texture.
		 * @param texturePath the texture path to use.
		 * @return the value produced by this operation.
		 */
		public Builder addTexture(Identifier texturePath)
		{
			String[] parsedName = texturePath.getPath().split("/");
			String textureName = parsedName[parsedName.length - 1];
			return this.addTexture(textureName.contains(".png") ? textureName.substring(0, textureName.length() - 4): textureName, texturePath);
		}
		
		/**
		 * Adds the texture.
		 * @param textureName the texture name to use.
		 * @param textureLocation the texture location to use.
		 * @return the value produced by this operation.
		 */
		public Builder addTexture(String textureName, Identifier textureLocation)
		{
			this.textures.add(new Pair<>(textureName, textureLocation));
			return this;
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
