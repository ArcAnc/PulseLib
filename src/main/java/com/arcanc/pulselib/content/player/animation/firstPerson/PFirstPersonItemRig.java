package com.arcanc.pulselib.content.player.animation.firstPerson;

import com.arcanc.pulselib.content.model.animation.PTransform;
import java.util.Objects;

/** Calibration from the vanilla item render origin to an authored item socket. */
public record PFirstPersonItemRig(PTransform renderOrigin, PTransform itemSocket)
{
	public PFirstPersonItemRig
	{
		renderOrigin = Objects.requireNonNull(renderOrigin);
		itemSocket = Objects.requireNonNull(itemSocket);
	}

	public PTransform originToSocket()
	{
		return this.renderOrigin.inverse().compose(this.itemSocket);
	}
}
