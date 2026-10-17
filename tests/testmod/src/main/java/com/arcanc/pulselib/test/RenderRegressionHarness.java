/**
 * @author ArcAnc
 * Created at: 17.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test;

import com.arcanc.pulselib.content.animatable.AnimManagerKey;
import com.arcanc.pulselib.content.animatable.instance.InstanceAnimationManager;
import com.arcanc.pulselib.content.animatable.singleton.SingletonAnimationManager;
import com.arcanc.pulselib.content.model.textures.PAlphaMode;
import com.arcanc.pulselib.content.model.deformer.gpu.PGpuDeformerBuffers;
import com.arcanc.pulselib.content.registration.block.block_entity.TestBlockEntity;
import com.arcanc.pulselib.content.registration.entity.renderer.TestEntityRender;
import com.arcanc.pulselib.content.registration.item.TestBlockItem;
import com.arcanc.pulselib.content.renderer.PRenderContexts;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.PRenderStagesHandler;
import com.arcanc.pulselib.content.renderer.legacy.GlRenderState;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.helpers.PLibHelper;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.ReportedException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL40;
import org.lwjgl.opengl.GL;

import java.nio.ByteBuffer;
import java.util.Arrays;

/** Pixel and state regressions, run by the existing opt-in smoke fixture. */
public final class RenderRegressionHarness
{
	private static final PGeometryData QUAD = H14Harness.quad();
	private static final ResourceLocation WHITE = DemoContent.id("render_regression_white");
	private static boolean completed;
	private static int cases;

	private RenderRegressionHarness()
	{
	}

	public static void run()
	{
		if (completed)
		{
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		NativeImage white = new NativeImage(1, 1, false);
		white.setPixelRGBA(0, 0, -1);
		mc.getTextureManager().register(WHITE, new DynamicTexture(white));
		Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
		VertexSorting sorting = RenderSystem.getVertexSorting();
		GraphicsStatus graphics = mc.options.graphicsMode().get();
		RenderSystem.getModelViewStack().pushMatrix();
		try (var state = new GlRenderState())
		{
			RenderSystem.getModelViewStack().identity();
			RenderSystem.applyModelViewMatrix();
			RenderSystem.setProjectionMatrix(new Matrix4f(), VertexSorting.ORTHOGRAPHIC_Z);
			for (GraphicsStatus mode : GraphicsStatus.values())
			{
				mc.options.graphicsMode().set(mode);
				worldPasses(mode);
				localScopes(mode);
			}
			if (mc.level != null)
			{
				DisposableTestWorlds.requireCurrentWorld();
				managerAccess();
				actualEntities();
			}
			completed = true;
			PLibDatabase.LOGGER.info("H13/H15 PIXEL PASS: {} cases; stage depth, alpha sorting, instancing, isolated queues, nested targets and exception GL state", cases);
		}
		finally
		{
			TestEntityRender.regressionAlphaMode = null;
			mc.options.graphicsMode().set(graphics);
			RenderSystem.getModelViewStack().popMatrix();
			RenderSystem.applyModelViewMatrix();
			RenderSystem.setProjectionMatrix(projection, sorting);
		}
	}

	private static RenderType alpha()
	{
		// Conventional alpha pipeline deliberately outside the built-in OIT membership set.
		return RenderType.create("pulselibtest:sorted_alpha", PRenderTypes.VertexFormatProvider.POSITION_TEX_NORMAL,
				com.mojang.blaze3d.vertex.VertexFormat.Mode.TRIANGLES, 1536, false, false,
				RenderType.CompositeState.builder().
						setShaderState(new RenderStateShard.ShaderStateShard(PRenderTypes.ShadersProvider::trianglesTranslucentEmissive)).
						setTextureState(new RenderStateShard.TextureStateShard(WHITE, false, false)).
						setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY).
						setCullState(RenderStateShard.NO_CULL).createCompositeState(false));
	}

