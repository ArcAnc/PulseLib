# Installation

PulseLib supports Minecraft 1.21.1 on NeoForge and Fabric with Java 21. Install exactly one loader artifact. Fabric additionally requires Fabric API. Architectury API is not required.

## For developers

```gradle
repositories {
    maven { url = "https://arcanc.github.io/Maven/" }
}
```

NeoForge, with ModDevGradle:

```gradle
dependencies {
    implementation "com.arcanc.pulselib:pulselib-neoforge:1.21.1-1.2.0"
}
```

Fabric, with Loom and official Mojang/Parchment mappings:

```gradle
dependencies {
    modImplementation "com.arcanc.pulselib:pulselib-fabric:1.21.1-1.2.0"
}
```

These coordinates describe the prepared 1.2.0 beta; it has not been published by this update. Use a version actually published to the repository. Refresh Gradle after changing dependencies. `common` is a development module, not an installable mod or Maven dependency published by this build. Mods using the old NeoForge API may need [source adaptation](plugins.md#migrating-from-pulselib-11x).

Declare PulseLib as a required mod dependency in your loader metadata. For Fabric this is the ordinary `depends.pulselib` declaration; [PulseLib plugins](plugins.md) require no extra entrypoint. Ordinary mod initialization for registering Minecraft items/blocks still uses the loader's normal lifecycle.

## For players

Download the artifact matching your loader from [CurseForge](https://www.curseforge.com/minecraft/mc-mods/pulselib) or [Modrinth](https://modrinth.com/mod/pulselib). Both game JARs bundle jgltf and Jackson dependencies.

## Building PulseLib

```sh
./gradlew build --no-daemon
```

This is also the standard CI command: it compiles all three modules, runs JUnit tests and builds both loader JARs. CI uploads the JARs and test reports; it does not start Minecraft or the demo mod.

Client/server runs are manual:

```sh
./gradlew :neoforge:runClient
./gradlew :fabric:runClient
./gradlew :neoforge:runServer
./gradlew :fabric:runServer
```

Game artifacts are `neoforge/build/libs/pulselib-1.21.1-neoforge-1.2.0.jar` and `fabric/build/libs/pulselib-1.21.1-fabric-1.2.0.jar`, alongside source and Javadoc artifacts. Fabric's installable JAR is the remapped artifact without a sources/Javadoc suffix. The [existing demo mod](../tests/testmod/README.md) remains available for opt-in manual runs.

To test Maven output without uploading a release:

```sh
./gradlew :neoforge:publish :fabric:publish -PmavenRepoDir=/tmp/pulselib-maven-stage2
```

Modrinth/CurseForge tasks are `:<loader>:modrinth` and `:<loader>:publishCurseForge`. `publishMods` uploads both loader variants, and `publishAll` also writes Maven artifacts. These are explicit release operations requiring the relevant credentials; building does not invoke them.
