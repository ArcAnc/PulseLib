/**
 * @author ArcAnc
 * Created at: 04.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation;


import com.arcanc.pulselib.util.PLibDatabase;

public final class PPlayerAnimationAnchors
{
	public static final PPlayerAnimationAnchor FIRST_PERSON_CAMERA =
				of("first_person_camera");

	/** Semantic camera-space socket for the right palm. */
	public static final PPlayerAnimationAnchor RIGHT_HAND =
				of("right_hand");

	/** Semantic camera-space socket for the left palm. */
	public static final PPlayerAnimationAnchor LEFT_HAND =
				of("left_hand");
	
	/**
	 * Binds the main-hand item to a camera-space hand-bone attachment point.
	 * First-person definitions normally map this anchor to {@code right_hand}.
	 */
	public static final PPlayerAnimationAnchor RIGHT_ITEM =
			of("right_item");
	
	/**
	 * Binds the off-hand item to a camera-space hand-bone attachment point.
	 * First-person definitions normally map this anchor to {@code left_hand}.
	 */
	public static final PPlayerAnimationAnchor LEFT_ITEM =
			of("left_item");
	
	private static PPlayerAnimationAnchor of(String path)
	{
		return new PPlayerAnimationAnchor(PLibDatabase.rl(path));
	}
}
