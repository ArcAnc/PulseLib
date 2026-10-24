/**
 * @author ArcAnc
 * Created at: 25.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer;

import com.arcanc.pulselib.content.renderer.base.PBlockRenderState;
import com.arcanc.pulselib.content.renderer.base.PEntityRenderState;
import net.minecraft.SharedConstants;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PWorldRenderPassTest
{
	@BeforeAll
	static void bootstrap()
	{
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void emptyWorldUsesNativeGraphicsChoice()
	{
		assertFalse(PWorldRenderPass.requiresSeparateTargets(new LevelRenderState()));
	}

	@Test
	void vanillaOwnersDoNotAllocateExtraTargets()
	{
		var state = new LevelRenderState();
		state.entityRenderStates.add(new EntityRenderState());
		assertFalse(PWorldRenderPass.requiresSeparateTargets(state));
	}

	@Test
	void visiblePulseEntityNeedsRetainedNativeLayers()
	{
		var state = new LevelRenderState();
		state.entityRenderStates.add(new PEntityRenderState.Impl<>());
		assertTrue(PWorldRenderPass.requiresSeparateTargets(state));
	}

	@Test
	void visiblePulseBlockEntityNeedsRetainedNativeLayers()
	{
		var state = new LevelRenderState();
		state.blockEntityRenderStates.add(new PBlockRenderState.Impl<>());
		assertTrue(PWorldRenderPass.requiresSeparateTargets(state));
	}

	@Test
	void panoramicRenderingKeepsItsNativeTargetPolicy()
	{
		var state = new LevelRenderState();
		state.entityRenderStates.add(new PEntityRenderState.Impl<>());
		state.cameraRenderState.isPanoramicMode = true;
		assertFalse(PWorldRenderPass.requiresSeparateTargets(state));
	}
}
