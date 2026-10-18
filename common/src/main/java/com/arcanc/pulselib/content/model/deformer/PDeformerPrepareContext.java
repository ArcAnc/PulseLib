/**
 * @author ArcAnc
 * Created at: 08.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.deformer;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects prepared mesh deformation operations in application order.
 */
public final class PDeformerPrepareContext
{
	private final List<PPreparedDeformer> operations = new ArrayList<>();

	/**
	 * Appends an operation to the prepared deformation sequence.
	 *
	 * @param operation the prepared deformation operation to append
	 */
	public void add(PPreparedDeformer operation)
	{
		this.operations.add(operation);
	}

	/**
	 * Creates a deformation stack from the collected operations.
	 *
	 * @return the prepared mesh deformation stack
	 */
	PDeformerStack build()
	{
		return new PDeformerStack(this.operations);
	}
}
