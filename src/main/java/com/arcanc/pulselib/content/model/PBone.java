/**
 * @author ArcAnc
 * Created at: 26.01.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model;


import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Provides support for bone.
 */
public class PBone
{
	private final UUID uuid;
	private final String name;
	private final List<UUID> meshUUIDS;
	private @Nullable PBone parent;
	private final List<PBone> children;
	private final Vector3f pivot;
	private final Quaternionf baseRotation;
	
	/**
	 * Creates an instance of the enclosing type.
	 * @param uuid the uuid to use.
	 * @param name the name to use.
	 * @param meshUUIDS the mesh uuids to use.
	 * @param parent the parent to use.
	 * @param children the children to use.
	 * @param pivot the pivot to use.
	 * @param baseRotation the base rotation to use.
	 */
	public PBone(
			UUID uuid,
			String name,
			List<UUID> meshUUIDS,
			@Nullable PBone parent,
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
	
	/**
	 * Creates an instance of the enclosing type.
	 * @param uuid the uuid to use.
	 * @param name the name to use.
	 * @param pivot the pivot to use.
	 * @param baseRotation the base rotation to use.
	 */
	public PBone(UUID uuid, String name, Vector3f pivot, Quaternionf baseRotation)
	{
		this(uuid, name, new ArrayList<>(), null, new ArrayList<>(), pivot, baseRotation);
	}
	
	/**
	 * Performs the uuid operation.
	 * @return the value produced by this operation.
	 */
	public UUID uuid()
	{
		return this.uuid;
	}
	
	/**
	 * Performs the name operation.
	 * @return the value produced by this operation.
	 */
	public String name()
	{
		return this.name;
	}
	
	/**
	 * Performs the mesh uuids operation.
	 * @return the value produced by this operation.
	 */
	public List<UUID> meshUUIDS()
	{
		return this.meshUUIDS;
	}
	
	/**
	 * Performs the parent operation.
	 * @return the value produced by this operation.
	 */
	public @Nullable PBone parent()
	{
		return this.parent;
	}
	
	/**
	 * Sets the parent.
	 * @param parent the parent to use.
	 */
	public void setParent(PBone parent)
	{
		this.parent = parent;
	}
	
	/**
	 * Performs the children operation.
	 * @return the value produced by this operation.
	 */
	public List<PBone> children()
	{
		return this.children;
	}
	
	/**
	 * Performs the pivot operation.
	 * @return the value produced by this operation.
	 */
	public Vector3f pivot()
	{
		return this.pivot;
	}
	
	/**
	 * Performs the base rotation operation.
	 * @return the value produced by this operation.
	 */
	public Quaternionf baseRotation()
	{
		return this.baseRotation;
	}
}
