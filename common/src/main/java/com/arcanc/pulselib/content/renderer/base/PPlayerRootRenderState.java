/**
 * @author ArcAnc
 * Created at: 26.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.base;

import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

/**
 * Defensive visual ROOT snapshot captured with the native entity render state.
 */
@ApiStatus.Internal
public interface PPlayerRootRenderState
{
	/**
	 * Returns an independent copy of the visual ROOT, or {@code null} for an empty state.
	 */
	@Nullable
	Matrix4f pulselib$getPlayerRoot();

	/**
	 * Copies the extracted visual ROOT; {@code null} clears a reused or non-player state.
	 */
	void pulselib$setPlayerRoot(@Nullable Matrix4f root);
}
