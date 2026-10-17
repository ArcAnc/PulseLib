/**
 * @author ArcAnc
 * Created at: 17.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.animatable;

import com.arcanc.pulselib.content.animatable.instance.InstanceAnimationManager;
import com.arcanc.pulselib.content.animatable.singleton.SingletonAnimationManager;
import com.arcanc.pulselib.content.model.animation.PAnimationGraph;
import com.arcanc.pulselib.content.model.animation.PAnimationState;
import com.arcanc.pulselib.util.helpers.PLibHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class PAnimationManagerAccessTest
{
	@AfterEach
	void cleanup()
	{
		SingletonAnimationManager.cleanUp();
		InstanceAnimationManager.cleanUp();
	}

	@Test
	void fieldInitializerDefersOwnerCallbackAndKeepsDirectOwnership()
	{
		InstanceOwner owner = new InstanceOwner();
		assertEquals(0, owner.registrations);
		assertSame(owner.manager, PLibHelper.getManager(owner));
		assertSame(owner.manager, PLibHelper.getManager(owner, new AnimManagerKey(72)));
		assertEquals(1, owner.registrations);
		assertEquals(1, owner.factories);
		assertEquals(1, owner.manager.getControllers().size());
	}

	@Test
	void directConstructorsInitializeOnFirstUseExactlyOnce()
	{
		SingletonOwner owner = new SingletonOwner();
		List<PAnimationManager<SingletonOwner>> managers = List.of(new PAnimationManager<>(owner),
				new InstanceAnimationManager<>(owner), new SingletonAnimationManager<>(owner),
				PLibHelper.createManager(owner, true), PLibHelper.createInstanceManager(owner),
				PLibHelper.createSingletonManager(owner));
		assertEquals(0, owner.registrations);
		for (PAnimationManager<SingletonOwner> manager : managers)
		{
			var controller = manager.getControllers().get("ready");
			assertNotNull(controller);
			manager.initializeControllers();
			assertSame(controller, manager.getControllers().get("ready"));
		}
		assertEquals(managers.size(), owner.registrations);
		assertEquals(managers.size(), owner.factories);
	}

	@Test
	void automaticFactoryUsesSingletonContextsForAnOrdinaryOwner()
	{
		AutomaticOwner owner = new AutomaticOwner();
		assertInstanceOf(SingletonAnimationManager.class, owner.manager);
		assertEquals(0, owner.registrations);
		AnimManagerKey key = new AnimManagerKey(41);
		var first = PLibHelper.getManager(owner, key);
		assertSame(first, owner.manager.getManager(key));
		assertSame(first, PLibHelper.getManager(owner, new AnimManagerKey(41)));
		var second = PLibHelper.getManager(owner, new AnimManagerKey(42));
		assertNotSame(first, second);
		assertEquals(key, first.key());
		assertEquals(1, first.getControllers().size());
		assertNotSame(first.getControllers().get("ready"), second.getControllers().get("ready"));
		assertEquals(2, owner.registrations);
		assertEquals(2, owner.factories);
	}

	@Test
	void explicitFactoriesOverrideAutomaticSelectionWithoutOwnerCallbacks()
	{
		AutomaticOwner owner = new AutomaticOwner();
		InstanceAnimationManager<AutomaticOwner> instance = PLibHelper.createInstanceManager(owner);
		SingletonAnimationManager<AutomaticOwner> singleton = PLibHelper.createSingletonManager(owner);
		assertInstanceOf(InstanceAnimationManager.class, PLibHelper.createManager(owner, false));
		assertInstanceOf(SingletonAnimationManager.class, PLibHelper.createManager(owner, true));
		assertEquals(0, owner.registrations);
		AnimManagerKey firstKey = new AnimManagerKey(1);
		AnimManagerKey secondKey = new AnimManagerKey(2);
		assertSame(instance, instance.getManager(firstKey));
		assertSame(instance, instance.getManager(secondKey));
		assertNotSame(singleton.getManager(firstKey), singleton.getManager(secondKey));
		assertSame(singleton.getManager(firstKey), owner.manager.getManager(firstKey));
		assertEquals(3, owner.registrations);
		assertEquals(3, owner.factories);
	}

	@Test
	void automaticSingletonFieldsKeepOwnersSeparateAndFollowCacheCleanup()
	{
		AutomaticOwner firstOwner = new AutomaticOwner();
		AutomaticOwner secondOwner = new AutomaticOwner();
		AnimManagerKey key = new AnimManagerKey(42);
		var first = PLibHelper.getManager(firstOwner, key);
		var second = PLibHelper.getManager(secondOwner, key);
		assertNotSame(first, second);
		SingletonAnimationManager.cleanUp();
		var recreated = PLibHelper.getManager(firstOwner, key);
		assertNotSame(first, recreated);
		assertSame(recreated, firstOwner.manager.getManager(key));
		assertEquals(2, firstOwner.registrations);
		assertEquals(1, secondOwner.registrations);
	}

	@Test
	void nonItemSingletonUsesOwnerAndContextIdentity()
	{
		SingletonOwner first = new SingletonOwner();
		SingletonOwner second = new SingletonOwner();
		AnimManagerKey key = new AnimManagerKey(42);
		var manager = PLibHelper.getManager(first, key);
		assertSame(manager, PLibHelper.getManager(first, new AnimManagerKey(42)));
		assertNotSame(manager, PLibHelper.getManager(first, new AnimManagerKey(43)));
		assertNotSame(manager, PLibHelper.getManager(second, key));
		assertSame(first, manager.getAnimatable());
		assertEquals(2, first.registrations);
		assertEquals(1, second.registrations);
		assertEquals(2, first.factories);
	}

	@Test
	void incompatibleOwnersCannotShareAKey()
	{
		var key = new AnimManagerKey(1);
		var first = PLibHelper.getManager(new SingletonOwner(), key);
		var second = SingletonAnimationManager.getManager(key, new InstanceOwner());
		assertNotSame(first, second);
		assertInstanceOf(InstanceOwner.class, second.getAnimatable());
	}

	@Test
	void cleanupDropsCachedContexts()
	{
		var owner = new SingletonOwner();
		var key = new AnimManagerKey(42);
		var previous = PLibHelper.getManager(owner, key);
		SingletonAnimationManager.cleanUp();
		var next = PLibHelper.getManager(owner, key);
		assertNotSame(previous, next);
		assertFalse(next.getControllers().isEmpty());
	}

	@Test
	void graphFactoriesKeepIndependentRuntimeAndAreCreatedOnce()
	{
		var owner = new GraphOwner();
		var first = PLibHelper.getManager(owner, new AnimManagerKey(1));
		var second = PLibHelper.getManager(owner, new AnimManagerKey(2));
		assertNotNull(first.getControllers().get("graph").graphRuntime());
		assertNotSame(first.getControllers().get("graph").graphRuntime(), second.getControllers().get("graph").graphRuntime());
		assertSame(first, PLibHelper.getManager(owner, new AnimManagerKey(1)));
		assertEquals(2, owner.factories);
	}

	private static final class InstanceOwner implements PAnimatable<InstanceOwner>
	{
		private final PAnimationManager<InstanceOwner> manager = PLibHelper.createInstanceManager(this);
		private final String ready = "ready";
		private int registrations;
		private int factories;

		@Override
		public PAnimationManager<InstanceOwner> getAnimationManager(AnimManagerKey key)
		{
			return this.manager;
		}

		@Override
		public void registerAnimationControllers(PAnimationManager.PAnimationRegistrar<InstanceOwner> registrar)
		{
			assertNotNull(this.manager);
			assertNotNull(this.ready);
			this.registrations++;
			registrar.add(this.ready, () ->
			{
				this.factories++;
				return state -> ControllerState.STOP;
			});
		}
	}

	private static final class AutomaticOwner implements PAnimatable<AutomaticOwner>
	{
		private final PAnimationManager<AutomaticOwner> manager = PLibHelper.createManager(this);
		private final List<String> controllerNames = List.of("ready");
		private int registrations;
		private int factories;

		@Override
		public PAnimationManager<AutomaticOwner> getAnimationManager(AnimManagerKey key)
		{
			// Also support legacy owners that simply return their stored manager.
			return this.manager;
		}

		@Override
		public void registerAnimationControllers(PAnimationManager.PAnimationRegistrar<AutomaticOwner> registrar)
		{
			assertNotNull(this.manager);
			assertNotNull(this.controllerNames);
			this.registrations++;
			registrar.add(this.controllerNames.getFirst(), () ->
			{
				this.factories++;
				return state -> ControllerState.STOP;
			});
		}
	}

	private static final class SingletonOwner implements PAnimatable<SingletonOwner>
	{
		private int registrations;
		private int factories;

		@Override
		public PAnimationManager<SingletonOwner> getAnimationManager(AnimManagerKey key)
		{
			return SingletonAnimationManager.getManager(key, this);
		}

		@Override
		public void registerAnimationControllers(PAnimationManager.PAnimationRegistrar<SingletonOwner> registrar)
		{
			this.registrations++;
			registrar.add("ready", () ->
			{
				this.factories++;
				return state -> ControllerState.STOP;
			});
		}

		@Override
		public boolean equals(Object object)
		{
			return object instanceof SingletonOwner;
		}

		@Override
		public int hashCode()
		{
			return 1;
		}
	}

	private static final class GraphOwner implements PAnimatable<GraphOwner>
	{
		private final PAnimationManager<GraphOwner> manager = PLibHelper.createManager(this);
		private int factories;

		@Override
		public PAnimationManager<GraphOwner> getAnimationManager(AnimManagerKey key)
		{
			return this.manager.getManager(key);
		}

		@Override
		public void registerAnimationControllers(PAnimationManager.PAnimationRegistrar<GraphOwner> registrar)
		{
			registrar.addGraph("graph", () ->
			{
				this.factories++;
				return new PAnimationGraph(List.of(new PAnimationState.Clip("idle", "idle")), List.of());
			});
		}
	}
}
