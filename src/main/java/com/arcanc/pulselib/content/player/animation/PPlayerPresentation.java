package com.arcanc.pulselib.content.player.animation;

import org.joml.Matrix4f;

/**
 * A render-space view of an already resolved canonical player skeleton.
 * Implementations only compose a PRESENTATION root with MODEL-space bones;
 * they never evaluate or convert animation channels.
 */
public interface PPlayerPresentation
{
	PBoneRenderMask renderMask();

	/**
	 * Writes {@code M_presentation * M_bone_model} into {@code destination}.
	 * The result is WORLD-space for third person or VIEW-space for first person.
	 */
	boolean boneMatrix(PPlayerAnimationFrame frame, String boneName, Matrix4f destination);
}
