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

/**
 * Provides support for default item model data.
 */
public class DefaultItemModelData extends PModelData
{
	/**
	 * Creates an instance of the enclosing type.
	 * @param builder the builder to use.
	 */
	public DefaultItemModelData(DefaultItemModelDataBuilder builder)
	{
		super(builder);
	}
	
/**
 * Builds default item model data.
 */
	public static class DefaultItemModelDataBuilder extends Builder
	{
		private final Identifier shortModelLocation;
		
		/**
		 * Creates an instance of the enclosing type.
		 * @param modelLocation the model location to use.
		 */
		public DefaultItemModelDataBuilder(Identifier modelLocation)
		{
			this(modelLocation, PModelData.DEFAULT_MODEL_FORMAT);
		}
		
		/**
		 * Creates an instance of the enclosing type.
		 * @param modelLocation the model location to use.
		 * @param modelFormat the model format to use.
		 */
		public DefaultItemModelDataBuilder(Identifier modelLocation, Identifier modelFormat)
		{
			super(modelLocation, "item");
			this.shortModelLocation = modelLocation;
			this.modelFormat = modelFormat;
			this.modelLocation = PModelData.generateDefaultModelLocation(modelLocation, this.modelType, this.modelFormat);
		}
		
		/**
		 * Performs the build operation.
		 * @return the value produced by this operation.
		 */
		@Override
		public DefaultItemModelData build()
		{
			return new DefaultItemModelData(this);
		}
	}
}
