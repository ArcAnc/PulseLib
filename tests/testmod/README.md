# PulseLib cross-platform demo and smoke tests

This opt-in external mod is built from the original NeoForge examples: TestBlock, TestBlockEntity/Renderer, TestEntity/Render, TestBlockItem/Renderer, TestArmor, TestTailItem, PTestArmor, TestDayTimeColor, PPlayerBallDemo and PPlayerAcrobaticDemo. Their implementations live once in src/main/java; small Fabric/NeoForge adapters handle vanilla registries, renderer factories, attributes and keys. No demo classes/assets are included in release PulseLib JARs.

These game/demo checks are run manually. Standard CI only runs `./gradlew build --no-daemon` for the library and JUnit tests; it does not build or launch this mod, accept Minecraft's EULA or create worlds.

Original GLB/glTF/Gecko assets were unpacked from the library's resource ZIPs without changing their geometry or animation. The acrobatic skin texture was recovered from the existing Blockbench player template. Small vanilla item/blockstate descriptors make the previously disabled game objects usable on 1.21.1. The synthetic triangle/cube/items of stage 1 were removed.

CommonTestPlugin exercises all three type registrars using existing built-in implementations. ClientTestPlugin contributes models, armor/tail attachments, both player demos and an observation callback on the ball's animated hand anchor through @PulsePlugin alone. No PulseLib entrypoints or event registrations are used. Loader initializers only register Minecraft content and ordinary renderer/key/tick callbacks.

## Run in development

```sh
./gradlew :neoforge:runServer -PpulseTestMod -PpulseSmokeAuto
./gradlew :fabric:runServer -PpulseTestMod -PpulseSmokeAuto
./gradlew :neoforge:runClient -PpulseTestMod -PpulseSmokeAuto
./gradlew :fabric:runClient -PpulseTestMod -PpulseSmokeAuto
```

Accept Minecraft's EULA in each disposable server run directory. Server assertions check one construction, channels/events/deformers registered once, one common plugin and no CLIENT class initialization. Clients load original models, sample the animation-only Gecko player rig, draw eight GPU-deformed instances of the original item pyramid, verify solid/OIT pixels, compare indexed blend/depth/scissor state, reload three times and verify actual GPU buffer deletion.

## Discover an installed JAR

Build first, then launch in a separate Gradle invocation so Loom resolves fresh file contents:

```sh
./gradlew :neoforge:testmodJar :fabric:testmodJar
./gradlew :neoforge:runServer -PpulseTestJar -PpulseSmokeAuto
./gradlew :fabric:runServer -PpulseTestJar -PpulseSmokeAuto
./gradlew :neoforge:runClient -PpulseTestJar -PpulseSmokeAuto
./gradlew :fabric:runClient -PpulseTestJar -PpulseSmokeAuto
```

Production Fabric uses :fabric:remapTestmodJar (pulselib-testmod-intermediary-1.2.0-fabric.jar), the remapped PulseLib JAR and Fabric API. Its regular testmodJar uses named development mappings. NeoForge's fixture JAR needs no remap. The demo mod version in loader metadata is an internal fixture version independent of library publication.

## In-world checks and manual examples

Create disposable saves named pulselib-smoke and pulselib-smoke-second in the client run directory. Use only test saves: automatic mode equips demo items and spawns/places the original demo entity/block. Never point it at an important save.

```sh
./gradlew :neoforge:runClient -PpulseTestJar -PpulseSmokeAuto -PpulseSmokeWorld -PpulseSmokeWorldSwitch
./gradlew :fabric:runClient -PpulseTestJar -PpulseSmokeAuto -PpulseSmokeWorld -PpulseSmokeWorldSwitch
```

Both clients exercise the block/entity/item renderers, armor adapter and hiding, living/tail attachments, ball-toss player animation and animated hand callback. Probe Mixins verify block-entity → particles → weather hook order. Three resource reloads per world retain plugin identity and one-time registrations, replace models/sprites and preserve rendering. Normal disconnect deletes captured geometry buffer IDs and empties instance/singleton/player animation managers before entering the second world.

Add -PpulseGeckoDemo to select the existing Gecko entity/armor resources instead of their glTF variants. The item remains glTF, so both parsers still run. Equivalent direct-launch VM properties are pulselib.test.auto, pulselib.test.world, pulselib.test.worldSwitch and pulselib.test.geckoEntity.

Without pulseSmokeAuto, the mod is a manual demo. Give pulselibtest:test_block, test_hat, test_chest, test_leggings or test_tail; summon pulselibtest:test_entity. Right-click the block to toggle its random animation and the entity to toggle PTestArmor. V plays the original ball toss; B plays acrobatics with knee deformation. Tail controllers are cleared when client worlds change.

PASS markers: PULSE TEST SERVER PASS, PULSE TEST WORLD PASS, PULSE TEST WORLD CLEANUP PASS, PULSE TEST CLIENT PASS. Always require the marker as well as a successful process exit: loaders can catch a server exception while Gradle exits successfully. Diagnostic reflection is confined to the smoke harness; library runtime handlers do no scanning/reflection.
