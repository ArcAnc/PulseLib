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
 * Calibration from the vanilla item render origin to an authored item socket.
 *
 * @param renderOrigin the render origin
 * @param itemSocket the item socket
 */
public record PFirstPersonItemRig(PTransform renderOrigin, PTransform itemSocket)
{
	/**
	 * Captures the reference item origin and authored attachment socket.
	 *
	 * @param renderOrigin the render origin
	 * @param itemSocket the item socket
	 */
	public PFirstPersonItemRig
	{
		renderOrigin = Objects.requireNonNull(renderOrigin);
		itemSocket = Objects.requireNonNull(itemSocket);
	}

	/**
	 * Returns the reference transform from the item render origin to its attachment socket.
	 *
	 * @return the reference transform from the item render origin to its attachment socket
	 */
	public PTransform originToSocket()
	{
		return this.renderOrigin.inverse().compose(this.itemSocket);
	}
}
