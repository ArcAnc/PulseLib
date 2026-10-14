# Animatables and Controllers

An animatable is any object that can provide animation state to a PulseLib renderer. The renderer does not decide whether an entity is walking, whether a block is open, or whether an item should be idling. That decision belongs to controllers registered by the animatable.

Animation state is built around:

* [`PAnimatable`](https://github.com/ArcAnc/PulseLib/blob/1.21.1-multiloader/common/src/main/java/com/arcanc/pulselib/content/animatable/PAnimatable.java)
* [`PAnimationManager`](https://github.com/ArcAnc/PulseLib/blob/1.21.1-multiloader/common/src/main/java/com/arcanc/pulselib/content/animatable/PAnimationManager.java)
* [`PAnimationController`](https://github.com/ArcAnc/PulseLib/blob/1.21.1-multiloader/common/src/main/java/com/arcanc/pulselib/content/animatable/PAnimationController.java)
* [`PRawAnimation`](https://github.com/ArcAnc/PulseLib/blob/1.21.1-multiloader/common/src/main/java/com/arcanc/pulselib/content/model/animation/PRawAnimation.java)

## PAnimatable

An animatable has two responsibilities. It returns a manager for the current object or stack, and it registers controller factories. The factories matter because items and other singleton-style objects may need separate controller instances for separate keys.

```java
public class ExampleEntity extends PathfinderMob implements PAnimatable<ExampleEntity> {
    private final PAnimationManager<ExampleEntity> manager = PLibHelper.createManager(this);

    @Override
    public PAnimationManager<ExampleEntity> getAnimationManager(AnimManagerKey key) {
        return this.manager;
    }

    @Override
    public void registerAnimationControllers(PAnimationManager.PAnimationRegistrar<ExampleEntity> registrar) {
        registrar.add("movement", () -> state -> {
            ExampleEntity entity = state.animatable();
            state.controller().play(entity.walkAnimation.isMoving() ? WALK : IDLE);
            return ControllerState.PLAY;
        });
    }
}
```

## Manager choice

Entities and block entities are real world instances, so they can usually keep one manager field. Items are different: there is one `Item` object for many `ItemStack`s, so PulseLib uses keys to separate stack animation state.

[`PLibHelper.createManager`](https://github.com/ArcAnc/PulseLib/blob/1.21.1-multiloader/common/src/main/java/com/arcanc/pulselib/util/helpers/PLibHelper.java) chooses instance managers for entities and block entities, and singleton managers for other animatables.

For items, use [`SingletonAnimationManager.getManager`](https://github.com/ArcAnc/PulseLib/blob/1.21.1-multiloader/common/src/main/java/com/arcanc/pulselib/content/animatable/singleton/SingletonAnimationManager.java) with the key passed by the renderer:

```java
@Override
public PAnimationManager<ExampleItem> getAnimationManager(AnimManagerKey key) {
    return SingletonAnimationManager.getManager(key, this);
}
```

[`AnimManagerKey`](https://github.com/ArcAnc/PulseLib/blob/1.21.1-multiloader/common/src/main/java/com/arcanc/pulselib/content/animatable/AnimManagerKey.java) can derive keys from `ItemStack`, `Entity`, `BlockEntity`, or an arbitrary object.

## PRawAnimation

`PRawAnimation` is not the animation data itself. The real keyframes come from the model file. `PRawAnimation` is the playback recipe: play this model animation once, wait a bit, then loop another model animation.

Stage names must match animation names inside the loaded model.

```java
private static final PRawAnimation SEQUENCE = PRawAnimation.begin()
        .thenPlay("deploy")
        .thenWait(10)
        .thenLoop("idle")
        .build();
```

Stage helpers:

* `thenPlay(name)` uses `PAnimationType.PLAY_ONCE`.
* `thenLoop(name)` uses `PAnimationType.CYCLE`.
* `thenHold(name)` uses `PAnimationType.HOLD_LAST_FRAME` and pauses on the last frame.
* `thenWait(ticks)` inserts a wait stage.
* `withSpeed(speed)` changes the last stage speed.
* `withInterpolation(type)` changes the last stage interpolation.

### Weighted random stages

`thenRandom(...)` chooses one clip from a weighted pool, plays it once, then advances to the next stage.
`thenRandomLoop(...)` chooses and plays a new clip every time the previous selected clip ends. A selected
clip is always played as `PAnimationType.PLAY_ONCE`; this is separate from `thenLoop(...)`, which cycles one
clip forever.

```java
private static final PRawAnimation IDLE = PRawAnimation.begin()
        .thenRandomLoop(random -> random
                .add("idle_breath", 6)
                .add("idle_look_left", 2)
                .add("idle_look_right", 2)
                .add("idle_scratch", 1)
                .preventImmediateRepeat())
        .build();
```

The random selection is runtime state owned by each controller playback. `preventImmediateRepeat()` removes the
previously selected animation from the next draw when the pool contains more than one entry.

Example with speed and interpolation:

```java
private static final PRawAnimation FAST_OPEN = PRawAnimation.begin()
        .thenPlay("open")
        .withSpeed(2.0f)
        .withInterpolation(PInterpolationType.BEZIER)
        .thenHold("open")
        .build();
```

Built-in interpolation types are defined in [`PInterpolationType`](https://github.com/ArcAnc/PulseLib/blob/1.21.1-multiloader/common/src/main/java/com/arcanc/pulselib/content/model/animation/PInterpolationType.java): `LINEAR`, `CATMULLROM`, `BEZIER`, `STEP`.

## Controller state handler

A controller owns one active `PRawAnimation` at a time. Think of the state handler as the current animation rule. It is called while ticking, checks the animatable's current state, and returns [`ControllerState`](https://github.com/ArcAnc/PulseLib/blob/1.21.1-multiloader/common/src/main/java/com/arcanc/pulselib/content/animatable/ControllerState.java).

```java
registrar.add("attack", () -> state -> {
    if (!state.animatable().swinging) {
        state.controller().stop();
        return ControllerState.STOP;
    }

    state.controller().play(ATTACK);
    return ControllerState.PLAY;
});
```

Useful controller methods:

* `play(PRawAnimation animation)`
* `play(PAnimationGraph graph)`
* `pause()`
* `resume()`
* `stop()`
* `getState()`
* `isPlaying()`
* `isPaused()`
* `isStopped()`
* `getTime()`
* `getCurrentStage()`
* `cyclePhase(model)` / `syncCycle(model, phase)` for looping animation synchronization

Multiple controllers are mixed on the same bones in renderer order. Register broad base pose controllers first and specific overrides later. A common pattern is one movement controller for idle/walk/run and another controller for attacks or short actions.

For expressions embedded in Gecko animation vectors, see [Molang animations](molang-animations.md). The renderer supplies frame-specific Molang queries, while each controller keeps its own persistent Molang context for variables and random state.

## Animation graphs

`PAnimationGraph` is a controller-driven state machine. Register it with `addGraph`; the resulting named controller owns the graph runtime and its parameters.

```java
private static final PAnimationGraph MOVEMENT = new PAnimationGraph(
        List.of(
                new PAnimationState.BlendSpace1D("locomotion", "speed", List.of(
                        new PAnimationState.BlendSpace1D.Point(0.0f, "idle"),
                        new PAnimationState.BlendSpace1D.Point(0.1f, "walk"),
                        new PAnimationState.BlendSpace1D.Point(0.3f, "run"))),
                new PAnimationState.Clip("jump", "jump", PAnimationType.PLAY_ONCE,
                        PInterpolationType.LINEAR, 1.0f, false),
                new PAnimationState.OneShotOverlay("attack", "attack", "attack")),
        List.of(
                new PAnimationTransition(0, 1, PCondition.parameter("jump"), 0.08f, 10,
                        PInterruptionPolicy.FROM_CURRENT),
                new PAnimationTransition(1, 0, PCondition.ALWAYS, 1.0f, 0.1f, 0,
                        PInterruptionPolicy.FROM_CURRENT)));

@Override
public void registerAnimationControllers(PAnimationManager.PAnimationRegistrar<ExampleEntity> registrar) {
    registrar.addGraph("movement", MOVEMENT);
}
```

The first non-overlay state is the initial state. A transition with the seven-argument constructor has an `exitTime` in normalized `[0, 1]`; the six-argument constructor has no exit-time gate. When multiple transitions are valid, the greatest `priority` wins. `COMPLETE_CURRENT` prevents interruption, `FROM_CURRENT` retains weighted live animation sources, and `RESTART` captures the currently computed base pose and blends that fixed snapshot into the new target. Independent overlays remain active above the base transition.

Set graph inputs from gameplay code through the controller:

```java
PAnimationController<ExampleEntity> controller = manager.getControllers().get("movement");
controller.setParameter("speed", entity.getDeltaMovement().horizontalDistance());
controller.setParameter("jump", entity.isInWater());
controller.trigger("attack");
```

`PCondition.parameter(name)` treats a non-zero boolean or numeric parameter as true. Use `PCondition.triggered(name)` for a one-use transition: it consumes the trigger once selected. `trigger(name)` starts a `OneShotOverlay` with the matching trigger name. Use separate trigger names when both are needed, because overlays consume their triggers before transitions are tested.

`BlendSpace2D` takes independent X and Y parameter names (for example speed and direction) and blends its points by inverse distance. Set `synchronizedCycle` to `true` on a looping clip or blend space to preserve its normalized phase when entering that state; controller `cyclePhase` / `syncCycle` also work with graph controllers.


### Runtime overlay control

`OneShotOverlay` is an immutable definition. Each controller's `graphRuntime()` owns at most one active instance per graph state index. The activation order remains deterministic; repeating a trigger restarts that instance without losing its runtime speed. Every definition sharing a trigger is activated/restarted before the trigger is consumed. A queued retrigger also retains the instance when that tick reaches the clip end or finishes cancellation.

```java
PAnimationGraphRuntime runtime = controller.graphRuntime();
float speed = runtime.overlaySpeed("statue_wake");
runtime.setOverlaySpeed("statue_wake", 0.0f); // pause at the current clip time
runtime.setOverlaySpeed("statue_wake", 1.0f); // resume from that same time
runtime.restartOverlay("statue_wake");      // reset to the direction's start, keep runtime speed
runtime.stopOverlay("statue_wake");         // cancel using fadeOutDuration
```

These four methods also accept a graph state index (`int`) instead of a state name. Names refer to overlay states, not triggers or clip names. Unknown, non-overlay and ambiguous names/indices throw `IllegalArgumentException`; use an index for duplicate state names. Reading or setting speed on an inactive definition throws `IllegalStateException`. Restart/stop return `false` for an inactive definition. A fresh activation uses the definition speed; `reset()` releases all active overlays and their runtime settings.

Overlay speeds must be finite. Positive plays forwards; negative plays backwards. A fresh negative-speed activation starts at the clip end and completes at zero. Zero stops the clip clock and event windows, retains its sampled frame indefinitely, and resumes without seeking when speed becomes nonzero. Pause retains the last nonzero direction; trigger/restart uses that direction's entry boundary (beginning for forwards, end for reverse). An initially zero-speed definition starts at the beginning. Fade-in and fade-out use elapsed ticks independently of clip speed. Cancellation freezes the clip immediately and fades from its current weight; zero fade-out removes it immediately. Repeated cancellation does not reset the fade. Trigger/restart cancels an existing fade-out and resets fade-in. Automatic fade-out uses distance to the endpoint in the current playback direction divided by absolute speed; a pause does not stop an already started fade clock, but only nonzero-speed playback reaching its directional endpoint automatically releases the instance. Explicit cancellation always releases it after fade-out, including at zero speed.

The bound model must contain the overlay clip before activation. Missing resources fail immediately with both the graph state and clip name; no invisible instance is created. Overlay control does not require rebuilding the graph or controller.


For full reverse playback of an active overlay:

```java
runtime.setOverlaySpeed("statue_wake", -1.0f);
runtime.restartOverlay("statue_wake"); // start at the clip end
```

Without `restartOverlay`, changing the sign reverses from the current cursor. At the opposite boundary, use restart explicitly to replay the full clip; changing speed alone never seeks. `NaN` and either Infinity are rejected. Other graph state types retain their existing speed contracts.

If direction changes during automatic fade-out, the runtime cancels that endpoint's fade and smoothly restores weight from its current value over `fadeOutDuration`, then applies the new endpoint's automatic fade. Recovery uses wall time and continues during pause. Changing speed/direction does not cancel a manual stop; trigger/restart does. Reverse event windows use the existing one-shot reverse rules: descending timestamps, departure boundary excluded, arrival boundary included. The end marker is excluded when starting from the end; the zero marker is included when reaching zero. Traversing an event again after a direction change fires it again, while pause/resume does not repeat the pivot event.


### RESTART pose snapshots

At a graph tick the runtime advances playback first, then handles triggers/transitions. RESTART captures the current **base** at that tick boundary (`partialTick = 1`), using current clip times and blend weights. It samples again at the request; it does not reuse a previous rendered pose. At zero transition weight all subsequent render samples start from that snapshot. Target time and transition weight interpolate between ticks. Translation and scale interpolate linearly; rotation uses shortest-path quaternion slerp. Missing channels retain their masks and fade against the reference; explicitly animated identity channels stay present. Scalar channels and visibility are copied too. The snapshot does not advance or generate events.

The controller supplies its persistent Molang state and the accumulated pose of preceding controllers at capture time. For queries normally populated by a custom renderer, configure `controller.setGraphSnapshotContexts((candidate, partialTick) -> freshContext)` using the same game-query population at the supplied tick boundary (`partialTick = 1`). The provider is called when RESTART is processed, so game code can read current state instead of retaining a previous render frame. It also supplies frame queries for preceding controllers while each retains its own persistent variables. Without a provider, previously supplied custom frame queries remain in the controller context; playback times and `anim_time` are still sampled at the current tick. `tickPrecise(model, delta, snapshotSampler)` is an additive overload for direct runtimes with a custom procedural context. Supply a fresh base-only pose through `sampleBasePose(model, 1.0f, context, accumulatedPose)`; exclude this runtime's overlays. The returned pose is copied independently. Game-specific render queries must be supplied from current game state when using this overload.

`samplePose(model, partialTick, context, accumulatedPose)` returns the complete local animation delta, including independent overlays once. `sampleBasePose(...)` omits overlays. `layers(model)` retains its original record/list signature and lists live clips at the current tick; `layers(model, partialTick)` interpolates them. A frozen pose is not a clip and is not listed as an animation layer. Custom consumers that resolve graphs from clip layers must use `samplePose` to include snapshots. The built-in runtime, resolver, visibility and active-bone paths already do so.

A positive-duration RESTART replaces all old base sources with one private deep copy. Repeated RESTART captures the current result, releases the previous source and keeps storage bounded. Completion and `reset()` release it. Zero blend duration switches immediately without retaining a snapshot. FROM_CURRENT continues to advance its live sources, including a live target carried forward from an interrupted snapshot transition. COMPLETE_CURRENT keeps its existing gate.

For manually mixed poses, the original `new PPoseMixer.Transition(initialLayer)` constructor still works with the initial layer's reference pose, or identity if none is supplied. Use `new PPoseMixer.Transition(mixer, referencePose, initialLayer)` when the actual reference or parent hierarchy differs, especially for additive mesh-space layers. RESTART resolves all current layer weights, masks, priorities and blend modes with that mixer/reference at the request, then freezes the computed result. The target remains live. `PPoseMixer.blend(reference, source, target, alpha)` is the shared resolved-pose interpolation. Returned snapshot layers are copies, so mutating them cannot mutate the stored source.
