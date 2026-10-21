/**
 * @author ArcAnc
 * Created at: 21.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1;

import com.arcanc.pulselib.content.animatable.AnimManagerKey;
import com.arcanc.pulselib.content.animatable.ControllerState;
import com.arcanc.pulselib.content.animatable.PAnimatable;
import com.arcanc.pulselib.content.animatable.PAnimationController;
import com.arcanc.pulselib.content.animatable.PAnimationManager;
import com.arcanc.pulselib.content.registration.item.renderer.TestBlockItemRenderer;
import com.arcanc.pulselib.data.gecko.MolangParser;
import com.arcanc.pulselib.util.PLibDatabase;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Renderer assertions from canonical PAnimationTimeTest, run with real MC26 GPU contexts. */
public final class Stage32TimeProbe
{
	public static @Nullable Object entityRenderer;
	public static @Nullable Object blockRenderer;

	private Stage32TimeProbe()
	{
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static void verify() throws ReflectiveOperationException
	{
		if (entityRenderer == null || blockRenderer == null)
		{
			throw new AssertionError("Real entity/block renderer factories must have executed");
		}
		Object item = new TestBlockItemRenderer(TestBlockItemRenderer.MODEL_DATA);
		var expression = MolangParser.parse("query.anim_time");
		int assertions = 0;
		for (float ticks : new float[]{0, 10, 20, 30})
		{
			PAnimationController<Dummy> controller = new PAnimationController<>(state -> ControllerState.STOP)
			{
				@Override
				public float getInterpolatedTime(float partialTick)
				{
					return ticks + partialTick * 2;
				}
			};
			for (float partialTick : new float[]{0, 0.25f, 0.5f, 0.75f, 1})
			{
				float expected = (ticks + partialTick * 2) / 20;
				for (Object renderer : List.of(entityRenderer, blockRenderer, item))
				{
					Class<?> owner = renderer.getClass().getSuperclass();
					// TestEntityRender extends PLivingEntityRenderer, then PEntityRenderer.
					while (Arrays.stream(owner.getDeclaredMethods()).noneMatch(method -> method.getName().equals("prepareMolangContexts")))
					{
						owner = owner.getSuperclass();
					}
					Method prepare = Arrays.stream(owner.getDeclaredMethods()).
							filter(method -> method.getName().equals("prepareMolangContexts")).findFirst().orElseThrow();
					prepare.setAccessible(true);
					Map<PAnimationController<?>, MolangParser.Context> contexts = (Map<PAnimationController<?>, MolangParser.Context>)
							prepare.invoke(renderer, null, new PAnimationManager(new Dummy(), new AnimManagerKey(1)), List.of(controller), partialTick);
					float actual = expression.evaluate(contexts.get(controller));
					if (!Float.isFinite(actual) || Math.abs(expected - actual) > 0.000001f)
					{
						throw new AssertionError(owner.getSimpleName() + " anim_time seconds: expected " + expected + ", got " + actual);
					}
					assertions++;
				}
			}
		}
		PLibDatabase.LOGGER.info("STAGE 3.2 RENDERER TIME PASS: {} assertions, Entity/BlockEntity/Item, interpolated seconds", assertions);
	}

	/** Captures vanilla lighting inputs for interpreting the renderer smoke images. */
	public static void lighting(int tick)
	{
		var level = Minecraft.getInstance().level;
		if (level == null)
		{
			throw new AssertionError("Lighting evidence requires a loaded client world");
		}
		PLibDatabase.LOGGER.info("STAGE 3.2 VANILLA LIGHT INPUTS: tick={}, leftSky={}, rightSky={}, leftRaw={}, rightRaw={}",
				tick, level.getBrightness(LightLayer.SKY, new BlockPos(-1, 80, 10)),
				level.getBrightness(LightLayer.SKY, new BlockPos(1, 80, 10)),
				level.getRawBrightness(new BlockPos(-1, 80, 10), 0),
				level.getRawBrightness(new BlockPos(1, 80, 10), 0));
	}

	private static final class Dummy implements PAnimatable<Dummy>
	{
		@Override
		public PAnimationManager<Dummy> getAnimationManager(AnimManagerKey key)
		{
			throw new UnsupportedOperationException("Time probe uses explicit controllers");
		}

		@Override
		public void registerAnimationControllers(PAnimationManager.PAnimationRegistrar<Dummy> registrar)
		{
		}
	}
}
