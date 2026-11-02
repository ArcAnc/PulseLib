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
before/after reload. The diagnostic baseline at commit `4195074` failed
36 behind-player translucent RGB checks and exited nonzero after writing the
complete `player-occlusion-results.json`. Frozen failing evidence remains in
journal section 38. The completed fix in section 39 passes all 252 comparisons;
depth is captured after native feature batches, before translucent terrain.
The fixture uses native sea lanterns for background contrast, retaining every
assertion and tolerance. Replaying old depth ordering on that same scene still
fails exactly 36 checks. H13 (32 cases) and H14 (1152 cases, Fast/Fancy/Fabulous,
water/glass/particles/rain and reload) pass with normal client/server exits.
Fabric runtime parity remains stage 6.

Item API update (2026-10-28): animated items now implement `PAnimatable` directly;
the separate item marker/renderer accessor was removed by explicit user request.
The unchanged `-PpulseStage31Probe` verifies real copy/split/stacking/NBT/network
identity and manager lifecycle with that contract. See [API checkpoint](../docs/porting/item-api-removal.md).

Stage **5.2b.2c.2a** extends the existing `-PpulseStage52b2bProbe` native suite.
Use it with `-PpulseStage1Smoke -PpulseStage52bProbe -PpulseStage52b2Probe`
and a fresh directory starting with `stage52b`. Submitted ModelPart nodes now
freeze every emitted local deformed quad, preserving arbitrary position-based
custom channel queries without retaining their value/operation closures in the
saved binding. Ordinary restoration scopes keep live caller semantics.

Four Gecko/glTF suites cover two owners, real controller stop, operation mutation,
hidden parents/skipDraw, main/outline/crumbling, nested failures and old nodes
replayed after actual resource reload. The unchanged GPU hand and GUI/glint
suites validate real draws separately. Main/first-person/armor extraction is now
covered by **5.2b.2c.2b**, below. Stage 5 and Fabric runtime remain incomplete.
See journal sections 40–41 and their structured evidence.

Stage **5.2b.2c.2b** uses the same `-PpulseStage52b2bProbe` command above.
`Stage52GeometryProbe` checks main extraction before controller stop and invalid
owner id; `Stage52ArmorProbe` adds two owners, four native equipment slots,
modern/registered deformers, one shared custom HumanoidModel for chest/legs,
texture/glint nodes, first-person arms, main/outline/crumbling, nested failures
and states retained through real reload. All geometry uses a strict immediate
canonical oracle. Source values and model selection must never run during
submit/replay. The fixture activates/deactivates temporary definitions with
normal animation ticks.

Recording buffers apply only to isolated feature suites. The native camera
observer wears armor in ordinary world rendering; GL queries count original
RenderType draws identified by their captured armor entry. Native 26.1 armor
can itself be a PlayerModel. The accepted run has 80 armor nodes, four hand
replays and 192 expected exceptions, with real GPU draws in both reload phases.
No recording consumer replaces world rendering. Canonical test mod/Fabric
runtime and full Fabulous armor pixel coverage remain later checkpoints.

Stage **5.2b.3** adds `-PpulseStage52b3Probe` to the native command above.
Use `-PpulseStage1Smoke -PpulseStage52bProbe -PpulseStage52b2Probe
-PpulseStage52b2bProbe -PpulseStage52b3Probe -PpulseStage2Probe` and a fresh
directory beginning with `stage52b`. `Stage52CallbackProbe` covers arbitrary
first/third-person anchor callbacks, platform arms, persistent first-person armor,
legacy anchor/material resolvers, rig/armor extraction and nested exceptions.
It compares child/sibling mesh matrices against an unmodified reference pass
using actual decoded Gecko/glTF geometry through dynamic PModelData lookup.

The accepted matrix contains 348 checks, 120 target checks (80 gate + 40 native chain),
68 expected exceptions and 48 WORLD/GUI/OFFSCREEN queue cases before/after reload.
Native transparency uses extracted OptionsRenderState, so the probe saves and
restores both live options and extracted flags. Offscreen queue cases use a real
separate TextureTarget. Only the probe's unique stage is consumed; world queues
keep their existing submissions. Already submitted commands retain their owner;
the caller remains responsible for discarding an aborted local plan.
These checks establish scope/target/queue behavior. Dedicated Fabulous
player/armor pixel-depth coverage remains checkpoint 5.2b.4. See journal §42.

