/**
 * @author ArcAnc
 * Created at: 27.02.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.testmod;

import com.arcanc.pulselib.content.animatable.AnimManagerKey;
import com.arcanc.pulselib.content.animatable.ControllerState;
import com.arcanc.pulselib.content.animatable.PAnimatable;
import com.arcanc.pulselib.content.animatable.PAnimationManager;
import com.arcanc.pulselib.content.model.animation.PRawAnimation;
import com.arcanc.pulselib.util.helpers.PLibHelper;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class TestCreature extends PathfinderMob implements PAnimatable<TestCreature>
{
	private static final EntityDataAccessor<Boolean> SHOW_ARMOR = SynchedEntityData.defineId(TestCreature.class, EntityDataSerializers.BOOLEAN);
	private static final PRawAnimation WALK = PRawAnimation.begin().thenLoop("walk").build();
	private static final PRawAnimation IDLE = PRawAnimation.begin().thenLoop("idle").build();

	private final PAnimationManager<TestCreature> animationManager = PLibHelper.createManager(this);

	public TestCreature(EntityType<? extends TestCreature> type, Level level)
	{
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes()
	{
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.2);
	}

	@Override
	protected void registerGoals()
	{
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		super.defineSynchedData(builder);
		builder.define(SHOW_ARMOR, true);
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand)
	{
		if (!this.level().isClientSide())
			this.entityData.set(SHOW_ARMOR, !showsArmor());
		return InteractionResult.SUCCESS;
	}

	public boolean showsArmor()
	{
		return this.entityData.get(SHOW_ARMOR);
	}

	@Override
	public PAnimationManager<TestCreature> getAnimationManager(AnimManagerKey key)
	{
		return this.animationManager;
	}

	@Override
	public void registerAnimationControllers(PAnimationManager.PAnimationRegistrar<TestCreature> registrar)
	{
		registrar.add("movement", () -> state ->
		{
			state.controller().play(state.animatable().walkAnimation.isMoving() ? WALK : IDLE);
			return ControllerState.PLAY;
		});
	}
}
