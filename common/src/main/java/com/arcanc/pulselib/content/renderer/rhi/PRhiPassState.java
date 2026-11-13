/**
 * @author ArcAnc
 * Created at: 13.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.rhi;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.ScissorState;
import org.jetbrains.annotations.ApiStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** One coherent, borrowed pass snapshot shared by every chunk of a material in one plan execution. */
@ApiStatus.Internal
public record PRhiPassState(PRhiMaterial.Attachments target, Map<String, GpuBufferSlice> uniforms,
		Map<String, PRhiMaterial.Texture> textures, Scissor scissor)
{
	public PRhiPassState
	{
		Objects.requireNonNull(target);
		Objects.requireNonNull(scissor);
		uniforms = Map.copyOf(uniforms);
		textures = Map.copyOf(textures);
	}

	static PRhiPassState resolve(PRhiMaterial material)
	{
		PRhiMaterial.Attachments target = material.target().get();
		Map<String, GpuBufferSlice> uniforms = new HashMap<>();
		Map<String, PRhiMaterial.Texture> textures = new HashMap<>();
		material.uniforms().forEach((name, value) -> uniforms.put(name, value.get()));
		material.textures().forEach((name, value) -> textures.put(name, value.get()));
		return new PRhiPassState(target, uniforms, textures, Scissor.DISABLED);
	}

	void validate(PRhiMaterial material)
	{
		if (!this.uniforms.keySet().equals(material.uniforms().keySet()) || !this.textures.keySet().equals(material.textures().keySet()))
			throw new IllegalArgumentException("Prepared RHI pass bindings differ from material declarations");
	}

	/** Immutable copy: later changes to Minecraft's mutable ScissorState cannot split a draw group. */
	public record Scissor(boolean enabled, int x, int y, int width, int height)
	{
		public static final Scissor DISABLED = new Scissor(false, 0, 0, 0, 0);

		public Scissor
		{
			if (enabled && (width < 0 || height < 0))
				throw new IllegalArgumentException("Negative RHI scissor extent");
		}

		public static Scissor copyOf(ScissorState state)
		{
			return new Scissor(state.enabled(), state.x(), state.y(), state.width(), state.height());
		}
	}
}
