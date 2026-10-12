## 1.2.0 — MultiLoader (beta)

- Support Minecraft 1.21.1 on NeoForge and Fabric with shared animation/rendering code.
- Discover `@PulsePlugin` automatically on both loaders; no plugin entrypoints or manual registration.
- Remove `PulseLibEvents`, the old NeoForge-specific extension registration mechanism and `PItemAnimatable.registerClientExtension()`. Use `@PulsePlugin` and `getPulseItemRenderer()` instead.
- Make discovery and registration failure states explicit and prevent replay of one-time phases.
- Move original demo blocks/entities/items/armor/tails/player animations into an external cross-platform test mod.
- Build Common, NeoForge and Fabric and run JUnit with `./gradlew build --no-daemon` in one CI job; upload loader JARs and test reports. Game/demo smoke tests remain opt-in manual checks.

This beta intentionally includes incompatible API changes: existing mods may require source adaptation. See the [migration guide](wiki/plugins.md). No release uploads performed.

PulseLib uses its own version scheme: `1.1.x` covers small updates and fixes, `1.2.x` covers major updates including MultiLoader and architectural changes, and `2.0.x` is reserved for a future stage defined by the author. API incompatibility alone does not determine a major-version increase.

Previous change: fix third person wrong hand rotation.