	private static void worldPasses(GraphicsStatus mode)
	{
		TextureTarget target = target();
		try
		{
			RenderType alpha = alpha();
			for (boolean emissive : new boolean[]{false, true})
			{
				for (int scenario = 0; scenario < 4; scenario++)
				{
					clear(target);
					try (var world = PRenderContexts.enter(PRenderContexts.Context.WORLD))
					{
						RenderType opaque = emissive ? PRenderTypes.RenderTypeProvider.trianglesSolidEmissive(WHITE) :
								PRenderTypes.RenderTypeProvider.trianglesSolid(WHITE);
						for (int instance = 0; instance < 32; instance++)
						{
							mesh(opaque, 0, 0xFFFF0000);
						}
						PRenderStagesHandler.afterBlockEntities();
						float red = H14Harness.pixels(target)[0];
						check(red > 0.3f, mode + " early opaque entity pixel");
						check(Math.abs(H14Harness.depth(target, 16) - 0.5f) < 0.0001f, "Opaque depth before terrain");
						float opacity = scenario == 2 ? 64f / 255f : 128f / 255f;
						// Water/glass proxy uses actual alpha draw and the early depth attachment.
						try (var terrain = PRenderContexts.enter(PRenderContexts.Context.OFFSCREEN))
						{
							if (scenario == 3)
							{
								PRenderQueue.submitEntityMesh(alpha, QUAD, new PInstanceHeader(
										new Matrix4f().m02(0.8f), 0x800000FF, 0xF000F0, OverlayTexture.NO_OVERLAY));
							}
							else
							{
								mesh(alpha, scenario == 0 ? 0.4f : -0.4f, scenario == 2 ? 0x400000FF : 0x800000FF);
							}
						}
						PRenderStagesHandler.afterParticles();
						PRenderStagesHandler.afterWeather(target);
						float[] pixels = H14Harness.pixels(target);
						for (int side = 0; side < 2; side++)
						{
							boolean front = scenario != 0 && (scenario != 3 || side == 0);
							float a = front ? opacity : 0;
							rgb(pixels, side, red * (1 - a), 0, a, mode + " opaque/terrain case " + scenario);
						}
						cases++;
					}
				}
			}
			clear(target);
			try (var world = PRenderContexts.enter(PRenderContexts.Context.WORLD))
			{
				// Different sources must sort together, far red then near green.
				mesh(alpha, 0.2f, 0x8000FF00);
				PRenderQueue.submitBlockEntityTranslucentMesh(alpha, QUAD,
						new PInstanceHeader(new Matrix4f().translation(0, 0, 0.7f), 0x80FF0000, 0xF000F0, OverlayTexture.NO_OVERLAY));
				PRenderStagesHandler.afterBlockEntities();
				rgb(H14Harness.pixels(target), 0, 0, 0, 0, "Transparency retained by early pass");
				PRenderStagesHandler.afterParticles();
				PRenderStagesHandler.afterWeather(target);
				rgb(H14Harness.pixels(target), 0, 0.25f, 0.5f, 0, "Combined Entity/BlockEntity transparency");
				cases++;
			}
		}
		finally
		{
			target.destroyBuffers();
			Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
		}
	}

	private static void localScopes(GraphicsStatus mode)
	{
		TextureTarget world = target();
		TextureTarget gui = target();
		TextureTarget nested = target();
		try
		{
			clear(nested);
			clear(gui);
			clear(world);
			try (var worldScope = PRenderContexts.enter(PRenderContexts.Context.WORLD))
			{
				mesh(PRenderTypes.RenderTypeProvider.trianglesSolidEmissive(WHITE), 0, 0xFFFF0000);
				gui.bindWrite(true);
				RenderSystem.enableScissor(1, 2, 60, 58);
				RenderSystem.depthFunc(GL11.GL_GREATER);
				RenderSystem.depthMask(false);
				RenderSystem.enableBlend();
				RenderSystem.blendFuncSeparate(GL11.GL_ONE, GL11.GL_ZERO, GL11.GL_ZERO, GL11.GL_ONE);
				if (GL.getCapabilities().OpenGL40)
				{
					GL30.glEnablei(GL11.GL_BLEND, 1);
					GL40.glBlendFuncSeparatei(1, GL11.GL_DST_COLOR, GL11.GL_SRC_COLOR, GL11.GL_ONE, GL11.GL_ZERO);
					GL40.glBlendEquationSeparatei(1, GL14.GL_FUNC_REVERSE_SUBTRACT, GL14.GL_FUNC_SUBTRACT);
				}
				int[] before = H14Harness.glState();
				var shader = RenderSystem.getShader();
				try (var guiScope = PRenderContexts.enter(PRenderContexts.Context.GUI))
				{
					check(PRenderContexts.current() == PRenderContexts.Context.GUI, "Explicit GUI context");
					mesh(PRenderTypes.RenderTypeProvider.trianglesSolidEmissive(WHITE), 0, 0xFF00FF00);
					try (var parentState = new GlRenderState())
					{
						nested.bindWrite(true);
						try (var nestedScope = PRenderContexts.enter(PRenderContexts.Context.OFFSCREEN))
						{
							mesh(PRenderTypes.RenderTypeProvider.trianglesSolidEmissive(WHITE), 0, 0xFF0000FF);
						}
						rgb(H14Harness.pixels(nested), 0, 0, 0, 1, "Successful nested render uses child target");
						cases++;
					}
					int[] outer = H14Harness.glState();
					try
					{
						try (var nestedScope = PRenderContexts.enter(PRenderContexts.Context.OFFSCREEN))
						{
							nested.bindWrite(true);
							RenderSystem.viewport(3, 4, 15, 16);
							RenderSystem.depthFunc(GL11.GL_GREATER);
							RenderSystem.disableScissor();
							throw new ExpectedFailure();
						}
					}
					catch (ExpectedFailure expected)
					{
						check(Arrays.equals(outer, H14Harness.glState()), "Nested exception must restore GL state");
						check(PRenderContexts.current() == PRenderContexts.Context.GUI, "Nested exception must restore GUI context");
					}
				}
				check(Arrays.equals(before, H14Harness.glState()), mode + " local flush must restore complete GL state");
				check(RenderSystem.getShader() == shader, "RenderSystem shader restored");
				rgb(H14Harness.pixels(gui), 0, 0, 1, 0, "GUI writes only its target");
				rgb(H14Harness.pixels(world), 0, 0, 0, 0, "World queue remains pending during GUI preview");
				world.bindWrite(true);
				RenderSystem.disableScissor();
				PRenderQueue.flushOpaque(PRenderQueue.RenderStage.ENTITIES);
				rgb(H14Harness.pixels(world), 0, 1, 0, 0, "World flush excludes GUI");
				rgb(H14Harness.pixels(gui), 0, 0, 1, 0, "World flush never writes GUI framebuffer");
				cases++;
			}
			clear(gui);
			try (var scope = PRenderContexts.enter(PRenderContexts.Context.OFFSCREEN))
			{
				// Orthographic projection uses signed depth, not radial distance.
				mesh(PRenderTypes.RenderTypeProvider.trianglesTranslucentEmissive(WHITE), -0.8f, 0x8000FF00);
				mesh(PRenderTypes.RenderTypeProvider.trianglesTranslucentEmissive(WHITE), 0.2f, 0x80FF0000);
			}
			rgb(H14Harness.pixels(gui), 0, 0.25f, 0.5f, 0, "Local OIT material uses sorted alpha without world OIT state");
			cases++;
		}
		finally
		{
			RenderSystem.disableScissor();
			world.destroyBuffers();
			gui.destroyBuffers();
			nested.destroyBuffers();
			Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
		}
	}

