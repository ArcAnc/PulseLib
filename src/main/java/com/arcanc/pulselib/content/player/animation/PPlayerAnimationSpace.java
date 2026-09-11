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
	/** Converts glTF coordinate values to Minecraft's player-model coordinates. */
	private static final Matrix4f GLTF_TO_PLAYER = new Matrix4f().
			scaling(-1.0f, -1.0f, 1.0f);
	
	private static final Matrix4f PLAYER_TO_GLTF =
			new Matrix4f(GLTF_TO_PLAYER).
					invert();

	/**
	 * Converts a vanilla player-model local coordinate into the coordinate
	 * convention used by the first-person replacement renderer.
	 *
	 * <p>The matrix currently has the same values as {@link #GLTF_TO_PLAYER},
	 * because that conversion is its own inverse. The two constants express
	 * different contracts, however: this one is a player-model-to-view
	 * conversion and must be derived from the first-person renderer if its
	 * coordinate convention changes.</p>
	 */
	private static final Matrix4f PLAYER_MODEL_TO_FIRST_PERSON =
			new Matrix4f().
					scaling(-1.0f, -1.0f, 1.0f);
	
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
	
	/**
	 * Changes both the source and destination bases of a model transform to
	 * Minecraft player-model space. For glTF, this is {@code C * M * C^-1}.
	 */
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
	
	/**
	 * Converts a camera-relative bone or item transform for the first-person
	 * replacement renderer. The returned transform maps vanilla player-model
	 * local coordinates into first-person camera coordinates.
	 *
	 * <p>For glTF input this becomes {@code M * C}: {@code toPlayerSpace}
	 * first converts the transform into player-model space, then
	 * {@link #PLAYER_MODEL_TO_FIRST_PERSON} converts its output into view
	 * space. Consequently an identity bone transform returns {@code C}; that
	 * is required to orient vanilla arms and items, whose source coordinates
	 * are still player-model coordinates.</p>
	 */
	public static Matrix4f toFirstPersonSpace(
			Matrix4fc matrix,
			PPlayerAnimationDefinition definition)
	{
		return new Matrix4f(PLAYER_MODEL_TO_FIRST_PERSON).
				mul(toPlayerSpace(matrix, definition));
	}

	/**
	 * Converts a camera-relative transform for a mesh attached to the animated
	 * model. Unlike vanilla arms and items, glTF mesh vertices already use the
	 * glTF source basis, so a glTF transform reduces to {@code M} here.
	 */
	public static Matrix4f toFirstPersonGeometrySpace(
			Matrix4fc matrix,
			PPlayerAnimationDefinition definition)
	{
		return new Matrix4f(PLAYER_MODEL_TO_FIRST_PERSON).
				mul(toPlayerGeometrySpace(matrix, definition));
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
