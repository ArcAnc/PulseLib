/**
 * @author ArcAnc
 * Created at: 24.02.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.base;


import com.arcanc.pulselib.content.animatable.AnimManagerKey;
import com.arcanc.pulselib.content.animatable.PAnimatable;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import org.jspecify.annotations.Nullable;

public interface PRenderState<T extends PAnimatable<T>>
{
	/**
	 * Extracts the data.
	 */
	void extractData();
	
	/**
	 * Performs the partial tick operation.
	 * @return the value produced by this operation.
	 */
	float partialTick();
	
	/**
	 * Returns the baked model.
	 * @return the value produced by this operation.
	 */
	@Nullable PBakedModel getBakedModel();
	
	/**
	 * Returns the animatable.
	 * @return the value produced by this operation.
	 */
	T getAnimatable();
	
	/**
	 * Returns the anim key.
	 * @return the value produced by this operation.
	 */
	AnimManagerKey getAnimKey();
}
