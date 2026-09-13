package com.arcanc.pulselib.content.player.animation.firstPerson;

/**
 * Extension point for first-person renderers.  PulseLib's default keeps one
 * canonical skeleton; a future viewmodel may provide its own presentation
 * without changing PPose evaluation.
 */
public enum PFirstPersonSkeletonMode
{
	SAME_SKELETON,
	VIEWMODEL_SKELETON
}