Stage **5.2b.4a** adds `-PpulseStage52b4aProbe` (implies the original
player occlusion probe). Run `:neoforge:runServer` first and then
`:neoforge:runClient`, both with `-PpulseStage1Smoke -PpulseStage52b4aProbe
-PpulseStage2Probe -PpulseVerificationDir=stage52b4a-runs`.
Use a new disposable world named `player-occlusion-smoke`, loopback port 25576,
and the client marker `.pulselib-player-occlusion` in that run directory.
The probe writes `player-occlusion-results.json` and stops the client normally;
stop the dedicated server through its console.

The real world matrix covers skin, vanilla armor, a custom NeoForge armor model
and its fixed BODY deformation, versus opaque/OIT Pulse entities in both depth
orders. Fast/Fancy/Fabulous are tested before/after reload: 288 states,
1512 comparisons and 324 independent geometry depth witnesses. A test observer
requires actual OIT depth passes in each owner/mode/reload group. Reports retain
raw color/depth and each frame's projection matrix; cross-frame comparisons use
reconstructed view-space distance. A temporary early-depth mutation failed with
nine RGB mismatches; it is excluded from production and test sources.

Animated rig/armor motion, arbitrary attachments, ordinary non-OIT alpha and
water/glass/particle/weather pixel witnesses remain **5.2b.4b**. The full 5.2b.4
checkpoint and stage 5 are still incomplete. See journal §43 and
[evidence](../docs/porting/evidence/stage-5.2b.4a.json).

Stage **5.2b.4b** uses `-PpulseStage52b4bProbe` with the same isolated server/world
and client marker as 5.2b.4a, in a fresh `stage52b4b-runs` directory. Start
`:neoforge:runServer`, then `:neoforge:runClient`, each with
`-PpulseStage1Smoke -PpulseStage52b4bProbe -PpulseStage2Probe
-PpulseVerificationDir=stage52b4b-runs`. The client writes
`player-occlusion-results.json`; both its normal exit and shutdown assertions
must pass. Stop the server through its console.

The matrix samples actual decoded Gecko/glTF keyframes through public seek/pause:
20 variants, native/custom animated armor, bare masked-rig anchor-only players,
and opaque/ordinary-alpha/OIT registered callbacks. It verifies 1440 states,
7560 RGB/depth comparisons, 1080 motion witnesses, 1296 attachment pixel witnesses,
1080 cross-format witnesses and 48 frustum-isolated anchor target witnesses in
Fast/Fancy/Fabulous before/after reload. The ordinary alpha pipeline and custom
armor bake factory are shared with existing fixtures. Release JARs exclude all
new test assets/classes.

This checkpoint fixes dropped animation channels on a glTF mesh-node beneath
another node. A real-parser JUnit covers animation/bind transforms and the
unchanged static mesh path. Final negative controls suppress armor rig at both
consumers or actual callback geometry; both must fail their pixel assertions.
They exist only in temporary compile projections. Legacy living attachment and
water/glass/particle/weather coverage remains **5.2b.4c**; stage 5 is incomplete.
See journal §44 and [evidence](../docs/porting/evidence/stage-5.2b.4b.json).


Stage **5.2b.4c.1** adds `-PpulseStage52b4cProbe` with the same isolated server,
world and `.pulselib-player-occlusion` client marker. Use
`-PpulseStage1Smoke -PpulseStage52b4cProbe -PpulseStage2Probe
-PpulseVerificationDir=stage52b4c1-runs` for both server and client; start the
client after server Done. This flag implies the player occlusion harness.

