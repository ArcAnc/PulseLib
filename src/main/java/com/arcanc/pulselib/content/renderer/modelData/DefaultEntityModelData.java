/**
 * @author ArcAnc
 * Created at: 28.02.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.modelData;


import net.minecraft.resources.Identifier;

public class DefaultEntityModelData extends PModelData
{
	/**
	 * Creates an instance of the enclosing type.
	 * @param builder the builder to use.
	 */
	public DefaultEntityModelData(DefaultEntityModelDataBuilder builder)
	{
		super(builder);
	}
	
	public static class DefaultEntityModelDataBuilder extends Builder
	{
		private final Identifier shortModelLocation;
		
		/**
		 * Creates an instance of the enclosing type.
		 * @param modelLocation the model location to use.
		 */
		public DefaultEntityModelDataBuilder(Identifier modelLocation)
		{
			this(modelLocation, PModelData.DEFAULT_MODEL_FORMAT);
		}
		
		/**
		 * Creates an instance of the enclosing type.
		 * @param modelLocation the model location to use.
		 * @param modelFormat the model format to use.
		 */
		public DefaultEntityModelDataBuilder(Identifier modelLocation, Identifier modelFormat)
		{
			super(modelLocation, "entity");
			this.shortModelLocation = modelLocation;
			this.modelFormat = modelFormat;
			this.modelLocation = PModelData.generateDefaultModelLocation(modelLocation, this.modelType, this.modelFormat);
		}
		
		/**
		 * Adds the texture.
		 * @param texturePath the texture path to use.
		 * @return the value produced by this operation.
		 */
		@Override
		public DefaultEntityModelDataBuilder addTexture(Identifier texturePath)
		{
			super.addTexture(PModelData.generateDefaultTextureLocation(texturePath, this.shortModelLocation, this.modelType, this.modelFormat));
			return this;
		}
		
		/**
		 * Performs the build operation.
		 * @return the value produced by this operation.
		 */
		@Override
		public DefaultEntityModelData build()
		{
			return new DefaultEntityModelData(this);
		}
	}
}
