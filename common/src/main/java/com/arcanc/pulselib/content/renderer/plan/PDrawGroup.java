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

/**
 * Compatible render pipeline, geometry, draw command and instance data grouped for execution.
 *
 * @param pipeline the pipeline
 * @param mesh the mesh
 * @param command the command
 * @param writeDepth whether this draw group writes depth
 * @param instances the instances
 */
public record PDrawGroup(
		PPipelineHandle pipeline,
		PMeshHandle mesh,
		PDrawCommand command,
		boolean writeDepth,
		List<PInstanceHeader> instances)
{
	/**
	 * Copies the instances belonging to one compatible pipeline and geometry draw.
	 *
	 * @param pipeline the pipeline
	 * @param mesh the mesh
	 * @param command the command
	 * @param writeDepth whether this draw group writes depth
	 * @param instances the instances
	 */
	public PDrawGroup
	{
		if (command.instanceCount() != instances.size())
			throw new IllegalArgumentException("Draw command instance count must match its instance stream");
		instances = List.copyOf(instances);
	}

	/**
	 * Returns a draw-group copy using the supplied resolved draw command.
	 *
	 * @param command the command
	 * @return a draw-group copy using the supplied resolved draw command
	 */
	public PDrawGroup withCommand(PDrawCommand command)
	{
		return new PDrawGroup(this.pipeline, this.mesh, command, this.writeDepth, this.instances);
	}
}
