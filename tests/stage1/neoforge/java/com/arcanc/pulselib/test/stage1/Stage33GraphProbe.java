/**
 * @author ArcAnc
 * Created at: 22.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1;

import com.arcanc.pulselib.content.animatable.PAnimationController;
import com.arcanc.pulselib.content.model.animation.PAnimationGraph;
import com.arcanc.pulselib.content.model.animation.PAnimationState;
import com.arcanc.pulselib.content.model.animation.PAnimationTransition;
import com.arcanc.pulselib.content.model.animation.PCondition;
import com.arcanc.pulselib.content.model.animation.PInterpolationType;
import com.arcanc.pulselib.content.model.animation.PInterruptionPolicy;
import com.arcanc.pulselib.content.registration.entity.TestEntity;
import com.arcanc.pulselib.content.registration.entity.renderer.TestEntityRender;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.helpers.PLibHelper;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/** Exercises real renderer owners across graph restart, overlay controls and resource reload. */
public final class Stage33GraphProbe
{
	private static final List<PAnimationController<TestEntity>> CONTROLLERS = new ArrayList<>();
	private static boolean reverseObserved;
	private static boolean restartObserved;

	private Stage33GraphProbe()
	{
	}

	public static void tick(int tick)
	{
		if (tick == 80)
		{
			var model = TestEntityRender.MODEL_DATA.getModel();
			check(model != null && model.animations().containsKey("idle") && model.animations().containsKey("walk"),
					"Original glTF clips must be loaded");
			var graph = new PAnimationGraph(List.of(new PAnimationState.Clip("idle", "idle"),
					new PAnimationState.Clip("walk", "walk"),
					new PAnimationState.OneShotOverlay("hold", "hold", "idle", 1, 2, PInterpolationType.LINEAR, 0, false)),
					List.of(new PAnimationTransition(0, 1, PCondition.triggered("walk"), 20, 0, PInterruptionPolicy.FROM_CURRENT),
							new PAnimationTransition(1, 0, PCondition.triggered("restart"), 20, 0, PInterruptionPolicy.RESTART)));
			for (var entity : Minecraft.getInstance().level.entitiesForRendering())
			{
				if (entity instanceof TestEntity owner)
				{
					var manager = PLibHelper.getManager(owner);
					var controller = new PAnimationController<TestEntity>("animController", graph);
					manager.getControllers().put("animController", controller);
					CONTROLLERS.add(controller);
				}
			}
			check(CONTROLLERS.size() == 2, "Scene must have two independent animated entities");
			check(CONTROLLERS.getFirst().graphRuntime() != CONTROLLERS.getLast().graphRuntime(), "Owners must not share graph state");
		}
		if (CONTROLLERS.isEmpty())
		{
			return;
		}
		var first = CONTROLLERS.getFirst();
		var graph = first.graphRuntime();
		var model = TestEntityRender.MODEL_DATA.getModel();
		if (tick == 90)
		{
			first.trigger("walk").trigger("hold");
		}
		if (tick == 95)
		{
			check(graph.overlaySpeed("hold") == 0, "Zero-speed overlay must remain active");
			check(graph.layers(model).stream().anyMatch(layer -> layer.overlay() && layer.time() == 0), "Paused overlay must hold its cursor");
			check(CONTROLLERS.getLast().graphRuntime().layers(model).stream().noneMatch(layer -> layer.overlay()), "Second owner must remain independent");
			first.trigger("restart");
		}
		if (tick == 98)
		{
			check(graph.isTransitioning() && graph.stateIndex() == 0, "RESTART must interrupt the active transition");
			restartObserved = true;
			graph.setOverlaySpeed("hold", -1);
			check(graph.restartOverlay("hold"), "Reverse overlay restart must find the active slot");
		}
		if (tick == 103)
		{
			check(graph.overlaySpeed("hold") == -1, "Overlay must preserve negative speed");
			check(graph.layers(model).stream().anyMatch(layer -> layer.overlay() && layer.time() > 0), "Reverse overlay must sample the original clip");
			reverseObserved = true;
			check(graph.stopOverlay("hold"), "Overlay stop must start fade-out");
		}
		if (tick == 110)
		{
			check(graph.layers(model).stream().noneMatch(layer -> layer.overlay()), "Stopped overlay must finish fade-out");
		}
		if (tick == 220)
		{
			check(restartObserved && reverseObserved && first.getTime() > 100, "Graph must survive actual model reload and keep advancing");
			PLibDatabase.LOGGER.info("STAGE 3.3 GRAPH RENDER PASS: owners=2, zero/reverse overlay, RESTART, reload, time={}", first.getTime());
		}
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
		{
			throw new AssertionError(message);
		}
	}
}
