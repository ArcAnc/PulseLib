/**
 * @author ArcAnc
 * Created at: 27.02.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.registration.entity.renderer;


import com.arcanc.pulselib.content.model.baked.PBakedBone;
import com.arcanc.pulselib.content.model.baked.PBakedMesh;
import com.arcanc.pulselib.content.model.baked.PMeshRenderContext;
import com.arcanc.pulselib.content.model.deformer.PChannelReference;
import com.arcanc.pulselib.content.model.deformer.PDeformerInstance;
import com.arcanc.pulselib.content.model.deformer.PDeformerStack;
import com.arcanc.pulselib.content.model.deformer.PHingeDeformer;
import com.arcanc.pulselib.content.model.deformer.PHingeDefinition;
import com.arcanc.pulselib.content.model.deformer.PMeshDeformation;
import com.arcanc.pulselib.content.registration.entity.TestEntity;
import com.arcanc.pulselib.content.registration.renderer.TestDayTimeColor;
import com.arcanc.pulselib.content.renderer.PEntityRenderer;
import com.arcanc.pulselib.content.renderer.base.PEntityRenderState;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.helpers.PLibHelper;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;

import java.util.List;

public class TestEntityRender extends PEntityRenderer<TestEntity, PEntityRenderState.LivingImpl<TestEntity>>
{
	public static final PModelData MODEL_DATA = PModelData.entity(PLibDatabase.rl("test_entity"));

	public static final Identifier TUBE = PLibDatabase.rl("entity/test_entity/tube");
	public static final Identifier SPHERE = PLibDatabase.rl("entity/test_entity/sphere");
	public static final Identifier TORUS = PLibDatabase.rl("entity/test_entity/torus");
	public static final Identifier ZERO = PLibDatabase.rl("entity/test_entity/0");
	public static final Identifier ARMOR = PLibDatabase.rl("entity/test_entity/armor/0");

	private static final PChannelReference<Float> ARM_HINGE_ANGLE = new PChannelReference<>("test_entity_arm_hinge_angle", 0.0f);
	private static final PDeformerStack ARM_HINGE = PDeformerStack.compile(List.of(
			new PDeformerInstance<>(PHingeDeformer.INSTANCE, new PHingeDefinition(
					new Vector3f(0.0f, -0.25f, 0.0f),
					new Vector3f(0.0f, -1.0f, 0.0f),
					new Vector3f(1.0f, 0.0f, 0.0f),
					ARM_HINGE_ANGLE))));
	private final Object leftArmHingeKey = new Object();
	private final Object rightArmHingeKey = new Object();
	
	public TestEntityRender(EntityRendererProvider.Context context)
	{
		super(context, MODEL_DATA,
				PRenderTypes.RenderTypeProvider :: trianglesTranslucent);
		
		addRenderLayer("body", new PTestArmor().
				bindBone("armor_chest", "body").
				bindBone("armor_helm", "head").
				bindBone("armor_left_hand", "hand_left"));
	}
	
	@Override
	public PEntityRenderState.LivingImpl<TestEntity> createRenderState()
	{
		return PLibHelper.livingRenderState();
	}
	
	@Override
	protected PMeshRenderContext resolveMeshRender(PEntityRenderState.LivingImpl<TestEntity> renderState,
	                                               PBakedBone bone,
	                                               PBakedMesh mesh,
	                                               PMeshRenderContext inherited)
	{
		PMeshRenderContext context = inherited.withColor(
				TestDayTimeColor.color(renderState.getAnimatable().level(), renderState.partialTick()));
		TestEntity animatable = renderState.getAnimatable();
		if (bone.name().equals("head"))
		{
			boolean alternateMaterial = (animatable.tickCount / 40 & 1) == 0;
			context = context.withTexture(alternateMaterial ? TORUS : ZERO).
					withEmissive(alternateMaterial);
		}
		if (!bone.name().equals("hand_left") && !bone.name().equals("hand_right"))
			return context;

		float phase = (animatable.tickCount + renderState.partialTick()) * 0.35f;
		if (bone.name().equals("hand_right"))
			phase += (float)Math.PI;
		float resolvedAngle = (float)Math.sin(phase) * 0.35f;
		Object cacheKey = bone.name().equals("hand_left") ? this.leftArmHingeKey : this.rightArmHingeKey;
		return context.withDeformation(new PMeshDeformation(
				ARM_HINGE,
				reference -> reference.name().equals(ARM_HINGE_ANGLE.name()) ? resolvedAngle : reference.defaultValue(),
				cacheKey,
				2));
	}
}
