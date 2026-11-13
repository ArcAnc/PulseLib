/**
 * @author ArcAnc
 * Created at: 13.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.porting;

import com.arcanc.pulselib.api.plugin.IPulseCommonPlugin;
import com.arcanc.pulselib.api.plugin.PulsePlugin;
import com.arcanc.pulselib.api.registration.PTypeRegistrar;
import com.arcanc.pulselib.content.animatable.AnimManagerKey;
import com.arcanc.pulselib.content.animatable.ControllerState;
import com.arcanc.pulselib.content.animatable.PAnimatable;
import com.arcanc.pulselib.content.animatable.PAnimationManager;
import com.arcanc.pulselib.content.model.animation.PAnimationChannel;
import com.arcanc.pulselib.content.registration.PLibRegistration;
import com.arcanc.pulselib.plugin.PulsePluginCandidate;
import com.arcanc.pulselib.plugin.PulsePluginManager;
import net.minecraft.SharedConstants;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Exercises actual common registration with the launcher's strict client-class rejection. */
public final class IntegrationChecks
{
	private static final List<String> CALLS = new ArrayList<>();
	private static @Nullable PTypeRegistrar closedScope;

	public static void main(String[] args)
	{
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		PLibRegistration.init();
		check(PLibRegistration.AnimationChannelReg.CHANNEL_TYPES.entries().size() == 3, "Built-in channels");
		check(PLibRegistration.AnimationEventReg.EVENT_TYPES.entries().size() == 5, "Built-in events");
		check(PLibRegistration.MeshDeformerReg.DEFORMERS.entries().size() == 7, "Built-in deformers");
		System.out.println("PASS real built-in registration without client/backend classes");
		plugins();
		managers();
		System.out.println("COMMON INTEGRATION CHECKS PASSED; full loader startup and native ItemStack Mixin remain separate checks.");
	}

	private static void plugins()
	{
		var a = new PulsePluginCandidate(FirstPlugin.class.getName(), "pulselib", PulsePlugin.Side.SERVER);
		var b = new PulsePluginCandidate(SecondPlugin.class.getName(), "pulselib", PulsePlugin.Side.BOTH);
		var client = new PulsePluginCandidate("net.minecraft.client.ForbiddenPulsePlugin", "pulselib", PulsePlugin.Side.CLIENT);
		var manager = new PulsePluginManager(() -> List.of(b, client, a, b), false, IntegrationChecks.class.getClassLoader());
		manager.registerCommon();
		manager.registerCommon();
		check(manager.pluginIds().equals(List.of(id("a"), id("b"))), "Plugin ordering, deduplication and side filtering");
		check(CALLS.equals(List.of("channels:a", "channels:b", "events:a", "events:b", "deformers:a", "deformers:b")),
				"Real manager phase order and once-only callbacks");
		check(PLibRegistration.AnimationChannelReg.CHANNEL_TYPES.get(id("fixture_channel")).isPresent(), "Applied custom channel");
		expectFailure(() -> closedScope.registerAnimationChannel(channel()));
		expectFailure(manager::registerClient);
		var bad = new PulsePluginCandidate(DuplicatePlugin.class.getName(), "pulselib", PulsePlugin.Side.SERVER);
		var duplicate = new PulsePluginManager(() -> List.of(a, bad), false, IntegrationChecks.class.getClassLoader());
		expectFailure(duplicate::discover);
		expectFailure(duplicate::discover);
		System.out.println("PASS real PulsePluginManager discovery, side-before-load, registration, scoped registrars and retained duplicate failure");
	}

	private static void managers()
	{
		Owner owner = new Owner();
		check(owner.registrations == 0, "Owner constructor must not register controllers");
		var first = owner.manager.getControllers().get("fixture");
		check(first != null && owner.registrations == 1, "Lazy initialization after owner construction");
		check(owner.manager.getControllers().get("fixture") == first && owner.registrations == 1, "Stable controller identity");
		check(new Owner().manager.getControllers().get("fixture") != first, "Independent managers");
		System.out.println("PASS real animation manager/controller lazy initialization and owner isolation (no model/GPU)");
	}

	private static PAnimationChannel.Vector3fChannelType channel()
	{
		return new PAnimationChannel.Vector3fChannelType(id("fixture_channel"), new Vector3f(), false);
	}

	private static Identifier id(String path)
	{
		return Identifier.fromNamespaceAndPath("porting_fixture", path);
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
			throw new AssertionError(message);
	}

	private static void expectFailure(Runnable action)
	{
		try
		{
			action.run();
		}
		catch (IllegalStateException expected)
		{
			return;
		}
		throw new AssertionError("Expected IllegalStateException");
	}

	@PulsePlugin(PulsePlugin.Side.SERVER)
	public static class FirstPlugin implements IPulseCommonPlugin
	{
		@Override
		public Identifier getPluginId()
		{
			return id("a");
		}

		@Override
		public void registerAnimationChannels(PTypeRegistrar registrar)
		{
			CALLS.add("channels:a");
			registrar.registerAnimationChannel(channel());
		}

		@Override
		public void registerAnimationEvents(PTypeRegistrar registrar)
		{
			CALLS.add("events:a");
			expectFailure(() -> registrar.registerAnimationChannel(channel()));
		}

		@Override
		public void registerDeformers(PTypeRegistrar registrar)
		{
			CALLS.add("deformers:a");
			closedScope = registrar;
		}
	}

	@PulsePlugin(PulsePlugin.Side.BOTH)
	public static final class SecondPlugin implements IPulseCommonPlugin
	{
		@Override
		public Identifier getPluginId()
		{
			return id("b");
		}

		@Override
		public void registerAnimationChannels(PTypeRegistrar registrar)
		{
			CALLS.add("channels:b");
		}

		@Override
		public void registerAnimationEvents(PTypeRegistrar registrar)
		{
			CALLS.add("events:b");
		}

		@Override
		public void registerDeformers(PTypeRegistrar registrar)
		{
			CALLS.add("deformers:b");
		}
	}

	@PulsePlugin(PulsePlugin.Side.SERVER)
	public static final class DuplicatePlugin extends FirstPlugin
	{
	}

	private static final class Owner implements PAnimatable<Owner>
	{
		private final PAnimationManager<Owner> manager = new PAnimationManager<>(this, new AnimManagerKey(42));
		private int registrations;
		private boolean ready;

		private Owner()
		{
			this.ready = true;
		}

		@Override
		public PAnimationManager<Owner> getAnimationManager(AnimManagerKey key)
		{
			return this.manager.getManager(key);
		}

		@Override
		public void registerAnimationControllers(PAnimationManager.PAnimationRegistrar<Owner> registrar)
		{
			check(this.ready, "Registration before owner construction");
			this.registrations++;
			registrar.add("fixture", () -> state -> ControllerState.STOP);
		}
	}
}
