/**
 * @author ArcAnc
 * Created at: 07.03.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;


import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;

@Mixin (VertexBuffer.class)
public interface VertexBufferAccessor
{
	@Accessor ("indexCount")
	void pulselib$setIndexCount(int index);
	
	@Accessor ("indexCount")
	int pulselib$getIndexCount();
	
	@Accessor("indexType")
	void pulselib$setIndexType(VertexFormat.IndexType type);
	
	@Accessor("indexType")
	VertexFormat.IndexType pulselib$getIndexType();
	
	@Invoker ("uploadIndexBuffer")
	@Nullable
	RenderSystem.AutoStorageIndexBuffer pulselib$UploadIndexBuffer(MeshData.DrawState drawState, @Nullable ByteBuffer buffer);
	
	@Accessor("mode")
	VertexFormat.Mode pulselib$getMode();
}
