/**
 * @author ArcAnc
 * Created at: 05.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;

/**
 * Timed incoming and outgoing channel values with the interpolation policy for a keyframe segment.
 *
 * @param <T> the sampled animation channel value type
 * @param time the animation position in game ticks
 * @param pre the pre
 * @param post the post
 * @param interpolation the keyframe interpolation policy
 */
public record PKeyframe<T>(float time,
                            PAnimationValue<T> pre,
                            PAnimationValue<T> post,
                            PInterpolation interpolation)
{
}
