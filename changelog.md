## PulseLib 1.2.0 for Minecraft 1.21.1

- Added Fabric support alongside NeoForge. Fabric requires Fabric API.
- Added controls to pause, reverse, restart, and stop overlay animations. Interrupted transitions now restart from the current pose, and animation events fire correctly across loop boundaries.
- Fixed shared playback between separate item stacks, incorrect entity draw order, and entity rendering in GUIs.
- Fixed translucent models disappearing or overlapping incorrectly in Fabulous graphics mode.
- Fixed camera offsets and attachment alignment during player animations, including rigs with nested bones and root motion.
- Moved demo content into a separate test mod.

**Upgrading from 1.1.x?** Extension registration now uses `@PulsePlugin` on both loaders. See the [migration guide](https://github.com/ArcAnc/PulseLib/blob/1.21.1-multiloader/wiki/plugins.md#migrating-from-pulselib-11x).
