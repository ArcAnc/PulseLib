/**
 * @author ArcAnc
 * Created at: 05.07.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.util.attachments;


import com.arcanc.pulselib.util.attachments.humanoid.PHumanoidAnchors;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.neoforged.bus.api.IEventBus;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Provides support for attachment anchor resolvers.
 */
public class PAttachmentAnchorResolvers
{
	private static final Map<Class<?>, Map<PAttachmentAnchor, AnchorResolver>> RESOLVERS = new Object2ObjectLinkedOpenHashMap<>();
	
	/**
	 * Performs the init operation.
	 * @param bus the bus to use.
	 */
	@ApiStatus.Internal
	public static void init(IEventBus bus)
	{
		PHumanoidAnchors.registerDefaults();
	}
	
	/**
	 * Performs the register operation.
	 * @param modelClass the model class to use.
	 * @param anchor the anchor to use.
	 * @param resolver the resolver to use.
	 */
	public static void register(
			Class<?> modelClass,
			PAttachmentAnchor anchor,
			AnchorResolver resolver)
	{
		RESOLVERS.computeIfAbsent(modelClass, _ -> new Object2ObjectLinkedOpenHashMap<>()).put(anchor, resolver);
	}
	
	/**
	 * Performs the resolve operation.
	 * @param state the state to use.
	 * @param model the model to use.
	 * @param anchor the anchor to use.
	 * @return the value produced by this operation.
	 */
	public static @Nullable <S extends EntityRenderState> ModelPart resolve(S state, EntityModel<? super S> model, PAttachmentAnchor anchor)
	{
		for (Map.Entry<Class<?>, Map<PAttachmentAnchor, AnchorResolver>> entry : RESOLVERS.entrySet())
		{
			if (!entry.getKey().isInstance(model))
				continue;
			
			AnchorResolver resolver = entry.getValue().get(anchor);
			if (resolver == null)
				continue;
			
			return resolver.resolve(state, model);
		}
		
		return null;
	}
	
	@FunctionalInterface
/**
 * Defines the contract for anchor resolver.
 */
	public interface AnchorResolver
	{
		/**
		 * Performs the resolve operation.
		 * @param state the state to use.
		 * @param model the model to use.
		 * @return the value produced by this operation.
		 */
		@Nullable ModelPart resolve(EntityRenderState state, EntityModel<?> model);
	}
}
