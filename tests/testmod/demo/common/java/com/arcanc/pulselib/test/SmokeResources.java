/**
 * @author ArcAnc
 * Created at: 13.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test;

import com.arcanc.pulselib.content.animatable.instance.InstanceAnimationManager;
import com.arcanc.pulselib.content.animatable.singleton.SingletonAnimationManager;
import com.arcanc.pulselib.content.model.baked.PDeformedMeshBuffers;
import com.arcanc.pulselib.content.model.baked.PGpuDeformedMeshBuffers;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.arcanc.pulselib.test.RegressionChecks.check;
import static com.arcanc.pulselib.test.RegressionChecks.field;

record SmokeResources(List<Integer> buffers, List<Integer> textures, int framebuffer, RegressionResources.Snapshot nativeResources)
{
	static SmokeResources capture()
	{
		RegressionResources.Snapshot snapshot = RegressionResources.all();
		List<Integer> buffers = new ArrayList<>(snapshot.buffers);
		List<Integer> textures = new ArrayList<>(snapshot.textures);
		check(!((Map<?, ?>)field(PDeformedMeshBuffers.class, "BUFFERS", null)).isEmpty() &&
				!((Map<?, ?>)field(PGpuDeformedMeshBuffers.class, "GEOMETRIES", null)).isEmpty(), "Both deformation caches must be populated before cleanup");
		check(!buffers.isEmpty() && !textures.isEmpty() && !snapshot.framebuffers.isEmpty(), "Live buffers, OIT textures and framebuffers required before cleanup");
		check(!((Map<?, ?>)field(InstanceAnimationManager.class, "MANAGERS", null)).isEmpty(), "Lifecycle fixture must populate instance managers");
		check(!((Map<?, ?>)field(SingletonAnimationManager.class, "MANAGERS", null)).isEmpty(), "Lifecycle fixture must populate singleton managers");
		return new SmokeResources(buffers, textures, snapshot.framebuffers.iterator().next(), snapshot);
	}

	void assertDeleted(String scenario)
	{
		for (int buffer : this.buffers)
			check(!GL15.glIsBuffer(buffer), scenario + ": GPU buffer leaked: " + buffer);
		for (int texture : this.textures)
			check(!GL11.glIsTexture(texture), scenario + ": GPU texture leaked: " + texture);
		check(this.framebuffer < 0 || !GL30.glIsFramebuffer(this.framebuffer), scenario + ": OIT framebuffer leaked");
		check(((Map<?, ?>)field(InstanceAnimationManager.class, "MANAGERS", null)).isEmpty(), scenario + ": instance managers leaked");
		check(((Map<?, ?>)field(SingletonAnimationManager.class, "MANAGERS", null)).isEmpty(), scenario + ": singleton managers leaked");
		check(((Map<?, ?>)field(PPlayerAnimations.class, "INSTANCES", null)).isEmpty(), scenario + ": player managers leaked");
		check(((Map<?, ?>)field(PGpuDeformedMeshBuffers.class, "GEOMETRIES", null)).isEmpty(), scenario + ": GPU deformation cache leaked");
		check(((Map<?, ?>)field(PDeformedMeshBuffers.class, "BUFFERS", null)).isEmpty(), scenario + ": CPU deformation cache leaked");
		Object resources = field(PRenderQueue.class, "RESOURCES", null);
		check(((Map<?, ?>)field(resources.getClass(), "meshResources", resources)).isEmpty(), scenario + ": render resources leaked");
		this.nativeResources.deleted(scenario);
		RegressionResources.empty();
	}
}
