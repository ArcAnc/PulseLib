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

/**
 * Exposes index metadata and index-buffer upload needed by the legacy geometry backend.
 */
@Mixin (VertexBuffer.class)
public interface VertexBufferAccessor
{
	/**
	 * Updates the index count retained by the vanilla vertex buffer.
	 *
	 * @param index the index
	 */
	@Accessor ("indexCount")
	void pulselib$setIndexCount(int index);
	
	/**
	 * Returns the index count retained by the vanilla vertex buffer.
	 *
	 * @return the index count retained by the vanilla vertex buffer
	 */
	@Accessor ("indexCount")
	int pulselib$getIndexCount();
	
	/**
	 * Updates the index element type retained by the vanilla vertex buffer.
	 *
	 * @param type the type
	 */
	@Accessor("indexType")
	void pulselib$setIndexType(VertexFormat.IndexType type);
	
	/**
	 * Returns the index element type retained by the vanilla vertex buffer.
	 *
	 * @return the index element type retained by the vanilla vertex buffer, or {@code null} when the requested value is unavailable
	 */
	@Accessor("indexType")
	@Nullable
	VertexFormat.IndexType pulselib$getIndexType();
	
	/**
	 * Uploads the supplied index bytes using the vanilla vertex-buffer implementation.
	 *
	 * @param drawState the draw state
	 * @param buffer the packed binary data; may be {@code null}
	 * @return the automatic sequential-index storage selected by the vanilla upload, if applicable, or {@code null} when the requested value is unavailable
	 */
	@Invoker ("uploadIndexBuffer")
	@Nullable
	RenderSystem.AutoStorageIndexBuffer pulselib$UploadIndexBuffer(MeshData.DrawState drawState, @Nullable ByteBuffer buffer);
	
	/**
	 * Returns the primitive drawing mode retained by the vanilla vertex buffer.
	 *
	 * @return the primitive drawing mode retained by the vanilla vertex buffer, or {@code null} when the requested value is unavailable
	 */
	@Accessor("mode")
	@Nullable
	VertexFormat.Mode pulselib$getMode();
}