The native suite covers third-person **legacy global/item living definitions**:
bare/global-opaque/global-alpha/item-opaque/item-alpha, each at 0/10 ticks of a
real decoded Gecko BODY clip. The item definition hides native diamond armor.
Independent controls cover opaque/OIT Pulse entities in front/behind, all three
native presets and resource reload: **720 states / 3780 RGB-depth comparisons**,
**864 green pixel / 540 motion / 120 extraction witnesses**. Real legacy draws
join the existing entity queue; ordinary alpha uses the instanced shaders and
main target, with no OIT promotion. Mouse-look is released during this automatic
suite; raw reports retain actual view/projection matrices and native-target
presence at readback. Existing RGB/distance tolerances remain unchanged.

The original immediate legacy layer fails the green-alpha pixel assertion in a
temporary compile projection, while bare/opaque controls pass. Final normal
classes also pass the original player-depth and native callback/scopes suites.
**Persistent first-person armor, animated water/glass/particles/weather and
additional legacy attachment format/controller coverage remain 5.2b.4c.2**.
Stage 5 remains incomplete. See journal §45 and
[evidence](../docs/porting/evidence/stage-5.2b.4c.1.json).


The ordinary/OIT architecture was corrected after stage 5.2b.4c.1: select
`PTransparencyMode.ORDINARY` or `PTransparencyMode.OIT` in the new queue
submission overloads. Both policies use the **same RenderType and GPU pipeline**;
no extra ordinary render types/pipelines are created. Existing overloads preserve
the historical defaults. The legacy probe checks six policy-binding/packing/
cleanup cases, each actual mesh's explicit ordinary policy, and actual backend
OIT passes: culled-entity controls must never start OIT for ordinary legacy alpha.
Ignoring policy in the executor fails that assertion. Original player-depth and
44-pixel GUI/offscreen regressions pass. See migration journal §46 and
[explicit transparency evidence](../docs/porting/evidence/explicit-transparency-mode.json).
The next migration checkpoint remains **5.2b.4c.2**.


Stage **5.2b.4c.2a** adds `-PpulseFirstPersonArmorProbe`, alongside
`-PpulseStage1Smoke -PpulseStage2Probe`, in fresh `stage52b4c2a-runs` server/client
directories. Server world must be `player-occlusion-smoke`; client marker is
`.pulselib-first-person-armor`. Start the client after server Done.

The native suite uses two real Gecko/glTF owner rigs, 0/10 ticks, both arms,
bare/global/item armor, opaque/ordinary alpha and front/behind controls in
Fast/Fancy/Fabulous before/after reload. **216 states / 9072 comparisons**,
**1728 green / 1944 motion-depth witnesses**; raw coords/projection/model-view/
depth-range and three readback boundaries are retained. Hands must be visible
(`hideGui=false`); held hands are empty and item variants equip a real chestplate.
Persistent native armor joins the existing FIRST_PERSON queue with explicit
ORDINARY policy, after native hand depth; direct GUI callback behavior is retained.
An initial-HEAD layer projection reproduces the missing front-alpha pixel.
No additional render types/pipelines are introduced. See journal §47 and
[evidence](../docs/porting/evidence/stage-5.2b.4c.2a.json).
Animated-owner water/glass/particles/weather remains **5.2b.4c.2b**.


Animated-owner water/glass pixels (**5.2b.4c.2b.1**): use
`-PpulseStage1Smoke -PpulseAnimatedEnvironmentProbe -PpulseStage2Probe` with
`-PpulseVerificationDir=stage52b4c2b1-runs`. Create a fresh flat server world
named `animated-environment-smoke`, loopback port 25576, and place
`.pulselib-animated-environment` in the separate client directory. Run
`:neoforge:runServer`, then `:neoforge:runClient` after server Done.

The real native scene contains stained glass, water inside a glass enclosure,
and a clear control ray. Canonical Gecko/glTF owner clips move native/custom
armor and registered opaque/explicit ordinary/OIT socket geometry. The two
alpha modes use the same existing material render type. No new pipelines are
registered. Eight independent front/behind controls, nine samples and six
mode/reload phases produce **1920 states / 10800 RGB-depth comparisons**,
2160 motion and 2160 format depth witnesses, and 2592 green mesh pixels.
`animated-environment-results.json` preserves the actual matrices, coordinates,
and main/terrain/item target RGBA/depth. It must report PASS and the client must
exit 0 with `ANIMATED ENVIRONMENT SHUTDOWN PASS`; then stop the server normally.
The opt-in probe/scene are excluded from release JARs. Water results include
its glass enclosure. Rig formats are compared using an existing Gecko socket
mesh; arbitrary mesh-controller clips and held-item/map combinations are not
covered. Animated particles/weather remain **5.2b.4c.2b.2**.


