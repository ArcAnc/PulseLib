# Installation

This branch targets **Minecraft 26.1.2**, **Java 25**, and **PulseLib 1.1.5.3**. Its platform versions are pinned in [`gradle.properties`](../gradle.properties): Fabric Loader `0.19.5`, Fabric API `0.155.3+26.1.2`, and NeoForge `26.1.2.95`.

## For developers

Use the loader-specific artifact. The Common module contains shared code and resources; it is not a standalone mod JAR. Both loader JARs include Common.

| Project | Dependency coordinate |
| --- | --- |
| Fabric | `com.arcanc.pulselib:pulselib-fabric:26.1.2-1.1.5.3` |
| NeoForge | `com.arcanc.pulselib:pulselib-neoforge:26.1.2-1.1.5.3` |

Both loader projects support Maven, Modrinth and CurseForge publication. The coordinates above describe this branch; use a version available in the public repository, or build locally until that version is released.

For a published release, add the [ArcAnc Maven repository](https://arcanc.github.io/Maven/) and the dependency matching your loader and Minecraft version:

```gradle
repositories {
    maven { url = "https://arcanc.github.io/Maven/" }
}

dependencies {
    // NeoForge consumer; use only a version available in the repository.
    implementation "com.arcanc.pulselib:pulselib-neoforge:26.1.2-1.1.5.3"
}
```

For the current unpublished port, build from this checkout with JDK 25:

```sh
./gradlew :fabric:build :neoforge:build
```

The loader JARs are written to `fabric/build/libs/` and `neoforge/build/libs/`. Add the matching JAR as a local dependency in your loader project; Fabric also needs Fabric API. For example, with the JAR in the consumer's `libs/` directory:

```gradle
dependencies {
    implementation files("libs/pulselib-26.1.2-neoforge-1.1.5.3.jar")
}
```

Keep shared PulseLib integration in your Common source set and Minecraft renderer registration in each loader's client source set. See [Getting started](getting-started.md).

## Publishing from this checkout

`publish` writes both loader publications to a local Maven repository. The default destination is `build/maven-repository/`; `mavenRepoDir` selects another directory:

```sh
./gradlew :fabric:publish :neoforge:publish -PmavenRepoDir=/absolute/path/to/maven-repository
```

The Maven workflow runs both publications and pushes the generated repository to `ArcAnc/Maven` using `MAVEN_REPO_TOKEN`. It runs on a published GitHub release or a manual workflow dispatch.

`publishMods` uploads both loaders to Modrinth and CurseForge. `publishAll` combines those uploads with Maven publication. For a Fabric-only upload, run `:fabric:modrinth` and `:fabric:publishCurseForge`. Each loader has its own version suffix and artifacts; Fabric releases declare Fabric API as a required dependency on both services.

Provide `MODRINTH_TOKEN` and `CURSEFORGE_TOKEN` in the environment, or Gradle properties `modrinthToken` and `curseforgeToken`, before running upload tasks. Keep credentials outside the repository. Use `./gradlew publishAll --dry-run` to inspect the release task graph without uploading files.

## For players

Download a release matching your Minecraft version and loader from [CurseForge](https://www.curseforge.com/minecraft/mc-mods/pulselib) or [Modrinth](https://modrinth.com/mod/pulselib). Install one loader-specific PulseLib JAR; Fabric installations also require Fabric API. The development branch and an uploaded release may have different availability.
