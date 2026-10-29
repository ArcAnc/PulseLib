/**
 * @author ArcAnc
 * Created at: 29.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.plan;

/** Transparency policy for a submission; the render type still supplies its shaders and material state. */
public enum PTransparencyMode
{
	/** Sort and blend translucent instances in the ordinary world alpha pass. */
	ORDINARY,
	/** Accumulate compatible translucent instances with order-independent transparency. */
	OIT
}
