package com.arcanc.pulselib.content.player.animation.attachment;

import com.arcanc.pulselib.content.player.animation.PPlayerAnimationAnchor;

public interface PPlayerAnimatedAttachmentRenderer
{
	PPlayerAnimationAnchor anchor();
	void render(PPlayerAnimatedAttachmentContext context);
}
