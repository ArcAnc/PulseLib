/**
 * @author ArcAnc
 * Created at: 13.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.rhi;

import com.arcanc.pulselib.neoforge.NeoForgeRhiPipelines;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.BusBuilder;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

import java.util.HashMap;
import java.util.Map;

/** Native event bus and native registration event; never substitutes a mock loader or event class. */
final class RhiRegistrationChecks
{
	static void run()
	{
		var bus = BusBuilder.builder().markerType(IModBusEvent.class).checkTypesOnDispatch().build();
		bus.addListener(NeoForgeRhiPipelines::register);
		Map<Identifier, RenderPipeline> registered = new HashMap<>();
		bus.post(new RegisterRenderPipelinesEvent(pipeline ->
		{
			if (registered.putIfAbsent(pipeline.getLocation(), pipeline) != null)
				throw new AssertionError("Duplicate RHI pipeline registration");
		}));
		RhiLayoutChecks.assertTrue(registered.size() == 4, "Four RHI profiles through actual mod event bus");
		var portable = PRhiPipelines.portable();
		RhiLayoutChecks.assertTrue(portable == PRhiPipelines.portable() && portable.instanceCapacity() == 128, "Stable portable Common definitions");
		portable.register(pipeline -> RhiLayoutChecks.assertTrue(registered.get(pipeline.getLocation()) == pipeline, "Registered definitions shared with material consumers"));
		System.out.println("PASS NeoForge native mod-bus RHI registration, four shared portable profiles, capacity 128");
	}
}
