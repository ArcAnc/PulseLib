/**
 * @author ArcAnc
 * Created at: 30.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage61client;

import com.arcanc.pulselib.api.registration.PModelRegistrar;
import com.arcanc.pulselib.content.mixin.SpriteContentsAccessor;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.model.textures.PAlphaMode;
import com.arcanc.pulselib.content.model.textures.atlas.PLibSpriteMetadata;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.data.gecko.PGeckoModelLoader;
import com.arcanc.pulselib.data.gltf.PGltfModelLoader;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PResourceCache;
import com.mojang.blaze3d.opengl.GlTexture;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.lwjgl.opengl.GL45.GL_PACK_ALIGNMENT;
import static org.lwjgl.opengl.GL45.GL_PACK_ROW_LENGTH;
import static org.lwjgl.opengl.GL45.GL_PACK_SKIP_ROWS;
import static org.lwjgl.opengl.GL45.GL_PACK_SKIP_PIXELS;
import static org.lwjgl.opengl.GL45.GL_PACK_IMAGE_HEIGHT;
import static org.lwjgl.opengl.GL45.GL_PACK_SKIP_IMAGES;
import static org.lwjgl.opengl.GL45.GL_PIXEL_PACK_BUFFER_BINDING;
import static org.lwjgl.opengl.GL45.GL_PIXEL_PACK_BUFFER;
import static org.lwjgl.opengl.GL45.GL_RGBA;
import static org.lwjgl.opengl.GL45.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL45.glGetInteger;
import static org.lwjgl.opengl.GL45.glBindBuffer;
import static org.lwjgl.opengl.GL45.glPixelStorei;
import static org.lwjgl.opengl.GL45.glGetTextureSubImage;

/** Native atlas GPU pixels, metadata and both-format baked mesh witnesses across reload. */
public final class AtlasProbe
{
	private static final List<PModelData> MODELS = models();
	private static final PAlphaMode[] ALPHA = {PAlphaMode.OPAQUE, PAlphaMode.CUTOUT, PAlphaMode.TRANSLUCENT,
			PAlphaMode.OPAQUE, PAlphaMode.CUTOUT, PAlphaMode.TRANSLUCENT};
	private static final List<PBakedModel> PREVIOUS = new ArrayList<>();
	private static final List<TextureAtlasSprite> SPRITES = new ArrayList<>();
	private static @Nullable GlTexture oldTexture;
	private static @Nullable CompletableFuture<Void> reload;
	private static int before;
	private static int ticks;
	private static int epochs;
	private static int meshes;
	private static int uvChecks;
	private static int pixels;
	private static boolean completed;

	private AtlasProbe()
	{
	}

	private static List<PModelData> models()
	{
		List<PModelData> result = new ArrayList<>();
		for (int format = 0; format < 2; format++)
			for (int material = 0; material < 6; material++)
				result.add(PModelData.direct(PLibDatabase.rl("lifecycle/atlas_" + (format == 0 ? "gecko_" : "gltf_") + material),
						format == 0 ? PGeckoModelLoader.INSTANCE.id() : PGltfModelLoader.INSTANCE.id()));
		return result;
	}

	public static void models(PModelRegistrar registrar)
	{
		for (int index = 0; index < MODELS.size(); index++)
			registrar.model(MODELS.get(index)).texture("0", PLibDatabase.rl("lifecycle/atlas_" + index % 6));
	}

	public static void register()
	{
		ClientTickEvents.END_CLIENT_TICK.register(AtlasProbe::tick);
		ClientLifecycleEvents.CLIENT_STOPPING.register(minecraft ->
		{
			check(completed && epochs == 2, "both atlas epochs finish before shutdown");
			PLibDatabase.LOGGER.info("STAGE61B2A ATLAS SHUTDOWN PASS");
		});
	}

