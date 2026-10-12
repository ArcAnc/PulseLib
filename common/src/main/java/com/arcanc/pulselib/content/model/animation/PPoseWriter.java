/**
 * @author ArcAnc
 * Created at: 05.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;

import org.joml.Quaternionf;
import org.joml.Vector3f;

public interface PPoseWriter
{
	void translation(int boneIndex, Vector3f value);

	void rotation(int boneIndex, Quaternionf value);

	void scale(int boneIndex, Vector3f value);
}
