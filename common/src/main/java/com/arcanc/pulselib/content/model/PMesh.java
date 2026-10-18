/**
 * @author ArcAnc
 * Created at: 26.01.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model;


import java.util.List;
import java.util.UUID;

/**
 * Source-model mesh identified by UUID and composed of indexed primitives.
 *
 * @param uuid the stable source-model bone identity
 * @param primitives the primitives
 */
public record PMesh(
			UUID uuid,
			List<PMeshPrimitive> primitives
)
{

}
