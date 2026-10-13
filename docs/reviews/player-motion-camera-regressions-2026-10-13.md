# Player Root Motion / camera — M-P2 and M-P3

Base: `4b22a67bcf3e1091de92dc5f8e2c94de9ac48eaf`, branch `1.21.1-multiloader`, Java 21, library version unchanged (`1.2.0`). Phase A started from a clean worktree and is committed as `5c2270513ef39634cae2c580d4647daf87b5c0ce`. The continuation started at that HEAD with existing uncommitted Phase B changes; they were preserved and reviewed. Author contract accepted on 2026-10-13. C1, H14, H17 and L38 production implementations are preserved.

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
| `cameraAnchorModelDelta()` | Point displacement and orientation from normalized bind/current visual camera frames | ROOT included once through the shared visual ROOT/pivot matrix |

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

`cameraAnchorModelDelta()` now describes displacement **at the camera origin**, plus inherited orientation and scale channels. Its signature is unchanged. Its translation intentionally no longer means the translation column of `current * inverse(bind)` at model zero. `actionTransform()` still returns the full model-space socket.

The camera and native player share `visualRootMatrix()`:

```text
V = T(rootDelta.translation) * T(rootPivot) * Q(rootDelta.rotation)
      * S(rootDelta.scale) * T(-rootPivot)
C_visual = V * inverse(C_ROOT) * C_camera
B_visual = inverse(B_ROOT) * B_camera
D = C_visual * inverse(B_visual)
p_bind = B_visual * (0, 0, 0, 1)
displacement = D * p_bind - p_bind
```

`C` and `B` are exact sequential local-bone matrix products. A rootless rig normalizes **both** current and bind camera products using the constant HEAD bind frame (or keeps model space if HEAD is absent). This supplies a common reference basis, rather than subtracting translation columns from different spaces. The displacement is equivalent to `C_visual * origin - B_visual * origin` after that normalization.

Orientation uses the inherited quaternion channels in that same reference frame: `q_current_visual * inverse(q_bind_visual)`. With ROOT, `q_current_visual = q_rootDelta * inverse(q_currentRoot) * q_currentCamera`; bind uses `inverse(q_bindRoot) * q_bindCamera`. Camera orientation stays a rotation even when the position matrix contains shear from nonuniform scales.

For camera-only rotation, the current and bind origins coincide, so displacement is zero. Rotation around ROOT instead changes `C_visual * origin`, retaining the actual arc about the same explicit pivot that the player renderer uses. This also handles nonzero root bind offsets and rotated bind frames.

At the rendering boundary, source glTF coordinates are converted into player axes once, then `PFirstPersonCameraSpace` converts player axes into Minecraft camera axes; both changes are `diag(-1, -1, 1)`, so they cancel for glTF. Gecko uses the latter conversion only. The existing body rotation then takes displacement into world axes and conjugates the animated quaternion before composing it with the vanilla camera rotation.

`PFirstPersonPresentation` keeps its existing shared `inverse(C_camera) * C_bone` frame. Arms, held items, custom anchors and automatic meshes all use that frame. Their matrices are already in VIEW space and do not receive another camera transform. Automatic mesh discovery includes static mesh descendants on an active rig when only the camera bone has a channel.

Camera definition weights, first-person activation, blend modes and crossfade algorithms remain unchanged. `VANILLA` remains guarded in `cameraPose()`; animation anchors still work without changing the real camera. Fabric's roll adapter now reconstructs the same signed quaternion as Minecraft/NeoForge's `Camera.setRotation(yaw, pitch, roll)`; this is necessary to verify animated orientation on both loaders.

## Regression Tests — phase B and continuation

The current camera suite was run with only `cameraAnchorModelDelta()` temporarily restored from Phase A HEAD, with automatic byte-for-byte restoration in `finally`. **7 of 8 tests failed**. The camera-only rotation witness expected `(0, 0, 0)` and observed `(-0.1, 0, 0.5)`; the ROOT-rotation witness expected `(0.1, 0, -0.5)` and observed approximately zero. Baseline log/XML: `/tmp/pulselib-current-mp3-baseline.log`, `/tmp/pulselib-current-mp3-baseline.xml`.

With the fix, **58 JUnit tests pass**: 8 hierarchy, 8 camera, 25 C1 and 17 plugin cases. The camera tests cover F/G/H, intermediate rotated/scaled parents, explicit ROOT pivot with nonzero bind origin, rotated ROOT/camera bind frames, rootless HEAD reference and signed yaw/pitch/roll round-trip. Required `./gradlew build --no-daemon` succeeded; log: `/tmp/pulselib-current-build.log`.

The expanded Phase A game suite passed **953 matrix/physics checks on each loader** before Phase B validation. It additionally samples intermediate crossfade frames, ROOT masks and part/bone weights. Log: `/tmp/pulselib-current-mp2-game.log`. Shutdown remained stalled after PASS and world save; termination was requested and the remaining test JVM required forced termination. This is assertion evidence, not a successful normal game exit.

The combined Phase A/B game run reached both PASS markers on each loader:

| Loader | M-P2 matrix/physics checks | M-P3 actual camera assertions | Process completion |
|---|---:|---:|---|
| Fabric | 1680 | 856 | JVM stalled after PASS/save; forcibly terminated |
| NeoForge | 1682 | 851 | JVM stalled after PASS/save; forcibly terminated |

