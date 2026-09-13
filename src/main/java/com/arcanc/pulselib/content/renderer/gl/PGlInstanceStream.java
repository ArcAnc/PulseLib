/**
 * @author ArcAnc
 * Created at: 14.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.gl;

import com.arcanc.pulselib.content.renderer.PRenderQueue;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Matrix4f;
import org.lwjgl.opengl.ARBBufferStorage;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL42;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

public final class PGlInstanceStream
{
	public static final int STRIDE = 96;
	private static final int RING_SLOTS = 9;

	private int buffer = -1;
	private int capacity;
	private ByteBuffer staging;
	private ByteBuffer persistentMapping;
	private boolean persistent;

	/**
	 * Performs the upload operation.
	 * @param instances the instances to use.
	 * @param frameSlot the frame slot to use.
	 * @param persistent the persistent to use.
	 * @return the value produced by this operation.
	 */
	public Upload upload(List<PRenderQueue.InstanceData> instances, int frameSlot, boolean persistent)
	{
		int required = Math.max(STRIDE, instances.size() * STRIDE);
		if (persistent)
		{
			this.ensurePersistentCapacity(required);
			ByteBuffer target = this.persistentMapping.duplicate().order(ByteOrder.nativeOrder());
			target.position(frameSlot * this.capacity).limit((frameSlot + 1) * this.capacity);
			target = target.slice().order(ByteOrder.nativeOrder());
			for (PRenderQueue.InstanceData instance : instances)
				write(target, instance);
			GL42.glMemoryBarrier(ARBBufferStorage.GL_CLIENT_MAPPED_BUFFER_BARRIER_BIT);
			return new Upload(this.buffer, (long)frameSlot * this.capacity);
		}
		this.ensureCapacity(required);
		this.staging.clear();
		for (PRenderQueue.InstanceData instance : instances)
			write(this.staging, instance);
		this.staging.flip();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.buffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, this.staging, GL15.GL_STREAM_DRAW);
		return new Upload(this.buffer, 0L);
	}

	/**
	 * Performs the close operation.
	 */
	public void close()
	{
		this.closeBuffer();
		this.capacity = 0;
		if (this.staging != null)
			MemoryUtil.memFree(this.staging);
		this.staging = null;
	}

	/**
	 * Performs the ensure capacity operation.
	 * @param required the required to use.
	 */
	private void ensureCapacity(int required)
	{
		if (this.persistent)
		{
			this.closeBuffer();
		}
		if (this.buffer == -1)
			this.buffer = GL15.glGenBuffers();
		if (this.staging == null)
		{
			this.capacity = Math.max(required, 1024 * STRIDE);
			this.staging = MemoryUtil.memAlloc(this.capacity).order(ByteOrder.nativeOrder());
		}
		else if (required > this.capacity)
		{
			this.capacity = Math.max(required, this.capacity * 2);
			this.staging = MemoryUtil.memRealloc(this.staging, this.capacity).order(ByteOrder.nativeOrder());
		}
	}

	/**
	 * Performs the ensure persistent capacity operation.
	 * @param required the required to use.
	 */
	private void ensurePersistentCapacity(int required)
	{
		if (this.persistent && required <= this.capacity)
			return;
		this.closeBuffer();
		if (this.staging != null)
		{
			MemoryUtil.memFree(this.staging);
			this.staging = null;
		}
		this.capacity = Math.max(required, 1024 * STRIDE);
		this.buffer = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.buffer);
		int flags = GL30.GL_MAP_WRITE_BIT | ARBBufferStorage.GL_MAP_PERSISTENT_BIT | ARBBufferStorage.GL_MAP_COHERENT_BIT;
		ARBBufferStorage.glBufferStorage(GL15.GL_ARRAY_BUFFER, (long)this.capacity * RING_SLOTS, flags);
		this.persistentMapping = GL30.glMapBufferRange(GL15.GL_ARRAY_BUFFER, 0L, (long)this.capacity * RING_SLOTS, flags, null);
		if (this.persistentMapping == null)
			throw new IllegalStateException("Failed to persistently map PulseLib instance stream");
		this.persistentMapping.order(ByteOrder.nativeOrder());
		this.persistent = true;
	}

	/**
	 * Closes the buffer.
	 */
	private void closeBuffer()
	{
		if (this.buffer != -1)
		{
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.buffer);
			if (this.persistent)
				GL15.glUnmapBuffer(GL15.GL_ARRAY_BUFFER);
			GL15.glDeleteBuffers(this.buffer);
		}
		this.buffer = -1;
		this.persistent = false;
		this.persistentMapping = null;
	}

	/**
	 * Performs the write operation.
	 * @param target the target to use.
	 * @param instance the instance to use.
	 */
	private static void write(ByteBuffer target, PRenderQueue.InstanceData instance)
	{
		Matrix4f matrix = instance.posMatrix();
		target.putFloat(matrix.m00()).putFloat(matrix.m10()).putFloat(matrix.m20()).putFloat(matrix.m30());
		target.putFloat(matrix.m01()).putFloat(matrix.m11()).putFloat(matrix.m21()).putFloat(matrix.m31());
		target.putFloat(matrix.m02()).putFloat(matrix.m12()).putFloat(matrix.m22()).putFloat(matrix.m32());
		target.putFloat(ARGB.red(instance.packedColor()) / 255f);
		target.putFloat(ARGB.green(instance.packedColor()) / 255f);
		target.putFloat(ARGB.blue(instance.packedColor()) / 255f);
		target.putFloat(ARGB.alpha(instance.packedColor()) / 255f);
		target.putFloat(LightCoordsUtil.block(instance.packedLight()));
		target.putFloat(LightCoordsUtil.sky(instance.packedLight()));
		target.putFloat(instance.packedOverlay() & 0xFFFF);
		target.putFloat(instance.packedOverlay() >> 16 & 0xFFFF);
		target.putInt(instance.deformerOperationOffset());
		target.putInt(instance.deformerValueOffset());
		target.putInt(instance.deformerOperationCount());
		target.putInt(0);
	}

	public record Upload(int buffer, long offset)
	{
	}
}
