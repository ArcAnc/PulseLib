/**
 * @author ArcAnc
 * Created at: 12.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.event;

import com.arcanc.pulselib.content.animatable.instance.InstanceAnimationManager;
import com.arcanc.pulselib.content.animatable.singleton.SingletonAnimationManager;
import com.arcanc.pulselib.content.model.baked.PDeformedMeshBuffers;
import com.arcanc.pulselib.content.model.baked.PGpuDeformedMeshBuffers;
import com.arcanc.pulselib.content.model.deformer.gpu.PGpuDeformerBuffers;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.util.PResourceCache;
import com.mojang.blaze3d.systems.RenderSystem;

/**
 * Client runtime entry points. Call only from physical-client adapters.
 */
public final class PulseRuntime
{
	private PulseRuntime()
	{
	}

	/**
	 * Clears runtime state that must not outlive the resources being reloaded.
	 */
	public static void beforeResourceReload()
	{
		PResourceCache.reloadRegistrations();
	}

	/**
	 * Releases animation and rendering state associated with the disconnected client session.
	 */
	public static void disconnected()
	{
		RenderSystem.assertOnRenderThread();
		PRenderQueue.cleanup();
		PDeformedMeshBuffers.cleanup();
		PGpuDeformedMeshBuffers.cleanup();
		PGpuDeformerBuffers.cleanup();
		InstanceAnimationManager.cleanUp();
		SingletonAnimationManager.cleanUp();
		PPlayerAnimations.cleanUp();
	}
}
