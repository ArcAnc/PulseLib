/**
 * @author ArcAnc
 * Created at: 11.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.util.helpers;

import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.PRenderTypes.ShadersProvider;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PShaderRegistrationTest
{
	@Test
	void canonicalAndNativeRegistrationShareOneDrainAndStablePipelineOwners() throws IOException
	{
		RenderPipeline solid = ShadersProvider.trianglesSolid();
		Set<RenderPipeline> registered = Collections.newSetFromMap(new IdentityHashMap<>());
		ShadersProvider.registerShaders(pipeline -> assertTrue(registered.add(pipeline)));
		assertEquals(19, registered.size());
		assertTrue(registered.contains(solid));
		assertTrue(registered.contains(ShadersProvider.oitFabulousComposite()));
		var repeated = new AtomicInteger();
		PRenderTypes.register(pipeline -> repeated.incrementAndGet());
		ShadersProvider.registerShaders(pipeline -> repeated.incrementAndGet());
		assertEquals(0, repeated.get());
		assertSame(solid, ShadersProvider.trianglesSolid());
	}
}
