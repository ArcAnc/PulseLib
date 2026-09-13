/**
 * @author ArcAnc
 * Created at: 20.05.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.modelData;


import net.minecraft.resources.Identifier;

/**
 * Provides support for default entity layer model data.
 */
public class DefaultEntityLayerModelData extends PModelData
{
	/**
	 * Creates an instance of the enclosing type.
	 * @param builder the builder to use.
	 */
	public DefaultEntityLayerModelData(DefaultEntityLayerModelDataBuilder builder)
	{
		super(builder);
	}
	
/**
 * Builds default entity layer model data.
 */
	public static class DefaultEntityLayerModelDataBuilder extends Builder
	{
		private final Identifier shortModelLocation;
		
		/**
		 * Creates an instance of the enclosing type.
		 * @param entityType the entity type to use.
		 * @param shortModelLocation the short model location to use.
		 */
		public DefaultEntityLayerModelDataBuilder(Identifier entityType, Identifier shortModelLocation)
		{
			this(entityType, shortModelLocation, PModelData.DEFAULT_MODEL_FORMAT);
		}
		
		/**
		 * Creates an instance of the enclosing type.
		 * @param entityType the entity type to use.
		 * @param shortModelLocation the short model location to use.
		 * @param modelFormat the model format to use.
		 */
		public DefaultEntityLayerModelDataBuilder(Identifier entityType, Identifier shortModelLocation, Identifier modelFormat)
		{
			super(shortModelLocation, "entity");
			this.shortModelLocation = shortModelLocation.withPrefix(entityType.getPath() + "/");
			this.modelFormat = modelFormat;
			this.modelLocation = PModelData.generateDefaultModelLocation(this.shortModelLocation, this.modelType, this.modelFormat);
		}
		
		/**
		 * Performs the build operation.
		 * @return the value produced by this operation.
		 */
		@Override
		public DefaultEntityLayerModelData build()
		{
			return new DefaultEntityLayerModelData(this);
		}
	}
}
