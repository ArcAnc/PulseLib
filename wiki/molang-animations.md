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

The parser supports numeric literals, arithmetic, comparisons, logical operators, ternary expressions, statements, assignments to `temp.*` and `variable.*`, `return`, `loop`, and `break`. It resolves the `query.*`, `variable.*`, `temp.*`, and `context.*` namespaces and their usual short aliases (`q`, `v`, `t`, and `c`).

Supported math functions include trigonometry, interpolation, clamping, rounding, powers, min/max, random and die-roll functions, plus the implemented `math.ease_*` variants. An unsupported function is reported as an error instead of silently evaluating to an arbitrary value.

## `variable.*` lifetime

`variable.*` assignments persist for the lifetime of a `PAnimationController`, including across render frames. They are private to that controller, so two controllers do not overwrite each other's variables. `temp.*` remains local to one expression evaluation.


## Sampling and time boundaries

Animation clip and keyframe timestamps remain in game ticks. `PAnimationEvaluationContext.ticksToSeconds(...)` converts controller time at the Molang boundary. A caller-supplied context already contains seconds and is not converted again.

`PExpressionEvaluator` evaluates every expression against its current context, including expressions classified as `TIME_ONLY`. It does not retain global time buckets or quantize sub-tick time. The existing `clearTimeCache()` method remains for source compatibility and has no cache to clear. Variables, custom query resolvers and random expressions therefore retain their own evaluation order and controller state.

Gecko rotation keys interpolate Euler degrees before conversion to quaternions, preserving complete positive and negative turns, including independent pre/post values. Vector and Euler Catmull-Rom sampling uses four neighboring control points with local segment progress; endpoint neighbors are duplicated. Overshoot is preserved. glTF quaternion tracks keep shortest-path slerp. Existing custom typed channels retain their two-point interpolation through the new default four-point overload.
