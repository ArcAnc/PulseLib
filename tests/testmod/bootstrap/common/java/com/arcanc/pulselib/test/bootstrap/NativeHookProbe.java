/**
 * @author ArcAnc
 * Created at: 11.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.bootstrap;

import com.arcanc.pulselib.content.animatable.AnimManagerKey;
import com.arcanc.pulselib.content.animatable.PAnimatable;
import com.arcanc.pulselib.content.animatable.PAnimationManager;
import com.arcanc.pulselib.content.animatable.instance.InstanceAnimationManager;
import com.arcanc.pulselib.content.mixin.ItemInHandRendererAccessor;
import com.arcanc.pulselib.content.mixin.RenderTypeAccessor;
import com.arcanc.pulselib.content.model.textures.atlas.RuntimeLoader;
import com.arcanc.pulselib.platform.PulseServices;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.PResourceCache;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.util.Map;

/** Native block removal and restored accessor witnesses before and after actual resource reload. */
public final class NativeHookProbe
{
	private static int epochs;

	private NativeHookProbe()
	{
	}

	public static void run(Minecraft mc)
	{
		try
		{
			blockRemoval(mc);
			for (var type : java.util.List.of(PRenderTypes.RenderTypeProvider.trianglesSolid(PResourceCache.ATLAS_LOCATION),
					PRenderTypes.RenderTypeProvider.trianglesTranslucent(PResourceCache.ATLAS_LOCATION)))
			{
				var setup = ((RenderTypeAccessor.CompositeRenderTypeAccessor)type).pulselib$getState();
				var access = (RenderTypeAccessor.CompositeStateAccessor)(Object)setup;
				BootstrapChecks.check(access.pulselib$getTransparencyState() == type.pipeline(), "Native render setup owns the same pipeline");
				BootstrapChecks.check(access.pulselib$getTransparencyState().getColorTargetState().blendFunction().equals(PRenderTypes.getTransparencyState(type)), "Canonical accessor exposes actual native blend state");
			}
			BootstrapChecks.check(mc.gameRenderer.itemInHandRenderer instanceof ItemInHandRendererAccessor, "Actual hand renderer implements canonical invoker");
			mc.gameRenderer.itemInHandRenderer.getClass().getMethod("pulselib$renderPlayerArm", PoseStack.class,
					SubmitNodeCollector.class, int.class, float.class, float.class, HumanoidArm.class);
			boolean fabric = PulseServices.PLATFORM.getClass().getName().endsWith("FabricPlatform");
			if (fabric)
			{
				var type = Class.forName("com.arcanc.pulselib.fabric.mixin.SpriteSourcesAccessor");
				var mapper = (net.minecraft.util.ExtraCodecs.LateBoundIdMapper<?, ?>)type.getMethod("pulselib$getTypes").invoke(null);
				BootstrapChecks.check(mapper.values().contains(RuntimeLoader.CODEC), "Canonical sprite accessor exposes registered runtime codec");
				levelChange(mc);
			}
			else
				Class.forName("com.arcanc.pulselib.neoforge.NeoForgeClientLifecycle").getConstructor().newInstance();
			PLibDatabase.LOGGER.info("NATIVE HOOK PASS: epoch={}, blockCases=4, renderStates=2, invoker=true, loader={}, levelChange={}", ++epochs, fabric ? "fabric" : "neoforge", fabric ? "PASS" : "prior-native-event");
		}
		catch (ReflectiveOperationException exception)
		{
			throw new IllegalStateException("Native hook application failed", exception);
		}
	}

	private static void levelChange(Minecraft mc) throws ReflectiveOperationException
	{
		ClientLevel original = mc.level;
		var sentinel = new OwnerBlock();
		var field = InstanceAnimationManager.class.getDeclaredField("MANAGERS");
		field.setAccessible(true);
		var tracked = (Map<?, ?>)field.get(null);
		try
		{
			InstanceAnimationManager.addManager(sentinel.manager);
			mc.setLevel(original);
			BootstrapChecks.check(tracked.containsKey(sentinel.manager), "Same ClientLevel preserves runtime managers");
			var replacement = new ClientLevel(mc.getConnection(), original.getLevelData(), original.dimension(),
					original.dimensionTypeRegistration(), 2, 2, mc.levelRenderer, false, 0L, original.getSeaLevel());
			mc.setLevel(replacement);
			BootstrapChecks.check(!tracked.containsKey(sentinel.manager), "Fabric ClientLevel replacement clears old runtime without disconnect");
		}
		finally
		{
			mc.setLevel(original);
			InstanceAnimationManager.removeManager(sentinel);
		}
	}

	private static void blockRemoval(Minecraft mc) throws ReflectiveOperationException
	{
		var first = new OwnerBlock();
		var second = new OwnerBlock();
		var unattached = new OwnerBlock();
		var server = new OwnerBlock();
		try
		{
			first.setLevel(mc.level);
			second.setLevel(mc.level);
			server.setLevel(mc.getSingleplayerServer().overworld());
			InstanceAnimationManager.addManager(first.manager);
			InstanceAnimationManager.addManager(second.manager);
			InstanceAnimationManager.addManager(unattached.manager);
			InstanceAnimationManager.addManager(server.manager);
			var field = InstanceAnimationManager.class.getDeclaredField("MANAGERS");
			field.setAccessible(true);
			var tracked = (Map<?, ?>)field.get(null);
			BootstrapChecks.check(tracked.containsKey(first.manager) && tracked.containsKey(second.manager), "Block managers tracked before native removal");
			first.setRemoved();
			BootstrapChecks.check(first.isRemoved() && !tracked.containsKey(first.manager) && tracked.containsKey(second.manager), "Native block removal releases only its owner manager");
			first.clearRemoved();
			InstanceAnimationManager.addManager(first.manager);
			first.setRemoved();
			first.setRemoved();
			BootstrapChecks.check(!tracked.containsKey(first.manager) && tracked.containsKey(second.manager), "Repeated native block removal is safe");
			unattached.setRemoved();
			BootstrapChecks.check(tracked.containsKey(unattached.manager), "Unattached block removal keeps the canonical null-level guard");
			server.setRemoved();
			BootstrapChecks.check(tracked.containsKey(server.manager), "Server-level block removal keeps the canonical client-side guard");
		}
		finally
		{
			for (var block : java.util.List.of(first, second, unattached, server))
				InstanceAnimationManager.removeManager(block);
		}
	}

	private static final class OwnerBlock extends ChestBlockEntity implements PAnimatable<OwnerBlock>
	{
		private final PAnimationManager<OwnerBlock> manager = new InstanceAnimationManager<>(this);

		private OwnerBlock()
		{
			super(BlockPos.ZERO, Blocks.CHEST.defaultBlockState());
		}

		@Override
		public PAnimationManager<OwnerBlock> getAnimationManager(AnimManagerKey key)
		{
			return this.manager;
		}

		@Override
		public void registerAnimationControllers(PAnimationManager.PAnimationRegistrar<OwnerBlock> registrar)
		{
		}
	}
}
