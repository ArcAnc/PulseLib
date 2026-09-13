package com.arcanc.pulselib.content.player.animation;

import org.joml.Matrix4f;

/**
 * MODEL-space player skeleton presented below Minecraft's normal WORLD render
 * root.  The entity renderer supplies that root through its PoseStack.
 */
public final class PThirdPersonPresentation implements PPlayerPresentation
{
	public static final PThirdPersonPresentation INSTANCE = new PThirdPersonPresentation();

	private PThirdPersonPresentation()
	{
	}

	@Override
	public PBoneRenderMask renderMask()
	{
		return PBoneRenderMask.ALL;
	}

	@Override
	public boolean boneMatrix(PPlayerAnimationFrame frame, String boneName, Matrix4f destination)
	{
		return frame.modelMatrix(boneName, destination);
	}
}
