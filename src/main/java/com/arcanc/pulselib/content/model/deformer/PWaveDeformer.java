/**
 * @author ArcAnc
 * Created at: 08.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.deformer;

import com.arcanc.pulselib.util.PLibDatabase;
import com.mojang.serialization.MapCodec;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;

/**
 * Deforms wave.
 */
public final class PWaveDeformer implements PMeshDeformer<PWaveDefinition>
{
	public static final PWaveDeformer INSTANCE = new PWaveDeformer();
	private static final float EPSILON = 1.0e-5f;

	/**
	 * Creates an instance of the enclosing type.
	 */
	private PWaveDeformer()
	{
	}

	/**
	 * Performs the id operation.
	 * @return the value produced by this operation.
	 */
	@Override
	public Identifier id()
	{
		return PLibDatabase.rl("wave");
	}

	/**
	 * Performs the codec operation.
	 * @return the value produced by this operation.
	 */
	@Override
	public MapCodec<PWaveDefinition> codec()
	{
		return PWaveDefinition.CODEC;
	}

	/**
	 * Performs the prepare operation.
	 * @param context the context to use.
	 * @param definition the definition to use.
	 */
	@Override
	public void prepare(PDeformerPrepareContext context, PWaveDefinition definition)
	{
		Vector3f length = unit(definition.lengthAxis(), "lengthAxis");
		Vector3f displacement = new Vector3f(definition.displacementAxis()).fma(-definition.displacementAxis().dot(length), length);
		if (displacement.lengthSquared() < EPSILON * EPSILON)
			throw new IllegalArgumentException("displacementAxis must not be parallel to lengthAxis");
		context.add(new Operation(new Vector3f(definition.origin()), length, displacement.normalize(), definition.positiveExtent(),
				definition.negativeExtent(), definition.wavelength(), definition.amplitude(), definition.phase()));
	}

	/**
	 * Performs the unit operation.
	 * @param axis the axis to use.
	 * @param name the name to use.
	 * @return the value produced by this operation.
	 */
	private static Vector3f unit(Vector3f axis, String name)
	{
		Vector3f result = new Vector3f(axis);
		if (result.lengthSquared() < EPSILON * EPSILON)
			throw new IllegalArgumentException(name + " must not be zero");
		return result.normalize();
	}

/**
 * Immutable value object representing operation.
 */
	private record Operation(Vector3f origin, Vector3f length, Vector3f displacement, float positiveExtent,
								 float negativeExtent, float wavelength, PChannelReference<Float> amplitude,
								 PChannelReference<Float> phase) implements PPreparedDeformer
	{
		/**
		 * Performs the deform operation.
		 * @param position the position to use.
		 * @param values the values to use.
		 */
		@Override
		public void deform(Vector3f position, PDeformerValueSource values)
		{
			float along = new Vector3f(position).sub(this.origin).dot(this.length);
			if (along < -this.negativeExtent || along > this.positiveExtent)
				return;
			float progress = (along + this.negativeExtent) / (this.positiveExtent + this.negativeExtent);
			float envelope = (float)Math.sin(Math.PI * progress);
			float angle = (float)(Math.PI * 2.0) * along / this.wavelength + values.resolve(this.phase);
			position.fma(values.resolve(this.amplitude) * envelope * (float)Math.sin(angle), this.displacement);
		}
	}
}
