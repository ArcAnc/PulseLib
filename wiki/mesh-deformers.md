# Mesh deformers

Mesh deformers modify vertices after a mesh has been baked. A stack may contain several ordered operations, such as bend followed by twist. PulseLib derives transformed normals from the deformation Jacobian, so lighting follows the deformed surface.

Built-in operations are registered as `pulselib:bend`, `pulselib:hinge`, `pulselib:twist`, `pulselib:stretch`, `pulselib:squash`, `pulselib:taper`, and `pulselib:wave`.

## Build a stack

Definitions use local mesh coordinates. Axes are normalized during preparation; `positiveExtent` and `negativeExtent` define the interval along a length axis, measured from `origin`. Angles are radians.

```java
private static final PChannelReference<Float> KNEE_ANGLE =
		new PChannelReference<>("knee_angle", 0.0f);

private static final PDeformerStack KNEE_BEND = PDeformerStack.compile(List.of(
		new PDeformerInstance<>(PBendDeformer.INSTANCE, new PBendDefinition(
				new Vector3f(0.0f, 6.0f, 0.0f), // origin
				new Vector3f(0.0f, 1.0f, 0.0f), // length axis
				new Vector3f(1.0f, 0.0f, 0.0f), // bend axis
				6.0f, 0.0f,                    // positive and negative extents
				KNEE_ANGLE))));
```

`PChannelReference` names a runtime float input and supplies its neutral fallback. Use `PChannelReference.constant(value)` for a fixed value. Stacks are immutable after `compile(...)`; use `PDeformerStack.compose(first, second)` to make a longer stack.

## Built-in operations

| Id | Definition | Effect |
| --- | --- | --- |
| `pulselib:bend` | `PBendDefinition` | Bends an interval around an arbitrary perpendicular axis. `angle` is the total bend angle. |
| `pulselib:hinge` | `PHingeDefinition` | Rotates the mesh around `hingeAxis` at `origin`; use it for limb-like joints. |
| `pulselib:twist` | `PTwistDefinition` | Rotates vertices progressively around `lengthAxis`; `angle` is the total twist. |
| `pulselib:stretch` | `PStretchDefinition` | Scales displacement along `axis`; `scale = 1` is neutral. |
| `pulselib:squash` | `PSquashDefinition` | Scales along `axis` and compensates perpendicular axes to preserve local volume. `scale = 1` is neutral. |
| `pulselib:taper` | `PTaperDefinition` | Scales the radial component from `1` at the negative end to `tipScale` at the positive end. |
| `pulselib:wave` | `PWaveDefinition` | Applies a sine displacement inside the interval. It uses `amplitude`, `phase`, and a positive `wavelength`. |

For bend and wave, the secondary axis must not be parallel to `lengthAxis`. All interval deformers reject an empty interval; stretch, squash, and taper clamp a non-positive scale to a small positive value.

## Custom model meshes

Use `PMeshDeformation` with a `PMeshRenderContext` to deform a baked triangle mesh. Supported built-in stacks compile into GPU operations, with current channel values captured for the instance. Stacks that cannot be represented by that shader contract, including custom prepared operations, use CPU-deformed dynamic geometry through the same queue/backend. CPU deformation updates vertices and normals before execution.

```java
private final Object bendKey = new Object();

PMeshDeformation deformation = new PMeshDeformation(
		KNEE_BEND,
		reference -> reference.name().equals(KNEE_ANGLE.name())
				? currentAngleRadians()
				: reference.defaultValue(),
		bendKey,
		2);

return baseMeshRenderContext().withDeformation(deformation);
```

`PMeshDeformation` subdivides the source triangle mesh only when necessary. Level `0` uses the original mesh; the default is `2`; valid levels are `0` through `4`. Higher levels make curved bends smoother but increase vertex count exponentially per source triangle. The subdivided GPU mesh is created lazily and cached per baked mesh and level.

`cacheKey` supplies identity for reusable CPU-deformed geometry. Keep a stable key per independently deformed render context; a new key every frame creates new cache entries, while sharing a mutable source across queued draws can overwrite earlier geometry. GPU subdivision is cached by baked mesh and subdivision level instead.

## Player animations

Attach a stack directly to a player animation definition. It is active only while that definition contributes, samples controller time with `partialTick`, and fades every channel back to its default during an activation crossfade.

```java
PPlayerAnimationDefinition.builder(model).
		bind(PPlayerPart.RIGHT_LEG, "right_leg").
		deform(PPlayerPart.RIGHT_LEG, KNEE_BEND, (context, reference) ->
		{
			if (!reference.name().equals(KNEE_ANGLE.name()))
				return reference.defaultValue();
			return kneeCurve(context.controllerSeconds("acrobatics"));
		}).
		build();
```

`PPlayerAnimationDeformerContext` exposes `player()`, `definition()`, `partialTick()`, `weight()`, `isPlaying(controller)`, `controllerTicks(controller)`, and `controllerSeconds(controller)`. `weight()` already includes definition, activation-fade, and part weights. Several `.deform(...)` calls may target one part and run in declaration order. Player deformations apply to outer skin layers and compatible armor model parts.

`PPlayerMeshDeformers.register(id, part, predicate, stack, values)` remains available for a deformation that is independent of an animation definition. Use `frameAt(...)` to obtain a deformed attachment position and an orthonormal `right`/`up`/`forward` frame; its overload with `partialTick` matches interpolated player animation renders.

## Custom deformer types

Implement `PMeshDeformer<D>` for a new operation. `codec()` describes a serializable definition and `prepare(...)` creates one or more efficient `PPreparedDeformer` operations. Register the type through `IPulseCommonPlugin.registerDeformers(PTypeRegistrar)` using `registrar.registerMeshDeformer(...)`. Choose a side-safe shared/server plugin when the type is needed on the dedicated server; keep client-only types in a client plugin.

```java
public final class MyDeformer implements PMeshDeformer<MyDefinition>
{
	@Override
	public Identifier id()
	{
		return MY_ID;
	}

	@Override
	public MapCodec<MyDefinition> codec()
	{
		return MyDefinition.CODEC;
	}

	@Override
	public void prepare(PDeformerPrepareContext context, MyDefinition definition)
	{
		context.add((position, values) ->
		{
			// Modify position in place.
		});
	}
}
```

Do not retain mutable render state inside a prepared operation. Read changing values through `PDeformerValueSource`. A custom Java operation does not automatically become a new GLSL opcode; it uses the CPU route unless the GPU compiler supports its definition. The built-in shader limit is eight supported operations per submitted mesh.
