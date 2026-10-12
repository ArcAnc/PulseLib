/**
 * @author ArcAnc
 * Created at: 13.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.plan;

import java.util.List;

public record PDrawGroup(
		PPipelineHandle pipeline,
		PMeshHandle mesh,
		PDrawCommand command,
		boolean writeDepth,
		List<PInstanceHeader> instances)
{
	public PDrawGroup
	{
		if (command.instanceCount() != instances.size())
			throw new IllegalArgumentException("Draw command instance count must match its instance stream");
		instances = List.copyOf(instances);
	}

	public PDrawGroup withCommand(PDrawCommand command)
	{
		return new PDrawGroup(this.pipeline, this.mesh, command, this.writeDepth, this.instances);
	}
}
