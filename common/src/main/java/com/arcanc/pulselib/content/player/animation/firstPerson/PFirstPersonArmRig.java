package com.arcanc.pulselib.content.player.animation.firstPerson;
import com.arcanc.pulselib.content.model.animation.PTransform;

import java.util.Objects;
/** Calibration from the vanilla rigid-arm render origin to an authored hand socket. */
public record PFirstPersonArmRig(PTransform armOrigin, PTransform handSocket)
{
	/**
	 * Creates an instance of the enclosing type.
	 * @param armOrigin the arm origin to use.
	 * @param handSocket the hand socket to use.
	 */
	public PFirstPersonArmRig
	{
		armOrigin = Objects.requireNonNull(armOrigin);
		handSocket = Objects.requireNonNull(handSocket);
	}

	/**
	 * Performs the arm to hand operation.
	 * @return the value produced by this operation.
	 */
	public PTransform armToHand()
	{
		return this.armOrigin.inverse().compose(this.handSocket);
	}
}
