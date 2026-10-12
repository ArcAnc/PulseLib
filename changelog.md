## 2.0.0-rc.1 — MultiLoader

- Support Minecraft 1.21.1 on NeoForge and Fabric with shared animation/rendering code.
- Discover `@PulsePlugin` automatically on both loaders; no plugin entrypoints or manual registration.
- Remove PulseLibEvents, registerClientExtension(), NeoForge source variants and compatibility-only registration paths.
- Make discovery and registration failure states explicit and prevent replay of one-time phases.
- Move original demo blocks/entities/items/armor/tails/player animations into an external cross-platform test mod.
- Split CI by loader and retain build/server diagnostics on failure.

Breaking API release candidate. Migration: wiki/plugins.md. No release uploads performed.

Previous change: fix third person wrong hand rotation.
