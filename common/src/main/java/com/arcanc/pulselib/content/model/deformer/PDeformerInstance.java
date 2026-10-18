/**
 * @author ArcAnc
 * Created at: 08.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.deformer;

import java.util.Objects;

/**
 * Pairs a registered mesh deformer type with one decoded configuration.
 *
 * @param <D> the serialized mesh deformer configuration type
 * @param type the registered mesh deformer implementation
 * @param definition the immutable configuration to register or evaluate
 */
public record PDeformerInstance<D>(PMeshDeformer<D> type, D definition)
{
	/**
	 * Pairs a registered deformer implementation with its decoded configuration.
	 *
	 * @param type the registered mesh deformer implementation
	 * @param definition the immutable configuration to register or evaluate
	 */
	public PDeformerInstance
	{
		Objects.requireNonNull(type);
		Objects.requireNonNull(definition);
	}

	/**
	 * Appends the operations for this deformer instance to the preparation context.
	 *
	 * @param context the destination collecting prepared deformation operations
	 */
	void prepare(PDeformerPrepareContext context)
	{
		this.type.prepare(context, this.definition);
	}
}
