package com.arcanc.pulselib.content.model.deformer;

import com.arcanc.pulselib.util.PLibDatabase;
import com.mojang.serialization.MapCodec;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;

/** Scales one axis and compensates the two perpendicular axes to preserve local volume. */
public final class PSquashDeformer implements PMeshDeformer<PSquashDefinition>
{
	public static final PSquashDeformer INSTANCE = new PSquashDeformer();
	private static final float EPSILON = 1.0e-5f;

	/**
	 * Creates an instance of the enclosing type.
	 */
	private PSquashDeformer()
	{
	}

	/**
	 * Performs the id operation.
	 * @return the value produced by this operation.
	 */
	@Override
	public Identifier id()
	{
		return PLibDatabase.rl("squash");
	}

	/**
	 * Performs the codec operation.
	 * @return the value produced by this operation.
	 */
	@Override
	public MapCodec<PSquashDefinition> codec()
	{
		return PSquashDefinition.CODEC;
	}

	/**
	 * Performs the prepare operation.
	 * @param context the context to use.
	 * @param definition the definition to use.
	 */
	@Override
	public void prepare(PDeformerPrepareContext context, PSquashDefinition definition)
	{
		Vector3f axis = new Vector3f(definition.axis());
		if (axis.lengthSquared() < EPSILON * EPSILON)
			throw new IllegalArgumentException("axis must not be zero");
		context.add(new Operation(new Vector3f(definition.origin()), axis.normalize(), definition.scale()));
	}

	private record Operation(Vector3f origin, Vector3f axis, PChannelReference<Float> scale) implements PPreparedDeformer
	{
		/**
		 * Performs the deform operation.
		 * @param position the position to use.
		 * @param values the values to use.
		 */
		@Override
		public void deform(Vector3f position, PDeformerValueSource values)
		{
			float axialScale = Math.max(values.resolve(this.scale), EPSILON);
			float radialScale = 1.0f / (float)Math.sqrt(axialScale);
			Vector3f relative = new Vector3f(position).sub(this.origin);
			Vector3f axial = new Vector3f(this.axis).mul(relative.dot(this.axis) * axialScale);
			Vector3f radial = relative.sub(new Vector3f(this.axis).mul(relative.dot(this.axis))).mul(radialScale);
			position.set(axial.add(radial).add(this.origin));
		}
	}
}
