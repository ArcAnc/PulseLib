/**
 * @author ArcAnc
 * Created at: 26.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1;

import com.arcanc.pulselib.api.registration.PAnimatedAttachmentRegistrar;
import com.arcanc.pulselib.api.registration.PPlayerAnimationRegistrar;
import com.arcanc.pulselib.content.animatable.AnimManagerKey;
import com.arcanc.pulselib.content.animatable.ControllerState;
import com.arcanc.pulselib.content.animatable.PAnimationController;
import com.arcanc.pulselib.content.model.animation.PPoseEasing;
import com.arcanc.pulselib.content.model.animation.PRawAnimation;
import com.arcanc.pulselib.content.model.animation.PTransitionInterruptionPolicy;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationAnchor;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationBlendMode;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationDefinition;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationInstance;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import com.arcanc.pulselib.content.player.animation.PPlayerPart;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAnimatedAttachmentContext;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAnimatedAttachmentRenderer;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAnimatedAttachments;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.data.gecko.MolangParser;
import com.arcanc.pulselib.data.gecko.PGeckoModelLoader;
import com.arcanc.pulselib.util.PLibDatabase;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Actual Player owners and plugin registrations; no native player motion or camera hooks are replaced here. */
public final class Stage51PlayerProbe
{
	public static final PModelData MODEL = PModelData.entity(PLibDatabase.rl("stage43b2_owner"), PGeckoModelLoader.INSTANCE.id());
	private static final Identifier FIRST = PLibDatabase.rl("stage51_first");
	private static final Identifier SECOND = PLibDatabase.rl("stage51_second");
	private static final PPlayerAnimationAnchor ANCHOR = new PPlayerAnimationAnchor(PLibDatabase.rl("stage51_socket"));
	private static final PRawAnimation SHIFT = PRawAnimation.begin().thenLoop("shift").build();
	private static final Map<PPlayerAnimationInstance, Float> QUERY_TIMES = new IdentityHashMap<>();
	private static final Map<PPlayerAnimationInstance, MolangParser.Context> QUERY_CONTEXTS = new IdentityHashMap<>();
	private static int querySamples;
	private static final Attachment RENDERER = new Attachment();
	private static final PPlayerAnimationDefinition FIRST_DEFINITION = definition(0.8f, 0.25f, 0);
	private static final PPlayerAnimationDefinition SECOND_DEFINITION = definition(0.6f, 0.5f, 1);
	private static final Method TICK = tickMethod();
	private static boolean probing;
	private static int suites;
	private static int factories;
	private static int attachmentCalls;
	private static @Nullable PBakedModel previousModel;

	private Stage51PlayerProbe()
	{
	}

