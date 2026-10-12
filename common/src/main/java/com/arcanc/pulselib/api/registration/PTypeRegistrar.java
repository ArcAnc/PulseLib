package com.arcanc.pulselib.api.registration;

import com.arcanc.pulselib.content.model.animation.PAnimationChannelType;
import com.arcanc.pulselib.content.model.animation.PAnimationEventType;
import com.arcanc.pulselib.content.model.deformer.PMeshDeformer;
import com.arcanc.pulselib.content.registration.PLibRegistration;
import com.arcanc.pulselib.content.registration.PRegistry;

public class PTypeRegistrar extends PulseRegistrar
{
	public enum Kind { CHANNELS, EVENTS, DEFORMERS, LEGACY }

	private final Kind kind;
	private final PRegistry<PAnimationChannelType<?>> channels = new PRegistry<>();
	private final PRegistry<PAnimationEventType<?>> events = new PRegistry<>();
	private final PRegistry<PMeshDeformer<?>> deformers = new PRegistry<>();

	public PTypeRegistrar(Kind kind)
	{
		this.kind = kind;
	}

	private void require(Kind expected)
	{
		checkOpen();
		if (this.kind != Kind.LEGACY && this.kind != expected)
			throw new IllegalStateException("Cannot register " + expected + " during " + this.kind);
	}

	public <T> PAnimationChannelType<T> registerAnimationChannel(PAnimationChannelType<T> type)
	{
		require(Kind.CHANNELS);
		return this.channels.register(type.id(), type);
	}

	public <T> PAnimationEventType<T> registerAnimationEvent(PAnimationEventType<T> type)
	{
		require(Kind.EVENTS);
		return this.events.register(type.id(), type);
	}

	public <T> PMeshDeformer<T> registerMeshDeformer(PMeshDeformer<T> type)
	{
		require(Kind.DEFORMERS);
		return this.deformers.register(type.id(), type);
	}

	public void apply()
	{
		checkOpen();
		PLibRegistration.AnimationChannelReg.CHANNEL_TYPES.validateAll(this.channels.entries());
		PLibRegistration.AnimationEventReg.EVENT_TYPES.validateAll(this.events.entries());
		PLibRegistration.MeshDeformerReg.DEFORMERS.validateAll(this.deformers.entries());
		PLibRegistration.AnimationChannelReg.CHANNEL_TYPES.registerAll(this.channels.entries());
		PLibRegistration.AnimationEventReg.EVENT_TYPES.registerAll(this.events.entries());
		PLibRegistration.MeshDeformerReg.DEFORMERS.registerAll(this.deformers.entries());
		close();
	}
}
