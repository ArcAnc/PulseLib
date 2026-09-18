/**
 * @author ArcAnc
 * Created at: 25.03.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.event;


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
import com.arcanc.pulselib.util.attachments.PLivingAttachmentDefinition;
import com.arcanc.pulselib.util.attachments.PLivingAttachments;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Provides support for pulse lib events.
 */
public class PulseLibEvents
{
/**
 * Provides support for type registration event.
 */
	public static class TypeRegistrationEvent extends Event implements IModBusEvent
	{
		/**
		 * Registers the animation channel.
		 * @param type the type to use.
		 * @return the value produced by this operation.
		 */
		public <T> PAnimationChannelType<T> registerAnimationChannel(PAnimationChannelType<T> type)
		{
			return PLibRegistration.AnimationChannelReg.CHANNEL_TYPES.register(type.id(), type);
		}

		/**
		 * Registers the animation event.
		 * @param type the type to use.
		 * @return the value produced by this operation.
		 */
		public <T> PAnimationEventType<T> registerAnimationEvent(PAnimationEventType<T> type)
		{
			return PLibRegistration.AnimationEventReg.EVENT_TYPES.register(type.id(), type);
		}

		/**
		 * Registers the mesh deformer.
		 * @param type the type to use.
		 * @return the value produced by this operation.
		 */
		public <T> PMeshDeformer<T> registerMeshDeformer(PMeshDeformer<T> type)
		{
			return PLibRegistration.MeshDeformerReg.DEFORMERS.register(type.id(), type);
		}
	}

/**
 * Provides support for register resource event.
 */
	public static class RegisterResourceEvent extends Event implements IModBusEvent
	{
		private final Map<Identifier, ModelRegistration> models = new LinkedHashMap<>();

		/**
		 * Gets or creates the registration for a glTF model.
		 *
		 * @param model the model id relative to the glTF loader root.
		 * @return the model registration.
		 */
		public ModelRegistration model(Identifier model)
		{
			return model(model, PGltfModelLoader.INSTANCE.id());
		}

		/**
		 * Gets or creates the registration for a model using the specified loader.
		 *
		 * @param model the model id relative to the selected loader root.
		 * @param modelLoaderId the id of the loader for the model.
		 * @return the model registration.
		 */
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

		/**
		 * Builds all model registrations into the supplied cache after event listeners
		 * have added their texture references.
		 *
		 * @param registeredResources the cache receiving the completed resources.
		 */
		public void apply(Map<Identifier, PModelResource> registeredResources)
		{
			Objects.requireNonNull(registeredResources);
			Map<Identifier, PModelResource> mergedResources = new LinkedHashMap<>();
			this.models.forEach((model, registration) -> mergedResources.put(model, registration.build()));
			registeredResources.putAll(mergedResources);
		}

		/**
		 * Accumulates the material texture registrations for one model.
		 */
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

			/**
			 * Adds a material texture reference to this model.
			 *
			 * @param reference the reference stored in the model material.
			 * @param texture the texture resource id, relative to {@code textures} and without {@code .png}.
			 * @return this registration.
			 */
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
	
/**
 * Provides support for attachment registration event.
 */
	public static class AttachmentRegistrationEvent extends Event implements IModBusEvent
	{
		private final AttachmentRegistration registration = new AttachmentRegistration();
		
		/**
		 * Performs the registration operation.
		 * @return the value produced by this operation.
		 */
		public AttachmentRegistration registration()
		{
			return this.registration;
		}
		
/**
 * Provides support for attachment registration.
 */
		public static final class AttachmentRegistration
		{
			private final List<Runnable> actions = new ArrayList<>();
			
			/**
			 * Registers the living.
			 * @param item the item to use.
			 * @param definition the definition to use.
			 */
			public void registerLiving(ItemLike item, PLivingAttachmentDefinition definition)
			{
				this.actions.add(() -> PLivingAttachments.register(item.asItem(), definition));
			}
			
			/**
			 * Registers the global living.
			 * @param definition the definition to use.
			 */
			public void registerGlobalLiving(PLivingAttachmentDefinition definition)
			{
				this.actions.add(() -> PLivingAttachments.registerGlobal(definition));
			}
			
			/**
			 * Performs the apply operation.
			 */
			public void apply()
			{
				this.actions.forEach(Runnable :: run);
			}
		}
	}

/**
 * Provides support for player animation registration event.
 */
	public static class PlayerAnimationRegistrationEvent extends Event implements IModBusEvent
	{
		private final PlayerAnimationRegistration registration = new PlayerAnimationRegistration();

		/**
		 * Performs the registration operation.
		 * @return the value produced by this operation.
		 */
		public PlayerAnimationRegistration registration()
		{
			return this.registration;
		}

/**
 * Provides support for player animation registration.
 */
		public static final class PlayerAnimationRegistration
		{
			private final List<Runnable> actions = new ArrayList<>();

			/**
			 * Performs the register operation.
			 * @param id the id to use.
			 * @param definition the definition to use.
			 */
			public void register(Identifier id, PPlayerAnimationDefinition definition)
			{
				this.actions.add(() -> PPlayerAnimations.register(id, definition));
			}

			/**
			 * Performs the apply operation.
			 */
			public void apply()
			{
				this.actions.forEach(Runnable :: run);
			}
		}
	}

/**
 * Provides support for player animated attachment registration event.
 */
	public static class PlayerAnimatedAttachmentRegistrationEvent extends Event implements IModBusEvent
	{
		private final PlayerAnimatedAttachmentRegistration registration = new PlayerAnimatedAttachmentRegistration();

		/**
		 * Performs the registration operation.
		 * @return the value produced by this operation.
		 */
		public PlayerAnimatedAttachmentRegistration registration()
		{
			return this.registration;
		}

/**
 * Provides support for player animated attachment registration.
 */
		public static final class PlayerAnimatedAttachmentRegistration
		{
			private final List<Runnable> actions = new ArrayList<>();

			/**
			 * Performs the register operation.
			 * @param renderer the renderer to use.
			 */
			public void register(PPlayerAnimatedAttachmentRenderer renderer)
			{
				this.actions.add(() -> PPlayerAnimatedAttachments.register(renderer));
			}

			/**
			 * Performs the apply operation.
			 */
			public void apply()
			{
				this.actions.forEach(Runnable :: run);
			}
		}
	}
}
