/**
 * @author ArcAnc
 * Created at: 04.07.2026
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
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Resolves namespaced attachment anchors for supported entity model classes.
 */
public class PAttachmentAnchorResolvers
{
	private static final Map<Class<?>, Map<PAttachmentAnchor, AnchorResolver>> RESOLVERS = new Object2ObjectLinkedOpenHashMap<>();
	
	/**
	 * Registers built-in model attachment anchors.
	 */
	@ApiStatus.Internal
	public static void init()
	{
		PHumanoidAnchors.registerDefaults();
	}
	
	/**
	 * Associates an anchor resolver with a supported model class.
	 *
	 * @param modelClass the model class
	 * @param anchor the anchor
	 * @param resolver the resolver
	 */
	public static void register(
			Class<?> modelClass,
			PAttachmentAnchor anchor,
			AnchorResolver resolver)
	{
		RESOLVERS.computeIfAbsent(modelClass, $ -> new Object2ObjectLinkedOpenHashMap<>()).put(anchor, resolver);
	}
	
	/**
	 * Resolves an entity model part for the supplied attachment anchor.
	 *
	 * @param <T> the entity type rendered or animated by this contract
	 * @param entity the entity being animated or rendered
	 * @param model the model
	 * @param anchor the anchor
	 * @return the matching vanilla model part, or {@code null} when unresolved
	 */
	public static @Nullable <T extends LivingEntity> ModelPart resolve(T entity, EntityModel<T> model, PAttachmentAnchor anchor)
	{
		for (Map.Entry<Class<?>, Map<PAttachmentAnchor, AnchorResolver>> entry : RESOLVERS.entrySet())
		{
			if (!entry.getKey().isInstance(model))
				continue;
			
			AnchorResolver resolver = entry.getValue().get(anchor);
			if (resolver == null)
				continue;
			
			return resolver.resolve(entity, model);
		}
		
		return null;
	}
	
	/**
	 * Finds the vanilla model part corresponding to one attachment anchor.
	 */
	@FunctionalInterface
	public interface AnchorResolver
	{
		/**
		 * Resolves the attachment model part for the supplied living entity and model.
		 *
		 * @param entity the entity being animated or rendered
		 * @param model the model
		 * @return the matching vanilla model part, or {@code null} when unresolved
		 */
		@Nullable ModelPart resolve(LivingEntity entity, EntityModel<?> model);
	}
}
