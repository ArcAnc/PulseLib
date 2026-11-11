/**
 * @author ArcAnc
 * Created at: 13.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.legacy;

import com.arcanc.pulselib.content.renderer.gl.PGlFrameArena;

/**
 * Rotating frame slots guarded by GPU fences for safe streamed-buffer reuse.
 */
public final class GlFrameArena
{
	private final PGlFrameArena arena;

	/** Allocates native rotating frame slots guarded by GPU fences. */
	public GlFrameArena(int slots)
	{
		this.arena = new PGlFrameArena(slots);
	}

	/** Acquires a reusable slot with a bounded wait for persistent storage. */
	public int begin(boolean persistent)
	{
		return this.arena.begin(persistent);
	}

	/** Waits for the next rotating slot when persistent storage is active. */
	public int beginWaiting(boolean persistent)
	{
		return this.arena.beginWaiting(persistent);
	}

	/** Marks the submitted persistent slot for GPU completion tracking. */
	public void finish(int slot, boolean persistent)
	{
		this.arena.finish(slot, persistent);
	}

	/** Waits for the selected slot and deletes its completed fence. */
	public void await(int slot)
	{
		this.arena.await(slot);
	}

	/** Waits for all outstanding GPU submissions. */
	public void awaitAll()
	{
		this.arena.awaitAll();
	}

	/** Retires all fences and resets the rotating slot selection. */
	public void reset()
	{
		this.arena.reset();
	}
}
