/**
 * @author ArcAnc
 * Created at: 25.03.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.event;


import com.arcanc.pulselib.util.attachments.PLivingAttachmentDefinition;
import com.arcanc.pulselib.util.attachments.PLivingAttachments;
import com.arcanc.pulselib.content.model.animation.PAnimationChannelType;
import com.arcanc.pulselib.content.model.animation.PAnimationEventType;
import com.arcanc.pulselib.content.model.deformer.PMeshDeformer;
import com.arcanc.pulselib.content.model.PTextureReference;
import com.arcanc.pulselib.content.model.resource.PModelResource;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationDefinition;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAnimatedAttachmentRenderer;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAnimatedAttachments;
import com.arcanc.pulselib.content.registration.PLibRegistration;
import com.arcanc.pulselib.data.PModelLoader;
import com.arcanc.pulselib.data.gltf.PGltfModelLoader;
import com.arcanc.pulselib.util.PModelCache;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

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
		private final Map<Identifier, ModelRegistration> models = new LinkedHashMap<>();

		public ModelRegistration model(Identifier model)
		{
			return model(model, PGltfModelLoader.INSTANCE.id());
		}

		public ModelRegistration model(Identifier model, Identifier modelLoaderId)
		{
			Objects.requireNonNull(model);
			Objects.requireNonNull(modelLoaderId);
			PModelLoader loader = PModelCache.getModelLoader(modelLoaderId).
					orElseThrow(() -> new IllegalStateException("No model loader registered for " + modelLoaderId));
			Identifier normalizedModel = loader.normalizeModelResourceLocation(model);
			ModelRegistration existing = this.models.get(normalizedModel);
			if (existing == null)
			{
				existing = new ModelRegistration(normalizedModel, modelLoaderId);
				this.models.put(normalizedModel, existing);
			}
			else if (!existing.modelLoaderId.equals(modelLoaderId))
				throw new IllegalStateException("Conflicting model loaders registered for model " + normalizedModel + ": " +
						existing.modelLoaderId + " and " + modelLoaderId);
			return existing;
		}

		public void apply(Map<Identifier, PModelResource> registeredResources)
		{
			Map<Identifier, PModelResource> completed = new LinkedHashMap<>();
			this.models.forEach((model, registration) -> completed.put(model, registration.build()));
			registeredResources.putAll(completed);
		}

		public static final class ModelRegistration
		{
			private final Identifier model;
			private final Identifier modelLoaderId;
			private final Map<String, Identifier> textures = new LinkedHashMap<>();

			private ModelRegistration(Identifier model, Identifier modelLoaderId)
			{
				this.model = model;
				this.modelLoaderId = modelLoaderId;
			}

			public ModelRegistration texture(String reference, Identifier texture)
			{
				Objects.requireNonNull(reference);
				Objects.requireNonNull(texture);
				String normalizedReference = PTextureReference.normalize(reference);
				Identifier previous = this.textures.putIfAbsent(normalizedReference, texture);
				if (previous != null && !previous.equals(texture))
					throw new IllegalStateException("Conflicting textures registered for model " + this.model + ", reference " +
							normalizedReference + ": " + previous + " and " + texture);
				return this;
			}

			private PModelResource build()
			{
				return new PModelResource(this.model, this.modelLoaderId, this.textures);
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

			public void register(Identifier id, PPlayerAnimationDefinition definition)
			{
				this.actions.add(() -> PPlayerAnimations.register(id, definition));
			}

			public void apply()
			{
				this.actions.forEach(Runnable :: run);
			}
		}
	}

	/** Lets client integrations register renderers for animated player anchors. */
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
