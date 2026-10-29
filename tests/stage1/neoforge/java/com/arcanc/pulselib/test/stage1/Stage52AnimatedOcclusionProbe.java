/**
 * @author ArcAnc
 * Created at: 29.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1;

import com.arcanc.pulselib.api.registration.PAnimatedAttachmentRegistrar;
import com.arcanc.pulselib.api.registration.PModelRegistrar;
import com.arcanc.pulselib.api.registration.PPlayerAnimationRegistrar;
import com.arcanc.pulselib.content.animatable.ControllerState;
import com.arcanc.pulselib.content.model.animation.PPoseEasing;
import com.arcanc.pulselib.content.model.animation.PRawAnimation;
import com.arcanc.pulselib.content.model.animation.PTransitionInterruptionPolicy;
import com.arcanc.pulselib.content.model.baked.PMeshRenderContext;
import com.arcanc.pulselib.content.model.baked.PMeshRenderMaterial;
import com.arcanc.pulselib.content.model.textures.PAlphaMode;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationAnchor;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationDefinition;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import com.arcanc.pulselib.content.player.animation.PPlayerPart;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAnimatedAttachmentContext;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAnimatedAttachmentRenderer;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.base.PEntityRenderState;
import com.arcanc.pulselib.content.renderer.base.PPlayerRigRenderState;
import com.arcanc.pulselib.content.renderer.base.PWorldRenderState;
import com.arcanc.pulselib.content.player.deformer.PPlayerGeometryRenderState;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.data.gecko.PGeckoModelLoader;
import com.arcanc.pulselib.data.gltf.PGltfModelLoader;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.PResourceCache;
import net.minecraft.client.model.Model;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Real decoded keyframes, canonical native rig/armor and arbitrary world attachment submissions. */
public final class Stage52AnimatedOcclusionProbe
{
	public static final int VARIANTS = 20;
	private static final String[] KINDS = {"native-armor", "custom-armor", "anchor-only-opaque", "anchor-alpha", "anchor-oit"};
	private static final List<PModelData> MODELS = List.of(
			PModelData.direct(PLibDatabase.rl("player/regression/depth_motion"), PGeckoModelLoader.INSTANCE.id()),
			PModelData.direct(PLibDatabase.rl("player/regression/depth_motion_gltf"), PGltfModelLoader.INSTANCE.id()));
	private static final List<Identifier> IDS = List.of(PLibDatabase.rl("stage52b4b/gecko"), PLibDatabase.rl("stage52b4b/gltf"));
	private static final PPlayerAnimationAnchor ANCHOR = new PPlayerAnimationAnchor(PLibDatabase.rl("stage52b4b/body"));
	private static final Map<Model<?>, Model<?>> CUSTOM = new IdentityHashMap<>();
	private static final Map<String, Control> CONTROLS = new HashMap<>();
	private static final int[][] OIT_PASSES = new int[6][VARIANTS];
	private static final int[][] CALLBACKS = new int[6][VARIANTS];
	private static int variant = -1;
	private static int phase;
	private static int configured;
	private static int selections;
	private static int poseWitnesses;
	private static int pixelWitnesses;
	private static int formatWitnesses;
	private static int anchorOnlyTargets;

	private Stage52AnimatedOcclusionProbe()
	{
	}

	public static void register(IEventBus bus)
	{
		Stage42aWorldStageProbe.registerAlpha(bus);
		bus.addListener((RegisterClientExtensionsEvent event) -> event.registerItem(new IClientItemExtensions()
		{
			@Override
			public Model<?> getGenericArmorModel(ItemStack stack, EquipmentClientInfo.LayerType type, Model original)
			{
				if (variant < 0 || kind() != 1)
					return original;
				selections++;
				return CUSTOM.computeIfAbsent(original, ignored -> Stage52ArmorOcclusionProbe.custom());
			}
		}, Items.DIAMOND_CHESTPLATE));
	}

	public static void registerModels(PModelRegistrar registrar)
	{
		for (PModelData data : MODELS)
			registrar.model(data).texture("0", Stage42aWorldStageProbe.WHITE);
	}

