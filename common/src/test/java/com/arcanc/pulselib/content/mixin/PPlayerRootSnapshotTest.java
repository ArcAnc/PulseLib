/**
 * @author ArcAnc
 * Created at: 26.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PPlayerRootSnapshotTest
{
	@Test
	void incomingAndReturnedMatricesCannotMutateTheExtractedRoot()
	{
		var state = new WorldEntityRenderStateMixin()
		{
		};
		var root = new Matrix4f().translate(1, 2, 3).rotateY(0.4f).scale(2, 3, 4);
		var expected = new Matrix4f(root);
		state.pulselib$setPlayerRoot(root);
		root.identity();
		var returned = state.pulselib$getPlayerRoot();
		assertNotNull(returned);
		assertTrue(expected.equals(returned, 0));
		returned.identity();
		assertTrue(expected.equals(state.pulselib$getPlayerRoot(), 0));
	}

	@Test
	void reusedNonPlayerStateDropsTheOldRootIndependentlyOfTargetEligibility()
	{
		var state = new WorldEntityRenderStateMixin()
		{
		};
		state.pulselib$setPlayerRoot(new Matrix4f().translation(1, 0, 0));
		state.pulselib$setRequiresSeparateTargets(true);
		state.pulselib$setPlayerRoot(null);
		assertNull(state.pulselib$getPlayerRoot());
		assertTrue(state.pulselib$requiresSeparateTargets());
	}
}
