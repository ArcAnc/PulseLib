/**
 * @author ArcAnc
 * Created at: 12.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.porting;

import com.arcanc.pulselib.api.plugin.PulsePlugin;
import com.arcanc.pulselib.content.animatable.PItemStackAnimationData;
import com.arcanc.pulselib.content.animatable.PItemStackAnimationIdentity;
import com.arcanc.pulselib.content.animatable.PItemStackAnimationIds;
import com.arcanc.pulselib.content.model.animation.PInterpolationType;
import com.arcanc.pulselib.content.registration.PRegistry;
import com.arcanc.pulselib.content.renderer.plan.PDrawCommand;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.data.gecko.MolangParser;
import com.arcanc.pulselib.data.gecko.PExpressionEvaluator;
import com.arcanc.pulselib.platform.IPulsePlatform;
import com.arcanc.pulselib.platform.PulseServices;
import com.arcanc.pulselib.plugin.PulsePluginClassScanner;
import com.arcanc.pulselib.plugin.PulsePluginLifecycle;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.helpers.PLibCodecs;
import com.google.gson.JsonArray;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Real source components against Minecraft 26.2; this suite does not claim loader or renderer integration. */
public final class CoreChecks
{
	public static void main(String[] args) throws Exception
	{
		SharedConstants.tryDetectVersion();
		check(SharedConstants.getCurrentVersion().id().equals("26.2"), "Minecraft version");
		Bootstrap.bootStrap();
		passed("Minecraft 26.2 vanilla bootstrap without client/backend classes (no world)");
		check(!PulseServices.PLATFORM.isClient(), "Server platform service");
		check(PulseServices.load(IPulsePlatform.class).getClass() == ServerPlatform.class, "ServiceLoader provider");
		passed("ServiceLoader and common-side platform contract");
		scanPlugins();
		lifecycle();
		registriesAndCodecs();
		molang();
		identity();
		portableGeometry();
		System.out.println("CORE COMPONENT CHECKS PASSED; PulsePluginManager/loader bootstrap and transformed ItemStack remain separate integration checks.");
	}

	private static void scanPlugins() throws Exception
	{
		ClassWriter writer = new ClassWriter(0);
		writer.visit(Opcodes.V25, Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "fixture/ClientPlugin", null,
				"net/minecraft/client/renderer/texture/AbstractTexture", null);
		var annotation = writer.visitAnnotation(PulsePluginClassScanner.ANNOTATION_DESCRIPTOR, true);
		annotation.visitEnum("value", "Lcom/arcanc/pulselib/api/plugin/PulsePlugin$Side;", "CLIENT");
		annotation.visitEnd();
		writer.visitEnd();
		byte[] bytes = writer.toByteArray();
		PulsePluginClassScanner scanner = new PulsePluginClassScanner();
		var candidate = scanner.read("fixture", new ByteArrayInputStream(bytes));
		check(candidate != null && candidate.className().equals("fixture.ClientPlugin"), "ASM class name");
		check(candidate.side() == PulsePlugin.Side.CLIENT && !candidate.side().matches(false), "Side filter metadata");
		var directory = Files.createTempDirectory("pulselib-scan-");
		Files.write(directory.resolve("ClientPlugin.class"), bytes);
		check(scanner.scan("fixture", directory).equals(List.of(candidate)), "Directory discovery");
		var archive = Files.createTempFile("pulselib-scan-", ".jar");
		try (var zip = new ZipOutputStream(Files.newOutputStream(archive)))
		{
			zip.putNextEntry(new ZipEntry("fixture/ClientPlugin.class"));
			zip.write(bytes);
			zip.closeEntry();
		}
		check(scanner.scan("fixture", archive).equals(List.of(candidate)), "Jar discovery");
		Files.delete(directory.resolve("ClientPlugin.class"));
		Files.delete(directory);
		Files.delete(archive);
		passed("Java 25 ASM discovery from stream/directory/JAR without loading a client superclass");
	}

