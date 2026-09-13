/**
 * @author ArcAnc
 * Created at: 14.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.gl;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL40;
import org.lwjgl.opengl.ARBBufferStorage;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

public final class PGlIndirectStream
{
	public static final int STRIDE = Integer.BYTES * 5;
	private static final int RING_SLOTS = 9;

	private int buffer = -1;
	private int capacity;
	private ByteBuffer staging;
	private ByteBuffer persistentMapping;
	private boolean persistent;
	private int frameSlot;
	private int cursor;

	/**
	 * Performs the begin operation.
	 * @param maximumCommands the maximum commands to use.
	 * @param frameSlot the frame slot to use.
	 * @param persistent the persistent to use.
	 */
	public void begin(int maximumCommands, int frameSlot, boolean persistent)
	{
		this.cursor = 0;
		this.frameSlot = frameSlot;
		if (persistent)
			this.ensurePersistentCapacity(Math.max(STRIDE, maximumCommands * STRIDE));
	}

	/**
	 * Performs the upload operation.
	 * @param commands the commands to use.
	 * @return the value produced by this operation.
	 */
	public Upload upload(List<Command> commands)
	{
		int required = Math.max(STRIDE, commands.size() * STRIDE);
		if (this.persistent)
		{
			if (this.cursor + required > this.capacity)
				throw new IllegalStateException("Indirect command stream exceeded its reserved frame page");
			long offset = (long)this.frameSlot * this.capacity + this.cursor;
			ByteBuffer target = this.persistentMapping.duplicate().order(ByteOrder.nativeOrder());
			target.position(Math.toIntExact(offset)).limit(Math.toIntExact(offset) + required);
			write(target.slice().order(ByteOrder.nativeOrder()), commands);
			this.cursor += required;
			GL42.glMemoryBarrier(ARBBufferStorage.GL_CLIENT_MAPPED_BUFFER_BARRIER_BIT);
			GL15.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, this.buffer);
			return new Upload(this.buffer, offset);
		}
		this.ensureCapacity(required);
		write(this.staging.clear(), commands);
		this.staging.flip();
		GL15.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, this.buffer);
		GL15.glBufferData(GL40.GL_DRAW_INDIRECT_BUFFER, this.staging, GL15.GL_STREAM_DRAW);
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
			this.closeBuffer();
		if (this.buffer == -1)
			this.buffer = GL15.glGenBuffers();
		if (this.staging == null)
		{
			this.capacity = Math.max(required, 64 * STRIDE);
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
		this.capacity = Math.max(required, 64 * STRIDE);
		this.buffer = GL15.glGenBuffers();
		GL15.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, this.buffer);
		int flags = GL30.GL_MAP_WRITE_BIT | ARBBufferStorage.GL_MAP_PERSISTENT_BIT | ARBBufferStorage.GL_MAP_COHERENT_BIT;
		ARBBufferStorage.glBufferStorage(GL40.GL_DRAW_INDIRECT_BUFFER, (long)this.capacity * RING_SLOTS, flags);
		this.persistentMapping = GL30.glMapBufferRange(GL40.GL_DRAW_INDIRECT_BUFFER, 0L, (long)this.capacity * RING_SLOTS, flags, null);
		if (this.persistentMapping == null)
			throw new IllegalStateException("Failed to persistently map PulseLib indirect stream");
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
			GL15.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, this.buffer);
			if (this.persistent)
				GL15.glUnmapBuffer(GL40.GL_DRAW_INDIRECT_BUFFER);
			GL15.glDeleteBuffers(this.buffer);
		}
		this.buffer = -1;
		this.persistent = false;
		this.persistentMapping = null;
	}

	/**
	 * Performs the write operation.
	 * @param target the target to use.
	 * @param commands the commands to use.
	 */
	private static void write(ByteBuffer target, List<Command> commands)
	{
		for (Command command : commands)
			target.putInt(command.indexCount()).putInt(command.instanceCount()).putInt(command.firstIndex()).
					putInt(command.baseVertex()).putInt(command.baseInstance());
	}

	public record Command(int indexCount, int instanceCount, int firstIndex, int baseVertex, int baseInstance)
	{
	}

	public record Upload(int buffer, long offset)
	{
	}
}
