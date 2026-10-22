/**
 * @author ArcAnc
 * Created at: 22.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1;

import com.arcanc.pulselib.content.mixin.GlBufferAccessor;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.gl.PGlRenderHost;
import com.arcanc.pulselib.content.renderer.gl.PGlResourceRegistry;
import com.arcanc.pulselib.content.renderer.plan.PMeshHandle;
import com.arcanc.pulselib.content.renderer.plan.PRenderExecutor;
import com.arcanc.pulselib.content.renderer.plan.PRenderFrame;
import com.arcanc.pulselib.util.PLibDatabase;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL31;
import org.lwjgl.system.MemoryStack;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/** Observes the real world executor and compares captured frame data with the actual projection UBO. */
public final class Stage41PlanProbe
{
	private static int requestedTick;
	private static int passes;
	private static @Nullable PMeshHandle oldMesh;

	private Stage41PlanProbe()
	{
	}

	public static void register()
	{
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Stage41PlanProbe::afterWorld);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Stage41PlanProbe::shutdown);
	}

	public static void tick(int tick)
	{
		if (tick == 100 || tick == 230)
			requestedTick = tick;
	}

	private static void afterWorld(RenderLevelStageEvent.AfterTranslucentFeatures event)
	{
		if (requestedTick == 0)
			return;
		var executor = (PRenderExecutor)field(PRenderQueue.class, null, "EXECUTOR");
		var registry = (PGlResourceRegistry)field(PRenderQueue.class, null, "RESOURCES");
		var resources = (Map<?, ?>)field(PGlResourceRegistry.class, registry, "meshResources");
		check(!resources.isEmpty(), "Canonical handles must resolve actual rendered meshes");
		if (passes == 1)
		{
			try
			{
				registry.mesh(oldMesh);
				throw new AssertionError("Reload must invalidate the old mesh handle");
			}
			catch (IllegalStateException expected)
			{
				check(expected.getMessage().contains("Unknown PulseLib mesh"), "Unexpected resource error");
			}
		}
		oldMesh = (PMeshHandle)resources.keySet().iterator().next();
		Object instances = field(executor.getClass(), executor, "instances");
		Object indirect = field(executor.getClass(), executor, "indirect");
		check((int)field(instances.getClass(), instances, "buffer") > 0, "Actual instance GPU storage must exist");
		check((int)field(indirect.getClass(), indirect, "buffer") > 0, "Actual indirect GPU storage must exist");
		Object geometry = field(executor.getClass(), executor, "geometry");
		check(!((List<?>)field(geometry.getClass(), geometry, "pages")).isEmpty(), "Original geometry arena must have live pages");
		PRenderFrame frame = new PGlRenderHost().captureFrame();
		verifyProjection(frame, RenderSystem.getProjectionMatrixBuffer());
		check(frame.modelView().equals(RenderSystem.getModelViewMatrix(), 0.000001f), "Capture must copy the actual model-view");
		PLibDatabase.LOGGER.info("STAGE 4.1a PLAN BACKEND PASS: tick={}, meshes={}, strategies={}, projection UBO matches CPU capture", requestedTick, resources.size(), executor.capabilities());
		passes++;
		requestedTick = 0;
	}

	private static void verifyProjection(PRenderFrame frame, GpuBufferSlice projection)
	{
		int previous = GL11.glGetInteger(GL31.GL_COPY_READ_BUFFER);
		try (MemoryStack stack = MemoryStack.stackPush())
		{
			var bytes = stack.malloc(16 * Float.BYTES);
			GL15.glBindBuffer(GL31.GL_COPY_READ_BUFFER, ((GlBufferAccessor)projection.buffer()).pulselib$getHandle());
			GL15.glGetBufferSubData(GL31.GL_COPY_READ_BUFFER, projection.offset(), bytes);
			check(frame.projection().equals(new Matrix4f(bytes.asFloatBuffer()), 0.000001f), "Captured projection must match actual GPU UBO contents");
		}
		finally
		{
			GL15.glBindBuffer(GL31.GL_COPY_READ_BUFFER, previous);
		}
	}

	private static void shutdown(GameShuttingDownEvent event)
	{
		check(passes == 2, "Both pre/post reload backend checks must complete before shutdown");
		Object executor = field(PRenderQueue.class, null, "EXECUTOR");
		Object registry = field(PRenderQueue.class, null, "RESOURCES");
		check(((Map<?, ?>)field(registry.getClass(), registry, "meshResources")).isEmpty(), "Normal cleanup must clear handle resources");
		check(field(executor.getClass(), executor, "projectionBuffer") == null, "Normal cleanup must release frame projection storage");
		PLibDatabase.LOGGER.info("STAGE 4.1a PLAN SHUTDOWN PASS: handles=0, frame projection released");
	}

	private static @Nullable Object field(Class<?> type, @Nullable Object owner, String name)
	{
		try
		{
			Field field = type.getDeclaredField(name);
			field.setAccessible(true);
			return field.get(owner);
		}
		catch (ReflectiveOperationException exception)
		{
			throw new AssertionError("Cannot inspect actual plan backend", exception);
		}
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
			throw new AssertionError(message);
	}
}
