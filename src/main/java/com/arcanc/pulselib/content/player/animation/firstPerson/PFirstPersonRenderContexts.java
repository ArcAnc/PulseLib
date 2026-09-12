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
	private static final ThreadLocal<Deque<Optional<PPlayerFirstPersonPose>>> PASSES =
			ThreadLocal.withInitial(ArrayDeque::new);

	private PFirstPersonRenderContexts()
	{
	}

	public static void beginPass(LocalPlayer player, float partialTick)
	{
		PASSES.get().push(Optional.ofNullable(PPlayerAnimations.firstPersonPose(player, partialTick)));
	}

	public static void endPass()
	{
		Deque<Optional<PPlayerFirstPersonPose>> passes = PASSES.get();
		if (!passes.isEmpty())
			passes.pop();
		if (passes.isEmpty())
		{
			PASSES.remove();
			CONTEXTS.remove();
		}
	}

	public static @Nullable PPlayerFirstPersonPose passPose()
	{
		Deque<Optional<PPlayerFirstPersonPose>> passes = PASSES.get();
		return passes.isEmpty() ? null : passes.peek().orElse(null);
	}

	public static void push(InteractionHand hand,
	                        HumanoidArm arm,
	                        ItemStack stack,
	                        PoseStack poseStack)
	{
		PPlayerFirstPersonPose pose = passPose();
		if (pose == null)
			return;
		CONTEXTS.get().push(new PFirstPersonRenderContext(hand, arm,
				poseStack.last().pose(), poseStack.last().normal(), pose, stack.getItem() instanceof MapItem));
	}

	public static void pop()
	{
		Deque<PFirstPersonRenderContext> contexts = CONTEXTS.get();
		if (!contexts.isEmpty())
			contexts.pop();
		if (contexts.isEmpty())
			CONTEXTS.remove();
	}

	public static @Nullable PFirstPersonRenderContext current()
	{
		Deque<PFirstPersonRenderContext> contexts = CONTEXTS.get();
		return contexts.isEmpty() ? null : contexts.peek();
	}
}