	private static void tick(Minecraft minecraft)
	{
		check(Files.isRegularFile(minecraft.gameDirectory.toPath().resolve(".pulselib-atlas")), "disposable atlas client marker");
		if (++ticks > 2400)
			throw new AssertionError("STAGE61B2A atlas reload timeout");
		if (completed || minecraft.getOverlay() != null || MODELS.stream().anyMatch(model -> model.getModel() == null))
			return;
		if (reload != null && !reload.isDone())
			return;
		if (reload != null)
			reload.join();
		check(ClientProbe.common == 1 && ClientProbe.client == 1, "registration phases remain once-only");
		verify();
		if (epochs == 1)
		{
			before = ClientProbe.models;
			reload = minecraft.reloadResourcePacks();
			return;
		}
		check(ClientProbe.models == before + 1, "one declaration collection per real reload");
		completed = true;
		PLibDatabase.LOGGER.info("STAGE61B2A ATLAS PASS: epochs={}, models={}, meshes={}, uvComparisons={}, gpuPixels={}, classifications=6, metadataOverrides=3, modelCollections={}",
				epochs, MODELS.size(), meshes, uvChecks, pixels, ClientProbe.models);
		minecraft.stop();
	}

	private static void verify()
	{
		var atlas = PResourceCache.getTextureAtlas();
		GlTexture texture = (GlTexture)atlas.getTexture();
		check(!texture.isClosed(), "native atlas texture is live");
		if (oldTexture != null)
			check(texture != oldTexture && oldTexture.isClosed(), "reload replaces and closes old native atlas texture");
		for (int material = 0; material < 6; material++)
		{
			var sprite = atlas.getSprite(PResourceCache.spriteId(PLibDatabase.rl("lifecycle/atlas_" + material)));
			check(sprite != atlas.missingSprite() && sprite.contents().width() == 2 && sprite.contents().height() == 2, "runtime source stitches actual PNG");
			var metadata = sprite.contents().getAdditionalMetadata(PLibSpriteMetadata.TYPE);
			if (material == 1 || material >= 3)
				check(metadata.isPresent() && metadata.get().emissive() == (material == 1 || material == 3), "atlas metadata decoding");
			if (material >= 3)
				check(metadata.get().alphaMode() == ALPHA[material], "explicit alpha metadata override");
			if (epochs > 0)
				check(sprite != SPRITES.get(material), "fresh sprites after reload");
			else
				SPRITES.add(sprite);
			pixels(texture, sprite, material);
		}
		for (int index = 0; index < MODELS.size(); index++)
		{
			int material = index % 6;
			var model = MODELS.get(index).getModel();
			check(model.bones().size() == 1 && model.bones().getFirst().meshes().size() == 1, "one real textured mesh per decoded model");
			if (epochs > 0)
				check(model != PREVIOUS.get(index) && model.bones().getFirst().meshes().getFirst() != PREVIOUS.get(index).bones().getFirst().meshes().getFirst(), "fresh model and baked mesh after reload");
			else
				PREVIOUS.add(model);
			var mesh = model.bones().getFirst().meshes().getFirst();
			check(mesh.alphaMode() == ALPHA[material] && mesh.isEmissive() == (material == 1 || material == 3), "baked alpha/emissive metadata and AUTO pixel classification");
			var id = PResourceCache.spriteId(PLibDatabase.rl("lifecycle/atlas_" + material));
			check(mesh.textureLocation().equals(id), "atlas-local sprite ID propagated to baked mesh");
			var sprite = atlas.getSprite(id);
			var geometry = mesh.geometry();
			var vertices = geometry.vertices().order(ByteOrder.nativeOrder());
			check(geometry.indexCount() == (index < 6 ? 36 : 3) && vertices.remaining() == mesh.source().vertexCount() * geometry.vertexStride(), "real indexed Gecko/glTF geometry");
			for (int vertex = 0; vertex < mesh.source().vertexCount(); vertex++)
			{
				float u = vertices.getFloat(vertex * geometry.vertexStride() + 12);
				float v = vertices.getFloat(vertex * geometry.vertexStride() + 16);
				check(Math.abs(u - sprite.getU(mesh.source().uvs().get(vertex * 2))) < 0.000001f &&
						Math.abs(v - sprite.getV(mesh.source().uvs().get(vertex * 2 + 1))) < 0.000001f, "packed geometry uses native atlas UVs");
				uvChecks += 2;
			}
			meshes++;
		}
		oldTexture = texture;
		epochs++;
	}