	private static void managerAccess()
	{
		Minecraft mc = Minecraft.getInstance();
		var entity = DemoContent.entity.get().create(mc.level);
		var block = new TestBlockEntity(BlockPos.ZERO, DemoContent.block.get().defaultBlockState());
		check(entity != null, "Entity field initializer");
		check(PLibHelper.getManager(entity) instanceof InstanceAnimationManager<?>, "Automatic Entity strategy");
		check(PLibHelper.getManager(block) instanceof InstanceAnimationManager<?>, "Automatic BlockEntity strategy");
		check(PLibHelper.getManager(entity) == PLibHelper.getManager(entity), "Entity keeps its manager");
		check(PLibHelper.getManager(block) == PLibHelper.getManager(block), "BlockEntity keeps its manager");
		check(!PLibHelper.getManager(block).getControllers().isEmpty(), "BlockEntity controllers ready");
		var item = (TestBlockItem)DemoContent.blockItem.get();
		ItemStack first = new ItemStack(item);
		ItemStack second = new ItemStack(item);
		check(!AnimManagerKey.of(first).equals(AnimManagerKey.of(second)), "Distinct stack runtime IDs");
		check(PLibHelper.getManager(first) instanceof SingletonAnimationManager<?>, "Automatic Item strategy");
		check(PLibHelper.createInstanceManager(item) instanceof InstanceAnimationManager<?>, "Explicit Item instance strategy");
		check(PLibHelper.createSingletonManager(entity) instanceof SingletonAnimationManager<?>, "Explicit Entity singleton strategy");
		check(PLibHelper.getManager(first) != PLibHelper.getManager(second), "Distinct stack managers");
		check(PLibHelper.getManager(first) == PLibHelper.getManager(first.copy()), "Copy keeps intentional identity");
		check(!PLibHelper.getManager(first).getControllers().isEmpty(), "ItemStack controllers ready");
		PLibDatabase.LOGGER.info("H18 GAME PASS: Entity/BlockEntity field initializers and typed access, ItemStack identities/controllers");
	}

