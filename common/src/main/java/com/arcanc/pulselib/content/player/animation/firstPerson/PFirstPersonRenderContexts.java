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
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

/** Render-thread passes own their hand frames; nested calls restore the enclosing pass unchanged. */
public final class PFirstPersonRenderContexts
{
	private static final Deque<Pass> PASSES = new ArrayDeque<>();

	private PFirstPersonRenderContexts()
	{
	}

	/**
	 * Samples one presentation for the entire native hand-rendering pass.
	 *
	 * @param player the player whose first-person animation is sampled
	 * @param partialTick the interpolation fraction for this pass
	 */
	public static void beginPass(LocalPlayer player, float partialTick)
	{
		RenderSystem.assertOnRenderThread();
		beginPass(PPlayerAnimations.firstPersonPresentation(player, partialTick), partialTick);
	}

	/** Binds an already sampled presentation independently of animation evaluation. */
	static void beginPass(@Nullable PFirstPersonRenderPresentation presentation)
	{
		beginPass(presentation, 0);
	}

	static void beginPass(@Nullable PFirstPersonRenderPresentation presentation, float partialTick)
	{
		RenderSystem.assertOnRenderThread();
		PASSES.push(new Pass(presentation, partialTick));
	}

	/** Returns the current native pass's extraction fraction, including nested hand submissions. */
	static float partialTick(float fallback)
	{
		RenderSystem.assertOnRenderThread();
		Pass pass = PASSES.peek();
		return pass == null ? fallback : pass.partialTick;
	}

	/** Releases this pass's hand frames and restores the enclosing pass. */
	public static void endPass()
	{
		RenderSystem.assertOnRenderThread();
		if (!PASSES.isEmpty())
			PASSES.pop();
	}

	/** Returns the presentation captured for the current pass. */
	public static @Nullable PFirstPersonRenderPresentation passPresentation()
	{
		RenderSystem.assertOnRenderThread();
		@Nullable Pass pass = PASSES.peek();
		return pass == null ? null : pass.presentation;
	}

	/**
	 * Captures a hand invocation in its owning pass; an empty pass has no animated hand frames.
	 *
	 * @param hand the main or off hand being rendered
	 * @param arm the corresponding physical arm
	 * @param stack the held item supplying map-rendering eligibility
	 * @param poseStack the invocation's base matrices, copied into the hand context
	 */
	public static void push(InteractionHand hand,
	                        HumanoidArm arm,
	                        ItemStack stack,
	                        PoseStack poseStack)
	{
		RenderSystem.assertOnRenderThread();
		@Nullable Pass pass = PASSES.peek();
		if (pass == null || pass.presentation == null)
			return;
		PFirstPersonRenderContext context = new PFirstPersonRenderContext(hand, arm,
				poseStack.last().pose(), poseStack.last().normal(), pass.presentation, stack.getItem() instanceof MapItem);
		pass.hand = new HandFrame(pass.hand, context);
	}

	/** Releases only the current pass's innermost hand invocation. */
	public static void pop()
	{
		RenderSystem.assertOnRenderThread();
		@Nullable Pass pass = PASSES.peek();
		if (pass != null && pass.hand != null)
			pass.hand = pass.hand.previous();
	}

	/** Returns the current hand frame; an empty nested pass never exposes an outer hand. */
	public static @Nullable PFirstPersonRenderContext current()
	{
		RenderSystem.assertOnRenderThread();
		@Nullable Pass pass = PASSES.peek();
		return pass == null || pass.hand == null ? null : pass.hand.context();
	}

	private static final class Pass
	{
		private final @Nullable PFirstPersonRenderPresentation presentation;
		private final float partialTick;
		private @Nullable HandFrame hand;

		private Pass(@Nullable PFirstPersonRenderPresentation presentation, float partialTick)
		{
			this.presentation = presentation;
			this.partialTick = partialTick;
		}
	}

	private record HandFrame(@Nullable HandFrame previous, PFirstPersonRenderContext context)
	{
	}
}
