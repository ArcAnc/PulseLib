/**
 * @author ArcAnc
 * Created at: 27.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.deformer;

import net.minecraft.client.model.geom.ModelPart;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Captures cube bindings for deferred replay and invocation-local restoration.
 * Value sources inside a binding keep their existing semantics; this does not sample channels.
 */
@ApiStatus.Internal
public final class PPlayerDeformerSnapshot implements AutoCloseable
{
	private final List<SavedBinding> bindings;

	private PPlayerDeformerSnapshot(List<SavedBinding> bindings)
	{
		this.bindings = List.copyOf(bindings);
	}

	public static PPlayerDeformerSnapshot capture(ModelPart root)
	{
		List<ModelPart.Cube> cubes = new ArrayList<>();
		for (ModelPart part : root.getAllParts())
			if ((Object)part instanceof PModelPartCubes access)
				cubes.addAll(access.pulselib$cubes());
		return captureCubes(cubes);
	}

	static PPlayerDeformerSnapshot captureCubes(List<? extends ModelPart.Cube> cubes)
	{
		List<SavedBinding> bindings = new ArrayList<>();
		for (ModelPart.Cube cube : cubes)
			if (cube instanceof PDeformedCuboid deformable)
				bindings.add(new SavedBinding(deformable, deformable.getDeformer()));
		return new PPlayerDeformerSnapshot(bindings);
	}

	/** Restores the captured bindings without changing cube geometry or hierarchy membership. */
	public void restore()
	{
		for (SavedBinding binding : this.bindings)
			binding.cube.setDeformer(binding.deformer);
	}

	@Override
	public void close()
	{
		this.restore();
	}

	private record SavedBinding(PDeformedCuboid cube, PPlayerVertexDeformer deformer)
	{
	}
}
