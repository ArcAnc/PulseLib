/**
 * @author ArcAnc
 * Created at: 14.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.porting.scene;

import com.arcanc.pulselib.content.model.deformer.PChannelReference;
import com.arcanc.pulselib.content.model.deformer.PDeformerInstance;
import com.arcanc.pulselib.content.model.deformer.PDeformerStack;
import com.arcanc.pulselib.content.model.deformer.PMeshDeformation;
import com.arcanc.pulselib.content.model.deformer.PStretchDeformer;
import com.arcanc.pulselib.content.model.deformer.PStretchDefinition;
import com.arcanc.pulselib.content.renderer.PRenderContexts;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.content.renderer.rhi.PRhiRenderHost;
import com.arcanc.pulselib.util.PRenderTypes;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.nio.ByteBuffer;
import java.util.List;

/** Real GUI scopes and their own queue; native pass observations verify one draw per object. */
public final class GuiQueueChecks
{
	private GuiQueueChecks()
	{
	}

	public static int run(GpuDevice device, GpuTexture color, GpuTexture depth, Identifier texture) throws Exception
	{
		int initial = SceneChecks.counters()[0];
		int[] initialInstant = SceneChecks.instantCounters();
		int legacy = SceneChecks.counters()[1];
		RenderType instant = PRenderTypes.RenderTypeProvider.trianglesInstantEmissiveSolid(texture);
		PGeometryData geometry = SceneChecks.geometry(0);
		var aColor = RenderSystem.outputColorTextureOverride;
		var aDepth = RenderSystem.outputDepthTextureOverride;
		var projection = RenderSystem.getProjectionMatrixBuffer();
		var projectionType = RenderSystem.getProjectionType();
		var modelView = RenderSystem.getModelViewMatrixCopy();
		int usage = GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_COPY_DST;
		try (var b = device.createTexture("GUI queue B", usage, GpuFormat.RGBA8_UNORM, 128, 128, 1, 1);
				var c = device.createTexture("GUI queue C", usage, GpuFormat.RGBA8_UNORM, 128, 128, 1, 1);
				var bDepth = device.createTexture("GUI queue B depth", usage, GpuFormat.D32_FLOAT, 128, 128, 1, 1);
				var cDepth = device.createTexture("GUI queue C depth", usage, GpuFormat.D32_FLOAT, 128, 128, 1, 1);
				var bv = device.createTextureView(b);
				var cv = device.createTextureView(c);
				var bdv = device.createTextureView(bDepth);
				var cdv = device.createTextureView(cDepth);
				var alternateProjection = new ProjectionMatrixBuffer("GUI queue alternate projection"))
		{
			try
			{
				SceneChecks.clear(device, color, depth);
				// A world submission exists before the GUI enters its own scope/compiler.
				PRenderQueue.submit(PRenderQueue.RenderStage.ENTITIES, SceneChecks.type(texture, false, true), geometry, header(0, 0, 0, 0xFF0000FF, 1));
				int[] before = SceneChecks.instantCounters();
				try (var scope = PRenderContexts.enter(PRenderContexts.Context.GUI))
				{
					submit(instant, geometry, header(-0.5f, 0, 0, 0xFFFF0000, 0.4f));
					submit(instant, geometry, header(0.5f, 0, 0, 0xFF00FF00, 0.4f));
					SceneChecks.check(SceneChecks.instantCounters()[1] == before[1], "Local submissions must wait only for their own GUI flush");
				}
				SceneChecks.check(SceneChecks.instantCounters()[0] == before[0] + 1 && SceneChecks.instantCounters()[1] == before[1] + 2,
						"One GUI flush must issue two single-object native draws");
				ByteBuffer pixels = SceneChecks.pixels(device, color);
				SceneChecks.check(SceneChecks.pixel(pixels, 32, 64) == 0xFFFF0000 && SceneChecks.pixel(pixels, 96, 64) == 0xFF00FF00 && SceneChecks.pixel(pixels, 64, 64) == 0,
						"GUI colors/transforms must be independent and world queue must remain pending");
				int singleDraws = SceneChecks.instantCounters()[1];
				PRenderQueue.flushOpaque(PRenderQueue.RenderStage.ENTITIES);
				SceneChecks.check(SceneChecks.pixel(SceneChecks.pixels(device, color), 64, 64) == 0xFF0000FF && SceneChecks.instantCounters()[1] == singleDraws,
						"World flush must render its sentinel without replaying GUI objects");
				SceneChecks.image(pixels, "gui-independent-queue");
				System.out.println("PASS GUI/world isolation: private compiler, own flush, two single draws, world sentinel preserved, no GUI replay");

				SceneChecks.clear(device, b, bDepth);
				RenderSystem.outputColorTextureOverride = bv;
				RenderSystem.outputDepthTextureOverride = bdv;
				try (var scope = PRenderContexts.enter(PRenderContexts.Context.GUI))
				{
					submit(instant, geometry, header(0, 0, 0, 0xFFFF00FF, 1));
				}
				SceneChecks.check(SceneChecks.pixel(SceneChecks.pixels(device, b), 64, 64) == 0xFFFF00FF && SceneChecks.pixel(SceneChecks.pixels(device, color), 64, 64) == 0xFF0000FF,
						"Consecutive GUI queues must not share output targets");

				// Nested queues capture their own target/frame/scissor; later mutations must not redirect their flush.
				RenderSystem.outputColorTextureOverride = aColor;
				RenderSystem.outputDepthTextureOverride = aDepth;
				SceneChecks.clear(device, color, depth);
				SceneChecks.clear(device, b, bDepth);
				SceneChecks.clear(device, c, cDepth);
				try (var parent = PRenderContexts.enter(PRenderContexts.Context.GUI))
				{
					submit(instant, geometry, header(0, 0, 0, 0xFFFF0000, 1));
					RenderSystem.outputColorTextureOverride = bv;
					RenderSystem.outputDepthTextureOverride = bdv;
					try (var child = PRenderContexts.enter(PRenderContexts.Context.OFFSCREEN))
					{
						submit(instant, geometry, header(0, 0, 0, 0xFF00FF00, 1));
						RenderSystem.outputColorTextureOverride = cv;
						RenderSystem.outputDepthTextureOverride = cdv;
						RenderSystem.getModelViewStack().translation(4, 0, 0);
						RenderSystem.setProjectionMatrix(alternateProjection.getBuffer(new Matrix4f().scale(0.5f)), ProjectionType.ORTHOGRAPHIC);
						RenderSystem.enableScissorForRenderTypeDraws(0, 0, 1, 1);
					}
					SceneChecks.check(RenderSystem.outputColorTextureOverride == bv && RenderSystem.getProjectionMatrixBuffer().equals(projection) &&
							RenderSystem.getModelViewMatrixCopy().equals(modelView) && !RenderSystem.getScissorStateForRenderTypeDraws().enabled(), "Child scope must restore native inputs");
					SceneChecks.check(SceneChecks.pixel(SceneChecks.pixels(device, b), 64, 64) == 0xFF00FF00 && SceneChecks.pixel(SceneChecks.pixels(device, color), 64, 64) == 0 &&
							SceneChecks.pixel(SceneChecks.pixels(device, c), 64, 64) == 0, "Child must flush its own captured target/frame/scissor only");
				}
				SceneChecks.check(SceneChecks.pixel(SceneChecks.pixels(device, color), 64, 64) == 0xFFFF0000 && RenderSystem.outputColorTextureOverride == aColor,
						"Parent queue must survive child flush and restore its target");
				System.out.println("PASS nested GUI/offscreen queues: independent targets, captured matrices/scissor and parent continuation");

				SceneChecks.clear(device, color, depth);
				SceneChecks.clear(device, b, bDepth);
				var stack = PDeformerStack.compile(List.of(new PDeformerInstance<>(PStretchDeformer.INSTANCE,
						new PStretchDefinition(new Vector3f(), new Vector3f(1, 0, 0), PChannelReference.constant(0.5f)))));
				var baked = SceneChecks.baked(geometry, texture, false, true);
				try (var parent = PRenderContexts.enter(PRenderContexts.Context.GUI))
				{
					PRenderQueue.submit(PRenderQueue.RenderStage.ENTITIES, instant, baked, new PMeshDeformation(stack, ref -> 0.5f, new Object(), 0),
							new PRenderQueue.InstanceData(new Matrix4f(), 0xFFFF0000, 0xF000F0, OverlayTexture.NO_OVERLAY));
					RenderSystem.outputColorTextureOverride = bv;
					RenderSystem.outputDepthTextureOverride = bdv;
					try (var child = PRenderContexts.enter(PRenderContexts.Context.GUI))
					{
						PRenderQueue.submit(PRenderQueue.RenderStage.ENTITIES, instant, baked, new PMeshDeformation(stack, ref -> 1.2f, new Object(), 0),
								new PRenderQueue.InstanceData(new Matrix4f(), 0xFF00FF00, 0xF000F0, OverlayTexture.NO_OVERLAY));
					}
					SceneChecks.check(SceneChecks.pixel(SceneChecks.pixels(device, b), 96, 64) == 0xFF00FF00, "Child queue must use its own sampled deformer value");
				}
				SceneChecks.check(SceneChecks.pixel(SceneChecks.pixels(device, color), 64, 64) == 0xFFFF0000 &&
						SceneChecks.pixel(SceneChecks.pixels(device, color), 96, 64) == 0, "Child flush must not clear or overwrite its parent's pending deformation values");
				System.out.println("PASS nested GUI deformation values: child 1.2 and pending parent 0.5 stay independent");

				SceneChecks.clear(device, color, depth);
				SceneChecks.clear(device, b, bDepth);
				try (var parent = PRenderContexts.enter(PRenderContexts.Context.GUI))
				{
					submit(instant, geometry, header(0, 0, 0, 0xFFFF0000, 1));
					RenderSystem.outputColorTextureOverride = bv;
					RenderSystem.outputDepthTextureOverride = bdv;
					try (var child = PRenderContexts.enter(PRenderContexts.Context.GUI))
					{
						submit(instant, geometry, header(0, 0, 0, 0xFF00FF00, 1));
						child.discard();
					}
				}
				SceneChecks.check(SceneChecks.pixel(SceneChecks.pixels(device, b), 64, 64) == 0 && SceneChecks.pixel(SceneChecks.pixels(device, color), 64, 64) == 0xFFFF0000,
						"Discarding a child must not discard or replay its parent");

				try
				{
					try (var scope = PRenderContexts.enter(PRenderContexts.Context.GUI))
					{
						var invalid = new PGeometryData(ByteBuffer.allocate(1), geometry.indices(), 24, 3, geometry.indexType());
						submit(instant, invalid, header(0, 0, 0, -1, 1));
					}
					throw new AssertionError("Invalid GUI geometry unexpectedly rendered");
				}
				catch (IllegalArgumentException expected)
				{
					SceneChecks.check(expected.getMessage().contains("geometry"), "Expected actual GUI flush validation failure");
				}
				SceneChecks.clear(device, color, depth);
				try (var scope = PRenderContexts.enter(PRenderContexts.Context.GUI))
				{
					submit(instant, geometry, header(0, 0, 0, 0xFF00FF00, 1));
				}
				SceneChecks.check(SceneChecks.pixel(SceneChecks.pixels(device, color), 64, 64) == 0xFF00FF00 && RenderSystem.outputColorTextureOverride == aColor,
						"A failing flush must clear/pop its local queue and permit the next GUI render");
				System.out.println("PASS GUI discard/flush-exception cleanup and next-frame recovery");

				SceneChecks.clear(device, color, depth);
				try (var scope = PRenderContexts.enter(PRenderContexts.Context.GUI))
				{
					submit(instant, geometry, header(-0.5f, 0, 0, 0xFFFF0000, 0.4f));
					before = SceneChecks.instantCounters();
					PRenderQueue.drawImmediate(instant, SceneChecks.baked(geometry, texture, false, true), null,
							header(0.5f, 0, 0, 0xFF00FF00, 0.4f), new PRhiRenderHost().captureFrame());
					SceneChecks.check(SceneChecks.instantCounters()[1] == before[1] + 1 && SceneChecks.pixel(SceneChecks.pixels(device, color), 96, 64) == 0xFF00FF00 &&
							SceneChecks.pixel(SceneChecks.pixels(device, color), 32, 64) == 0, "drawImmediate must submit now without draining its surrounding GUI queue");
				}
				SceneChecks.check(SceneChecks.pixel(SceneChecks.pixels(device, color), 32, 64) == 0xFFFF0000, "Pending GUI object must flush at its own scope boundary");

				SceneChecks.clear(device, color, depth);
				before = SceneChecks.instantCounters();
				try (var scope = PRenderContexts.enter(PRenderContexts.Context.GUI))
				{
					for (int i = 0; i < 130; i++)
						submit(instant, geometry, header((i % 13 + 0.5f) / 13 * 2 - 1, (i / 13 + 0.5f) / 10 * 2 - 1, 0, color(i), 0.09f));
				}
				SceneChecks.check(SceneChecks.instantCounters()[0] == before[0] + 1 && SceneChecks.instantCounters()[1] == before[1] + 130,
						"130 GUI objects must produce exactly 130 native single draws, never an instanced batch");
				pixels = SceneChecks.pixels(device, color);
				for (int i = 0; i < 130; i++)
					SceneChecks.check(SceneChecks.pixel(pixels, (int)((i % 13 + 0.5f) / 13 * 128), (int)((i / 13 + 0.5f) / 10 * 128)) == color(i), "Per-object GUI uniforms/transform " + i);
				SceneChecks.image(pixels, "gui-130-single-draws");
				System.out.println("PASS 130 GUI objects: 130 single draws and individual 48-byte uniforms; no array or instancing");

				SceneChecks.clear(device, color, depth);
				RenderType alpha = PRenderTypes.RenderTypeProvider.trianglesInstantEmissiveTranslucent(texture);
				try (var scope = PRenderContexts.enter(PRenderContexts.Context.GUI))
				{
					// Submit near first: the local compiler must still composite far -> near under reversed-Z.
					submit(alpha, geometry, header(0, 0, 0.25f, 0x800000FF, 1));
					submit(alpha, geometry, header(0, 0, -0.25f, 0x80FF0000, 1));
				}
				int composed = SceneChecks.pixel(SceneChecks.pixels(device, color), 64, 64);
				SceneChecks.check(composed == 0xC0400080, "GUI alpha must composite far red then near blue: " + Integer.toHexString(composed));
				SceneChecks.image(SceneChecks.pixels(device, color), "gui-alpha-order");
				System.out.println("PASS GUI transparency order under reversed-Z");
				SceneChecks.clear(device, c, cDepth);
				RenderSystem.outputColorTextureOverride = cv;
				RenderSystem.outputDepthTextureOverride = null;
				try (var scope = PRenderContexts.enter(PRenderContexts.Context.OFFSCREEN))
				{
					submit(instant, geometry, header(0.5f, 0, 0, 0xFF00FFFF, 0.4f));
					submit(SceneChecks.type(texture, false, true), geometry, header(-0.5f, 0, 0, 0xFFFF00FF, 0.4f));
				}
				SceneChecks.check(SceneChecks.pixel(SceneChecks.pixels(device, c), 96, 64) == 0xFF00FFFF &&
						SceneChecks.pixel(SceneChecks.pixels(device, c), 32, 64) == 0xFFFF00FF && RenderSystem.outputDepthTextureOverride == null,
						"A depthless GUI target must not inherit the main world's depth attachment");
				RenderSystem.outputColorTextureOverride = aColor;
				RenderSystem.outputDepthTextureOverride = aDepth;
				System.out.println("PASS depthless offscreen GUI target: no world depth attachment inherited");

				SceneChecks.check(SceneChecks.counters()[1] == legacy, "GUI queues must not fall back to legacy instancing");
				SceneChecks.check(!color.isClosed() && !depth.isClosed() && !b.isClosed(), "Scopes borrow their targets");
			}
			finally
			{
				RenderSystem.outputColorTextureOverride = aColor;
				RenderSystem.outputDepthTextureOverride = aDepth;
				RenderSystem.setProjectionMatrix(projection, projectionType);
				RenderSystem.getModelViewStack().set(modelView);
				RenderSystem.disableScissorForRenderTypeDraws();
				PRenderQueue.cleanup();
			}
		}
		int[] finished = SceneChecks.instantCounters();
		SceneChecks.check(finished[0] - initialInstant[0] == 14 && finished[1] - initialInstant[1] == 144,
				"GUI suite must issue exactly 144 single-object draws and 14 executor entries, including the rejected geometry case");
		System.out.println("PASS instant GUI counters: 14 executor entries (one expected rejection), 144 native single-object draws, no instancing");
		return SceneChecks.counters()[0] - initial;
	}

	private static void submit(RenderType type, PGeometryData geometry, PInstanceHeader object)
	{
		PRenderQueue.submit(PRenderQueue.RenderStage.ENTITIES, type, geometry, object);
	}

	private static PInstanceHeader header(float x, float y, float z, int color, float scale)
	{
		return new PInstanceHeader(new Matrix4f().translation(x, y, z).scale(scale, scale, 1), color, 0xF000F0, OverlayTexture.NO_OVERLAY);
	}

	private static int color(int index)
	{
		return 0xFF00003F | (index + 1) << 16 | (index % 9 + 1) << 8;
	}
}
