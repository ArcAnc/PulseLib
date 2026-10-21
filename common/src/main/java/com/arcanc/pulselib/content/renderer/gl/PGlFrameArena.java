/**
 * @author ArcAnc
 * Created at: 14.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.gl;

import org.lwjgl.opengl.GL32;

/**
 * Manages gl frame allocations.
 */
final class PGlFrameArena
{
	private static final long MAX_REUSE_WAIT_NANOS = 1_000_000L;
	private final long[] fences;
	private long submissionIndex;

	/**
	 * Creates an instance of the enclosing type.
	 * @param slots the slots to use.
	 */
	PGlFrameArena(int slots)
	{
		this.fences = new long[slots];
	}

	/**
	 * Performs the begin operation.
	 * @return the value produced by this operation.
	 */
	int begin()
	{
		for (int attempt = 0; attempt < this.fences.length; attempt++)
		{
			int slot = (int)(this.submissionIndex++ % this.fences.length);
			if (this.tryAwait(slot))
				return slot;
		}
		return -1;
	}

	/**
	 * Performs the finish operation.
	 * @param slot the slot to use.
	 */
	void finish(int slot)
	{
		this.fences[slot] = GL32.glFenceSync(GL32.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
	}

	/**
	 * Performs the await all operation.
	 */
	void awaitAll()
	{
		for (int slot = 0; slot < this.fences.length; slot++)
			this.await(slot);
	}

	/**
	 * Performs the try await operation.
	 * @param slot the slot to use.
	 * @return the value produced by this operation.
	 */
	private boolean tryAwait(int slot)
	{
		long fence = this.fences[slot];
		if (fence == 0L)
			return true;
		int result = GL32.glClientWaitSync(fence, GL32.GL_SYNC_FLUSH_COMMANDS_BIT, MAX_REUSE_WAIT_NANOS);
		if (result == GL32.GL_TIMEOUT_EXPIRED)
			return false;
		if (result == GL32.GL_WAIT_FAILED)
			throw new IllegalStateException("Failed while waiting for a PulseLib frame-stream fence");
		GL32.glDeleteSync(fence);
		this.fences[slot] = 0L;
		return true;
	}

	/**
	 * Performs the await operation.
	 * @param slot the slot to use.
	 */
	private void await(int slot)
	{
		long fence = this.fences[slot];
		if (fence == 0L)
			return;
		int result = GL32.glClientWaitSync(fence, GL32.GL_SYNC_FLUSH_COMMANDS_BIT, GL32.GL_TIMEOUT_IGNORED);
		if (result == GL32.GL_WAIT_FAILED)
			throw new IllegalStateException("Failed while waiting for a PulseLib frame-stream fence");
		GL32.glDeleteSync(fence);
		this.fences[slot] = 0L;
	}
}
