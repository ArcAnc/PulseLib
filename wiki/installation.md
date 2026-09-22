# Installation

## For developers

Add the official ArcAnc Maven repository and PulseLib dependency to your `build.gradle`:

```gradle
repositories {
    maven {
        url = "https://arcanc.github.io/Maven/"
    }
}

dependencies {
    // Current project version: Minecraft 26.2.2, PulseLib 1.1.4.3
    implementation "com.arcanc.pulselib:pulselib-neoforge:26.2.2-1.1.4.3"
}
```

Replace the version with the PulseLib release that matches your Minecraft version, then refresh Gradle.

## For players

Download PulseLib from [CurseForge](https://www.curseforge.com/minecraft/mc-mods/pulselib) or [Modrinth](https://modrinth.com/mod/pulselib).
