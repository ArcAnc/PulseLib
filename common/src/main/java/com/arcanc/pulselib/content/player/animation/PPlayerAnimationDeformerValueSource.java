/**
 * @author ArcAnc
 * Created at: 09.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation;

import com.arcanc.pulselib.content.model.deformer.PChannelReference;

@FunctionalInterface
public interface PPlayerAnimationDeformerValueSource
{
	float resolve(PPlayerAnimationDeformerContext context, PChannelReference<Float> reference);
}
