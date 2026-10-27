# Migration regression fixtures

The canonical regression suites and real Gecko/glTF assets are pinned at
`7ce6f024b307bb821ce543344d3896c8e1fcf26b` on `1.21.1-multiloader`.
Port each suite alongside its subsystem as described in
[the migration journal](../docs/porting/26.1.2-multiloader.md).

## Available development runs

The canonical external test mod (`pulselibtest`) has not been migrated yet.
`-PpulseTestMod` does **not** enable it on `26.1.2-multiloader`; Gradle now rejects
the option before launching Minecraft. Fabric runtime integration is planned for
stage 6, and the complete test mod/harness integration for stage 7.1.

The current playable verification fixture is **NeoForge only**, enabled with
`-PpulseStage1Smoke`. It registers the original 26.1 demo content in `pulselib:*`,
not `pulselibtest:*`, and is excluded from release JARs. Run the server first,
then the client in another terminal:

```bash
./gradlew :neoforge:runServer -PpulseStage1Smoke -PpulseVerificationDir=manual-test-runs --no-daemon --console=plain
./gradlew :neoforge:runClient -PpulseStage1Smoke -PpulseVerificationDir=manual-test-runs --no-daemon --console=plain
```

These runs use `neoforge/build/manual-test-runs/{server,client}`. Prepare the
server's `eula.txt` after accepting the Minecraft EULA, and `server.properties`
for a **new disposable world**, loopback `127.0.0.1`, port `25576`, and local
development authentication (`online-mode=false`). The client automatically
connects to that port; the fixture takes a screenshot and stops the client after
300 world ticks. Stop the server with the console command `stop`. See stage 1.2
in [the migration journal](../docs/porting/26.1.2-multiloader.md) for the recorded
environment and additional verification settings.

## Unknown `pulselibtest` registry entries

`Unknown registry key ... pulselibtest:test_block_entity` means a saved world
contains test-mod content whose registration is absent from the current run.
The same problem affects `pulselibtest:test_block` blocks and inventory items.
In the reported Fabric run, the loaded-mod list contained `pulselib` but no
`pulselibtest`; Gradle previously ignored the unimplemented `-PpulseTestMod`
property and used the ordinary `fabric/runs/client` directory.

Keep those saves separate from migration verification. Saving a world after
loading it without the test mod can discard its test blocks, block entities,
and items. To recover lost content, use a backup made before that run with the
matching original Minecraft/loader/test-mod setup. The NeoForge fixture above
does not provide compatibility registrations for `pulselibtest:*` saves.

Client/server regression results must be recorded explicitly; a successful
compilation or Gradle dry run does not establish them.

Stage 4.3a provides explicit local rendering scopes through the canonical
`PRenderContexts` / `legacy.GlRenderState` API. The opt-in NeoForge GPU harness is
`-PpulseStage1Smoke -PpulseStage43aProbe -PpulseStage2Probe`, with an isolated
`-PpulseVerificationDir=stage43a-runs` and a disposable `stage43a-smoke` server.
It checks 44 pixels before/after reload, nested targets/frames, all portable
queue overloads, baked GPU subdivision, world queue/OIT/deformer isolation and
native state restoration. Clients run with a visible window and stop normally.
`python3 docs/porting/tools/verify_render_context_api.py` verifies all 4 canonical
types / 14 public declarations. Automatic native GUI Entity/Item execution and
`PLibHelper.renderModelInGui` remain stage 4.3b; this harness does not claim H15
completion. See journal section 27 for commands, evidence and exact limitations.

Stage 4.3b.1 adds native GUI execution coverage. The opt-in NeoForge harness is
`-PpulseStage1Smoke -PpulseStage43bProbe -PpulseStage2Probe`, with disposable
`-PpulseVerificationDir=stage43b-runs` and a `stage43b-smoke` server (wait for
server Done before connecting). It checks 192 GPU regions across native Entity
PIP, normal Item atlas and oversized Item PIP, opaque/transparent phases, scissor,
independent owners, vanilla text and resource reload. Six real submit/deferred
callback failures verify discarded Pulse plans, retained native pose/collector
recovery and subsequent successful native draws. Both visible clients and the
server stop normally; the existing 4.3a harness also passes 44 regression pixels.
See journal section 28 and `docs/porting/evidence/stage-4.3b.1.json` for commands,
source isolation, screenshots and exact limitations. The canonical
`PLibHelper.renderModelInGui` custom GUI state remains checkpoint 4.3b.2;
standalone item/glint eligibility and the final H15 matrix remain open. These
fixtures do not claim atomic rollback of arbitrary vanilla pixels already drawn
before a mixed-batch failure, driver-loss recovery or Fabric runtime coverage.

