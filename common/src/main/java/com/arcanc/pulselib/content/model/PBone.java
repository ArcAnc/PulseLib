/**
 * @author ArcAnc
 * Created at: 26.01.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model;


import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PBone
{
	private final UUID uuid;
	private final String name;
	private final List<UUID> meshUUIDS;
	private PBone parent;
	private final List<PBone> children;
	private final Vector3f pivot;
	private final Quaternionf baseRotation;
	
	public PBone(
			UUID uuid,
			String name,
			List<UUID> meshUUIDS,
			PBone parent,
			List<PBone> children,
			Vector3f pivot,
			Quaternionf baseRotation)
	{
		this.uuid = uuid;
		this.name = name;
		this.meshUUIDS = meshUUIDS;
		this.parent = parent;
		this.children = children;
		this.pivot = pivot;
		this.baseRotation = baseRotation;
	}
	
	public PBone(UUID uuid, String name, Vector3f pivot, Quaternionf baseRotation)
	{
		this(uuid, name, new ArrayList<>(), null, new ArrayList<>(), pivot, baseRotation);
	}
	
	public UUID uuid()
	{
		return this.uuid;
	}
	
	public String name()
	{
		return this.name;
	}
	
	public List<UUID> meshUUIDS()
	{
		return this.meshUUIDS;
	}
	
	public @Nullable PBone parent()
	{
		return this.parent;
	}
	
	public void setParent(PBone parent)
	{
		this.parent = parent;
	}
	
	public List<PBone> children()
	{
		return this.children;
	}
	
	public Vector3f pivot()
	{
		return this.pivot;
	}
	
	public Quaternionf baseRotation()
	{
		return this.baseRotation;
	}
}

