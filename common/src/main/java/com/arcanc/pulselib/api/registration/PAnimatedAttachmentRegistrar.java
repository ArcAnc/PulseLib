/**
 * @author ArcAnc
 * Created at: 12.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.api.registration;

import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAnimatedAttachmentRenderer;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAnimatedAttachments;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Collects animated player attachment renderers during a client plugin callback.
 */
public class PAnimatedAttachmentRegistrar extends PulseRegistrar
{
	private final List<Runnable> actions = new ArrayList<>();

	/**
	 * Queues an animated attachment renderer for installation.
	 *
	 * @param renderer the renderer
	 */
	public void register(PPlayerAnimatedAttachmentRenderer renderer)
	{
		checkOpen();
		Objects.requireNonNull(renderer);
		this.actions.add(() -> PPlayerAnimatedAttachments.register(renderer));
	}

	/**
	 * Installs the collected renderers and closes this registration scope.
	 */
	public void apply()
	{
		checkOpen();
		this.actions.forEach(Runnable :: run);
		close();
	}
}
