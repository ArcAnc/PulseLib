/**
 * @author ArcAnc
 * Created at: 09.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation.attachment;

import com.arcanc.pulselib.content.player.animation.PPlayerAnimationAnchor;

/**
 * Defines the contract for player animated attachment renderer.
 */
public interface PPlayerAnimatedAttachmentRenderer
{
	/**
	 * Performs the anchor operation.
	 * @return the value produced by this operation.
	 */
	PPlayerAnimationAnchor anchor();

	/**
	 * Performs the render operation.
	 * @param context the context to use.
	 */
	void render(PPlayerAnimatedAttachmentContext context);
}