	private static void lifecycle()
	{
		var lifecycle = new PulsePluginLifecycle();
		expect(IllegalStateException.class, () -> lifecycle.begin(PulsePluginLifecycle.Phase.EVENTS));
		for (var phase : PulsePluginLifecycle.Phase.values())
		{
			check(lifecycle.begin(phase), "First lifecycle entry " + phase);
			lifecycle.complete(phase);
		}
		check(!lifecycle.begin(PulsePluginLifecycle.Phase.CHANNELS), "One-time phase");
		check(lifecycle.begin(PulsePluginLifecycle.Phase.MODELS), "Reload phase");
		lifecycle.fail(PulsePluginLifecycle.Phase.MODELS, new IllegalStateException("fixture reload failure"));
		check(lifecycle.begin(PulsePluginLifecycle.Phase.MODELS), "Reload retry");
		lifecycle.complete(PulsePluginLifecycle.Phase.MODELS);
		var failed = new PulsePluginLifecycle();
		failed.begin(PulsePluginLifecycle.Phase.CHANNELS);
		failed.fail(PulsePluginLifecycle.Phase.CHANNELS, new IllegalStateException("fixture registration failure"));
		expect(IllegalStateException.class, () -> failed.begin(PulsePluginLifecycle.Phase.CHANNELS));
		passed("Plugin phase ordering, one-time registration, reload retry and retained failures");
	}

	private static void registriesAndCodecs()
	{
		PRegistry<Object> registry = new PRegistry<>();
		var first = PLibDatabase.rl("first");
		var second = PLibDatabase.rl("second");
		Object value = new Object();
		registry.register(first, value);
		expect(IllegalArgumentException.class, () -> registry.registerAll(Map.of(first, new Object(), second, value)));
		check(registry.get(first).orElseThrow() == value && registry.get(second).isEmpty(), "Atomic duplicate rejection");
		Vector3f vector = new Vector3f(1.25f, -2.5f, 3.75f);
		var encoded = PLibCodecs.VECTOR3F_CODEC.encodeStart(JsonOps.INSTANCE, vector).getOrThrow();
		check(PLibCodecs.VECTOR3F_CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow().equals(vector), "Vector codec round trip");
		JsonArray invalid = new JsonArray();
		invalid.add(1);
		check(PLibCodecs.VECTOR3F_CODEC.parse(JsonOps.INSTANCE, invalid).error().isPresent(), "Vector codec rejects wrong length");
		UUID uuid = UUID.randomUUID();
		var buffer = Unpooled.buffer();
		try
		{
			PLibCodecs.STREAM_CODEC.encode(buffer, uuid);
			check(PLibCodecs.STREAM_CODEC.decode(buffer).equals(uuid), "UUID stream codec");
		}
		finally
		{
			buffer.release();
		}
		passed("Identifier/registry atomicity, DFU vector codec and UUID stream codec");
	}

	private static void molang()
	{
		var context = new MolangParser.Context().query("query.anim_time", 1).variable("variable.counter", 0);
		close(MolangParser.evaluate("math.sin(90)", context), 1, "Molang angle units");
		var expression = MolangParser.parse("query.anim_time * 2");
		close(PExpressionEvaluator.SHARED.evaluate(expression, context, 1), 2, "First context value");
		context.query("query.anim_time", 3);
		close(PExpressionEvaluator.SHARED.evaluate(expression, context, 1), 6, "Same-time changed context is not cached");
		MolangParser.evaluate("variable.counter = variable.counter + 1; return variable.counter;", context);
		close(MolangParser.evaluate("variable.counter", context), 1, "Persistent Molang variables");
		close((float)PInterpolationType.catmullRom(0.5, 0, 0, 1, 0), 0.5625f, "Four-point Catmull-Rom");
		close((float)PInterpolationType.catmullRom(0, 4, 5, 6, 9), 5, "Catmull-Rom start");
		close((float)PInterpolationType.catmullRom(1, 4, 5, 6, 9), 6, "Catmull-Rom end");
		passed("Molang degree math, mutable context, persistent variables and Catmull-Rom anchors");
	}

