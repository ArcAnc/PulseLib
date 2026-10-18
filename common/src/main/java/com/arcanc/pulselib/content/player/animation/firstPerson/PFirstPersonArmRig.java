/**
 * @author ArcAnc
 * Created at: 23.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation.firstPerson;

import com.arcanc.pulselib.content.model.animation.PTransform;

import java.util.Objects;
/**
 * Calibration from the vanilla rigid-arm render origin to an authored hand socket.
 *
 * @param armOrigin the arm origin
 * @param handSocket the hand socket
 */
public record PFirstPersonArmRig(PTransform armOrigin, PTransform handSocket)
{
	/**
	 * Captures the reference arm origin and authored hand socket.
	 *
	 * @param armOrigin the arm origin
	 * @param handSocket the hand socket
	 */
	public PFirstPersonArmRig
	{
		armOrigin = Objects.requireNonNull(armOrigin);
		handSocket = Objects.requireNonNull(handSocket);
	}

	/**
	 * Returns the reference transform from the arm render origin to the hand socket.
	 *
	 * @return the reference transform from the arm render origin to the hand socket
	 */
	public PTransform armToHand()
	{
		return this.armOrigin.inverse().compose(this.handSocket);
	}
}
