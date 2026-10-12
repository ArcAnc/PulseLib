# PulseLib external integration fixture

This is an opt-in mod excluded from release artifacts. `CommonTestPlugin` and `ClientTestPlugin` are found through `@PulsePlugin` only. Loader initializers register two Minecraft items; they never register PulseLib plugins and add no PulseLib-specific entrypoint.

## Development directories

```sh
./gradlew :neoforge:runServer -PpulseTestMod -PpulseSmokeAuto
./gradlew :fabric:runServer -PpulseTestMod -PpulseSmokeAuto
./gradlew :neoforge:runClient -PpulseTestMod -PpulseSmokeAuto
./gradlew :fabric:runClient -PpulseTestMod -PpulseSmokeAuto
```

`pulseSmokeAuto` enables assertions and exits after a pass. Servers test common registration and side filtering. Clients test both parsers, animation, actual instanced/GPU-deformed/OIT GL pixels and two reloads. Assertion failures are explicit in game logs; always check the PASS marker because a loader can catch a server failure while Gradle still returns success.

## Installed JAR discovery

Build the fixture first, then launch in a separate Gradle invocation so Loom resolves the new file contents:

```sh
./gradlew :neoforge:testmodJar :fabric:testmodJar
./gradlew :neoforge:runServer -PpulseTestJar -PpulseSmokeAuto
./gradlew :fabric:runServer -PpulseTestJar -PpulseSmokeAuto
./gradlew :neoforge:runClient -PpulseTestJar -PpulseSmokeAuto
./gradlew :fabric:runClient -PpulseTestJar -PpulseSmokeAuto
```

For a production Fabric installation, build `:fabric:remapTestmodJar` and install `pulselib-testmod-intermediary-<version>-fabric.jar` together with the installable PulseLib remapped JAR and Fabric API. The regular Fabric `testmodJar` is in named development mappings. NeoForge's `testmodJar` needs no remap.

## World and loader adapters

Create a dedicated smoke server world first, or create a disposable client world named `pulselib-smoke`. Copy only that test world to the chosen client's `saves/pulselib-smoke` directory. Do not use an important save: the fixture equips the player with its item/armor and a vanilla helmet.

```sh
./gradlew :neoforge:runClient -PpulseTestMod -PpulseSmokeAuto -PpulseSmokeWorld
./gradlew :fabric:runClient -PpulseTestMod -PpulseSmokeAuto -PpulseSmokeWorld
```

The world test switches to third-person, plays a player animation, confirms item/armor/living/animated attachment callbacks, reloads twice, disconnects and verifies deletion of geometry GPU buffer IDs and empty animation manager collections. In-world item adapter counters cover held items. The test harness's one-time introspection is confined to test classes, never the library's runtime handlers.

Pass markers are `PULSE TEST SERVER PASS`, `PULSE TEST WORLD PASS` and `PULSE TEST CLIENT PASS`. Source assets include an embedded-buffer glTF triangle, a Gecko cube/clip and a texture. Renderer/model factory implementations reside in client helper classes so loading the item classes on dedicated servers is safe.
