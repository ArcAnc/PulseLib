/**
 * @author ArcAnc
 * Created at: 13.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.plan;

import org.joml.Matrix4f;

/**
 * Model-view and projection matrices captured for one render submission scope.
 *
 * @param modelView the captured model-view matrix
 * @param projection the captured projection matrix
 */
public record PRenderFrame(Matrix4f modelView, Matrix4f projection)
{
	/**
	 * Copies model-view and projection matrices for later render-plan execution.
	 *
	 * @param modelView the captured model-view matrix
	 * @param projection the captured projection matrix
	 */
	public PRenderFrame
	{
		modelView = new Matrix4f(modelView);
		projection = new Matrix4f(projection);
	}
}
