/**
 * @author ArcAnc
 * Created at: 08.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.deformer;

import com.arcanc.pulselib.content.model.deformer.PDeformerFrame;
import com.arcanc.pulselib.content.model.deformer.PDeformerStack;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationDeformerApplication;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import com.arcanc.pulselib.content.player.animation.PPlayerPart;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

public final class PPlayerMeshDeformers
{
	private static final Map<ResourceLocation, Binding> BINDINGS = new LinkedHashMap<>();

	private PPlayerMeshDeformers()
	{
	}

	public static void register(ResourceLocation id, PPlayerPart part, Predicate<Player> applies,
							PDeformerStack stack, PPlayerDeformerValueSource values)
	{
		Objects.requireNonNull(id);
		if (BINDINGS.putIfAbsent(id, new Binding(part, applies, stack, values)) != null)
			throw new IllegalArgumentException("Duplicate player deformer binding " + id);
	}

	public static void unregister(ResourceLocation id)
	{
		BINDINGS.remove(id);
	}

	public static void clear()
	{
		BINDINGS.clear();
	}
	
	public static PDeformerFrame frameAt(Player player, PPlayerPart part, Vector3f localPosition,
										Vector3f forwardAxis, Vector3f upAxis)
	{
		return frameAt(player, part, localPosition, forwardAxis, upAxis, 0.0f);
	}

	public static PDeformerFrame frameAt(Player player, PPlayerPart part, Vector3f localPosition,
										Vector3f forwardAxis, Vector3f upAxis, float partialTick)
	{
		List<ActiveBinding> active = active(player, part, partialTick);
		Vector3f position = deform(player, active, new Vector3f(localPosition));
		float epsilon = 0.001f;
		Vector3f forward = deform(player, active, new Vector3f(localPosition).fma(epsilon, forwardAxis)).sub(position);
		if (forward.lengthSquared() < 1.0e-10f) forward.set(forwardAxis);
		forward.normalize();
		Vector3f up = deform(player, active, new Vector3f(localPosition).fma(epsilon, upAxis)).sub(position);
		up.fma(-up.dot(forward), forward);
		if (up.lengthSquared() < 1.0e-10f) up.set(upAxis).fma(-upAxis.dot(forward), forward);
		if (up.lengthSquared() < 1.0e-10f) up.set(forward.z, 0.0f, -forward.x);
		up.normalize();
		Vector3f right = new Vector3f(forward).cross(up).normalize();
		up.set(right).cross(forward).normalize();
		return new PDeformerFrame(position, right, up, forward);
	}

	public static void apply(Player player, PlayerModel<?> model)
	{
		apply(player, model, 0.0f);
	}

	public static void apply(Player player, PlayerModel<?> model, float partialTick)
	{
		for (PPlayerPart part : PPlayerPart.values())
			for (ModelPart modelPart : part.resolve(model))
				apply(player, part, modelPart, partialTick);
	}

	public static void apply(Player player, HumanoidModel<?> model)
	{
		apply(player, model, 0.0f);
	}

	public static void apply(Player player, HumanoidModel<?> model, float partialTick)
	{
		apply(player, PPlayerPart.HEAD, model.head, partialTick);
		apply(player, PPlayerPart.BODY, model.body, partialTick);
		apply(player, PPlayerPart.RIGHT_ARM, model.rightArm, partialTick);
		apply(player, PPlayerPart.LEFT_ARM, model.leftArm, partialTick);
		apply(player, PPlayerPart.RIGHT_LEG, model.rightLeg, partialTick);
		apply(player, PPlayerPart.LEFT_LEG, model.leftLeg, partialTick);
	}

	private static void apply(Player player, PPlayerPart part, ModelPart modelPart, float partialTick)
	{
		List<ActiveBinding> active = active(player, part, partialTick);
		PPlayerVertexDeformer deformer = active.isEmpty() ? PPlayerVertexDeformer.IDENTITY : position ->
		{
			for (ActiveBinding binding : active)
				binding.stack.deformInPlace(position, reference -> binding.values.resolve(player, reference));
		};
		for (ModelPart.Cube cube : ((PModelPartCubes)(Object)modelPart).pulselib$cubes())
			if (cube instanceof PDeformedCuboid deformable)
				deformable.setDeformer(deformer);
	}

	private static List<ActiveBinding> active(Player player, PPlayerPart part, float partialTick)
	{
		List<ActiveBinding> active = new ArrayList<>();
		for (Binding binding : BINDINGS.values())
			if (binding.part == part && binding.applies.test(player))
				active.add(new ActiveBinding(binding.stack, binding.values));
		for (PPlayerAnimationDeformerApplication application : PPlayerAnimations.activeDeformers(player, part, partialTick))
			active.add(new ActiveBinding(application.stack(), application.values()));
		return active;
	}

	private static Vector3f deform(Player player, List<ActiveBinding> bindings, Vector3f position)
	{
		for (ActiveBinding binding : bindings)
			binding.stack.deformInPlace(position, reference -> binding.values.resolve(player, reference));
		return position;
	}

	private record Binding(PPlayerPart part, Predicate<Player> applies, PDeformerStack stack, PPlayerDeformerValueSource values)
	{
		private Binding
		{
			Objects.requireNonNull(part);
			Objects.requireNonNull(applies);
			Objects.requireNonNull(stack);
			Objects.requireNonNull(values);
		}
	}

	private record ActiveBinding(PDeformerStack stack, PPlayerDeformerValueSource values)
	{
	}
}
