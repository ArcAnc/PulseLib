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
 * Backend contract for executing render plans and releasing backend-owned resources.
 */
public interface PRenderExecutor
{
	/**
	 * Returns the strategies selected by this rendering backend.
	 *
	 * @return the strategies selected by this rendering backend
	 */
	PRenderCapabilities capabilities();

	/**
	 * Executes a compiled render plan using the supplied frame matrices.
	 *
	 * @param plan the plan
	 * @param frame the frame matrices or sampled animation values used by this operation
	 */
	void execute(PRenderPlan plan, PRenderFrame frame);

	/**
	 * Releases resources owned by this rendering backend.
	 */
	void cleanup();
}