Animated native particle/rain pixels (**5.2b.4c.2b.2**): use
`-PpulseStage1Smoke -PpulseAnimatedEffectsProbe -PpulseStage2Probe` with
`-PpulseVerificationDir=stage52b4c2b2-runs`. Create a fresh flat server world
named `animated-effects-smoke`, loopback port 25576; put
`.pulselib-animated-effects` in its separate client directory. Start
`:neoforge:runServer`, then `:neoforge:runClient` after server Done.

The opt-in scene disables mob spawning/day progression, uses peaceful,
clears non-player entities and confines native rain to front/behind columns
with real roofs. Native SOUL particles use the normal provider. The probe
reuses the previous Gecko/glTF rigs, native/custom armor and registered
opaque/explicit ordinary/OIT sockets. Six controls, nine samples and six
mode/reload phases produce **1440 states / 38880 RGB-depth comparisons**,
1080 motion and 1080 format witnesses, and 1296 green pixels. Same-frame
main/native targets and the actual callback transform provide the reference;
behind fragments may be hidden during composition instead of early clipping.
`animated-effects-results.json` must report PASS; client exit 0 and
`ANIMATED EFFECTS SHUTDOWN PASS` are mandatory. Stop the server normally.
Helpers remain outside release JARs. This compares owner rig formats using
an existing static Gecko socket mesh; arbitrary legacy mesh clips and
first-person held-item/map combinations remain outside this checkpoint.
Next: **5.2b.5a — M-P2 server root-extraction isolation**.


### Stage 5.2b.5a: dedicated-server root extraction

Run `:neoforge:runServer -PpulseStage1Smoke -PpulseServerMotionProbe -PpulseStage2Probe -PpulseVerificationDir=stage525a-runs` with an isolated server configuration/EULA. The opt-in probe checks two FakePlayerFactory server owners, two definitions/masks and 16 forward/reverse/zero root-motion extraction cases against unchanged physics. It shuts the server down normally after PASS. Require exit 0 and a classloading log without Minecraft client/Blaze3D classes; Gradle SUCCESS alone does not prove server success. Network player/respawn coverage belongs to 5.2b.5b. See migration journal section 50.


### Stage 5.2b.5b: native respawn and animation lifetime

Run `:neoforge:runServer` and then `:neoforge:runClient` with `-PpulseStage1Smoke -PpulseRespawnProbe -PpulseStage2Probe -PpulseVerificationDir=stage525b-runs`. Use an isolated server world/EULA and client `.pulselib-respawn` marker; wait for server Done at loopback 127.0.0.1:25576. The opt-in command invokes real PlayerList.respawn for the connected player twice around a resource reload. Require two SERVER RESPAWN PASS lines, four client suites (Gecko/glTF), CLIENT PASS, SHUTDOWN PASS and client exit 0; then console `stop` the server and require exit 0. No survival death screen or GPU pixel coverage is claimed. See migration journal section 51.


### Stage 6.1a: Fabric dedicated-server bootstrap

Run `:fabric:runServer -PpulseFabricBootstrapProbe` with a separate EULA and server configuration in `fabric/build/stage61a-runs/server` (loopback 127.0.0.1:25577). The external `pulselib_stage61` source-set mod supplies BOTH/SERVER/CLIENT candidates; require exactly two loaded plugins, exactly-once registration, actual channel presence, BOOTSTRAP PASS, SHUTDOWN PASS and exit 0. The probe shuts the server down normally. Check classloading.log for absence of Minecraft client/Blaze3D/ClientPoisonPlugin classes. It is excluded from release JARs. Client lifecycle/reload/Mixins and GPU parity remain 6.1b/6.2; see migration journal section 52.


### Stage 6.1b.1: Fabric client bootstrap and CPU reload

