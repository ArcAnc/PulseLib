/**
 * @author ArcAnc
 * Created at: 10.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.deformer.gpu;

import com.arcanc.pulselib.content.model.deformer.*;
import org.joml.Vector3f;

import java.util.*;

public final class PGpuDeformerStack
{
	public static final int MAX_OPERATIONS = 8;
	public static final int VEC4S_PER_OPERATION = 4;

	public static final int STRETCH = 1;
	public static final int SQUASH = 2;
	public static final int TAPER = 3;
	public static final int TWIST = 4;
	public static final int BEND = 5;
	public static final int WAVE = 6;
	public static final int HINGE = 7;

	private static final float EPSILON = 1.0e-5f;

	private final float[] operationData;
	private final List<PChannelReference<Float>> channels;
	private final int operationCount;

	private PGpuDeformerStack(float[] operationData, List<PChannelReference<Float>> channels, int operationCount)
	{
		this.operationData = operationData;
		this.channels = List.copyOf(channels);
		this.operationCount = operationCount;
	}

	public float[] operationData()
	{
		return this.operationData;
	}

	public List<PChannelReference<Float>> channels()
	{
		return this.channels;
	}

	public int operationCount()
	{
		return this.operationCount;
	}
	
	public static Optional<PGpuDeformerStack> compile(PDeformerStack stack)
	{
		if (stack.isEmpty())
			return Optional.of(new PGpuDeformerStack(new float[0], List.of(), 0));
		if (stack.definitions().size() > MAX_OPERATIONS)
			return Optional.empty();

		List<Float> data = new ArrayList<>(stack.definitions().size() * VEC4S_PER_OPERATION * 4);
		List<PChannelReference<Float>> channels = new ArrayList<>();
		Map<String, Integer> namedChannels = new HashMap<>();
		for (PDeformerInstance<?> instance : stack.definitions())
		{
			Object definition = instance.definition();
			switch (definition)
			{
				case PStretchDefinition stretch -> stretch(data, channels, namedChannels, stretch);
				case PSquashDefinition squash -> squash(data, channels, namedChannels, squash);
				case PTaperDefinition taper -> taper(data, channels, namedChannels, taper);
				case PTwistDefinition twist -> twist(data, channels, namedChannels, twist);
				case PBendDefinition bend -> bend(data, channels, namedChannels, bend);
				case PWaveDefinition wave -> wave(data, channels, namedChannels, wave);
				case PHingeDefinition hinge -> hinge(data, channels, namedChannels, hinge);
				default ->
				{
					return Optional.empty();
				}
			}
		}

		float[] packed = new float[data.size()];
		for (int index = 0; index < packed.length; index++)
			packed[index] = data.get(index);
		return Optional.of(new PGpuDeformerStack(packed, channels, stack.definitions().size()));
	}

	private static void stretch(List<Float> data, List<PChannelReference<Float>> channels, Map<String, Integer> names, PStretchDefinition d)
	{
		vec4(data, STRETCH, channel(channels, names, d.scale()), -1, 0);
		vec4(data, d.origin(), 0);
		vec4(data, unit(d.axis()), 0);
		vec4(data, 0, 0, 0, 0);
	}

	private static void squash(List<Float> data, List<PChannelReference<Float>> channels, Map<String, Integer> names, PSquashDefinition d)
	{
		vec4(data, SQUASH, channel(channels, names, d.scale()), -1, 0);
		vec4(data, d.origin(), 0);
		vec4(data, unit(d.axis()), 0);
		vec4(data, 0, 0, 0, 0);
	}

	private static void taper(List<Float> data, List<PChannelReference<Float>> channels, Map<String, Integer> names, PTaperDefinition d)
	{
		vec4(data, TAPER, channel(channels, names, d.tipScale()), -1, 0);
		vec4(data, d.origin(), d.positiveExtent());
		vec4(data, unit(d.lengthAxis()), d.negativeExtent());
		vec4(data, 0, 0, 0, 0);
	}

	private static void twist(List<Float> data, List<PChannelReference<Float>> channels, Map<String, Integer> names, PTwistDefinition d)
	{
		vec4(data, TWIST, channel(channels, names, d.angle()), -1, 0);
		vec4(data, d.origin(), d.positiveExtent());
		vec4(data, unit(d.lengthAxis()), d.negativeExtent());
		vec4(data, 0, 0, 0, 0);
	}

	private static void bend(List<Float> data, List<PChannelReference<Float>> channels, Map<String, Integer> names, PBendDefinition d)
	{
		Vector3f length = unit(d.lengthAxis());
		Vector3f axis = new Vector3f(d.bendAxis()).fma(-d.bendAxis().dot(length), length);
		if (axis.lengthSquared() < EPSILON * EPSILON)
			throw new IllegalArgumentException("bendAxis must not be parallel to lengthAxis");
		axis.normalize();
		vec4(data, BEND, channel(channels, names, d.angle()), -1, 0);
		vec4(data, d.origin(), d.positiveExtent());
		vec4(data, length, d.negativeExtent());
		vec4(data, axis, 0);
	}

	private static void wave(List<Float> data, List<PChannelReference<Float>> channels, Map<String, Integer> names, PWaveDefinition d)
	{
		Vector3f length = unit(d.lengthAxis());
		Vector3f displacement = new Vector3f(d.displacementAxis()).fma(-d.displacementAxis().dot(length), length);
		if (displacement.lengthSquared() < EPSILON * EPSILON)
			throw new IllegalArgumentException("displacementAxis must not be parallel to lengthAxis");
		displacement.normalize();
		vec4(data, WAVE, channel(channels, names, d.amplitude()), channel(channels, names, d.phase()), 0);
		vec4(data, d.origin(), d.positiveExtent());
		vec4(data, length, d.negativeExtent());
		vec4(data, displacement, d.wavelength());
	}

	private static void hinge(List<Float> data, List<PChannelReference<Float>> channels, Map<String, Integer> names, PHingeDefinition d)
	{
		Vector3f length = unit(d.lengthAxis());
		Vector3f hinge = new Vector3f(d.hingeAxis()).fma(-d.hingeAxis().dot(length), length);
		if (hinge.lengthSquared() < EPSILON * EPSILON)
			throw new IllegalArgumentException("hingeAxis must not be parallel to lengthAxis");
		vec4(data, HINGE, channel(channels, names, d.angle()), -1, 0);
		vec4(data, d.origin(), 0);
		vec4(data, length, 0);
		vec4(data, hinge.normalize(), 0);
	}

	private static int channel(List<PChannelReference<Float>> channels, Map<String, Integer> names, PChannelReference<Float> reference)
	{
		if (reference.name().isEmpty())
		{
			channels.add(reference);
			return channels.size() - 1;
		}
		Integer index = names.get(reference.name());
		if (index != null)
			return index;
		int result = channels.size();
		channels.add(reference);
		names.put(reference.name(), result);
		return result;
	}

	private static Vector3f unit(Vector3f axis)
	{
		Vector3f result = new Vector3f(axis);
		if (result.lengthSquared() < EPSILON * EPSILON)
			throw new IllegalArgumentException("Deformer axis must not be zero");
		return result.normalize();
	}

	private static void vec4(List<Float> data, Vector3f value, float w)
	{
		vec4(data, value.x, value.y, value.z, w);
	}

	private static void vec4(List<Float> data, float x, float y, float z, float w)
	{
		data.add(x);
		data.add(y);
		data.add(z);
		data.add(w);
	}
}
