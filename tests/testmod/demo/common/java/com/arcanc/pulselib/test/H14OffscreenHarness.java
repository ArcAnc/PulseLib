/**
 * @author ArcAnc
 * Created at: 13.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test;

import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.legacy.GlRenderState;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.content.renderer.plan.PTransparencyMode;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PModelCache;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.PResourceCache;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.opengl.GlRenderPipeline;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL40;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Canonical H14 offscreen slice; native world/terrain/weather and baseline modes remain separate. */
public final class H14OffscreenHarness
{
	static final Identifier WHITE = Identifier.fromNamespaceAndPath("pulselibtest", "h14_white");
	private static final String[] TARGETS = {"translucent", "itemEntity", "particles", "weather", "clouds"};
	private static final PRenderQueue.RenderStage STAGE = new PRenderQueue.RenderStage("h14_offscreen");
	private static boolean completed;
	private static int phase;
	private static int cases;
	private static int nativeChains;
	private static int originalWidth;
	private static int originalHeight;
	private static int framebufferWidth;
	private static @Nullable CompletableFuture<Void> reloading;
	private static long deadline;

	private H14OffscreenHarness()
	{
	}

	public static boolean enabled()
	{
		return Boolean.getBoolean("pulselib.test.h14Offscreen");
	}

	public static void frame()
	{
		Minecraft mc = Minecraft.getInstance();
		if (completed || mc.getOverlay() != null || PModelCache.getModels() == null || ClientTestPlugin.GLTF.getModel() == null)
			return;
		if (deadline == 0)
		{
			DemoChecks.plugins(true);
			deadline = System.nanoTime() + 90_000_000_000L;
			GLFW.glfwHideWindow(mc.getWindow().handle());
		}
		check(System.nanoTime() < deadline, "H14 offscreen completion deadline");
		if (reloading != null)
		{
			if (!reloading.isDone())
				return;
			reloading.join();
			reloading = null;
		}
		try (var callerState = new GlRenderState(); var clearState = new H14GpuTargets.ClearState())
		{
			NativeImage white = new NativeImage(1, 1, false);
			white.setPixelABGR(0, 0, -1);
			mc.getTextureManager().register(WHITE, new DynamicTexture(() -> "H14 canonical white", white));
			suite(phase == 0 ? 64 : 96, phase == 0 ? 32 : 48, phase == 0 ? "initial" : "reloaded-resized");
			if (phase == 0)
			{
				IntBuffer width = BufferUtils.createIntBuffer(1);
				IntBuffer height = BufferUtils.createIntBuffer(1);
				GLFW.glfwGetWindowSize(mc.getWindow().handle(), width, height);
				originalWidth = width.get(0);
				originalHeight = height.get(0);
				framebufferWidth = mc.getMainRenderTarget().width;
				GLFW.glfwSetWindowSize(mc.getWindow().handle(), originalWidth + 32, originalHeight + 16);
				phase = 1;
				reloading = mc.reloadResourcePacks();
				return;
			}
			check(mc.getMainRenderTarget().width != framebufferWidth, "Actual window framebuffer must resize");
			check(ClientTestPlugin.models == 2 && cases == 38 && nativeChains == 36, "Both canonical H14 epochs and native chains complete");
			GLFW.glfwSetWindowSize(mc.getWindow().handle(), originalWidth, originalHeight);
			completed = true;
			DemoChecks.writeResult(Map.of("status", "PASS", "fixture", "h14-offscreen", "cases", cases, "epochs", 2,
					"nativeChains", nativeChains, "origin", DemoChecks.jar(H14OffscreenHarness.class), "framebufferResized", true));
			PLibDatabase.LOGGER.info("H14 PIXEL PASS: {} cases, native postchain, direct reference, GL state, resource reload and window resize", cases);
			if (CanonicalProfile.oitWorld())
				CanonicalRun.offscreenComplete();
			else
				mc.stop();
		}
		catch (IOException exception)
		{
			throw new IllegalStateException("Cannot run H14 pixel regression", exception);
		}
	}

	public static void stopped()
	{
		check(completed && cases == 38, "Both H14 offscreen suites finish before shutdown");
		PLibDatabase.LOGGER.info("H14 SHUTDOWN PASS: {} cases", cases);
	}

