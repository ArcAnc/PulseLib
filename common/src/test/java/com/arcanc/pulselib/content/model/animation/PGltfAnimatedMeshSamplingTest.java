/**
 * @author ArcAnc
 * Created at: 29.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;

import com.arcanc.pulselib.content.model.PModel;
import com.arcanc.pulselib.data.gltf.PGltfModelParser;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class PGltfAnimatedMeshSamplingTest
{
	@Test
	void animatedMeshUnderParentRetainsChannelAndAppliesBindTransformOnce() throws IOException
	{
		PModel model = model(true);
		var animation = model.animations.get("depth");
		assertEquals(20, animation.length());
		var body = model.bones.values().stream().filter(bone -> bone.name().equals("body")).findFirst().orElseThrow();
		var root = model.bones.values().stream().filter(bone -> bone.name().equals("root")).findFirst().orElseThrow();
		assertSame(root, body.parent());
		assertEquals(3, root.pivot().x());
		assertEquals(2, body.pivot().x());
		var mesh = model.meshes.get(body.meshUUIDS().getFirst());
		assertEquals(0, mesh.primitives().getFirst().positions().get(0));
		assertEquals(1, mesh.primitives().getFirst().positions().get(3));
		for (int quarter = 0; quarter <= 4; quarter++)
		{
			var pose = animation.calculateBoneTransformations("body", quarter * 5, PInterpolationType.LINEAR);
			assertEquals(0, pose.translation().x(), 0.000001f);
			assertEquals(quarter * 0.125f, pose.translation().z(), 0.000001f);
		}
	}

	@Test
	void staticMeshUnderParentKeepsExistingFlattenedGeometry() throws IOException
	{
		PModel model = model(false);
		assertEquals(1, model.bones.size());
		var root = model.bones.values().iterator().next();
		assertEquals("root", root.name());
		var mesh = model.meshes.get(root.meshUUIDS().getFirst());
		assertEquals(2, mesh.primitives().getFirst().positions().get(0));
		assertEquals(3, mesh.primitives().getFirst().positions().get(3));
	}

	private static PModel model(boolean animated) throws IOException
	{
		ByteBuffer bytes = ByteBuffer.allocate(136).order(ByteOrder.LITTLE_ENDIAN);
		for (float value : new float[] {0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1})
			bytes.putFloat(value);
		bytes.putShort((short)0).putShort((short)1).putShort((short)2).putShort((short)0);
		for (float value : new float[] {0, 1, 2, 0, 0, 2, 0, 0.5f})
			bytes.putFloat(value);
		String animation = animated ? """
				,"animations":[{"name":"depth","samplers":[{"input":4,"output":5,"interpolation":"LINEAR"}],
				"channels":[{"sampler":0,"target":{"node":1,"path":"translation"}}]}]
				""" : "";
		String source = """
				{"asset":{"version":"2.0"},"scene":0,"scenes":[{"nodes":[0]}],
				"nodes":[{"name":"root","translation":[3,0,0],"children":[1]},{"name":"body","translation":[2,0,0],"mesh":0}],
				"buffers":[{"byteLength":136,"uri":"data:application/octet-stream;base64,%s"}],
				"bufferViews":[{"buffer":0,"byteOffset":0,"byteLength":36},{"buffer":0,"byteOffset":36,"byteLength":36},
				{"buffer":0,"byteOffset":72,"byteLength":24},{"buffer":0,"byteOffset":96,"byteLength":6},
				{"buffer":0,"byteOffset":104,"byteLength":8},{"buffer":0,"byteOffset":112,"byteLength":24}],
				"accessors":[{"bufferView":0,"componentType":5126,"count":3,"type":"VEC3","min":[0,0,0],"max":[1,1,0]},
				{"bufferView":1,"componentType":5126,"count":3,"type":"VEC3"},{"bufferView":2,"componentType":5126,"count":3,"type":"VEC2"},
				{"bufferView":3,"componentType":5123,"count":3,"type":"SCALAR"},
				{"bufferView":4,"componentType":5126,"count":2,"type":"SCALAR","min":[0],"max":[1]},
				{"bufferView":5,"componentType":5126,"count":2,"type":"VEC3"}],
				"meshes":[{"primitives":[{"attributes":{"POSITION":0,"NORMAL":1,"TEXCOORD_0":2},"indices":3}]}]%s}
				""".formatted(Base64.getEncoder().encodeToString(bytes.array()), animation);
		try (var stream = new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)))
		{
			return PGltfModelParser.parse(stream);
		}
	}
}
