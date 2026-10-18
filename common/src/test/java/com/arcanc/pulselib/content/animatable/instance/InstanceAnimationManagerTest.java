/**
 * @author ArcAnc
 * Created at: 18.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.animatable.instance;

import com.arcanc.pulselib.content.animatable.AnimManagerKey;
import com.arcanc.pulselib.content.animatable.PAnimatable;
import com.arcanc.pulselib.content.animatable.PAnimationManager;
import com.arcanc.pulselib.content.model.animation.PAnimation;
import com.arcanc.pulselib.content.model.animation.PAnimationGraph;
import com.arcanc.pulselib.content.model.animation.PAnimationState;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstanceAnimationManagerTest
{
	@AfterEach
	void cleanup()
	{
		InstanceAnimationManager.cleanUp();
	}

	@Test
	void publicRenderAccessRefreshesExistingRegistration() throws ReflectiveOperationException
	{
		Owner owner = new Owner();
		InstanceAnimationManager.addManager(owner.manager, 0);
		InstanceAnimationManager.addManager(owner.manager);
		InstanceAnimationManager.addManager(owner.manager);
		assertTrue(registry().get(owner.manager) > 0, "Rendering must refresh the existing entry");
		assertEquals(1, size());
	}

	@Test
	void controlledTimeExpiresOnlyAfterTheLastUseAndKeepsControllerState() throws ReflectiveOperationException
	{
		Owner owner = new Owner();
		var controller = owner.manager.getControllers().get("graph");
		var graph = controller.graphRuntime();
		var model = new PBakedModel(List.of(), Map.of("idle", new PAnimation("idle", 100, Map.of())));
		owner.manager.bindModel(model);
		graph.parameters().set("saved", 42);
		graph.tickPrecise(model, 12);
		InstanceAnimationManager.addManager(owner.manager, 0);
		InstanceAnimationManager.addManager(owner.manager, 3000);
		InstanceAnimationManager.addManager(owner.manager, 6000);
		InstanceAnimationManager.removeUnused(8001);
		assertEquals(1, size());
		InstanceAnimationManager.tickAll();
		assertEquals(13, controller.getTime());
		assertEquals(42, graph.parameters().get("saved"));
		InstanceAnimationManager.removeUnused(11000);
		assertEquals(1, size(), "Existing TTL uses a strict greater-than comparison");
		InstanceAnimationManager.removeUnused(11001);
		assertEquals(0, size());
		InstanceAnimationManager.addManager(owner.manager, 12000);
		InstanceAnimationManager.addManager(owner.manager, 12001);
		assertEquals(1, size());
		assertSame(controller, owner.manager.getControllers().get("graph"));
		assertSame(graph, controller.graphRuntime());
		assertEquals(13, controller.getTime());
		assertEquals(1, owner.registrations);
		InstanceAnimationManager.removeManager(owner);
		assertEquals(0, size());
	}

	@Test
	void aThousandActiveManagersHaveNoDuplicatesAndCleanupReleasesAll() throws ReflectiveOperationException
	{
		List<Owner> owners = new ArrayList<>();
		for (int index = 0; index < 1000; index++)
		{
			Owner owner = new Owner();
			owners.add(owner);
			InstanceAnimationManager.addManager(owner.manager, 0);
		}
		for (int cycle = 1; cycle <= 5; cycle++)
		{
			for (Owner owner : owners)
			{
				owner.hash++;
				InstanceAnimationManager.addManager(owner.manager, cycle * 4000);
				InstanceAnimationManager.addManager(owner.manager, cycle * 4000);
			}
			InstanceAnimationManager.removeUnused(cycle * 4000 + 1000);
			assertEquals(1000, size());
		}
		InstanceAnimationManager.removeManager(owners.getFirst());
		assertEquals(999, size());
		InstanceAnimationManager.cleanUp();
		assertEquals(0, size());
		InstanceAnimationManager.addManager(owners.getFirst().manager, 30000);
		assertEquals(1, size());
	}

	@SuppressWarnings("unchecked")
	private static Map<InstanceAnimationManager<?>, Long> registry() throws ReflectiveOperationException
	{
		Field field = InstanceAnimationManager.class.getDeclaredField("MANAGERS");
		field.setAccessible(true);
		return (Map<InstanceAnimationManager<?>, Long>)field.get(null);
	}

	private static int size() throws ReflectiveOperationException
	{
		return registry().size();
	}

	private static final class Owner implements PAnimatable<Owner>
	{
		private final InstanceAnimationManager<Owner> manager = new UnstableManager(this);
		private int registrations;
		private int hash;

		@Override
		public PAnimationManager<Owner> getAnimationManager(AnimManagerKey key)
		{
			return this.manager;
		}

		@Override
		public void registerAnimationControllers(PAnimationManager.PAnimationRegistrar<Owner> registrar)
		{
			this.registrations++;
			registrar.addGraph("graph", new PAnimationGraph(List.of(new PAnimationState.Clip("idle", "idle")), List.of()));
		}

		@Override
		public int hashCode()
		{
			return this.hash;
		}
	}
	/** Identity registration must also survive an overriding manager's equals/hashCode. */
	private static final class UnstableManager extends InstanceAnimationManager<Owner>
	{
		private UnstableManager(Owner owner)
		{
			super(owner);
		}

		@Override
		public boolean equals(Object other)
		{
			return other instanceof UnstableManager;
		}

		@Override
		public int hashCode()
		{
			return getAnimatable().hash;
		}
	}

}
