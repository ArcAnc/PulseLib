/**
 * @author ArcAnc
 * Created at: 08.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.deformer;

/**
 * Scoped flag that enables deformable cube creation while player model layers are baked.
 */
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

	/**
	 * Enters a deformable cube bake scope and returns its restoration handle.
	 *
	 * @return the scope that restores the previous cube-baking state
	 */
	public static Scope begin()
	{
		DEPTH.set(DEPTH.get() + 1);
		return new Scope();
	}

	/**
	 * Returns whether deformable cube baking is enabled in the current scope.
	 *
	 * @return whether deformable cube baking is enabled in the current scope
	 */
	public static boolean isActive()
	{
		return DEPTH.get() > 0;
	}

	/**
	 * Restores the previous deformable-cube baking state when closed.
	 */
	public static final class Scope implements AutoCloseable
	{
		/**
		 * Restores the cube-baking state that was active before this scope was entered.
		 */
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