	static void baseline() throws IOException
	{
		List<Surface> surfaces = List.of(new Surface(0.4f, 0x80FF0000, true));
		float[] fancy = scene(false, false, 64, 32, "fancy-before", surfaces, List.of());
		float[] fabulous = scene(true, true, 64, 32, "fabulous-before", surfaces, List.of());
		check(fancy[0] > 0.55f && fabulous[0] < 0.25f && fabulous[4] > 0.5f, "Original weather routing must reproduce H14");
		PLibDatabase.LOGGER.info("H14 BASELINE REPRODUCED: Fancy opaque {},{},{}; Fabulous opaque {},{},{}; Fabulous sky {},{},{}",
				fancy[0], fancy[1], fancy[2], fabulous[0], fabulous[1], fabulous[2], fabulous[4], fabulous[5], fabulous[6]);
	}

	private static void suite(int width, int height, String prefix) throws IOException
	{
		verify(width, height, prefix + "-front", List.of(new Surface(0.4f, 0x80FF0000, true)), List.of());
		verify(width, height, prefix + "-behind", List.of(new Surface(0.6f, 0x80FF0000, true)), List.of());
		verify(width, height, prefix + "-crossing", List.of(new Surface(0.4f, 0x80FF0000, true, 0.3f),
				new Surface(0.4f, 0x8000FF00, true, -0.3f)), List.of());
		verify(width, height, prefix + "-four-layers", List.of(new Surface(0.2f, 0x40FF0000, true),
				new Surface(0.3f, 0x4000FF00, true), new Surface(0.4f, 0x400000FF, true),
				new Surface(0.45f, 0x40FFFF00, true)), List.of());
		verify(width, height, prefix + "-emissive", List.of(new Surface(0.4f, 0x8060C0FF, true)), List.of());
		verify(width, height, prefix + "-lit", List.of(new Surface(0.4f, 0x80FF0000, false)), List.of());
		for (String target : TARGETS)
		{
			verify(width, height, prefix + "-" + target + "-front", List.of(new Surface(0.4f, 0x80FF0000, true)),
					List.of(new VanillaLayer(target, 0.25f, 0x660000FF)));
			verify(width, height, prefix + "-" + target + "-behind", List.of(new Surface(0.4f, 0x80FF0000, true)),
					List.of(new VanillaLayer(target, 0.65f, 0x660000FF)));
		}
		verify(width, height, prefix + "-interleaved", List.of(new Surface(0.2f, 0x80FF0000, true),
				new Surface(0.4f, 0x8000FF00, true), new Surface(0.7f, 0x800000FF, true)),
				List.of(new VanillaLayer("particles", 0.3f, 0x66FFFF00),
						new VanillaLayer("weather", 0.6f, 0x6600FFFF), new VanillaLayer("clouds", 0.8f, 0x66FF00FF)));
		verify(width, height, prefix + "-opaque-particle", List.of(new Surface(0.4f, 0x80FF0000, true)),
				List.of(new VanillaLayer("particles", 0.3f, 0xFF0000FF)));
		exceptionState(width, height);
	}

	private static void verify(int width, int height, String name, List<Surface> surfaces, List<VanillaLayer> layers) throws IOException
	{
		float[] actual = scene(true, false, width, height, name + "-fabulous", surfaces, layers);
		boolean lit = surfaces.stream().anyMatch(surface -> !surface.emissive());
		if (layers.isEmpty())
		{
			float[] fancy = scene(false, false, width, height, name + "-fancy", surfaces, layers);
			assertRgb(actual, fancy, name + " Fancy/Fabulous parity");
		}
		if (!lit)
			assertRgb(actual, expected(width, surfaces, layers), name + " analytic ordered-alpha reference");
		check(Math.abs(actual[3] - 1f) < 0.01f && Math.abs(actual[7] - 1f) < 0.01f, "Resolved scene alpha must remain opaque");
		cases++;
		PLibDatabase.LOGGER.info("H14 CASE PASS {}: opaque RGB {},{},{}; sky RGB {},{},{}", name,
				actual[0], actual[1], actual[2], actual[4], actual[5], actual[6]);
	}

