/**
 * @author ArcAnc
 * Created at: 17.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer;

import com.arcanc.pulselib.content.renderer.legacy.GlRenderState;
import com.arcanc.pulselib.content.renderer.plan.PFrameCompiler;
import com.arcanc.pulselib.content.renderer.plan.PRenderFrame;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/** Render-thread scopes. Bind an offscreen target before entering its scope. */
public final class PRenderContexts
{
	private static final ThreadLocal<Deque<Scope>> SCOPES = ThreadLocal.withInitial(ArrayDeque::new);

	private PRenderContexts()
	{
	}

	/**
	 * Render environment used to select queue ownership and submission timing.
	 */
	public enum Context
	{
		/**
		 * World rendering with stage-based queue flushing.
		 */
		WORLD,
		/**
		 * Isolated GUI rendering.
		 */
		GUI,
		/**
		 * Isolated rendering into an offscreen target.
		 */
		OFFSCREEN,
		/**
		 * First-person arm, held-item and attachment rendering.
		 */
		FIRST_PERSON
	}

	/**
	 * Resolves actual invocation scope and target; Entity has no ItemDisplayContext.
	 *
	 * @return the active render environment
	 */
	public static Context current()
	{
		RenderSystem.assertOnRenderThread();
		Scope scope = SCOPES.get().peek();
		int framebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
		if (scope != null && scope.framebuffer == framebuffer)
			return scope.context;
		return framebuffer == Minecraft.getInstance().getMainRenderTarget().frameBufferId ? Context.GUI : Context.OFFSCREEN;
	}

	/**
	 * Enters a render context and returns a scope that restores the previous context.
	 *
	 * @param context the context
	 * @return the scope restoring the previous render environment
	 */
	public static Scope enter(Context context)
	{
		RenderSystem.assertOnRenderThread();
		Scope scope = new Scope(Objects.requireNonNull(context));
		SCOPES.get().push(scope);
		return scope;
	}

	/**
	 * Supports direct calls to PEntityRenderer in addition to dispatcher calls.
	 *
	 * @return the scope isolating entity rendering when required, or {@code null} when the requested value is unavailable
	 */
	public static @Nullable Scope enterEntity()
	{
		Context context = current();
		if (context == Context.WORLD ||
			context == Context.FIRST_PERSON ||
			(localScope() != null && localScope().framebuffer == GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING)))
				return null;
		return enter(context);
	}

	/**
	 * Returns the entity submission stage appropriate for the active render context.
	 *
	 * @return the entity submission stage appropriate for the active render context
	 */
	static PRenderQueue.RenderStage entityStage()
	{
		Scope scope = SCOPES.get().peek();
		return scope != null && scope.context == Context.FIRST_PERSON ?
				PRenderQueue.RenderStage.FIRST_PERSON : PRenderQueue.RenderStage.ENTITIES;
	}

	/**
	 * Returns the active isolated local render scope, if one exists.
	 *
	 * @return the active isolated local render scope, if one exists, or {@code null} when the requested value is unavailable
	 */
	static @Nullable Scope localScope()
	{
		Scope scope = SCOPES.get().peek();
		return scope != null && scope.compiler != null ? scope : null;
	}

	/** Each local scope owns its submissions and never drains the world compiler. */
	public static final class Scope implements AutoCloseable
	{
		private final Context context;
		private final Thread thread = Thread.currentThread();
		private final int framebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
		private final @Nullable GlRenderState state;
		final @Nullable PFrameCompiler<PRenderQueue.RenderStage> compiler;
		private boolean closed;

		private Scope(Context context)
		{
			this.context = context;
			boolean local = context == Context.GUI || context == Context.OFFSCREEN;
			this.state = local ? new GlRenderState() : null;
			this.compiler = local ? new PFrameCompiler<>(instance ->
			{
				Vector3f position = instance.transform().getTranslation(new Vector3f());
				RenderSystem.getModelViewMatrix().transformPosition(position);
				return Math.copySign(1f, RenderSystem.getProjectionMatrix().m22()) * position.z;
			}) : null;
		}

		/** Drops incomplete geometry when a renderer throws; close still restores state. */
		public void discard()
		{
			if (this.compiler != null)
				this.compiler.clear();
		}

		/**
		 * Flushes or discards owned local submissions as configured and restores the preceding render context.
		 */
		@Override
		public void close()
		{
			if (this.closed)
				return;
			if (Thread.currentThread() != this.thread || SCOPES.get().peek() != this)
				throw new IllegalStateException("Render scopes must close on their render thread in reverse order");
			try
			{
				if (this.compiler != null)
				{
					this.state.bindTarget();
					PRenderQueue.flushLocal(this.compiler, new PRenderFrame(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix()), this.state);
				}
			}
			finally
			{
				try
				{
					if (this.state != null)
						this.state.close();
				}
				finally
				{
					SCOPES.get().pop();
					if (SCOPES.get().isEmpty())
					{
						SCOPES.remove();
						if (this.compiler != null)
							PRenderQueue.finishLocalFrame();
					}
					this.closed = true;
				}
			}
		}
	}
}
