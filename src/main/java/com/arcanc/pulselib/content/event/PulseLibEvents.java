/**
 * @author ArcAnc
 * Created at: 25.03.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.event;


import com.arcanc.pulselib.content.model.PTextureReference;
import com.arcanc.pulselib.content.model.animation.PAnimationChannelType;
import com.arcanc.pulselib.content.model.animation.PAnimationEventType;
import com.arcanc.pulselib.content.model.deformer.PMeshDeformer;
import com.arcanc.pulselib.content.model.resource.PModelResource;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationDefinition;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAnimatedAttachmentRenderer;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAnimatedAttachments;
import com.arcanc.pulselib.content.registration.PLibRegistration;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.data.gltf.PGltfModelLoader;
import com.arcanc.pulselib.util.PModelCache;
import com.arcanc.pulselib.util.attachments.PLivingAttachmentDefinition;
import com.arcanc.pulselib.util.attachments.PLivingAttachments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

import java.util.*;

public class PulseLibEvents
{
	public static class TypeRegistrationEvent extends Event implements IModBusEvent
	{
		public <T> PAnimationChannelType<T> registerAnimationChannel(PAnimationChannelType<T> type)
		{
			return PLibRegistration.AnimationChannelReg.CHANNEL_TYPES.register(type.id(), type);
		}

		public <T> PAnimationEventType<T> registerAnimationEvent(PAnimationEventType<T> type)
		{
			return PLibRegistration.AnimationEventReg.EVENT_TYPES.register(type.id(), type);
		}

		public <T> PMeshDeformer<T> registerMeshDeformer(PMeshDeformer<T> type)
		{
			return PLibRegistration.MeshDeformerReg.DEFORMERS.register(type.id(), type);
		}
	}

	public static class RegisterResourceEvent extends Event implements IModBusEvent
	{
		private final Map<ResourceLocation, ModelRegistration> models = new LinkedHashMap<>();

		public ModelRegistration model(PModelData modelData)
		{
			Objects.requireNonNull(modelData);
			return model(modelData.getModelId(), modelData.getModelLoaderId());
		}

		public ModelRegistration model(ResourceLocation modelId)
		{
			return model(modelId, PGltfModelLoader.INSTANCE.id());
		}

		public ModelRegistration model(ResourceLocation modelId, ResourceLocation modelLoaderId)
		{
			PModelCache.getModelLoader(modelLoaderId).
					orElseThrow(() -> new IllegalStateException("No model loader registered for " + modelLoaderId));
			modelId = Objects.requireNonNull(modelId);
			ModelRegistration registration = models.get(modelId);
			if (registration == null)
			{
				registration = new ModelRegistration(modelId, modelLoaderId);
				models.put(modelId, registration);
			}
			else if (!registration.modelLoaderId.equals(modelLoaderId))
				throw new IllegalStateException("Conflicting model loaders registered for " + modelId);
			return registration;
		}

		public void apply(Map<ResourceLocation, PModelResource> target)
		{
			models.forEach((modelId, registration) -> target.put(modelId, registration.build()));
		}

		public static final class ModelRegistration
		{
			private final ResourceLocation modelId;
			private final ResourceLocation modelLoaderId;
			private final Map<String, ResourceLocation> textures = new LinkedHashMap<>();

			private ModelRegistration(ResourceLocation modelId, ResourceLocation modelLoaderId)
			{
				this.modelId = modelId;
				this.modelLoaderId = modelLoaderId;
			}

			public ModelRegistration texture(String reference, ResourceLocation texture)
			{
				String normalized = PTextureReference.normalize(reference);
				ResourceLocation previous = textures.putIfAbsent(normalized, Objects.requireNonNull(texture));
				if (previous != null && !previous.equals(texture))
					throw new IllegalStateException("Conflicting texture for model " + modelId + ": " + normalized);
				return this;
			}

			private PModelResource build()
			{
				return new PModelResource(modelId, modelLoaderId, textures);
			}
		}
	}
	
	public static class AttachmentRegistrationEvent extends Event implements IModBusEvent
	{
		private final AttachmentRegistration registration = new AttachmentRegistration();
		
		public AttachmentRegistration registration()
		{
			return this.registration;
		}
		
		public static final class AttachmentRegistration
		{
			private final List<Runnable> actions = new ArrayList<>();
			
			public void registerLiving(ItemLike item, PLivingAttachmentDefinition definition)
			{
				this.actions.add(() -> PLivingAttachments.register(item.asItem(), definition));
			}
			
			public void registerGlobalLiving(PLivingAttachmentDefinition definition)
			{
				this.actions.add(() -> PLivingAttachments.registerGlobal(definition));
			}
			
			public void apply()
			{
				this.actions.forEach(Runnable :: run);
			}
		}
	}

	public static class PlayerAnimationRegistrationEvent extends Event implements IModBusEvent
	{
		private final PlayerAnimationRegistration registration = new PlayerAnimationRegistration();

		public PlayerAnimationRegistration registration()
		{
			return this.registration;
		}

		public static final class PlayerAnimationRegistration
		{
			private final List<Runnable> actions = new ArrayList<>();

			public void register(ResourceLocation id, PPlayerAnimationDefinition definition)
			{
				this.actions.add(() -> PPlayerAnimations.register(id, definition));
			}

			public void apply()
			{
				this.actions.forEach(Runnable :: run);
			}
		}
	}

	/** Registers renderers for semantic player-animation anchors. */
	public static class PlayerAnimatedAttachmentRegistrationEvent extends Event implements IModBusEvent
	{
		private final PlayerAnimatedAttachmentRegistration registration = new PlayerAnimatedAttachmentRegistration();

		public PlayerAnimatedAttachmentRegistration registration()
		{
			return this.registration;
		}

		public static final class PlayerAnimatedAttachmentRegistration
		{
			private final List<Runnable> actions = new ArrayList<>();

			public void register(PPlayerAnimatedAttachmentRenderer renderer)
			{
				this.actions.add(() -> PPlayerAnimatedAttachments.register(renderer));
			}

			public void apply()
			{
				this.actions.forEach(Runnable::run);
			}
		}
	}
}
