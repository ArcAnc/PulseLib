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
 * Carries deformer prepare context.
 */
public final class PDeformerPrepareContext
{
	private final List<PPreparedDeformer> operations = new ArrayList<>();

	/**
	 * Performs the add operation.
	 * @param operation the operation to use.
	 */
	public void add(PPreparedDeformer operation)
	{
		this.operations.add(operation);
	}

	/**
	 * Performs the build operation.
	 * @return the value produced by this operation.
	 */
	PDeformerStack build()
	{
		return new PDeformerStack(this.operations);
	}
}
