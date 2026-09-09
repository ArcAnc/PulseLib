/**
 * @author ArcAnc
 * Created at: 05.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation;


import com.arcanc.pulselib.data.gltf.PGltfModelLoader;
import org.joml.*;

public final class PPlayerAnimationSpace
{
	private static final Matrix4f GLTF_TO_PLAYER = new Matrix4f().
			scaling(-1.0f, -1.0f, 1.0f);
	
	private static final Matrix4f PLAYER_TO_GLTF =
			new Matrix4f(GLTF_TO_PLAYER).
					invert();
	
	private PPlayerAnimationSpace()
	{
	}
	
	public static Vector3f toPlayerSpace(
			Vector3fc vector,
			PPlayerAnimationDefinition definition)
	{
		Vector3f result = new Vector3f(vector);
		
		if (!usesGltfCoordinates(definition))
			return result;
		
		return result.mul(
				-1.0f,
				-1.0f,
				1.0f);
	}
	
	public static Quaternionf toPlayerSpace(
			Quaternionfc rotation,
			PPlayerAnimationDefinition definition)
	{
		Quaternionf result = new Quaternionf(rotation);
		
		if (!usesGltfCoordinates(definition))
			return result;
		
		return result.set(
				-result.x,
				-result.y,
				result.z,
				result.w);
	}
	
	public static Matrix4f toPlayerSpace(
			Matrix4fc matrix,
			PPlayerAnimationDefinition definition)
	{
		if (!usesGltfCoordinates(definition))
			return new Matrix4f(matrix);
		
		return new Matrix4f(GLTF_TO_PLAYER).
				mul(matrix).
				mul(PLAYER_TO_GLTF);
	}
	
	public static Matrix4f toPlayerGeometrySpace(
			Matrix4fc matrix,
			PPlayerAnimationDefinition definition)
	{
		if (!usesGltfCoordinates(definition))
			return new Matrix4f(matrix);
		return new Matrix4f(GLTF_TO_PLAYER).mul(matrix);
	}
	
	static PPlayerBonePose toPlayerSpace(
			PPlayerBonePose pose,
			PPlayerAnimationDefinition definition)
	{
		return new PPlayerBonePose(
				toPlayerSpace(
						pose.translation(),
						definition),
				toPlayerSpace(
						pose.rotation(),
						definition),
				new Vector3f(pose.scale()),
				pose.hasTranslation(),
				pose.hasRotation(),
				pose.hasScale());
	}
	
	private static boolean usesGltfCoordinates(
			PPlayerAnimationDefinition definition)
	{
		return definition.modelData().
				getModelFormat().
				equals(PGltfModelLoader.INSTANCE.id());
	}
}
