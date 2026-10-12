/**
 * @author ArcAnc
 * Created at: 08.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.deformer;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class PDeformerStack
{
	public static final PDeformerStack EMPTY = new PDeformerStack(List.of());
	private static final float FRAME_EPSILON = 0.001f;

	private final List<PPreparedDeformer> operations;

	private final List<PDeformerInstance<?>> definitions;

	PDeformerStack(List<PPreparedDeformer> operations)
	{
		this(operations, List.of());
	}

	private PDeformerStack(List<PPreparedDeformer> operations, List<PDeformerInstance<?>> definitions)
	{
		this.operations = List.copyOf(operations);
		this.definitions = List.copyOf(definitions);
	}

	public static PDeformerStack compile(List<? extends PDeformerInstance<?>> definitions)
	{
		Objects.requireNonNull(definitions);
		PDeformerPrepareContext context = new PDeformerPrepareContext();
		for (PDeformerInstance<?> definition : definitions)
			definition.prepare(context);
		PDeformerStack prepared = context.build();
		return new PDeformerStack(prepared.operations, List.copyOf(definitions));
	}

	public static PDeformerStack compose(PDeformerStack... stacks)
	{
		List<PPreparedDeformer> operations = new ArrayList<>();
		List<PDeformerInstance<?>> definitions = new ArrayList<>();
		for (PDeformerStack stack : stacks)
		{
			operations.addAll(stack.operations);
			definitions.addAll(stack.definitions);
		}
		return operations.isEmpty() ? EMPTY : new PDeformerStack(operations, definitions);
	}

	public boolean isEmpty()
	{
		return this.operations.isEmpty();
	}

	/**
	 * MUTATE THOSE LIST IS FORBIDDEN!!!
	 */
	public List<PDeformerInstance<?>> definitions()
	{
		return this.definitions;
	}

	public Vector3f deform(Vector3f localPosition, PDeformerValueSource values)
	{
		Vector3f result = new Vector3f(localPosition);
		deformInPlace(result, values);
		return result;
	}

	public void deformInPlace(Vector3f localPosition, PDeformerValueSource values)
	{
		for (PPreparedDeformer operation : this.operations)
			operation.deform(localPosition, values);
	}
	
	public Vector3f deformNormal(Vector3f localPosition, Vector3f normal, PDeformerValueSource values)
	{
		if (this.operations.isEmpty())
			return new Vector3f(normal);
		Vector3f p = deform(localPosition, values);
		Vector3f dx = deform(new Vector3f(localPosition).add(FRAME_EPSILON, 0.0f, 0.0f), values).
				sub(p).
				div(FRAME_EPSILON);
		Vector3f dy = deform(new Vector3f(localPosition).add(0.0f, FRAME_EPSILON, 0.0f), values).
				sub(p).
				div(FRAME_EPSILON);
		Vector3f dz = deform(new Vector3f(localPosition).add(0.0f, 0.0f, FRAME_EPSILON), values).
				sub(p).
				div(FRAME_EPSILON);
		Vector3f result = new Vector3f(dy).cross(dz).mul(normal.x).
						add(new Vector3f(dz).cross(dx).mul(normal.y)).
						add(new Vector3f(dx).cross(dy).mul(normal.z));
		return result.lengthSquared() < 1.0e-12f ? new Vector3f(normal) : result.normalize();
	}
	
	public PDeformerFrame frameAt(Vector3f localPosition, Vector3f forwardAxis, Vector3f upAxis, PDeformerValueSource values)
	{
		Vector3f position = deform(localPosition, values);
		Vector3f forward = deform(new Vector3f(localPosition).fma(FRAME_EPSILON, forwardAxis), values).
				sub(position);
		if (forward.lengthSquared() < 1.0e-10f)
			forward.set(forwardAxis);
		forward.normalize();

		Vector3f up = deform(new Vector3f(localPosition).fma(FRAME_EPSILON, upAxis), values).
				sub(position);
		up.fma(-up.dot(forward), forward);
		if (up.lengthSquared() < 1.0e-10f)
		{
			up.set(upAxis).fma(-upAxis.dot(forward), forward);
			if (up.lengthSquared() < 1.0e-10f)
				up.set(forward.z, 0.0f, -forward.x);
		}
		up.normalize();
		Vector3f right = new Vector3f(forward).cross(up).normalize();
		up.set(right).cross(forward).normalize();
		return new PDeformerFrame(position, right, up, forward);
	}
}
