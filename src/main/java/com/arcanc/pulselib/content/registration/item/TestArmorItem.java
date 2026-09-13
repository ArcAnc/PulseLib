/**
 * @author ArcAnc
 * Created at: 05.07.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.registration.item;


import com.arcanc.pulselib.content.model.animation.PRawAnimation;
import com.arcanc.pulselib.content.model.baked.PBakedBone;
import com.arcanc.pulselib.content.model.baked.PBakedMesh;
import com.arcanc.pulselib.content.model.baked.PMeshRenderContext;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.util.PLibDatabase;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class TestArmorItem extends Item
{
	public static final Identifier TEXTURE = PLibDatabase.rl("entity/armor/test_armor/0");
	public static final PModelData MODEL_DATA = new PModelData.Builder(PLibDatabase.rl("armor/test_armor"), "entity").build();
	private final PRawAnimation tailSwing = PRawAnimation.begin().
			thenLoop("swing").
			build();

	/**
	 * Creates an instance of the enclosing type.
	 * @param properties the properties to use.
	 */
	public TestArmorItem(Properties properties)
	{
		super(properties);
	}
	
	/**
	 * Resolves the armor render.
	 * @param entity the entity to use.
	 * @param stack the stack to use.
	 * @param bone the bone to use.
	 * @param mesh the mesh to use.
	 * @param inherited the inherited to use.
	 * @param partialTick the partial tick to use.
	 * @return the value produced by this operation.
	 */
	public static PMeshRenderContext resolveArmorRender(LivingEntity entity,
	                                                    ItemStack stack,
	                                                    PBakedBone bone,
	                                                    PBakedMesh mesh,
	                                                    PMeshRenderContext inherited,
	                                                    float partialTick)
	{
		return inherited.withColor(dayTimeColor(entity, partialTick));
	}
	
	/**
	 * Performs the day time color operation.
	 * @param entity the entity to use.
	 * @param partialTick the partial tick to use.
	 * @return the value produced by this operation.
	 */
	private static int dayTimeColor(LivingEntity entity, float partialTick)
	{
		float time = ((entity.level().getOverworldClockTime() % 24000L) + partialTick) / 24000f;
		
		if (time < 0.25f)
			return lerpColor(0xFFFFD36A, 0xFFFFFFFF, time / 0.25f);
		if (time < 0.50f)
			return lerpColor(0xFFFFFFFF, 0xFFFF9A3D, (time - 0.25f) / 0.25f);
		if (time < 0.75f)
			return lerpColor(0xFFFF9A3D, 0xFF5E7CFF, (time - 0.50f) / 0.25f);
		return lerpColor(0xFF5E7CFF, 0xFFFFD36A, (time - 0.75f) / 0.25f);
	}
	
	/**
	 * Performs the lerp color operation.
	 * @param from the from to use.
	 * @param to the to to use.
	 * @param delta the delta to use.
	 * @return the value produced by this operation.
	 */
	private static int lerpColor(int from, int to, float delta)
	{
		int alpha = lerp((from >>> 24) & 0xFF, (to >>> 24) & 0xFF, delta);
		int red = lerp((from >>> 16) & 0xFF, (to >>> 16) & 0xFF, delta);
		int green = lerp((from >>> 8) & 0xFF, (to >>> 8) & 0xFF, delta);
		int blue = lerp(from & 0xFF, to & 0xFF, delta);
		
		return alpha << 24 | red << 16 | green << 8 | blue;
	}
	
	/**
	 * Performs the lerp operation.
	 * @param from the from to use.
	 * @param to the to to use.
	 * @param delta the delta to use.
	 * @return the value produced by this operation.
	 */
	private static int lerp(int from, int to, float delta)
	{
		return (int)(from + (to - from) * delta);
	}
}
