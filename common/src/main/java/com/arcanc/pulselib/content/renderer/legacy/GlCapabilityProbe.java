/**
 * @author ArcAnc
 * Created at: 13.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.legacy;

import com.arcanc.pulselib.content.renderer.plan.PRenderCapabilityMatrix;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

public final class GlCapabilityProbe
{
	private GlCapabilityProbe()
	{
	}

	public static PRenderCapabilityMatrix currentContext()
	{
		GLCapabilities gl = GL.getCapabilities();
		return new PRenderCapabilityMatrix(
				gl.OpenGL31 || gl.GL_ARB_texture_buffer_object,
				gl.OpenGL43 || gl.GL_ARB_shader_storage_buffer_object,
				gl.OpenGL43 || gl.GL_ARB_multi_draw_indirect,
				gl.OpenGL42 || gl.GL_ARB_base_instance,
				gl.OpenGL44 || gl.GL_ARB_buffer_storage);
	}
}
