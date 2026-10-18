/**
 * @author ArcAnc
 * Created at: 23.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation.attachment;

import com.arcanc.pulselib.content.player.animation.PPlayerAnimationAnchor;

/**
 * Renders content attached to a named animated player action anchor.
 */
public interface PPlayerAnimatedAttachmentRenderer
{
	/**
	 * Returns the animation anchor this renderer follows.
	 *
	 * @return the animation anchor this renderer follows
	 */
	PPlayerAnimationAnchor anchor();
	/**
	 * Renders the attachment using the already resolved anchor context.
	 *
	 * @param context the resolved player attachment render inputs
	 */
	void render(PPlayerAnimatedAttachmentContext context);
}
