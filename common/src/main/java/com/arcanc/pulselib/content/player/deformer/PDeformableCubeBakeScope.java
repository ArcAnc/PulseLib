/**
 * @author ArcAnc
 * Created at: 08.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.deformer;

public final class PDeformableCubeBakeScope
{
	/*
	 * REQUIRE CHANGE: this ThreadLocal transports EntityModelSet.bakeLayer's layer selection to
	 * CubeDefinition.bake because vanilla does not expose that context there. Replace it with an
	 * explicit per-CubeDefinition marker once the model-definition tree has a stable accessor path.
	 */
	private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

	private PDeformableCubeBakeScope()
	{
	}

	public static Scope begin()
	{
		DEPTH.set(DEPTH.get() + 1);
		return new Scope();
	}

	public static boolean isActive()
	{
		return DEPTH.get() > 0;
	}

	public static final class Scope implements AutoCloseable
	{
		@Override
		public void close()
		{
			int depth = DEPTH.get() - 1;
			if (depth <= 0)
				DEPTH.remove();
			else
				DEPTH.set(depth);
		}
	}
}
