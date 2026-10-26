/**
 * @author ArcAnc
 * Created at: 26.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer;

import com.arcanc.pulselib.content.renderer.base.PWorldRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PWorldRenderEligibilityTest
{
	@Test
	void actualItemExtractionRetainsLayersForAVanillaOwner()
	{
		var owner = new State();
		assertSame(owner, PWorldRenderEligibility.capture(() ->
		{
			PWorldRenderEligibility.markItem();
			return owner;
		}));
		var world = new LevelRenderState();
		world.entityRenderStates.add(owner);
		assertTrue(PWorldRenderPass.requiresSeparateTargets(world));
		world.cameraRenderState.isPanoramicMode = true;
		assertFalse(PWorldRenderPass.requiresSeparateTargets(world));
	}

	@Test
	void reusedStateLosesOldItemEligibility()
	{
		var owner = new State();
		PWorldRenderEligibility.capture(() ->
		{
			PWorldRenderEligibility.markItem();
			return owner;
		});
		PWorldRenderEligibility.capture(() -> owner);
		assertFalse(owner.required);
	}

	@Test
	void nestedExtractionDoesNotMarkAnUnrelatedOuterOwner()
	{
		var inner = new State();
		var outer = PWorldRenderEligibility.capture(() ->
		{
			PWorldRenderEligibility.capture(() ->
			{
				PWorldRenderEligibility.markItem();
				return inner;
			});
			return new State();
		});
		assertTrue(inner.required);
		assertFalse(outer.required);
	}

	@Test
	void failedNestedExtractionRestoresItsParentAndFailurePropagates()
	{
		var failure = new IllegalArgumentException("extraction");
		var outer = PWorldRenderEligibility.capture(() ->
		{
			assertSame(failure, assertThrows(IllegalArgumentException.class, () -> PWorldRenderEligibility.capture(() ->
			{
				PWorldRenderEligibility.markItem();
				throw failure;
			})));
			PWorldRenderEligibility.markItem();
			return new State();
		});
		assertTrue(outer.required);
		PWorldRenderEligibility.markItem();
		assertFalse(PWorldRenderEligibility.capture(State::new).required);
	}

	@Test
	void failedTopLevelExtractionDoesNotLeakIntoTheNextInvocation()
	{
		assertThrows(IllegalStateException.class, () -> PWorldRenderEligibility.capture(() ->
		{
			PWorldRenderEligibility.markItem();
			throw new IllegalStateException("extraction");
		}));
		assertFalse(PWorldRenderEligibility.capture(State::new).required);
	}

	private static final class State extends EntityRenderState implements PWorldRenderState
	{
		private boolean required;

		@Override
		public boolean pulselib$requiresSeparateTargets()
		{
			return this.required;
		}

		@Override
		public void pulselib$setRequiresSeparateTargets(boolean required)
		{
			this.required = required;
		}
	}
}
