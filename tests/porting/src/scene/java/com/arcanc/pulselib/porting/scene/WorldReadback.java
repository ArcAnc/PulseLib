/**
 * @author ArcAnc
 * Created at: 15.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.porting.scene;

import com.arcanc.pulselib.util.PRenderTypes;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.Optional;

/** Test-only native readback. No framebuffer replacement, postchain substitution or GL handles. */
final class WorldReadback
{
	private WorldReadback()
	{
	}

	static Samples read(GpuTexture texture, int probeX, @Nullable String imageName) throws Exception
	{
		if (texture.getFormat().hasDepthAspect())
			return depth(texture, probeX);
		var device = RenderSystem.getDevice();
		int width = texture.getWidth(0), height = texture.getHeight(0);
		GpuFormat format = texture.getFormat();
		int stride = format.blockSize();
		try (var buffer = device.createBuffer(() -> "World native acceptance readback",
				GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_READ, (long)width * height * stride))
		{
			var encoder = device.createCommandEncoder();
			encoder.copyTextureToBuffer(texture, buffer, 0, () -> {}, 0);
			try (var fence = encoder.createFence())
			{
				encoder.submit();
				SceneChecks.check(fence.awaitCompletion(Long.MAX_VALUE), "World readback fence completed");
			}
			try (var mapped = buffer.map(true, false))
			{
				ByteBuffer bytes = mapped.data().order(ByteOrder.nativeOrder());
				float[] center = pixel(bytes, (height / 2 * width + width / 2) * stride, format);
				float[] probe = pixel(bytes, (height / 2 * width + probeX) * stride, format);
				int nonempty = 0;
				for (int p = 0; p < width * height; p++)
					if (component(bytes, p * stride, format, format == GpuFormat.D32_FLOAT ? 0 : 3) > 0)
						nonempty++;
				if (imageName != null)
					try (var image = new NativeImage(width, height, false))
					{
						for (int y = 0; y < height; y++)
							for (int x = 0; x < width; x++)
								image.setPixel(x, y, packed(pixel(bytes, (y * width + x) * stride, format)));
						image.writeToFile(Path.of(imageName));
					}
				return new Samples(center, probe, nonempty);
			}
		}
	}

	private static Samples depth(GpuTexture texture, int probeX) throws Exception
	{
		var device = RenderSystem.getDevice();
		try (var converted = device.createTexture("World RHI depth readback", GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_COPY_SRC,
				GpuFormat.RG32_FLOAT, texture.getWidth(0), texture.getHeight(0), 1, 1);
				var target = device.createTextureView(converted);
				var source = device.createTextureView(texture))
		{
			try (var pass = device.createCommandEncoder().createRenderPass(() -> "World depth to float color", target, Optional.empty()))
			{
				pass.setPipeline(PRenderTypes.ShadersProvider.oitFabulousMetadata());
				pass.disableScissor();
				var sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
				pass.bindTexture("RevealSampler", source, sampler);
				pass.bindTexture("DepthSampler", source, sampler);
				pass.draw(3, 1, 0, 0);
			}
			return read(converted, probeX, null);
		}
	}

	private static float[] pixel(ByteBuffer bytes, int offset, GpuFormat format)
	{
		if (format == GpuFormat.RG32_FLOAT)
			return new float[]{bytes.getFloat(offset), bytes.getFloat(offset + 4), 0, 1};
		return new float[]{component(bytes, offset, format, 0), component(bytes, offset, format, 1),
				component(bytes, offset, format, 2), component(bytes, offset, format, 3)};
	}

	private static float component(ByteBuffer bytes, int offset, GpuFormat format, int channel)
	{
		return switch (format)
		{
			case RGBA8_UNORM -> (bytes.get(offset + channel) & 255) / 255f;
			case RGBA16_FLOAT -> Float.float16ToFloat(bytes.getShort(offset + channel * 2));
			case RG32_FLOAT -> channel < 2 ? bytes.getFloat(offset + channel * 4) : channel == 3 ? 1 : 0;
			case RGBA32_FLOAT -> bytes.getFloat(offset + channel * 4);
			case D32_FLOAT -> bytes.getFloat(offset);
			default -> throw new IllegalArgumentException("Unverified world readback format " + format);
		};
	}

	static int packed(float[] color)
	{
		return (Math.round(Math.clamp(color[3], 0, 1) * 255) << 24) | (Math.round(Math.clamp(color[0], 0, 1) * 255) << 16) |
				(Math.round(Math.clamp(color[1], 0, 1) * 255) << 8) | Math.round(Math.clamp(color[2], 0, 1) * 255);
	}

	record Samples(float[] center, float[] probe, int nonempty)
	{
	}
}
