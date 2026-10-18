/**
 * @author ArcAnc
 * Created at: 26.01.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model;


import com.arcanc.pulselib.content.model.animation.PAnimation;
import com.mojang.datafixers.util.Pair;

import java.util.*;

/**
 * Mutable model loading result containing source bones, meshes, bone-to-mesh associations and named
 * animations.
 */
public class PModel
{
	/**
	 * Source bones indexed by their stable UUID.
	 */
	public final Map<UUID, PBone> bones = new LinkedHashMap<>();
	/**
	 * Source meshes indexed by their stable UUID.
	 */
	public final Map<UUID, PMesh> meshes = new LinkedHashMap<>();
	/**
	 * Associations between model bones and their referenced meshes.
	 */
	public final Map<UUID, Pair<UUID, List<UUID>>> boneMeshes = new HashMap<>();
	/**
	 * Animation clips indexed by their names in the source model.
	 */
	public final Map<String, PAnimation> animations = new HashMap<>();
}
