/**
 * @author ArcAnc
 * Created at: 13.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.plan;

import java.util.ArrayList;
import java.util.List;

/**
 * Compiled draw groups and packed instance data ready for render-backend execution.
 *
 * @param groups the groups
 * @param instances the instances
 */
public record PRenderPlan(List<PDrawGroup> groups, List<PInstanceHeader> instances)
{
	/**
	 * Render plan containing no draw groups or instances.
	 */
	public static final PRenderPlan EMPTY = new PRenderPlan(List.of(), List.of());

	/**
	 * Packs draw groups and instance data into an executable render plan.
	 *
	 * @param groups the groups
	 */
	public PRenderPlan(List<PDrawGroup> groups)
	{
		this(pack(groups));
	}

	private PRenderPlan(Packed packed)
	{
		this(packed.groups(), packed.instances());
	}

	/**
	 * Packs draw groups and instance data into an executable render plan.
	 *
	 * @param groups the groups
	 * @param instances the instances
	 */
	public PRenderPlan
	{
		groups = List.copyOf(groups);
		instances = List.copyOf(instances);
	}

	/**
	 * Returns whether the plan contains no draw groups.
	 *
	 * @return whether the plan contains no draw groups
	 */
	public boolean isEmpty()
	{
		return this.groups.isEmpty();
	}

	private static Packed pack(List<PDrawGroup> groups)
	{
		List<PDrawGroup> packedGroups = new ArrayList<>(groups.size());
		List<PInstanceHeader> packedInstances = new ArrayList<>();
		for (PDrawGroup group : groups)
		{
			int firstInstance = packedInstances.size();
			packedInstances.addAll(group.instances());
			packedGroups.add(group.withCommand(group.command().withInstances(group.instances().size(), firstInstance)));
		}
		return new Packed(packedGroups, packedInstances);
	}

	/**
	 * Packed draw-group and instance lists used to construct a render plan.
	 *
	 * @param groups the groups
	 * @param instances the instances
	 */
	private record Packed(List<PDrawGroup> groups, List<PInstanceHeader> instances)
	{
	}
}
