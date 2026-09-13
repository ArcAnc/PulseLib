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
	 * Creates an instance of the enclosing type.
	 */
	private PFirstPersonRenderContexts()
	{
	}

	/**
	 * Performs the begin pass operation.
	 * @param player the player to use.
	 * @param partialTick the partial tick to use.
	 */
	public static void beginPass(LocalPlayer player, float partialTick)
	{
		PASSES.get().push(Optional.ofNullable(PPlayerAnimations.firstPersonPresentation(player, partialTick)));
	}

	/**
	 * Performs the end pass operation.
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
	 * Performs the pass presentation operation.
	 * @return the value produced by this operation.
	 */
	public static @Nullable PFirstPersonRenderPresentation passPresentation()
	{
		Deque<Optional<PFirstPersonRenderPresentation>> passes = PASSES.get();
		return passes.isEmpty() ? null : passes.peek().orElse(null);
	}

	/**
	 * Performs the push operation.
	 * @param hand the hand to use.
	 * @param arm the arm to use.
	 * @param stack the stack to use.
	 * @param poseStack the pose stack to use.
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
	 * Performs the pop operation.
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
	 * Performs the current operation.
	 * @return the value produced by this operation.
	 */
	public static @Nullable PFirstPersonRenderContext current()
	{
		Deque<PFirstPersonRenderContext> contexts = CONTEXTS.get();
		return contexts.isEmpty() ? null : contexts.peek();
	}
}
