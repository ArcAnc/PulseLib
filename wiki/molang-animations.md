# Molang animations

PulseLib evaluates Molang expressions in Gecko animation vectors. A numeric vector component may therefore be written either as a JSON number or as a Molang string:

```json
{
  "bones": {
    "head": {
      "rotation": ["math.sin(query.anim_time * 180) * 15", 0, 0],
      "position": [0, "query.vertical_offset", 0]
    }
  }
}
```

The Gecko parser compiles each string into a `MolangParser.Expression` while the model is loaded. It does not parse the source again every tick or for every bone. The compiled expression is evaluated when the current animation pose is mixed.

## Context and evaluation

Each built-in renderer prepares one frame context per animation controller and render pass. The controller copies its frame-specific query and context values into its own persistent `MolangParser.Context` before it mixes the pose. This applies to entity, block entity, item, and item GUI rendering, including recursive GUI drawing.

The renderer supplies these values by default:

* `query.anim_time` (also available as `q.anim_time`) is the controller's interpolated animation time in seconds.
* `this` is supplied by the animation mixer from the accumulated value of the vector component currently being evaluated.
* `math.random`, `math.random_integer`, `math.die_roll`, and `math.die_roll_integer` use the controller's persistent random state. If deterministic results are required for custom code, seed it through `controller.persistentMolangContext().randomSeed(...)`.

This split keeps render-specific queries fresh without sharing Molang variables or random sequences between different controllers.

## Providing custom queries

Override `populateMolangContext` in the renderer for the animatable type. The hook exists in `PEntityRenderer`, `PBlockRenderer`, and `PItemRenderer`.

```java
@Override
protected void populateMolangContext(RobotEntity robot,
		PAnimationController<RobotEntity> controller,
		MolangParser.Context context,
		float partialTick)
{
	context.query("ground_speed", (float) robot.getDeltaMovement().horizontalDistance());
	context.query("is_active", robot.isActive() ? 1f : 0f);
}
```

The model can then use `q.ground_speed` and `query.is_active`. Query names are not globally predefined by PulseLib: a renderer exposes only the values appropriate for its animatable.

`MolangParser.Context` also exposes `query(...)`, `variable(...)`, `context(...)`, `thisValue(...)`, and `queryResolver(...)` for custom rendering paths that construct their own context.

## Language surface

The parser supports numeric literals, arithmetic, comparisons, logical operators, ternary expressions, statements, assignments to `temp.*` and `variable.*`, `return`, `loop`, `break`, and `continue`. It resolves the `query.*`, `variable.*`, `temp.*`, and `context.*` namespaces and their usual short aliases (`q`, `v`, `t`, and `c`).

Supported math functions include trigonometry, interpolation, clamping, rounding, powers, min/max, random and die-roll functions, plus the implemented `math.ease_*` variants. An unsupported function is reported as an error instead of silently evaluating to an arbitrary value.

## `variable.*` lifetime

`variable.*` assignments persist for the lifetime of a `PAnimationController`, including across render frames. They are private to that controller, so two controllers do not overwrite each other's variables. `temp.*` remains local to one expression evaluation.

## Time and sampling contract

Controller time, clip lengths and keyframe timestamps remain in game ticks.
`PAnimationEvaluationContext.ticksToSeconds` converts the already interpolated
controller time to seconds once when filling `query.anim_time`, including player
animations and Animation Graph contexts. Supplied custom contexts are evaluated
as provided and are not divided again.

Expression dependencies describe what a program reads; `TIME_ONLY` does not
imply that it is independent of its context or custom `queryResolver`. Evaluation
results are no longer cached globally or quantized. Parsed constant programs
still fold their value when loaded. Variables, random state and resolver side
effects stay with the actual controller context.

Gecko rotations retain their original Euler XYZ degrees through interpolation
and convert to `Quaternionf` afterwards. Authored full turns and their direction
are preserved; glTF quaternion tracks and pose blending keep their existing SLERP.

`catmullrom` samples four control points with uniform point weights, using local
segment alpha even when timestamps are uneven. It does not use time-aware tangent
weights. Missing outer points duplicate the nearest endpoint. For segment i→i+1,
controls are `(i-1).post`, `i.post`, `(i+1).pre`, `(i+2).pre`; exact key times
select the outgoing `post`. Deliberate pre/post jumps remain jumps. Duplicate
timestamps select the last key at that time. Position, Scale and Gecko Euler
rotation support this spline; existing custom two-point channels retain their
interpolation API and can override the optional four-point overload.