Run `:fabric:runClient -PpulseFabricClientProbe` with `.pulselib-client-lifecycle` in `fabric/build/stage61b1-runs/client`. Require ordered common/client callbacks once each, two declaration collections, a fresh baked one-bone Gecko model after actual resource reload, six public access-widener targets, CLIENT RELOAD PASS, SHUTDOWN PASS and exit 0. The separate `:fabric:runServer -PpulseFabricClientProbe` boundary run uses its own EULA/properties (loopback 127.0.0.1:25578); console stop after Done, require exit 0 and no client classes loaded. This probe covers title-screen CPU model reload; atlas/mesh/world lifecycle and full render Mixins remain 6.1b.2/6.2. See migration journal section 53.


### Stage 6.1b.2a: Fabric native atlas and textured reload

Run `:fabric:runClient -PpulseFabricAtlasProbe -PpulseVerificationDir=stage61b2a-runs` with `.pulselib-atlas` in its disposable client directory. Require ATLAS PASS / SHUTDOWN PASS and exit 0: 12 Gecko/glTF models, 24 mesh observations, 648 UV comparisons, 48 GPU atlas pixels across startup/reload, metadata overrides/AUTO classification, fresh models/sprites and closed old GPU texture. Native sprite padding and solidify of zero-alpha RGB are accounted for; no Pulse geometry draw is claimed. The server boundary run uses the same properties, its own EULA/config (loopback 127.0.0.1:25579), console stop after Done and exit 0. Fixture remains outside release JARs; remaining world/Mixin work is 6.1b.2b. See migration journal section 54.

### Stage 6.1b.2b: Fabric world lifecycle and full Common Mixins

Run `:fabric:runServer -PpulseFabricWorldProbe -PpulseVerificationDir=stage61b2b-runs`,
then the same `:fabric:runClient` after server Done. Prepare a separate accepted
EULA and properties: fresh flat world `fabric-world-lifecycle`, loopback
127.0.0.1:25580, online-mode=false, gamemode=creative, force-gamemode=true,
difficulty=peaceful. Put `.pulselib-world-lifecycle` in the separate client
directory. The server-only fixture disables mob spawning and enforces creative
on join. Neither fixture is included in release JARs.

Require MIXIN AUDIT PASS (all 46 merged Common origins / 39 native targets),
WORLD PASS, WORLD SHUTDOWN PASS and client exit 0. Two real connections of the
same UUID each cover exact instance/singleton/player tick windows, native
entity removal, native 200-tick maintenance expiry, actual resource reload,
and network disconnect clearing all three caches while keeping definitions.
The probe never ticks or clears production managers itself. The stale-time
seed uses the existing package-private manager seam; no wall-clock TTL or
complete render-consumer parity is claimed. Native channel close precedes UI
teardown. Stop the server with console `stop` and require exit 0 and side-safe
classloading. Removing only ENTITY_UNLOAD subscription in a temporary source
projection must fail `real ClientLevel removal dispatch removes native entity manager`.

The Common hand Mixin accepts exactly one of the vanilla five-argument or
NeoForge six-argument selectors per hand. NeoForge stage 5.2a matrix/GPU hand
and Fabric atlas fixtures are regression checks. See migration journal §55
and its structured evidence. Next checkpoint: **6.2 Fabric render hooks/parity**.

### Stage 6.2a: Fabric native pipelines and world stages

Run `:fabric:runServer -PpulseFabricStagesProbe -PpulseVerificationDir=stage62a-runs`,
then the same `:fabric:runClient` after Done. Use a fresh flat world named
`fabric-world-lifecycle`, loopback 127.0.0.1:25580, separate accepted EULA,
offline authentication and creative/peaceful server properties. The existing
server-only WorldScene enforces creative and disables mob spawning. Put
`.pulselib-world-stages` in the separate client directory.

