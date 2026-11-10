/**
 * @author ArcAnc
 * Created at: 12.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test;

import com.arcanc.pulselib.content.animatable.AnimManagerKey;
import com.arcanc.pulselib.content.animatable.ControllerState;
import com.arcanc.pulselib.content.animatable.PAnimatable;
import com.arcanc.pulselib.content.animatable.PAnimationManager;
import com.arcanc.pulselib.content.animatable.instance.InstanceAnimationManager;
import com.arcanc.pulselib.content.animatable.singleton.SingletonAnimationManager;
import com.arcanc.pulselib.content.event.PulseRuntime;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import com.arcanc.pulselib.content.registration.player.PPlayerBallDemo;
import com.arcanc.pulselib.util.PLibDatabase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import org.jspecify.annotations.Nullable;

import java.util.Map;

import static com.arcanc.pulselib.test.RegressionChecks.check;

/** Canonical SmokeHarness explicit/natural lifecycle slice; graphics and other suites remain separate. */
public final class SmokeLifecycleHarness
{
	private static @Nullable SmokeResources naturalResources;
	private static long disconnectDeadline;
	private static int naturalHooks;
	private static boolean explicitCleanup;
	private static boolean naturalDeleted;
	private static boolean completed;
	private static @Nullable String explicitSnapshot;
	private static @Nullable String naturalSnapshot;

	private SmokeLifecycleHarness()
	{
	}

	static void begin()
	{
		Minecraft mc = Minecraft.getInstance();
		DemoChecks.world(mc.getSingleplayerServer(), true);
		PBakedModel gecko = ClientTestPlugin.GECKO.getModel();
		SmokeLifecycleWorkload.draw();
		runExplicitCleanup(gecko);
		SmokeLifecycleWorkload.draw();
		seedManagers(gecko);
		PPlayerBallDemo.play(mc.player);
		naturalResources = SmokeResources.capture();
		naturalSnapshot = naturalResources.nativeResources().toString();
		disconnectDeadline = System.nanoTime() + 15_000_000_000L;
		mc.disconnect(new TitleScreen(), false);
	}

	static boolean frame()
	{
		if (completed)
			return true;
		if (naturalHooks == 0 && System.nanoTime() < disconnectDeadline)
			return false;
		check(naturalHooks > 0, "Natural disconnect did not invoke a loader lifecycle hook");
		if (!naturalDeleted)
			naturalResources.assertDeleted("natural disconnect");
		check(naturalDeleted, "Natural disconnect must complete resource assertions inside the actual cleanup callback");
		naturalResources = null;
		completed = true;
		PLibDatabase.LOGGER.info("PULSE TEST NATURAL DISCONNECT PASS: {} loader hook calls", naturalHooks);
		return true;
	}

	static void explicitOnly()
	{
		SmokeLifecycleWorkload.draw();
		runExplicitCleanup(ClientTestPlugin.GECKO.getModel());
	}

	private static void runExplicitCleanup(PBakedModel model)
	{
		seedManagers(model);
		SmokeResources resources = SmokeResources.capture();
		explicitSnapshot = resources.nativeResources().toString();
		explicitCleanup = true;
		try
		{
			PulseRuntime.disconnected();
		}
		finally
		{
			explicitCleanup = false;
		}
		resources.assertDeleted("explicit cleanup");
		PLibDatabase.LOGGER.info("PULSE TEST EXPLICIT CLEANUP PASS: {}", explicitSnapshot);
	}

	private static void seedManagers(PBakedModel model)
	{
		Dummy sentinel = new Dummy();
		sentinel.manager.bindModel(model);
		InstanceAnimationManager.addManager(sentinel.manager);
		SingletonAnimationManager.getManager(AnimManagerKey.ofObject(sentinel), sentinel);
	}

	public static boolean observeDisconnectHook()
	{
		if (!explicitCleanup && naturalResources != null && !completed)
		{
			naturalHooks++;
			if (Boolean.getBoolean("pulselib.test.skipDisconnectHook"))
			{
				PLibDatabase.LOGGER.info("PULSE TEST NEGATIVE CONTROL: loader cleanup hook suppressed");
				return true;
			}
		}
		return false;
	}

	/** Assert before any title-screen allocation can recycle deleted OpenGL names. */
	public static void afterDisconnectHook()
	{
		if (!explicitCleanup && naturalResources != null && !completed)
		{
			naturalResources.assertDeleted("natural disconnect");
			naturalDeleted = true;
			PLibDatabase.LOGGER.info("PULSE TEST NATURAL RESOURCES PASS: {}", naturalSnapshot);
		}
	}

	static Map<String, Object> result()
	{
		return Map.of("completed", completed, "hooks", naturalHooks,
				"explicitResources", explicitSnapshot, "naturalResources", naturalSnapshot);
	}

	private static final class Dummy implements PAnimatable<Dummy>
	{
		private final PAnimationManager<Dummy> manager = new InstanceAnimationManager<>(this);

		@Override
		public PAnimationManager<Dummy> getAnimationManager(AnimManagerKey key)
		{
			return this.manager;
		}

		@Override
		public void registerAnimationControllers(PAnimationManager.PAnimationRegistrar<Dummy> registrar)
		{
			registrar.add("idle", () -> state -> ControllerState.PLAY);
		}
	}
}
