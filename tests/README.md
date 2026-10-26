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