	public static void registerAnimations(PPlayerAnimationRegistrar registrar)
	{
		for (int format = 0; format < MODELS.size(); format++)
		{
			int selected = format;
			registrar.register(IDS.get(format), PPlayerAnimationDefinition.builder(MODELS.get(format)).
					when(player -> variant >= 0 && player.getId() == 0x526a01 && format() == selected).
					bind(PPlayerPart.BODY, "body").mask((player, part, tick) -> kind() != 2).anchor(ANCHOR, "body").meshAttachment("stage52b4b_no_automatic_mesh").
					crossfade(0, PPoseEasing.LINEAR, PTransitionInterruptionPolicy.FROM_CURRENT).
					controllers(controllers -> controllers.add("depth", () -> state -> state.controller().getState())).build());
		}
	}

	public static void registerAttachments(PAnimatedAttachmentRegistrar registrar)
	{
		registrar.register(new PPlayerAnimatedAttachmentRenderer()
		{
			@Override
			public PPlayerAnimationAnchor anchor()
			{
				return ANCHOR;
			}

			@Override
			public void render(PPlayerAnimatedAttachmentContext context)
			{
				if (variant < 0 || kind() < 2)
					return;
				check(!context.firstPerson() && context.player().getId() == 0x526a01 && context.animation().equals(IDS.get(format())),
						"Only the active third-person owner/definition may submit this socket");
				CALLBACKS[phase][variant]++;
				var poses = context.poseStack();
				poses.pushPose();
				try
				{
					poses.translate(0, 0.15, -0.4);
					poses.scale(0.7f, 0.4f, 0.15f);
					var mesh = Stage52PlayerOcclusionProbe.MODEL.getModel().bones().getFirst().meshes().getFirst();
					var inputs = new PMeshRenderContext(PRenderTypes.RenderTypeProvider::trianglesSolid,
							kind() == 2 ? 0xFF00FF00 : 0x8000FF00, context.packedLight(), OverlayTexture.NO_OVERLAY).
							withEmissive(true).withAlphaMode(kind() == 2 ? PAlphaMode.OPAQUE : PAlphaMode.TRANSLUCENT);
					var material = PMeshRenderMaterial.resolve(mesh, inputs);
					var type = kind() == 3 ? Stage42aWorldStageProbe.ordinaryAlpha() : material.resolveRenderType(inputs, PResourceCache.ATLAS_LOCATION);
					check(PRenderTypes.usesOit(type) == (kind() == 4) && PRenderTypes.isTransparent(type) == (kind() >= 3),
							"Attachment material must select the requested opaque/ordinary/OIT path");
					PRenderQueue.submitEntityMesh(type, material.mesh(), null,
							new PRenderQueue.InstanceData(new Matrix4f(poses.last().pose()), inputs.color(), material.packedLight(), inputs.packedOverlay()));
				}
				finally
				{
					poses.popPose();
				}
			}
		});
	}

	public static boolean ready()
	{
		for (PModelData data : MODELS)
		{
			var model = data.getModel();
			if (model == null)
				return false;
			var clip = model.animations().get("depth");
			check(clip != null && clip.length() == 20 && clip.boneAnimations().containsKey("body"),
					"Actual decoded channel must exist: " + data.getModelId() + " clip=" + clip);
		}
		return true;
	}

	public static void configure(RemotePlayer player, int value, int mode)
	{
		phase = mode;
		if (variant != value)
		{
			variant = value;
			configured++;
			for (Identifier id : IDS)
				PPlayerAnimations.getHandle(player, id).stopAll();
			player.setItemSlot(EquipmentSlot.CHEST, kind() == 2 ? ItemStack.EMPTY : new ItemStack(Items.DIAMOND_CHESTPLATE));
			check(PPlayerAnimations.getHandle(player, IDS.get(format())).play("depth", PRawAnimation.begin().thenLoop("depth").build()),
					"Real per-player controller starts the decoded animation");
		}
		var controller = PPlayerAnimations.getHandle(player, IDS.get(format())).controller("depth");
		check(controller != null, "Active controller exists");
		controller.seek(MODELS.get(format()).getModel(), pose() * 10);
		controller.pause();
		check(controller.getState() == ControllerState.PAUSE && controller.getInterpolatedTime(0.5f) == pose() * 10,
				"Hold a precise sampled animation time across the independent control frames");
	}

