/**
 * @author ArcAnc
 * Created at: 09.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation;

import com.arcanc.pulselib.content.model.deformer.PDeformerStack;

import java.util.Objects;

/**
 * Player model part, prepared mesh deformer stack and animation-aware channel value provider.
 *
 * @param part the player model part receiving the contribution
 * @param stack the item stack supplying render or animation state
 * @param values the values
 */
public record PPlayerAnimationDeformer(
		PPlayerPart part,
		PDeformerStack stack,
		PPlayerAnimationDeformerValueSource values)
{
	/**
	 * Binds an animation-aware deformation stack to a player model part.
	 *
	 * @param part the player model part receiving the contribution
	 * @param stack the item stack supplying render or animation state
	 * @param values the values
	 */
	public PPlayerAnimationDeformer
	{
		Objects.requireNonNull(part);
		Objects.requireNonNull(stack);
		Objects.requireNonNull(values);
	}
}
