/**
 * @author ArcAnc
 * Created at: 07.07.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.baked;


/**
 * Allows a renderer to override inherited render inputs for an individual baked mesh.
 */
@FunctionalInterface
public interface PMeshRenderResolver
{
	/**
	 * Resolves the render context to use for one mesh of the supplied bone.
	 *
	 * @param bone the baked bone being sampled or submitted
	 * @param mesh the baked mesh geometry and material
	 * @param inherited the render inputs inherited from the parent bone or renderer
	 * @return the inherited or overridden mesh render inputs
	 */
	PMeshRenderContext resolve(PBakedBone bone, PBakedMesh mesh, PMeshRenderContext inherited);
}
