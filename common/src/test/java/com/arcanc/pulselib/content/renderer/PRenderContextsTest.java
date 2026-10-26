/**
 * @author ArcAnc
 * Created at: 26.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PRenderContextsTest
{
	@BeforeAll
	static void renderThread()
	{
		if (!RenderSystem.isOnRenderThread())
			RenderSystem.initRenderThread();
	}

	@Test
	void nestedWorldAndHandScopesRestoreTheirCallerAfterAnException()
	{
		var caller = PRenderContexts.current();
		try (var world = PRenderContexts.enter(PRenderContexts.Context.WORLD))
		{
			assertEquals(PRenderContexts.Context.WORLD, PRenderContexts.current());
			assertEquals(PRenderQueue.RenderStage.ENTITIES, PRenderContexts.entityStage());
			assertThrows(IllegalArgumentException.class, () ->
			{
				try (var hand = PRenderContexts.enter(PRenderContexts.Context.FIRST_PERSON))
				{
					assertEquals(PRenderContexts.Context.FIRST_PERSON, PRenderContexts.current());
					assertEquals(PRenderQueue.RenderStage.FIRST_PERSON, PRenderContexts.entityStage());
					assertNull(PRenderContexts.enterEntity());
					throw new IllegalArgumentException("hand callback");
				}
			});
			assertEquals(PRenderContexts.Context.WORLD, PRenderContexts.current());
			assertNull(PRenderContexts.localScope());
		}
		assertEquals(caller, PRenderContexts.current());
	}

	@Test
	void outOfOrderCloseDoesNotConsumeTheInnerScopeAndCloseIsIdempotent()
	{
		var caller = PRenderContexts.current();
		try (var world = PRenderContexts.enter(PRenderContexts.Context.WORLD))
		{
			try (var hand = PRenderContexts.enter(PRenderContexts.Context.FIRST_PERSON))
			{
				assertThrows(IllegalStateException.class, world::close);
				assertEquals(PRenderContexts.Context.FIRST_PERSON, PRenderContexts.current());
				hand.close();
				hand.close();
				assertEquals(PRenderContexts.Context.WORLD, PRenderContexts.current());
			}
		}
		assertEquals(caller, PRenderContexts.current());
	}

	@Test
	void workerThreadCannotReadOrChangeTheSharedRenderStack() throws InterruptedException
	{
		try (var world = PRenderContexts.enter(PRenderContexts.Context.WORLD))
		{
			rejectOffThread(PRenderContexts::current);
			rejectOffThread(() -> PRenderContexts.enter(PRenderContexts.Context.FIRST_PERSON));
			rejectOffThread(PRenderContexts::enterEntity);
			rejectOffThread(PRenderContexts::entityStage);
			rejectOffThread(PRenderContexts::localScope);
			rejectOffThread(world::discard);
			rejectOffThread(world::close);
			assertEquals(PRenderContexts.Context.WORLD, PRenderContexts.current());
		}
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
		}, "render context guard test");
		worker.setDaemon(true);
		worker.start();
		worker.join(2000);
		assertFalse(worker.isAlive());
		assertInstanceOf(IllegalStateException.class, failure.get());
	}
}
