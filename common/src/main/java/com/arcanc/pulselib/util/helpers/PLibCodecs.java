/**
 * @author ArcAnc
 * Created at: 05.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.util.helpers;


import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

/**
 * Codecs for vector and quaternion values used by serialized animation and deformation definitions.
 */
public class PLibCodecs
{
	/**
	 * Codec for three-component float vectors.
	 */
	public static final Codec<Vector3f> VECTOR3F_CODEC = fixedSizeFloatListCodec(3).xmap(
			values -> new Vector3f(values.getFirst(), values.get(1), values.get(2)),
			value -> List.of(value.x(), value.y(), value.z()));
	/**
	 * Codec for quaternion rotation values.
	 */
	public static final Codec<Quaternionf> QUATERNION_CODEC = fixedSizeFloatListCodec(4).xmap(
			values -> new Quaternionf(values.getFirst(), values.get(1), values.get(2), values.get(3)),
			value -> List.of(value.x(), value.y(), value.z(), value.w()));
	
	private static Codec<List<Float>> fixedSizeFloatListCodec(int size)
	{
		return Codec.FLOAT.listOf().comapFlatMap(values -> values.size() == size ?
						DataResult.success(values) :
						DataResult.error(() -> "Expected " + size + " floats, got " + values.size()),
				values -> values);
	}
}
