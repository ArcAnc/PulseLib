/**
 * @author ArcAnc
 * Created at: 13.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1;

import com.arcanc.pulselib.api.registration.PModelRegistrar;
import com.arcanc.pulselib.api.registration.PPlayerAnimationRegistrar;
import com.arcanc.pulselib.content.animatable.AnimManagerKey;
import com.arcanc.pulselib.content.animatable.ControllerState;
import com.arcanc.pulselib.content.model.animation.BoneFrame;
import com.arcanc.pulselib.content.model.animation.PPoseEasing;
import com.arcanc.pulselib.content.model.animation.PRawAnimation;
import com.arcanc.pulselib.content.model.animation.PTransitionInterruptionPolicy;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationAnchors;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationDefinition;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationFrame;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationInstance;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import com.arcanc.pulselib.content.player.animation.PPlayerPart;
import com.arcanc.pulselib.content.player.animation.firstPerson.PFirstPersonCameraMode;
import com.arcanc.pulselib.content.player.animation.firstPerson.PPlayerFirstPersonSettings;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.data.gecko.PGeckoModelLoader;
import com.arcanc.pulselib.data.gltf.PGltfModelLoader;
import com.arcanc.pulselib.util.PLibDatabase;
import com.mojang.authlib.GameProfile;

import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Native Camera M-P3 oracles from the pinned PlayerMotionHarness, plus extracted ROOT lifetime.
 */
public final class Stage52CameraProbe
{
	private static final boolean ENABLED = Boolean.getBoolean("pulselib.stage52bProbe");
	private static final List<PModelData> MODELS = List.of(
		PModelData.direct(PLibDatabase.rl("player/regression/motion"), PGeckoModelLoader.INSTANCE.id()),
		PModelData.direct(PLibDatabase.rl("player/regression/motion_gltf"), PGltfModelLoader.INSTANCE.id()));
	private static final List<Scene> SCENES = scenes();
	private static final Set<String> VERIFIED = new HashSet<>();
	private static final UUID OBSERVER = new UUID(0, 0x525b01);
	private static @Nullable Scene selectedScene;
	private static int sceneIndex;
	private static int startTick = -1;
	private static int elapsed;
	private static int phase;
	private static int cameraChecks;
	private static @Nullable Snapshot beforeCamera;
	private static @Nullable Camera observedCamera;
	private static @Nullable Vec3 baselinePosition;
	private static @Nullable Quaternionf baselineRotation;
	private static float cameraPartialTick;
	private static @Nullable Identifier observedDefinition;
	private static @Nullable CompletableFuture<Void> reload;
	private static @Nullable Camera foreignCamera;
	private static @Nullable Vec3 foreignPosition;
	private static @Nullable Quaternionf foreignRotation;
	private static int foreignChecks;
	private static boolean completed;

	private Stage52CameraProbe()
	{
	}

	private static List<Scene> scenes()
	{
		List<Scene> scenes = new ArrayList<>();
		for (int format = 0; format < MODELS.size(); format++)
		{
			for (String clip : new String[] {"F", "G", "H"})
				for (float weight : new float[] {0.25f, 0.6f, 1})
					scenes.add(new Scene(PLibDatabase.rl("stage52b/" + format + "/" + clip.toLowerCase() + "/" + (int)(weight * 100)), MODELS.get(format), clip, weight, PFirstPersonCameraMode.ANIMATED, format));
			scenes.add(new Scene(PLibDatabase.rl("stage52b/" + format + "/vanilla"), MODELS.get(format), "I", 1, PFirstPersonCameraMode.VANILLA, format));
		}
		return List.copyOf(scenes);
	}

