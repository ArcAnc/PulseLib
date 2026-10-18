/**
 * @author ArcAnc
 * Created at: 12.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.api.plugin;

import com.arcanc.pulselib.api.registration.PAnimatedAttachmentRegistrar;
import com.arcanc.pulselib.api.registration.PAttachmentRegistrar;
import com.arcanc.pulselib.api.registration.PModelRegistrar;
import com.arcanc.pulselib.api.registration.PPlayerAnimationRegistrar;

/**
 * Physical-client plugin. Model declarations repeat on reload; other methods run once.
 */
public interface IPulseClientPlugin extends IPulseCommonPlugin
{
	/**
	 * Declares models and texture overrides to load during client resource reloads.
	 *
	 * @param registrar the active registration scope for this callback
	 */
	default void registerModels(PModelRegistrar registrar)
	{
	}

	/**
	 * Registers item-specific and global attachments for living entity rendering.
	 *
	 * @param registrar the active registration scope for this callback
	 */
	default void registerAttachments(PAttachmentRegistrar registrar)
	{
	}

	/**
	 * Registers named player animation definitions.
	 *
	 * @param registrar the active registration scope for this callback
	 */
	default void registerPlayerAnimations(PPlayerAnimationRegistrar registrar)
	{
	}

	/**
	 * Registers renderers for attachments driven by player animation bones.
	 *
	 * @param registrar the active registration scope for this callback
	 */
	default void registerAnimatedAttachments(PAnimatedAttachmentRegistrar registrar)
	{
	}
}
