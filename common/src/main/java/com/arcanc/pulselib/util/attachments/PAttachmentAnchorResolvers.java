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

public class PAttachmentAnchorResolvers
{
	private static final Map<Class<?>, Map<PAttachmentAnchor, AnchorResolver>> RESOLVERS = new Object2ObjectLinkedOpenHashMap<>();
	
	@ApiStatus.Internal
	public static void init()
	{
		PHumanoidAnchors.registerDefaults();
	}
	
	public static void register(
			Class<?> modelClass,
			PAttachmentAnchor anchor,
			AnchorResolver resolver)
	{
		RESOLVERS.computeIfAbsent(modelClass, $ -> new Object2ObjectLinkedOpenHashMap<>()).put(anchor, resolver);
	}
	
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
	
	@FunctionalInterface
	public interface AnchorResolver
	{
		@Nullable ModelPart resolve(LivingEntity entity, EntityModel<?> model);
	}
}
