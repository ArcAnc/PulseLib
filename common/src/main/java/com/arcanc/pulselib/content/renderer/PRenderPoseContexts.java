/**
 * @author ArcAnc
 * Created at: 22.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer;

import com.arcanc.pulselib.content.animatable.PAnimatable;
import com.arcanc.pulselib.content.animatable.PAnimationController;
import com.arcanc.pulselib.content.model.animation.PAnimationPoseResolver;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.data.gecko.MolangParser;

import java.util.*;

/** Shares one evaluated pose across a submission's bone traversal and attached models. */
final class PRenderPoseContexts<T extends PAnimatable<T>> extends IdentityHashMap<PAnimationController<T>, MolangParser.Context>
{
	private final List<Sample<T>> poses = new ArrayList<>();

	/** Resolves through the modern evaluator while keeping existing protected renderer hooks. */
	@SuppressWarnings("unchecked")
	static <T extends PAnimatable<T>> PAnimationPoseResolver<T> resolve(PBakedModel model,
	                                                                  Collection<PAnimationController<T>> controllers,
	                                                                  Map<PAnimationController<T>, MolangParser.Context> contexts,
	                                                                  float partialTick)
	{
		if (contexts instanceof PRenderPoseContexts<?> cache)
		{
			PRenderPoseContexts<T> typed = (PRenderPoseContexts<T>)cache;
			for (Sample<T> sample : typed.poses)
				if (sample.model() == model && sample.controllers() == controllers && sample.partialTick() == partialTick)
					return sample.resolver();
			PAnimationPoseResolver<T> resolver = evaluate(model, controllers, contexts, partialTick);
			typed.poses.add(new Sample<>(model, controllers, partialTick, resolver));
			return resolver;
		}
		return evaluate(model, controllers, contexts, partialTick);
	}

	private static <T extends PAnimatable<T>> PAnimationPoseResolver<T> evaluate(PBakedModel model,
	                                                                         Collection<PAnimationController<T>> controllers,
	                                                                         Map<PAnimationController<T>, MolangParser.Context> contexts,
	                                                                         float partialTick)
	{
		return new PAnimationPoseResolver<>(model, controllers, (controller, tick) ->
		{
			MolangParser.Context context = contexts.get(controller);
			return context == null ? PAnimationPoseResolver.<T>defaultContexts().context(controller, tick) : context;
		}, partialTick);
	}

	private record Sample<T extends PAnimatable<T>>(PBakedModel model,
	                                                Collection<PAnimationController<T>> controllers,
	                                                float partialTick,
	                                                PAnimationPoseResolver<T> resolver)
	{
	}
}
