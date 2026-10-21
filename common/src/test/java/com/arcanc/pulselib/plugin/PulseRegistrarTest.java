/**
 * @author ArcAnc
 * Created at: 21.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.plugin;

import com.arcanc.pulselib.api.registration.PModelRegistrar;
import com.arcanc.pulselib.api.registration.PTypeRegistrar;
import com.arcanc.pulselib.content.model.animation.PAnimationChannel;
import com.arcanc.pulselib.content.model.resource.PModelResource;
import com.arcanc.pulselib.content.registration.PLibRegistration;
import net.minecraft.SharedConstants;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PulseRegistrarTest
{
	@BeforeAll
	static void bootstrap()
	{
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void wrongPhaseAndClosedScopeRejectNewTypesWithoutChangingRegistries()
	{
		Identifier id = Identifier.fromNamespaceAndPath("test", "closed_channel");
		var channel = new PAnimationChannel.Vector3fChannelType(id, new Vector3f(), false);
		try (var registrar = new PTypeRegistrar(PTypeRegistrar.Kind.EVENTS))
		{
			assertThrows(IllegalStateException.class, () -> registrar.registerAnimationChannel(channel));
		}
		var registrar = new PTypeRegistrar(PTypeRegistrar.Kind.CHANNELS);
		registrar.close();
		assertThrows(IllegalStateException.class, () -> registrar.registerAnimationChannel(channel));
		assertThrows(IllegalStateException.class, registrar::apply);
		assertTrue(PLibRegistration.AnimationChannelReg.CHANNEL_TYPES.get(id).isEmpty());
	}

	@Test
	void conflictingTypeBatchDoesNotPublishItsOtherEntries()
	{
		Identifier id = Identifier.fromNamespaceAndPath("test", "atomic_channel");
		try (var registrar = new PTypeRegistrar(PTypeRegistrar.Kind.CHANNELS))
		{
			registrar.registerAnimationChannel(new PAnimationChannel.Vector3fChannelType(id, new Vector3f(), false));
			registrar.registerAnimationChannel(new PAnimationChannel.Vector3fChannelType(
					PLibRegistration.AnimationChannelReg.POSITION.id(), new Vector3f(), false));
			assertThrows(IllegalArgumentException.class, registrar::apply);
		}
		assertTrue(PLibRegistration.AnimationChannelReg.CHANNEL_TYPES.get(id).isEmpty());
	}

	@Test
	void conflictingModelBatchIsAtomicAndEscapedBuilderExpiresAfterApply()
	{
		Identifier id = Identifier.fromNamespaceAndPath("test", "existing_model");
		Identifier other = Identifier.fromNamespaceAndPath("test", "uncommitted_model");
		Map<Identifier, PModelResource> resources = new LinkedHashMap<>();
		var first = new PModelRegistrar();
		var escaped = first.model(id).texture("0", id);
		first.apply(resources);
		Map<Identifier, PModelResource> snapshot = Map.copyOf(resources);
		assertThrows(IllegalStateException.class, () -> escaped.texture("1", other));
		assertThrows(IllegalStateException.class, () -> first.apply(resources));
		try (var conflicting = new PModelRegistrar())
		{
			conflicting.model(other).texture("0", other);
			conflicting.model(id).texture("0", other);
			assertThrows(IllegalStateException.class, () -> conflicting.apply(resources));
		}
		assertEquals(snapshot, resources);
	}
}
