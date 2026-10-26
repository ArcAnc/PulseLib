/**
 * @author ArcAnc
 * Created at: 26.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1;

import com.arcanc.pulselib.content.model.textures.PAlphaMode;
import com.arcanc.pulselib.content.model.baked.PBakedBone;
import com.arcanc.pulselib.content.model.baked.PBakedMesh;
import com.arcanc.pulselib.content.model.baked.PMeshRenderContext;
import com.arcanc.pulselib.content.registration.item.TestBlockItem;
import com.arcanc.pulselib.content.registration.item.renderer.renderState.TestBlockItemRenderState;
import com.arcanc.pulselib.content.renderer.PItemRenderer;
import com.arcanc.pulselib.content.renderer.PRenderContexts;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.PWorldRenderPass;
import com.arcanc.pulselib.content.renderer.base.PEntityRenderState;
import com.arcanc.pulselib.content.renderer.base.PBlockRenderState;
import com.arcanc.pulselib.content.renderer.base.PWorldRenderState;
import com.arcanc.pulselib.content.renderer.legacy.GlRenderState;
import com.arcanc.pulselib.content.renderer.plan.PFrameCompiler;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.PResourceCache;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.arcanc.pulselib.test.stage1.Stage41bGeometryProbe.field;
import static com.arcanc.pulselib.test.stage1.Stage42b1OitProbe.clear;
import static com.arcanc.pulselib.test.stage1.Stage42b1OitProbe.pixel;

/** Standalone vanilla owners, native target eligibility and actual item entrypoints in changing invocation targets. */
public final class Stage43cItemProbe
{
	private static final PRenderQueue.RenderStage SENTINEL = new PRenderQueue.RenderStage("stage43c sentinel");
	private static int ticks;
	private static int phase;
	private static int frames;
	private static int suites;
	private static int pixels;
	private static int worldItems;
	private static int hands;
	private static int foil;
	private static int plain;
	private static int nativeFrames;
	private static boolean finished;
	private static boolean advance;
	private static boolean reloaded;
	private static boolean fail;
	private static boolean direct;
	private static boolean lastFoil;
	private static @Nullable CompletableFuture<Void> reload;

	private Stage43cItemProbe()
	{
	}


