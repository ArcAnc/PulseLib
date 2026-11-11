/**
 * @author ArcAnc
 * Created at: 11.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test;

import com.arcanc.pulselib.api.registration.PAttachmentRegistrar;
import com.arcanc.pulselib.api.registration.PModelRegistrar;
import com.arcanc.pulselib.content.animatable.AnimManagerKey;
import com.arcanc.pulselib.content.animatable.PAnimatable;
import com.arcanc.pulselib.content.animatable.PAnimationController;
import com.arcanc.pulselib.content.animatable.PAnimationManager;
import com.arcanc.pulselib.content.model.animation.PRawAnimation;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.model.textures.PAlphaMode;
import com.arcanc.pulselib.content.renderer.PRenderContexts;
import com.arcanc.pulselib.content.renderer.base.PLivingAttachmentRenderState;
import com.arcanc.pulselib.content.renderer.base.PModelPartSnapshot;
import com.arcanc.pulselib.content.renderer.legacy.GlRenderState;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.data.gecko.PGeckoModelLoader;
import com.arcanc.pulselib.data.gltf.PGltfModelLoader;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.attachments.PLivingAttachmentDefinition;
import com.arcanc.pulselib.util.attachments.PLivingAttachmentLayer;
import com.arcanc.pulselib.util.attachments.PTransform;
import com.arcanc.pulselib.util.attachments.humanoid.PHumanoidAnchors;
import com.arcanc.pulselib.util.attachments.humanoid.PHumanoidBindings;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/** Own attachment clips reach real legacy extraction/layer/queue/GPU draws on both native model formats. */
public final class LegacyClipHarness
{
	private static final List<PModelData> MODELS = List.of(
			PModelData.direct(DemoContent.id("regression/legacy_clips"), PGeckoModelLoader.INSTANCE.id()),
			PModelData.direct(DemoContent.id("regression/legacy_clips_gltf"), PGltfModelLoader.INSTANCE.id()));
	private static final List<Map<String, Object>> RESULTS = new ArrayList<>();
	private static List<PBakedModel> previousModels = List.of();
	private static @Nullable PAnimationController<ClipOwner> controller;
	private static @Nullable CompletableFuture<Void> reload;
	private static int format = -1;
	private static int source = -1;
	private static int providers;
	private static int resolvers;
	private static int epoch;
	private static int phase;
	private static long deadline;
	private static boolean complete;

	private LegacyClipHarness()
	{
	}

	public static boolean enabled()
	{
		return Boolean.getBoolean("pulselib.test.legacyClips");
	}

	public static void registerModels(PModelRegistrar registrar)
	{
		if (enabled())
			for (var model : MODELS)
				registrar.model(model).texture("0", H14OffscreenHarness.WHITE);
	}

	public static void registerAttachments(PAttachmentRegistrar registrar)
	{
		if (!enabled())
			return;
		for (int model = 0; model < 2; model++)
			for (int origin = 0; origin < 2; origin++)
			{
				int selected = model, slot = origin;
				var definition = new PLivingAttachmentDefinition(MODELS.get(model),
						(entity, equipment, stack) -> format == selected && source == slot && entity == Minecraft.getInstance().player &&
								(slot == 0 || equipment == EquipmentSlot.CHEST),
						List.of(PHumanoidBindings.bind(PHumanoidAnchors.BODY, "root", new PTransform(new Vector3f(), new Quaternionf(), new Vector3f(1)))),
						(entity, stack, bone, mesh, context, partialTick) ->
						{
							resolvers++;
							return context.withColor(0xFF00FF00).withEmissive(true).withAlphaMode(PAlphaMode.OPAQUE);
						}, false,
						(entity, stack, baked, partialTick) ->
						{
							DemoChecks.check(entity == Minecraft.getInstance().player && (slot == 0 ? stack.isEmpty() : stack.is(Items.DIAMOND_CHESTPLATE)), "Legacy clip provider receives its actual owner/source stack");
							providers++;
							return Boolean.getBoolean("pulselib.test.legacySkipControllers") ? List.of() : List.of(Objects.requireNonNull(controller));
						});
				if (origin == 0)
					registrar.registerGlobalLiving(definition);
				else
					registrar.registerLiving(Items.DIAMOND_CHESTPLATE, definition);
			}
	}

