package com.arcanc.pulselib.content.event;

import com.arcanc.pulselib.content.animatable.instance.InstanceAnimationManager;
import com.arcanc.pulselib.content.animatable.singleton.SingletonAnimationManager;
import com.arcanc.pulselib.content.model.baked.PDeformedMeshBuffers;
import com.arcanc.pulselib.content.model.baked.PGpuDeformedMeshBuffers;
import com.arcanc.pulselib.content.model.deformer.gpu.PGpuDeformerBuffers;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.util.PResourceCache;

/** Client runtime entry points. Call only from physical-client adapters. */
public final class PulseRuntime
{
	private PulseRuntime()
	{
	}

	public static void beforeResourceReload()
	{
		PResourceCache.reloadRegistrations();
	}

	public static void disconnected()
	{
		com.mojang.blaze3d.systems.RenderSystem.assertOnRenderThread();
		PRenderQueue.cleanup();
		PDeformedMeshBuffers.cleanup();
		PGpuDeformedMeshBuffers.cleanup();
		PGpuDeformerBuffers.cleanup();
		InstanceAnimationManager.cleanUp();
		SingletonAnimationManager.cleanUp();
		PPlayerAnimations.cleanUp();
	}
}