Log: `/tmp/pulselib-current-motion-all.log`. Both completed game JVMs stalled after saving their disposable worlds and did not complete normal shutdown; they were terminated for cleanup. Both formats run F/G/H with three weights and all five blend modes, plus unchanged VANILLA I, before and after reload. Perspective switches, first-/third-person submissions, policy weights/masks, intermediate transition frames, respawn and the two rendered client observers are included. Counts of transition checks depend on render/tick timing. Position, movement, bounding box and dimensions remain unchanged around client tick/render/camera operations; the integrated-server extraction snapshot is unchanged. A separate bounded Fabric repeat again reached PASS with **1714 matrix/physics checks and 872 camera assertions**, then stalled after world save. Log: `/tmp/pulselib-current-motion-fabric-exit.log`; the 240-second timeout returned **124**, and the remaining game JVM was terminated for cleanup. Normal process completion remains unverified. The camera oracle compares the actual `Camera.setup` result with independent local-matrix/quaternion products in the Minecraft reference basis. During definition transitions it includes the existing first-person activation weight; the earlier oracle incorrectly omitted this factor and failed at scenario 42 on both loaders. No production blending change was made to satisfy that expectation.

## Changed Files

Phase A (`5c22705`) added exact `modelMatrix` / `bindMatrix` and current-root-relative APIs in `PPlayerAnimationFrame`; routed native anchors and third-person automatic meshes through them in `PPlayerAnimations`, `PPlayerAnimatedAttachments` and `PPlayerAutomaticMeshAttachments`; and used exact shared VIEW matrices for first-person extras from the existing, unchanged `PFirstPersonPresentation`. `PRootMotionRuntime` / `PRootMotionDelta` changes are contract documentation only. That commit also added the opt-in harness, Gecko/embedded glTF fixtures, queue probes, hierarchy tests and disposable-run configuration.

Phase B/continuation files:

| File | Change |
|---|---|
| `PPlayerAnimationFrame.java` | Point displacement and quaternion orientation in the native visual reference frame. |
| `PPlayerAnimationSpace.java` | Shared visual ROOT/pivot matrix. |
| `PPlayerAnimations.java` | Uses that shared matrix for native ROOT; converts camera delta from source to player axes once. |
| `PPlayerAutomaticMeshAttachments.java` | Finds static mesh descendants for an active camera-only rig. |
| `PFirstPersonCameraSpace.java`, Fabric `FabricCameraService.java` | Shared Minecraft yaw/pitch/roll reconstruction with NeoForge's signed roll. |
| `PPlayerHierarchyTest.java`, `PPlayerCameraTest.java` | Fixtures for parent chains and rotated bind frames; eight camera regressions. |
| `PlayerMotionHarness.java` | F/G/H/I, transition and policy checks, actual camera state and physics assertions. |
| `PlayerMotionCameraMixin.java`, `pulselib-test.mixins.json` | Test-only observation before animation changes the native camera. |
| Testmod README and stabilization tracker/backlog/report | Reproduction commands, evidence and explicit incomplete criteria. |

## Compatibility

No entity movement, hitbox, eye-height, physics, collision, server synchronization, controller, graph, manager, interpolation, GPU or OIT implementation is changed. No Minecraft launch is added to CI, and no user save is opened. Java 21 source/target, Common/Fabric/NeoForge separation, version/publication configuration and existing author/date headers are preserved. New test classes reuse existing nonnull package-info files. Signatures remain compatible; the intentional camera displacement behavior change is documented above and covered by regressions.

Production and existing regression files listed for C1/H14/H17/L38 in the tracker are unchanged compared with `4b22a67`. C1's 25 JUnit tests passed again. Historical H14/L38 pixel/resource suites retain their prior evidence; they were not rerun in this continuation.

## Completion Criteria

- [x] ROOT and extracted deltas remain visual/data only; no entity/server movement system is added.
- [x] Tested parent hierarchies, anchors and meshes inherit all parents; native ROOT is applied once.
- [x] Animated camera uses the full hierarchy; camera-only rotation has zero false displacement, and ROOT rotation retains the camera arc.
- [x] Shared first-person camera frame keeps tested arm/item/mesh transforms consistent; VANILLA position/rotation is unchanged.
- [x] Key Gecko and supported embedded glTF cases pass numerically and at game submission/camera boundaries.
- [x] Perspective switches, reload, respawn and two simultaneously rendered client observer players are exercised.
- [x] Required build and all 58 JUnit tests pass; C1/H14/H17/L38 implementation files are preserved.
- [ ] Complete successful game-process termination verified on both loaders.
- [ ] Full visual/pixel comparison, real networked multi-client scenario and unrelated subsystem regression suites are verified.

M-P2 and M-P3 remain **IN PROGRESS — implemented, validation incomplete**. A successful build or assertion PASS alone does not close either ID.

## Remaining Issues / Limits

- Normal JVM termination after game PASS is unverified. Shutdown saves all disposable-world dimensions; `jcmd` attach times out and diagnostic SIGQUIT did not yield a Java stack. Root cause is not established. Do not label this an animation regression or fix renderer/driver shutdown as part of M-P2/M-P3.
- These are matrix/submission/lifecycle regressions, not screenshot or pixel equivalence tests of every vanilla arm, item, skin or third-party renderer. Public TRS snapshots and vanilla ModelPart/arm/item command blending cannot represent shear.
- Development launches still log `GL_INVALID_OPERATION: Array object is not active` after world loading, also present in earlier phase logs. Its source is not established; renderer/GL changes are outside this task.
- Real multiplayer with additional connected clients has not been exercised. Client observer entities verify per-player instance and render paths only.
- C1/H14/H17/L38 are preserved; historical H14/L38 pixel/resource suites were not rerun in this continuation.
