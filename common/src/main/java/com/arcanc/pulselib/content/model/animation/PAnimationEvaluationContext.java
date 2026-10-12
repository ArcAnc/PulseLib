/**
 * @author ArcAnc
 * Created at: 05.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;

import com.arcanc.pulselib.data.gecko.MolangParser;
import com.arcanc.pulselib.data.gecko.PExpressionEvaluator;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Objects;

public final class PAnimationEvaluationContext
{
	private final MolangParser.Context molang;
	private final float animationTime;
	private final Vector3f temporaryVector = new Vector3f();
	private final Quaternionf temporaryQuaternion = new Quaternionf();

	public PAnimationEvaluationContext(MolangParser.Context molang)
	{
		this(molang, 0f);
	}

	public PAnimationEvaluationContext(MolangParser.Context molang, float animationTime)
	{
		this.molang = Objects.requireNonNull(molang);
		this.animationTime = animationTime;
	}

	public MolangParser.Context molang()
	{
		return this.molang;
	}

	public Vector3f temporaryVector()
	{
		return this.temporaryVector;
	}

	public Quaternionf temporaryQuaternion()
	{
		return this.temporaryQuaternion;
	}

	public void thisValues(Vector3f value)
	{
		this.molang.thisValues(value.x(), value.y(), value.z());
	}

	public float evaluate(MolangParser.Expression expression)
	{
		return PExpressionEvaluator.SHARED.evaluate(expression, this.molang, this.animationTime);
	}
}
