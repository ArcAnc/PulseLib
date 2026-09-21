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

public class DefaultEntityLayerModelData extends PModelData
{
	public DefaultEntityLayerModelData(DefaultEntityLayerModelDataBuilder builder)
	{
		super(builder);
	}
	
	public static class DefaultEntityLayerModelDataBuilder extends Builder
	{
		private final Identifier shortModelLocation;
		
		public DefaultEntityLayerModelDataBuilder(Identifier entityType, Identifier shortModelLocation)
		{
			this(entityType, shortModelLocation, PModelData.DEFAULT_MODEL_FORMAT);
		}
		
		public DefaultEntityLayerModelDataBuilder(Identifier entityType, Identifier shortModelLocation, Identifier modelFormat)
		{
			super(shortModelLocation, "entity");
			this.shortModelLocation = shortModelLocation.withPrefix(entityType.getPath() + "/");
			this.modelFormat = modelFormat;
			this.modelLocation = PModelData.generateDefaultModelLocation(this.shortModelLocation, this.modelType, this.modelFormat);
		}
		
		@Override
		public DefaultEntityLayerModelData build()
		{
			return new DefaultEntityLayerModelData(this);
		}
	}
}