	public static void tick()
	{
		Minecraft mc = Minecraft.getInstance();
		if (complete)
			return;
		if (deadline == 0)
			deadline = System.nanoTime() + 120_000_000_000L;
		DemoChecks.check(System.nanoTime() < deadline, "Legacy clip completion deadline");
		if (mc.getOverlay() != null || MODELS.stream().anyMatch(model -> model.getModel() == null))
			return;
		if (phase == 0)
		{
			DemoChecks.plugins(true);
			GLFW.glfwHideWindow(mc.getWindow().handle());
			DisposableDemoWorlds.create();
			phase = 1;
		}
		if (phase == 1 && mc.player != null && mc.screen == null && mc.player.tickCount >= 30)
			phase = 2;
	}

	public static void frame()
	{
		Minecraft mc = Minecraft.getInstance();
		if (complete || phase != 2 || mc.getOverlay() != null)
			return;
		DemoChecks.world(mc.getSingleplayerServer(), true);
		if (reload != null)
		{
			if (!reload.isDone())
				return;
			reload.join();
			reload = null;
			for (int index = 0; index < 2; index++)
				DemoChecks.check(previousModels.get(index) != MODELS.get(index).getModel(), "Both attachment formats are genuinely reloaded");
		}
		try
		{
			suite();
			if (epoch++ == 0)
			{
				previousModels = MODELS.stream().map(model -> Objects.requireNonNull(model.getModel())).toList();
				reload = mc.reloadResourcePacks();
				return;
			}
			DemoChecks.check(RESULTS.size() == 48 && ClientTestPlugin.models == 2 && providers == 48, "Both legacy clip epochs and source providers complete");
			complete = true;
			DemoChecks.writeResult(Map.of("status", "PASS", "fixture", "legacy-clips", "cases", RESULTS.size(), "epochs", 2,
					"providers", providers, "resolvers", resolvers, "records", RESULTS, "origin", DemoChecks.jar(LegacyClipHarness.class)));
			PLibDatabase.LOGGER.info("LEGACY CLIPS PASS: 48 cases, own Gecko/glTF clips, global/item sources, real layer pixels, reverse seeks and post-submit mutation, two epochs");
			mc.stop();
		}
		catch (IOException exception)
		{
			throw new IllegalStateException("Cannot save legacy clip witnesses", exception);
		}
	}

	private static void suite() throws IOException
	{
		Minecraft mc = Minecraft.getInstance();
		var projection = RenderSystem.getProjectionMatrixBuffer();
		var sorting = RenderSystem.getProjectionType();
		ItemStack oldStack = mc.player.getItemBySlot(EquipmentSlot.CHEST);
		RenderSystem.getModelViewStack().pushMatrix();
		var renderer = mc.getEntityRenderDispatcher().getPlayerRenderer(mc.player);
		var model = renderer.getModel();
		TextureTarget target = new TextureTarget("Legacy attachment clips", 128, 128, true);
		try (var caller = new GlRenderState(); var clear = new H14GpuTargets.ClearState(); var buffer = new ProjectionMatrixBuffer("Legacy clip projection");
				var snapshot = PModelPartSnapshot.capture(model.root()))
		{
			RenderSystem.getModelViewStack().identity();
			RenderSystem.setProjectionMatrix(buffer.getBuffer(new Matrix4f()), ProjectionType.ORTHOGRAPHIC);
			RenderSystem.disableScissorForRenderTypeDraws();
			for (format = 0; format < 2; format++)
				for (source = 0; source < 2; source++)
					for (String clip : List.of("move", "shape"))
					{
						controller = new PAnimationController<>("legacy-attachment", state -> state.controller().getState());
						controller.play(PRawAnimation.begin().thenLoop(clip).build());
						for (int time : new int[]{20, 0, 10})
						{
							PBakedModel baked = Objects.requireNonNull(MODELS.get(format).getModel());
							DemoChecks.check(baked.animations().containsKey(clip), "Attachment's own decoded clip exists");
							controller.seek(baked, time);
							controller.pause();
							mc.player.setItemSlot(EquipmentSlot.CHEST, source == 0 ? ItemStack.EMPTY : new ItemStack(Items.DIAMOND_CHESTPLATE));
							int before = providers;
							var state = (AvatarRenderState)mc.getEntityRenderDispatcher().extractEntity(mc.player, 0.5F);
							var entries = ((PLivingAttachmentRenderState)state).pulselib$getAttachments();
							DemoChecks.check(entries != null && entries.size() == 1 && providers == before + 1, "Actual native extraction selects exactly one legacy clip definition");
							model.body.x = model.body.y = model.body.z = 0;
							model.body.xRot = model.body.yRot = model.body.zRot = 0;
							model.body.xScale = model.body.yScale = model.body.zScale = 1;
							H14OffscreenHarness.clear(target, 0, 0, 0, 0, 1);
							RenderRegressionTargets.bind(target);
							int callbacks = resolvers;
							try (var local = PRenderContexts.enter(PRenderContexts.Context.OFFSCREEN))
							{
								new PLivingAttachmentLayer<>(renderer).submit(new PoseStack(), mc.gameRenderer.getFeatureRenderDispatcher().getSubmitNodeStorage(), 0xF000F0, state, 0, 0);
								DemoChecks.check(resolvers > callbacks, "Native legacy layer resolves decoded attachment meshes");
								// A later mutation must not rewrite the matrices already submitted to the local compiler.
								controller.seek(baked, time == 0 ? 20 : 0);
							}
							verifyPixels(target, clip, time);
							if (time == 20)
								H14OffscreenHarness.save(target, "legacy-" + epoch + "-" + format + "-" + source + "-" + clip);
						}
					}
			GlErrors.check();
		}
		finally
		{
			format = source = -1;
			controller = null;
			mc.player.setItemSlot(EquipmentSlot.CHEST, oldStack);
			target.destroyBuffers();
			RenderSystem.getModelViewStack().popMatrix();
			RenderSystem.setProjectionMatrix(projection, sorting);
		}
	}

