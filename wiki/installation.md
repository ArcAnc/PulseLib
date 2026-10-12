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
    implementation "com.arcanc.pulselib:pulselib-neoforge:1.21.1-1.1.5.3"
}
```

Fabric, with Loom and official Mojang/Parchment mappings:

```gradle
dependencies {
    modImplementation "com.arcanc.pulselib:pulselib-fabric:1.21.1-1.1.5.3"
}
```

These coordinates describe the MultiLoader build; use a version actually published to the repository. Refresh Gradle after changing dependencies. `common` is a development module, not an installable mod or Maven dependency published by this build.

Declare PulseLib as a required mod dependency in your loader metadata. For Fabric this is the ordinary `depends.pulselib` declaration; [PulseLib plugins](plugins.md) require no extra entrypoint. Ordinary mod initialization for registering Minecraft items/blocks still uses the loader's normal lifecycle.

## For players

Download the artifact matching your loader from [CurseForge](https://www.curseforge.com/minecraft/mc-mods/pulselib) or [Modrinth](https://modrinth.com/mod/pulselib). Both game JARs bundle jgltf and Jackson dependencies.

## Building PulseLib

```sh
./gradlew build
./gradlew :neoforge:runClient
./gradlew :fabric:runClient
./gradlew :neoforge:runServer
./gradlew :fabric:runServer
```

Game artifacts are in `neoforge/build/libs` and `fabric/build/libs`, alongside source and Javadoc artifacts. Fabric's installable JAR is the remapped artifact without a sources/Javadoc suffix.

To test Maven output without uploading a release:

```sh
./gradlew :neoforge:publish :fabric:publish -PmavenRepoDir=/tmp/pulselib-maven
```

Modrinth/CurseForge tasks are `:<loader>:modrinth` and `:<loader>:publishCurseForge`. `publishMods` uploads both loader variants, and `publishAll` also writes Maven artifacts. These are explicit release operations requiring the relevant credentials; building does not invoke them.
