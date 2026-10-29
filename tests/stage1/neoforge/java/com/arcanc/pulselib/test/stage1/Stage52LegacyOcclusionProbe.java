/**
 * @author ArcAnc
 * Created at: 29.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1;

import com.arcanc.pulselib.api.registration.PAttachmentRegistrar;
import com.arcanc.pulselib.api.registration.PModelRegistrar;
import com.arcanc.pulselib.api.registration.PPlayerAnimationRegistrar;
import com.arcanc.pulselib.content.animatable.ControllerState;
import com.arcanc.pulselib.content.model.animation.PPoseEasing;
import com.arcanc.pulselib.content.model.animation.PRawAnimation;
import com.arcanc.pulselib.content.model.animation.PTransitionInterruptionPolicy;
import com.arcanc.pulselib.content.model.baked.PMeshRenderMaterial;
import com.arcanc.pulselib.content.model.textures.PAlphaMode;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationDefinition;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import com.arcanc.pulselib.content.player.animation.PPlayerPart;
import com.arcanc.pulselib.content.player.deformer.PPlayerGeometryRenderState;
import com.arcanc.pulselib.content.renderer.PWorldRenderPass;
import com.arcanc.pulselib.content.renderer.gl.PGlResourceRegistry;
import com.arcanc.pulselib.content.renderer.plan.PTransparencyMode;
import com.arcanc.pulselib.content.renderer.plan.PFrameCompiler;
import com.arcanc.pulselib.content.renderer.plan.PMeshHandle;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.content.renderer.base.PEntityRenderState;
import com.arcanc.pulselib.content.renderer.base.PLivingAttachmentRenderState;
import com.arcanc.pulselib.content.renderer.base.PWorldRenderState;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.data.gecko.PGeckoModelLoader;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.PResourceCache;
import com.arcanc.pulselib.util.attachments.PLivingAttachmentDefinition;
import com.arcanc.pulselib.util.attachments.PLivingAttachments;
import com.arcanc.pulselib.util.attachments.PTransform;
import com.arcanc.pulselib.util.attachments.humanoid.PHumanoidAnchors;
import com.arcanc.pulselib.util.attachments.humanoid.PHumanoidBindings;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Real legacy layer draws: global and item armor definitions against independent native world controls. */
public final class Stage52LegacyOcclusionProbe
{
	public static final int VARIANTS = 10;
	private static final String[] KINDS = {"bare", "global-opaque", "global-alpha", "item-opaque", "item-alpha"};
	private static final PModelData RIG = PModelData.direct(PLibDatabase.rl("player/regression/depth_motion"), PGeckoModelLoader.INSTANCE.id());
	private static final Identifier ANIMATION = PLibDatabase.rl("stage52b4c/legacy");
	private static final Map<String, Control> CONTROLS = new HashMap<>();
	private static final int[][] RESOLVERS = new int[6][VARIANTS];
	private static final int[][] SUBMISSIONS = new int[6][VARIANTS];
	private static int variant = -1;
	private static int phase;
	private static int control;
	private static final int[][] OIT_PASSES = new int[6][VARIANTS];
	private static int configured;
	private static int pixelWitnesses;
	private static int motionWitnesses;
	private static int extractionWitnesses;
	private static int legacyTargetWitnesses;
	private static int policyWitnesses;

	private Stage52LegacyOcclusionProbe()
	{
	}

	public static void registerModels(PModelRegistrar registrar)
	{
		registrar.model(RIG).texture("0", Stage42aWorldStageProbe.WHITE);
	}

	public static void registerAnimations(PPlayerAnimationRegistrar registrar)
	{
		registrar.register(ANIMATION, PPlayerAnimationDefinition.builder(RIG).
				when(player -> variant >= 0 && player.getId() == 0x526a01).
				bind(PPlayerPart.BODY, "body").meshAttachment("stage52b4c_no_automatic_mesh").
				crossfade(0, PPoseEasing.LINEAR, PTransitionInterruptionPolicy.FROM_CURRENT).
				controllers(controllers -> controllers.add("depth", () -> state -> state.controller().getState())).build());
	}

