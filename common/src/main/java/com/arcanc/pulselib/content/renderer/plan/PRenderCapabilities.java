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
 * Selected pose storage, draw submission and streamed-upload strategy of a render backend.
 *
 * @param storageMode the selected pose storage strategy
 * @param submissionMode the selected draw-command submission strategy
 * @param uploadMode the selected streamed-data upload strategy
 * @param matrix the matrix
 */
public record PRenderCapabilities(StorageMode storageMode,
							  SubmissionMode submissionMode,
							  UploadMode uploadMode,
							  PRenderCapabilityMatrix matrix)
{
	/**
	 * Selects compatible storage, submission and upload strategies for a render backend.
	 *
	 * @param storageMode the selected pose storage strategy
	 * @param submissionMode the selected draw-command submission strategy
	 * @param uploadMode the selected streamed-data upload strategy
	 */
	public PRenderCapabilities(StorageMode storageMode, SubmissionMode submissionMode, UploadMode uploadMode)
	{
		this(storageMode, submissionMode, uploadMode, PRenderCapabilityMatrix.NONE);
	}
	/**
	 * GPU storage mechanism selected for pose and deformation data.
	 */
	public enum StorageMode
	{
		/**
		 * Stores shader data in shader storage buffer objects.
		 */
		SSBO,
		/**
		 * Stores shader data in texture buffer objects.
		 */
		TEXEL_BUFFER,
		/**
		 * Uses CPU-side data preparation without shader pose storage.
		 */
		CPU
	}

	/**
	 * Draw-command submission strategy supported by a render backend.
	 */
	public enum SubmissionMode
	{
		/**
		 * Submits compatible draw commands in one multi-draw indirect call.
		 */
		MULTI_DRAW_INDIRECT,
		/**
		 * Submits draw commands from indirect command storage.
		 */
		INDIRECT,
		/**
		 * Submits indexed draws directly.
		 */
		DIRECT
	}

	/**
	 * Strategy used to upload streamed instance and command data.
	 */
	public enum UploadMode
	{
		/**
		 * Writes into persistently mapped, fence-protected ring storage.
		 */
		PERSISTENT_RING,
		/**
		 * Uploads through mapped rotating storage.
		 */
		MAPPED_RING,
		/**
		 * Uploads through conventional buffer writes.
		 */
		WRITE
	}
}