	private static void identity() throws Exception
	{
		Identity source = new Identity(true);
		Identity copy = new Identity(true);
		PItemStackAnimationIdentity.copy(source, copy);
		long original = source.id;
		check(original > 0 && copy.id == original && source.requests == 1, "Copy identity without manager access");
		PItemStackAnimationIdentity.split(source, copy, false);
		check(source.id == original && copy.id > 0 && copy.id != original, "Partial split identity");
		PItemStackAnimationIdentity.copy(source, copy);
		PItemStackAnimationIdentity.split(source, copy, true);
		check(source.id == 0 && copy.id == original, "Full transfer identity");
		Identity plain = new Identity(false);
		Identity plainCopy = new Identity(false);
		PItemStackAnimationIdentity.copy(plain, plainCopy);
		PItemStackAnimationIdentity.split(plain, plainCopy, false);
		check(plain.id == 0 && plainCopy.id == 0, "Nonanimated identity remains unassigned");
		try (var executor = Executors.newFixedThreadPool(8))
		{
			List<Future<Long>> ids = executor.invokeAll(IntStream.range(0, 1024).
					<java.util.concurrent.Callable<Long>>mapToObj(index -> PItemStackAnimationIds::nextId).toList());
			var unique = new HashSet<Long>();
			for (var id : ids)
				check(id.get() > 0 && unique.add(id.get()), "Concurrent session identity uniqueness");
		}
		passed("Production identity transfer rules and 1024 concurrent allocator calls; native Mixin integration not exercised");
	}

	private static void portableGeometry()
	{
		ByteBuffer vertices = ByteBuffer.allocate(12).putFloat(1).putFloat(2).putFloat(3).flip();
		ByteBuffer indices = ByteBuffer.allocate(2).putShort((short)0).flip();
		var geometry = new PGeometryData(vertices, indices, 12, 1, PGeometryData.IndexType.UNSIGNED_SHORT);
		vertices.putFloat(0, 99);
		check(geometry.vertices().getFloat(0) == 1 && geometry.indexType().bytes() == 2, "Immutable geometry and index width");
		var command = new PDrawCommand(3, 513, 7, -2, 11).withInstances(1, 523);
		check(command.firstIndex() == 7 && command.baseVertex() == -2 && command.firstInstance() == 523, "Portable offsets retained");
		Matrix4f transform = new Matrix4f().translation(1, 2, 3);
		var header = new PInstanceHeader(transform, 0xFFFFFFFF, 0, 0);
		transform.identity();
		check(header.transform().m30() == 1, "Instance transform copied");
		passed("Portable geometry ownership, draw offsets and copied instance transforms (no GPU execution)");
	}

	private static void passed(String name)
	{
		System.out.println("PASS " + name);
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
			throw new AssertionError(message);
	}

	private static void close(float actual, float expected, String message)
	{
		check(Math.abs(actual - expected) < 0.00001f, message + ": " + actual + " != " + expected);
	}

	private static void expect(Class<? extends Throwable> type, Runnable operation)
	{
		try
		{
			operation.run();
		}
		catch (Throwable failure)
		{
			if (type.isInstance(failure))
				return;
			throw new AssertionError("Unexpected failure", failure);
		}
		throw new AssertionError("Expected " + type.getName());
	}

	/** Test service implementation, never packaged with PulseLib. */
	public static final class ServerPlatform implements IPulsePlatform
	{
		@Override
		public boolean isClient()
		{
			return false;
		}

		@Override
		public boolean shouldRiderSit(Entity mount)
		{
			return true;
		}
	}

	private static final class Identity implements PItemStackAnimationData
	{
		private final boolean animated;
		private long id;
		private int requests;

		private Identity(boolean animated)
		{
			this.animated = animated;
		}

		@Override
		public long pulselib$getAnimationId()
		{
			return this.id;
		}

		@Override
		public void pulselib$setAnimationId(long id)
		{
			this.id = id;
		}

		@Override
		public long pulselib$getOrCreateAnimationId()
		{
			this.requests++;
			if (this.animated && this.id == 0)
				this.id = PItemStackAnimationIds.nextId();
			return this.id;
		}
	}
}
