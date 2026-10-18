/**
 * @author ArcAnc
 * Created at: 23.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation.firstPerson;

import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
/** Render-thread scopes; nested calls are supported and never use a global hand field. */
public final class PFirstPersonRenderContexts
{
	private static final ThreadLocal<Deque<PFirstPersonRenderContext>> CONTEXTS =
			ThreadLocal.withInitial(ArrayDeque::new);
	private static final ThreadLocal<Deque<Optional<PFirstPersonRenderPresentation>>> PASSES =
			ThreadLocal.withInitial(ArrayDeque::new);

	/**
	 * Creates a first person render contexts.
	 */
	private PFirstPersonRenderContexts()
	{
	}

	/**
	 * Samples and stores the first-person presentation for one complete hand render pass.
	 *
	 * @param player the player whose animation or presentation is being processed
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 */
	public static void beginPass(LocalPlayer player, float partialTick)
	{
		PASSES.get().push(Optional.ofNullable(PPlayerAnimations.firstPersonPresentation(player, partialTick)));
	}

	/**
	 * Clears hand contexts and the sampled first-person pass presentation.
	 */
	public static void endPass()
	{
		Deque<Optional<PFirstPersonRenderPresentation>> passes = PASSES.get();
		if (!passes.isEmpty())
			passes.pop();
		if (passes.isEmpty())
		{
			PASSES.remove();
			CONTEXTS.remove();
		}
	}

	/**
	 * Returns the first-person presentation captured for the active hand render pass.
	 *
	 * @return the first-person presentation captured for the active hand render pass, or {@code null} when no frame presentation has been captured
	 */
	public static @Nullable PFirstPersonRenderPresentation passPresentation()
	{
		Deque<Optional<PFirstPersonRenderPresentation>> passes = PASSES.get();
		return passes.isEmpty() ? null : passes.peek().orElse(null);
	}

	/**
	 * Captures the current hand-pass basis and pushes a scoped hand render context.
	 *
	 * @param hand the interaction hand being rendered
	 * @param arm the humanoid arm whose geometry is being processed
	 * @param stack the item stack supplying render or animation state
	 * @param poseStack the current model-to-render pose stack
	 */
	public static void push(InteractionHand hand,
	                        HumanoidArm arm,
	                        ItemStack stack,
	                        PoseStack poseStack)
	{
		PFirstPersonRenderPresentation pose = passPresentation();
		if (pose == null)
			return;
		CONTEXTS.get().push(new PFirstPersonRenderContext(hand, arm,
				poseStack.last().pose(), poseStack.last().normal(), pose, stack.getItem() instanceof MapItem));
	}

	/**
	 * Restores the preceding first-person hand render context.
	 */
	public static void pop()
	{
		Deque<PFirstPersonRenderContext> contexts = CONTEXTS.get();
		if (!contexts.isEmpty())
			contexts.pop();
		if (contexts.isEmpty())
			CONTEXTS.remove();
	}

	/**
	 * Returns the active first-person hand render context, or {@code null} outside a hand scope.
	 *
	 * @return the active first-person hand render context, or {@code null} outside a hand scope
	 */
	public static @Nullable PFirstPersonRenderContext current()
	{
		Deque<PFirstPersonRenderContext> contexts = CONTEXTS.get();
		return contexts.isEmpty() ? null : contexts.peek();
	}
}
