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
 * Detected low-level renderer features used to select compatible backend strategies.
 *
 * @param texelBuffer the texel buffer
 * @param shaderStorageBuffer the shader storage buffer
 * @param multiDrawIndirect the multi draw indirect
 * @param baseInstance the base instance
 * @param persistentMapping the persistent mapping
 */
public record PRenderCapabilityMatrix(
		boolean texelBuffer,
		boolean shaderStorageBuffer,
		boolean multiDrawIndirect,
		boolean baseInstance,
		boolean persistentMapping)
{
	/**
	 * Feature matrix with no optional acceleration capabilities.
	 */
	public static final PRenderCapabilityMatrix NONE = new PRenderCapabilityMatrix(false, false, false, false, false);

	/**
	 * Returns whether the context supports the required multi-draw indirect features.
	 *
	 * @return whether the context supports the required multi-draw indirect features
	 */
	public boolean supportsMultiDrawIndirect()
	{
		return this.multiDrawIndirect && this.baseInstance;
	}

	/**
	 * Selects the best available pose storage mechanism.
	 *
	 * @return the best supported pose storage strategy
	 */
	public PRenderCapabilities.StorageMode bestPoseStorage()
	{
		if (this.shaderStorageBuffer)
			return PRenderCapabilities.StorageMode.SSBO;
		return this.texelBuffer ? PRenderCapabilities.StorageMode.TEXEL_BUFFER : PRenderCapabilities.StorageMode.CPU;
	}

	/**
	 * Builds a compatible strategy selection for the requested pose storage and mapping preference.
	 *
	 * @param poseStorage the requested pose storage strategy
	 * @param usePersistentMapping whether persistent mapped upload storage should be selected when supported
	 * @return the compatible backend strategy selection
	 */
	public PRenderCapabilities selectedCapabilities(PRenderCapabilities.StorageMode poseStorage, boolean usePersistentMapping)
	{
		if (poseStorage == PRenderCapabilities.StorageMode.SSBO && !this.shaderStorageBuffer)
			throw new IllegalArgumentException("SSBO pose storage was selected without SSBO support");
		if (poseStorage == PRenderCapabilities.StorageMode.TEXEL_BUFFER && !this.texelBuffer)
			throw new IllegalArgumentException("Texel-buffer pose storage was selected without TBO support");
		if (usePersistentMapping && !this.persistentMapping)
			throw new IllegalArgumentException("Persistent mapping was selected without buffer-storage support");
		return new PRenderCapabilities(
				poseStorage,
				this.supportsMultiDrawIndirect() ? PRenderCapabilities.SubmissionMode.MULTI_DRAW_INDIRECT : PRenderCapabilities.SubmissionMode.DIRECT,
				usePersistentMapping ? PRenderCapabilities.UploadMode.PERSISTENT_RING : PRenderCapabilities.UploadMode.WRITE,
				this);
	}
}
