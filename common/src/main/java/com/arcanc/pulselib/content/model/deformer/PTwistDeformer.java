package com.arcanc.pulselib.content.model.deformer;
import com.arcanc.pulselib.util.PLibDatabase;
import com.mojang.serialization.MapCodec;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionf;
import org.joml.Vector3f;
/** Built-in linear twist implementation. */
public final class PTwistDeformer implements PMeshDeformer<PTwistDefinition>
{
	public static final PTwistDeformer INSTANCE = new PTwistDeformer();
	private static final float EPSILON = 1.0e-5f;

	/**
	 * Creates an instance of the enclosing type.
	 */
	private PTwistDeformer()
	{
	}

	/**
	 * Performs the id operation.
	 * @return the value produced by this operation.
	 */
	@Override
	public Identifier id()
	{
		return PLibDatabase.rl("twist");
	}

	/**
	 * Performs the codec operation.
	 * @return the value produced by this operation.
	 */
	@Override
	public MapCodec<PTwistDefinition> codec()
	{
		return PTwistDefinition.CODEC;
	}

	/**
	 * Performs the prepare operation.
	 * @param context the context to use.
	 * @param definition the definition to use.
	 */
	@Override
	public void prepare(PDeformerPrepareContext context, PTwistDefinition definition)
	{
		Vector3f length = new Vector3f(definition.lengthAxis());
		if (length.lengthSquared() < EPSILON * EPSILON)
			throw new IllegalArgumentException("lengthAxis must not be zero");
		context.add(new Operation(new Vector3f(definition.origin()), length.normalize(), definition.positiveExtent(), definition.negativeExtent(), definition.angle()));
	}

/**
 * Immutable value object representing operation.
 */
	private record Operation(Vector3f origin, Vector3f length, float positiveExtent, float negativeExtent,
							 PChannelReference<Float> angle) implements PPreparedDeformer
	{
		/**
		 * Performs the deform operation.
		 * @param position the position to use.
		 * @param values the values to use.
		 */
		@Override
		public void deform(Vector3f position, PDeformerValueSource values)
		{
			Vector3f relative = new Vector3f(position).sub(this.origin);
			float along = relative.dot(this.length);
			float clampedAlong = Math.clamp(along, -this.negativeExtent, this.positiveExtent);
			float twist = values.resolve(this.angle) * clampedAlong / (this.positiveExtent + this.negativeExtent);
			Quaternionf rotation = new Quaternionf().fromAxisAngleRad(this.length, twist);
			position.set(rotation.transform(relative).add(this.origin));
		}
	}
}
