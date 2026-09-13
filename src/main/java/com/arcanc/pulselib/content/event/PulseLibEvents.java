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
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationDefinition;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAnimatedAttachmentRenderer;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAnimatedAttachments;
import com.arcanc.pulselib.content.registration.PLibRegistration;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PulseLibEvents
{
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

	public static class RegisterTextureEvent extends Event implements IModBusEvent
	{
		private final Set<Identifier> registeredTextures;
		
		/**
		 * Creates an instance of the enclosing type.
		 * @param registeredTextures the registered textures to use.
		 */
		public RegisterTextureEvent(Set<Identifier> registeredTextures)
		{
			this.registeredTextures = registeredTextures;
		}
		
		/**
		 * Adds the texture location.
		 * @param textureLocation the texture location to use.
		 * @return the value produced by this operation.
		 */
		public RegisterTextureEvent addTextureLocation(Identifier textureLocation)
		{
			this.registeredTextures.add(textureLocation);
			return this;
		}
	}
	
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
