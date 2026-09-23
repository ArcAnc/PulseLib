# Mesh deformers

Mesh deformers modify vertices after a mesh has been baked. A stack may contain several ordered operations, such as bend followed by twist. PulseLib derives transformed normals from the deformation Jacobian, so lighting follows the deformed surface.

## GPU execution

For world-rendered PulseLib models, the built-in `bend`, `hinge`, `twist`, `stretch`, `squash`, `taper`, and `wave` operations run in the custom vertex shaders. Mesh geometry remains in the static geometry arena; the backend uploads a compact operation table once per compiled stack and uploads the current channel values for each frame. The shader source is shared by render types through `assets/pulselib/shaders/include/deformers.glsl`; its attributes and buffer samplers are described on [Shaders](shaders.md).

GPU execution is automatic when every operation in a stack is built in and the stack has at most eight operations. A custom `PMeshDeformer` remains CPU-rendered; a future GPU extension point can add a shader descriptor for custom operations. CPU fallback uploads a dynamic mesh buffer for the current deformation. GUI's immediate rendering also remains on the CPU path.

Curved deformers still need enough vertices. PulseLib creates and caches a static subdivided mesh for the requested `subdivisionLevel`; it does not rebuild that mesh each frame.

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
| `pulselib:hinge` | `PHingeDefinition` | Rigidly rotates the positive side of `lengthAxis` around `hingeAxis`; the negative side does not move. `angle` is in radians. |
| `pulselib:twist` | `PTwistDefinition` | Rotates vertices progressively around `lengthAxis`; `angle` is the total twist. |
| `pulselib:stretch` | `PStretchDefinition` | Scales displacement along `axis`; `scale = 1` is neutral. |
| `pulselib:squash` | `PSquashDefinition` | Scales along `axis` and compensates perpendicular axes to preserve local volume. `scale = 1` is neutral. |
| `pulselib:taper` | `PTaperDefinition` | Scales the radial component from `1` at the negative end to `tipScale` at the positive end. |
| `pulselib:wave` | `PWaveDefinition` | Applies a sine displacement inside the interval. It uses `amplitude`, `phase`, and a positive `wavelength`. |

For bend, hinge, and wave, the secondary axis must not be parallel to `lengthAxis`. Hinge's fold boundary is the plane through `origin` perpendicular to `lengthAxis`; points on that plane stay on the stationary side. All interval deformers reject an empty interval; stretch, squash, and taper clamp a non-positive scale to a small positive value.

## Custom model meshes

Use `PMeshDeformation` with a `PMeshRenderContext` to deform a baked triangle mesh. The `cacheKey` must be a stable object for one independently rendered deformation; PulseLib reuses its dynamic vertex buffer and uploads current vertices each frame.

```java
private final Object bendBufferKey = new Object();

PMeshDeformation deformation = new PMeshDeformation(
        KNEE_BEND,
        reference -> reference.name().equals(KNEE_ANGLE.name())
                ? currentAngleRadians()
                : reference.defaultValue(),
        bendBufferKey,
        2);

return baseMeshRenderContext().withDeformation(deformation);
```

`PMeshDeformation` subdivides the source triangle mesh only when necessary. Level `0` uses the original mesh; the default is `2`; valid levels are `0` through `4`. Higher levels make curved bends smoother but increase vertex count exponentially per source triangle. The subdivided static source is cached per baked mesh and level, including on the GPU path.

The shader deformer stream is reset after the translucent world stage. Do not call `PGpuDeformerBuffers.finishFrame()` yourself; the render-stage handler owns this lifecycle. More details are in [Render backend](render-backend.md).

## Player animations

Attach a stack directly to a player animation definition. It is active only while that definition contributes, samples controller time with `partialTick`, and fades every channel back to its default during an activation crossfade.

```java
PPlayerAnimationDefinition.builder(model)
        .bind(PPlayerPart.RIGHT_LEG, "right_leg")
        .deform(PPlayerPart.RIGHT_LEG, KNEE_BEND, (context, reference) -> {
            if (!reference.name().equals(KNEE_ANGLE.name()))
                return reference.defaultValue();
            return kneeCurve(context.controllerSeconds("acrobatics"));
        })
        .build();
```

`PPlayerAnimationDeformerContext` exposes `player()`, `definition()`, `partialTick()`, `weight()`, `isPlaying(controller)`, `controllerTicks(controller)`, and `controllerSeconds(controller)`. `weight()` already includes definition, activation-fade, and part weights. Several `.deform(...)` calls may target one part and run in declaration order. Player deformations apply to outer skin layers and compatible armor model parts.

`PPlayerMeshDeformers.register(id, part, predicate, stack, values)` remains available for a deformation that is independent of an animation definition. Use `frameAt(...)` to obtain a deformed attachment position and an orthonormal `right`/`up`/`forward` frame; its overload with `partialTick` matches interpolated player animation renders.

## Custom deformer types

Implement `PMeshDeformer<D>` for a new operation. `codec()` describes a serializable definition and `prepare(...)` creates one or more efficient `PPreparedDeformer` operations. Register the type from the mod event bus through `PulseLibEvents.TypeRegistrationEvent.registerMeshDeformer(...)`.

```java
public final class MyDeformer implements PMeshDeformer<MyDefinition> {
    public static final MyDeformer INSTANCE = new MyDeformer();

    @Override public ResourceLocation id() { return MY_ID; }
    @Override public MapCodec<MyDefinition> codec() { return MyDefinition.CODEC; }

    @Override
    public void prepare(PDeformerPrepareContext context, MyDefinition definition) {
        context.add((position, values) -> {
            // Modify position in place.
        });
    }
}
```

```java
@SubscribeEvent
public static void registerTypes(PulseLibEvents.TypeRegistrationEvent event) {
    event.registerMeshDeformer(MyDeformer.INSTANCE);
}
```

Do not retain mutable render state inside a prepared operation. Read changing values through `PDeformerValueSource` instead.
