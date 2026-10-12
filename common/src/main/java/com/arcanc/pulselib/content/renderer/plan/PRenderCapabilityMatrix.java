/**
 * @author ArcAnc
 * Created at: 13.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.plan;

public record PRenderCapabilityMatrix(
		boolean texelBuffer,
		boolean shaderStorageBuffer,
		boolean multiDrawIndirect,
		boolean baseInstance,
		boolean persistentMapping)
{
	public static final PRenderCapabilityMatrix NONE = new PRenderCapabilityMatrix(false, false, false, false, false);

	public boolean supportsMultiDrawIndirect()
	{
		return this.multiDrawIndirect && this.baseInstance;
	}

	public PRenderCapabilities.StorageMode bestPoseStorage()
	{
		if (this.shaderStorageBuffer)
			return PRenderCapabilities.StorageMode.SSBO;
		return this.texelBuffer ? PRenderCapabilities.StorageMode.TEXEL_BUFFER : PRenderCapabilities.StorageMode.CPU;
	}

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
