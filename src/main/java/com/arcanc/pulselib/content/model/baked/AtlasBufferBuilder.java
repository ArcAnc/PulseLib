/**
 * @author ArcAnc
 * Created at: 04.04.2026
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

/**
 * Builds atlas buffer.
 */
public class AtlasBufferBuilder extends BufferBuilder
{
	private final TextureAtlasSprite sprite;
	
	/**
	 * Creates an instance of the enclosing type.
	 * @param buffer the buffer to use.
	 * @param mode the mode to use.
	 * @param format the format to use.
	 * @param sprite the sprite to use.
	 */
	public AtlasBufferBuilder(ByteBufferBuilder buffer, VertexFormat.Mode mode, VertexFormat format, TextureAtlasSprite sprite)
	{
		super(buffer, mode, format);
		this.sprite = sprite;
	}
	
	/**
	 * Sets the uv.
	 * @param u the u to use.
	 * @param v the v to use.
	 * @return the value produced by this operation.
	 */
	@Override
	public VertexConsumer setUv(float u, float v)
	{
		super.setUv(this.sprite.getU(u), this.sprite.getV(v));
		return this;
	}
}
