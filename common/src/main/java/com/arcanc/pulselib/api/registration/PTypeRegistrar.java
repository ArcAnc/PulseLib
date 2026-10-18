/**
 * @author ArcAnc
 * Created at: 12.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.api.registration;

import com.arcanc.pulselib.content.model.animation.PAnimationChannelType;
import com.arcanc.pulselib.content.model.animation.PAnimationEventType;
import com.arcanc.pulselib.content.model.deformer.PMeshDeformer;
import com.arcanc.pulselib.content.registration.PLibRegistration;
import com.arcanc.pulselib.content.registration.PRegistry;

/**
 * Restricts common plugin registrations to the active channel, event or deformer phase.
 */
public class PTypeRegistrar extends PulseRegistrar
{
	/**
	 * Common registration phase accepted by a type registrar.
	 */
	public enum Kind
	{
		/**
		 * Accepts animation channel type declarations.
		 */
		CHANNELS,
		/**
		 * Accepts animation event type declarations.
		 */
		EVENTS,
		/**
		 * Accepts mesh deformer type declarations.
		 */
		DEFORMERS
	}

	private final Kind kind;
	private final PRegistry<PAnimationChannelType<?>> channels = new PRegistry<>();
	private final PRegistry<PAnimationEventType<?>> events = new PRegistry<>();
	private final PRegistry<PMeshDeformer<?>> deformers = new PRegistry<>();

	/**
	 * Creates a registrar restricted to one common type-registration phase.
	 *
	 * @param kind the kind
	 */
	public PTypeRegistrar(Kind kind)
	{
		this.kind = kind;
	}

	private void require(Kind expected)
	{
		checkOpen();
		if (this.kind != expected)
			throw new IllegalStateException("Cannot register " + expected + " during " + this.kind);
	}

	/**
	 * Registers an animation channel type in the channel phase and returns the registered type.
	 *
	 * @param <T> the value type carried by this contract
	 * @param type the registered animation channel type
	 * @return the registered channel type
	 */
	public <T> PAnimationChannelType<T> registerAnimationChannel(PAnimationChannelType<T> type)
	{
		require(Kind.CHANNELS);
		return this.channels.register(type.id(), type);
	}

	/**
	 * Registers an animation event type in the event phase and returns the registered type.
	 *
	 * @param <T> the value type carried by this contract
	 * @param type the registered animation event type
	 * @return the registered event type
	 */
	public <T> PAnimationEventType<T> registerAnimationEvent(PAnimationEventType<T> type)
	{
		require(Kind.EVENTS);
		return this.events.register(type.id(), type);
	}

	/**
	 * Registers a mesh deformer type in the deformer phase and returns the registered type.
	 *
	 * @param <T> the value type carried by this contract
	 * @param type the registered mesh deformer implementation
	 * @return the registered deformer type
	 */
	public <T> PMeshDeformer<T> registerMeshDeformer(PMeshDeformer<T> type)
	{
		require(Kind.DEFORMERS);
		return this.deformers.register(type.id(), type);
	}

	/**
	 * Applies the declarations collected for the active type registration phase.
	 */
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