Stage 4.3b.2 restores the canonical `PLibHelper.renderModelInGui` using Minecraft
26.1 `GuiGraphicsExtractor`. The opt-in NeoForge harness is
`-PpulseStage1Smoke -PpulseStage43b2Probe -PpulseStage2Probe` with isolated
`-PpulseVerificationDir=stage43b2-runs` and a new `stage43b2-smoke` server.
It checks 208 RGB/text cases in 16 frames before/after resource reload, frozen
animation/GUI pose/scale, rotation/scissor, mirrored scale, independent native
PIP textures, alpha and packed overlay. Two fault suites verify missing-model
behavior, invalid extraction, native draw failure, actual following GPU pixels,
texture close and caller state/lights restoration. CPU readbacks save/reset/
restore GL_PACK_ROW_LENGTH, including the native screenshot stride 960.
The native 4.3b.1 harness also passes 192 regression checks and six failures.
`python3 docs/porting/tools/verify_gui_helper_api.py` checks all nine canonical
helper declarations with explicit Minecraft type substitutions. See journal
section 29 / `docs/porting/evidence/stage-4.3b.2.json` for exact commands and limits.
Next checkpoint is 4.3c (standalone Item/glint/final H15); Fabric GUI registration
and runtime parity remain stage 6. Existing user worlds are never fixtures.

Stage 4.3c completes the current NeoForge H15/item boundary matrix. Standalone
verification uses `-PpulseStage1Smoke -PpulseStage43cProbe -PpulseStage2Probe`,
`-PpulseVerificationDir=stage43c-runs` and a new `stage43c-smoke` server world.
It requires two real ItemEntity owners and an armed vanilla owner, with no Pulse
Entity/BlockEntity states, in Fast/Fancy/Fabulous before/after resource reload.
The probe checks five live native targets, player held-item eligibility, all ten
ItemDisplayContext values in changing world/hand/offscreen invocation targets,
132 GPU checks, direct submit failures/recovery and complete world queue snapshots.

Native glint verification combines `-PpulseStage43cGlintProbe` with
`-PpulseStage43bProbe` and the existing stage1/stage2 flags in isolated
`stage43c-gui-runs`. It adds 32 equal vanilla diamond comparisons with/without
glint beside actual Pulse atlas/PIP draws, checks the extracted special-renderer
foil flag, and retains all 192 native GUI checks and six failure/recovery cases.
Pulse geometry keeps canonical behavior: there is no new visual Pulse glint pass.
The helper/context regression runs retain 208 / 44 checks. See migration journal
section 30 and `docs/porting/evidence/stage-4.3c.json` for exact commands,
accepted logs, source isolation and limitations. Stage 4 is complete within
these NeoForge boundaries; player migration is stage 5, Fabric runtime stage 6,
and the full testmod/compiled API/performance matrix stage 7.

Stage 5.1 restores the canonical player core and camera/hierarchy math. The opt-in
NeoForge probe uses `-PpulseStage1Smoke -PpulseStage51Probe -PpulseStage2Probe`
with isolated `-PpulseVerificationDir=stage51-final-runs` and a new disposable
server world. It checks two actual Player UUIDs times two plugin definitions,
independent managers/controllers/Molang contexts and clocks, sample cache,
owner replacement, seconds at the Molang boundary, weighted ModelPart application
and restoration, idempotent attachment registration and cleanup. Two suites
before/after resource reload and the shutdown assertion must pass. The 16 pinned
PlayerHierarchy/PlayerCamera JUnit tests and two mesh-root regressions run through
`:common:test`; `verify_player_core_api.py` checks 27 types / 203 members.
See journal section 32 and `docs/porting/evidence/stage-5.1.json`. Native player
rig/item/camera/armor/deformer rendering and PlayerMotionHarness remain stage 5.2;
this core probe does not claim that matrix is complete. Fixtures remain excluded
from release JARs, and existing user worlds must never be used for these runs.

Stage 5.2a ports exact player anchor/mesh attachment matrices and native hand
queue submission. Use `-PpulseStage1Smoke -PpulseStage52aProbe -PpulseStage2Probe`
with a new isolated `-PpulseVerificationDir=stage52a-final-runs` server/client
pair; the fixture clears the Pulse held item and supplies a vanilla stick so
GPU hand witnesses refer to player mesh geometry. It runs 996 matrix/physics
cases from the pinned PlayerMotionHarness attachment subset, covering Gecko and
embedded glTF, five blend modes, fractional weights, ROOT policy, transitions,
two definitions, reload, caller restoration and pending-world-queue isolation.
A test-only queue mixin observes actual compiler appends and GPU hand flushes
without cancelling them. Four recovery suites check eight callback exceptions
and edited affine commands. The pinned assets stay unchanged; the logical texture
name regression runs in `PLibParserTextureTest`. `verify_player_attachment_api.py`
checks five types / 32 members, including canonical `buffers()` and the retained
native collector accessor. See journal section 33 / `docs/porting/evidence/stage-5.2a.json`.
This is not the complete M-P2/M-P3 or native player matrix: camera, rig/items,
armor/deformers, Fabulous targets, server extraction and real respawn remain
5.2b. Run the existing 5.1 smoke against its own original server flags, which
preserve its required Pulse held item, in a separate disposable world.

