/**
 * @author ArcAnc
 * Created at: 26.03.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.baked;


import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public class AtlasBufferBuilder extends BufferBuilder
{
	private final TextureAtlasSprite sprite;
	
	public AtlasBufferBuilder(ByteBufferBuilder buffer, VertexFormat.Mode mode, VertexFormat format, TextureAtlasSprite sprite)
	{
		super(buffer, mode, format);
		this.sprite = sprite;
	}
	
	@Override
	public VertexConsumer setUv(float u, float v)
	{
		super.setUv(this.sprite.getU(u), this.sprite.getV(v));
		return this;
	}
}
