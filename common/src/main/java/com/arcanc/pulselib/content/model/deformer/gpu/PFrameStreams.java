/**
 * @author ArcAnc
 * Created at: 13.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.deformer.gpu;

/**
 * Groups the pose and deformer data streams consumed during one render frame.
 */
public final class PFrameStreams
{
	private final PPoseStream poses = new PPoseStream();
	private final PDeformerStream deformers = new PDeformerStream();

	/**
	 * Returns the poses.
	 *
	 * @return the poses
	 */
	public PPoseStream poses()
	{
		return this.poses;
	}

	/**
	 * Returns the deformers.
	 *
	 * @return the deformers
	 */
	public PDeformerStream deformers()
	{
		return this.deformers;
	}

	/**
	 * Resets frame-local pose and deformation data after rendering completes.
	 */
	public void finishFrame()
	{
		this.poses.finishFrame();
		this.deformers.finishFrame();
	}

	/**
	 * Clears cached deformation definitions and resets frame streams.
	 */
	public void clearDefinitions()
	{
		this.poses.finishFrame();
		this.deformers.clearDefinitions();
	}
}