	public static void registerAttachments(PAttachmentRegistrar registrar)
	{
		for (int source = 0; source < 2; source++)
		{
			int selected = source;
			var definition = new PLivingAttachmentDefinition(Stage52PlayerOcclusionProbe.MODEL,
					(entity, slot, stack) -> variant >= 0 && entity.getId() == 0x526a01 && kind() > 0 &&
							(kind() >= 3 ? 1 : 0) == selected && (selected == 0 || slot == EquipmentSlot.CHEST),
					List.of(PHumanoidBindings.bind(PHumanoidAnchors.BODY, "probe",
							new PTransform(new Vector3f(0, -0.85f, -0.4f), new Quaternionf(), new Vector3f(0.7f, 0.4f, 0.15f)))),
					(entity, stack, bone, mesh, inherited, tick) ->
					{
						check(selected == 0 ? stack.isEmpty() : stack.is(Items.DIAMOND_CHESTPLATE), "Extracted legacy stack belongs to its source");
						RESOLVERS[phase][variant]++;
						var context = inherited.withColor(alpha() ? 0x8000FF00 : 0xFF00FF00).withEmissive(true).
								withAlphaMode(alpha() ? PAlphaMode.TRANSLUCENT : PAlphaMode.OPAQUE);
						var type = PMeshRenderMaterial.resolve(mesh, context).resolveRenderType(context, PResourceCache.ATLAS_LOCATION);
						check(PRenderTypes.isTransparent(type) == alpha(),
								"Material transparency remains independent of the explicit submission policy");
						return context;
					}, source == 1);
			if (source == 0)
				registrar.registerGlobalLiving(definition);
			else
				registrar.registerLiving(Items.DIAMOND_CHESTPLATE, definition);
		}
	}

	public static boolean ready()
	{
		var model = RIG.getModel();
		if (model == null)
			return false;
		var clip = model.animations().get("depth");
		check(clip != null && clip.length() == 20 && clip.boneAnimations().containsKey("body"), "Decoded owner animation exists");
		return true;
	}

	public static void configure(RemotePlayer player, int value, int mode, int currentControl)
	{
		phase = mode;
		control = currentControl;
		if (variant != value)
		{
			variant = value;
			configured++;
			if (value == 0)
				verifyPolicies();
			var handle = PPlayerAnimations.getHandle(player, ANIMATION);
			handle.stopAll();
			player.setItemSlot(EquipmentSlot.CHEST, kind() >= 3 ? new ItemStack(Items.DIAMOND_CHESTPLATE) : ItemStack.EMPTY);
			check(handle.play("depth", PRawAnimation.begin().thenLoop("depth").build()), "Start real rig animation");
		}
		var controller = PPlayerAnimations.getHandle(player, ANIMATION).controller("depth");
		check(controller != null, "Owner controller exists");
		controller.seek(RIG.getModel(), pose() * 10);
		controller.pause();
		check(controller.getState() == ControllerState.PAUSE && controller.getInterpolatedTime(0.5f) == pose() * 10, "Hold the decoded pose across controls");
		check(kind() < 3 || PLivingAttachments.hidesVanillaArmor(player.getItemBySlot(EquipmentSlot.CHEST), EquipmentSlot.CHEST, player),
				"Item definition suppresses native armor");
	}

	public static void verifyExtracted(LevelRenderState state, int control)
	{
		if (control != 1)
			return;
		check(state.entityRenderStates.stream().noneMatch(owner -> owner instanceof PEntityRenderState<?>), "Player-only control culls Pulse entity");
		var owner = state.entityRenderStates.stream().filter(value -> value instanceof AvatarRenderState avatar && avatar.id == 0x526a01).findFirst().orElseThrow();
		var entries = ((PLivingAttachmentRenderState)owner).pulselib$getAttachments();
		check(kind() == 0 ? entries == null : entries != null && entries.size() == 1, "Only the requested legacy definition is extracted");
		var geometry = ((PPlayerGeometryRenderState)owner).pulselib$getPlayerGeometry();
		check(geometry != null && !geometry.hasArmor(), "Native armor must not supply these pixels");
		check(kind() == 0 || PWorldRenderPass.requiresSeparateTargets(state), "Legacy snapshot retains native targets through the world gate");
		if (kind() > 0 && !((PWorldRenderState)owner).pulselib$requiresSeparateTargets())
			legacyTargetWitnesses++;
		extractionWitnesses++;
	}

	public static boolean allowsMissingItemTarget(int control)
	{
		return kind() == 0 && control < 2;
	}

	private static int kind()
	{
		return variant / 2;
	}

	private static int pose()
	{
		return variant % 2;
	}

	private static boolean alpha()
	{
		return kind() == 2 || kind() == 4;
	}

	public static String variant()
	{
		return KINDS[kind()] + "/time-" + pose() * 10;
	}

	public static void verifyControl(List<Float> distances, List<float[]> colors, int mode, int material)
	{
		String prefix = mode + "/" + material + "/";
		for (int sample = 0; sample < 9; sample++)
		{
			if (pose() == 1)
			{
				var previous = CONTROLS.get(prefix + (variant - 1));
				check(previous != null && Math.abs(distances.get(sample) - previous.distances.get(sample)) > 0.15f,
						"Animated legacy surface must move: " + variant() + " distances=" + distances + " previous=" + previous);
				motionWitnesses++;
			}
			if (kind() > 0)
			{
				float[] actual = colors.get(sample), bare = CONTROLS.get(prefix + pose()).colors.get(sample);
				check(alpha() ? actual[1] > bare[1] + 0.03f && actual[0] < bare[0] - 0.03f :
						actual[1] > 0.9f && actual[0] < 0.02f && actual[2] < 0.02f,
						"Legacy resolver must draw green pixels: " + variant() + " sample=" + sample + " actual=" + java.util.Arrays.toString(actual));
				pixelWitnesses++;
			}
		}
		CONTROLS.put(prefix + variant, new Control(List.copyOf(distances), List.copyOf(colors)));
	}

