# PulseLib test content

The test mod targets Minecraft 26.1.2. Run from the repository root with JDK 25.

```sh
./gradlew :fabric:runTestClient
./gradlew :neoforge:runTestClient
```

Run one loader at a time. Each test client uses its own `<loader>/runs/testmod` directory. Normal dev launches and release JARs use the production source sets.

`./gradlew check` compiles the test mod for both loaders without starting Minecraft.

Create a creative world with commands enabled.

```mcfunction
/summon pulselib_test:test_entity
/give @s pulselib_test:test_block
```

- The creature plays idle and walk animations. Right-click it to toggle its armor layer. The armor follows its animated body, head and left arm.
- Inspect the block item in the inventory and in hand, then place it. The item plays its own idle animation and the placed block entity loops its block animation.

The four glTF models and their texture files come from the `1.21.1` branch at `e364f60`. The item display transforms come from its `models.zip`. This mod keeps those assets in its own namespace.
