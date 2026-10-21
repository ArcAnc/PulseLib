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
 * Immutable value object representing deformer instance.
 */
public record PDeformerInstance<D>(PMeshDeformer<D> type, D definition)
{
	/**
	 * Creates an instance of the enclosing type.
	 * @param type the type to use.
	 * @param definition the definition to use.
	 */
	public PDeformerInstance
	{
		Objects.requireNonNull(type);
		Objects.requireNonNull(definition);
	}

	/**
	 * Performs the prepare operation.
	 * @param context the context to use.
	 */
	void prepare(PDeformerPrepareContext context)
	{
		this.type.prepare(context, this.definition);
	}
}
