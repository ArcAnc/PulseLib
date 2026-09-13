/**
 * @author ArcAnc
 * Created at: 05.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation;


import com.arcanc.pulselib.content.model.animation.PTransform;
import com.arcanc.pulselib.data.gltf.PGltfModelLoader;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class PPlayerAnimationSpace
{
	/** Converts glTF coordinate values to Minecraft's player-model coordinates. */
	private static final Quaternionf GLTF_TO_PLAYER_ROTATION =
			new Quaternionf().rotationZ((float)Math.PI);
	/**
	 * Vertex space used by the baked first-person mesh pass. GLTF mesh vertices
	 * remain in their source basis. Bone and locator matrices are instead
	 * converted once by {@code PFirstPersonPresentation}'s MODEL-to-VIEW root.
	 */
	private static final PTransform PLAYER_MODEL_TO_FIRST_PERSON =
			PTransform.rotation(GLTF_TO_PLAYER_ROTATION);

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
	public static PTransform toPlayerSpace(
			PTransform transform,
			PPlayerAnimationDefinition definition)
	{
		if (!usesGltfCoordinates(definition))
			return transform;
		return new PTransform(
				toPlayerSpace(transform.translation(), definition),
				toPlayerSpace(transform.rotation(), definition),
				transform.scale());
	}
	
	public static PTransform toPlayerGeometrySpace(
			PTransform transform,
			PPlayerAnimationDefinition definition)
	{
		if (!usesGltfCoordinates(definition))
			return transform;
		return PLAYER_MODEL_TO_FIRST_PERSON.compose(transform);
	}
	
	/**
	 * Converts a camera-relative transform for a mesh attached to the animated
	 * model. Unlike vanilla arms and items, glTF mesh vertices already use the
	 * glTF source basis, so a glTF transform reduces to {@code M} here.
	 */
	public static PTransform toFirstPersonGeometrySpace(
			PTransform transform,
			PPlayerAnimationDefinition definition)
	{
		return PLAYER_MODEL_TO_FIRST_PERSON.compose(toPlayerGeometrySpace(transform, definition));
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
