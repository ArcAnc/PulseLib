/**
 * @author ArcAnc
 * Created at: 08.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.deformer;

import com.mojang.serialization.MapCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * Registered mesh deformation type with a configuration codec and preparation callback.
 *
 * @param <D> the serialized mesh deformer configuration type
 */
public interface PMeshDeformer<D>
{
	/**
	 * Returns the namespaced identifier of this mesh deformer type.
	 *
	 * @return the namespaced identifier of this mesh deformer type
	 */
	ResourceLocation id();

	/**
	 * Returns the codec for the deformer configuration.
	 *
	 * @return the codec for the deformer configuration
	 */
	MapCodec<D> codec();

	/**
	 * Compiles one deformer definition into the supplied preparation context.
	 *
	 * @param context the destination collecting prepared deformation operations
	 * @param definition the immutable configuration to register or evaluate
	 */
	void prepare(PDeformerPrepareContext context, D definition);
}
