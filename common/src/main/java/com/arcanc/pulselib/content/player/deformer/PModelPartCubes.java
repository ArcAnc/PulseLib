/**
 * @author ArcAnc
 * Created at: 08.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.deformer;

import net.minecraft.client.model.geom.ModelPart;

import java.util.List;

/**
 * Mixin-accessible view of the cubes belonging to a vanilla model part.
 */
public interface PModelPartCubes
{
	/**
	 * Returns the model part's cubes for player mesh deformation.
	 *
	 * @return the model part's cubes for player mesh deformation
	 */
	List<ModelPart.Cube> pulselib$cubes();
}
