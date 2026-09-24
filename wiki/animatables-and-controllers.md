# Animatables and Controllers

An animatable is any object that can provide animation state to a PulseLib renderer. The renderer does not decide whether an entity is walking, whether a block is open, or whether an item should be idling. That decision belongs to controllers registered by the animatable.

Animation state is built around:

* [`PAnimatable`](https://github.com/ArcAnc/PulseLib/blob/master/src/main/java/com/arcanc/pulselib/content/animatable/PAnimatable.java)
* [`PAnimationManager`](https://github.com/ArcAnc/PulseLib/blob/master/src/main/java/com/arcanc/pulselib/content/animatable/PAnimationManager.java)
* [`PAnimationController`](https://github.com/ArcAnc/PulseLib/blob/master/src/main/java/com/arcanc/pulselib/content/animatable/PAnimationController.java)
* [`PRawAnimation`](https://github.com/ArcAnc/PulseLib/blob/master/src/main/java/com/arcanc/pulselib/content/model/animation/PRawAnimation.java)

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

[`PLibHelper.createManager`](https://github.com/ArcAnc/PulseLib/blob/master/src/main/java/com/arcanc/pulselib/util/helpers/PLibHelper.java) chooses instance managers for entities and block entities, and singleton managers for other animatables.

For items, use [`SingletonAnimationManager.getManager`](https://github.com/ArcAnc/PulseLib/blob/master/src/main/java/com/arcanc/pulselib/content/animatable/singleton/SingletonAnimationManager.java) with the key passed by the renderer:

```java
@Override
public PAnimationManager<ExampleItem> getAnimationManager(AnimManagerKey key) {
    return SingletonAnimationManager.getManager(key, this);
}
```

[`AnimManagerKey`](https://github.com/ArcAnc/PulseLib/blob/master/src/main/java/com/arcanc/pulselib/content/animatable/AnimManagerKey.java) can derive keys from `ItemStack`, `Entity`, `BlockEntity`, or an arbitrary object.

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

Built-in interpolation types are defined in [`PInterpolationType`](https://github.com/ArcAnc/PulseLib/blob/master/src/main/java/com/arcanc/pulselib/content/model/animation/PInterpolationType.java): `LINEAR`, `CATMULLROM`, `BEZIER`, `STEP`.

## Controller state handler

A controller owns one active `PRawAnimation` at a time. Think of the state handler as the current animation rule. It is called while ticking, checks the animatable's current state, and returns [`ControllerState`](https://github.com/ArcAnc/PulseLib/blob/master/src/main/java/com/arcanc/pulselib/content/animatable/ControllerState.java).

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

The first non-overlay state is the initial state. A transition with the seven-argument constructor has an `exitTime` in normalized `[0, 1]`; the six-argument constructor has no exit-time gate. When multiple transitions are valid, the greatest `priority` wins. `COMPLETE_CURRENT` prevents interruption, `FROM_CURRENT` crossfades from the current mixed pose, and `RESTART` fades the new target in from the bind pose.

Set graph inputs from gameplay code through the controller:

```java
PAnimationController<ExampleEntity> controller = manager.getControllers().get("movement");
controller.setParameter("speed", entity.getDeltaMovement().horizontalDistance());
controller.setParameter("jump", entity.isInWater());
controller.trigger("attack");
```

`PCondition.parameter(name)` treats a non-zero boolean or numeric parameter as true. Use `PCondition.triggered(name)` for a one-use transition: it consumes the trigger once selected. `trigger(name)` starts a `OneShotOverlay` with the matching trigger name. Use separate trigger names when both are needed, because overlays consume their triggers before transitions are tested.

`BlendSpace2D` takes independent X and Y parameter names (for example speed and direction) and blends its points by inverse distance. Set `synchronizedCycle` to `true` on a looping clip or blend space to preserve its normalized phase when entering that state; controller `cyclePhase` / `syncCycle` also work with graph controllers.
