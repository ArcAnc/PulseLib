/**
 * @author ArcAnc
 * Created at: 23.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PRenderStateScopeTest
{
	@Test
	void nestedOwnerFailureRestoresTheOuterInvocationAndThenReleasesIt()
	{
		var scope = new PRenderStateScope<Object>();
		Object outer = new Object();
		Object inner = new Object();
		scope.run(outer, () ->
		{
			assertThrows(IllegalStateException.class, () -> scope.run(inner, () ->
			{
				assertSame(inner, scope.current());
				throw new IllegalStateException("material hook failed");
			}));
			assertSame(outer, scope.current());
		});
		assertNull(scope.current());
	}

	@Test
	void rendererInstancesDoNotShareSubmissionStateAfterFailureOrReentry()
	{
		var first = new PRenderStateScope<Object>();
		var second = new PRenderStateScope<Object>();
		Object owner = new Object();
		assertThrows(IllegalArgumentException.class, () -> first.run(owner, () ->
		{
			assertNull(second.current());
			second.run(owner, () -> assertSame(owner, first.current()));
			assertNull(second.current());
			throw new IllegalArgumentException("owner callback failed");
		}));
		assertNull(first.current());
		first.run(owner, () -> assertSame(owner, first.current()));
		assertNull(first.current());
	}
}
