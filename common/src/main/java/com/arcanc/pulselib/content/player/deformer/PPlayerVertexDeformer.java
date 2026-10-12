/**
 * @author ArcAnc
 * Created at: 08.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.deformer;

import org.joml.Vector3f;

@FunctionalInterface
interface PPlayerVertexDeformer
{
	PPlayerVertexDeformer IDENTITY = position -> {};

	void deform(Vector3f position);
}
