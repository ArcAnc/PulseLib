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

/**
 * Mutable source-model bone with a pivot, bind rotation, hierarchy and referenced meshes.
 */
public class PBone
{
	private final UUID uuid;
	private final String name;
	private final List<UUID> meshUUIDS;
	private PBone parent;
	private final List<PBone> children;
	private final Vector3f pivot;
	private final Quaternionf baseRotation;
	
	/**
	 * Creates a source bone with its bind transform and hierarchy links.
	 *
	 * @param uuid the stable source-model bone identity
	 * @param name the bone name
	 * @param meshUUIDS the source mesh identifiers attached to this bone
	 * @param parent the parent
	 * @param children the child bones in hierarchy order
	 * @param pivot the local pivot used when composing the bone transform
	 * @param baseRotation the unanimated local bone rotation
	 */
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
	
	/**
	 * Creates a source bone with its bind transform and hierarchy links.
	 *
	 * @param uuid the stable source-model bone identity
	 * @param name the bone name
	 * @param pivot the local pivot used when composing the bone transform
	 * @param baseRotation the unanimated local bone rotation
	 */
	public PBone(UUID uuid, String name, Vector3f pivot, Quaternionf baseRotation)
	{
		this(uuid, name, new ArrayList<>(), null, new ArrayList<>(), pivot, baseRotation);
	}
	
	/**
	 * Returns the stable source-model bone identity.
	 *
	 * @return the stable source-model bone identity
	 */
	public UUID uuid()
	{
		return this.uuid;
	}
	
	/**
	 * Returns the name.
	 *
	 * @return the name
	 */
	public String name()
	{
		return this.name;
	}
	
	/**
	 * Returns the source mesh identifiers attached to this bone.
	 *
	 * @return the source mesh identifiers attached to this bone
	 */
	public List<UUID> meshUUIDS()
	{
		return this.meshUUIDS;
	}
	
	/**
	 * Returns the parent.
	 *
	 * @return the parent, or {@code null} when the requested value is unavailable
	 */
	public @Nullable PBone parent()
	{
		return this.parent;
	}
	
	/**
	 * Updates the parent used when constructing the source-model hierarchy.
	 *
	 * @param parent the parent
	 */
	public void setParent(PBone parent)
	{
		this.parent = parent;
	}
	
	/**
	 * Returns the child bones in hierarchy order.
	 *
	 * @return the child bones in hierarchy order
	 */
	public List<PBone> children()
	{
		return this.children;
	}
	
	/**
	 * Returns the local pivot used when composing the bone transform.
	 *
	 * @return the local pivot used when composing the bone transform
	 */
	public Vector3f pivot()
	{
		return this.pivot;
	}
	
	/**
	 * Returns the unanimated local bone rotation.
	 *
	 * @return the unanimated local bone rotation
	 */
	public Quaternionf baseRotation()
	{
		return this.baseRotation;
	}
}