	private static void actualEntities()
	{
		Minecraft mc = Minecraft.getInstance();
		var entity = DemoContent.entity.get().create(mc.level);
		check(entity != null, "Real entity for dispatcher");
		TextureTarget target = target();
		var dispatcher = mc.getEntityRenderDispatcher();
		try
		{
			dispatcher.setRenderShadow(false);
			for (PAlphaMode alpha : new PAlphaMode[]{PAlphaMode.OPAQUE, PAlphaMode.TRANSLUCENT})
			{
				TestEntityRender.regressionAlphaMode = alpha;
				clear(target);
				RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(-2, 2, -0.5f, 3.5f, -10, 10), VertexSorting.ORTHOGRAPHIC_Z);
				check(PRenderContexts.current() == PRenderContexts.Context.OFFSCREEN, "Unscoped FBO is offscreen");
				int[] before = H14Harness.glState();
				dispatcher.render(entity, 0, 0, 0, 0, 1, new PoseStack(), mc.renderBuffers().bufferSource(), 0xF000F0);
				check(Arrays.equals(before, H14Harness.glState()), "Actual dispatcher restores GL state");
				check(PGpuDeformerBuffers.streams().deformers().values().isEmpty(), "Standalone preview retires deformation values");
				check(coloredPixels(target) > 10, "Actual " + alpha + " entity must render into offscreen framebuffer");
				clear(target);
				// Vanilla GUI entry point, with an actual PulseLib renderer and its original model.
				RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, 64, 64, 0, -1000, 1000), VertexSorting.ORTHOGRAPHIC_Z);
				InventoryScreen.renderEntityInInventory(new GuiGraphics(mc, mc.renderBuffers().bufferSource()),
						32, 56, 16, new Vector3f(), new Quaternionf().rotationZ((float)Math.PI), null, entity);
				check(coloredPixels(target) > 10, "Actual " + alpha + " inventory entity must render");
				clear(target);
				PRenderQueue.flush(PRenderQueue.RenderStage.ENTITIES);
				rgb(H14Harness.pixels(target), 0, 0, 0, 0, "Actual GUI/offscreen geometry must never leak into world flush");
				cases += 2;
			}
			clear(target);
			int[] exceptionState = H14Harness.glState();
			TestEntityRender.regressionFailAfter = 1;
			boolean failed = false;
			try
			{
				try (var outer = PRenderContexts.enter(PRenderContexts.Context.GUI))
				{
					try
					{
						dispatcher.render(entity, 0, 0, 0, 0, 1, new PoseStack(), mc.renderBuffers().bufferSource(), 0xF000F0);
					}
					catch (ReportedException expected)
					{
						check(expected.getCause() instanceof IllegalStateException &&
								"Intentional H15 renderer failure".equals(expected.getCause().getMessage()), "Expected renderer exception");
						check(PRenderContexts.current() == PRenderContexts.Context.GUI, "Failed dispatcher restores enclosing GUI scope");
						failed = true;
					}
				}
			}
			finally
			{
				TestEntityRender.regressionFailAfter = -1;
			}
			check(failed, "Actual renderer must throw after one partial submission");
			check(Arrays.equals(exceptionState, H14Harness.glState()), "Actual renderer exception restores GL state");
			rgb(H14Harness.pixels(target), 0, 0, 0, 0, "Failed renderer discards partial local submissions");
			cases++;

			PLibDatabase.LOGGER.info("H15 ENTITY PIXEL PASS: actual EntityRenderDispatcher and InventoryScreen, opaque/translucent, independent framebuffer and GL restoration");
		}
		finally
		{
			TestEntityRender.regressionAlphaMode = null;
			dispatcher.setRenderShadow(true);
			target.destroyBuffers();
			mc.getMainRenderTarget().bindWrite(true);
		}
	}

	private static int coloredPixels(RenderTarget target)
	{
		GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, target.frameBufferId);
		ByteBuffer pixels = BufferUtils.createByteBuffer(target.width * target.height * 4);
		GL11.glReadPixels(0, 0, target.width, target.height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
		int colored = 0;
		for (int offset = 0; offset < pixels.capacity(); offset += 4)
		{
			if ((pixels.get(offset) & 255) + (pixels.get(offset + 1) & 255) + (pixels.get(offset + 2) & 255) > 15)
			{
				colored++;
			}
		}
		return colored;
	}

	private static TextureTarget target()
	{
		return new TextureTarget(64, 64, true, Minecraft.ON_OSX);
	}

	private static void clear(RenderTarget target)
	{
		H14Harness.clear(target, 0, 0, 0, 1, 1);
	}

	private static void mesh(RenderType type, float z, int color)
	{
		PRenderQueue.submitEntityMesh(type, QUAD, new PInstanceHeader(new Matrix4f().translation(0, 0, z),
				color, 0xF000F0, OverlayTexture.NO_OVERLAY));
	}

	private static void rgb(float[] actual, int side, float red, float green, float blue, String message)
	{
		float[] expected = {red, green, blue};
		for (int channel = 0; channel < 3; channel++)
		{
			check(Math.abs(actual[side * 4 + channel] - expected[channel]) < 0.035f,
					message + " channel " + channel + " expected " + expected[channel] + " got " + actual[side * 4 + channel]);
		}
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
		{
			throw new AssertionError("H13/H15/H18: " + message);
		}
	}

	private static final class ExpectedFailure extends RuntimeException
	{
	}
}
