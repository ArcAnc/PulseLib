/**
 * @author ArcAnc
 * Created at: 13.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.deformer.gpu;

public final class PFrameStreams
{
	private final PPoseStream poses = new PPoseStream();
	private final PDeformerStream deformers = new PDeformerStream();

	public PPoseStream poses()
	{
		return this.poses;
	}

	public PDeformerStream deformers()
	{
		return this.deformers;
	}

	public void finishFrame()
	{
		this.poses.finishFrame();
		this.deformers.finishFrame();
	}

	public void clearDefinitions()
	{
		this.poses.finishFrame();
		this.deformers.clearDefinitions();
	}
}
