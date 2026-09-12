package com.arcanc.pulselib.content.player.animation.firstPerson;

import com.arcanc.pulselib.content.model.animation.PTransform;
import org.joml.Matrix4f;

import java.util.Objects;

/** A change of basis between an imported first-person asset and Pulse space. */
public final class PFirstPersonBasis
{
	public static final PFirstPersonBasis IDENTITY = new PFirstPersonBasis(new Matrix4f());

	private final Matrix4f sourceToPulse;
	private final Matrix4f pulseToSource;

	public PFirstPersonBasis(Matrix4f sourceToPulse)
	{
		this.sourceToPulse = new Matrix4f(Objects.requireNonNull(sourceToPulse));
		this.pulseToSource = new Matrix4f(sourceToPulse).invert();
	}

	/** Returns {@code C * transform * C^-1}. */
	public PTransform convert(PTransform transform)
	{
		Matrix4f matrix = new Matrix4f(this.sourceToPulse).
				mul(transform.matrix()).
				mul(this.pulseToSource);
		return PTransform.fromMatrix(matrix);
	}

}