	public static void register()
	{
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Stage43cItemProbe::early);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Stage43cItemProbe::shutdown);
	}

	public static void tick()
	{
		var mc = Minecraft.getInstance();
		if (mc.level == null)
			return;
		mc.mouseHandler.releaseMouse();
		KeyMapping.releaseAll();
		if (++ticks > 1800)
			throw new AssertionError("Standalone item phases timed out");
		if (ticks == 20)
			mode();
		if (advance)
		{
			advance = false;
			phase++;
			if (phase != 3 && phase < 6)
				mode();
		}
		if (phase == 3 && reload == null && !reloaded)
			reload = mc.reloadResourcePacks();
		if (reload != null && reload.isDone() && phase == 3 && !reloaded)
		{
			reload.join();
			reloaded = true;
			mode();
		}
		if (phase == 6 && !finished)
		{
			check(worldItems > 20 && hands > 20, "Actual standalone world and first-person item submissions");
			check(foil > 0 && plain > 0, "Both foil flags reached the native special renderer");
			Stage2PluginProbe.verify(true);
			finished = true;
			Screenshot.grab(mc.gameDirectory, "stage43c-items.png", mc.getMainRenderTarget(), 1, message -> PLibDatabase.LOGGER.info("{}", message.getString()));
			PLibDatabase.LOGGER.info("STAGE 4.3c ITEM PASS: phases={}, suites={}, pixels={}, nativeFrames={}, world={}, hand={}, foil={}, plain={}", phase, suites, pixels, nativeFrames, worldItems, hands, foil, plain);
			mc.stop();
		}
	}

	private static void mode()
	{
		var mc = Minecraft.getInstance();
		mc.options.graphicsPreset().set(GraphicsPreset.values()[phase % 3]);
		mc.levelRenderer.allChanged();
		frames = 0;
	}

	private static void early(RenderLevelStageEvent.AfterOpaqueFeatures event)
	{
		if (ticks < 20 || advance || phase >= 6 || phase == 3 && !reloaded || Minecraft.getInstance().getOverlay() != null)
			return;
		var state = event.getLevelRenderState();
		for (var block : state.blockEntityRenderStates)
			check(!(block instanceof PBlockRenderState<?>), "No Pulse block entity may mask standalone eligibility: " + block.getClass().getName() + " at " + block.blockPos);
		int drops = 0;
		int held = 0;
		for (var entity : state.entityRenderStates)
		{
			check(!(entity instanceof PEntityRenderState<?>), "No Pulse entity may mask standalone eligibility");
			if (entity instanceof PWorldRenderState pulse && pulse.pulselib$requiresSeparateTargets())
			{
				if (entity instanceof ItemEntityRenderState)
					drops++;
				if (entity instanceof ArmorStandRenderState)
					held++;
			}
		}
		if (drops < 2 || held == 0 || ++frames < 50)
			return;
		check(PWorldRenderPass.requiresSeparateTargets(state), "Vanilla item/armed owners retain layers");
		check(Minecraft.useShaderTransparency() == (phase % 3 == 2), "Native graphics choice is unchanged");
		var mc = Minecraft.getInstance();
		var player = mc.getEntityRenderDispatcher().extractEntity(mc.player, 0);
		check(((PWorldRenderState)player).pulselib$requiresSeparateTargets(), "Vanilla player held-item extraction retains layers");
		suite();
		PLibDatabase.LOGGER.info("STAGE 4.3c PHASE PASS: phase={}, mode={}, drops={}, armed={}, nativeFrames={}, pixels={}", phase, mc.options.graphicsPreset().get(), drops, held, nativeFrames, pixels);
		advance = true;
	}

	/** The production composite can run only after these native framegraph resources have survived. */
	public static void nativeTargets(RenderTarget destination, List<RenderTarget> targets)
	{
		check(targets.size() == 5, "Five native transparency inputs");
		for (var target : targets)
			check(target.width == destination.width && target.height == destination.height && !target.getColorTexture().isClosed() && !target.getDepthTexture().isClosed(), "Live native layers have the current dimensions");
		nativeFrames++;
	}

	@SuppressWarnings("unchecked")
	private static void suite()
	{
		var mc = Minecraft.getInstance();
		var color = RenderSystem.outputColorTextureOverride;
		var depth = RenderSystem.outputDepthTextureOverride;
		var projection = RenderSystem.getProjectionMatrixBuffer();
		var projectionType = RenderSystem.getProjectionType();
		var matrix = new Matrix4f(RenderSystem.getModelViewMatrix());
		var compiler = (PFrameCompiler<PRenderQueue.RenderStage>)field(PRenderQueue.class, null, "COMPILER");
		var pendingWorld = List.of(snapshot(field(PFrameCompiler.class, compiler, "opaque")), snapshot(field(PFrameCompiler.class, compiler, "translucent")));
		var a = new TextureTarget("stage43c caller", 48, 48, true);
		var b = new TextureTarget("stage43c nested", 32, 32, true);
		try (var caller = new GlRenderState(); var identity = new ProjectionMatrixBuffer("stage43c identity"))
		{
			RenderSystem.setProjectionMatrix(identity.getBuffer(new Matrix4f()), ProjectionType.ORTHOGRAPHIC);
			RenderSystem.getModelViewStack().identity();
			RenderSystem.disableScissorForRenderTypeDraws();
			var type = PRenderTypes.RenderTypeProvider.trianglesEmissiveSolid(PResourceCache.ATLAS_LOCATION);
			PRenderQueue.submit(SENTINEL, type, Stage42aWorldStageProbe.quad(), new PInstanceHeader(new Matrix4f(), -1, 0xF000F0, OverlayTexture.NO_OVERLAY));
			var collector = mc.gameRenderer.getFeatureRenderDispatcher().getSubmitNodeStorage();
			direct = true;
			for (var context : ItemDisplayContext.values())
			{
				clear(a, 0xFF000000, 1);
				clear(b, 0xFF000000, 1);
				bind(a);
				try (var invocation = PRenderContexts.enter(context.firstPerson() ? PRenderContexts.Context.FIRST_PERSON : PRenderContexts.Context.WORLD))
				{
					// Changing attachments invalidates the outer world/hand invocation. Direct item submit must own a local scope.
					bind(b);
					var item = new net.minecraft.client.renderer.item.ItemStackRenderState();
					var stack = new ItemStack(Stage1Content.ItemReg.TEST_ITEM.get());
					stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, context.ordinal() % 2 == 0);
					mc.getItemModelResolver().updateForTopItem(item, stack, context, mc.level, mc.player, 0);
					var poses = new PoseStack();
					item.submit(poses, collector, 0xF000F0, OverlayTexture.NO_OVERLAY, 0);
					check(poses.isEmpty(), "Native item submit restores pose stack");
					check(lastFoil == stack.hasFoil(), "Native item submit forwards foil for " + context);
					var actual = pixel(b);
					check(actual[0] > 0.95f && actual[1] < 0.02f && actual[2] < 0.02f, "Direct offscreen item pixels " + context + " " + java.util.Arrays.toString(actual));
					pixels++;
					check(pixel(a)[0] < 0.02f, "Parent target is untouched by nested item " + context);
					pixels++;
				}
			}
			bind(b);
			clear(b, 0xFF000000, 1);
			var renderer = new ItemRenderer();
			var argument = renderer.extractArgument(new ItemStack(Stage1Content.ItemReg.TEST_ITEM.get()));
			argument.extractItemRenderState(new net.minecraft.client.renderer.item.ItemStackRenderState());
			var poses = new PoseStack();
			fail = true;
			try
			{
				renderer.submit(argument, poses, collector, 0xF000F0, OverlayTexture.NO_OVERLAY, true, 0);
				throw new AssertionError("Expected direct item failure");
			}
			catch (IllegalArgumentException expected)
			{
				check(expected.getMessage().equals("stage43c item failure"), "Original direct exception");
			}
			fail = false;
			check(poses.isEmpty() && new Matrix4f().equals(poses.last().pose()), "Failed direct item pose restoration");
			check(pixel(b)[0] < 0.02f, "Failed direct item plan discarded");
			pixels++;
			renderer.submit(argument, poses, collector, 0xF000F0, OverlayTexture.NO_OVERLAY, false, 0);
			check(pixel(b)[0] > 0.95f, "Following direct item pixels recover");
			pixels++;
			var vanilla = mc.getEntityRenderDispatcher().extractEntity(new ItemEntity(mc.level, 0, 80, 5, new ItemStack(Items.DIAMOND)), 0);
			check(!((PWorldRenderState)vanilla).pulselib$requiresSeparateTargets(), "Ordinary vanilla extraction after GUI/offscreen items must not inherit Pulse eligibility");
			check(compiler.compile(SENTINEL).instances().size() == 1, "Local direct items preserve world sentinel");
			check(pendingWorld.equals(List.of(snapshot(field(PFrameCompiler.class, compiler, "opaque")), snapshot(field(PFrameCompiler.class, compiler, "translucent")))), "Local items preserve every pending world submission without leaking geometry");
			check(GL11.glGetError() == GL11.GL_NO_ERROR, "Direct matrix GL error");
			suites++;
		}
		finally
		{
			direct = fail = false;
			RenderSystem.outputColorTextureOverride = color;
			RenderSystem.outputDepthTextureOverride = depth;
			RenderSystem.setProjectionMatrix(projection, projectionType);
			RenderSystem.getModelViewStack().set(matrix);
			a.destroyBuffers();
			b.destroyBuffers();
		}
	}

	private static Object snapshot(Object value)
	{
		if (value instanceof Map<?, ?> map)
		{
			Map<Object, Object> copy = new HashMap<>();
			map.forEach((key, entry) -> copy.put(key, snapshot(entry)));
			return copy;
		}
		return value instanceof List<?> list ? List.copyOf(list) : value;
	}

	private static void bind(RenderTarget target)
	{
		RenderSystem.outputColorTextureOverride = target.getColorTextureView();
		RenderSystem.outputDepthTextureOverride = target.getDepthTextureView();
	}

	private static void shutdown(GameShuttingDownEvent event)
	{
		check(finished && suites == 6 && nativeFrames > 0, "All standalone item modes/reload must complete");
		PLibDatabase.LOGGER.info("STAGE 4.3c ITEM SHUTDOWN PASS");
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
			throw new AssertionError("Stage 4.3c: " + message);
	}

	public static final class ItemRenderer extends PItemRenderer<TestBlockItem, TestBlockItemRenderState>
	{
		public ItemRenderer()
		{
			super(Stage42b4TranslucentProbe.MODEL, PRenderTypes.RenderTypeProvider::trianglesEmissiveSolid);
		}

		@Override
		protected TestBlockItemRenderState createRenderState()
		{
			return new TestBlockItemRenderState();
		}

		@Override
		public void getExtents(Consumer<Vector3fc> output)
		{
			output.accept(new Vector3f(-0.5f));
			output.accept(new Vector3f(0.5f));
		}

		@Override
		public void preSubmit(PoseStack poses, TestBlockItemRenderState state, CameraRenderState camera, SubmitNodeCollector collector)
		{
			if (direct)
				poses.last().setIdentity();
			poses.translate(0, -0.5f, 0);
			poses.scale(0.5f, 0.5f, 0.5f);
		}

		@Override
		protected PMeshRenderContext resolveMeshRender(TestBlockItem owner, ItemStack stack, ItemDisplayContext context, PBakedBone bone, PBakedMesh mesh, PMeshRenderContext inherited, float partialTick)
		{
			return direct ? inherited.withColor(0xFFFF0000).withEmissive(true) :
					inherited.withColor(0x80FF0000).withEmissive(true).withAlphaMode(PAlphaMode.TRANSLUCENT);
		}

		@Override
		public void postSubmit(PoseStack poses, TestBlockItemRenderState state, CameraRenderState camera, SubmitNodeCollector collector)
		{
			lastFoil = state.hasFoil();
			if (state.hasFoil())
				foil++;
			else
				plain++;
			if (!direct)
			{
				if (PRenderContexts.current() == PRenderContexts.Context.WORLD)
					worldItems++;
				if (PRenderContexts.current() == PRenderContexts.Context.FIRST_PERSON)
					hands++;
			}
			if (fail)
				throw new IllegalArgumentException("stage43c item failure");
		}
	}

	public record Unbaked() implements SpecialModelRenderer.Unbaked<TestBlockItemRenderState>
	{
		public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

		@Override
		public ItemRenderer bake(SpecialModelRenderer.BakingContext context)
		{
			return new ItemRenderer();
		}

		@Override
		public MapCodec<Unbaked> type()
		{
			return MAP_CODEC;
		}
	}
}
