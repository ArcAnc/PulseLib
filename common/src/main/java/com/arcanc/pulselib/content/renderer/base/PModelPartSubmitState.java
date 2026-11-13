/**
 * @author ArcAnc
 * Created at: 27.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.base;

import org.jetbrains.annotations.ApiStatus;

/** Frozen pose on a native Model.Simple submission. Other unified model nodes are not ModelPart consumers. */
@ApiStatus.Internal
public interface PModelPartSubmitState
{
	PModelPartSnapshot pulselib$getModelPartPose();
}
