/**
 * @author ArcAnc
 * Created at: 23.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation;

import com.arcanc.pulselib.util.PLibDatabase;

/** Standard semantic sockets exposed by player animations. */
public final class PPlayerAnimationAnchors
{
	public static final PPlayerAnimationAnchor FIRST_PERSON_CAMERA = of("first_person_camera");
	public static final PPlayerAnimationAnchor RIGHT_HAND = of("right_hand");
	public static final PPlayerAnimationAnchor LEFT_HAND = of("left_hand");
	public static final PPlayerAnimationAnchor RIGHT_ITEM = of("right_item");
	public static final PPlayerAnimationAnchor LEFT_ITEM = of("left_item");

	private PPlayerAnimationAnchors()
	{
	}

	private static PPlayerAnimationAnchor of(String path)
	{
		return new PPlayerAnimationAnchor(PLibDatabase.rl(path));
	}
}
