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
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Captures live bindings for invocation-local restoration or frozen local geometry for submitted nodes.
 * Geometry capture evaluates the complete tessellation; it does not assume a finite list of channel names.
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
		return captureCubes(cubes(root, false));
	}

	/** Samples complete local geometry; its saved binding holds no Player, value-source or prepared-operation closure. */
	public static PPlayerDeformerSnapshot captureGeometry(ModelPart root)
	{
		return captureGeometryCubes(cubes(root, true));
	}

	private static List<ModelPart.Cube> cubes(ModelPart root, boolean visibleOnly)
	{
		Set<ModelPart> hidden = Collections.newSetFromMap(new IdentityHashMap<>());
		if (visibleOnly)
			for (ModelPart part : root.getAllParts())
				if (!part.visible)
					hidden.addAll(part.getAllParts());
		List<ModelPart.Cube> cubes = new ArrayList<>();
		for (ModelPart part : root.getAllParts())
			if ((!visibleOnly || !part.skipDraw && !hidden.contains(part)) && (Object)part instanceof PModelPartCubes access)
				cubes.addAll(access.pulselib$cubes());
		return cubes;
	}

	static PPlayerDeformerSnapshot captureCubes(List<? extends ModelPart.Cube> cubes)
	{
		return captureCubes(cubes, false);
	}

	static PPlayerDeformerSnapshot captureGeometryCubes(List<? extends ModelPart.Cube> cubes)
	{
		return captureCubes(cubes, true);
	}

	private static PPlayerDeformerSnapshot captureCubes(List<? extends ModelPart.Cube> cubes, boolean freeze)
	{
		List<SavedBinding> bindings = new ArrayList<>();
		for (ModelPart.Cube cube : cubes)
			if (cube instanceof PDeformedCuboid deformable)
				bindings.add(new SavedBinding(deformable, freeze ? deformable.freezeBinding() : deformable.getBinding()));
		return new PPlayerDeformerSnapshot(bindings);
	}

	/** Restores captured bindings without replacing baked cubes or hierarchy membership. */
	public void restore()
	{
		for (SavedBinding binding : this.bindings)
			binding.cube.setBinding(binding.binding);
	}

	@Override
	public void close()
	{
		this.restore();
	}

	private record SavedBinding(PDeformedCuboid cube, PDeformedCuboid.Binding binding)
	{
	}
}
