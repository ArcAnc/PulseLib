package com.arcanc.pulselib.content.event;

import com.arcanc.pulselib.api.registration.*;
import com.arcanc.pulselib.content.model.animation.PAnimationChannelType;
import com.arcanc.pulselib.content.model.animation.PAnimationEventType;
import com.arcanc.pulselib.content.model.deformer.PMeshDeformer;
import com.arcanc.pulselib.content.model.resource.PModelResource;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

import java.util.IdentityHashMap;
import java.util.Map;

/** NeoForge compatibility adapter. New integrations should use @PulsePlugin. */
@Deprecated
public class PulseLibEvents
{
	public static class TypeRegistrationEvent extends Event implements IModBusEvent
	{
		private final PTypeRegistrar registrar = new PTypeRegistrar(PTypeRegistrar.Kind.LEGACY);

		public <T> PAnimationChannelType<T> registerAnimationChannel(PAnimationChannelType<T> type)
		{
			return this.registrar.registerAnimationChannel(type);
		}

		public <T> PAnimationEventType<T> registerAnimationEvent(PAnimationEventType<T> type)
		{
			return this.registrar.registerAnimationEvent(type);
		}

		public <T> PMeshDeformer<T> registerMeshDeformer(PMeshDeformer<T> type)
		{
			return this.registrar.registerMeshDeformer(type);
		}

		public void apply()
		{
			this.registrar.apply();
		}
	}

	public static class RegisterResourceEvent extends Event implements IModBusEvent
	{
		private final PModelRegistrar registrar = new PModelRegistrar();
		private final Map<PModelRegistrar.ModelRegistration, ModelRegistration> wrappers = new IdentityHashMap<>();

		public ModelRegistration model(PModelData data)
		{
			return wrap(this.registrar.model(data));
		}

		public ModelRegistration model(ResourceLocation id)
		{
			return wrap(this.registrar.model(id));
		}

		public ModelRegistration model(ResourceLocation id, ResourceLocation loader)
		{
			return wrap(this.registrar.model(id, loader));
		}

		private ModelRegistration wrap(PModelRegistrar.ModelRegistration registration)
		{
			return this.wrappers.computeIfAbsent(registration, ModelRegistration::new);
		}

		public void apply(Map<ResourceLocation, PModelResource> target)
		{
			this.registrar.apply(target);
		}

		public static final class ModelRegistration
		{
			private final PModelRegistrar.ModelRegistration delegate;

			private ModelRegistration(PModelRegistrar.ModelRegistration delegate)
			{
				this.delegate = delegate;
			}

			public ModelRegistration texture(String reference, ResourceLocation texture)
			{
				this.delegate.texture(reference, texture);
				return this;
			}
		}
	}

	public static class AttachmentRegistrationEvent extends Event implements IModBusEvent
	{
		private final AttachmentRegistration registration = new AttachmentRegistration();

		public AttachmentRegistration registration() { return this.registration; }

		public static final class AttachmentRegistration extends PAttachmentRegistrar { }
	}

	public static class PlayerAnimationRegistrationEvent extends Event implements IModBusEvent
	{
		private final PlayerAnimationRegistration registration = new PlayerAnimationRegistration();

		public PlayerAnimationRegistration registration() { return this.registration; }

		public static final class PlayerAnimationRegistration extends PPlayerAnimationRegistrar { }
	}

	public static class PlayerAnimatedAttachmentRegistrationEvent extends Event implements IModBusEvent
	{
		private final PlayerAnimatedAttachmentRegistration registration = new PlayerAnimatedAttachmentRegistration();

		public PlayerAnimatedAttachmentRegistration registration() { return this.registration; }

		public static final class PlayerAnimatedAttachmentRegistration extends PAnimatedAttachmentRegistrar { }
	}
}