	public static void verifyExtracted(LevelRenderState state, int control)
	{
		if (kind() != 2 || control != 1)
			return;
		check(state.entityRenderStates.stream().noneMatch(owner -> owner instanceof PEntityRenderState<?>),
				"Anchor-only pixel control must actually cull the Pulse entity");
		var owner = state.entityRenderStates.stream().filter(value -> value instanceof AvatarRenderState avatar && avatar.id == 0x526a01).findFirst().orElseThrow();
		var rig = ((PPlayerRigRenderState)owner).pulselib$getPlayerRig();
		var geometry = ((PPlayerGeometryRenderState)owner).pulselib$getPlayerGeometry();
		check(rig != null && rig.isEmpty() && geometry != null && !geometry.hasArmor() &&
				((PWorldRenderState)owner).pulselib$requiresSeparateTargets(),
				"Bare masked-rig player must retain targets through its registered active anchor");
		anchorOnlyTargets++;
	}

	public static void depthPass()
	{
		if (phase < 6 && variant >= 0)
			OIT_PASSES[phase][variant]++;
	}

	private static int format()
	{
		return variant / 10;
	}

	private static int kind()
	{
		return variant % 10 / 2;
	}

	private static int pose()
	{
		return variant % 2;
	}

	public static String variant()
	{
		return (format() == 0 ? "gecko/" : "gltf/") + KINDS[kind()] + "/time-" + pose() * 10;
	}

	public static void verifyControl(List<Float> distances, List<float[]> colors, int mode, int material)
	{
		String prefix = mode + "/" + material + "/";
		for (int sample = 0; sample < distances.size(); sample++)
		{
			if (pose() == 1)
			{
				var previous = CONTROLS.get(prefix + (variant - 1));
				check(previous != null && Math.abs(distances.get(sample) - previous.distances.get(sample)) > 0.15f,
						"Decoded animation must move the actual depth surface: " + variant() + " sample=" + sample + " distances=" + distances + " previous=" + previous);
				poseWitnesses++;
			}
			if (kind() >= 2)
			{
				var vanilla = CONTROLS.get(prefix + (format() * 10 + pose()));
				float[] actual = colors.get(sample), base = vanilla.colors.get(sample);
				check(kind() == 2 ? actual[1] > 0.9f && actual[0] < 0.02f && actual[2] < 0.02f :
						actual[1] > base[1] + 0.03f && actual[0] < base[0] - 0.03f,
						"Arbitrary callback must emit visible green attachment pixels: " + variant() + " sample=" + sample + " actual=" + java.util.Arrays.toString(actual));
				pixelWitnesses++;
			}
			if (format() == 1)
			{
				var gecko = CONTROLS.get(prefix + (variant - 10));
				check(gecko != null && Math.abs(distances.get(sample) - gecko.distances.get(sample)) < 0.002f,
						"Equivalent real glTF/Gecko channels must produce the same depth surface: " + variant());
				formatWitnesses++;
			}
		}
		CONTROLS.put(prefix + variant, new Control(List.copyOf(distances), List.copyOf(colors)));
	}

	public static void complete()
	{
		check(configured == 120 && selections > 0 && poseWitnesses == 1080 && pixelWitnesses == 1296 && formatWitnesses == 1080 && anchorOnlyTargets == 48,
				"Every format, material, pose and mode/reload group must finish");
		for (int mode = 0; mode < 6; mode++)
			for (int owner = 0; owner < VARIANTS; owner++)
			{
				check(OIT_PASSES[mode][owner] > 0, "Every group needs actual backend OIT passes");
				if (owner % 10 / 2 >= 2)
					check(CALLBACKS[mode][owner] > 0, "Every socket group needs actual callbacks");
			}
		CUSTOM.clear();
		PLibDatabase.LOGGER.info("STAGE 5.2b.4b ANIMATED DEPTH PASS: configured={}, selections={}, poseWitnesses={}, pixelWitnesses={}, formatWitnesses={}, anchorOnlyTargets={}, callbacks={}, oitPasses={}",
				configured, selections, poseWitnesses, pixelWitnesses, formatWitnesses, anchorOnlyTargets, java.util.Arrays.deepToString(CALLBACKS), java.util.Arrays.deepToString(OIT_PASSES));
		variant = -1;
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