	private static void pixels(GlTexture texture, TextureAtlasSprite sprite, int material)
	{
		int alignment = glGetInteger(GL_PACK_ALIGNMENT);
		int rowLength = glGetInteger(GL_PACK_ROW_LENGTH);
		int skipRows = glGetInteger(GL_PACK_SKIP_ROWS);
		int skipPixels = glGetInteger(GL_PACK_SKIP_PIXELS);
		int buffer = glGetInteger(GL_PIXEL_PACK_BUFFER_BINDING);
		int imageHeight = glGetInteger(GL_PACK_IMAGE_HEIGHT);
		int skipImages = glGetInteger(GL_PACK_SKIP_IMAGES);
		try (MemoryStack stack = MemoryStack.stackPush())
		{
			glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);
			glPixelStorei(GL_PACK_ALIGNMENT, 1);
			glPixelStorei(GL_PACK_ROW_LENGTH, 0);
			glPixelStorei(GL_PACK_SKIP_ROWS, 0);
			glPixelStorei(GL_PACK_SKIP_PIXELS, 0);
			glPixelStorei(GL_PACK_IMAGE_HEIGHT, 0);
			glPixelStorei(GL_PACK_SKIP_IMAGES, 0);
			var data = stack.malloc(16);
			// Native sprite x/y include edge padding; UV zero is the first image texel.
			int x = Math.round(sprite.getU0() * texture.getWidth(0));
			int y = Math.round(sprite.getV0() * texture.getHeight(0));
			glGetTextureSubImage(texture.glId(), 0, x, y, 0, 2, 2, 1, GL_RGBA, GL_UNSIGNED_BYTE, data);
			for (int pixel = 0; pixel < 4; pixel++)
			{
				int alpha = material == 0 || material == 5 ? 255 : material == 1 ? pixel == 0 ? 0 : 255 : 128;
				int[] expected = {17 + material * 20, 40 + pixel * 30, 200 - pixel * 30, alpha};
				if (alpha == 0)
				{
					// Vanilla solidify fills transparent RGB before upload; alpha is still authored.
					int prepared = ((SpriteContentsAccessor)sprite.contents()).pulselib$getOriginalImage().getPixel(pixel % 2, pixel / 2);
					check((prepared >>> 24) == 0, "native solidify preserves transparent alpha");
					expected[0] = prepared >> 16 & 255;
					expected[1] = prepared >> 8 & 255;
					expected[2] = prepared & 255;
				}
				for (int channel = 0; channel < 4; channel++)
				{
					int actual = Byte.toUnsignedInt(data.get(pixel * 4 + channel));
					check(actual == expected[channel], "GPU atlas material=" + material + " pixel=" + pixel +
							" channel=" + channel + " expected=" + expected[channel] + " actual=" + actual);
				}
				pixels++;
			}
		}
		finally
		{
			glPixelStorei(GL_PACK_ALIGNMENT, alignment);
			glPixelStorei(GL_PACK_ROW_LENGTH, rowLength);
			glPixelStorei(GL_PACK_SKIP_ROWS, skipRows);
			glPixelStorei(GL_PACK_SKIP_PIXELS, skipPixels);
			glPixelStorei(GL_PACK_IMAGE_HEIGHT, imageHeight);
			glPixelStorei(GL_PACK_SKIP_IMAGES, skipImages);
			glBindBuffer(GL_PIXEL_PACK_BUFFER, buffer);
		}
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
			throw new AssertionError("STAGE61B2A: " + message);
	}
}
