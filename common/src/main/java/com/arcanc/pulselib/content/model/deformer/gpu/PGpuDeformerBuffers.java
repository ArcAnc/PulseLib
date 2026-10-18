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

/**
 * Shared entry point for packing GPU deformation submissions and resetting their frame lifetime.
 */
public final class PGpuDeformerBuffers
{
	/**
	 * Submission that disables GPU deformation for an instance.
	 */
	public static final Submission NONE = new Submission(-1, -1, 0);
	private static final PFrameStreams STREAMS = new PFrameStreams();

	private PGpuDeformerBuffers()
	{
	}

	/**
	 * Packs the supplied mesh deformation and returns its shader stream offsets.
	 *
	 * @param deformation the prepared mesh deformation and its current sampled values; may be {@code null}
	 * @return the operation and value offsets for this deformation submission
	 */
	public static Submission submit(@Nullable PMeshDeformation deformation)
	{
		return STREAMS.deformers().submit(deformation);
	}

	/**
	 * Resets frame-local stream values after all consumers have rendered.
	 */
	public static void finishFrame()
	{
		STREAMS.finishFrame();
	}

	/**
	 * Releases GPU deformation storage and clears associated stream data.
	 */
	public static void cleanup()
	{
		STREAMS.clearDefinitions();
	}

	/**
	 * Invalidates cached GPU deformation definitions.
	 */
	public static void clearDefinitions()
	{
		STREAMS.clearDefinitions();
	}

	/**
	 * DO NOT MUTATE RESULTED LISTS!
	 *
	 * @return the shared pose and deformer frame streams
	 */
	public static PFrameStreams streams()
	{
		return STREAMS;
	}

	/**
	 * Offsets and operation count used to select GPU deformation data for one draw instance.
	 *
	 * @param operationOffset the operation offset
	 * @param valueOffset the value offset
	 * @param operationCount the operation count
	 */
	public record Submission(int operationOffset, int valueOffset, int operationCount)
	{
		/**
		 * Returns whether this submission contains GPU deformation operations.
		 *
		 * @return whether this submission contains GPU deformation operations
		 */
		public boolean enabled()
		{
			return this.operationCount > 0;
		}
	}
}
