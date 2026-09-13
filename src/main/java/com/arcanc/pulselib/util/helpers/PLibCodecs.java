/**
 * @author ArcAnc
 * Created at: 05.04.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.util.helpers;


import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;
import java.util.List;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class PLibCodecs
{
	public static final Codec<UUID> UUID_CODEC = UUIDUtil.CODEC;
	public static final MapCodec<UUID> UUID_MAP_CODEC = UUID_CODEC.fieldOf("uuid");
	public static final StreamCodec<ByteBuf, UUID> STREAM_CODEC = UUIDUtil.STREAM_CODEC;
	public static final Codec<Vector3f> VECTOR3F_CODEC = fixedSizeFloatListCodec(3).xmap(
			values -> new Vector3f(values.getFirst(), values.get(1), values.get(2)),
			value -> List.of(value.x(), value.y(), value.z()));
	public static final Codec<Quaternionf> QUATERNION_CODEC = fixedSizeFloatListCodec(4).xmap(
			values -> new Quaternionf(values.getFirst(), values.get(1), values.get(2), values.get(3)),
			value -> List.of(value.x(), value.y(), value.z(), value.w()));

	/**
	 * Performs the fixed size float list codec operation.
	 * @param size the size to use.
	 * @return the value produced by this operation.
	 */
	private static Codec<List<Float>> fixedSizeFloatListCodec(int size)
	{
		return Codec.FLOAT.listOf().comapFlatMap(values -> values.size() == size ?
						DataResult.success(values) :
						DataResult.error(() -> "Expected " + size + " floats, got " + values.size()),
				values -> values);
	}
}
