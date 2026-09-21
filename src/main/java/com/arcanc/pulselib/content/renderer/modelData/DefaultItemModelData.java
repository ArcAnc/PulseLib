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

public class DefaultItemModelData extends PModelData
{
	public DefaultItemModelData(DefaultItemModelDataBuilder builder)
	{
		super(builder);
	}
	
	public static class DefaultItemModelDataBuilder extends Builder
	{
		private final Identifier shortModelLocation;
		
		public DefaultItemModelDataBuilder(Identifier modelLocation)
		{
			this(modelLocation, PModelData.DEFAULT_MODEL_FORMAT);
		}
		
		public DefaultItemModelDataBuilder(Identifier modelLocation, Identifier modelFormat)
		{
			super(modelLocation, "item");
			this.shortModelLocation = modelLocation;
			this.modelFormat = modelFormat;
			this.modelLocation = PModelData.generateDefaultModelLocation(modelLocation, this.modelType, this.modelFormat);
		}
		
		@Override
		public DefaultItemModelData build()
		{
			return new DefaultItemModelData(this);
		}
	}
}