Stage 5.2b.1 adds `-PpulseStage52bProbe` beside the stage1/stage2 flags, in new
`stage52b1-runs` server and `stage52b1-final-runs` client directories. It checks
40 native Camera M-P3 cases (Gecko/glTF, F/G/H weights and VANILLA, reload), actual
camera-owner isolation, and four real AvatarRenderer ROOT suites. The ROOT probe
uses captured native states with unavailable late entity IDs, independent vanilla
placement, nested submits/failures on one renderer/stack, normal/depth restoration
and recovery. Test collectors discard manual draw nodes; ordinary world ROOT
boundaries are observed separately. Native Camera oracles preserve player physics.
ROOT mutation/reset and generic eligibility regressions run in Common JUnit.
See journal section 34 / `docs/porting/evidence/stage-5.2b.1.json`. The existing
5.2a matrix/GPU probe is retained as regression with its own matching server flags.
Deferred rig/item/ModelPart/armor/deformer capture/restoration, full Fabulous player
parity, server extraction and real respawn remain checkpoint 5.2b.2.

Stage **5.2b.2a** adds extracted rig commands and invocation scopes restoring the
shared native PlayerModel after submit and deferred model execution. Its opt-in
probe extends the 5.2b.1 run:

```bash
./gradlew :neoforge:runClient -PpulseStage1Smoke -PpulseStage52bProbe -PpulseStage52b2Probe -PpulseStage2Probe -PpulseVerificationDir=stage52b2a-manual-runs --no-daemon --console=plain
```

Prepare a new disposable server/world as above, use a run directory starting
with `stage52b`, and keep the server console open for normal `stop`. The probe
checks Gecko/glTF before and after reload, frozen rig after controller stop and
missing owner ID, native submit/deferred nested failures, model pose/visibility
restoration and native vertex emission. Stage 5.2a is the separate unchanged
GPU hand regression. ModelPart/arm queues, armor/deformer lifetime, Fabulous
player pixels and the remaining server/respawn witnesses are still open in
**5.2b.2b**; this result does not complete stage 5. See section 35 of the journal.

Stage **5.2b.2b** adds native ModelPart pose snapshots, native hand/layer scopes,
and queued item quad/tint ownership with nested scratch restoration. Extend the
5.2b native player probe with `-PpulseStage52b2bProbe`:

```bash
./gradlew :neoforge:runClient -PpulseStage1Smoke -PpulseStage52bProbe -PpulseStage52b2Probe -PpulseStage52b2bProbe -PpulseStage2Probe -PpulseVerificationDir=stage52b2b-manual-runs --no-daemon --console=plain
```

Use a new disposable world/server as above and keep its console open for normal
`stop`. Four suites cover both format scenes before/after reload, opposite sleeve
flags, native ModelPart main/outline/crumbling and item main/outline/foil calls,
real ItemStackRenderState reuse, and nested hand/part/item/layer failure/recovery.
Recording buffers apply only to direct suites; unchanged 5.2a GPU hand and
4.3b/4.3c GUI/glint probes verify real draws separately. Armor/deformer sampled
values, cube bindings and remaining Fabulous/player server witnesses stay open
in **5.2b.2c**. See section 36 of the migration journal for evidence and limits.

Stage 5.2b.2c.1 extends the existing `-PpulseStage52b2bProbe` native part/item
suite with cube binding replay checks in main/outline/crumbling passes, nested
failure recovery, and real deformed vertex comparisons before/after reload.
Use it with `-PpulseStage1Smoke -PpulseStage52bProbe -PpulseStage52b2Probe`
and a fresh `-PpulseVerificationDir`, as recorded in journal section 37.
`verify_player_deformer_api.py` audits all six canonical player deformer types.
Binding snapshots preserve callbacks; extracted channel values and armor owner
isolation remain checkpoint 5.2b.2c.2. This suite does not establish Fabulous
player/armor pixel parity or Fabric runtime support.

Player occlusion diagnostic: `-PpulseStage1Smoke -PpulsePlayerOcclusionProbe`
on NeoForge requires a new `player-occlusion-smoke` server world and a client
marker `.pulselib-player-occlusion` in a fresh `-PpulseVerificationDir` directory.
It tests 48 world states / 252 RGB-depth comparisons with native players,
opaque/translucent PulseLib entities, both depth orders and transparency modes,
before/after reload. The current baseline **fails 36 behind-player translucent
RGB checks** and intentionally exits nonzero after writing the complete
`player-occlusion-results.json`. This is a reproduced defect, not a passing
migration gate. See journal section 38 and committed raw GPU evidence.

Item API update (2026-10-28): animated items now implement `PAnimatable` directly;
the separate item marker/renderer accessor was removed by explicit user request.
The unchanged `-PpulseStage31Probe` verifies real copy/split/stacking/NBT/network
identity and manager lifecycle with that contract. See [API checkpoint](../docs/porting/item-api-removal.md).