	public static void register()
	{
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Stage51PlayerProbe::shutdown);
	}

	public static void registerAnimations(PPlayerAnimationRegistrar registrar)
	{
		registrar.register(FIRST, FIRST_DEFINITION);
		registrar.register(FIRST, FIRST_DEFINITION);
		registrar.register(SECOND, SECOND_DEFINITION);
	}

	public static void registerAttachments(PAnimatedAttachmentRegistrar registrar)
	{
		registrar.register(RENDERER);
		registrar.register(RENDERER);
	}

	private static PPlayerAnimationDefinition definition(float weight, float boneWeight, int priority)
	{
		return PPlayerAnimationDefinition.builder(MODEL).
				when(player -> false).
				bind(PPlayerPart.HEAD, "probe").
				mask(PPlayerPart.HEAD).
				weight((player, partialTick) -> probing ? weight : 0).
				partWeight(PPlayerPart.HEAD, 0.5f).
				boneWeight("probe", boneWeight).
				anchor(ANCHOR, "probe").
				blendMode(PPlayerAnimationBlendMode.ADDITIVE_LOCAL).
				priority(priority).
				crossfade(0, PPoseEasing.LINEAR, PTransitionInterruptionPolicy.RESTART).
				controllers(registrar -> registrar.add("shift", () ->
				{
					factories++;
					return state -> ControllerState.PLAY;
				})).
				populateMolangContext((player, instance, controller, context, partialTick) ->
				{
					float seconds = MolangParser.evaluate("query.anim_time", context);
					float expected = controller.getInterpolatedTime(partialTick) / 20;
					check(Math.abs(seconds - expected) < 0.000001f, "Player query.anim_time must use seconds exactly once");
					QUERY_TIMES.put(instance, seconds);
					QUERY_CONTEXTS.put(instance, context);
					querySamples++;
				}).
				build();
	}

	public static void tick(int tick)
	{
		if (tick == 80 || tick == 210)
			verify(tick);
	}

	private static void verify(int tick)
	{
		var mc = Minecraft.getInstance();
		PBakedModel model = MODEL.getModel();
		check(model != null, "Loaded real Gecko player-core fixture");
		if (suites == 1)
			check(model != previousModel, "Resource reload replaces the sampled model");
		previousModel = model;
		var profile = new GameProfile(UUID.fromString("12345678-1234-4567-8123-456789abcdef"), "Stage51Owner");
		var remote = new RemotePlayer(mc.level, profile);
		Player[] players = {mc.player, remote};
		Identifier[] ids = {FIRST, SECOND};
		var instances = new PPlayerAnimationInstance[4];
		var controllers = new PAnimationController<?>[4];
		Set<Object> managers = Collections.newSetFromMap(new IdentityHashMap<>());
		Set<Object> uniqueControllers = Collections.newSetFromMap(new IdentityHashMap<>());
		QUERY_TIMES.clear();
		QUERY_CONTEXTS.clear();
		probing = true;
		try
		{
			for (int player = 0; player < 2; player++)
				for (int definition = 0; definition < 2; definition++)
				{
					int index = player * 2 + definition;
					var instance = PPlayerAnimations.getInstance(players[player], ids[definition]);
					check(instance != null, "Plugin-installed player definition");
					instances[index] = instance;
					var manager = instance.getAnimationManager(new AnimManagerKey(0));
					managers.add(manager);
					var controller = manager.getControllers().get("shift");
					check(controller != null, "Canonical lazy player controllers");
					uniqueControllers.add(controller);
					controllers[index] = controller;
					controller.stop();
					controller.play(SHIFT);
					for (int step = 0; step < 3 + index * 4; step++)
						tick(instance, true);
					var frame = instance.sampleFrame(0.25f);
					check(frame != null && Math.abs(frame.modelTransform("probe").translation().x - 1) < 0.000001f, "Real Gecko clip passes through the single pose evaluator");
					int queries = querySamples;
					check(frame == instance.sampleFrame(0.25f) && querySamples == queries, "Frame reused for the same partial tick");
				}
			Set<MolangParser.Context> contexts = Collections.newSetFromMap(new IdentityHashMap<>());
			contexts.addAll(QUERY_CONTEXTS.values());
			check(contexts.size() == 4, "Each player/definition samples its own Molang context");
			check(managers.size() == 4 && uniqueControllers.size() == 4 && QUERY_TIMES.size() == 4, "Two UUIDs times two definitions own four independent managers/controllers/contexts");
			for (int index = 1; index < 4; index++)
				check(Math.abs(controllers[index].getTime() - controllers[0].getTime()) > 1, "Independent controller clocks");
			var replacement = new RemotePlayer(mc.level, profile);
			check(PPlayerAnimations.getInstance(replacement, FIRST) == instances[2] && instances[2].player() == replacement, "Same UUID refreshes the native Player owner without replacing its animation instance");
			tick(instances[0], false);
			check(controllers[0].isPlaying(), "Deactivation fades contribution without stopping the canonical controller");
			tick(instances[0], true);
			tick(instances[0], true);
			check(FIRST_DEFINITION.mask().equals(Set.of(PPlayerPart.HEAD)) && Math.abs(FIRST_DEFINITION.weight(mc.player, PPlayerPart.HEAD, 0) - 0.4f) < 0.000001f, "Canonical mask and combined part weight API");
			check(PPlayerAnimations.isPartAnimating(mc.player, PPlayerPart.HEAD, 0) && !PPlayerAnimations.isPartAnimating(mc.player, PPlayerPart.BODY, 0), "Dynamic application respects the selected parts");
			var part = new ModelPart(List.of(), Map.of());
			part.setPos(3, 4, 5);
			var nativeModel = mc.getEntityRenderDispatcher().getPlayerRenderer(mc.player).getModel();
			var original = PPlayerAnimations.applyPart(mc.player, nativeModel, PPlayerPart.HEAD, part, 0);
			check(Math.abs(part.x - 7) < 0.00001f && part.y == 4 && part.z == 5, "Independent additive definitions multiply definition/part/bone weights once");
			original.restore();
			check(part.x == 3 && part.y == 4 && part.z == 5, "ModelPart pose snapshot restores the caller");
			int before = attachmentCalls;
			var poses = new PoseStack();
			PPlayerAnimatedAttachments.renderThirdPerson(mc.player, poses, mc.gameRenderer.getFeatureRenderDispatcher().getSubmitNodeStorage(), 0xF000F0, 0);
			check(attachmentCalls - before == 2 && poses.isEmpty(), "Duplicate attachment registration is idempotent for both definition anchors and restores pose");
			var previous = instances[0];
			PPlayerAnimations.cleanUp();
			check(PPlayerAnimations.get(FIRST) == FIRST_DEFINITION && PPlayerAnimations.getInstance(mc.player, FIRST) != previous, "Cleanup drops instances while retaining plugin definitions");
			suites++;
			PLibDatabase.LOGGER.info("STAGE 5.1 PLAYER CORE PASS: tick={}, suites={}, UUIDs=2, definitions=2, distinctManagers={}, distinctControllers={}, queryContexts={}, sampledSeconds={}, factories={}, attachmentCalls={}, weightedHeadX=7, ownerRefresh/cache/reload/cleanup PASS", tick, suites, managers.size(), uniqueControllers.size(), contexts.size(), List.of(QUERY_TIMES.get(instances[0]), QUERY_TIMES.get(instances[1]), QUERY_TIMES.get(instances[2]), QUERY_TIMES.get(instances[3])), factories, attachmentCalls);
		}
		finally
		{
			probing = false;
			PPlayerAnimations.cleanUp();
		}
	}

	/** Exercise the package-private owner tick without exposing a fixture API in production. */
	private static Method tickMethod()
	{
		try
		{
			Method method = PPlayerAnimationInstance.class.getDeclaredMethod("tick", boolean.class);
			method.setAccessible(true);
			return method;
		}
		catch (ReflectiveOperationException exception)
		{
			throw new ExceptionInInitializerError(exception);
		}
	}

	private static void tick(PPlayerAnimationInstance instance, boolean active)
	{
		try
		{
			TICK.invoke(instance, active);
		}
		catch (ReflectiveOperationException exception)
		{
			throw new AssertionError("Player instance tick failed", exception);
		}
	}

	private static void shutdown(GameShuttingDownEvent event)
	{
		check(suites == 2, "Both player-core suites before/after resource reload must complete");
		PLibDatabase.LOGGER.info("STAGE 5.1 PLAYER CORE SHUTDOWN PASS");
	}

	private static void check(boolean value, String message)
	{
		if (!value)
			throw new AssertionError("Stage 5.1: " + message);
	}

	private static final class Attachment implements PPlayerAnimatedAttachmentRenderer
	{
		@Override
		public PPlayerAnimationAnchor anchor()
		{
			return ANCHOR;
		}

		@Override
		public void render(PPlayerAnimatedAttachmentContext context)
		{
			check(probing && context.anchor().equals(ANCHOR), "Only the isolated core check invokes this callback");
			attachmentCalls++;
		}
	}
}
