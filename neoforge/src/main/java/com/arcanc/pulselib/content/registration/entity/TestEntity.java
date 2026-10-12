/**
 * @author ArcAnc
 * Created at: 27.02.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.registration.entity;


import com.arcanc.pulselib.content.animatable.AnimManagerKey;
import com.arcanc.pulselib.content.animatable.ControllerState;
import com.arcanc.pulselib.content.animatable.PAnimatable;
import com.arcanc.pulselib.content.animatable.PAnimationManager;
import com.arcanc.pulselib.content.model.animation.PRawAnimation;
import com.arcanc.pulselib.util.helpers.PLibHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class TestEntity extends PathfinderMob implements PAnimatable<TestEntity>
{
	private static final PRawAnimation ANIMATION = PRawAnimation.begin().
			thenPlay("animation").
			thenWait(20).
			thenLoop("animation").
			build();
	
	private static final PRawAnimation ATTACK = PRawAnimation.begin().thenPlay("attack").build();
	private static final PRawAnimation WALK = PRawAnimation.begin().thenLoop("walk").build();
	private static final PRawAnimation IDLE = PRawAnimation.begin().thenLoop("idle").build();
	private static final PRawAnimation DEATH = PRawAnimation.begin().thenHold("death").build();
	
	private final PAnimationManager<TestEntity> animationManager = PLibHelper.createManager(this);
	
	public boolean showArmor = false;
	
	public TestEntity(EntityType<? extends PathfinderMob> type, Level level)
	{
		super(type, level);
	}
	
	public static AttributeSupplier.Builder createAttributes()
	{
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.2F);
	}
	
	@Override
	protected void registerGoals()
	{
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
		this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
	}
	
	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand)
	{
		this.showArmor = !this.showArmor;
		return InteractionResult.SUCCESS;
	}
	
	@Override
	public PAnimationManager<TestEntity> getAnimationManager(AnimManagerKey key)
	{
		return this.animationManager;
	}
	
	@Override
	public void registerAnimationControllers(PAnimationManager.@NotNull PAnimationRegistrar<TestEntity> registrar)
	{
		registrar.add("animController", () -> state ->
		{
			TestEntity animatable = state.animatable();
			if (animatable.swinging)
				state.controller().play(ATTACK);
			else
				state.controller().play(animatable.walkAnimation.isMoving() ? WALK : IDLE);
			return state.controller().getState();
		}).
				add("deathController", () -> state ->
		{
			if (!state.animatable().isDeadOrDying())
				return ControllerState.STOP;
			state.controller().play(DEATH);
			return ControllerState.PLAY;
		});
		
		/*registrar.add(() -> state ->
		{
			state.controller().play(ANIMATION);
			return ControllerState.PLAY;
		});*/
	}
}
