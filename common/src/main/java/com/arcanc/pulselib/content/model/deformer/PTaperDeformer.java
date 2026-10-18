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
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

/**
 * Prepares deformation that varies transverse scale along a longitudinal axis.
 */
public final class PTaperDeformer implements PMeshDeformer<PTaperDefinition>
{
	/**
	 * Shared instance of this stateless implementation.
	 */
	public static final PTaperDeformer INSTANCE = new PTaperDeformer();
	private static final float EPSILON = 1.0e-5f;

	private PTaperDeformer()
	{
	}

	/**
	 * Returns the namespaced identifier of the taper deformer.
	 *
	 * @return the namespaced identifier of the taper deformer
	 */
	@Override
	public ResourceLocation id()
	{
		return PLibDatabase.rl("taper");
	}

	/**
	 * Returns the taper configuration codec.
	 *
	 * @return the taper configuration codec
	 */
	@Override
	public MapCodec<PTaperDefinition> codec()
	{
		return PTaperDefinition.CODEC;
	}

	/**
	 * Appends a prepared taper operation to the supplied context.
	 *
	 * @param context the destination collecting prepared deformation operations
	 * @param definition the immutable configuration to register or evaluate
	 */
	@Override
	public void prepare(PDeformerPrepareContext context, PTaperDefinition definition)
	{
		Vector3f axis = new Vector3f(definition.lengthAxis());
		if (axis.lengthSquared() < EPSILON * EPSILON)
			throw new IllegalArgumentException("lengthAxis must not be zero");
		context.add(new Operation(new Vector3f(definition.origin()), axis.normalize(), definition.positiveExtent(),
				definition.negativeExtent(), definition.tipScale()));
	}

	/**
	 * Prepared taper operation with resolved axes and channel reference.
	 *
	 * @param origin the local pivot or origin of the deformation
	 * @param axis the normalized deformation axis
	 * @param positiveExtent the positive-axis deformation extent
	 * @param negativeExtent the negative-axis deformation extent
	 * @param tipScale the tip scale channel reference and its fallback value
	 */
	private record Operation(Vector3f origin, Vector3f axis, float positiveExtent, float negativeExtent,
								 PChannelReference<Float> tipScale) implements PPreparedDeformer
	{
		/**
		 * Applies longitudinally varying transverse scale to the position in place.
		 *
		 * @param position the position
		 * @param values the source of current animated deformation channel values
		 */
		@Override
		public void deform(Vector3f position, PDeformerValueSource values)
		{
			Vector3f relative = new Vector3f(position).sub(this.origin);
			float along = relative.dot(this.axis);
			float progress = Math.clamp((along + this.negativeExtent) / (this.positiveExtent + this.negativeExtent), 0.0f, 1.0f);
			float scale = 1.0f + (Math.max(values.resolve(this.tipScale), EPSILON) - 1.0f) * progress;
			Vector3f axial = new Vector3f(this.axis).mul(along);
			position.set(relative.sub(axial).mul(scale).add(axial).add(this.origin));
		}
	}
}
