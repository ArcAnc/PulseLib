package com.arcanc.pulselib.content.player.animation.firstPerson;

import com.arcanc.pulselib.content.model.animation.PTransform;
import java.util.Objects;

/** Calibration from the vanilla rigid-arm render origin to an authored hand socket. */
public record PFirstPersonArmRig(PTransform armOrigin, PTransform handSocket)
{
	public PFirstPersonArmRig
	{
		armOrigin = Objects.requireNonNull(armOrigin);
		handSocket = Objects.requireNonNull(handSocket);
	}

	public PTransform armToHand()
	{
		return this.armOrigin.inverse().compose(this.handSocket);
	}
}
