/**
 * @author ArcAnc
 * Created at: 13.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.plan;

/**
 * Backend-independent indexed draw command, including geometry offsets and instance range.
 *
 * @param indexCount the number of indices to draw
 * @param instanceCount the number of instances to draw
 * @param firstIndex the first index element in the geometry storage
 * @param baseVertex the vertex offset added to each index
 * @param firstInstance the first instance-data record used by the draw
 */
public record PDrawCommand(
		int indexCount,
		int instanceCount,
		int firstIndex,
		int baseVertex,
		int firstInstance)
{
	/**
	 * Creates an indexed draw command, validating counts and non-negative offsets.
	 *
	 * @param indexCount the number of indices to draw
	 * @param instanceCount the number of instances to draw
	 * @param firstIndex the first index element in the geometry storage
	 * @param baseVertex the vertex offset added to each index
	 * @param firstInstance the first instance-data record used by the draw
	 */
	public PDrawCommand
	{
		if (indexCount < 0 || instanceCount < 0 || firstIndex < 0 || firstInstance < 0)
			throw new IllegalArgumentException("Draw command values must be non-negative");
	}

	/**
	 * Returns a command copy targeting the supplied instance range.
	 *
	 * @param instanceCount the number of instances to draw
	 * @param firstInstance the first instance-data record used by the draw
	 * @return a command copy targeting the supplied instance range
	 */
	public PDrawCommand withInstances(int instanceCount, int firstInstance)
	{
		return new PDrawCommand(this.indexCount, instanceCount, this.firstIndex, this.baseVertex, firstInstance);
	}
}