	public static void complete()
	{
		check(configured == 60 && pixelWitnesses == 864 && motionWitnesses == 540 && extractionWitnesses == 120 && policyWitnesses == 6, "Entire legacy matrix must finish");
		for (int mode = 0; mode < 6; mode++)
			for (int owner = 2; owner < VARIANTS; owner++)
				check(RESOLVERS[mode][owner] > 0 && SUBMISSIONS[mode][owner] == RESOLVERS[mode][owner] && OIT_PASSES[mode][owner] > 0,
						"Every resolved legacy mesh submits its explicit ordinary policy");
		PLibDatabase.LOGGER.info("STAGE 5.2b.4c.1 LEGACY DEPTH PASS: configured={}, pixelWitnesses={}, motionWitnesses={}, extractionWitnesses={}, legacyTargetWitnesses={}, policyWitnesses={}, oitPasses={}, resolvers={}",
				configured, pixelWitnesses, motionWitnesses, extractionWitnesses, legacyTargetWitnesses, policyWitnesses, java.util.Arrays.deepToString(OIT_PASSES), java.util.Arrays.deepToString(RESOLVERS));
		variant = -1;
	}

	public static void depthPass()
	{
		if (variant < 0)
			return;
		check(control >= 2, "Ordinary legacy geometry must not start OIT when the Pulse entity is culled");
		OIT_PASSES[phase][variant]++;
	}

	public static void submitted(RenderType type, PTransparencyMode transparency)
	{
		check(variant >= 0 && kind() > 0 && transparency == PTransparencyMode.ORDINARY,
				"Legacy geometry explicitly selects ordinary blending");
		check(PRenderTypes.isTransparent(type) == alpha() && PRenderTypes.usesOit(type) == alpha(),
				"Ordinary alpha must use the existing OIT-capable material without changing its render type");
		SUBMISSIONS[phase][variant]++;
	}

	/** A single actual render type must retain separate ordinary/OIT submissions through packing and cleanup. */
	private static void verifyPolicies()
	{
		var registry = new PGlResourceRegistry();
		var type = PRenderTypes.RenderTypeProvider.trianglesEmissiveTranslucent(PResourceCache.ATLAS_LOCATION);
		var ordinary = registry.pipeline(type, PTransparencyMode.ORDINARY);
		var oit = registry.pipeline(type, PTransparencyMode.OIT);
		check(ordinary != oit && registry.pipeline(type, PTransparencyMode.ORDINARY) == ordinary && registry.pipeline(type) == oit,
				"Stable policy bindings do not merge distinct modes");
		check(registry.pipeline(ordinary) == type && registry.pipeline(oit) == type &&
				registry.pipeline(ordinary).pipeline() == registry.pipeline(oit).pipeline(), "Both modes use one unchanged RenderType and GPU pipeline");
		var compiler = new PFrameCompiler<String>();
		var mesh = new PMeshHandle(0);
		for (int depth : new int[] {2, 8, 5})
		{
			compiler.submit("entity", ordinary, mesh, 6, new PInstanceHeader(new Matrix4f().translation(0, 0, depth), -1, 0, 0), true);
			compiler.submit("block", oit, mesh, 6, new PInstanceHeader(new Matrix4f().translation(0, 0, depth), -1, 0, 0), true);
		}
		var plan = compiler.compileCombined(List.of("entity", "block"), handle -> registry.transparency(handle) == PTransparencyMode.OIT);
		check(plan.groups().size() == 2 && plan.groups().getFirst().pipeline() == oit && plan.groups().getFirst().command().instanceCount() == 3,
				"Only explicit OIT submissions batch without sorting");
		check(plan.groups().getLast().instances().stream().map(instance -> instance.transform().m32()).toList().equals(List.of(8f, 5f, 2f)),
				"Ordinary submissions of the same render type remain globally sorted");
		registry.clear();
		try
		{
			registry.transparency(ordinary);
			throw new AssertionError("Clear must invalidate policy bindings");
		}
		catch (IllegalStateException expected)
		{
			check(registry.pipeline(type, PTransparencyMode.ORDINARY).value() > oit.value(), "Reload must not recycle stale policy handles");
		}
		policyWitnesses++;
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
			throw new AssertionError(message);
	}

	private record Control(List<Float> distances, List<float[]> colors)
	{
	}
}
