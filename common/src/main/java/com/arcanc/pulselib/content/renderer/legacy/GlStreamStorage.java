/**
 * @author ArcAnc
 * Created at: 13.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.legacy;

import com.arcanc.pulselib.content.model.deformer.gpu.PDeformerStream;
import com.arcanc.pulselib.content.model.deformer.gpu.PFrameStreams;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import java.nio.FloatBuffer;
import java.util.List;

public final class GlStreamStorage
{
	private static int operationsBuffer = -1;
	private static int operationsTexture = -1;
	private static int valuesBuffer = -1;
	private static int valuesTexture = -1;

	private GlStreamStorage()
	{
	}

	public static void bind(ShaderInstance shader, PFrameStreams streams)
	{
		upload(streams.deformers());
		int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
		int program = shader.getId();
		bindSampler(program, "DeformerOperations", 7, operationsTexture);
		bindSampler(program, "DeformerValues", 8, valuesTexture);
		GL13.glActiveTexture(activeTexture);
	}

	public static void cleanup()
	{
		if (operationsTexture != -1)
			GL11.glDeleteTextures(operationsTexture);
		if (valuesTexture != -1)
			GL11.glDeleteTextures(valuesTexture);
		if (operationsBuffer != -1)
			GL15.glDeleteBuffers(operationsBuffer);
		if (valuesBuffer != -1)
			GL15.glDeleteBuffers(valuesBuffer);
		operationsTexture = valuesTexture = operationsBuffer = valuesBuffer = -1;
	}

	private static void upload(PDeformerStream data)
	{
		ensureTextures();
		if (data.operationsDirty())
		{
			upload(operationsBuffer, data.operations(), GL15.GL_STATIC_DRAW);
			data.markOperationsUploaded();
		}
		if (data.valuesDirty())
		{
			upload(valuesBuffer, data.values(), GL15.GL_STREAM_DRAW);
			data.markValuesUploaded();
		}
	}

	private static void ensureTextures()
	{
		if (operationsBuffer != -1)
			return;
		operationsBuffer = GL15.glGenBuffers();
		operationsTexture = GL11.glGenTextures();
		valuesBuffer = GL15.glGenBuffers();
		valuesTexture = GL11.glGenTextures();
		GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, operationsBuffer);
		GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, 16L, GL15.GL_STATIC_DRAW);
		GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, operationsTexture);
		GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, GL30.GL_RGBA32F, operationsBuffer);
		GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, valuesBuffer);
		GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, 16L, GL15.GL_STREAM_DRAW);
		GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, valuesTexture);
		GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, GL30.GL_RGBA32F, valuesBuffer);
		GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, 0);
		GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, 0);
	}

	private static void bindSampler(int program, String name, int unit, int texture)
	{
		int location = GL20.glGetUniformLocation(program, name);
		if (location < 0)
			return;
		GL20.glUniform1i(location, unit);
		GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
		GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, texture);
	}

	private static void upload(int buffer, List<Float> values, int usage)
	{
		int vec4Count = Math.max(1, (values.size() + 3) / 4);
		FloatBuffer packed = BufferUtils.createFloatBuffer(vec4Count * 4);
		for (float value : values)
			packed.put(value);
		packed.flip();
		GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, buffer);
		GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, packed, usage);
		GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, 0);
	}
}
