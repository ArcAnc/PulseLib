/**
 * @author ArcAnc
 * Created at: 05.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.deformer;

import com.arcanc.pulselib.util.PLibDatabase;
import com.mojang.serialization.MapCodec;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class PTwistDeformer implements PMeshDeformer<PTwistDefinition>
{
	public static final PTwistDeformer INSTANCE = new PTwistDeformer();
	private static final float EPSILON = 1.0e-5f;

	private PTwistDeformer()
	{
	}

	@Override
	public ResourceLocation id()
	{
		return PLibDatabase.rl("twist");
	}

	@Override
	public MapCodec<PTwistDefinition> codec()
	{
		return PTwistDefinition.CODEC;
	}

	@Override
	public void prepare(PDeformerPrepareContext context, PTwistDefinition definition)
	{
		Vector3f length = new Vector3f(definition.lengthAxis());
		if (length.lengthSquared() < EPSILON * EPSILON)
			throw new IllegalArgumentException("lengthAxis must not be zero");
		context.add(new Operation(new Vector3f(definition.origin()), length.normalize(), definition.positiveExtent(), definition.negativeExtent(), definition.angle()));
	}

	private record Operation(Vector3f origin, Vector3f length, float positiveExtent, float negativeExtent,
							 PChannelReference<Float> angle) implements PPreparedDeformer
	{
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
