/**
 * @author ArcAnc
 * Created at: 12.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test;

import com.arcanc.pulselib.content.animatable.AnimManagerKey;
import com.arcanc.pulselib.content.animatable.ControllerState;
import com.arcanc.pulselib.content.animatable.PAnimatable;
import com.arcanc.pulselib.content.animatable.PAnimationManager;
import com.arcanc.pulselib.content.animatable.instance.InstanceAnimationManager;
import com.arcanc.pulselib.content.model.animation.PRawAnimation;
import com.arcanc.pulselib.content.model.baked.PBakedMesh;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.model.baked.PGpuDeformedMeshBuffers;
import com.arcanc.pulselib.content.model.deformer.PDeformerStack;
import com.arcanc.pulselib.content.model.deformer.PDeformerInstance;
import com.arcanc.pulselib.content.model.deformer.PBendDeformer;
import com.arcanc.pulselib.content.model.deformer.PBendDefinition;
import com.arcanc.pulselib.content.model.deformer.PChannelReference;
import com.arcanc.pulselib.content.model.deformer.PMeshDeformation;
import com.arcanc.pulselib.content.model.deformer.PDeformerValueSource;
import com.arcanc.pulselib.content.model.deformer.gpu.PGpuDeformerBuffers;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import com.arcanc.pulselib.content.player.animation.PPlayerPart;
import com.arcanc.pulselib.content.registration.block.block_entity.TestBlockEntity;
import com.arcanc.pulselib.content.registration.block.block_entity.ber.TestBlockEntityRenderer;
import com.arcanc.pulselib.content.registration.entity.TestEntity;
import com.arcanc.pulselib.content.registration.entity.renderer.TestEntityRender;
import com.arcanc.pulselib.content.registration.item.TestArmor;
import com.arcanc.pulselib.content.registration.item.renderer.TestBlockItemRenderer;
import com.arcanc.pulselib.content.registration.player.PPlayerBallDemo;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.legacy.GlRenderState;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.content.renderer.plan.PTransparencyMode;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PModelCache;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.PResourceCache;
import com.arcanc.pulselib.util.attachments.PLivingAttachments;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.arcanc.pulselib.test.RegressionChecks.check;

public final class SmokeHarness
{
	private static final PRenderQueue.RenderStage STAGE = new PRenderQueue.RenderStage("canonical_smoke");
	private static boolean completed;
	private static boolean worldVerified;
	private static int worldStart = -1;
	private static final BlockPos TEST_BLOCK = new BlockPos(11, 140, 14);
	private static @Nullable CompletableFuture<Void> worldSetup;
	private static boolean active;
	private static boolean windowHidden;
	private static int draws;
	private static long deadline;
	private static final List<Map<String, Object>> RESULTS = new ArrayList<>();
	private static int blockCalls;
	private static int sceneEntityId = -1;

	private SmokeHarness()
	{
	}

	public static boolean enabled()
	{
		return Boolean.getBoolean("pulselib.test.smokeRegression");
	}

	/** Same GL-preserving input isolation as the canonical controlled measurement window. */
	public static void prepareAutomationWindow()
	{
		if (!enabled())
			return;
		Minecraft mc = Minecraft.getInstance();
		mc.options.pauseOnLostFocus = false;
		if (!windowHidden)
		{
			org.lwjgl.glfw.GLFW.glfwHideWindow(mc.getWindow().handle());
			windowHidden = true;
		}
	}

	public static void begin()
	{
		active = true;
		deadline = System.nanoTime() + 90_000_000_000L;
	}

	public static boolean completed()
	{
		return completed;
	}

	public static Map<String, Object> result()
	{
		return Map.of("enabled", enabled(), "completed", completed, "draws", RESULTS,
				"stages", ClientTestPlugin.stageFrames, "layers", ClientTestPlugin.layerRenders,
				"camera", Minecraft.getInstance().options.getCameraType().name());
	}

	public static void frame()
	{
		if (!enabled() || !active || completed)
			return;
		check(System.nanoTime() < deadline, "Smoke world/graphics deadline");
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null)
			return;
		if (mc.getOverlay() != null || PModelCache.getModels() == null)
			return;
		PBakedModel gltf = ClientTestPlugin.GLTF.getModel();
		PBakedModel gecko = ClientTestPlugin.GECKO.getModel();
		check(gltf != null && gecko != null, "Both model parsers must load");
		check(!meshes(gltf).isEmpty(), "Original glTF item must bake geometry");
		check(gecko.boneCount() > 0 && !gecko.animations().
				isEmpty(), "Original Gecko player rig must load bones and animations");
		check(!meshes(TestEntityRender.MODEL_DATA.getModel()).isEmpty(), "Original Gecko entity must bake geometry");
		check(ClientTestPlugin.constructions == 1 && CommonTestPlugin.constructions == 1, "Plugins must be constructed once");
		check(ClientTestPlugin.attachments == 1 && ClientTestPlugin.playerAnimations == 1 && ClientTestPlugin.animatedRegistrations == 1
				&& CommonTestPlugin.registrations == 1 && CommonTestPlugin.events == 1 && CommonTestPlugin.deformers == 1,
			"One-time phases must not repeat");
		check(PLivingAttachments.contains(DemoContent.helmet.get()), "Attachment was not registered");
		TextureAtlasSprite sprite = PResourceCache.getTextureAtlas().
				getSprite(PResourceCache.resolve(ClientTestPlugin.GLTF.getModelId(), "circle"));
		check(!sprite.contents().name().getPath().equals("missingno"), "Test texture must be stitched");
		if (!worldVerified)
		{
			if (worldSetup == null)
			{
				DemoChecks.world(mc.getSingleplayerServer(), true);
				var server = mc.getSingleplayerServer();
				var uuid = mc.player.getUUID();
				worldSetup = CompletableFuture.runAsync(() ->
				{
					var player = server.getPlayerList().getPlayer(uuid);
					check(player != null, "Server fixture player must exist");
					var level = player.level();
					for (int x = 5; x <= 16; x++)
						for (int z = 5; z <= 18; z++)
						{
							level.setBlockAndUpdate(new BlockPos(x, 139, z), Blocks.STONE.defaultBlockState());
							for (int y = 140; y <= 147; y++)
								level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
						}
					player.setGameMode(GameType.CREATIVE);
					player.teleportTo(level, 10.5, 140.0, 10.5, Set.of(), 0.0f, 0.0f, true);
					equip(player);
					level.setBlockAndUpdate(TEST_BLOCK, DemoContent.block.get().defaultBlockState());
					check(level.getBlockEntity(TEST_BLOCK) instanceof TestBlockEntity, "Server fixture BlockEntity must exist");
					((TestBlockEntity)level.getBlockEntity(TEST_BLOCK)).changePlayAnimation();
					var entity = DemoContent.entity.get().create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
					check(entity != null, "Server fixture entity must construct");
					entity.setPos(9.0, 140.0, 14.5);
					entity.showArmor = true;
					level.addFreshEntity(entity);
					sceneEntityId = entity.getId();
					PLibDatabase.LOGGER.info("PULSE SMOKE SPAWN: id={}, entity={}", sceneEntityId, entity);
				}, server);
				return;
			}
			if (!worldSetup.isDone())
				return;
			worldSetup.join();
			if (!(mc.level.getBlockEntity(TEST_BLOCK) instanceof TestBlockEntity block))
				return;
			check(block.getType() == DemoContent.blockEntity.get() && !block.isRemoved() && block.getLevel() == mc.level &&
					block.getBlockState().is(DemoContent.block.get()), "Client fixture BlockEntity type/state must match");
			check((Object)mc.getBlockEntityRenderDispatcher().getRenderer(block) instanceof TestBlockEntityRenderer,
					"Loader must register the original BlockEntityRenderer");
			if (worldStart < 0)
			{
				blockCalls = 0;
				equip(mc.player);
				mc.options.setCameraType(Boolean.getBoolean("pulselib.test.smokeFirstPerson") ? CameraType.FIRST_PERSON : CameraType.THIRD_PERSON_BACK);
				PPlayerAnimations.getHandle(mc.player, PPlayerBallDemo.ID).stopAll();
				PPlayerBallDemo.play(mc.player);
				worldStart = mc.player.tickCount;
			}

			if (mc.player.tickCount - worldStart < 30)
				return;
			for (var entity : mc.level.entitiesForRendering())
				if (entity instanceof TestEntity testEntity)
					testEntity.showArmor = true;
			PLibDatabase.LOGGER.info("PULSE SMOKE CAMERA PROBE: playerYaw={}, nativeYaw={}, type={}, blockCalls={}, armorSetups={}",
					mc.player.getYRot(), mc.gameRenderer.getMainCamera().yRot(), mc.options.getCameraType(), blockCalls, TestArmor.setups);
			float expectedYaw = Boolean.getBoolean("pulselib.test.smokeFirstPerson") ? 180.0f : 0.0f;
			check(Math.abs(net.minecraft.util.Mth.wrapDegrees(mc.gameRenderer.getMainCamera().yRot() - expectedYaw)) < 0.001f,
					"Native Smoke camera must reflect the fixed profile");
			check(ClientTestPlugin.stageFrames > 0, "Actual block/particle/weather stage cycle must complete");
			check(blockCalls > 0, "Original block entity renderer must run");
			check(TestEntityRender.renders > 0, "Original entity renderer must run");
			check(TestBlockItemRenderer.renders > 0, "Loader item renderer adapter must be invoked");
			check(TestArmor.setups > 0, "Loader custom armor adapter must be invoked");
			check(ClientTestPlugin.layerRenders > 0, "Attachment layer must render");
			check(ClientTestPlugin.animatedRenders > 0, "Animated attachment layer must render");
			check(PPlayerAnimations.isPartAnimating(mc.player,
				PPlayerPart.RIGHT_ARM, 0.5f), "Existing ball-toss demo must animate the player arm");
			check(PLivingAttachments.hidesVanillaArmor(mc.player.getItemBySlot(EquipmentSlot.HEAD), EquipmentSlot.HEAD, mc.player), "Attachment must hide vanilla helmet");
			PLibDatabase.LOGGER.info("PULSE TEST WORLD PASS: item {}, armor {}, attachment {}, animated attachment {}",
				TestBlockItemRenderer.renders, TestArmor.setups, ClientTestPlugin.layerRenders, ClientTestPlugin.animatedRenders);
		}

		worldVerified = true;
		Dummy dummy = new Dummy();
		dummy.manager.bindModel(gecko);
		var controller = dummy.manager.getControllers().get("idle");
		controller.play(PRawAnimation.begin().thenLoop("animation.pulselib.player.flip").build());
		dummy.manager.tick();
		check(controller.getCurrentStage() != null, "Animation controller must start the Gecko clip");
		gecko.evaluate(dummy.manager.getControllers().values(), Map.of(), 0.5f);
		InstanceAnimationManager.removeManager(dummy);
		PBakedMesh pyramid = meshes(gltf).stream().filter(mesh -> mesh.textureReference().equals("pyramid")).findFirst().
				orElseThrow();
		draw(pyramid, false);
		draw(pyramid, true);
		completed = true;
		PLibDatabase.LOGGER.info("PULSE TEST SMOKE COMPLETE: camera={}, stages={}, layers={}, draws={}",
				mc.options.getCameraType(), ClientTestPlugin.stageFrames, ClientTestPlugin.layerRenders, draws);
	}

	/** Release the transient Smoke actor before the independent retention suite selects its owners. */
	static void releaseScene(net.minecraft.server.level.ServerLevel level)
	{
		var entity = level.getEntity(sceneEntityId);
		PLibDatabase.LOGGER.info("PULSE SMOKE RELEASE: id={}, entity={}", sceneEntityId, entity);
		if (entity != null)
			entity.discard();
		level.removeBlock(TEST_BLOCK, false);
	}

	static boolean sceneReleased(Minecraft mc)
	{
		return sceneEntityId >= 0 && mc.level.getEntity(sceneEntityId) == null;
	}

	public static void recordBlockRender(TestBlockEntity block)
	{
		if (block.getBlockPos().equals(TEST_BLOCK) && block.getLevel() == Minecraft.getInstance().level)
			blockCalls++;
	}

	public static void lockWorldCamera()
	{
		Minecraft mc = Minecraft.getInstance();
		if (!enabled() || !active || completed || worldStart < 0 || mc.player == null)
			return;
		KeyMapping.releaseAll();
		float yaw = Boolean.getBoolean("pulselib.test.smokeFirstPerson") ? 180.0f : 0.0f;
		mc.player.setYRot(yaw);
		mc.player.yRotO = yaw;
		mc.player.yBodyRot = yaw;
		mc.player.yBodyRotO = yaw;
		mc.player.setXRot(0.0f);
		mc.player.xRotO = 0.0f;
	}

	private static void equip(LivingEntity player)
	{
		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(DemoContent.helmet.get()));
		player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(DemoContent.chestplate.get()));
		player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(DemoContent.blockItem.get()));
		player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(DemoContent.tail.get()));
	}

	private static List<PBakedMesh> meshes(PBakedModel model)
	{
		return java.util.stream.IntStream.range(0, model.boneCount()).mapToObj(model::bone).
				flatMap(bone -> bone.meshes().stream()).toList();
	}

	private static void draw(PBakedMesh mesh, boolean translucent)
	{
		var target = new TextureTarget("PulseLib canonical Smoke pixels", 64, 64, true);
		var projection = RenderSystem.getProjectionMatrixBuffer();
		var projectionType = RenderSystem.getProjectionType();
		var color = RenderSystem.outputColorTextureOverride;
		var depth = RenderSystem.outputDepthTextureOverride;
		int read = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
		int row = GL11.glGetInteger(GL11.GL_PACK_ROW_LENGTH);
		int alignment = GL11.glGetInteger(GL11.GL_PACK_ALIGNMENT);
		int skipRows = GL11.glGetInteger(GL11.GL_PACK_SKIP_ROWS);
		int skipPixels = GL11.glGetInteger(GL11.GL_PACK_SKIP_PIXELS);
		int packBuffer = GL11.glGetInteger(GL21.GL_PIXEL_PACK_BUFFER_BINDING);
		int fbo = GL30.glGenFramebuffers();
		var modelView = RenderSystem.getModelViewStack();
		modelView.pushMatrix();
		try (var state = new GlRenderState(); var ortho = new ProjectionMatrixBuffer("PulseLib Smoke projection"))
		{
			GlErrors.check();
			RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(target.getColorTexture(), 0, target.getDepthTexture(), 1);
			RenderSystem.outputColorTextureOverride = target.getColorTextureView();
			RenderSystem.outputDepthTextureOverride = target.getDepthTextureView();
			modelView.identity();
			RenderSystem.setProjectionMatrix(ortho.getBuffer(new Matrix4f().setOrtho(-1f, 1f, -1f, 1f, -10f, 10f)), ProjectionType.ORTHOGRAPHIC);
			var type = translucent ? PRenderTypes.RenderTypeProvider.trianglesEmissiveTranslucent(PResourceCache.ATLAS_LOCATION) :
				PRenderTypes.RenderTypeProvider.trianglesEmissiveSolid(PResourceCache.ATLAS_LOCATION);
			PDeformerStack stack = PDeformerStack.compile(List.of(new PDeformerInstance<>(PBendDeformer.INSTANCE,
				new PBendDefinition(new Vector3f(), new Vector3f(0, 1, 0), new Vector3f(1, 0, 0), 1f, 1f,
					new PChannelReference<>("test_angle", (float)Math.toRadians(15))))));
			var submission = PGpuDeformerBuffers.submit(new PMeshDeformation(stack, PDeformerValueSource.DEFAULTS, mesh, 1));
			check(submission.operationCount() > 0, "GPU deformer stream must receive operations");
			var geometry = PGpuDeformedMeshBuffers.resolve(mesh, 1);
			for (int i = 0; i < (Boolean.getBoolean("pulselib.test.smokeSkipDraw") ? 0 : 8); i++)
				PRenderQueue.submit(STAGE, type, geometry,
					new PInstanceHeader(meshTransform(mesh).translate(0, 0, i * 0.001f), translucent ? 0x80FFFFFF : -1, 0xF000F0, OverlayTexture.NO_OVERLAY,
						submission.operationOffset(), submission.valueOffset(), submission.operationCount()),
						translucent ? PTransparencyMode.OIT : PTransparencyMode.ORDINARY);
			PRenderQueue.flush(STAGE, target);
			PGpuDeformerBuffers.finishFrame();
			int[] before = glState();
			PRenderQueue.compositeTranslucency(target);
			check(Arrays.equals(before, glState()), "OIT composite must restore indexed blend/depth/scissor state");
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, fbo);
			GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, ((GlTexture)target.getColorTexture()).glId(), 0);
			GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0);
			GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, 0);
			GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
			GL11.glPixelStorei(GL11.GL_PACK_SKIP_ROWS, 0);
			GL11.glPixelStorei(GL11.GL_PACK_SKIP_PIXELS, 0);
			var pixels = BufferUtils.createByteBuffer(64 * 64 * 4);
			GL11.glReadPixels(0, 0, 64, 64, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
			int error = GL11.glGetError();
			check(error == GL11.GL_NO_ERROR, "OpenGL error " + error + " during " + (translucent ? "OIT" : "instancing"));
			int colored = 0;
			int partialAlpha = 0;
			for (int offset = 0; offset < pixels.capacity(); offset += 4)
			{
				int alpha = pixels.get(offset + 3) & 255;
				if (alpha > 0 && ((pixels.get(offset) & 255) + (pixels.get(offset + 1) & 255) + (pixels.get(offset + 2) & 255)) > 10)
					colored++;
				if (alpha > 0 && alpha < 255)
					partialAlpha++;
			}
			PLibDatabase.LOGGER.info("PULSE TEST {}: {} colored pixels, {} partial-alpha pixels from original item mesh",
				translucent ? "OIT" : "solid", colored, partialAlpha);
			check(colored > 0, "Original demo geometry must produce visible pixels");
			if (translucent)
				check(partialAlpha > 0, "OIT must preserve partial alpha");
			draws++;
			RESULTS.add(Map.of("translucent", translucent, "colored", colored, "partialAlpha", partialAlpha));
		}
		finally
		{
			GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, packBuffer);
			GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, row);
			GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, alignment);
			GL11.glPixelStorei(GL11.GL_PACK_SKIP_ROWS, skipRows);
			GL11.glPixelStorei(GL11.GL_PACK_SKIP_PIXELS, skipPixels);
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, read);
			GL30.glDeleteFramebuffers(fbo);
			RenderSystem.outputColorTextureOverride = color;
			RenderSystem.outputDepthTextureOverride = depth;
			RenderSystem.setProjectionMatrix(projection, projectionType);
			modelView.popMatrix();
			target.destroyBuffers();
		}
	}

	private static int[] glState()
	{
		int count = GL11.glGetInteger(GL20.GL_MAX_DRAW_BUFFERS);
		int[] state = new int[count * 7 + 4];
		int[] parameters = {GL14.GL_BLEND_SRC_RGB, GL14.GL_BLEND_DST_RGB,
			GL14.GL_BLEND_SRC_ALPHA, GL14.GL_BLEND_DST_ALPHA,
			GL20.GL_BLEND_EQUATION_RGB, GL20.GL_BLEND_EQUATION_ALPHA};
		for (int i = 0; i < count; i++)
		{
			state[i * 7] = GL30.glIsEnabledi(GL11.GL_BLEND, i) ? 1 : 0;
			for (int j = 0; j < 6; j++)
				state[i * 7 + j + 1] = GL30.glGetIntegeri(parameters[j], i);
		}
		state[count * 7] = GL11.glIsEnabled(GL11.GL_DEPTH_TEST) ? 1 : 0;
		state[count * 7 + 1] = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK) ? 1 : 0;
		state[count * 7 + 2] = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
		state[count * 7 + 3] = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST) ? 1 : 0;
		return state;
	}

	private static Matrix4f meshTransform(PBakedMesh mesh)
	{
		var positions = mesh.source().positions();
		Vector3f min = new Vector3f(Float.POSITIVE_INFINITY);
		Vector3f max = new Vector3f(Float.NEGATIVE_INFINITY);
		for (int i = 0; i < mesh.source().vertexCount(); i++)
		{
			Vector3f position = new Vector3f(positions.get(i * 3), positions.get(i * 3 + 1), positions.get(i * 3 + 2));
			min.min(position);
			max.max(position);
		}
		Vector3f size = new Vector3f(max).sub(min);
		Vector3f center = new Vector3f(min).add(max).mul(0.5f);
		float scale = 1.2f / Math.max(size.x, Math.max(size.y, size.z));
		return new Matrix4f().rotateY(0.4f).scale(scale).translate(-center.x, -center.y, -center.z);
	}

	private static final class Dummy implements PAnimatable<Dummy>
	{
		private final PAnimationManager<Dummy> manager = new InstanceAnimationManager<>(this);

		@Override
		public PAnimationManager<Dummy> getAnimationManager(AnimManagerKey key)
		{
			return this.manager;
		}

		@Override
		public void registerAnimationControllers(PAnimationManager.PAnimationRegistrar<Dummy> registrar)
		{
			registrar.add("idle", () -> state -> ControllerState.PLAY);
		}
	}
}
