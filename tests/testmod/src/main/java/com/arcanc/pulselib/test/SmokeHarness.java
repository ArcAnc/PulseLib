package com.arcanc.pulselib.test;

import com.arcanc.pulselib.content.animatable.*;
import com.arcanc.pulselib.content.animatable.instance.InstanceAnimationManager;
import com.arcanc.pulselib.content.event.PulseRuntime;
import com.arcanc.pulselib.content.model.baked.PBakedMesh;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.model.baked.PGpuDeformedMeshBuffers;
import com.arcanc.pulselib.content.model.deformer.*;
import com.arcanc.pulselib.content.model.deformer.gpu.PGpuDeformerBuffers;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.util.*;
import com.arcanc.pulselib.util.attachments.PLivingAttachments;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class SmokeHarness
{
	private static boolean completed;
	private static int reloads;
	private static int worldStart = -1;
	private static CompletableFuture<Void> reloading;
	private static PBakedModel previous;
	private static TextureAtlasSprite previousSprite;

	private SmokeHarness() { }

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
		check(!meshes(gltf).isEmpty() && !meshes(gecko).isEmpty(), "Both models must bake geometry");
		check(ClientTestPlugin.constructions == 1 && CommonTestPlugin.constructions == 1, "Plugin instances must survive reload");
		check(ClientTestPlugin.attachments == 1 && ClientTestPlugin.playerAnimations == 1 && CommonTestPlugin.registrations == 1,
				"One-time phases must not repeat");
		check(PLivingAttachments.contains(Items.DIAMOND_HELMET), "Attachment was not registered");
		TextureAtlasSprite sprite = PResourceCache.getTextureAtlas().getSprite(PResourceCache.resolve(ClientTestPlugin.GLTF.getModelId(), "base"));
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
				mc.player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
				mc.player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(TestItems.armor));
				mc.player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(TestItems.animated));
				mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
				if (mc.getSingleplayerServer() != null)
				{
					var server = mc.getSingleplayerServer();
					var uuid = mc.player.getUUID();
					server.execute(() -> {
						var player = server.getPlayerList().getPlayer(uuid);
						if (player != null)
						{
							player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
							player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(TestItems.armor));
							player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(TestItems.animated));
						}
					});
				}
				var handle = com.arcanc.pulselib.content.player.animation.PPlayerAnimations.getHandle(mc.player, ClientTestPlugin.id("player"));
				check(handle != null && handle.play("idle", com.arcanc.pulselib.content.model.animation.PRawAnimation.begin().thenLoop("idle").build()), "Player clip must start");
				worldStart = mc.player.tickCount;
			}
			if (mc.player.tickCount - worldStart < 30)
				return;
			check(TestItem.renders > 0, "Loader item renderer adapter must be invoked");
			check(TestArmorItem.setups > 0, "Loader custom armor adapter must be invoked");
			check(ClientTestPlugin.layerRenders > 0, "Attachment layer must render");
			check(ClientTestPlugin.animatedRenders > 0, "Animated attachment layer must render");
			check(com.arcanc.pulselib.content.player.animation.PPlayerAnimations.isPartAnimating(mc.player,
					com.arcanc.pulselib.content.player.animation.PPlayerPart.BODY, 0.5f), "Player body must animate");
			check(PLivingAttachments.hidesVanillaArmor(mc.player.getItemBySlot(EquipmentSlot.HEAD), EquipmentSlot.HEAD, mc.player), "Attachment must hide vanilla helmet");
			PLibDatabase.LOGGER.info("PULSE TEST WORLD PASS: item {}, armor {}, attachment {}, animated attachment {}",
					TestItem.renders, TestArmorItem.setups, ClientTestPlugin.layerRenders, ClientTestPlugin.animatedRenders);
		}

		Dummy dummy = new Dummy();
		dummy.manager.bindModel(gecko);
		var controller = dummy.manager.getControllers().get("idle");
		controller.play(com.arcanc.pulselib.content.model.animation.PRawAnimation.begin().thenLoop("idle").build());
		dummy.manager.tick();
		check(controller.getCurrentStage() != null, "Animation controller must start the Gecko clip");
		gecko.evaluate(dummy.manager.getControllers().values(), java.util.Map.of(), 0.5f);
		InstanceAnimationManager.removeManager(dummy);
		draw(meshes(gltf).getFirst(), false);
		draw(meshes(gltf).getFirst(), true);
		previous = gltf;
		previousSprite = sprite;
		PLibDatabase.LOGGER.info("PULSE TEST graphics and animation pass, reload {}", reloads);
		if (reloads++ < 2)
		{
			worldStart = -1;
			reloading = mc.reloadResourcePacks();
			return;
		}
		check(ClientTestPlugin.models >= 3, "Model registration must repeat across resource reloads");
		completed = true;
		java.util.List<Integer> buffers = captureGeometryBuffers();
		if (mc.level != null)
		{
			mc.level.disconnect();
			mc.disconnect();
		}
		PulseRuntime.disconnected();
		for (int buffer : buffers)
			check(!org.lwjgl.opengl.GL15.glIsBuffer(buffer), "GPU geometry buffer leaked on disconnect: " + buffer);
		check(((java.util.Collection<?>)field(InstanceAnimationManager.class, "MANAGERS", null)).isEmpty(), "Instance managers leaked");
		check(((java.util.Map<?, ?>)field(com.arcanc.pulselib.content.animatable.singleton.SingletonAnimationManager.class, "MANAGERS", null)).isEmpty(), "Singleton managers leaked");
		check(((java.util.Map<?, ?>)field(com.arcanc.pulselib.content.player.animation.PPlayerAnimations.class, "INSTANCES", null)).isEmpty(), "Player animation instances leaked");
		PLibDatabase.LOGGER.info("PULSE TEST CLIENT PASS: glTF, Gecko, animation, attachment, instancing, GPU deformation, OIT, two reloads, cleanup");
		mc.stop();
	}

	private static List<PBakedMesh> meshes(PBakedModel model)
	{
		return model.bones().stream().flatMap(SmokeHarness::meshes).toList();
	}

	private static java.util.stream.Stream<PBakedMesh> meshes(com.arcanc.pulselib.content.model.baked.PBakedBone bone)
	{
		return java.util.stream.Stream.concat(bone.meshes().stream(), bone.children().stream().flatMap(SmokeHarness::meshes));
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
			while (GL11.glGetError() != GL11.GL_NO_ERROR) { }
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
						new PInstanceHeader(new Matrix4f().translate(0, 0, i * 0.001f), translucent ? 0x80FFFFFF : -1, 0xF000F0, OverlayTexture.NO_OVERLAY,
								submission.operationOffset(), submission.valueOffset(), submission.operationCount()));
			PRenderQueue.flush(PRenderQueue.RenderStage.ENTITIES, target);
			PGpuDeformerBuffers.finishFrame();
			PRenderQueue.compositeTranslucency(target);
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, target.frameBufferId);
			var pixel = BufferUtils.createByteBuffer(4);
			GL11.glReadPixels(32, 24, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixel);
			int error = GL11.glGetError();
			check(error == GL11.GL_NO_ERROR, "OpenGL error " + error + " during " + (translucent ? "OIT" : "instancing"));
			PLibDatabase.LOGGER.info("PULSE TEST {} pixel [{}, {}, {}, {}]", translucent ? "OIT" : "solid",
					pixel.get(0) & 255, pixel.get(1) & 255, pixel.get(2) & 255, pixel.get(3) & 255);
			check((pixel.get(1) & 255) > 10, "Rendered pixel must contain test geometry");
			if (translucent)
				check((pixel.get(3) & 255) > 0 && (pixel.get(3) & 255) < 255, "OIT must preserve partial alpha");
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

	private static java.util.List<Integer> captureGeometryBuffers()
	{
		Object resources = field(PRenderQueue.class, "RESOURCES", null);
		Object arena = field(resources.getClass(), "geometryArena", resources);
		var pages = (java.util.List<?>)field(arena.getClass(), "pages", arena);
		var buffers = new java.util.ArrayList<Integer>();
		for (Object page : pages)
		{
			buffers.add((Integer)field(page.getClass(), "vertexBuffer", page));
			buffers.add((Integer)field(page.getClass(), "indexBuffer", page));
		}
		check(!buffers.isEmpty(), "Geometry arena must allocate GPU buffers");
		return buffers;
	}

	private static Object field(Class<?> type, String name, Object instance)
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
		@Override public PAnimationManager<Dummy> getAnimationManager(AnimManagerKey key) { return this.manager; }
		@Override public void registerAnimationControllers(PAnimationManager.PAnimationRegistrar<Dummy> registrar)
		{
			registrar.add("idle", () -> state -> ControllerState.PLAY);
		}
	}
}