Require two EPOCH PASS markers, WORLD PASS, SHUTDOWN PASS and client exit 0:
19 native registered pipelines, stageCalls=[2,2,2], 40 main-target RGB/depth
samples / 160 component comparisons before/after actual resource reload.
The probe checks early opaque depth, retained alpha across terrain, three-source
back-to-front ordinary blending and a late END_MAIN submission without alpha
depth writes. It uses the existing translucent material render type with
explicit ORDINARY policy; no custom render types or pipelines. Test Mixin
observers only observe shared operations; the probe never flushes the queue.
`world-stages-results.json` stores expected/actual samples and stage counts.

Removing only the production END_MAIN subscription in a temporary source
projection must fail `native late stage callback must execute once before END_MAIN observer`.
Restore normal inputs and use the existing world lifecycle fixture for regression.
Stop the server with console `stop` and require exit 0 / side-safe classloading.
The lifecycle regression now waits for a completion sentinel on the same client executor
after production disconnect cleanup; all original cache/definition guards remain.
Removing the cleanup subscription still fails those guards.
Fixtures and observer config are excluded from release JARs. See journal §56
and structured evidence. OIT/Fabulous targets and GUI/item/armor/player consumers
remain open; next checkpoint is **6.2b OIT/Fabulous parity**.

### Stage 6.2b: Fabric OIT and native transparency targets

Run `:fabric:runServer -PpulseFabricTransparencyProbe -PpulseVerificationDir=stage62b-runs`,
then the same `:fabric:runClient` after server SCENE PASS. Use a fresh flat world
`fabric-world-transparency`, loopback 127.0.0.1:25580, separate accepted EULA
and creative/peaceful/offline-auth properties. Put `.pulselib-world-transparency`
in the separate client directory. The server fixture loads scene chunks,
builds actual glass/water/lit wall and verifies native block states.

Require all 36 PHASE PASS markers, WORLD PASS, SHUTDOWN PASS and client exit 0:
2 policies x 3 native graphics presets x 3 effects x 2 resource epochs,
4 accepted frames x 8 lanes per phase = 1152 final RGB/depth cases. The same
existing translucent material supports explicit OIT and ORDINARY submissions.
A real extracted marker requests retained targets through PWorldRenderState;
no production gate is overridden. Readbacks use explicit event phases after
production callbacks. The test observer independently checks native postchain
before production resolve and final ordered-alpha/depth afterward, using actual
terrain/item/particle/weather/cloud target pixels. Every particle/rain phase
requires positive coverage; weather fragments require real depth below 1.
`transparency-results.json` saves coverage and 72 same-frame records.

The test-only JVM flag `pulselib.fabricTransparencyNegative=true` skips only
the production resolve call site: require Java 255 and `final phase=0 lane=0`
pixel failure after the native oracle. Restore normal inputs, run the complete
matrix and existing 6.2a stage/ordinary fixture. Then stop server normally and
require exit 0 / side-safe classloading. Fixtures stay out of release JARs.
This covers queue/target composition, not native GUI/item/armor/player render
consumers. Item/cloud targets are live/read; positive item/cloud contribution
is not claimed. See journal §57 and structured evidence. Next: **6.2c GUI/PiP/item**.

### Stage 6.2c.1: Fabric native PiP factory and canonical GUI helper

Run `:fabric:runClient -PpulseFabricGuiProbe -PpulseVerificationDir=stage62c1-runs`
with `.pulselib-gui` in the separate client directory. No world/server connection
is required. The external Screen calls canonical PLibHelper.renderModelInGui;
the registered shared renderer is selected by the actual native Fabric PiP pool.
Require two HELPER PHASE PASS and FAULT PASS markers, GUI COMPLETE, SHUTDOWN PASS,
and exit 0: 192 RGB samples + 16 text regions = 208 regions across actual reload.
Six independent PiP renderer identities must prepare each GUI frame. The matrix
covers animation, scissor/rotation, mirrored/nonuniform scale, copied inputs,
alpha/overlay, pending global-queue isolation and vanilla GUI text.

Fault suites cover unloaded model, NaN extraction, actual native draw exception,
caller state restoration, transparent failed target, following draw recovery,
texture deletion and pixel-pack state. The probe is adapted from the stage
4.3b.2 helper regression; original author/date and Gecko asset credits remain.
`gui-results.json` and screenshots store the actual results. Removing only the
production factory registration in a temporary projection must fail with zero
native renderer identities instead of six, even though direct renderer recovery
still passes. Restore normal inputs and repeat the complete GUI suite.

