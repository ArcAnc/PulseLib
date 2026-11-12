/**
 * @author ArcAnc
 * Created at: 27.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

/**
 * Ordered, sampled rig contributions. Retains no player, manager, definition or model references.
 * Vanilla setup still supplies the base pose at each consumer; canonical blending is replayed once.
 */
@ApiStatus.Internal
public final class PPlayerRigPose
{
	private final List<Contribution> contributions;

	PPlayerRigPose(List<Contribution> contributions)
	{
		this.contributions = List.copyOf(contributions);
	}

	/** Captures through the same evaluator used by the canonical immediate API. */
	public static PPlayerRigPose capture(Player player, float partialTick)
	{
		return PPlayerAnimations.captureRig(player, partialTick);
	}

	/** Applies immutable contributions to the current vanilla pose without resolving a live owner. */
	public void apply(PlayerModel model)
	{
		PPlayerAnimations.applyRig(model, this.contributions);
	}

	/** Whether this extraction has no eligible native rig consumers. */
	public boolean isEmpty()
	{
		return this.contributions.isEmpty();
	}

	/** Armor uses the same sampled commands against its own vanilla base pose. */
	public void apply(HumanoidModel<?> model)
	{
		PPlayerAnimations.applyRig(model, this.contributions);
	}

	record Contribution(PPlayerPart part, PPlayerBonePose pose, PPlayerAnimationBlendMode mode, float weight)
	{
	}
}