	private static float[] scene(boolean fabulous, boolean baseline, int width, int height, String name,
	                            List<Surface> surfaces, List<VanillaLayer> layers) throws IOException
	{
		Minecraft mc = Minecraft.getInstance();
		TextureTarget main = new TextureTarget("H14 resolved", width, height, true);
		TextureTarget caller = new TextureTarget("H14 caller", 16, 16, true);
		List<RenderTarget> targets = new ArrayList<>();
		for (String target : TARGETS)
			targets.add(new TextureTarget("H14 " + target, width, height, true));
		int callerVao = GL30.glGenVertexArrays();
		int callerVertexBuffer = GL15.glGenBuffers();
		int callerIndexBuffer = GL15.glGenBuffers();
		var projection = RenderSystem.getProjectionMatrixBuffer();
		var sorting = RenderSystem.getProjectionType();
		RenderSystem.getModelViewStack().pushMatrix().identity();
		try (var state = new GlRenderState(); var identity = new ProjectionMatrixBuffer("H14 identity"))
		{
			for (RenderTarget target : targets)
				clear(target, 0f, 0f, 0f, 0f, 1f);
			for (VanillaLayer layer : layers)
			{
				float[] color = color(layer.color());
				clear(targets.get(Arrays.asList(TARGETS).indexOf(layer.target())),
						color[0] * color[3], color[1] * color[3], color[2] * color[3], color[3], layer.depth());
			}
			clear(main, 0.1f, 0.25f, 0.5f, 1f, 1f);
			try (var binding = new H14GpuTargets.Binding(main, true))
			{
				GlStateManager._enableScissorTest();
				GlStateManager._scissorBox(0, 0, width / 2, height);
				GL11.glClearColor(0.2f, 0.2f, 0.2f, 1f);
				GL11.glClearDepth(0.5);
				GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
				GlStateManager._disableScissorTest();
			}
			RenderSystem.setProjectionMatrix(identity.getBuffer(new Matrix4f()), ProjectionType.ORTHOGRAPHIC);
			RenderSystem.outputColorTextureOverride = main.getColorTextureView();
			RenderSystem.outputDepthTextureOverride = main.getDepthTextureView();
			PGeometryData geometry = atlasQuad();
			for (Surface surface : surfaces)
			{
				var type = surface.emissive() ? PRenderTypes.RenderTypeProvider.trianglesEmissiveTranslucent(WHITE) :
						PRenderTypes.RenderTypeProvider.trianglesTranslucent(WHITE);
				PRenderQueue.submit(STAGE, type, geometry, new PInstanceHeader(
						new Matrix4f().translation(0f, 0f, surface.depth() * 2f - 1f).m02(surface.slope()),
						surface.color(), 0xF000F0, OverlayTexture.NO_OVERLAY), PTransparencyMode.OIT);
			}
			PRenderQueue.flush(STAGE, main);
			if (fabulous && baseline)
			{
				PRenderQueue.compositeTranslucency(targets.get(3));
				vanilla(main, targets);
			}
			else
			{
				if (fabulous)
					vanilla(main, targets);
				float leftDepth = depth(main, width / 4);
				float rightDepth = depth(main, width * 3 / 4);
				try (var binding = new H14GpuTargets.Binding(caller, true))
				{
					GlStateManager._viewport(2, 3, 5, 7);
					GlStateManager._enableDepthTest();
					GlStateManager._depthFunc(GL11.GL_GREATER);
					GlStateManager._depthMask(true);
					GlStateManager._enableScissorTest();
					GlStateManager._scissorBox(0, 0, width, height);
					GlStateManager._enableBlend();
					GlStateManager._blendFuncSeparate(GL11.GL_ONE, GL11.GL_ZERO, GL11.GL_ZERO, GL11.GL_ONE);
					GL30.glBindVertexArray(callerVao);
					GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, callerVertexBuffer);
					GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, callerIndexBuffer);
					if (GL.getCapabilities().OpenGL40)
					{
						GL30.glDisablei(GL11.GL_BLEND, 1);
						GL40.glBlendFuncSeparatei(1, GL11.GL_DST_COLOR, GL11.GL_SRC_COLOR, GL11.GL_ONE, GL11.GL_ZERO);
						GL40.glBlendEquationSeparatei(1, GL14.GL_FUNC_REVERSE_SUBTRACT, GL14.GL_FUNC_SUBTRACT);
					}
					var program = (GlRenderPipeline)RenderSystem.getDevice().precompilePipeline(RenderPipelines.GUI);
					check(program.isValid(), "Native caller shader program must be valid");
					GlStateManager._glUseProgram(program.program().getProgramId());
					int white = ((GlTexture)mc.getTextureManager().getTexture(WHITE).getTexture()).glId();
					for (int unit = 0; unit < 12; unit++)
					{
						GlStateManager._activeTexture(GL13.GL_TEXTURE0 + unit);
						GlStateManager._bindTexture(white);
					}
					if (fabulous)
					{
						GlStateManager._activeTexture(GL13.GL_TEXTURE0 + 15);
						GL11.glBindTexture(GL11.GL_TEXTURE_2D, white);
					}
					int activeShader = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
					int[] before = glState();
					if (fabulous)
					{
						if (!Boolean.getBoolean("pulselib.test.h14SkipResolve"))
							PRenderQueue.compositeFabulousTranslucency(main, targets);
					}
					else
						PRenderQueue.compositeTranslucency(main);
					check(Arrays.equals(before, glState()), name + " composite must restore GL state");
					check(GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM) == activeShader, name + " RenderSystem shader must remain unchanged");
					check(leftDepth == depth(main, width / 4) && rightDepth == depth(main, width * 3 / 4), name + " must not write depth");
					for (RenderTarget target : targets)
					{
						float expectedDepth = layers.stream().filter(layer -> target == targets.get(Arrays.asList(TARGETS).indexOf(layer.target()))).
								map(VanillaLayer::depth).findFirst().orElse(1f);
						check(Math.abs(depth(target, width / 4) - expectedDepth) < 0.00001f, name + " native target depth changed");
					}
				}
			}
			check(GL11.glGetError() == GL11.GL_NO_ERROR, name + " OpenGL error");
			save(main, name);
			return pixels(main);
		}
		finally
		{
			GlStateManager._activeTexture(GL13.GL_TEXTURE0);
			GL30.glBindVertexArray(0);
			GL30.glDeleteVertexArrays(callerVao);
			GL15.glDeleteBuffers(callerVertexBuffer);
			GL15.glDeleteBuffers(callerIndexBuffer);
			PRenderQueue.cleanup();
			targets.forEach(RenderTarget::destroyBuffers);
			main.destroyBuffers();
			caller.destroyBuffers();
			RenderSystem.getModelViewStack().popMatrix();
			RenderSystem.setProjectionMatrix(projection, sorting);
		}
	}

	private static void vanilla(RenderTarget main, List<RenderTarget> targets)
	{
		FrameGraphBuilder frame = new FrameGraphBuilder();
		LevelTargetBundle bundle = new LevelTargetBundle();
		bundle.main = frame.importExternal("main", main);
		bundle.translucent = frame.importExternal("translucent", targets.get(0));
		bundle.itemEntity = frame.importExternal("item_entity", targets.get(1));
		bundle.particles = frame.importExternal("particles", targets.get(2));
		bundle.weather = frame.importExternal("weather", targets.get(3));
		bundle.clouds = frame.importExternal("clouds", targets.get(4));
		var chain = Minecraft.getInstance().getShaderManager().getPostChain(Identifier.withDefaultNamespace("transparency"), LevelTargetBundle.SORTING_TARGETS);
		check(chain != null, "Real MC26 transparency postchain must load");
		chain.addToFrame(frame, main.width, main.height, bundle);
		List<String> passes = new ArrayList<>();
		frame.execute(GraphicsResourceAllocator.UNPOOLED, new FrameGraphBuilder.Inspector()
		{
			@Override
			public void beforeExecutePass(String pass)
			{
				passes.add(pass);
			}
		});
		check(!passes.isEmpty(), "Real vanilla framegraph passes must execute");
		nativeChains++;
	}

	private static void exceptionState(int width, int height)
	{
		TextureTarget target = new TextureTarget("H14 exception", width, height, true);
		try
		{
			clear(target, 0f, 0f, 0f, 1f, 1f);
			PRenderQueue.submit(STAGE, PRenderTypes.RenderTypeProvider.trianglesEmissiveTranslucent(WHITE), quad(),
					new PInstanceHeader(new Matrix4f(), 0x80FFFFFF, 0xF000F0, OverlayTexture.NO_OVERLAY), PTransparencyMode.OIT);
			PRenderQueue.flush(STAGE, target);
			List<RenderTarget> invalid = new ArrayList<>(List.of(target, target, target, target, target));
			invalid.set(4, null);
			int[] before = glState();
			boolean failed = false;
			try
			{
				PRenderQueue.compositeFabulousTranslucency(target, invalid);
			}
			catch (NullPointerException expected)
			{
				failed = true;
			}
			check(failed && Arrays.equals(before, glState()), "Composite exception must restore GL state");
			cases++;
		}
		finally
		{
			PRenderQueue.cleanup();
			target.destroyBuffers();
		}
	}

	private static float[] expected(int width, List<Surface> surfaces, List<VanillaLayer> layers)
	{
		float[] result = new float[8];
		for (int side = 0; side < 2; side++)
		{
			List<ReferenceLayer> ordered = new ArrayList<>();
			ordered.add(new ReferenceLayer(side == 0 ? 0.5f : 1f,
					side == 0 ? new float[]{0.2f, 0.2f, 0.2f, 1f} : new float[]{0.1f, 0.25f, 0.5f, 1f}));
			for (VanillaLayer layer : layers)
				ordered.add(new ReferenceLayer(layer.depth(), color(layer.color())));
			float x = 2f * (width * (side * 2 + 1) / 4 + 0.5f) / width - 1f;
			for (Surface surface : surfaces)
			{
				float depth = surface.depth() + surface.slope() * x * 0.5f;
				if (side != 0 || depth <= 0.5f)
					ordered.add(new ReferenceLayer(depth, color(surface.color())));
			}
			ordered.sort(Comparator.comparingDouble(ReferenceLayer::depth).reversed());
			for (ReferenceLayer layer : ordered)
				for (int component = 0; component < 3; component++)
					result[side * 4 + component] = result[side * 4 + component] * (1f - layer.color()[3]) +
							layer.color()[component] * layer.color()[3];
			result[side * 4 + 3] = 1f;
		}
		return result;
	}

	static int[] glState()
	{
		List<Integer> state = new ArrayList<>();
		int[] parameters = {GL30.GL_DRAW_FRAMEBUFFER_BINDING, GL30.GL_READ_FRAMEBUFFER_BINDING,
				GL20.GL_CURRENT_PROGRAM, GL30.GL_VERTEX_ARRAY_BINDING, GL15.GL_ARRAY_BUFFER_BINDING,
				GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING, GL13.GL_ACTIVE_TEXTURE, GL11.GL_DEPTH_FUNC};
		for (int parameter : parameters)
			state.add(GL11.glGetInteger(parameter));
		for (int parameter : new int[]{GL11.GL_VIEWPORT, GL11.GL_SCISSOR_BOX})
		{
			IntBuffer values = BufferUtils.createIntBuffer(4);
			GL11.glGetIntegerv(parameter, values);
			for (int index = 0; index < 4; index++)
				state.add(values.get(index));
		}
		state.add(GL11.glIsEnabled(GL11.GL_DEPTH_TEST) ? 1 : 0);
		state.add(GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK) ? 1 : 0);
		state.add(GL11.glIsEnabled(GL11.GL_SCISSOR_TEST) ? 1 : 0);
		int count = GL11.glGetInteger(GL20.GL_MAX_DRAW_BUFFERS);
		for (int index = 0; index < count; index++)
		{
			state.add(GL11.glGetInteger(GL20.GL_DRAW_BUFFER0 + index));
			state.add(GL30.glIsEnabledi(GL11.GL_BLEND, index) ? 1 : 0);
			for (int parameter : new int[]{GL14.GL_BLEND_SRC_RGB, GL14.GL_BLEND_DST_RGB, GL14.GL_BLEND_SRC_ALPHA,
					GL14.GL_BLEND_DST_ALPHA, GL20.GL_BLEND_EQUATION_RGB, GL20.GL_BLEND_EQUATION_ALPHA})
				state.add(GL30.glGetIntegeri(parameter, index));
		}
		int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
		state.add(GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D));
		for (int unit = 0; unit < 12; unit++)
		{
			GlStateManager._activeTexture(GL13.GL_TEXTURE0 + unit);
			state.add(GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D));
		}
		GlStateManager._activeTexture(active);
		return state.stream().mapToInt(Integer::intValue).toArray();
	}

	static void clear(RenderTarget target, float red, float green, float blue, float alpha, float depth)
	{
		try (var binding = new H14GpuTargets.Binding(target, true))
		{
			GlStateManager._disableScissorTest();
			GlStateManager._depthMask(true);
			GL11.glClearColor(red, green, blue, alpha);
			GL11.glClearDepth(depth);
			GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		}
	}

	/** Maps the canonical standalone white texel to the atlas used by the 26.1 backend. */
	static PGeometryData atlasQuad()
	{
		var sprite = PResourceCache.getTextureAtlas().getSprite(PResourceCache.spriteId(WHITE));
		check(sprite.contents().name().equals(PResourceCache.spriteId(WHITE)), "H14 white atlas sprite must survive resource reload");
		PGeometryData canonical = quad();
		ByteBuffer vertices = BufferUtils.createByteBuffer(canonical.vertices().remaining()).order(ByteOrder.nativeOrder());
		vertices.put(canonical.vertices()).flip();
		for (int vertex = 0; vertex < 4; vertex++)
		{
			vertices.putFloat(vertex * canonical.vertexStride() + 12, (sprite.getU0() + sprite.getU1()) * 0.5f);
			vertices.putFloat(vertex * canonical.vertexStride() + 16, (sprite.getV0() + sprite.getV1()) * 0.5f);
		}
		return new PGeometryData(vertices, canonical.indices(), canonical.vertexStride(), canonical.indexCount(), canonical.indexType());
	}

	static PGeometryData quad()
	{
		ByteBuffer vertices = BufferUtils.createByteBuffer(4 * 24).order(ByteOrder.nativeOrder());
		for (float[] position : new float[][]{{-0.9f, -0.9f}, {0.9f, -0.9f}, {0.9f, 0.9f}, {-0.9f, 0.9f}})
			vertices.putFloat(position[0]).putFloat(position[1]).putFloat(0f).
					putFloat(0.5f).putFloat(0.5f).put((byte)0).put((byte)0).put((byte)127).put((byte)0);
		vertices.flip();
		ByteBuffer indices = BufferUtils.createByteBuffer(6 * 2).order(ByteOrder.nativeOrder());
		for (short index : new short[]{0, 1, 2, 0, 2, 3})
			indices.putShort(index);
		indices.flip();
		return new PGeometryData(vertices, indices, 24, 6, PGeometryData.IndexType.UNSIGNED_SHORT);
	}

	static float[] color(int color)
	{
		return new float[]{((color >> 16) & 255) / 255f, ((color >> 8) & 255) / 255f, (color & 255) / 255f, ((color >>> 24) & 255) / 255f};
	}

	static float[] pixels(RenderTarget target)
	{
		try (var binding = new H14GpuTargets.Binding(target, false))
		{
			FloatBuffer pixel = BufferUtils.createFloatBuffer(4);
			float[] result = new float[8];
			for (int side = 0; side < 2; side++)
			{
				pixel.clear();
				GL11.glReadPixels(target.width * (side * 2 + 1) / 4, target.height / 2, 1, 1, GL11.GL_RGBA, GL11.GL_FLOAT, pixel);
				pixel.get(result, side * 4, 4);
			}
			return result;
		}
	}

	static float depth(RenderTarget target, int x)
	{
		try (var binding = new H14GpuTargets.Binding(target, false))
		{
			FloatBuffer value = BufferUtils.createFloatBuffer(1);
			GL11.glReadPixels(x, target.height / 2, 1, 1, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, value);
			return value.get(0);
		}
	}

	static void save(RenderTarget target, String name) throws IOException
	{
		Path output = DemoChecks.runDirectory().resolve("h14-offscreen");
		Files.createDirectories(output);
		ByteBuffer pixels = BufferUtils.createByteBuffer(target.width * target.height * 4);
		try (var binding = new H14GpuTargets.Binding(target, false))
		{
			GL11.glReadPixels(0, 0, target.width, target.height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
		}
		try (NativeImage image = new NativeImage(target.width, target.height, false))
		{
			for (int y = 0; y < target.height; y++)
				for (int x = 0; x < target.width; x++)
				{
					int offset = (y * target.width + x) * 4;
					int rgba = (pixels.get(offset) & 255) | (pixels.get(offset + 1) & 255) << 8 |
							(pixels.get(offset + 2) & 255) << 16 | (pixels.get(offset + 3) & 255) << 24;
					image.setPixelABGR(x, target.height - y - 1, rgba);
				}
			image.writeToFile(output.resolve(name + ".png"));
		}
	}

	static void assertRgb(float[] actual, float[] expected, String name)
	{
		for (int side = 0; side < 2; side++)
			for (int component = 0; component < 3; component++)
			{
				int index = side * 4 + component;
				check(Math.abs(actual[index] - expected[index]) < 0.025f,
						name + ": pixel " + side + " component " + component + " expected " + expected[index] + ", got " + actual[index]);
			}
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
			throw new AssertionError("H14: " + message);
	}

	private record Surface(float depth, int color, boolean emissive, float slope)
	{
		private Surface(float depth, int color, boolean emissive)
		{
			this(depth, color, emissive, 0f);
		}
	}

	private record VanillaLayer(String target, float depth, int color)
	{
	}

	private record ReferenceLayer(float depth, float[] color)
	{
	}
}
