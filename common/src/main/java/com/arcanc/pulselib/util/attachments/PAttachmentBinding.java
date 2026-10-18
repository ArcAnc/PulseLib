/**
 * @author ArcAnc
 * Created at: 07.07.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.util.attachments;


/**
 * Binds an attachment-model bone to a vanilla entity model anchor with a local transform.
 */
public interface PAttachmentBinding
{
	/**
	 * Returns the vanilla model anchor followed by this binding.
	 *
	 * @return the vanilla model anchor followed by this binding
	 */
	PAttachmentAnchor anchor();
	
	/**
	 * Returns the name of the attachment-model bone to submit.
	 *
	 * @return the name of the attachment-model bone to submit
	 */
	String bone();
	
	/**
	 * Returns the additional local transform applied at the anchor.
	 *
	 * @return the additional local transform applied at the anchor
	 */
	PTransform transform();
}