	private static void verifyPixels(TextureTarget target, String clip, int time)
	{
		var colors = BufferUtils.createFloatBuffer(128 * 128 * 4);
		var depths = BufferUtils.createFloatBuffer(128 * 128);
		try (var binding = new H14GpuTargets.Binding(target, false))
		{
			GL11.glReadPixels(0, 0, 128, 128, GL11.GL_RGBA, GL11.GL_FLOAT, colors);
			GL11.glReadPixels(0, 0, 128, 128, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, depths);
		}
		double t = time / 20.0;
		double movement = clip.equals("move") ? t - 0.5 : 0;
		double angle = clip.equals("shape") ? t * Math.PI / 2 : 0;
		double scale = clip.equals("shape") ? 1 + t : 1;
		int inside = 0, outside = 0;
		for (int y = 0; y < 128; y++)
			for (int x = 0; x < 128; x++)
			{
				// Independent inverse of the authored rectangle's translation/rotation/scale and native BODY Z flip.
				double px = -(2 * (x + 0.5) / 128 - 1) - movement, py = -(2 * (y + 0.5) / 128 - 1);
				double localX = (Math.cos(angle) * px + Math.sin(angle) * py) / scale;
				double localY = -Math.sin(angle) * px + Math.cos(angle) * py;
				boolean expected = Math.abs(localX) < 0.115 && Math.abs(localY) < 0.0525;
				boolean empty = Math.abs(localX) > 0.145 || Math.abs(localY) > 0.0825;
				int offset = (y * 128 + x) * 4;
				float red = colors.get(offset), green = colors.get(offset + 1), blue = colors.get(offset + 2), depth = depths.get(y * 128 + x);
				if (expected)
				{
					if (!(green > 0.9F && red < 0.02F && blue < 0.02F && depth > 0.45F && depth < 0.55F))
						throw new AssertionError("Legacy animated pixels: epoch=" + epoch + " format=" + format + " source=" + source + " clip=" + clip + " time=" + time + " pixel=" + x + "," + y + " rgba=" + red + "," + green + "," + blue + " depth=" + depth);
					inside++;
				}
				else if (empty)
				{
					DemoChecks.check(red < 0.02F && green < 0.02F && blue < 0.02F && depth > 0.999F, "No unrequested/static/late-mutated mesh pixels outside the authored footprint");
					outside++;
				}
			}
		DemoChecks.check(inside >= 32 && outside > 15000, "Independent inner and outer pixel witnesses exist");
		RESULTS.add(Map.of("epoch", epoch, "format", format == 0 ? "gecko" : "gltf", "source", source == 0 ? "global" : "item", "clip", clip, "ticks", time, "inside", inside, "outside", outside));
	}

	public static void stopped()
	{
		DemoChecks.check(complete && RESULTS.size() == 48, "All legacy clip pixels complete before normal shutdown");
		PLibDatabase.LOGGER.info("LEGACY CLIPS SHUTDOWN PASS");
	}

	private static final class ClipOwner implements PAnimatable<ClipOwner>
	{
		@Override
		public PAnimationManager<ClipOwner> getAnimationManager(AnimManagerKey key)
		{
			throw new UnsupportedOperationException("This fixture uses the canonical standalone attachment controller provider");
		}

		@Override
		public void registerAnimationControllers(PAnimationManager.PAnimationRegistrar<ClipOwner> registrar)
		{
		}
	}
}
