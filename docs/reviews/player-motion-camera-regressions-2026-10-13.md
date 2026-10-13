# Player Root Motion / camera — M-P2 and M-P3

Base: `4b22a67bcf3e1091de92dc5f8e2c94de9ac48eaf`, branch `1.21.1-multiloader`, Java 21, library version unchanged (`1.2.0`). Initial worktree was clean. Author contract accepted on 2026-10-13. C1, H14, H17 and L38 production implementations are preserved.

## Root Motion Contract

ROOT changes visual model matrices only. `PRootMotionRuntime` / `PRootMotionDelta` extract animation data. Entity movement, deltaMovement, collisions, bounding boxes, server movement and synchronization remain the consuming mod's responsibility. No server root-motion system is added.

## Coordinate Spaces

Let `C_b` / `B_b` be the exact current/bind hierarchy products and `R` the bound ROOT.

| API / consumer | Space / composition | External ROOT |
|---|---|---|
| `canonicalModelDelta(b)` | TRS animation channels between `C_R^-1 C_b` and `B_R^-1 B_b`; ROOT itself is a model-space channel delta | Excluded for children; vanilla player stack adds visual ROOT |
| `rootRelativeTransform(b)` | `B_R^-1 C_b`; rootless rigs use HEAD bind as before | Included; caller must not add it again |
| `currentRootRelativeTransform` / `currentRootRelativeMatrix` | `C_R^-1 C_b`; rootless rigs retain HEAD-bind normalization | Excluded; intended for the already animated native stack |
| `actionTransform(anchor)` | Full source MODEL-space socket, including ROOT and every parent | Included; public semantic contract retained |
| Third-person native anchors | Current-root-relative matrix, conjugated into player axes for glTF | Added once by `LivingEntityRendererMixin` after `setupAnim` |
| Third-person automatic meshes | Same relative matrix; output basis changes but imported vertices stay in their source basis | Same native stack |
| First-person presentation | `C_camera^-1 C_b`, shared by bones, anchors, arms, items and meshes | Included once in both model matrices and removed once by the camera frame |
| First-person extras | Already VIEW-space matrices | No additional ROOT/camera application |

`PTransform` remains a three-channel TRS record. A product of rotated nonuniform scales can contain shear; exact render matrices retain it, while public TRS channel snapshots retain their existing representational limit. `modelMatrix`, `bindMatrix` and the current-root-relative matrix API expose the exact products. The channel blend algorithms are not replaced.

## M-P2

The native player stack already contains weighted visual ROOT, including the existing explicit `rootPivot`, masks and part/bone weights. Applying full model anchors or bind-relative mesh transforms to that stack repeated ROOT. Native consumers now use an explicit current-root-relative matrix. Public model-space sockets and the bind-relative API keep their spaces.

The player frame computes exact hierarchy matrices from the already evaluated LOCAL_BONE channels. This fixes intermediate-parent position errors under rotated nonuniform scales without changing Gecko/glTF interpolation, controller evaluation or the global `PModelPose` channel cache. Automatic discovery traverses bound skeleton bones and includes mesh children animated through their parents, even when the mesh root has no channel.

First-person automatic meshes and custom anchors use exact camera-relative frame matrices. A caller's changes to command TRS transforms are retained as a correction over the exact sampled matrix. Native ModelPart/arm/item command blending continues to use its existing TRS representation.

## Regression Tests — phase A

Before production changes, five controlled JUnit witnesses failed against HEAD: ROOT translation, ROOT rotation plus all intermediate parents, rotated nonuniform scale, shared first-person camera frame, and two fractional roots. The corrected fixture marks evaluated pose channels dirty; the baseline was rerun with all five failures and the original production files restored in a `finally` block.

After the fix, **8 hierarchy JUnit tests pass**, including nonzero bind translation, explicit pivot, exact affine render matrices and caller command overrides. Existing **25 C1 tests** and **17 plugin tests** also pass (50 total at this phase).

The opt-in testmod loads a Gecko/Bedrock rig and a supported glTF rig with embedded buffer/image, indexed rigid geometry and Blockbench group/mesh separation. No skin, external-buffer or interpolation support is added.

| Loader | In-game assertions | Evidence | Process completion |
|---|---|---|---|
| Fabric | 244 cases, PASS | `/tmp/pulselib-motion-fabric-mp2-expanded.log`; third callbacks 7532, first 1573 | JVM stalled after PASS and world save; normal successful exit remains unverified |
| NeoForge | 244 cases, PASS | `/tmp/pulselib-motion-neoforge-mp2.log`; third callbacks 7586, first 1611 | JVM stalled after PASS and world save; forcibly ended for subsequent work |

Each loader checks A–D at weights 0.25 / 0.6 / 1 for all five blend modes, before and after one resource reload. E additionally runs two simultaneously active definitions with different weights and independent expected visual-root products. The camera-relative item/mesh matrices, custom anchor PoseStack and actual entity/first-person mesh queue submission matrices are compared with an independent sequential JOML product. Perspective switches, real integrated-server respawn and two client-only observer actors reaching actual world player rendering are observed.

Client snapshots bracket the actual animation tick, root application, anchor dispatch, first-person commands, both mesh submissions and root-motion extraction. Position, deltaMovement, bounding box, width and height are unchanged for the local player and both observers. An integrated-server snapshot separately brackets extraction, before the test intentionally invokes respawn. The observers are client fixtures, not additional networked clients; real multiplayer synchronization is not claimed.

## M-P3

Phase B pending. Camera assertions must reproduce the nonzero-bind-offset rotation witness before changing production camera behavior.

## Remaining Issues / Limits

- Normal JVM termination after game PASS is unverified. Shutdown saves all disposable-world dimensions; `jcmd` attach times out and diagnostic SIGQUIT did not yield a Java stack. Root cause is not established. Do not label this an animation regression or fix renderer/driver shutdown as part of M-P2/M-P3.
- These are matrix/submission/lifecycle regressions, not screenshot or pixel equivalence tests of every vanilla arm, item, skin or third-party renderer. Public TRS snapshots and vanilla ModelPart/arm/item command blending cannot represent shear.
- Real multiplayer with additional connected clients has not been exercised. Client observer entities verify per-player instance and render paths only.
- C1/H14/H17/L38 are preserved; the historical H14/L38 pixel/resource suites have not yet been rerun in this task.
