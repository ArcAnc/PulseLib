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
import com.arcanc.pulselib.content.renderer.legacy.GlStreamStorage;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.arcanc.pulselib.test.SmokeHarness.check;
import static com.arcanc.pulselib.test.SmokeHarness.field;

record SmokeResources(List<Integer> buffers, List<Integer> textures, int framebuffer)
{
	static SmokeResources capture()
	{
		List<Integer> buffers = new ArrayList<>(SmokeHarness.captureGeometryBuffers());
		List<Integer> textures = new ArrayList<>();
		Object executor = field(PRenderQueue.class, "EXECUTOR", null);
		Object instances = field(executor.getClass(), "instanceBuffer", executor);
		Object indirect = field(executor.getClass(), "indirectBuffer", executor);
		buffers.add((Integer)field(instances.getClass(), "vbo", instances));
		buffers.add((Integer)field(indirect.getClass(), "buffer", indirect));
		for (String name : List.of("operations", "values"))
		{
			buffers.add((Integer)field(GlStreamStorage.class, name + "Buffer", null));
			textures.add((Integer)field(GlStreamStorage.class, name + "Texture", null));
		}
		Object oit = field(executor.getClass(), "weightedBlendedOit", executor);
		for (String name : List.of("accumulationTextures", "revealageTextures", "layerDepthTextures"))
		{
			for (int texture : (int[])field(oit.getClass(), name, oit))
			{
				textures.add(texture);
			}
		}
		textures.add((Integer)field(oit.getClass(), "fabulousMetadataTexture", oit));
		check(!((Collection<?>)field(InstanceAnimationManager.class, "MANAGERS", null)).isEmpty(), "Lifecycle fixture must populate instance managers");
		check(!((Map<?, ?>)field(SingletonAnimationManager.class, "MANAGERS", null)).isEmpty(), "Lifecycle fixture must populate singleton managers");
		return new SmokeResources(buffers.stream().filter(id -> id > 0).distinct().toList(),
				textures.stream().filter(id -> id > 0).distinct().toList(), (Integer)field(oit.getClass(), "framebuffer", oit));
	}

	void assertDeleted(String scenario)
	{
		for (int buffer : this.buffers)
		{
			check(!GL15.glIsBuffer(buffer), scenario + ": GPU buffer leaked: " + buffer);
		}
		for (int texture : this.textures)
		{
			check(!GL11.glIsTexture(texture), scenario + ": GPU texture leaked: " + texture);
		}
		check(this.framebuffer < 0 || !GL30.glIsFramebuffer(this.framebuffer), scenario + ": OIT framebuffer leaked");
		check(((Collection<?>)field(InstanceAnimationManager.class, "MANAGERS", null)).isEmpty(), scenario + ": instance managers leaked");
		check(((Map<?, ?>)field(SingletonAnimationManager.class, "MANAGERS", null)).isEmpty(), scenario + ": singleton managers leaked");
		check(((Map<?, ?>)field(PPlayerAnimations.class, "INSTANCES", null)).isEmpty(), scenario + ": player managers leaked");
		check(((Map<?, ?>)field(PGpuDeformedMeshBuffers.class, "GEOMETRIES", null)).isEmpty(), scenario + ": GPU deformation cache leaked");
		check(((Map<?, ?>)field(PDeformedMeshBuffers.class, "BUFFERS", null)).isEmpty(), scenario + ": CPU deformation cache leaked");
		Object resources = field(PRenderQueue.class, "RESOURCES", null);
		check(((Map<?, ?>)field(resources.getClass(), "geometryResources", resources)).isEmpty(), scenario + ": render resources leaked");
	}
}
