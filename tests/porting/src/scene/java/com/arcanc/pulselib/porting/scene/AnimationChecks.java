/**
 * @author ArcAnc
 * Created at: 20.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.porting.scene;

import com.arcanc.pulselib.content.model.animation.PAnimation;
import com.arcanc.pulselib.content.model.animation.PAnimationGraph;
import com.arcanc.pulselib.content.model.animation.PAnimationGraphRuntime;
import com.arcanc.pulselib.content.model.animation.PAnimationState;
import com.arcanc.pulselib.content.model.animation.PAnimationTransition;
import com.arcanc.pulselib.content.model.animation.PCondition;
import com.arcanc.pulselib.content.model.animation.PInterpolationType;
import com.arcanc.pulselib.content.model.animation.PInterruptionPolicy;
import com.arcanc.pulselib.content.model.baked.PBakedBone;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.data.gecko.PGeckoModelParser;
import com.arcanc.pulselib.data.gltf.PGltfModelParser;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class AnimationChecks
{
	private AnimationChecks()
	{
	}

	static void run() throws IOException
	{
		PAnimation gecko = gecko();
		PAnimation gltf = gltf();
		graph(gecko, gltf);
		System.out.println("PASS Gecko Euler Catmull-Rom/pre-post, glTF quaternion interpolation, graph blend/overlay/reverse");
	}

	private static PAnimation gecko() throws IOException
	{
		var model = PGeckoModelParser.parse(json("""
				{"minecraft:geometry":[{"description":{"identifier":"geometry.fixture"},"bones":[{"name":"root","pivot":[0,0,0]}]}]}
				"""), json("""
				{"animations":{"gecko":{"animation_length":3,"bones":{"root":{"rotation":{
				  "0":[0,0,0],
				  "1":{"pre":[0,0,15],"post":[0,0,30],"lerp_mode":"catmullrom"},
				  "2":{"pre":[0,0,90],"post":[0,0,100]},
				  "3":[0,0,200]
				}}}}}}
				"""));
		PAnimation animation = Objects.requireNonNull(model.animations.get("gecko"));
		SceneChecks.check(animation.length() == 60 && model.bones.size() == 1, "Gecko timeline/model size");
		assertRotation(animation, 10, 7.5);
		assertRotation(animation, 20, 30);
		assertRotation(animation, 30, 55);
		assertRotation(animation, 40, 100);
		return animation;
	}

	private static PAnimation gltf() throws IOException
	{
		float half = (float)Math.sqrt(0.5);
		var bytes = ByteBuffer.allocate(40).order(ByteOrder.LITTLE_ENDIAN);
		bytes.asFloatBuffer().put(new float[]{0, 1, 0, 0, 0, 1, 0, 0, half, half});
		String binary = Base64.getEncoder().encodeToString(bytes.array());
		var model = PGltfModelParser.parse(json("""
				{"asset":{"version":"2.0"},"scene":0,"scenes":[{"nodes":[0]}],"nodes":[{"name":"root"}],
				 "buffers":[{"byteLength":40,"uri":"data:application/octet-stream;base64,%s"}],
				 "bufferViews":[{"buffer":0,"byteOffset":0,"byteLength":8},{"buffer":0,"byteOffset":8,"byteLength":32}],
				 "accessors":[{"bufferView":0,"componentType":5126,"count":2,"type":"SCALAR","min":[0],"max":[1]},
				              {"bufferView":1,"componentType":5126,"count":2,"type":"VEC4"}],
				 "animations":[{"name":"gltf","samplers":[{"input":0,"output":1,"interpolation":"LINEAR"}],
				                "channels":[{"sampler":0,"target":{"node":0,"path":"rotation"}}]}]}
				""".formatted(binary)));
		PAnimation animation = Objects.requireNonNull(model.animations.get("gltf"));
		SceneChecks.check(animation.length() == 20 && model.bones.size() == 1, "glTF timeline/model size");
		assertRotation(animation, 0, 0);
		assertRotation(animation, 10, 45);
		assertRotation(animation, 20, 90);
		return animation;
	}

	private static void graph(PAnimation gecko, PAnimation gltf)
	{
		var bone = new PBakedBone("root", new Vector3f(), new Quaternionf(), List.of(), null, List.of());
		var overlay = new PAnimation("overlay", gltf.length(), gltf.boneAnimations());
		var model = new PBakedModel(List.of(bone), Map.of("gecko", gecko, "gltf", gltf, "overlay", overlay));
		var runtime = new PAnimationGraphRuntime(new PAnimationGraph(
				List.of(new PAnimationState.Clip("idle", "gecko"), new PAnimationState.Clip("move", "gltf"),
						new PAnimationState.OneShotOverlay("gesture", "play", "overlay")),
				List.of(new PAnimationTransition(0, 1, PCondition.triggered("move"), 4, 1, PInterruptionPolicy.FROM_CURRENT))));
		runtime.parameters().trigger("move");
		runtime.parameters().trigger("play");
		runtime.tickPrecise(model, 0);
		runtime.tickPrecise(model, 2);
		var layers = runtime.layers(model);
		SceneChecks.check(runtime.isTransitioning() && layers.size() == 3 &&
				layers.get(0).animation().equals("gecko") && layers.get(0).weight() == 0.5f &&
				layers.get(1).animation().equals("gltf") && layers.get(1).weight() == 0.5f &&
				layers.get(2).overlay() && layers.get(2).time() == 2.0f, "Animation Graph midpoint/overlay");
		SceneChecks.check(!runtime.parameters().isTriggered("move") && !runtime.parameters().isTriggered("play"), "Graph trigger consumption");
		runtime.setOverlaySpeed("gesture", -1);
		var tracks = runtime.tickPrecise(model, 1);
		SceneChecks.check(tracks.stream().anyMatch(track -> track.animation().equals("overlay") && track.from() == 2 && track.to() == 1) &&
				runtime.layers(model).getLast().time() == 1.0f, "Animation Graph reverse overlay interval");
		runtime.tickPrecise(model, 1);
		SceneChecks.check(!runtime.isTransitioning() && runtime.stateIndex() == 1 &&
				runtime.layers(model).size() == 1 && runtime.layers(model).getFirst().weight() == 1.0f, "Animation Graph target/overlay completion");
		runtime.reset();
		SceneChecks.check(runtime.stateIndex() == 0 && runtime.time() == 0 &&
				runtime.layers(model).getFirst().animation().equals("gecko"), "Animation Graph reset");
	}

	private static void assertRotation(PAnimation animation, float tick, double degrees)
	{
		var frame = Objects.requireNonNull(animation.calculateBoneTransformations("root", tick, PInterpolationType.LINEAR));
		var expected = new Quaternionf().rotationZ((float)Math.toRadians(degrees));
		SceneChecks.check(Math.abs(frame.rotation().dot(expected)) > 0.999999f,
				animation.name() + " quaternion at " + tick + ": " + frame.rotation());
	}

	private static ByteArrayInputStream json(String data)
	{
		return new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
	}
}
