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
