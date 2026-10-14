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
import com.arcanc.pulselib.content.animatable.singleton.SingletonAnimationManager;
import com.arcanc.pulselib.content.event.PulseRuntime;
import com.arcanc.pulselib.content.model.animation.PRawAnimation;
import com.arcanc.pulselib.content.model.baked.PBakedBone;
import com.arcanc.pulselib.content.model.baked.PBakedMesh;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.model.baked.PGpuDeformedMeshBuffers;
import com.arcanc.pulselib.content.model.deformer.*;
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
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PModelCache;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.PResourceCache;
import com.arcanc.pulselib.util.attachments.PLivingAttachments;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import java.util.*;
import java.util.stream.Stream;
import java.util.concurrent.CompletableFuture;

public final class SmokeHarness
{
	private static boolean completed;
	private static int worldStart = -1;
	private static final BlockPos TEST_BLOCK = new BlockPos(11, 140, 14);
	private static @Nullable CompletableFuture<Void> worldSetup;
	private static @Nullable SmokeResources naturalResources;
	private static long disconnectDeadline;
	private static int naturalHooks;
	private static int blockCalls;
	private static boolean explicitCleanup;

	private SmokeHarness()
	{
	}

	public static void frame()
	{
		Minecraft mc = Minecraft.getInstance();
		if (naturalResources != null)
		{
			if (naturalHooks == 0 && System.nanoTime() < disconnectDeadline)
			{
				return;
			}
			check(naturalHooks > 0, "Natural disconnect did not invoke a loader lifecycle hook");
			naturalResources.assertDeleted("natural disconnect");
			naturalResources = null;
			completed = true;
			PLibDatabase.LOGGER.info("PULSE TEST NATURAL DISCONNECT PASS: {} loader hook calls", naturalHooks);
			PLibDatabase.LOGGER.info("PULSE TEST CLIENT PASS: graphics, animation, explicit cleanup and natural lifecycle");
			mc.stop();
			return;
		}
		if (completed)
		{
			return;
		}
		if (Boolean.getBoolean("pulselib.test.world") && (mc.player == null || mc.level == null))
			return;
		if (mc.getOverlay() != null || PModelCache.getModels() == null)
			return;
		PBakedModel gltf = ClientTestPlugin.GLTF.getModel();
		PBakedModel gecko = ClientTestPlugin.GECKO.getModel();
		check(gltf != null && gecko != null, "Both model parsers must load");
		check(!meshes(gltf).isEmpty(), "Original glTF item must bake geometry");
		check(!gecko.bones().isEmpty() && !gecko.animations().
				isEmpty(), "Original Gecko player rig must load bones and animations");
		if (Boolean.getBoolean("pulselib.test.geckoEntity"))
			check(!meshes(TestEntityRender.MODEL_DATA.getModel()).isEmpty(), "Original Gecko entity must bake geometry");
		check(ClientTestPlugin.constructions == 1 && CommonTestPlugin.constructions == 1, "Plugins must be constructed once");
		check(ClientTestPlugin.attachments == 1 && ClientTestPlugin.playerAnimations == 1 && ClientTestPlugin.animatedRegistrations == 1
				&& CommonTestPlugin.registrations == 1 && CommonTestPlugin.events == 1 && CommonTestPlugin.deformers == 1,
			"One-time phases must not repeat");
		check(PLivingAttachments.contains(DemoContent.helmet.get()), "Attachment was not registered");
		TextureAtlasSprite sprite = PResourceCache.getTextureAtlas().
				getSprite(PResourceCache.resolve(ClientTestPlugin.GLTF.getModelId(), "circle"));
		check(!sprite.contents().name().getPath().equals("missingno"), "Test texture must be stitched");
		if (Boolean.getBoolean("pulselib.test.world"))
		{
			if (worldSetup == null)
			{
				DisposableTestWorlds.requireCurrentWorld();
				var server = mc.getSingleplayerServer();
				var uuid = mc.player.getUUID();
				worldSetup = CompletableFuture.runAsync(() ->
				{
					var player = server.getPlayerList().getPlayer(uuid);
					check(player != null, "Server fixture player must exist");
					var level = player.serverLevel();
					for (int x = 5; x <= 16; x++)
					{
						for (int z = 5; z <= 18; z++)
						{
							level.setBlockAndUpdate(new BlockPos(x, 139, z), Blocks.STONE.defaultBlockState());
							for (int y = 140; y <= 147; y++)
							{
								level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
							}
						}
					}
					player.setGameMode(GameType.CREATIVE);
					player.teleportTo(level, 10.5, 140.0, 10.5, 0.0f, 0.0f);
					equip(player);
					level.setBlockAndUpdate(TEST_BLOCK, DemoContent.block.get().defaultBlockState());
					check(level.getBlockEntity(TEST_BLOCK) instanceof TestBlockEntity, "Server fixture BlockEntity must exist");
					((TestBlockEntity)level.getBlockEntity(TEST_BLOCK)).changePlayAnimation();
					var entity = DemoContent.entity.get().create(level);
					check(entity != null, "Server fixture entity must construct");
					entity.moveTo(9.0, 140.0, 14.5);
					entity.showArmor = true;
					level.addFreshEntity(entity);
				}, server);
				return;
			}
			if (!worldSetup.isDone())
			{
				return;
			}
			worldSetup.join();
			if (!(mc.level.getBlockEntity(TEST_BLOCK) instanceof TestBlockEntity block))
			{
				return;
			}
			check(block.getType() == DemoContent.blockEntity.get() && !block.isRemoved() && block.getLevel() == mc.level &&
					block.getBlockState().is(DemoContent.block.get()), "Client fixture BlockEntity type/state must match");
			check(mc.getBlockEntityRenderDispatcher().getRenderer(block) instanceof TestBlockEntityRenderer,
					"Loader must register the original BlockEntityRenderer");
			if (worldStart < 0)
			{
				blockCalls = 0;
				equip(mc.player);
				mc.options.setCameraType(Boolean.getBoolean("pulselib.test.reproduceCamera") ? CameraType.FIRST_PERSON : CameraType.THIRD_PERSON_BACK);
				PPlayerAnimations.getHandle(mc.player, PPlayerBallDemo.ID).stopAll();
				PPlayerBallDemo.play(mc.player);
				worldStart = mc.player.tickCount;
			}

			if (mc.player.tickCount - worldStart < 30)
				return;
			for (var entity : mc.level.entitiesForRendering())
				if (entity instanceof TestEntity testEntity)
					testEntity.showArmor = true;
			check(ClientTestPlugin.stageFrames > 0, "Actual block/particle/weather stage cycle must complete");
			PLibDatabase.LOGGER.info("PULSE TEST BER PROBE: block {}, type {}, removed {}, renderer {}, calls {}, camera {}, yaw {}, frustumVisible {}",
					block.getBlockPos(), block.getType(), block.isRemoved(), mc.getBlockEntityRenderDispatcher().getRenderer(block),
					blockCalls, mc.gameRenderer.getMainCamera().getPosition(), mc.player.getYRot(),
					((Frustum)field(mc.levelRenderer.getClass(), "cullingFrustum", mc.levelRenderer)).isVisible(new AABB(TEST_BLOCK)));
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

		AnimationLifecycleHarness.run(TestEntityRender.MODEL_DATA.getModel());
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
		PLibDatabase.LOGGER.info("PULSE TEST graphics and animation pass");
		runExplicitCleanup(gecko);
		if (mc.level == null)
		{
			completed = true;
			PLibDatabase.LOGGER.info("PULSE TEST CLIENT PASS: graphics, animation and explicit cleanup (no world lifecycle requested)");
			mc.stop();
			return;
		}
		draw(pyramid, true);
		seedManagers(gecko);
		PPlayerBallDemo.play(mc.player);
		naturalResources = SmokeResources.capture();
		disconnectDeadline = System.nanoTime() + 15_000_000_000L;
		mc.level.disconnect();
		mc.disconnect();
	}

	private static void runExplicitCleanup(PBakedModel model)
	{
		seedManagers(model);
		SmokeResources resources = SmokeResources.capture();
		explicitCleanup = true;
		try
		{
			PulseRuntime.disconnected();
		}
		finally
		{
			explicitCleanup = false;
		}
		resources.assertDeleted("explicit cleanup");
		PLibDatabase.LOGGER.info("PULSE TEST EXPLICIT CLEANUP PASS");
	}

	private static void seedManagers(PBakedModel model)
	{
		Dummy sentinel = new Dummy();
		sentinel.manager.bindModel(model);
		InstanceAnimationManager.addManager(sentinel.manager);
		SingletonAnimationManager.getManager(AnimManagerKey.ofObject(sentinel), sentinel);
	}

	public static boolean observeDisconnectHook()
	{
		if (!explicitCleanup && naturalResources != null)
		{
			naturalHooks++;
			if (Boolean.getBoolean("pulselib.test.skipDisconnectHook"))
			{
				PLibDatabase.LOGGER.info("PULSE TEST NEGATIVE CONTROL: loader cleanup hook suppressed");
				return true;
			}
		}
		return false;
	}

	public static void recordBlockRender(TestBlockEntity block)
	{
		if (block.getBlockPos().equals(TEST_BLOCK) && block.getLevel() == Minecraft.getInstance().level)
		{
			blockCalls++;
		}
	}

	public static void lockWorldCamera()
	{
		Minecraft mc = Minecraft.getInstance();
		if (worldStart < 0 || mc.player == null || naturalResources != null)
		{
			return;
		}
		KeyMapping.releaseAll();
		float yaw = Boolean.getBoolean("pulselib.test.reproduceCamera") ? 180.0f : 0.0f;
		mc.player.setYRot(yaw);
		mc.player.yRotO = yaw;
		mc.player.yBodyRot = yaw;
		mc.player.yBodyRotO = yaw;
		mc.player.setXRot(0.0f);
		mc.player.xRotO = 0.0f;
	}

	private static void equip(net.minecraft.world.entity.LivingEntity player)
	{
		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(DemoContent.helmet.get()));
		player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(DemoContent.chestplate.get()));
		player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(DemoContent.blockItem.get()));
		player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(DemoContent.tail.get()));
	}

	private static List<PBakedMesh> meshes(PBakedModel model)
	{
		return model.bones().stream().flatMap(SmokeHarness :: meshes).toList();
	}

	private static Stream<PBakedMesh> meshes(PBakedBone bone)
	{
		return Stream.concat(bone.meshes().stream(), bone.children().stream().flatMap(SmokeHarness :: meshes));
	}

	private static void draw(PBakedMesh mesh, boolean translucent)
	{
		var target = new TextureTarget(64, 64, true, Minecraft.ON_OSX);
		Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
		VertexSorting sorting = RenderSystem.getVertexSorting();
		var modelView = RenderSystem.getModelViewStack();
		modelView.pushMatrix();
		try
		{
			while (GL11.glGetError() != GL11.GL_NO_ERROR)
			{
			}
			target.setClearColor(0f, 0f, 0f, 0f);
			target.clear(Minecraft.ON_OSX);
			target.bindWrite(true);
			modelView.identity();
			RenderSystem.applyModelViewMatrix();
			RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(-1f, 1f, -1f, 1f, -10f, 10f), VertexSorting.ORTHOGRAPHIC_Z);
			RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
			var type = translucent ? PRenderTypes.RenderTypeProvider.trianglesTranslucentEmissive(PResourceCache.ATLAS_LOCATION) :
				PRenderTypes.RenderTypeProvider.trianglesSolidEmissive(PResourceCache.ATLAS_LOCATION);
			PDeformerStack stack = PDeformerStack.compile(List.of(new PDeformerInstance<>(PBendDeformer.INSTANCE,
				new PBendDefinition(new Vector3f(), new Vector3f(0, 1, 0), new Vector3f(1, 0, 0), 1f, 1f,
					new PChannelReference<>("test_angle", (float)Math.toRadians(15))))));
			var submission = PGpuDeformerBuffers.submit(new PMeshDeformation(stack, PDeformerValueSource.DEFAULTS, mesh));
			check(submission.enabled(), "GPU deformer stream must receive operations");
			var geometry = PGpuDeformedMeshBuffers.resolve(mesh, 1);
			for (int i = 0; i < 8; i++)
				PRenderQueue.submit(PRenderQueue.RenderStage.ENTITIES, type, geometry,
					new PInstanceHeader(meshTransform(mesh).translate(0, 0, i * 0.001f), translucent ? 0x80FFFFFF : -1, 0xF000F0, OverlayTexture.NO_OVERLAY,
						submission.operationOffset(), submission.valueOffset(), submission.operationCount()));
			PRenderQueue.flush(PRenderQueue.RenderStage.ENTITIES, target);
			PGpuDeformerBuffers.finishFrame();
			int[] before = glState();
			PRenderQueue.compositeTranslucency(target);
			check(Arrays.equals(before, glState()), "OIT composite must restore indexed blend/depth/scissor state");
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, target.frameBufferId);
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
		}
		finally
		{
			target.destroyBuffers();
			modelView.popMatrix();
			RenderSystem.applyModelViewMatrix();
			RenderSystem.setProjectionMatrix(projection, sorting);
			Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
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

	static List<Integer> captureGeometryBuffers()
	{
		Object resources = field(PRenderQueue.class, "RESOURCES", null);
		Object arena = field(resources.getClass(), "geometryArena", resources);
		var pages = (List<?>)field(arena.getClass(), "pages", arena);
		var buffers = new ArrayList<Integer>();
		for (Object page : pages)
		{
			buffers.add((Integer)field(page.getClass(), "vertexBuffer", page));
			buffers.add((Integer)field(page.getClass(), "indexBuffer", page));
		}
		check(!buffers.isEmpty(), "Geometry arena must allocate GPU buffers");
		return buffers;
	}

	static @Nullable Object field(Class<?> type, String name, @Nullable Object instance)
	{
		try
		{
			var field = type.getDeclaredField(name);
			field.setAccessible(true);
			return field.get(instance);
		}
		catch (ReflectiveOperationException exception)
		{
			throw new AssertionError(exception);
		}
	}

	static void check(boolean condition, String message)
	{
		if (!condition)
			throw new AssertionError("PulseLib smoke test: " + message);
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
