/**
 * @author ArcAnc
 * Created at: 24.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1;

import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.PWorldRenderPass;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.PResourceCache;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Native transparency postchain + production framegraph resolve; live world hooks in both MC26 modes. */
public final class Stage42b2FabulousProbe
{
	private static final PRenderQueue.RenderStage STAGE = new PRenderQueue.RenderStage("stage42b2_fabulous");
	private static int pending;
	private static int nativePending;
	private static int cases;
	private static int nativeCases;
	private static int suites;
	private static boolean originalImproved;
	private static boolean optionsSaved;

	private Stage42b2FabulousProbe()
	{
	}

	public static void register()
	{
		NeoForge.EVENT_BUS.addListener(Stage42b2FabulousProbe::suite);
		NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, Stage42b2FabulousProbe::worldSubmit);
		NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, Stage42b2FabulousProbe::lateDepth);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Stage42b2FabulousProbe::worldResult);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Stage42b2FabulousProbe::shutdown);
	}

	public static void tick(int tick)
	{
		if (!optionsSaved)
		{
			originalImproved = Minecraft.getInstance().options.improvedTransparency().get();
			optionsSaved = true;
		}
		if (tick == 55 || tick == 200)
			Minecraft.getInstance().options.improvedTransparency().set(false);
		if (tick == 75 || tick == 255)
			Minecraft.getInstance().options.improvedTransparency().set(true);
		if (tick == 60 || tick == 80 || tick == 205 || tick == 260)
			nativePending = tick;
		if (tick == 118 || tick == 248)
			pending = tick;
	}

	private static void suite(RenderLevelStageEvent.AfterOpaqueBlocks event)
	{
		if (pending == 0)
			return;
		var projection = RenderSystem.getProjectionMatrixBuffer();
		var projectionType = RenderSystem.getProjectionType();
		var state = new Stage42b1OitProbe.State();
		RenderSystem.getModelViewStack().pushMatrix().identity();
		try (ProjectionMatrixBuffer identity = new ProjectionMatrixBuffer("stage42b2 identity"))
		{
			RenderSystem.setProjectionMatrix(identity.getBuffer(new Matrix4f()), ProjectionType.ORTHOGRAPHIC);
			for (int target = 0; target < 5; target++)
			{
				verify("target-" + target + "-front", 1, List.of(new Surface(0.4f, 0x80FF0000)), List.of(new Vanilla(target, 0.2f, 0x660000FF)));
				verify("target-" + target + "-behind", 1, List.of(new Surface(0.4f, 0x80FF0000)), List.of(new Vanilla(target, 0.7f, 0x660000FF)));
			}
			verify("interleaved-layers", 1, List.of(new Surface(0.2f, 0x80FF0000), new Surface(0.4f, 0x8000FF00), new Surface(0.7f, 0x800000FF)),
					List.of(new Vanilla(2, 0.3f, 0x66FFFF00), new Vanilla(3, 0.6f, 0x6600FFFF), new Vanilla(4, 0.8f, 0x66FF00FF)));
			verify("opaque-particle-and-occluder", 0.5f, List.of(new Surface(0.4f, 0x80FF0000), new Surface(0.7f, 0x8000FF00)),
					List.of(new Vanilla(2, 0.2f, 0xFF0000FF)));
			suites++;
			PLibDatabase.LOGGER.info("STAGE 4.2b.2 FABULOUS SUITE PASS: tick={}, cases={}, actual vanilla postchain -> production resolve, five targets/front/behind/interleaving/opaque particles/depth/alpha", pending, cases);
			pending = 0;
		}
		finally
		{
			RenderSystem.setProjectionMatrix(projection, projectionType);
			RenderSystem.getModelViewStack().popMatrix();
			state.restore();
		}
	}

	private static void verify(String name, float opaqueDepth, List<Surface> surfaces, List<Vanilla> layers)
	{
		int width = pending < 200 ? 64 : 96;
		TextureTarget main = new TextureTarget("stage42b2 resolved", width, width / 2, true);
		TextureTarget opaque = new TextureTarget("stage42b2 opaque depth", width, width / 2, true);
		List<RenderTarget> inputs = new ArrayList<>();
		for (int i = 0; i < 5; i++)
			inputs.add(new TextureTarget("stage42b2 native input " + i, width, width / 2, true));
		try
		{
			Stage42b1OitProbe.clear(main, 0xFF1A4080, opaqueDepth);
			Stage42b1OitProbe.clear(opaque, 0, opaqueDepth);
			for (RenderTarget input : inputs)
				Stage42b1OitProbe.clear(input, 0, 1);
			for (Vanilla layer : layers)
				Stage42b1OitProbe.clear(inputs.get(layer.target()), premultiply(layer.color()), layer.depth());
			float[] background = Stage42b1OitProbe.pixel(main);
			List<float[]> before = inputs.stream().map(Stage42b1OitProbe::pixel).toList();
			var geometry = Stage42aWorldStageProbe.quad();
			var type = PRenderTypes.RenderTypeProvider.trianglesEmissiveTranslucent(PResourceCache.ATLAS_LOCATION);
			for (Surface surface : surfaces)
				PRenderQueue.submit(STAGE, type, geometry, new PInstanceHeader(new Matrix4f().translation(0, 0, surface.depth() * 2 - 1),
						surface.color(), 0xF000F0, OverlayTexture.NO_OVERLAY));
			PRenderQueue.flush(STAGE, opaque);
			try
			{
				PRenderQueue.compositeFabulousTranslucency(main, inputs.subList(0, 4));
				throw new AssertionError("Invalid Fabulous target count must throw");
			}
			catch (IllegalArgumentException expected)
			{
				check(Arrays.equals(background, Stage42b1OitProbe.pixel(main)), name + " validation changed destination");
			}
			FrameGraphBuilder frame = new FrameGraphBuilder();
			LevelTargetBundle bundle = new LevelTargetBundle();
			bundle.main = frame.importExternal("main", main);
			bundle.translucent = frame.importExternal("translucent", inputs.get(0));
			bundle.itemEntity = frame.importExternal("item_entity", inputs.get(1));
			bundle.particles = frame.importExternal("particles", inputs.get(2));
			bundle.weather = frame.importExternal("weather", inputs.get(3));
			bundle.clouds = frame.importExternal("clouds", inputs.get(4));
			var chain = Minecraft.getInstance().getShaderManager().getPostChain(Identifier.withDefaultNamespace("transparency"), LevelTargetBundle.SORTING_TARGETS);
			check(chain != null, "Real MC26 transparency postchain must load");
			chain.addToFrame(frame, width, width / 2, bundle);
			PWorldRenderPass.addToFrame(frame, bundle);
			List<String> passes = new ArrayList<>();
			frame.execute(GraphicsResourceAllocator.UNPOOLED, new FrameGraphBuilder.Inspector()
			{
				@Override
				public void beforeExecutePass(String pass)
				{
					passes.add(pass);
				}
			});
			check(passes.size() >= 3 && passes.getLast().equals("pulselib_transparency"), "Native chain must run before production resolve: " + passes);
			float[] actual = Stage42b1OitProbe.pixel(main);
			float[] expected = reference(background, opaqueDepth, surfaces, layers);
			for (int channel = 0; channel < 4; channel++)
				check(Math.abs(actual[channel] - expected[channel]) < 0.018f, name + " expected=" + Arrays.toString(expected) + " actual=" + Arrays.toString(actual));
			check(actual[4] == background[4], name + " resolved image depth changed");
			for (int i = 0; i < inputs.size(); i++)
				check(Arrays.equals(before.get(i), Stage42b1OitProbe.pixel(inputs.get(i))), name + " native input color/depth modified");
			PRenderQueue.compositeFabulousTranslucency(main, inputs);
			check(Arrays.equals(actual, Stage42b1OitProbe.pixel(main)), name + " composite repeated");
			PLibDatabase.LOGGER.info("STAGE 4.2b.2 FABULOUS CASE PASS: tick={}, {}, RGBA/depth={}", pending, name, Arrays.toString(actual));
			cases++;
		}
		finally
		{
			inputs.forEach(RenderTarget::destroyBuffers);
			main.destroyBuffers();
			opaque.destroyBuffers();
		}
	}

	private static float[] reference(float[] background, float opaqueDepth, List<Surface> surfaces, List<Vanilla> vanilla)
	{
		List<Surface> sorted = new ArrayList<>(surfaces);
		for (Vanilla layer : vanilla)
			sorted.add(new Surface(layer.depth(), layer.color()));
		sorted.sort(Comparator.comparingDouble(Surface::depth).reversed());
		float[] result = Arrays.copyOf(background, 4);
		for (Surface layer : sorted)
		{
			if (layer.depth() > opaqueDepth)
				continue;
			float alpha = (layer.color() >>> 24) / 255f;
			for (int channel = 0; channel < 3; channel++)
				result[channel] = ((layer.color() >>> (16 - channel * 8)) & 255) / 255f * alpha + result[channel] * (1 - alpha);
		}
		return result;
	}

	private static int premultiply(int color)
	{
		int alpha = color >>> 24;
		return alpha << 24 | ((color >>> 16 & 255) * alpha / 255) << 16 | ((color >>> 8 & 255) * alpha / 255) << 8 | (color & 255) * alpha / 255;
	}

	private static void worldSubmit(RenderLevelStageEvent.AfterOpaqueFeatures event)
	{
		if (nativePending == 0)
			return;
		var geometry = Stage42aWorldStageProbe.quad();
		PRenderQueue.submitEntityMesh(PRenderTypes.RenderTypeProvider.trianglesEmissiveSolid(PResourceCache.ATLAS_LOCATION), geometry,
				Stage42aWorldStageProbe.header(0, 0.7f, 0xFF000000));
		PRenderQueue.submitEntityMesh(PRenderTypes.RenderTypeProvider.trianglesEmissiveTranslucent(PResourceCache.ATLAS_LOCATION), geometry,
				Stage42aWorldStageProbe.header(0, 0.5f, 0x80FF0000));
	}

	private static void lateDepth(RenderLevelStageEvent.AfterTranslucentParticles event)
	{
		if (nativePending == 0)
			return;
		// A nearer late depth must not replace the opaque snapshot used by the OIT accumulation.
		RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
		var state = new Stage42b1OitProbe.State();
		int fbo = GL30.glGenFramebuffers();
		double previousClearDepth = GL11.glGetDouble(GL11.GL_DEPTH_CLEAR_VALUE);
		try
		{
			GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, fbo);
			GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, ((GlTexture)target.getDepthTexture()).glId(), 0);
			GL11.glDrawBuffer(GL11.GL_NONE);
			GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, fbo);
			GL11.glReadBuffer(GL11.GL_NONE);
			check(GL30.glCheckFramebufferStatus(GL30.GL_DRAW_FRAMEBUFFER) == GL30.GL_FRAMEBUFFER_COMPLETE, "Late depth framebuffer incomplete");
			GlStateManager._enableScissorTest();
			GlStateManager._scissorBox((int)(target.width * 0.19), (int)(target.height * 0.79), (int)(target.width * 0.02) + 1, (int)(target.height * 0.02) + 1);
			GlStateManager._depthMask(true);
			GL11.glClearDepth(0.1);
			GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
			var depth = BufferUtils.createFloatBuffer(1);
			GL11.glReadPixels((int)(target.width * 0.2), (int)(target.height * 0.8), 1, 1, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, depth);
			check(Math.abs(depth.get(0) - 0.1f) < 0.00001f, "Actual late world depth must be nearer than the OIT surface");
		}
		finally
		{
			GL11.glClearDepth(previousClearDepth);
			state.restore();
			GL30.glDeleteFramebuffers(fbo);
		}
	}

	private static void worldResult(RenderLevelStageEvent.AfterLevel event)
	{
		if (nativePending == 0)
			return;
		RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
		int previous = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
		int fbo = GL30.glGenFramebuffers();
		float[] actual = new float[4];
		try
		{
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, fbo);
			GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, ((GlTexture)target.getColorTexture()).glId(), 0);
			GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, ((GlTexture)target.getDepthTexture()).glId(), 0);
			var depth = BufferUtils.createFloatBuffer(1);
			GL11.glReadPixels((int)(target.width * 0.2), (int)(target.height * 0.8), 1, 1, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, depth);
			check(Math.abs(depth.get(0) - 0.1f) < 0.00001f, "Native resolve must preserve actual late main depth");
			var pixel = BufferUtils.createFloatBuffer(4);
			GL11.glReadPixels((int)(target.width * 0.2), (int)(target.height * 0.8), 1, 1, GL11.GL_RGBA, GL11.GL_FLOAT, pixel);
			pixel.get(actual);
		}
		finally
		{
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previous);
			GL30.glDeleteFramebuffers(fbo);
		}
		check(Math.abs(actual[0] - 128f / 255) < 0.015 && actual[1] < 0.01 && actual[2] < 0.01,
				"Native world resolve failed, tick=" + nativePending + " RGBA=" + Arrays.toString(actual));
		Object executor = Stage41bGeometryProbe.field(PRenderQueue.class, null, "EXECUTOR");
		check(Boolean.FALSE.equals(Stage41bGeometryProbe.field(executor.getClass(), executor, "worldDepthValid")), "Native framegraph must finish snapshot ownership");
		boolean improved = Minecraft.useShaderTransparency();
		check(improved == Minecraft.getInstance().options.improvedTransparency().get(), "Actual extracted graphics mode must match the requested mode");
		if (improved)
		{
			Object snapshot = Stage41bGeometryProbe.field(executor.getClass(), executor, "worldOpaqueDepth");
			var oits = (Map<?, ?>)Stage41bGeometryProbe.field(executor.getClass(), executor, "weightedBlendedOits");
			Object oit = oits.get(snapshot);
			check(oit != null && Stage41bGeometryProbe.field(oit.getClass(), oit, "fabulousMetadataView") instanceof GpuTextureView view && !view.isClosed(),
					"The native world must execute the Fabulous metadata pipeline on live GPU storage");
		}
		PLibDatabase.LOGGER.info("STAGE 4.2b.2 NATIVE WORLD PASS: tick={}, improved={}, RGBA={}, opaque snapshot survives nearer late depth", nativePending,
				improved, Arrays.toString(actual));
		nativeCases++;
		if (nativePending == 260)
			Minecraft.getInstance().options.improvedTransparency().set(originalImproved);
		nativePending = 0;
	}

	private static void shutdown(GameShuttingDownEvent event)
	{
		check(suites == 2 && cases == 24 && nativeCases == 4, "Both postchain suites and four native world passes must complete");
		Object executor = Stage41bGeometryProbe.field(PRenderQueue.class, null, "EXECUTOR");
		check(Stage41bGeometryProbe.field(executor.getClass(), executor, "worldOpaqueDepth") == null, "Opaque snapshot must be released on shutdown");
		PLibDatabase.LOGGER.info("STAGE 4.2b.2 SHUTDOWN PASS: 24 native-postchain cases, four world mode passes; snapshot released");
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
			throw new AssertionError("Stage 4.2b.2: " + message);
	}

	private record Surface(float depth, int color)
	{
	}

	private record Vanilla(int target, float depth, int color)
	{
	}
}
