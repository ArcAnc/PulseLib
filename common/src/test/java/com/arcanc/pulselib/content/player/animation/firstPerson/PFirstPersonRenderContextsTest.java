/**
 * @author ArcAnc
 * Created at: 26.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation.firstPerson;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PFirstPersonRenderContextsTest
{
	@BeforeAll
	static void bootstrap()
	{
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		if (!RenderSystem.isOnRenderThread())
			RenderSystem.initRenderThread();
	}

	@Test
	void emptyNestedPassNeverExposesOrPopsTheOuterHand()
	{
		var outer = presentation();
		PFirstPersonRenderContexts.beginPass(outer);
		try
		{
			push(InteractionHand.MAIN_HAND, HumanoidArm.RIGHT);
			var hand = PFirstPersonRenderContexts.current();
			assertNotNull(hand);
			PFirstPersonRenderContexts.beginPass(null);
			try
			{
				assertNull(PFirstPersonRenderContexts.passPresentation());
				assertNull(PFirstPersonRenderContexts.current());
				push(InteractionHand.OFF_HAND, HumanoidArm.LEFT);
				assertNull(PFirstPersonRenderContexts.current());
				PFirstPersonRenderContexts.pop();
				assertNull(PFirstPersonRenderContexts.current());
			}
			finally
			{
				PFirstPersonRenderContexts.endPass();
			}
			assertSame(outer, PFirstPersonRenderContexts.passPresentation());
			assertSame(hand, PFirstPersonRenderContexts.current());
		}
		finally
		{
			PFirstPersonRenderContexts.endPass();
		}
		assertNull(PFirstPersonRenderContexts.current());
		assertNull(PFirstPersonRenderContexts.passPresentation());
	}

	@Test
	void failedNestedPassReleasesItsHandsAndRestoresItsParent()
	{
		var outer = presentation();
		var inner = presentation();
		PFirstPersonRenderContexts.beginPass(outer);
		try
		{
			push(InteractionHand.MAIN_HAND, HumanoidArm.RIGHT);
			var hand = PFirstPersonRenderContexts.current();
			assertNotNull(hand);
			assertThrows(IllegalStateException.class, () ->
			{
				PFirstPersonRenderContexts.beginPass(inner);
				try
				{
					assertSame(inner, PFirstPersonRenderContexts.passPresentation());
					assertNull(PFirstPersonRenderContexts.current());
					push(InteractionHand.OFF_HAND, HumanoidArm.LEFT);
					assertSame(inner, PFirstPersonRenderContexts.current().presentation());
					assertEquals(HumanoidArm.LEFT, PFirstPersonRenderContexts.current().arm());
					throw new IllegalStateException("nested pass callback");
				}
				finally
				{
					PFirstPersonRenderContexts.endPass();
				}
			});
			assertSame(outer, PFirstPersonRenderContexts.passPresentation());
			assertSame(hand, PFirstPersonRenderContexts.current());
		}
		finally
		{
			PFirstPersonRenderContexts.endPass();
		}
	}

	@Test
	void failedNestedHandRestoresThePreviousHand()
	{
		PFirstPersonRenderContexts.beginPass(presentation());
		try
		{
			push(InteractionHand.MAIN_HAND, HumanoidArm.RIGHT);
			var hand = PFirstPersonRenderContexts.current();
			assertNotNull(hand);
			assertThrows(IllegalArgumentException.class, () ->
			{
				push(InteractionHand.OFF_HAND, HumanoidArm.LEFT);
				try
				{
					assertEquals(InteractionHand.OFF_HAND, PFirstPersonRenderContexts.current().hand());
					throw new IllegalArgumentException("hand callback");
				}
				finally
				{
					PFirstPersonRenderContexts.pop();
				}
			});
			assertSame(hand, PFirstPersonRenderContexts.current());
			PFirstPersonRenderContexts.pop();
			assertNull(PFirstPersonRenderContexts.current());
		}
		finally
		{
			PFirstPersonRenderContexts.endPass();
		}
	}

	@Test
	void callsOutsideAPassDoNotRetainHandState()
	{
		push(InteractionHand.MAIN_HAND, HumanoidArm.RIGHT);
		assertNull(PFirstPersonRenderContexts.current());
		PFirstPersonRenderContexts.pop();
		PFirstPersonRenderContexts.endPass();
		assertNull(PFirstPersonRenderContexts.passPresentation());
	}

	@Test
	void workerThreadCannotReadOrChangeTheActivePass() throws InterruptedException
	{
		var pose = presentation();
		PFirstPersonRenderContexts.beginPass(pose);
		try
		{
			push(InteractionHand.MAIN_HAND, HumanoidArm.RIGHT);
			var hand = PFirstPersonRenderContexts.current();
			assertNotNull(hand);
			rejectOffThread(() -> PFirstPersonRenderContexts.beginPass(presentation()));
			rejectOffThread(PFirstPersonRenderContexts::endPass);
			rejectOffThread(PFirstPersonRenderContexts::passPresentation);
			rejectOffThread(() -> push(InteractionHand.OFF_HAND, HumanoidArm.LEFT));
			rejectOffThread(PFirstPersonRenderContexts::pop);
			rejectOffThread(PFirstPersonRenderContexts::current);
			assertSame(pose, PFirstPersonRenderContexts.passPresentation());
			assertSame(hand, PFirstPersonRenderContexts.current());
		}
		finally
		{
			PFirstPersonRenderContexts.endPass();
		}
	}

	private static void push(InteractionHand hand, HumanoidArm arm)
	{
		PFirstPersonRenderContexts.push(hand, arm, ItemStack.EMPTY, new PoseStack());
	}

	private static PFirstPersonRenderPresentation presentation()
	{
		return new PFirstPersonRenderPresentation(PFirstPersonArmPose.vanilla(), PFirstPersonArmPose.vanilla(),
				PFirstPersonItemPose.vanilla(), PFirstPersonItemPose.vanilla(), List.of(), List.of());
	}

	private static void rejectOffThread(Runnable action) throws InterruptedException
	{
		var failure = new AtomicReference<Throwable>();
		var worker = new Thread(() ->
		{
			try
			{
				action.run();
			}
			catch (Throwable thrown)
			{
				failure.set(thrown);
			}
		}, "first person context guard test");
		worker.setDaemon(true);
		worker.start();
		worker.join(2000);
		assertFalse(worker.isAlive());
		assertInstanceOf(IllegalStateException.class, failure.get());
	}
}
