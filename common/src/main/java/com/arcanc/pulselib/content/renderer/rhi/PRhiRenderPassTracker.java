/**
 * @author ArcAnc
 * Created at: 13.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.rhi;

import com.mojang.blaze3d.systems.CommandEncoder;
import org.jetbrains.annotations.ApiStatus;

import java.util.IdentityHashMap;
import java.util.Map;

/** Tracks successful native passes across the fresh frontend encoders returned by a device. */
@ApiStatus.Internal
public final class PRhiRenderPassTracker
{
	private static final Map<CommandEncoder, Thread> ACTIVE = new IdentityHashMap<>();

	private PRhiRenderPassTracker()
	{
	}

	public static synchronized void opened(CommandEncoder encoder)
	{
		ACTIVE.put(encoder, Thread.currentThread());
	}

	public static synchronized void closed(CommandEncoder encoder)
	{
		ACTIVE.remove(encoder);
	}

	/** Whether the calling thread owns a live pass, independent of which frontend encoder a scope obtains. */
	public static synchronized boolean hasActivePass()
	{
		for (Thread owner : ACTIVE.values())
			if (owner == Thread.currentThread())
				return true;
		return false;
	}
}
