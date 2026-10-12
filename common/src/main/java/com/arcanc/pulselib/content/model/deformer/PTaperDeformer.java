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

public final class PTaperDeformer implements PMeshDeformer<PTaperDefinition>
{
	public static final PTaperDeformer INSTANCE = new PTaperDeformer();
	private static final float EPSILON = 1.0e-5f;

	private PTaperDeformer()
	{
	}

	@Override
	public ResourceLocation id()
	{
		return PLibDatabase.rl("taper");
	}

	@Override
	public MapCodec<PTaperDefinition> codec()
	{
		return PTaperDefinition.CODEC;
	}

	@Override
	public void prepare(PDeformerPrepareContext context, PTaperDefinition definition)
	{
		Vector3f axis = new Vector3f(definition.lengthAxis());
		if (axis.lengthSquared() < EPSILON * EPSILON)
			throw new IllegalArgumentException("lengthAxis must not be zero");
		context.add(new Operation(new Vector3f(definition.origin()), axis.normalize(), definition.positiveExtent(),
				definition.negativeExtent(), definition.tipScale()));
	}

	private record Operation(Vector3f origin, Vector3f axis, float positiveExtent, float negativeExtent,
								 PChannelReference<Float> tipScale) implements PPreparedDeformer
	{
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
