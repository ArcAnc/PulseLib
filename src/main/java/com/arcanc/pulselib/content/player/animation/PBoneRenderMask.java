package com.arcanc.pulselib.content.player.animation;

import java.util.Objects;
import java.util.Set;

/**
 * A presentation-only selection of MODEL-space bones.  It never changes pose
 * evaluation: hidden ancestors still participate in skeleton resolution.
 */
public final class PBoneRenderMask
{
	public static final PBoneRenderMask ALL = new PBoneRenderMask(null);

	private final Set<String> includedBones;

	private PBoneRenderMask(Set<String> includedBones)
	{
		this.includedBones = includedBones;
	}

	public static PBoneRenderMask only(Set<String> boneNames)
	{
		Objects.requireNonNull(boneNames);
		return new PBoneRenderMask(Set.copyOf(boneNames));
	}

	public boolean renders(String boneName)
	{
		return this.includedBones == null || this.includedBones.contains(boneName);
	}
}
