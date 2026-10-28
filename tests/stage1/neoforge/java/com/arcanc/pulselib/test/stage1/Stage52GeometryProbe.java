/**
 * @author ArcAnc
 * Created at: 28.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1;

import com.arcanc.pulselib.content.model.animation.PRawAnimation;
import com.arcanc.pulselib.content.model.deformer.PChannelReference;
import com.arcanc.pulselib.content.model.deformer.PDeformerInstance;
import com.arcanc.pulselib.content.model.deformer.PDeformerStack;
import com.arcanc.pulselib.content.model.deformer.PStretchDeformer;
import com.arcanc.pulselib.content.model.deformer.PStretchDefinition;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import com.arcanc.pulselib.content.player.animation.PPlayerPart;
import com.arcanc.pulselib.content.player.deformer.PPlayerMeshDeformers;
import com.arcanc.pulselib.content.renderer.base.PModelPartSnapshot;
import com.arcanc.pulselib.util.PLibDatabase;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/** Native extraction-to-submit lifetime; the oracle is the canonical immediate deformation API. */
public final class Stage52GeometryProbe
{
	private static int suites;

	private Stage52GeometryProbe()
	{
	}

	public static void verify(Identifier definition, String clip)
	{
		var mc = Minecraft.getInstance();
		var renderer = mc.getEntityRenderDispatcher().getPlayerRenderer(mc.player);
		var model = renderer.getModel();
		var id = PLibDatabase.rl("stage52c2b_geometry");
		var stack = PDeformerStack.compile(List.of(new PDeformerInstance<>(PStretchDeformer.INSTANCE,
				new PStretchDefinition(new Vector3f(), new Vector3f(0, 1, 0), PChannelReference.constant(1)))));
		int[] reads = {0};
		boolean[] alive = {true};
		PPlayerMeshDeformers.register(id, PPlayerPart.BODY, player -> player == mc.player, stack, (player, reference) ->
		{
			check(alive[0], "A submitted model must not read a live value source");
			reads[0]++;
			return PPlayerAnimations.getHandle(player, definition).isPlaying("motion") ? 2 : 9;
		});
		try (var caller = PModelPartSnapshot.capture(model.root()))
		{
			var state = renderer.createRenderState(mc.player, 0.65f);
			int extractionReads = reads[0];
			model.setupAnim(state);
			PPlayerMeshDeformers.apply(mc.player, model, 0.65f);
			List<Vector3f> expected = geometry(model, new PoseStack());
			caller.restore();
			PPlayerAnimations.getHandle(mc.player, definition).stopAll();
			alive[0] = false;
			state.id = Integer.MAX_VALUE;
			int beforeSubmit = reads[0];
			var node = new SubmitNodeStorage.ModelSubmit<>(new PoseStack().last().copy(), model, state, 15728880, 0, -1, null, 0, null);
			List<Vector3f> actual = new ArrayList<>();
			draw(new ModelFeatureRenderer(), node, consumer(actual));
			check(expected.equals(actual), "Main model keeps extracted deformed geometry after owner stop and invalid id");
			check(extractionReads > 0 && reads[0] == beforeSubmit, "Sampling occurs in extraction, never in submit/replay");
			PLibDatabase.LOGGER.info("STAGE 5.2b.2c.2b GEOMETRY CASE PASS: suite={}, extractionReads={}, replayReads={}, vertices={}", suites, extractionReads, reads[0] - beforeSubmit, actual.size());
			suites++;
		}
		finally
		{
			PPlayerMeshDeformers.unregister(id);
			check(PPlayerAnimations.getHandle(mc.player, definition).play("motion", PRawAnimation.begin().thenLoop(clip).build()), "Restore geometry fixture controller");
		}
	}

	public static void completed()
	{
		check(suites == 4, "Both formats before/after reload");
		PLibDatabase.LOGGER.info("STAGE 5.2b.2c.2b GEOMETRY PASS: suites={}", suites);
	}

	private static List<Vector3f> geometry(Model<?> model, PoseStack poses)
	{
		List<Vector3f> result = new ArrayList<>();
		model.renderToBuffer(poses, consumer(result), 15728880, 0, -1);
		return result;
	}

	private static VertexConsumer consumer(List<Vector3f> result)
	{
		return (VertexConsumer)Proxy.newProxyInstance(VertexConsumer.class.getClassLoader(), new Class<?>[] {VertexConsumer.class}, (proxy, method, args) ->
		{
			if (method.getName().equals("addVertex"))
				result.add(new Vector3f((float)args[0], (float)args[1], (float)args[2]));
			return method.getReturnType() == VertexConsumer.class ? proxy : null;
		});
	}

	private static void draw(ModelFeatureRenderer feature, SubmitNodeStorage.ModelSubmit<?> node, VertexConsumer buffer)
	{
		try
		{
			var method = java.util.Arrays.stream(ModelFeatureRenderer.class.getDeclaredMethods()).filter(candidate -> candidate.getName().equals("renderModel")).findFirst().orElseThrow();
			method.setAccessible(true);
			method.invoke(feature, node, RenderTypes.entitySolid(PLibDatabase.rl("textures/stage42a_white.png")), buffer, null, null);
		}
		catch (InvocationTargetException exception)
		{
			if (exception.getCause() instanceof Error error)
				throw error;
			if (exception.getCause() instanceof RuntimeException runtime)
				throw runtime;
			throw new AssertionError(exception.getCause());
		}
		catch (ReflectiveOperationException exception)
		{
			throw new AssertionError(exception);
		}
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
			throw new AssertionError("Stage 5.2b.2c.2b: " + message);
	}
}
