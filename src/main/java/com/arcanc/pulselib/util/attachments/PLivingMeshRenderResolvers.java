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
 * Provides support for living mesh render resolvers.
 */
public final class PLivingMeshRenderResolvers
{
	/**
	 * Creates an instance of the enclosing type.
	 */
	private PLivingMeshRenderResolvers()
	{
	}
	
	/**
	 * Performs the inherited operation.
	 * @return the value produced by this operation.
	 */
	public static PLivingMeshRenderResolver inherited()
	{
		return (entity, stack, bone, mesh, inherited, partialTick) -> inherited;
	}
	
	/**
	 * Performs the default lit operation.
	 * @return the value produced by this operation.
	 */
	public static PLivingMeshRenderResolver defaultLit()
	{
		return inherited();
	}
}
