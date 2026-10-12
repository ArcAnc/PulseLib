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
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public final class SmokeHarness
{
	private static boolean completed;
	private static int reloads;
	private static int worlds;
	private static int worldStart = -1;
	private static @Nullable CompletableFuture<Void> reloading;
	private static @Nullable PBakedModel previous;
	private static @Nullable TextureAtlasSprite previousSprite;

	private SmokeHarness()
	{
	}

	public static void frame()
	{
		if (completed)
			return;
		Minecraft mc = Minecraft.getInstance();
		if (Boolean.getBoolean("pulselib.test.world") && (mc.player == null || mc.level == null))
			return;
		if (mc.getOverlay() != null || PModelCache.getModels() == null)
			return;
		if (reloading != null && !reloading.isDone())
			return;
		if (reloading != null)
			reloading.join();
		PBakedModel gltf = ClientTestPlugin.GLTF.getModel();
		PBakedModel gecko = ClientTestPlugin.GECKO.getModel();
		check(gltf != null && gecko != null, "Both model parsers must load");
		check(!meshes(gltf).isEmpty(), "Original glTF item must bake geometry");
		check(!gecko.bones().isEmpty() && !gecko.animations().
				isEmpty(), "Original Gecko player rig must load bones and animations");
		if (Boolean.getBoolean("pulselib.test.geckoEntity"))
			check(!meshes(TestEntityRender.MODEL_DATA.getModel()).isEmpty(), "Original Gecko entity must bake geometry");
		check(ClientTestPlugin.constructions == 1 && CommonTestPlugin.constructions == 1, "Plugin instances must survive reload");
		check(ClientTestPlugin.attachments == 1 && ClientTestPlugin.playerAnimations == 1 && ClientTestPlugin.animatedRegistrations == 1
				&& CommonTestPlugin.registrations == 1 && CommonTestPlugin.events == 1 && CommonTestPlugin.deformers == 1,
			"One-time phases must not repeat");
		check(PLivingAttachments.contains(DemoContent.helmet.get()), "Attachment was not registered");
		TextureAtlasSprite sprite = PResourceCache.getTextureAtlas().
				getSprite(PResourceCache.resolve(ClientTestPlugin.GLTF.getModelId(), "circle"));
		check(!sprite.contents().name().getPath().equals("missingno"), "Test texture must be stitched");
		if (previous != null)
		{
			check(previous != gltf, "Models must be rebuilt on reload");
			check(previousSprite != sprite, "Texture references must be rebuilt on reload");
		}
		if (Boolean.getBoolean("pulselib.test.world"))
		{
			if (worldStart < 0)
			{
				mc.player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(DemoContent.helmet.get()));
				mc.player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(DemoContent.chestplate.get()));
				mc.player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(DemoContent.blockItem.get()));
				mc.player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(DemoContent.tail.get()));
				mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
				if (mc.getSingleplayerServer() != null)
				{
					var server = mc.getSingleplayerServer();
					var uuid = mc.player.getUUID();
					server.execute(() ->
					{
						var player = server.getPlayerList().getPlayer(uuid);
						if (player != null)
						{
							player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(DemoContent.helmet.get()));
							player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(DemoContent.chestplate.get()));
							player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(DemoContent.blockItem.get()));
							player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(DemoContent.tail.get()));
							if (reloads == 0)
							{
								var pos = player.blockPosition().offset(2, 0, 0);
								player.serverLevel().
										setBlockAndUpdate(pos, DemoContent.block.get().defaultBlockState());
								if (player.serverLevel().getBlockEntity(pos) instanceof TestBlockEntity block)
									block.changePlayAnimation();
								var entity = DemoContent.entity.get().create(player.serverLevel());
								entity.moveTo(player.getX() - 2, player.getY(), player.getZ());
								player.serverLevel().addFreshEntity(entity);
							}
						}
					});
				}
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
			check(TestBlockEntityRenderer.renders > 0, "Original block entity renderer must run");
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
		previous = gltf;
		previousSprite = sprite;
		PLibDatabase.LOGGER.info("PULSE TEST graphics and animation pass, reload {}", reloads);
		if (reloads++ < 3)
		{
			worldStart = -1;
			reloading = mc.reloadResourcePacks();
			return;
		}
		check(ClientTestPlugin.models >= 4, "Model registration must repeat across resource reloads");
		completed = true;
		List<Integer> buffers = captureGeometryBuffers();
		if (mc.level != null)
		{
			mc.level.disconnect();
			mc.disconnect();
		}
		PulseRuntime.disconnected();
		for (int buffer : buffers)
			check(!GL15.glIsBuffer(buffer), "GPU geometry buffer leaked on disconnect: " + buffer);
		check(((Collection<?>)field(InstanceAnimationManager.class, "MANAGERS", null)).isEmpty(), "Instance managers leaked");
		check(((Map<?, ?>)field(SingletonAnimationManager.class, "MANAGERS", null)).isEmpty(), "Singleton managers leaked");
		check(((Map<?, ?>)field(PPlayerAnimations.class, "INSTANCES", null)).isEmpty(), "Player animation instances leaked");
		if (Boolean.getBoolean("pulselib.test.worldSwitch") && worlds == 0)
		{
			worlds = 1;
			PLibDatabase.LOGGER.info("PULSE TEST WORLD CLEANUP PASS: entering second test world");
			reloads = 0;
			worldStart = -1;
			previous = null;
			previousSprite = null;
			reloading = null;
			mc.createWorldOpenFlows().openWorld("pulselib-smoke-second", () ->
			{
				throw new AssertionError("Second smoke world failed to open");
			});
			completed = false;
			return;
		}
		PLibDatabase.LOGGER.info("PULSE TEST CLIENT PASS: glTF, Gecko, animation, attachment, instancing, GPU deformation, OIT, three reloads, GL state, {} worlds, cleanup", Boolean.getBoolean("pulselib.test.world") ? worlds + 1 : 0);
		mc.stop();
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

	private static List<Integer> captureGeometryBuffers()
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

	private static @Nullable Object field(Class<?> type, String name, @Nullable Object instance)
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

	private static void check(boolean condition, String message)
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
