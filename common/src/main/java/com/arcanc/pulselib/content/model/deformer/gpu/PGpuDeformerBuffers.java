/**
 * @author ArcAnc
 * Created at: 10.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.deformer.gpu;

import com.arcanc.pulselib.content.model.deformer.PMeshDeformation;
import org.jetbrains.annotations.Nullable;

public final class PGpuDeformerBuffers
{
	public static final Submission NONE = new Submission(-1, -1, 0);
	private static final PFrameStreams STREAMS = new PFrameStreams();

	private PGpuDeformerBuffers()
	{
	}

	public static Submission submit(@Nullable PMeshDeformation deformation)
	{
		return STREAMS.deformers().submit(deformation);
	}

	public static void finishFrame()
	{
		STREAMS.finishFrame();
	}

	public static void cleanup()
	{
		STREAMS.clearDefinitions();
	}

	public static void clearDefinitions()
	{
		STREAMS.clearDefinitions();
	}

	/**
	 * DO NOT MUTATE RESULTED LISTS!
	 */
	public static PFrameStreams streams()
	{
		return STREAMS;
	}

	public record Submission(int operationOffset, int valueOffset, int operationCount)
	{
		public boolean enabled()
		{
			return this.operationCount > 0;
		}
	}
}
