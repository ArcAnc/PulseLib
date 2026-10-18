/**
 * @author ArcAnc
 * Created at: 07.07.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.util.attachments;


/**
 * Common render-context policies for living attachment meshes.
 */
public final class PLivingMeshRenderResolvers
{
	private PLivingMeshRenderResolvers()
	{
	}
	
	/**
	 * Returns a resolver that preserves inherited mesh render inputs.
	 *
	 * @return a resolver that preserves inherited mesh render inputs
	 */
	public static PLivingMeshRenderResolver inherited()
	{
		return (entity, stack, bone, mesh, inherited, partialTick) -> inherited;
	}
	
	/**
	 * Returns a resolver that selects ordinary lit attachment rendering.
	 *
	 * @return a resolver that selects ordinary lit attachment rendering
	 */
	public static PLivingMeshRenderResolver defaultLit()
	{
		return inherited();
	}
}
