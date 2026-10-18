/**
 * @author ArcAnc
 * Created at: 18.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;

import com.arcanc.pulselib.data.gltf.PGltfModelParser;

import org.joml.Quaternionf;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PGltfQuaternionSamplingTest
{
	@Test
	void decodedGltfQuaternionsKeepShortestPathSlerp() throws IOException
	{
		Quaternionf from = new Quaternionf();
		Quaternionf to = new Quaternionf().rotationY((float)Math.toRadians(270));
		ByteBuffer data = ByteBuffer.allocate(40).order(ByteOrder.LITTLE_ENDIAN);
		data.putFloat(0).putFloat(1);
		for (Quaternionf value : new Quaternionf[]{from, to})
		{
			data.putFloat(value.x).putFloat(value.y).putFloat(value.z).putFloat(value.w);
		}
		String json = """
				{"asset":{"version":"2.0"}, "scene":0, "scenes":[{"nodes":[0]}], "nodes":[{"name":"root"}],
				 "buffers":[{"byteLength":40,"uri":"data:application/octet-stream;base64,%s"}],
				 "bufferViews":[{"buffer":0,"byteOffset":0,"byteLength":8},{"buffer":0,"byteOffset":8,"byteLength":32}],
				 "accessors":[{"bufferView":0,"componentType":5126,"count":2,"type":"SCALAR","min":[0],"max":[1]},
				              {"bufferView":1,"componentType":5126,"count":2,"type":"VEC4"}],
				 "animations":[{"name":"turn","samplers":[{"input":0,"output":1,"interpolation":"LINEAR"}],
				                "channels":[{"sampler":0,"target":{"node":0,"path":"rotation"}}]}]}
				""".formatted(Base64.getEncoder().encodeToString(data.array()));
		try (var stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))
		{
			PAnimation animation = PGltfModelParser.parse(stream).animations.get("turn");
			for (int quarter = 0; quarter <= 4; quarter++)
			{
				float alpha = quarter / 4f;
				Quaternionf expected = new Quaternionf(from).slerp(to, alpha);
				Quaternionf actual = animation.calculateBoneTransformations("root", alpha * 20, PInterpolationType.LINEAR).rotation();
				assertEquals(1, Math.abs(expected.dot(actual)), 0.000001f);
			}
		}
	}
}
