package com.arcanc.pulselib.fabric.mixin;

import com.arcanc.pulselib.util.PResourceCache;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.LinkedHashMap;
import java.util.Map;

@Mixin(ModelManager.class)
public abstract class ModelManagerMixin
{
	@ModifyArg(method = "<init>", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/resources/model/AtlasSet;<init>(Ljava/util/Map;Lnet/minecraft/client/renderer/texture/TextureManager;)V"), index = 0)
	private Map<ResourceLocation, ResourceLocation> pulselib$addAtlas(Map<ResourceLocation, ResourceLocation> original)
	{
		Map<ResourceLocation, ResourceLocation> atlases = new LinkedHashMap<>(original);
		atlases.put(PResourceCache.ATLAS_LOCATION, PResourceCache.ATLAS_FILE_LOCATION);
		return atlases;
	}
}
