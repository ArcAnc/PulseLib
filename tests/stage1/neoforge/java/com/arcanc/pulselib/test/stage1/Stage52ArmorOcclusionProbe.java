/**
 * @author ArcAnc
 * Created at: 29.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1;

import com.arcanc.pulselib.content.model.deformer.PChannelReference;
import com.arcanc.pulselib.content.model.deformer.PDeformerInstance;
import com.arcanc.pulselib.content.model.deformer.PDeformerStack;
import com.arcanc.pulselib.content.model.deformer.PStretchDeformer;
import com.arcanc.pulselib.content.model.deformer.PStretchDefinition;
import com.arcanc.pulselib.content.player.animation.PPlayerPart;
import com.arcanc.pulselib.content.player.deformer.PDeformableCubeBakeScope;
import com.arcanc.pulselib.content.player.deformer.PDeformedCuboid;
import com.arcanc.pulselib.content.player.deformer.PModelPartCubes;
import com.arcanc.pulselib.content.player.deformer.PPlayerMeshDeformers;
import com.arcanc.pulselib.util.PLibDatabase;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.joml.Vector3f;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Extends the world pixel oracle with native, selected and deformed armor; no draw interception. */
public final class Stage52ArmorOcclusionProbe
{
	private static final String[] VARIANTS = {"skin", "native-armor", "custom-armor", "deformed-custom-armor"};
	private static final Map<Model<?>, Model<?>> MODELS = new IdentityHashMap<>();
	private static final Map<String, List<Float>> CONTROLS = new java.util.HashMap<>();
	private static final int[][] OIT_PASSES = new int[6][4];
	private static int phase;
	private static int variant = -1;
	private static int selections;
	private static int deformerReads;
	private static int depthWitnesses;
	private static int equipped;

	private Stage52ArmorOcclusionProbe()
	{
	}

	public static void register(IEventBus bus)
	{
		bus.addListener((RegisterClientExtensionsEvent event) -> event.registerItem(new IClientItemExtensions()
		{
			@Override
			public Model<?> getGenericArmorModel(ItemStack stack, EquipmentClientInfo.LayerType type, Model original)
			{
				if (variant < 2)
					return original;
				selections++;
				return MODELS.computeIfAbsent(original, ignored -> custom());
			}
		}, Items.DIAMOND_CHESTPLATE));
		var deformation = PDeformerStack.compile(List.of(new PDeformerInstance<>(PStretchDeformer.INSTANCE,
				new PStretchDefinition(new Vector3f(), new Vector3f(0, 0, 1), PChannelReference.constant(1)))));
		PPlayerMeshDeformers.register(PLibDatabase.rl("stage52b4a/body"), PPlayerPart.BODY,
				player -> variant == 3 && player.getId() == 0x526a01, deformation, (player, reference) ->
				{
					deformerReads++;
					return 1.75f;
				});
	}

	private static Model<?> custom()
	{
		var mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0);
		mesh.getRoot().addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).
				addBox(-4, 0, -2, 8, 12, 4, new CubeDeformation(2)), PartPose.ZERO);
		// Direct custom bakes use the same explicit scope as EntityModelSet.bakeLayer.
		try (var baking = PDeformableCubeBakeScope.begin())
		{
			var root = LayerDefinition.create(mesh, 64, 32).bakeRoot();
			check(((PModelPartCubes)(Object)root.getChild("body")).pulselib$cubes().stream().allMatch(cube -> cube instanceof PDeformedCuboid),
					"Custom body contains actual deformable cuboids");
			return new HumanoidModel<AvatarRenderState>(root);
		}
	}

	public static void equip(RemotePlayer player, int value, int mode)
	{
		phase = mode;
		if (variant == value)
			return;
		variant = value;
		player.setItemSlot(EquipmentSlot.CHEST, value == 0 ? ItemStack.EMPTY : new ItemStack(Items.DIAMOND_CHESTPLATE));
		equipped++;
	}

	public static void depthPass()
	{
		if (phase < 6 && variant >= 0)
			OIT_PASSES[phase][variant]++;
	}

	public static String variant()
	{
		return VARIANTS[variant];
	}

	/** Independent player-only frames must prove that each successive geometry reaches the depth buffer. */
	public static void verifyControl(List<Float> depths, int phase, int material)
	{
		String key = phase + "/" + material + "/";
		if (variant > 0)
		{
			var previous = CONTROLS.get(key + (variant - 1));
			check(previous != null, "Previous isolated geometry control exists");
			for (int sample = 0; sample < depths.size(); sample++)
			{
				check(depths.get(sample) < previous.get(sample) - 0.001f,
						"Actual armor geometry must move the sampled surface towards the camera: " + variant() +
								" phase=" + phase + " material=" + material + " sample=" + sample + " depths=" + previous + "/" + depths);
				depthWitnesses++;
			}
		}
		CONTROLS.put(key + variant, List.copyOf(depths));
	}

	public static void complete()
	{
		check(equipped == 24 && depthWitnesses == 324 && selections > 0 && deformerReads > 0,
				"All six mode/reload phases need real selected and deformed armor depth witnesses");
		for (int[] mode : OIT_PASSES)
			for (int passes : mode)
				check(passes > 0, "Every geometry/mode/reload group must execute the actual OIT depth pass");
		PPlayerMeshDeformers.unregister(PLibDatabase.rl("stage52b4a/body"));
		MODELS.clear();
		PLibDatabase.LOGGER.info("STAGE 5.2b.4a ARMOR DEPTH PASS: variants={}, equipped={}, depthWitnesses={}, selections={}, deformerReads={}, oitPasses={}",
				VARIANTS.length, equipped, depthWitnesses, selections, deformerReads, java.util.Arrays.deepToString(OIT_PASSES));
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
			throw new AssertionError(message);
	}
}
