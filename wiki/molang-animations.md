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

Each built-in renderer creates one `MolangParser.Context` per animation controller for a render pass and passes that context to every bone mixed by that controller. This applies to entity, block entity, item, and item GUI rendering, including recursive GUI drawing.

The renderer supplies these values by default:

* `query.anim_time` (also available as `q.anim_time`) is the controller's interpolated animation time in seconds.
* `this` is supplied by the animation mixer from the accumulated value of the vector component currently being evaluated.
* The renderer initializes each prepared frame context with `PAnimationManager.key()`. A controller copies only its frame values into its persistent context, so the random functions use the persistent generator described below.

The renderer prepares the frame context outside `PAnimationController`. Before it mixes a pose, the controller copies the current `query.*`, `context.*`, `this`, and query resolver values into its own persistent Molang context. This keeps frame-dependent data current while preserving state owned by the controller.

## Providing custom queries

Override `populateMolangContext` in the renderer for the animatable type. The hook exists in `PEntityRenderer`, `PBlockRenderer`, and `PItemRenderer`.

```java
@Override
protected void populateMolangContext(RobotEntity robot,
                                     PAnimationController<RobotEntity> controller,
                                     MolangParser.Context context,
                                     float partialTick) {
    context.query("ground_speed", (float) robot.getDeltaMovement().horizontalDistance());
    context.query("is_active", robot.isActive() ? 1f : 0f);
}
```

The model can then use `q.ground_speed` and `query.is_active`. Query names are not globally predefined by PulseLib: a renderer exposes only the values appropriate for its animatable.

`MolangParser.Context` also exposes `query(...)`, `variable(...)`, `context(...)`, `thisValue(...)`, and `queryResolver(...)` for custom rendering paths that construct their own context.

## Language surface

The parser supports numeric literals, arithmetic, comparisons, logical operators, ternary expressions, statements, assignments to `temp.*` and `variable.*`, `return`, `loop`, and `break`. It resolves the `query.*`, `variable.*`, `temp.*`, and `context.*` namespaces and their usual short aliases (`q`, `v`, `t`, and `c`).

Supported math functions include trigonometry, interpolation, clamping, rounding, powers, min/max, random and die-roll functions, plus the implemented `math.ease_*` variants. An unsupported function is reported as an error instead of silently evaluating to an arbitrary value.

## `variable.*` lifetime and random state

Each `PAnimationController` owns a persistent `MolangParser.Context`. An assignment to `variable.*` therefore remains available on later frames while that controller remains alive. Separate controllers, including controllers created for different animation-manager keys, have separate variable maps.

The renderer supplies queries afresh for every pass. `temp.*` remains temporary to one expression evaluation, and assignments to it never persist. The persistent context also retains the random-generator stream; the per-pass manager-key seed belongs to the renderer's temporary context and is not copied into that stream. Do not use `math.random` when a repeatable result across controller recreation is required.
