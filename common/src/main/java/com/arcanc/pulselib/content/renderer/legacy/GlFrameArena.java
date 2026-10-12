/**
 * @author ArcAnc
 * Created at: 13.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.legacy;

import org.lwjgl.opengl.GL32;

public final class GlFrameArena
{
	private static final long MAX_REUSE_WAIT_NANOS = 1_000_000L;
	private final long[] fences;
	private long submissionIndex;

	public GlFrameArena(int slots)
	{
		if (slots < 1)
			throw new IllegalArgumentException("Frame arena requires at least one slot");
		this.fences = new long[slots];
	}

	public int begin(boolean persistent)
	{
		if (!persistent)
			return (int)(this.submissionIndex++ % this.fences.length);
		for (int attempt = 0; attempt < this.fences.length; attempt++)
		{
			int slot = (int)(this.submissionIndex++ % this.fences.length);
			if (this.tryAwait(slot))
				return slot;
		}
		return -1;
	}

	public void finish(int slot, boolean persistent)
	{
		if (persistent)
			this.markSubmitted(slot);
	}

	public void await(int slot)
	{
		long fence = this.fences[slot];
		if (fence == 0L)
			return;
		int result = GL32.glClientWaitSync(fence, 0, 0L);
		if (result == GL32.GL_TIMEOUT_EXPIRED)
		{
			result = GL32.glClientWaitSync(fence, GL32.GL_SYNC_FLUSH_COMMANDS_BIT, GL32.GL_TIMEOUT_IGNORED);
		}
		if (result == GL32.GL_WAIT_FAILED)
			throw new IllegalStateException("Failed while waiting for a PulseLib frame-arena fence");
		GL32.glDeleteSync(fence);
		this.fences[slot] = 0L;
	}

	private boolean tryAwait(int slot)
	{
		long fence = this.fences[slot];
		if (fence == 0L)
			return true;
		int result = GL32.glClientWaitSync(fence, GL32.GL_SYNC_FLUSH_COMMANDS_BIT, MAX_REUSE_WAIT_NANOS);
		if (result == GL32.GL_TIMEOUT_EXPIRED)
			return false;
		if (result == GL32.GL_WAIT_FAILED)
			throw new IllegalStateException("Failed while waiting for a PulseLib frame-arena fence");
		GL32.glDeleteSync(fence);
		this.fences[slot] = 0L;
		return true;
	}

	public void awaitAll()
	{
		for (int slot = 0; slot < this.fences.length; slot++)
			this.await(slot);
	}

	public void reset()
	{
		this.awaitAll();
		this.submissionIndex = 0L;
	}

	private void markSubmitted(int slot)
	{
		if (this.fences[slot] != 0L)
			throw new IllegalStateException("PulseLib frame-arena slot was submitted twice without waiting");
		this.fences[slot] = GL32.glFenceSync(GL32.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
	}
}
