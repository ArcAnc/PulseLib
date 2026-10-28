/**
 * @author ArcAnc
 * Created at: 28.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.deformer;

import com.arcanc.pulselib.content.player.deformer.PPlayerModelGeometry.Entry;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

/** Immutable extraction data plus an invocation-local armor entry, copied into each native node. */
@ApiStatus.Internal
public interface PPlayerGeometryRenderState
{
	@Nullable
	PPlayerModelGeometry pulselib$getPlayerGeometry();

	void pulselib$setPlayerGeometry(@Nullable PPlayerModelGeometry geometry);

	@Nullable
	Entry pulselib$getSubmittingGeometry();

	void pulselib$setSubmittingGeometry(@Nullable Entry entry);
}
