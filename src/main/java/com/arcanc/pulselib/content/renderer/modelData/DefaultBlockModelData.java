/**
 * @author ArcAnc
 * Created at: 28.02.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.modelData;


import net.minecraft.resources.ResourceLocation;

public class DefaultBlockModelData extends PModelData
{
	public DefaultBlockModelData(DefaultBlockModelDataBuilder builder)
	{
		super(builder);
	}
	
	public static class DefaultBlockModelDataBuilder extends Builder
	{
		public DefaultBlockModelDataBuilder(ResourceLocation modelLocation)
		{
			this(modelLocation, PModelData.DEFAULT_MODEL_FORMAT);
		}
		
		public DefaultBlockModelDataBuilder(ResourceLocation modelLocation, ResourceLocation modelFormat)
		{
			super(modelLocation, "block");
			this.modelFormat = modelFormat;
			this.modelLocation = PModelData.generateDefaultModelLocation(modelLocation, this.modelType, this.modelFormat);
		}
		
		@Override
		public DefaultBlockModelData build()
		{
			return new DefaultBlockModelData(this);
		}
	}
}
