# PulseLib 1.2.0 — Minecraft 26.2

## Changes

* Ported PulseLib to Minecraft 26.2 for Fabric and NeoForge.
* Moved world and GUI rendering onto Minecraft’s native rendering API for OpenGL and Vulkan. The render queue uses indexed instancing for supported geometry.
* Added four-layer order-independent transparency (OIT) for supported translucent draws, with depth-aware compositing in Fabulous mode.
* Restored isolated rendering for individual GUI models and preserved player armor rendering through the owning player’s render state.
* Updated the wiki and added runnable test-mod examples for both loaders.

## Requirements

* Minecraft 26.2 and Java 25.
* Install the PulseLib artifact matching your loader. Fabric also requires Fabric API.

With NeoForge 26.2.0.57, Vulkan startup may fail with GLFW error 65540. Set `earlyWindowControl = false` in `config/fml.toml` if you encounter this error.
