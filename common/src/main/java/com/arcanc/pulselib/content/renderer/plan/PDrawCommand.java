/**
 * @author ArcAnc
 * Created at: 13.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.plan;

public record PDrawCommand(
		int indexCount,
		int instanceCount,
		int firstIndex,
		int baseVertex,
		int firstInstance)
{
	public PDrawCommand
	{
		if (indexCount < 0 || instanceCount < 0 || firstIndex < 0 || firstInstance < 0)
			throw new IllegalArgumentException("Draw command values must be non-negative");
	}

	public PDrawCommand withInstances(int instanceCount, int firstInstance)
	{
		return new PDrawCommand(this.indexCount, instanceCount, this.firstIndex, this.baseVertex, firstInstance);
	}
}