The separate server boundary uses the same property, its own accepted EULA and
properties (world `fabric-gui-boundary`, loopback port 25581); stop normally after
Done and require exit 0 and absence of client classes in classloading.log.
Fixture code/assets/config are excluded from release JARs. See journal §58
and structured evidence. Native ItemStack identity and item/GUI consumers remain
**6.2c.2**; no world/hand recovery or complete item parity is claimed here.

### Stage 6.2c.2: Fabric native item identity, menu/network and GUI consumers

`-PpulseFabricItemProbe` enables a separate `stage62Item` fixture with a shared registered
animatable item, a dedicated-server menu and client-only special-model/GUI observers.
It is excluded from every release JAR. Use new `fabric/build/stage62c2-runs/{server,client}`
directories, client marker `.pulselib-items`, loopback `127.0.0.1:25582`, and disposable
flat/creative/peaceful world **fabric-item-consumers**. Never use existing saves.

```bash
./gradlew :fabric:runServer -PpulseFabricItemProbe --offline --no-daemon --console=plain
./gradlew :fabric:runClient -PpulseFabricItemProbe --offline --no-daemon --console=plain
./gradlew build stage2Evidence :fabric:compileStage62ItemJava -PpulseFabricItemProbe -I docs/porting/tools/stage2-evidence.gradle --offline --no-daemon --console=plain
```

Require all 16 native identity regressions, `STAGE62C2 SERVER MENU PASS`, 8 GUI phase
markers, `GUI COMPLETE`, `GUI SHUTDOWN PASS`, manager shutdown PASS and exit 0.
The native menu input transfers eight items; decoded full-content packets carry no
runtime IDs. Each resource epoch covers atlas/oversized PiP × opaque/alpha, eight
accepted frames each: **384 regions**, actual named-item native route counters, and
two atlas item/deferred failure/recovery suites. Pixel thresholds remain those of the
migrated 4.3b fixture. Native observers exclude unnamed hotbar items. `item-results.json`
contains actual frame records/counters; stop the server with console `stop` and require
exit 0. An exit code alone is insufficient (an early native network disconnect was
rejected despite exit 0). Full reconnect/drop/pickup/GC/H15/world-item matrix remains
stage 7. For the negative control, remove only the copy injection in a temporary
`ItemStackMixin` projection and require `Copy before first render must assign and
inherit identity`, then restore inputs. Details/evidence: migration journal §59.

### Stage 6.2d.1: Fabric native living/player layers and attachment snapshots

`-PpulseFabricLayersProbe` enables the separate `stage62Layers` external fixture.
Prepare new `fabric/build/stage62d1-runs/{client,server}` directories, client marker
`.pulselib-layers`, disposable flat/creative/peaceful world **fabric-living-layers**,
and loopback `127.0.0.1:25583`. Never use existing saves.

```bash
./gradlew :fabric:runServer -PpulseFabricLayersProbe --offline --no-daemon --console=plain
./gradlew :fabric:runClient -PpulseFabricLayersProbe --offline --no-daemon --console=plain
./gradlew build stage2Evidence :fabric:compileStage62LayersJava -PpulseFabricLayersProbe -I docs/porting/tools/stage2-evidence.gradle --offline --no-daemon --console=plain
```

Require `STAGE62D1 LAYERS PASS` twice, `PHASE PASS` four times, `COMPLETE`,
`SHUTDOWN PASS` and client exit 0; stop the server with console `stop` and require
exit 0. The fixture audits exactly one Common layer per native living renderer,
player/mannequin skin variants, snapshot partial tick / ItemStack copying / clearing /
world target eligibility, and **224 GUI regions** across opaque/alpha and reload.
Real native entity PiPs draw pig/global, armor-stand/item and player/global attachments;
a registered animated-player callback is observed separately. Native body/equipment
are masked only in test render states to isolate attachment pixels. `layers-results.json`
contains counts, peaks, callbacks and audits. Fixture assets/classes stay out of release.