	public static void register()
	{
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (GameShuttingDownEvent event) ->
		{
			check(completed, "All native camera and ROOT suites complete before shutdown");
			PLibDatabase.LOGGER.info("STAGE 5.2b.1 NATIVE PLAYER SHUTDOWN PASS");
		});
	}

	public static void registerModels(PModelRegistrar registrar)
	{
		if (ENABLED)
			for (PModelData model : MODELS)
				registrar.model(model).texture("0", PLibDatabase.rl("stage42a_white"));
	}

	public static void registerAnimations(PPlayerAnimationRegistrar registrar)
	{
		if (!ENABLED)
			return;
		for (Scene scene : SCENES)
			registrar.register(scene.id, PPlayerAnimationDefinition.builder(scene.model).
					when(player -> selectedScene == scene).
					bind(PPlayerPart.ROOT, "root").
					bind(PPlayerPart.BODY, "body").
					bind(PPlayerPart.RIGHT_ARM, "arm").
					anchor(PPlayerAnimationAnchors.FIRST_PERSON_CAMERA, "camera").
					firstPerson(new PPlayerFirstPersonSettings(true, 0, 0, scene.mode)).
					rootPivot(0.2f, 0.3f, 0.4f).
					weight(scene.weight).
					crossfade(2, PPoseEasing.LINEAR, PTransitionInterruptionPolicy.FROM_CURRENT).
					controllers(controllers -> controllers.add("motion", () -> state -> state.controller().isStopped() ? ControllerState.STOP : ControllerState.PLAY)).
					build());
	}

	public static void frame()
	{
		var mc = Minecraft.getInstance();
		if (!ENABLED || completed || mc.player == null || mc.level == null || mc.getOverlay() != null || MODELS.stream().anyMatch(data -> data.getModel() == null))
			return;
		check(mc.gameDirectory.toPath().toAbsolutePath().normalize().getParent().getFileName().toString().startsWith("stage52b"), "Only a disposable stage52b directory");
		if (++elapsed > 1800)
			throw new AssertionError("Stage 5.2b.1 native motion timeout");
		mc.mouseHandler.releaseMouse();
		KeyMapping.releaseAll();
		mc.options.setCameraType(CameraType.FIRST_PERSON);
		if (mc.player.tickCount < 20)
			return;
		if (reload != null)
		{
			if (!reload.isDone())
				return;
			reload.join();
			reload = null;
			phase = 1;
		}
		createObserver();
		if (sceneIndex == SCENES.size())
		{
			selectedScene = null;
			if (phase == 0)
			{
				PLibDatabase.LOGGER.info("STAGE 5.2b.1 PRE-RELOAD PASS: cameraCases={}, nativeCameraChecks={}, rootSuites={}, foreignCameras={}", VERIFIED.size(), cameraChecks, Stage52RootProbe.suites(), foreignChecks);
				sceneIndex = 0;
				startTick = -1;
				reload = mc.reloadResourcePacks();
				return;
			}
			check(VERIFIED.size() == 2 * SCENES.size() && Stage52RootProbe.suites() == 4 && foreignChecks == 4 && Stage52RootProbe.worldSamples() > 0,
				  "Both formats/phases, F/G/H weights, VANILLA, native world ROOT, nested failures and foreign cameras: cases=" + VERIFIED.size() + ", roots=" + Stage52RootProbe.suites() + ", foreign=" + foreignChecks + ", world=" + Stage52RootProbe.worldSamples());
			Stage2PluginProbe.verify(true);
			if (Boolean.getBoolean("pulselib.stage52b2Probe"))
				Stage52RigProbe.completed();
			if (Boolean.getBoolean("pulselib.stage52b2bProbe"))
				Stage52PartsProbe.completed();
			PLibDatabase.LOGGER.info("STAGE 5.2b.1 NATIVE PLAYER PASS: cameraCases={}, cameraChecks={}, rootSuites={}, nativeWorldRoots={}, foreignCameras={}, formats=Gecko/glTF, F/G/H weights=0.25/0.6/1, VANILLA, extraction/nested failure/recovery/reload PASS", VERIFIED.size(), cameraChecks, Stage52RootProbe.suites(), Stage52RootProbe.worldSamples(), foreignChecks);
			completed = true;
			mc.stop();
			return;
		}
		if (startTick < 0)
		{
			for (Player player : mc.level.players())
				for (Scene scene : SCENES)
					PPlayerAnimations.getHandle(player, scene.id).stopAll();
			selectedScene = SCENES.get(sceneIndex);
			for (Player player : mc.level.players())
				check(PPlayerAnimations.getHandle(player, selectedScene.id).play("motion", PRawAnimation.begin().thenLoop(selectedScene.clip).build()), "Camera fixture controller starts");
			startTick = mc.player.tickCount;
			return;
		}
		if (mc.player.tickCount < startTick + 6 || !VERIFIED.contains(key()))
			return;
		if (selectedScene.clip.equals("G") && selectedScene.weight == 1)
		{
			Stage52RootProbe.verify();
			verifyForeignCamera();
		}
		if (Boolean.getBoolean("pulselib.stage52b2Probe") && selectedScene.clip.equals("H") && selectedScene.weight == 1)
			Stage52RigProbe.verify(selectedScene.id, selectedScene.clip);
		if (Boolean.getBoolean("pulselib.stage52b2bProbe") && selectedScene.clip.equals("H") && selectedScene.weight == 1)
			Stage52PartsProbe.verify(selectedScene.id, selectedScene.clip);
		sceneIndex++;
		startTick = -1;
	}

	private static String key()
	{
		return phase + ":" + sceneIndex;
	}

	private static void createObserver()
	{
		var mc = Minecraft.getInstance();
		if (mc.level.players().stream().anyMatch(player -> player.getUUID().equals(OBSERVER)))
			return;
		var observer = new RemotePlayer(mc.level, new GameProfile(OBSERVER, "Stage52bObserver"));
		observer.setId(0x525b01);
		observer.setPos(mc.player.getX() + 1, mc.player.getY(), mc.player.getZ() + 3);
		mc.level.addEntity(observer);
	}

	public static void before(Camera camera)
	{
		if (camera == foreignCamera)
			return;
		observedCamera = null;
		baselinePosition = null;
		var mc = Minecraft.getInstance();
		if (selectedScene != null && mc.player != null && camera.entity() == mc.player)
			beforeCamera = new Snapshot(mc.player);
	}

	public static void baseline(Camera camera, float partialTick)
	{
		if (camera == foreignCamera)
		{
			foreignPosition = camera.position();
			foreignRotation = new Quaternionf(camera.rotation());
			return;
		}
		var mc = Minecraft.getInstance();
		if (selectedScene == null || mc.player == null || camera.entity() != mc.player || !mc.options.getCameraType().isFirstPerson())
			return;
		observedCamera = camera;
		baselinePosition = camera.position();
		baselineRotation = new Quaternionf(camera.rotation());
		cameraPartialTick = partialTick;
		observedDefinition = selectedScene.id;
	}

	public static void after(Camera camera)
	{
		if (camera == foreignCamera)
		{
			check(foreignPosition != null && foreignPosition.distanceToSqr(camera.position()) < 1.0e-10, "Other camera entity retains its native position");
			rotation(foreignRotation, camera.rotation(), "Other camera entity retains its native rotation");
			foreignChecks++;
			return;
		}
		var mc = Minecraft.getInstance();
		if (selectedScene == null || observedCamera != camera || mc.player == null || mc.getOverlay() != null || selectedScene.model.getModel() == null)
			return;
		var instance = PPlayerAnimations.getInstance(mc.player, selectedScene.id);
		if (!instance.getAnimationManager(new AnimManagerKey(0)).getControllers().get("motion").isPlaying())
			return;
		verifyNativeCamera(mc.player);
		if (startTick >= 0 && sceneIndex < SCENES.size() && selectedScene == SCENES.get(sceneIndex) && mc.player.tickCount >= startTick + 3)
			VERIFIED.add(key());
	}

	private static void verifyForeignCamera()
	{
		var mc = Minecraft.getInstance();
		Player observer = mc.level.players().stream().filter(player -> player.getUUID().equals(OBSERVER)).findFirst().orElseThrow();
		var local = new Snapshot(mc.player);
		var remote = new Snapshot(observer);
		foreignCamera = new Camera();
		foreignCamera.setEntity(observer);
		foreignPosition = null;
		try
		{
			var method = Camera.class.getDeclaredMethod("alignWithEntity", float.class);
			method.setAccessible(true);
			method.invoke(foreignCamera, 0.65f);
			check(foreignPosition != null, "Native foreign camera baseline observed");
			local.check(mc.player);
			remote.check(observer);
		}
		catch (ReflectiveOperationException failure)
		{
			throw new AssertionError("Foreign camera native invocation", failure);
		}
		finally
		{
			foreignCamera = null;
		}
	}

	private static void verifyNativeCamera(Player player)
	{
		if (observedCamera == null)
			return;

		check(selectedScene.id.equals(observedDefinition), "native camera observed the current fixture");
		beforeCamera.check(player);
		PPlayerAnimationInstance instance = PPlayerAnimations.getInstance(player, selectedScene.id);
		PPlayerAnimationFrame frame = instance.sampleFrame(cameraPartialTick);
		if (frame.definition().firstPersonSettings().cameraMode() == PFirstPersonCameraMode.VANILLA)
		{
			check(PPlayerAnimations.cameraPose(player, cameraPartialTick) == null, "VANILLA has no animated camera pose");
			check(baselinePosition.distanceToSqr(observedCamera.position()) < 1.0e-10, "VANILLA camera position");
			rotation(baselineRotation, observedCamera.rotation(), "VANILLA camera rotation");
			cameraChecks++;
			return;
		}
		PBakedModel model = frame.definition().modelData().getModel();
		BoneFrame root = frame.localTransform("root");
		Vector3f pivot = frame.definition().rootPivot();
		Vector3f rootTranslation = new Vector3f(root.translation()).sub(model.bone(model.boneIndex("root")).basePosition());
		Quaternionf rootRotation = new Quaternionf(root.rotation()).mul(new Quaternionf(model.bone(model.boneIndex("root")).baseRotation()).invert());
		Matrix4f visualRoot = new Matrix4f().translate(rootTranslation).translate(pivot).rotate(rootRotation).scale(root.scale()).translate(new Vector3f(pivot).negate());
		Matrix4f current = visualRoot.mul(sequential(frame, model, "root", false).invert()).mul(sequential(frame, model, "camera", false));
		Matrix4f bind = sequential(frame, model, "root", true).invert().mul(sequential(frame, model, "camera", true));
		float previousActivation = (Float)Stage41bGeometryProbe.field(PPlayerAnimationInstance.class, instance, "previousFirstPersonActivation");
		float activation = (Float)Stage41bGeometryProbe.field(PPlayerAnimationInstance.class, instance, "firstPersonActivation");
		float cameraWeight = frame.definition().weight(player, cameraPartialTick) *
							 Mth.lerp(cameraPartialTick, previousActivation, activation);
		Vector3f displacement = current.transformPosition(new Vector3f()).sub(bind.transformPosition(new Vector3f())).mul(cameraWeight);
		Quaternionf delta = sequentialRotation(frame, model, "camera", false).mul(sequentialRotation(frame, model, "camera", true).invert());
		delta = new Quaternionf().slerp(delta, cameraWeight);
		// Independent basis oracle: Gecko player axes -> Minecraft is diag(-1,-1,1).
		// glTF -> player -> Minecraft applies that basis twice and cancels.
		if (frame.definition().modelData().getModelLoaderId().equals(PGeckoModelLoader.INSTANCE.id()))
		{
			displacement.mul(-1, -1, 1);
			delta.set(-delta.x, -delta.y, delta.z, delta.w);
		}
		Quaternionf body = new Quaternionf().rotationY((float)Math.toRadians(180 - Mth.rotLerp(cameraPartialTick, player.yBodyRotO, player.yBodyRot)));
		displacement.rotate(body);
		Vec3 expectedPosition = baselinePosition.add(displacement.x, displacement.y, displacement.z);
		check(expectedPosition.distanceToSqr(observedCamera.position()) < 1.0e-8,
			  "native camera anchor position; scenario=" + sceneIndex + " expected=" + expectedPosition + " actual=" + observedCamera.position());
		Quaternionf expectedRotation = new Quaternionf(body).mul(delta).mul(new Quaternionf(body).invert()).mul(baselineRotation);
		rotation(expectedRotation, observedCamera.rotation(), "native camera anchor orientation");
		cameraChecks++;
	}

	private static Quaternionf sequentialRotation(PPlayerAnimationFrame frame, PBakedModel model, String bone, boolean bind)
	{
		List<Integer> chain = new ArrayList<>();
		for (int index = model.boneIndex(bone); index >= 0; index = model.parentIndex(index))
			chain.addFirst(index);

		Quaternionf result = new Quaternionf();
		for (int index : chain)
			result.mul(bind ? model.bone(index).baseRotation() : frame.localTransform(model.bone(index).name()).rotation());

		return result;
	}

	private static void rotation(Quaternionf expected, Quaternionf actual, String label)
	{
		check(Math.abs(expected.dot(actual)) > 0.99999f, label + " expected=" + expected + " actual=" + actual);
	}

	private static Matrix4f sequential(PPlayerAnimationFrame frame, PBakedModel model, String bone, boolean bind)
	{
		List<Integer> chain = new ArrayList<>();
		for (int index = model.boneIndex(bone); index >= 0; index = model.parentIndex(index))
			chain.addFirst(index);

		Matrix4f result = new Matrix4f();
		for (int index : chain)
			if (bind)
				result.translate(model.bone(index).basePosition()).rotate(model.bone(index).baseRotation());

			else
			{
				BoneFrame local = frame.localTransform(model.bone(index).name());
				result.translate(local.translation()).rotate(local.rotation()).scale(local.scale());
			}

		return result;
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
			throw new AssertionError("Stage 5.2b.1: " + message);
	}

	private record Scene(Identifier id, PModelData model, String clip, float weight, PFirstPersonCameraMode mode, int format)
	{
	}

	private record Snapshot(Vec3 position, Vec3 movement, AABB bounds, float width, float height)
	{
		Snapshot(Player player)
		{
			this(player.position(), player.getDeltaMovement(), player.getBoundingBox(), player.getBbWidth(), player.getBbHeight());
		}

		void check(Player player)
		{
			Stage52CameraProbe.check(this.position.equals(player.position()) && this.movement.equals(player.getDeltaMovement()) &&
										 this.bounds.equals(player.getBoundingBox()) && this.width == player.getBbWidth() && this.height == player.getBbHeight(),
									 "root animation tick/render/extraction must preserve position, movement, bounds and dimensions");
		}
	}
}
