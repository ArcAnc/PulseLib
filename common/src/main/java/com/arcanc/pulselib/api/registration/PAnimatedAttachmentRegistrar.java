package com.arcanc.pulselib.api.registration;

import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAnimatedAttachmentRenderer;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAnimatedAttachments;

import java.util.ArrayList;
import java.util.List;

public class PAnimatedAttachmentRegistrar extends PulseRegistrar
{
	private final List<Runnable> actions = new ArrayList<>();

	public void register(PPlayerAnimatedAttachmentRenderer renderer)
	{
		checkOpen();
		java.util.Objects.requireNonNull(renderer);
		this.actions.add(() -> PPlayerAnimatedAttachments.register(renderer));
	}

	public void apply()
	{
		checkOpen();
		this.actions.forEach(Runnable::run);
		close();
	}
}
