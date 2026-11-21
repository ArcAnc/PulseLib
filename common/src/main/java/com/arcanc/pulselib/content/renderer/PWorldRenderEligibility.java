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
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/** Captures early declarations for deferred world rendering during native entity extraction. */
@ApiStatus.Internal
public final class PWorldRenderEligibility
{
	private static final ThreadLocal<@Nullable Extraction> CURRENT = new ThreadLocal<>();

	private PWorldRenderEligibility()
	{
	}

	public static <S extends EntityRenderState> S capture(Supplier<S> extract)
	{
		Extraction parent = CURRENT.get();
		Extraction extraction = new Extraction();
		CURRENT.set(extraction);
		try
		{
			S state = extract.get();
			if (state instanceof PWorldRenderState pulse)
				pulse.pulselib$setRequiresSeparateTargets(extraction.requiresTargets);
			return state;
		}
		finally
		{
			if (parent == null)
				CURRENT.remove();
			else
				CURRENT.set(parent);
		}
	}

	/** Compatibility hook for special renderers that require targets for deferred OIT. */
	public static void markItem()
	{
		markGeometry();
	}

	/** Declares a possible deferred OIT submission from the current entity. */
	public static void markGeometry()
	{
		Extraction extraction = CURRENT.get();
		if (extraction != null)
			extraction.requiresTargets = true;
	}

	private static final class Extraction
	{
		private boolean requiresTargets;
	}
}