Negative controls use temporary Fabric source projections: remove only the registration
call and require `Exactly one shared living layer ...: 0`; retain registration and omit
only the extraction injection, requiring `Native final extraction captures the eligible
item snapshot`. Restore normal inputs and rerun the full positive suite. An exit code
without markers is insufficient. Canonical armor-provider API, loader armor/arm bridges
and deformation/instancing parity remain **6.2d.2**; see migration journal §60.

Stage 6.2d.2a restores the exact canonical `PArmorModelProvider` and captures its
selected model, animation, slot visibility and frozen deformation during native
extraction. `-PpulseArmorProviderProbe` enables shared `tests/stage62armor` with
thin adapters for **both Fabric and NeoForge**. Each loader checks 96 actual
native entity PiP regions, four equipment slots, one armor stand/two players,
shared custom model poisoning, ordinary armor, resource reload, nested capture,
six callback failures and no deferred provider/deformer queries. NeoForge also
checks humanoid generic extension composition exactly once per eligible slot;
unrelated equipment types retain their native path. The replay negative control
fails immediately before native GPU drawing. This does not claim full arm,
world OIT/depth, first-person or instancing parity; those remain 6.2d.2b.

Use separate `:fabric:runServer` / `:fabric:runClient` and
`:neoforge:runServer` / `:neoforge:runClient` with that property, wait for server
Done, require PHASE PASS x4 / COMPLETE / SHUTDOWN PASS and exit 0 on each client,
then console-stop each server. Runs are confined to each module's
`build/stage62d2a-runs/{client,server}`, disposable flat/creative/peaceful world
`armor-provider-fixture`, client marker `.pulselib-armor-provider`, ports
25584 (Fabric) / 25585 (NeoForge), visible 960x640 window. Prepare these directories
using only your own accepted EULA/options/properties, never existing user saves.
For isolated validation of the dirty workspace, use the saved HEAD Java
projection described in journal section 61. Full build plus `stage2Evidence`,
both `compileStage62ArmorJava` tasks and the pinned inventory verifier pass;
241 JUnit tests are rerun. Production JARs exclude this fixture. See section 61
and `docs/porting/evidence/stage-6.2d.2a.json` for results and exact limits.

Stage 6.2d.2b validates native arm/deformation and portable GPU instancing parity
on both loaders, using `-PpulseDeformationParityProbe` / `stage62Deformation`.
`tests/stage62deformation` adapts the established native vertex oracle: two
owners, main/four-slot armor/foil/main-outline-crumbling, both hands with sleeves
on/off, actual ItemInHandRenderer call sites, frozen queries, poison and nested
failure recovery before/after resource reload. Controlled CPU feature tests use
recording consumers; normal rendering retains native buffers. Actual deferred
hand vertices then pass through the existing Pulse compiler/PGl executor as six
transform/color instances, shared/copy/32-bit/dynamic sources and four draw
groups. Real GPU primitives, vertex/index/matrix readback and 192 total RGB
regions pass, including independent native-replay and indirect-count negative
controls. Production classes and all three release JARs remain byte-identical.

Use separate `:fabric:runServer` / `:fabric:runClient` and
`:neoforge:runServer` / `:neoforge:runClient` with that property. Prepare only
own disposable `build/stage62d2b-runs/{client,server}` directories per module,
flat/creative/peaceful world `deformation-fixture`, client marker
`.pulselib-deformation`, ports 25586 / 25587, visible 960x640 window. Wait for
server Done, require NATIVE PASS x2 / EPOCH PASS x2 / COMPLETE / SHUTDOWN PASS and
client exit 0, then console-stop the server. Use only your own accepted EULA,
options and properties; never existing user saves. Full build, fresh 241 JUnit,
stage2Evidence, inventory, class/JAR fingerprints and fixture exclusion pass.
See journal section 62 and `docs/porting/evidence/stage-6.2d.2b.json` for exact
bounds, source projection and artifacts. Complete strategy/GPU-deformer modes,
first-person presentation/motion and final regression/API/performance matrix
remain stage 7; the next checkpoint is external testmod/packaging 7.1.
